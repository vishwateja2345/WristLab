package studio.teja.gesturelab;

import android.app.*;
import android.bluetooth.*;
import android.content.*;
import android.content.pm.*;
import android.hardware.*;
import android.os.*;
import android.view.*;
import java.lang.ref.WeakReference;

/** User-visible Bluetooth session. No disk logs, wake lock, network, or screen-content access. */
public final class SessionService extends Service implements SensorEventListener {
  public interface Listener {
    void changed();
  }

  public static volatile SessionService instance;
  public boolean active = true, connected, priming, uiVisible, screenOn, sensorsOn;
  public String status = "Connecting…", feedback = "", source = "";
  public int battery = 100, samples, pinches, previousActions;
  public boolean phoneLocked;
  public long feedbackAt, lastInput, lastActivity, lastLatency;
  private PhoneBridgeClient link;
  private SensorManager sensors;
  private Sensor gyro;
  private final BackGesture back = new BackGesture();
  private final Handler main = new Handler(Looper.getMainLooper());
  private WeakReference<Listener> listener = new WeakReference<>(null);
  private long settle, primeStart, lastGyro;
  private int retries;
  private boolean registered;
  private double biasY;
  private final Runnable idle =
      new Runnable() {
        public void run() {
          if (!active) return;
          if (SystemClock.elapsedRealtime() - lastInput > 300_000) {
            stopSession();
            return;
          }
          main.postDelayed(this, 30_000);
        }
      };
  private final BroadcastReceiver power =
      new BroadcastReceiver() {
        public void onReceive(Context c, Intent i) {
          String a = i.getAction();
          if (Intent.ACTION_BATTERY_CHANGED.equals(a)) {
            battery =
                i.getIntExtra(BatteryManager.EXTRA_LEVEL, 100)
                    * 100
                    / Math.max(1, i.getIntExtra(BatteryManager.EXTRA_SCALE, 100));
            if (battery < 5) {
              stopSession();
              return;
            }
          }
          screenOn = getSystemService(PowerManager.class).isInteractive();
          if (Intent.ACTION_SCREEN_OFF.equals(a)) screenOn = false;
          updateMotion();
          changed();
        }
      };

  @Override
  public void onCreate() {
    super.onCreate();
    instance = this;
    screenOn = getSystemService(PowerManager.class).isInteractive();
    sensors = getSystemService(SensorManager.class);
    gyro = sensors.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
    biasY = getSharedPreferences("gesturelab", 0).getFloat("by", 0);
    lastInput = lastActivity = SystemClock.elapsedRealtime();
    link =
        new PhoneBridgeClient(
            this,
            new PhoneBridgeClient.Listener() {
              public void state(String text) {
                if (!active) return;
                status = text;
                connected = link.connected;
                if (connected) {
                  retries = 0;
                  prime();
                } else {
                  stopSensors();
                  if (retries < 3) {
                    final int attempt = ++retries;
                    main.postDelayed(
                        () -> {
                          if (active && !connected && retries == attempt) connect();
                        },
                        attempt * 3000);
                  }
                }
                notifyState();
              }

              public void completed(String result) {
                lastLatency = link.lastLatencyMs;
                phoneLocked = result.equals("LOCKED");
                feedbackAt = SystemClock.elapsedRealtime();
                if (result.equals("DONE")) haptic();
                feedback =
                    result.equals("DONE")
                        ? "One step completed"
                        : result.equals("LOCKED")
                            ? "Unlock your phone"
                            : "Swipe " + result.toLowerCase();
                lastActivity = SystemClock.elapsedRealtime();
                changed();
              }
            });
    IntentFilter f = new IntentFilter();
    f.addAction(Intent.ACTION_SCREEN_ON);
    f.addAction(Intent.ACTION_SCREEN_OFF);
    f.addAction(Intent.ACTION_USER_PRESENT);
    f.addAction(Intent.ACTION_BATTERY_CHANGED);
    registerReceiver(power, f);
    registered = true;
    connect();
    main.postDelayed(idle, 30_000);
  }

  private void connect() {
    String host = getSharedPreferences("gesturelab", 0).getString("host", null);
    if (host == null) {
      status = "Choose your phone in Setup";
      notifyState();
      return;
    }
    link.connect(host);
  }

  private void notifyState() {
    changed();
  }

  public void observe(Listener l) {
    listener = new WeakReference<>(l);
    changed();
  }

  private void changed() {
    Listener l = listener.get();
    if (l != null) l.changed();
  }

  public void setUiVisible(boolean value) {
    boolean switched = uiVisible != value;
    uiVisible = value;
    updateMotion();
    if (value) {
      lastActivity = SystemClock.elapsedRealtime();
      if (connected && !sensorsOn) prime();
    }
    changed();
  }

  private boolean unlocked() {
    KeyguardManager k = getSystemService(KeyguardManager.class);
    return k == null || !k.isKeyguardLocked();
  }

  private void prime() {
    if (!active || !connected) return;
    priming = true;
    settle = 0;
    primeStart = SystemClock.elapsedRealtime();
    back.reset();
    updateMotion();
  }

  private void updateMotion() {
    boolean run = SessionPolicy.motion(active, connected, uiVisible, screenOn, unlocked(), battery);
    if (run && !sensorsOn) {
      if (gyro != null) {
        sensors.registerListener(this, gyro, 20_000);
        sensorsOn = true;
        priming = true;
        primeStart = SystemClock.elapsedRealtime();
        settle = 0;
      }
    } else if (!run) stopSensors();
  }

  private void stopSensors() {
    sensors.unregisterListener(this);
    sensorsOn = false;
    priming = false;
    back.reset();
  }

  public boolean next() {
    if (!SessionPolicy.send(active, connected, uiVisible, unlocked())) return false;
    pinches++;
    source = uiVisible ? "PINCH_NEXT" : "BG_PINCH_NEXT";
    back.cancel(SystemClock.elapsedRealtimeNanos() + 900_000_000L);
    return send(1);
  }

  public boolean previous() {
    if (!active || !connected || !uiVisible || !screenOn || !unlocked()) return false;
    source = "WRIST_PREVIOUS";
    return send(-1);
  }

  private boolean send(int direction) {
    boolean ok = link.swipe(direction);
    lastInput = lastActivity = SystemClock.elapsedRealtime();
    if (ok) {
      if (direction < 0) previousActions++;
      feedback = direction > 0 ? "Next reel requested" : "Previous reel requested";
    } else feedback = "Swipe finishing · wait briefly";
    changed();
    return ok;
  }

  public void recenter() {
    prime();
    feedback = "Hold your wrist still briefly";
    changed();
  }

  private void haptic() {
    Vibrator v = getSystemService(Vibrator.class);
    if (v != null && v.hasVibrator())
      v.vibrate(VibrationEffect.createOneShot(20, VibrationEffect.DEFAULT_AMPLITUDE));
  }

  @Override
  public void onAccuracyChanged(Sensor sensor, int accuracy) {}

  @Override
  public void onSensorChanged(SensorEvent event) {
    if (!active || !sensorsOn) return;
    samples++;
    lastGyro = event.timestamp;
    float x = event.values[0], y = event.values[1], z = event.values[2];
    double speed = Math.sqrt(x * x + y * y + z * z);
    long now = SystemClock.elapsedRealtime();
    if (priming) {
      if (speed < .35) {
        if (settle == 0) settle = now;
        if (now - settle >= 600) {
          priming = false;
          feedback = "Ready";
          back.reset();
          changed();
        }
      } else settle = 0;
      if (now - primeStart > 8000) {
        priming = false;
        back.reset();
        feedback = "Ready · keep gestures deliberate";
        changed();
      }
      return;
    }
    if (back.sample(y - biasY, event.timestamp) != 0) previous();
  }

  public void stopSession() {
    active = false;
    stopSensors();
    if (link != null) link.close();
    connected = false;
    status = "Controls stopped";
    changed();
    stopForeground(STOP_FOREGROUND_REMOVE);
    stopSelf();
  }

  @Override
  public int onStartCommand(Intent i, int flags, int id) {
    if (i != null && "STOP".equals(i.getAction())) {
      stopSession();
      return START_NOT_STICKY;
    }
    return START_NOT_STICKY;
  }

  @Override
  public void onTaskRemoved(Intent rootIntent) {
    stopSession();
    super.onTaskRemoved(rootIntent);
  }

  @Override
  public void onDestroy() {
    active = false;
    main.removeCallbacksAndMessages(null);
    stopSensors();
    if (link != null) link.destroy();
    if (registered)
      try {
        unregisterReceiver(power);
      } catch (Exception ignored) {
      }
    if (instance == this) instance = null;
    getSystemService(NotificationManager.class).cancel(3);
    super.onDestroy();
  }

  @Override
  public IBinder onBind(Intent i) {
    return null;
  }

  @Override
  protected void dump(java.io.FileDescriptor fd, java.io.PrintWriter w, String[] args) {
    w.println(
        "SessionState active="
            + active
            + " ui="
            + uiVisible
            + " awake="
            + screenOn
            + " connected="
            + connected
            + " sensors="
            + sensorsOn
            + " priming="
            + priming
            + " phoneLocked="
            + phoneLocked
            + " samples="
            + samples
            + " pinches="
            + pinches
            + " previous="
            + previousActions
            + " source="
            + source
            + " result="
            + (link == null ? "" : link.lastResult)
            + " latencyMs="
            + lastLatency
            + " status="
            + status);
  }
}

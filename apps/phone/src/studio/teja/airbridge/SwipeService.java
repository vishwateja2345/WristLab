package studio.teja.airbridge;

import android.accessibilityservice.*;
import android.app.*;
import android.bluetooth.*;
import android.content.*;
import android.graphics.*;
import android.os.*;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;

public final class SwipeService extends AccessibilityService {
  public static volatile SwipeService instance;
  public LinkReceiver receiver;
  private boolean registered, sessionActive;
  private final Handler sessionHandler = new Handler(Looper.getMainLooper());
  private final Runnable setupTimeout =
      () -> {
        if (receiver != null && !receiver.connected) endSession();
      };

  public interface Reply {
    void result(String result);
  }

  private final BroadcastReceiver bluetooth =
      new BroadcastReceiver() {
        public void onReceive(Context c, Intent i) {
          BluetoothAdapter a = getSystemService(BluetoothManager.class).getAdapter();
          if (a != null && a.isEnabled()) ensureReady();
          else if (receiver != null) {
            receiver.stop();
            updateNotification();
          }
        }
      };

  @Override
  protected void onServiceConnected() {
    instance = this;
    sessionActive = true;
    receiver = new LinkReceiver(this);
    getSharedPreferences("bridge", 0).edit().putBoolean("autoReady", false).apply();
    if (!registered) {
      if (Build.VERSION.SDK_INT >= 33)
        registerReceiver(
            bluetooth,
            new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED),
            Context.RECEIVER_EXPORTED);
      else registerReceiver(bluetooth, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
      registered = true;
    }
    ensureReady();
  }

  public void ensureReady() {
    if (sessionActive
        && receiver != null
        && checkSelfPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
            == android.content.pm.PackageManager.PERMISSION_GRANTED
        && sessionActive) receiver.start();
    sessionHandler.removeCallbacks(setupTimeout);
    if (sessionActive) sessionHandler.postDelayed(setupTimeout, 60_000);
    updateNotification();
  }

  public void setReady(boolean enabled) {
    if (enabled) {
      sessionActive = true;
      ensureReady();
    } else endSession();
  }

  public void controllerGone() {
    sessionHandler.post(() -> {
      if (sessionActive) endSession();
    });
  }

  private void endSession() {
    sessionActive = false;
    getSharedPreferences("bridge", 0).edit().putBoolean("autoReady", false).apply();
    sessionHandler.removeCallbacks(setupTimeout);
    if (receiver != null) receiver.stop();
    updateNotification();
    disableSelf();
  }

  public void updateNotification() {
    getSystemService(NotificationManager.class).cancel(2);
  }

  @Override
  public int onStartCommand(Intent i, int flags, int id) {
    if (i != null) {
      if (i.getAction() != null && i.getAction().equals("PAUSE")) setReady(false);
      else if (i.getAction() != null && i.getAction().equals("READY")) setReady(true);
    }
    return START_NOT_STICKY;
  }

  @Override
  public void onAccessibilityEvent(
      AccessibilityEvent e) {} // No event subscription or foreground-app detection.

  @Override
  public void onInterrupt() {}

  @Override
  public void onDestroy() {
    sessionActive = false;
    sessionHandler.removeCallbacksAndMessages(null);
    if (instance == this) instance = null;
    if (receiver != null) receiver.destroy();
    if (registered)
      try {
        unregisterReceiver(bluetooth);
      } catch (Exception ignored) {
      }
    getSystemService(NotificationManager.class).cancel(2);
    super.onDestroy();
  }

  public void swipe(int direction, java.util.function.BooleanSupplier active, Reply reply) {
    long received = SystemClock.elapsedRealtime();
    new Handler(Looper.getMainLooper())
        .post(() -> {
          if (instance != this
              || !active.getAsBoolean()
              || SystemClock.elapsedRealtime() - received > 500) {
            reply.result("OFF");
            return;
          }
          KeyguardManager k = getSystemService(KeyguardManager.class);
          if (k != null && k.isKeyguardLocked()) {
            reply.result("LOCKED");
            return;
          }
          try {
            Rect b = getSystemService(WindowManager.class).getMaximumWindowMetrics().getBounds();
            SwipePlan p = new SwipePlan(b.width(), b.height(), direction);
            Path path = new Path();
            path.moveTo(b.left + p.x, b.top + p.startY);
            path.lineTo(b.left + p.x, b.top + p.endY);
            GestureDescription g =
                new GestureDescription.Builder()
                    .addStroke(new GestureDescription.StrokeDescription(path, 0, p.durationMs))
                    .build();
            boolean sent =
                dispatchGesture(
                    g,
                    new GestureResultCallback() {
                      @Override
                      public void onCompleted(GestureDescription d) {
                        reply.result("DONE");
                      }

                      @Override
                      public void onCancelled(GestureDescription d) {
                        reply.result("CANCELLED");
                      }
                    },
                    null);
            if (!sent) reply.result("REJECTED");
          } catch (Exception e) {
            reply.result("REJECTED");
          }
        });
  }

  @Override
  protected void dump(java.io.FileDescriptor fd, java.io.PrintWriter w, String[] args) {
    LinkReceiver r = receiver;
    w.println(
        "SimpleLinkState bound="
            + (instance == this)
            + " sessionActive="
            + sessionActive
            + " listening="
            + (r != null && r.listening)
            + " connected="
            + (r != null && r.connected)
            + " completed="
            + (r == null ? 0 : r.completed)
            + " direction="
            + (r == null ? "" : r.lastDirection)
            + " result="
            + (r == null ? "" : r.lastResult));
  }
}

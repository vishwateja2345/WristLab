package studio.teja.watchdeck;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.Settings;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.util.Locale;

public final class MainActivity extends Activity implements SensorEventListener {
  private static final int BG = Color.rgb(8, 12, 16),
      SURFACE = Color.rgb(22, 29, 34),
      INK = Color.rgb(241, 245, 242),
      MUTED = Color.rgb(146, 160, 165),
      ACCENT = Color.rgb(168, 230, 209);
  private final DeckState state = new DeckState();
  private final Handler handler = new Handler(Looper.getMainLooper());
  private SharedPreferences preferences;
  private SensorManager sensorManager;
  private Sensor stepSensor;
  private boolean resumed;
  private int tab;
  private ScrollView scroll;
  private LinearLayout column;
  private ProgressRing ring;
  private TextView timerText;
  private Button primaryTimer;
  private final Runnable tick =
      new Runnable() {
        public void run() {
          if (!resumed) return;
          if (state.settle(SystemClock.elapsedRealtime())) {
            save();
            if (state.haptics) vibrate();
          }
          if (tab == 1) updateTimer();
          if (state.running) handler.postDelayed(this, tab == 1 ? 500 : 1000);
        }
      };

  @Override
  public void onCreate(Bundle b) {
    super.onCreate(b);
    getWindow()
        .getDecorView()
        .setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    preferences = getSharedPreferences("deck", MODE_PRIVATE);
    state.sessionSteps = preferences.getLong("steps", 0);
    state.stepTarget = preferences.getInt("target", 2000);
    state.focusMinutes = preferences.getInt("minutes", 25);
    state.haptics = preferences.getBoolean("haptics", true);
    state.remaining = preferences.getLong("remaining", state.focusMinutes * 60000L);
    state.deadline = preferences.getLong("deadline", 0);
    state.running = preferences.getBoolean("running", false);
    int lastBoot = preferences.getInt("boot", -1), currentBoot = bootCount();
    if (lastBoot >= 0 && currentBoot >= 0 && lastBoot != currentBoot) state.deviceRebooted();
    sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
    stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
    tab = b == null ? 0 : b.getInt("tab", 0);
    buildScreen();
  }

  private int bootCount() {
    try {
      return Settings.Global.getInt(getContentResolver(), "boot_count");
    } catch (Exception ignored) {
      return -1;
    }
  }

  @Override
  protected void onResume() {
    super.onResume();
    resumed = true;
    state.beginForeground();
    registerSteps();
    if (state.settle(SystemClock.elapsedRealtime())) {
      save();
      if (state.haptics) vibrate();
    }
    buildScreen();
    handler.post(tick);
  }

  @Override
  protected void onPause() {
    resumed = false;
    handler.removeCallbacks(tick);
    sensorManager.unregisterListener(this);
    state.endForeground();
    save();
    super.onPause();
  }

  @Override
  protected void onSaveInstanceState(Bundle b) {
    b.putInt("tab", tab);
    super.onSaveInstanceState(b);
  }

  @Override
  public void onBackPressed() {
    if (tab != 0) {
      tab = 0;
      buildScreen();
    } else super.onBackPressed();
  }

  private boolean activityAccess() {
    return checkSelfPermission(Manifest.permission.ACTIVITY_RECOGNITION)
        == PackageManager.PERMISSION_GRANTED;
  }

  private void requestActivityAccess() {
    if (preferences.getBoolean("activityAsked", false)
        && !shouldShowRequestPermissionRationale(Manifest.permission.ACTIVITY_RECOGNITION)) {
      startActivity(
          new Intent(
              Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
              Uri.parse("package:" + getPackageName())));
    } else {
      preferences.edit().putBoolean("activityAsked", true).apply();
      requestPermissions(new String[] {Manifest.permission.ACTIVITY_RECOGNITION}, 7);
    }
  }

  private void registerSteps() {
    if (resumed && activityAccess() && stepSensor != null)
      sensorManager.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_NORMAL);
  }

  @Override
  public void onSensorChanged(SensorEvent e) {
    if (e.sensor.getType() == Sensor.TYPE_STEP_COUNTER) {
      state.acceptStepCounter((long) e.values[0]);
      if (ring != null && tab == 0)
        ring.setState(
            state.sessionSteps / (float) state.stepTarget,
            activityAccess() ? String.format(Locale.getDefault(), "%,d", state.sessionSteps) : "—",
            "SESSION STEPS");
      save();
    }
  }

  @Override
  public void onAccuracyChanged(Sensor s, int accuracy) {}

  @Override
  public void onRequestPermissionsResult(int r, String[] permissions, int[] grants) {
    super.onRequestPermissionsResult(r, permissions, grants);
    if (r == 7) {
      state.beginForeground();
      registerSteps();
      buildScreen();
    }
  }

  private void save() {
    preferences
        .edit()
        .putLong("steps", state.sessionSteps)
        .putInt("target", state.stepTarget)
        .putInt("minutes", state.focusMinutes)
        .putBoolean("haptics", state.haptics)
        .putBoolean("running", state.running)
        .putLong("remaining", state.remaining)
        .putLong("deadline", state.deadline)
        .putInt("boot", bootCount())
        .apply();
  }

  private void vibrate() {
    Vibrator v = (Vibrator) getSystemService(VIBRATOR_SERVICE);
    if (v != null && v.hasVibrator())
      v.vibrate(VibrationEffect.createOneShot(120, VibrationEffect.DEFAULT_AMPLITUDE));
  }

  private int dp(float x) {
    return Math.round(x * getResources().getDisplayMetrics().density);
  }

  private TextView label(String text, float size, int color) {
    TextView t = new TextView(this);
    t.setText(text);
    t.setTextSize(size);
    t.setTextColor(color);
    t.setGravity(Gravity.CENTER);
    t.setFontFeatureSettings("tnum");
    return t;
  }

  private void add(View v, int height) {
    LinearLayout.LayoutParams lp =
        new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, height < 0 ? height : dp(height));
    lp.topMargin = dp(6);
    column.addView(v, lp);
  }

  private Button button(String title, boolean prominent, Runnable action) {
    Button b = new Button(this);
    b.setText(title);
    b.setAllCaps(false);
    b.setTextSize(14);
    b.setTextColor(prominent ? BG : INK);
    b.setMinHeight(dp(48));
    b.setMinimumHeight(dp(48));
    b.setMinWidth(0);
    b.setMinimumWidth(0);
    b.setPadding(dp(8), 0, dp(8), 0);
    GradientDrawable base = new GradientDrawable();
    base.setCornerRadius(dp(26));
    base.setColor(prominent ? ACCENT : SURFACE);
    GradientDrawable mask = new GradientDrawable();
    mask.setCornerRadius(dp(26));
    mask.setColor(Color.WHITE);
    b.setBackground(
        new RippleDrawable(ColorStateList.valueOf(Color.argb(40, 220, 255, 245)), base, mask));
    b.setOnClickListener(v -> action.run());
    return b;
  }

  private void buildScreen() {
    ring = null;
    timerText = null;
    primaryTimer = null;
    scroll = new ScrollView(this);
    scroll.setBackgroundColor(BG);
    scroll.setFillViewport(true);
    scroll.setClipToPadding(false);
    scroll.setPadding(dp(24), dp(16), dp(24), dp(30));
    scroll.setVerticalScrollBarEnabled(false);
    column = new LinearLayout(this);
    column.setOrientation(LinearLayout.VERTICAL);
    column.setGravity(Gravity.CENTER_HORIZONTAL);
    scroll.addView(column, new ScrollView.LayoutParams(-1, -2));
    TextView title = label("WATCH DECK", 12, MUTED);
    title.setLetterSpacing(.16f);
    add(title, 18);
    if (tab == 0) buildSteps();
    else if (tab == 1) buildFocus();
    else buildMore();
    LinearLayout nav = new LinearLayout(this);
    nav.setGravity(Gravity.CENTER);
    String[] names = {"Steps", "Focus", "More"};
    for (int i = 0; i < 3; i++) {
      final int choice = i;
      Button b =
          button(
              names[i],
              tab == i,
              () -> {
                tab = choice;
                buildScreen();
                handler.removeCallbacks(tick);
                handler.post(tick);
              });
      b.setTextSize(12);
      LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(48), 1);
      lp.setMargins(dp(2), 0, dp(2), 0);
      nav.addView(b, lp);
    }
    add(nav, 48);
    scroll.setOnGenericMotionListener((v, e) -> {
      if (e.getAction() == MotionEvent.ACTION_SCROLL) {
        scroll.scrollBy(0, Math.round(-e.getAxisValue(MotionEvent.AXIS_SCROLL) * dp(46)));
        return true;
      }
      return false;
    });
    setContentView(scroll);
  }

  private void buildSteps() {
    boolean allowed = activityAccess();
    ring = new ProgressRing(this);
    ring.setState(
        allowed ? state.sessionSteps / (float) state.stepTarget : 0,
        allowed ? String.format(Locale.getDefault(), "%,d", state.sessionSteps) : "—",
        "SESSION STEPS");
    add(ring, 118);
    add(
        label("Target " + String.format(Locale.getDefault(), "%,d", state.stepTarget), 14, INK),
        24);
    if (!allowed) {
      add(button("Enable activity access", true, () -> requestActivityAccess()), 48);
      add(label("Optional. Focus works without it.", 12, MUTED), 34);
    } else if (stepSensor == null)
      add(label("This watch has no step counter sensor.", 13, MUTED), 42);
    else
      add(
          label(
              "Counts while Watch Deck is open.\nNot your Samsung Health daily total.", 12, MUTED),
          44);
    add(
        button(
            "New walking session",
            false,
            () ->
                new AlertDialog.Builder(this)
                    .setTitle("Reset session steps?")
                    .setMessage("Your Samsung Health history is unchanged.")
                    .setNegativeButton("Cancel", null)
                    .setPositiveButton(
                        "Reset",
                        (d, w) -> {
                          state.resetSteps();
                          save();
                          buildScreen();
                        })
                    .show()),
        48);
  }

  private void buildFocus() {
    add(label("FOCUS", 12, MUTED), 16);
    timerText = label("", 40, INK);
    timerText.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
    add(timerText, 48);
    ring = new ProgressRing(this);
    ring.small = true;
    add(ring, 8);
    primaryTimer =
        button(
            state.running ? "Pause" : "Start focus",
            true,
            () -> {
              long now = SystemClock.elapsedRealtime();
              if (state.running) state.pause(now);
              else state.start(now);
              save();
              updateTimer();
              handler.removeCallbacks(tick);
              handler.post(tick);
            });
    add(primaryTimer, 48);
    add(
        button(
            "Reset",
            false,
            () -> {
              state.resetTimer();
              save();
              updateTimer();
            }),
        48);
    add(button("Length · " + state.focusMinutes + " min", false, () -> chooseDuration()), 48);
    add(label("No background alarm.\nHaptic while open or on return.", 12, MUTED), 48);
    updateTimer();
  }

  private void updateTimer() {
    if (timerText == null) return;
    long remaining = state.remainingAt(SystemClock.elapsedRealtime());
    long seconds = (remaining + 999) / 1000;
    timerText.setText(String.format(Locale.getDefault(), "%02d:%02d", seconds / 60, seconds % 60));
    timerText.setContentDescription(
        "Focus time remaining " + seconds / 60 + " minutes " + seconds % 60 + " seconds");
    if (primaryTimer != null)
      primaryTimer.setText(
          state.running ? "Pause" : remaining == 0 ? "Start again" : "Start focus");
    if (ring != null) ring.setState(1 - remaining / (float) (state.focusMinutes * 60000L), "", "");
  }

  private void chooseDuration() {
    int[] minutes = {15, 25, 45};
    String[] options = {"15 minutes", "25 minutes", "45 minutes"};
    new AlertDialog.Builder(this)
        .setTitle("Length (resets timer)")
        .setItems(
            options,
            (d, w) -> {
              state.changeMinutes(minutes[w]);
              save();
              buildScreen();
            })
        .show();
  }

  private void buildMore() {
    add(label("Make it yours", 20, INK), 42);
    add(
        button(
            "Step target · " + String.format(Locale.getDefault(), "%,d", state.stepTarget),
            false,
            () -> {
              int[] targets = {1000, 2000, 5000, 10000};
              String[] labels = {
                "1,000 session steps",
                "2,000 session steps",
                "5,000 session steps",
                "10,000 session steps"
              };
              new AlertDialog.Builder(this)
                  .setTitle("Session target")
                  .setItems(
                      labels,
                      (d, w) -> {
                        state.stepTarget = targets[w];
                        save();
                        buildScreen();
                      })
                  .show();
            }),
        52);
    add(button("Focus length · " + state.focusMinutes + " min", false, () -> chooseDuration()), 52);
    add(
        button(
            "Finish haptic · " + (state.haptics ? "On" : "Off"),
            false,
            () -> {
              state.haptics = !state.haptics;
              save();
              buildScreen();
            }),
        52);
    add(label("Activity access · " + (activityAccess() ? "Allowed" : "Off"), 13, MUTED), 32);
    add(label("Watch Deck 0.1\nLocal tools. No account or network.", 12, MUTED), 46);
  }

  private final class ProgressRing extends View {
    final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    float value;
    String number = "", caption = "";
    boolean small;

    ProgressRing(Context c) {
      super(c);
      setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
    }

    void setState(float v, String n, String cap) {
      value = Math.max(0, Math.min(1, v));
      number = n;
      caption = cap;
      setContentDescription(
          small
              ? "Focus progress " + Math.round(value * 100) + " percent"
              : n + " session steps, " + Math.round(value * 100) + " percent of target");
      invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
      super.onDraw(canvas);
      float w = getWidth(), h = getHeight();
      paint.setStyle(Paint.Style.STROKE);
      paint.setStrokeCap(Paint.Cap.ROUND);
      paint.setStrokeWidth(dp(small ? 4 : 5));
      if (small) {
        paint.setColor(SURFACE);
        canvas.drawLine(dp(8), h / 2, w - dp(8), h / 2, paint);
        paint.setColor(ACCENT);
        canvas.drawLine(dp(8), h / 2, dp(8) + (w - dp(16)) * value, h / 2, paint);
        return;
      }
      float dia = Math.min(w, h) - dp(14), left = (w - dia) / 2, top = (h - dia) / 2;
      RectF arc = new RectF(left, top, left + dia, top + dia);
      paint.setColor(SURFACE);
      canvas.drawArc(arc, 135, 270, false, paint);
      paint.setColor(ACCENT);
      canvas.drawArc(arc, 135, 270 * value, false, paint);
      paint.setStyle(Paint.Style.FILL);
      paint.setTextAlign(Paint.Align.CENTER);
      paint.setColor(INK);
      paint.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
      paint.setTextSize(dp(38));
      canvas.drawText(number, w / 2, h * .52f, paint);
      paint.setTextSize(dp(10));
      paint.setColor(MUTED);
      paint.setTypeface(Typeface.DEFAULT);
      canvas.drawText(caption, w / 2, h * .68f, paint);
    }
  }
}

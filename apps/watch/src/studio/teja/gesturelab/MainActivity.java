package studio.teja.gesturelab;

import android.Manifest;
import android.app.*;
import android.bluetooth.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.text.SimpleDateFormat;
import java.util.*;

public final class MainActivity extends Activity implements SessionService.Listener {
  private final Handler main = new Handler(Looper.getMainLooper());
  private SessionService observed;
  private ActionTarget action;
  private Surface surface;
  private boolean pinchEnabled;
  private boolean visible, setup, standby, lastInteractive;
  private long touched;
  private final Runnable tick =
      new Runnable() {
        public void run() {
          if (!visible) return;
          SessionService s = SessionService.instance;
          if (s != null && s != observed) {
            observed = s;
            s.observe(MainActivity.this);
            s.setUiVisible(!setup);
          }
          standby =
              (SystemClock.elapsedRealtime() - touched > 12_000)
                  || !getSystemService(PowerManager.class).isInteractive();
          if (surface != null && !setup) surface.invalidate();
          main.postDelayed(
              this, standby ? Math.max(1000, 60000 - System.currentTimeMillis() % 60000) : 1000);
        }
      };

  @Override
  public void onCreate(Bundle state) {
    super.onCreate(state);
    getWindow()
        .getDecorView()
        .setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    if (Build.VERSION.SDK_INT >= 33)
      getOnBackInvokedDispatcher()
          .registerOnBackInvokedCallback(
              android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT, () -> finish());
    home();
  }

  private boolean permission() {
    return checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)
        == PackageManager.PERMISSION_GRANTED;
  }

  private void start() {
    if (!permission()) {
      ArrayList<String> p = new ArrayList<>();
      p.add(Manifest.permission.BLUETOOTH_CONNECT);
      if (Build.VERSION.SDK_INT >= 33) p.add(Manifest.permission.POST_NOTIFICATIONS);
      requestPermissions(p.toArray(new String[0]), 1);
      return;
    }
    try {
      startService(new Intent(this, SessionService.class));
    } catch (Exception e) {
      Toast.makeText(this, "Open the app to start controls", Toast.LENGTH_SHORT).show();
    }
  }

  @Override
  protected void onResume() {
    super.onResume();
    visible = true;
    pinchEnabled = false;
    for (android.accessibilityservice.AccessibilityServiceInfo info :
        getSystemService(android.view.accessibility.AccessibilityManager.class)
            .getEnabledAccessibilityServiceList(
                android.accessibilityservice.AccessibilityServiceInfo.FEEDBACK_ALL_MASK)) {
      if (info.getId() != null && info.getId().contains("UniversalGestureService"))
        pinchEnabled = true;
    }
    lastInteractive = getSystemService(PowerManager.class).isInteractive();
    touched = SystemClock.elapsedRealtime();
    standby = false;
    if (SessionService.instance == null) start();
    else SessionService.instance.setUiVisible(true);
    main.post(tick);
  }

  @Override
  protected void onPause() {
    visible = false;
    main.removeCallbacks(tick);
    SessionService s = SessionService.instance;
    if (s != null) {
      s.observe(null);
      s.stopSession();
    }
    stopService(new Intent(this, SessionService.class));
    observed = null;
    super.onPause();
  }

  @Override
  protected void onDestroy() {
    main.removeCallbacksAndMessages(null);
    super.onDestroy();
  }

  @Override
  public void onRequestPermissionsResult(int r, String[] p, int[] g) {
    super.onRequestPermissionsResult(r, p, g);
    if (permission()) start();
  }

  @Override
  public void changed() {
    boolean awake = getSystemService(PowerManager.class).isInteractive();
    if (awake && !lastInteractive) {
      touched = SystemClock.elapsedRealtime();
      standby = false;
      main.removeCallbacks(tick);
      if (visible) main.postDelayed(tick, 1000);
    }
    if (!awake) standby = true;
    lastInteractive = awake;
    if (awake && SystemClock.elapsedRealtime() - touched < 12000 && standby) {
      standby = false;
      main.removeCallbacks(tick);
      if (visible) main.postDelayed(tick, 1000);
    }
    if (surface != null && !setup) surface.invalidate();
  }

  private void home() {
    setup = false;
    touched = SystemClock.elapsedRealtime();
    surface = new Surface();
    FrameLayout layer = new FrameLayout(this);
    layer.addView(surface, new FrameLayout.LayoutParams(-1, -1));
    action = new ActionTarget();
    float scale = getResources().getDisplayMetrics().widthPixels / 480f;
    FrameLayout.LayoutParams p =
        new FrameLayout.LayoutParams(Math.round(310 * scale), Math.round(110 * scale));
    p.leftMargin = Math.round(85 * scale);
    p.topMargin = Math.round(163 * scale);
    layer.addView(action, p);
    setContentView(layer);
    SessionService session = SessionService.instance;
    if (session != null) session.setUiVisible(true);
    action.postDelayed(
        () ->
            action.performAccessibilityAction(
                android.view.accessibility.AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS, null),
        500);
  }

  private void settings() {
    setup = true;
    SessionService session = SessionService.instance;
    if (session != null) session.setUiVisible(false);
    ScrollView scroll = new ScrollView(this);
    scroll.setBackgroundColor(Color.BLACK);
    scroll.setPadding(dp(24), dp(28), dp(24), dp(28));
    LinearLayout col = new LinearLayout(this);
    col.setOrientation(1);
    scroll.addView(col);
    setContentView(scroll);
    label(col, "SETUP", 18);
    button(col, "Choose phone", this::choose);
    button(
        col,
        "Samsung pinch settings",
        () -> {
          try {
            startActivity(new Intent("com.samsung.accessibility.action.UNIVERSAL_GESTURE"));
          } catch (Exception e) {
            Toast.makeText(this, "Settings > Accessibility > Universal gestures", Toast.LENGTH_LONG)
                .show();
          }
        });
    button(
        col,
        "Recenter wrist gesture",
        () -> {
          SessionService s = SessionService.instance;
          if (s != null) s.recenter();
          home();
        });
    button(
        col,
        "Stop controls",
        () -> {
          SessionService s = SessionService.instance;
          if (s != null) s.stopSession();
          home();
        });
    button(col, "Back", this::home);
    label(
        col,
        "Pinch: next. Turn the watch face slightly away and back: previous. Manual sessions only."
            + " Leaving this app stops the watch controller. Start a temporary phone session, open"
            + " Reels, then open this watch screen.",
        12);
  }

  private void choose() {
    if (!permission()) {
      start();
      return;
    }
    BluetoothAdapter a = getSystemService(BluetoothManager.class).getAdapter();
    ArrayList<BluetoothDevice> ds = new ArrayList<>(a.getBondedDevices());
    String[] names = new String[ds.size()];
    for (int n = 0; n < names.length; n++) names[n] = ds.get(n).getName();
    new AlertDialog.Builder(this)
        .setTitle("Your phone")
        .setItems(
            names,
            (dialog, index) -> {
              getSharedPreferences("gesturelab", 0)
                  .edit()
                  .putString("host", ds.get(index).getAddress())
                  .apply();
              SessionService s = SessionService.instance;
              if (s != null) s.stopSession();
              start();
              home();
            })
        .show();
  }

  private int dp(int n) {
    return Math.round(n * getResources().getDisplayMetrics().density);
  }

  private void label(LinearLayout col, String text, int size) {
    TextView t = new TextView(this);
    t.setText(text);
    t.setTextSize(size);
    t.setTextColor(0xffb4c7ca);
    t.setGravity(Gravity.CENTER);
    t.setPadding(0, dp(8), 0, dp(8));
    col.addView(t, new LinearLayout.LayoutParams(-1, -2));
  }

  private void button(LinearLayout col, String text, Runnable r) {
    Button b = new Button(this);
    b.setText(text);
    b.setAllCaps(false);
    b.setTextSize(13);
    b.setOnClickListener(v -> r.run());
    col.addView(b, new LinearLayout.LayoutParams(-1, dp(52)));
  }

  private final class ActionTarget extends View {
    ActionTarget() {
      super(MainActivity.this);
      setClickable(true);
      setFocusable(true);
      setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
      setContentDescription("Next reel");
    }

    @Override
    public CharSequence getAccessibilityClassName() {
      return "android.widget.Button";
    }

    @Override
    public void onInitializeAccessibilityNodeInfo(
        android.view.accessibility.AccessibilityNodeInfo n) {
      super.onInitializeAccessibilityNodeInfo(n);
      n.setClassName("android.widget.Button");
      n.setText("Next reel");
      n.setClickable(true);
      n.addAction(
          android.view.accessibility.AccessibilityNodeInfo.AccessibilityAction.ACTION_CLICK);
    }

    @Override
    public boolean performAccessibilityAction(int code, Bundle args) {
      if (code == android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK)
        return performClick();
      return super.performAccessibilityAction(code, args);
    }

    @Override
    public boolean performClick() {
      super.performClick();
      touched = SystemClock.elapsedRealtime();
      SessionService s = SessionService.instance;
      if (s == null) {
        start();
        return true;
      }
      return s.next();
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
      if (e.getAction() == MotionEvent.ACTION_UP) {
        if (standby) {
          standby = false;
          touched = SystemClock.elapsedRealtime();
          surface.invalidate();
        } else performClick();
      }
      return true;
    }
  }

  private final class Surface extends View {
    private final Paint p = new Paint(3);
    private float scale;
    private final RectF next = new RectF(85, 163, 395, 273),
        pause = new RectF(92, 359, 222, 457),
        config = new RectF(258, 359, 388, 457);

    Surface() {
      super(MainActivity.this);
      setFocusable(false);
      setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
    }

    private void text(Canvas c, String s, float x, float y, int size, int color) {
      p.setStyle(Paint.Style.FILL);
      p.setColor(color);
      p.setTypeface(Typeface.create("sans-serif-medium", 0));
      p.setTextAlign(Paint.Align.CENTER);
      p.setTextSize(size);
      c.drawText(s, x, y, p);
    }

    private void outline(Canvas c, RectF rect, String text) {
      p.setStyle(Paint.Style.STROKE);
      p.setStrokeWidth(2);
      p.setColor(0xff2b4c53);
      c.drawRoundRect(rect, 28, 28, p);
      text(c, text, rect.centerX(), rect.centerY() + 8, rect == next ? 32 : 24, 0xffb8cdce);
    }

    @Override
    protected void onDraw(Canvas c) {
      super.onDraw(c);
      c.drawColor(Color.BLACK);
      scale = getWidth() / 480f;
      c.save();
      c.scale(scale, scale);
      SessionService s = SessionService.instance;
      boolean running = s != null && s.active;
      String time = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());
      if (standby) {
        text(c, time, 240, 220, 48, 0xff879599);
        text(
            c,
            s != null && s.phoneLocked
                ? "UNLOCK PHONE"
                : running ? "CONTROLS READY" : "CONTROLS STOPPED",
            240,
            275,
            16,
            0xff53686c);
        text(
            c,
            (pinchEnabled ? "Raise wrist · pinch next" : "Pinch off · touch to wake"),
            240,
            324,
            14,
            0xff53686c);
      } else {
        text(c, time, 240, 54, 18, 0xff84969a);
        text(c, "GESTURE LAB", 240, 100, 24, 0xff83e1ce);
        text(
            c,
            s == null
                ? "Starting…"
                : s.connected
                    ? (s.phoneLocked
                        ? "Unlock your phone"
                        : SystemClock.elapsedRealtime() - s.feedbackAt < 3000
                            ? s.feedback
                            : s.priming ? "Hold wrist still briefly" : "Phone connected")
                    : s.status,
            240,
            137,
            20,
            0xff91a9af);
        outline(c, next, running ? "↑  NEXT" : "START");
        text(
            c,
            (pinchEnabled ? "Pinch → next reel" : "Pinch off · tap NEXT"),
            240,
            310,
            24,
            0xffd9e7e8);
        text(c, "Turn away & back → previous", 240, 342, 20, 0xff93acb2);
        outline(c, pause, running ? "STOP" : "START");
        outline(c, config, "SETUP");
      }
      c.restore();
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
      if (e.getAction() != MotionEvent.ACTION_UP) return true;
      float x = e.getX() / scale, y = e.getY() / scale;
      if (standby) {
        standby = false;
        touched = SystemClock.elapsedRealtime();
        invalidate();
        return true;
      }
      touched = SystemClock.elapsedRealtime();
      if (config.contains(x, y)) {
        settings();
        return true;
      }
      SessionService s = SessionService.instance;
      if (pause.contains(x, y)) {
        if (s != null) s.stopSession();
        else start();
      } else if (next.contains(x, y)) {
        if (s != null) s.next();
        else start();
      }
      invalidate();
      return true;
    }
  }

  @Override
  public void dump(String prefix, java.io.FileDescriptor fd, java.io.PrintWriter w, String[] args) {
    super.dump(prefix, fd, w, args);
    w.println(
        "InterfaceState visible="
            + visible
            + " standby="
            + standby
            + " setup="
            + setup
            + " pinchEnabled="
            + pinchEnabled);
  }
}

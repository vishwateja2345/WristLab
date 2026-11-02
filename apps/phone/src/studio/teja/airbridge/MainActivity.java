package studio.teja.airbridge;

import android.Manifest;
import android.app.*;
import android.bluetooth.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.os.*;
import android.provider.Settings;
import android.view.*;
import android.widget.*;
import java.util.*;

public final class MainActivity extends Activity {
  private TextView state, detail, watch;
  private Button toggle;
  private boolean visible;
  private String shown = "";
  private final Handler main = new Handler(Looper.getMainLooper());
  private final Runnable refresh =
      new Runnable() {
        public void run() {
          if (!visible) return;
          render();
          main.postDelayed(this, 2000);
        }
      };

  @Override
  public void onCreate(Bundle b) {
    super.onCreate(b);
    getWindow().setStatusBarColor(0xff071016);
    getWindow().setNavigationBarColor(0xff071016);
    ScrollView scroll = new ScrollView(this);
    scroll.setBackgroundColor(0xff071016);
    LinearLayout col = new LinearLayout(this);
    col.setOrientation(1);
    col.setPadding(dp(24), dp(24), dp(24), dp(30));
    scroll.addView(col);
    setContentView(scroll);
    scroll.setOnApplyWindowInsetsListener((v, i) -> {
      Insets a = i.getInsets(WindowInsets.Type.systemBars());
      v.setPadding(a.left, a.top, a.right, a.bottom);
      return i;
    });
    ImageView icon = new ImageView(this);
    icon.setImageResource(
        getResources().getIdentifier("launcher_foreground", "drawable", getPackageName()));
    col.addView(icon, new LinearLayout.LayoutParams(dp(72), dp(72)));
    label(col, "Gesture Lab", 30, 0xffe4efef);
    label(col, "PHONE LINK", 12, 0xff83e1ce);
    state = label(col, "Preparing…", 23, 0xffe4efef);
    detail = label(col, "", 16, 0xffa2b7bd);
    watch = label(col, "", 14, 0xff7f999f);
    toggle = button(col, "Enable controls", true, this::toggle);
    button(col, "Choose watch", false, this::choose);
    label(col, "Pinch → next reel", 18, 0xffd2e2e4);
    label(col, "Turn away and back → previous", 16, 0xffa2b7bd);
    label(
        col,
        "Open Reels first, then Gesture Lab on your watch. Keep the phone unlocked while"
            + " scrolling.",
        15,
        0xff91a9b0);
    label(
        col,
        "No always-ready background mode. The helper turns off when the watch disconnects, or if no"
            + " watch connects within one minute.",
        14,
        0xff708b93);
    render();
  }

  private void render() {
    SwipeService s = SwipeService.instance;
    LinkReceiver r = s == null ? null : s.receiver;
    boolean ready = r != null && r.listening;
    String title =
        r != null && r.connected ? "Connected" : ready ? "Ready" : s == null ? "Off" : "Paused";
    String message =
        s == null
            ? "Start a temporary session only when you want watch control."
            : ready
                ? "Temporary session ready. Leaving the watch app ends it."
                : "Enable controls when you want to use your watch.";
    String value = title + message + getSharedPreferences("bridge", 0).getString("name", "");
    if (!value.equals(shown)) {
      shown = value;
      state.setText(title);
      detail.setText(message);
      watch.setText(
          "Watch: " + getSharedPreferences("bridge", 0).getString("name", "choose below"));
      toggle.setText(ready ? "End session" : "Start session");
    }
  }

  private boolean permitted() {
    return checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)
        == PackageManager.PERMISSION_GRANTED;
  }

  private void permissions() {
    ArrayList<String> p = new ArrayList<>();
    p.add(Manifest.permission.BLUETOOTH_CONNECT);
    if (Build.VERSION.SDK_INT >= 33) p.add(Manifest.permission.POST_NOTIFICATIONS);
    requestPermissions(p.toArray(new String[0]), 1);
  }

  private void toggle() {
    if (!permitted()) {
      permissions();
      return;
    }
    SwipeService s = SwipeService.instance;
    if (s == null) {
      startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
      return;
    }
    boolean ready = s.receiver != null && s.receiver.listening;
    s.setReady(!ready);
    shown = "";
    render();
  }

  private void choose() {
    if (!permitted()) {
      permissions();
      return;
    }
    BluetoothAdapter a = getSystemService(BluetoothManager.class).getAdapter();
    if (a == null || !a.isEnabled()) {
      detail.setText("Turn on Bluetooth first.");
      return;
    }
    ArrayList<BluetoothDevice> ds = new ArrayList<>(a.getBondedDevices());
    String[] names = new String[ds.size()];
    for (int n = 0; n < names.length; n++) names[n] = ds.get(n).getName();
    new AlertDialog.Builder(this)
        .setTitle("Your paired watch")
        .setItems(
            names,
            (d, n) -> {
              getSharedPreferences("bridge", 0)
                  .edit()
                  .putString("watch", ds.get(n).getAddress())
                  .putString("name", names[n])
                  .putBoolean("autoReady", false)
                  .apply();
              SwipeService s = SwipeService.instance;
              if (s != null && s.receiver != null) {
                s.receiver.stop();
                s.ensureReady();
              }
              shown = "";
              render();
            })
        .show();
  }

  @Override
  protected void onResume() {
    super.onResume();
    visible = true;
    if (SwipeService.instance != null) SwipeService.instance.ensureReady();
    main.post(refresh);
  }

  @Override
  protected void onPause() {
    visible = false;
    main.removeCallbacks(refresh);
    super.onPause();
  }

  private int dp(int n) {
    return Math.round(n * getResources().getDisplayMetrics().density);
  }

  private TextView label(LinearLayout c, String text, int size, int color) {
    TextView t = new TextView(this);
    t.setText(text);
    t.setTextSize(size);
    t.setTextColor(color);
    t.setPadding(0, dp(8), 0, dp(8));
    c.addView(t, new LinearLayout.LayoutParams(-1, -2));
    return t;
  }

  private Button button(LinearLayout c, String text, boolean filled, Runnable action) {
    Button b = new Button(this);
    b.setText(text);
    b.setAllCaps(false);
    b.setTextSize(16);
    b.setTextColor(filled ? 0xff08252b : 0xffc5d8db);
    GradientDrawable bg = new GradientDrawable();
    bg.setColor(filled ? 0xff83e1ce : 0xff0c1b24);
    bg.setCornerRadius(dp(16));
    if (!filled) bg.setStroke(dp(1), 0xff2b4c53);
    b.setBackgroundTintList(null);
    b.setBackground(bg);
    b.setOnClickListener(v -> action.run());
    LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, dp(56));
    p.topMargin = dp(10);
    p.bottomMargin = dp(8);
    c.addView(b, p);
    return b;
  }
}

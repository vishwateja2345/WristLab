package studio.teja.airbridge;
import android.Manifest;import android.app.*;import android.bluetooth.*;import android.content.*;import android.content.pm.PackageManager;import android.graphics.Color;import android.os.*;import android.provider.Settings;import android.view.*;import android.widget.*;import java.util.*;

public final class MainActivity extends Activity {
 private TextView state,watch;private final Handler handler=new Handler(Looper.getMainLooper());private boolean visible;
 private final Runnable refresh=new Runnable(){public void run(){if(!visible)return;state.setText(BridgeService.status+"\nProfile: "+ControlContext.mode()+"\nSwipe control: "+(SwipeService.instance!=null?"enabled":"needs setup"));handler.postDelayed(this,1000);}};
 @Override public void onCreate(Bundle b){super.onCreate(b);getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);getWindow().setStatusBarColor(Color.rgb(6,11,16));getWindow().setNavigationBarColor(Color.rgb(6,11,16));
  ScrollView sc=new ScrollView(this);sc.setBackgroundColor(Color.rgb(6,11,16));LinearLayout col=new LinearLayout(this);col.setOrientation(1);col.setPadding(28,35,28,40);sc.addView(col);setContentView(sc);
  sc.setOnApplyWindowInsetsListener((v,i)->{android.graphics.Insets in=i.getInsets(WindowInsets.Type.systemBars());v.setPadding(in.left,in.top,in.right,in.bottom);return i;});
  label(col,"GESTURE LAB",30);label(col,"Air controls for your phone.",18);state=label(col,"Session stopped",16);watch=label(col,"",15);showWatch();
  button(col,"1 · Choose your watch",this::chooseWatch);button(col,"2 · Enable swipe control",()->startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
  button(col,"3 · Start session",this::start);button(col,"Stop session",()->{stopService(new Intent(this,BridgeService.class));state.setText("Session stopped");});
  button(col,"Calibrate Instagram comments button",()->{SwipeService service=SwipeService.instance;if(service==null){state.setText("Enable swipe control first");return;}Intent launch=getPackageManager().getLaunchIntentForPackage("com.instagram.android");if(launch==null){state.setText("Instagram is unavailable");return;}startActivity(launch);handler.postDelayed(()->OverlayPicker.show(service),800);});
  button(col,"Control profile",()->new AlertDialog.Builder(this).setTitle("Control profile").setItems(new String[]{"Auto by active app","Reels","Reading","Media","Navigation"},(d,w)->{ControlContext.selected=new String[]{"AUTO","REELS","READING","MEDIA","NAV"}[w];ControlContext.comments=false;getSharedPreferences("bridge",0).edit().putString("profile",ControlContext.selected).apply();BridgeService.publishState();}).show());
  label(col,"Open Gesture Lab on your watch, enable Phone controls in Options, choose this phone, and tap Start. The profile changes what each gesture does. Twist to use the context action or open Air Shortcuts.",16);
  label(col,"Only your selected paired watch can send swipes. Reads only the active app package name. No screen-content reading, internet, microphone, or health access. The session has a Stop notification and ends after 15 minutes idle.",14);
 }
 @Override public void dump(String prefix,java.io.FileDescriptor fd,java.io.PrintWriter w,String[] args){super.dump(prefix,fd,w,args);w.println("AirBridgeState running="+BridgeService.running+" completed="+BridgeService.completed+" accessibility="+(SwipeService.instance!=null)+" watchChosen="+getSharedPreferences("bridge",MODE_PRIVATE).contains("watch")+" status="+BridgeService.status);}
 private boolean permitted(){return checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)==PackageManager.PERMISSION_GRANTED;}
 private void permission(){ArrayList<String> p=new ArrayList<>();p.add(Manifest.permission.BLUETOOTH_CONNECT);if(Build.VERSION.SDK_INT>=33)p.add(Manifest.permission.POST_NOTIFICATIONS);requestPermissions(p.toArray(new String[0]),1);}
 private void chooseWatch(){if(!permitted()){permission();return;}BluetoothAdapter a=getSystemService(BluetoothManager.class).getAdapter();if(a==null||!a.isEnabled()){state.setText("Turn on Bluetooth, then choose your watch");return;}
  ArrayList<BluetoothDevice> devices=new ArrayList<>(a.getBondedDevices());String[] names=new String[devices.size()];for(int n=0;n<devices.size();n++)names[n]=devices.get(n).getName();
  new AlertDialog.Builder(this).setTitle("Your paired watch").setItems(names,(d,n)->{stopService(new Intent(this,BridgeService.class));getSharedPreferences("bridge",MODE_PRIVATE).edit().putString("watch",devices.get(n).getAddress()).putString("name",names[n]).apply();showWatch();}).show();
 }
 private void showWatch(){watch.setText("Watch: "+getSharedPreferences("bridge",MODE_PRIVATE).getString("name","choose below"));}
 private void start(){if(!permitted()){permission();return;}if(SwipeService.instance==null){state.setText("Enable Gesture Lab swipe control in Accessibility first");return;}if(getSharedPreferences("bridge",MODE_PRIVATE).getString("watch",null)==null){chooseWatch();return;}
  try{startForegroundService(new Intent(this,BridgeService.class));}catch(Exception e){state.setText("Could not start · check Bluetooth and try again");}}
 @Override protected void onResume(){super.onResume();visible=true;handler.post(refresh);}
 @Override protected void onPause(){visible=false;handler.removeCallbacks(refresh);super.onPause();}
 private TextView label(LinearLayout c,String s,int size){TextView t=new TextView(this);t.setText(s);t.setTextColor(Color.rgb(213,230,236));t.setTextSize(size);t.setPadding(0,16,0,16);c.addView(t,new LinearLayout.LayoutParams(-1,-2));return t;}
 private void button(LinearLayout c,String s,Runnable r){Button b=new Button(this);b.setText(s);b.setAllCaps(false);b.setOnClickListener(v->r.run());c.addView(b,new LinearLayout.LayoutParams(-1,Math.round(60*getResources().getDisplayMetrics().density)));}
}

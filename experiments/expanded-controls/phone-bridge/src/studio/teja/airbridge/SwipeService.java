package studio.teja.airbridge;
import android.accessibilityservice.*;import android.view.accessibility.AccessibilityEvent;import android.app.KeyguardManager;import android.graphics.*;import android.view.*;import android.os.*;import android.content.*;import android.media.AudioManager;

public final class SwipeService extends AccessibilityService {
 public interface Reply{void result(String result);}
 public static volatile SwipeService instance;
 @Override protected void onServiceConnected(){instance=this;ControlContext.selected=getSharedPreferences("bridge",0).getString("profile","AUTO");}
 @Override public void onAccessibilityEvent(AccessibilityEvent event){CharSequence p=event.getPackageName();if(p==null)return;String name=p.toString();if((name.equals(getPackageName())&&OverlayPicker.showing)||name.contains("inputmethod"))return;if(!name.equals(ControlContext.activePackage)){ControlContext.activePackage=name;if(!name.equals("com.instagram.android"))ControlContext.comments=false;BridgeService.publishState();}}
 @Override public void onInterrupt(){}
 @Override public void onDestroy(){if(instance==this)instance=null;super.onDestroy();}
 public void action(String requested,java.util.function.BooleanSupplier sessionActive,Reply reply){
  final long received=SystemClock.elapsedRealtime();new Handler(Looper.getMainLooper()).post(()->{
   if(instance!=this||!sessionActive.getAsBoolean()||SystemClock.elapsedRealtime()-received>500){reply.result("OFF");return;}
   String mode=ControlContext.mode(),command=ControlContext.resolve(requested,mode);
   KeyguardManager k=getSystemService(KeyguardManager.class);boolean media=command.startsWith("MEDIA_")||command.startsWith("VOLUME_");if(!media&&k!=null&&k.isKeyguardLocked()){reply.result("LOCKED");return;}
   try{
    if(command.startsWith("PROFILE_")){String profile=command.substring(8);if(!profile.equals("AUTO")&&!profile.equals("REELS")&&!profile.equals("READING")&&!profile.equals("MEDIA")&&!profile.equals("NAV")){reply.result("REJECTED");return;}ControlContext.selected=profile;ControlContext.comments=false;getSharedPreferences("bridge",0).edit().putString("profile",profile).apply();BridgeService.publishState();reply.result("DONE");return;}
    if(command.equals("VIDEO_RESET")){ControlContext.comments=false;BridgeService.publishState();reply.result("DONE");return;}
    if(media){AudioManager audio=getSystemService(AudioManager.class);if(command.startsWith("VOLUME_")){audio.adjustStreamVolume(AudioManager.STREAM_MUSIC,command.equals("VOLUME_UP")?AudioManager.ADJUST_RAISE:AudioManager.ADJUST_LOWER,AudioManager.FLAG_SHOW_UI);}else{int key=command.equals("MEDIA_NEXT")?KeyEvent.KEYCODE_MEDIA_NEXT:command.equals("MEDIA_PREV")?KeyEvent.KEYCODE_MEDIA_PREVIOUS:KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE;audio.dispatchMediaKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN,key));audio.dispatchMediaKeyEvent(new KeyEvent(KeyEvent.ACTION_UP,key));}reply.result("DONE");return;}
    if(command.startsWith("LAUNCH_")){String pkg=command.equals("LAUNCH_INSTAGRAM")?"com.instagram.android":command.equals("LAUNCH_YOUTUBE")?"com.google.android.youtube":command.equals("LAUNCH_BROWSER")?"com.brave.browser":command.equals("LAUNCH_MAPS")?"com.google.android.apps.maps":null;Intent launch=pkg==null?null:getPackageManager().getLaunchIntentForPackage(pkg);if(launch==null){reply.result("UNAVAILABLE");return;}startActivity(launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));reply.result("DONE");return;}
    int global=command.equals("BACK")?GLOBAL_ACTION_BACK:command.equals("HOME")?GLOBAL_ACTION_HOME:command.equals("RECENTS")?GLOBAL_ACTION_RECENTS:command.equals("NOTIFICATIONS")?GLOBAL_ACTION_NOTIFICATIONS:command.equals("QUICK_SETTINGS")?GLOBAL_ACTION_QUICK_SETTINGS:command.equals("SCREENSHOT")?GLOBAL_ACTION_TAKE_SCREENSHOT:0;
    if(global!=0){boolean ok=performGlobalAction(global);if(ok&&command.equals("BACK"))ControlContext.comments=false;BridgeService.publishState();reply.result(ok?"DONE":"REJECTED");return;}
    Rect bounds=getSystemService(WindowManager.class).getMaximumWindowMetrics().getBounds();int direction=command.equals("PREV")||command.equals("SCROLL_UP")?-1:1;
    if(command.equals("COMMENTS")){
     if(!ControlContext.activePackage.equals("com.instagram.android")){reply.result("OPEN_INSTAGRAM");return;}
     if(ControlContext.comments){boolean ok=performGlobalAction(GLOBAL_ACTION_BACK);if(ok)ControlContext.comments=false;BridgeService.publishState();reply.result(ok?"DONE":"REJECTED");return;}
     android.content.SharedPreferences prefs=getSharedPreferences("bridge",0);if(!prefs.getBoolean("commentsCalibrated",false)){reply.result("CALIBRATE_COMMENTS");return;}if(bounds.width()>bounds.height()){reply.result("USE_PORTRAIT");return;}
     float x=bounds.left+bounds.width()*prefs.getFloat("commentX",.9f),y=bounds.top+bounds.height()*prefs.getFloat("commentY",.5f);Path tap=new Path();tap.moveTo(x,y);dispatch(tap,65,result->{if(result.equals("DONE")){ControlContext.comments=true;ControlContext.selected="REELS";}BridgeService.publishState();reply.result(result);});return;
    }
    if(!command.equals("NEXT")&&!command.equals("PREV")&&!command.equals("SCROLL_DOWN")&&!command.equals("SCROLL_UP")){reply.result("REJECTED");return;}
    SwipePlan plan=new SwipePlan(bounds.width(),bounds.height(),direction,mode,command.startsWith("SCROLL_"));Path path=new Path();path.moveTo(bounds.left+plan.x,bounds.top+plan.startY);path.lineTo(bounds.left+plan.x,bounds.top+plan.endY);dispatch(path,plan.durationMs,reply);
   }catch(Exception e){reply.result("REJECTED");}
  });
 }
 @Override protected void dump(java.io.FileDescriptor fd,java.io.PrintWriter w,String[] args){android.content.SharedPreferences p=getSharedPreferences("bridge",0);w.println("AirControlState mode="+ControlContext.mode()+" selected="+ControlContext.selected+" activePackage="+ControlContext.activePackage+" comments="+ControlContext.comments+" calibrated="+p.getBoolean("commentsCalibrated",false)+" commentX="+p.getFloat("commentX",0)+" commentY="+p.getFloat("commentY",0)+" overlay="+OverlayPicker.showing);}
 private void dispatch(Path path,int duration,Reply reply){GestureDescription gesture=new GestureDescription.Builder().addStroke(new GestureDescription.StrokeDescription(path,0,duration)).build();boolean accepted=dispatchGesture(gesture,new GestureResultCallback(){@Override public void onCompleted(GestureDescription g){reply.result("DONE");}@Override public void onCancelled(GestureDescription g){reply.result("CANCELLED");}},null);if(!accepted)reply.result("REJECTED");}
}

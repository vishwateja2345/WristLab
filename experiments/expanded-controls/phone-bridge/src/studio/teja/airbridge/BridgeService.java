package studio.teja.airbridge;
import android.app.*;import android.bluetooth.*;import android.content.*;import android.content.pm.ServiceInfo;import android.os.*;
import java.io.*;import java.nio.charset.StandardCharsets;import java.util.UUID;import java.util.concurrent.*;

public final class BridgeService extends Service {
 public static final UUID UUID_SERVICE=UUID.fromString("2fb8f7c0-a624-4bc1-a9f4-e21e12d349b7");
 private static volatile BridgeService live;private volatile OutputStream peerOutput;public static void publishState(){BridgeService b=live;if(b!=null&&b.peerOutput!=null)b.line(b.peerOutput,"STATE "+ControlContext.mode());}
 public static volatile boolean running;public static volatile String status="Session stopped";public static volatile int completed;
 private volatile boolean stopped;private volatile BluetoothServerSocket server;private volatile BluetoothSocket client;
 private final Handler main=new Handler(Looper.getMainLooper());private final ExecutorService replies=Executors.newSingleThreadExecutor();
 private volatile long lastActivity;private Thread thread;private String allowed;
 private final Runnable idle=new Runnable(){public void run(){if(stopped)return;if(SystemClock.elapsedRealtime()-lastActivity>15*60_000){status="Stopped after 15 minutes idle";stopSelf();return;}main.postDelayed(this,30_000);}};
 @Override public void onCreate(){super.onCreate();live=this;NotificationManager nm=getSystemService(NotificationManager.class);nm.createNotificationChannel(new NotificationChannel("air_swipe","Watch swipe session",NotificationManager.IMPORTANCE_LOW));}
 private Notification notification(String text){
  PendingIntent open=PendingIntent.getActivity(this,0,new Intent(this,MainActivity.class),PendingIntent.FLAG_IMMUTABLE);
  PendingIntent stop=PendingIntent.getService(this,1,new Intent(this,BridgeService.class).setAction("STOP"),PendingIntent.FLAG_IMMUTABLE);
  return new Notification.Builder(this,"air_swipe").setSmallIcon(android.R.drawable.ic_media_play).setContentTitle("Watch swipe control").setContentText(text).setContentIntent(open).setOngoing(true).addAction(new Notification.Action.Builder(null,"Stop",stop).build()).build();
 }
 private void state(String text){status=text;if(!stopped)getSystemService(NotificationManager.class).notify(1,notification(text));}
 @Override public int onStartCommand(Intent intent,int flags,int startId){
  if(intent!=null&&"STOP".equals(intent.getAction())){stopSelf();return START_NOT_STICKY;}
  if(running)return START_NOT_STICKY;
  allowed=getSharedPreferences("bridge",MODE_PRIVATE).getString("watch",null);
  if(allowed==null||SwipeService.instance==null){status="Choose watch and enable swipe control first";stopSelf();return START_NOT_STICKY;}
  stopped=false;running=true;lastActivity=SystemClock.elapsedRealtime();
  startForeground(1,notification("Waiting for your watch"),ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE);
  thread=new Thread(this::serve,"watch-swipe-bridge");thread.start();main.postDelayed(idle,30_000);return START_NOT_STICKY;
 }
 private void serve(){
  try {
   BluetoothAdapter adapter=getSystemService(BluetoothManager.class).getAdapter();
   server=adapter.listenUsingRfcommWithServiceRecord("Gesture Lab Swipe Bridge",UUID_SERVICE);
   state("Waiting for your watch");
   while(!stopped){
    BluetoothSocket accepted=server.accept();client=accepted;
    BluetoothDevice peer=accepted.getRemoteDevice();
    if(peer.getBondState()!=BluetoothDevice.BOND_BONDED||!allowed.equals(peer.getAddress())){accepted.close();continue;}
    try {
     InputStream input=accepted.getInputStream();OutputStream output=accepted.getOutputStream();
     if(!"GLAB/1".equals(readLine(input))){accepted.close();continue;}
     write(output,"READY");peerOutput=output;publishState();lastActivity=SystemClock.elapsedRealtime();state("Connected · one gesture, one swipe");
     SwipeGate gate=new SwipeGate();String line;
     while(!stopped&&(line=readLine(input))!=null){
      String[] parts=line.split(" ");if(parts.length!=2||!ControlContext.allowed(parts[0]))continue;
      long id;try{id=Long.parseLong(parts[1]);}catch(NumberFormatException bad){continue;}
      String decision=gate.request(id,SystemClock.elapsedRealtime());
      if(!"ACCEPT".equals(decision)){ack(output,decision,id);continue;}
      SwipeService swipe=SwipeService.instance;
      if(swipe==null){gate.complete();ack(output,"OFF",id);continue;}
      lastActivity=SystemClock.elapsedRealtime();
      swipe.action(parts[0],()->!stopped&&client==accepted&&accepted.isConnected(),result->{gate.complete();if("DONE".equals(result))completed++;ack(output,result,id);state("Connected · completed actions: "+completed);});
     }
    }catch(IOException ignored){}finally{try{accepted.close();}catch(IOException ignored){}if(client==accepted){client=null;peerOutput=null;}}
    if(!stopped)state("Waiting for your watch");
   }
  }catch(Exception e){if(!stopped)status="Bluetooth session stopped · reopen to retry";}
  finally{if(!stopped)stopSelf();}
 }
 private void line(OutputStream out,String message){if(!stopped)try{replies.execute(()->{try{write(out,message);}catch(IOException ignored){}});}catch(RejectedExecutionException ignored){}}
 private void ack(OutputStream out,String result,long id){if(!stopped)try{replies.execute(()->{try{write(out,result+" "+id);}catch(IOException ignored){}});}catch(RejectedExecutionException ignored){}}
 private static synchronized void write(OutputStream out,String s)throws IOException{out.write((s+"\n").getBytes(StandardCharsets.US_ASCII));out.flush();}
 private static String readLine(InputStream in)throws IOException{StringBuilder s=new StringBuilder();for(int i=0;i<64;i++){int b=in.read();if(b<0)return null;if(b=='\n')return s.toString();if(b<32||b>126)throw new IOException("Invalid command");s.append((char)b);}throw new IOException("Command too long");}
 @Override public void onDestroy(){if(live==this)live=null;peerOutput=null;stopped=true;running=false;main.removeCallbacks(idle);try{if(client!=null)client.close();}catch(IOException ignored){}try{if(server!=null)server.close();}catch(IOException ignored){}replies.shutdownNow();stopForeground(STOP_FOREGROUND_REMOVE);if(!status.startsWith("Stopped after"))status="Session stopped";super.onDestroy();}
 @Override protected void dump(java.io.FileDescriptor fd,java.io.PrintWriter w,String[] args){w.println("AirBridgeState running="+running+" listening="+(server!=null)+" client="+(client!=null&&client.isConnected())+" mode="+ControlContext.mode()+" completed="+completed+" accessibility="+(SwipeService.instance!=null)+" status="+status);}
 @Override public IBinder onBind(Intent i){return null;}
}

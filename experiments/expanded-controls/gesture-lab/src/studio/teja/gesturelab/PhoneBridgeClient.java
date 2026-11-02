package studio.teja.gesturelab;
import android.bluetooth.*;import android.content.Context;import android.os.*;
import java.io.*;import java.nio.charset.StandardCharsets;import java.util.UUID;import java.util.concurrent.*;

/** Paired Bluetooth link for acknowledged single touch swipes. Never retries commands. */
public final class PhoneBridgeClient {
 public interface Listener{void state(String text);void completed(String result);void contextChanged(String profile);}
 private static final UUID SERVICE=UUID.fromString("2fb8f7c0-a624-4bc1-a9f4-e21e12d349b7");
 private final BluetoothAdapter adapter;private final Listener listener;private final Handler main=new Handler(Looper.getMainLooper());
 private final ExecutorService writes=Executors.newSingleThreadExecutor();private volatile BluetoothSocket socket;private volatile OutputStream output;
 public volatile boolean connected,pending;public volatile String lastResult="",profile="READING";private volatile int generation;private long sequence,pendingId,lastSend;
 public PhoneBridgeClient(Context context,Listener l){adapter=context.getSystemService(BluetoothManager.class).getAdapter();listener=l;}
 private void state(String text,int gen){main.post(()->{if(gen==generation)listener.state(text);});}
 public void connect(String address){
  close();final int gen=generation;
  if(address==null){state("Choose your phone first",gen);return;}
  state("Connecting to phone controls…",gen);
  new Thread(()->{
   BluetoothSocket local=null;
   try {
    BluetoothDevice phone=adapter.getRemoteDevice(address);
    if(phone.getBondState()!=BluetoothDevice.BOND_BONDED)throw new IOException("Phone not paired");
    local=phone.createRfcommSocketToServiceRecord(SERVICE);if(gen!=generation){local.close();return;}socket=local;
    final BluetoothSocket timed=local;main.postDelayed(()->{if(gen==generation&&!connected)try{timed.close();}catch(IOException ignored){}},10_000);
    local.connect();if(gen!=generation){local.close();return;}
    output=local.getOutputStream();output.write("GLAB/1\n".getBytes(StandardCharsets.US_ASCII));output.flush();
    InputStream input=local.getInputStream();if(!"READY".equals(readLine(input)))throw new IOException("Handshake failed");
    connected=true;state("Connected · phone controls",gen);
    String line;while(gen==generation&&(line=readLine(input))!=null){
     String[] p=line.split(" ");if(p.length!=2)continue;if(p[0].equals("STATE")){final String mode=p[1];main.post(()->{if(gen==generation){profile=mode;listener.contextChanged(mode);}});continue;}long id;try{id=Long.parseLong(p[1]);}catch(NumberFormatException e){continue;}
     final String result=p[0];main.post(()->{if(gen==generation&&pending&&id==pendingId){pending=false;lastResult=result;listener.completed(result);}});
    }
   }catch(Exception ignored){}finally{
    if(local!=null)try{local.close();}catch(IOException ignored){}
    if(gen==generation){connected=false;pending=false;output=null;state("Phone bridge disconnected · start phone session",gen);}
   }
  },"phone-swipe-link").start();
 }
 public boolean swipe(int direction){return command(direction>0?"NEXT":"PREV");}
 public boolean command(String action){
  long now=SystemClock.elapsedRealtime();
  if(!connected||pending||now-lastSend<900)return false;
  final int gen=generation;final long id=++sequence;pendingId=id;pending=true;lastSend=now;
  writes.execute(()->{try{OutputStream out=output;if(gen!=generation||out==null)return;out.write((action+" "+id+"\n").getBytes(StandardCharsets.US_ASCII));out.flush();}catch(IOException e){main.post(()->{if(gen==generation){close();listener.state("Phone bridge disconnected");}});}});
  main.postDelayed(()->{if(gen==generation&&pending&&pendingId==id){pending=false;lastResult="TIMEOUT";listener.completed("TIMEOUT");}},2000);
  return true;
 }
 public void close(){generation++;connected=pending=false;output=null;BluetoothSocket old=socket;socket=null;if(old!=null)try{old.close();}catch(IOException ignored){}}
 public void destroy(){close();writes.shutdownNow();}
 private static String readLine(InputStream in)throws IOException{StringBuilder s=new StringBuilder();for(int i=0;i<64;i++){int b=in.read();if(b<0)return null;if(b=='\n')return s.toString();if(b<32||b>126)throw new IOException("Invalid reply");s.append((char)b);}throw new IOException("Reply too long");}
}

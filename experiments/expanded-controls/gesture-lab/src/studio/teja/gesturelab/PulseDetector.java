package studio.teja.gesturelab;

/** Experimental double/triple inertial pulses; cannot identify which finger moved. */
public final class PulseDetector {
 public double threshold=300;public double turn;
 private long previous,quietUntil,lastPulse,muteUntil;private double x,y,z,background;private int count;
 public double jerk;
 public void reset(){previous=quietUntil=lastPulse=muteUntil=0;count=0;background=0;jerk=0;}
 public void mute(long until){muteUntil=Math.max(muteUntil,until);count=0;}
 public int tick(long now){if(count>0&&now-lastPulse>650_000_000L){int n=count;count=0;return n==2?2:0;}return 0;}
 public int sample(double ax,double ay,double az,long now){
  double dt=previous==0?0:(now-previous)/1e9;previous=now;
  jerk=dt>=.005&&dt<=.08?Math.sqrt(sq(ax-x)+sq(ay-y)+sq(az-z))/dt:0;x=ax;y=ay;z=az;
  double before=background;background+=.1*(jerk-background);int result=tick(now);
  if(now<muteUntil||now<quietUntil||dt==0)return result;
  if(turn>1.0){count=0;return result;}
  if(jerk>=threshold&&before<threshold*.5){
   if(count>0&&now-lastPulse>650_000_000L)count=0;
   count++;lastPulse=now;quietUntil=now+180_000_000L;
   if(count==3){count=0;return 3;}
  }
  return result;
 }
 private static double sq(double x){return x*x;}
}

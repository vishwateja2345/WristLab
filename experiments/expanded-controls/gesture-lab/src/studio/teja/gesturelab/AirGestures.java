package studio.teja.gesturelab;
/** Two deliberate opposite Y-axis rotations switch mode; screen-down hold pauses. */
public final class AirGestures {
 private long started,until,downSince;private int direction;
 public int turn(double rate,long now){if(now<until)return 0;if(started!=0&&now-started>900_000_000L){started=0;direction=0;}if(started==0){if(Math.abs(rate)>2.5){direction=rate>0?1:-1;started=now;}return 0;}if(rate*direction < -2.0&&now-started>100_000_000L){int d=direction;started=0;direction=0;until=now+1_500_000_000L;return d;}return 0;}
 public boolean turning(){return started!=0;}
 public boolean faceDown(double z,long now){if(z> -6.5){downSince=0;return false;}if(downSince==0)downSince=now;return now-downSince>=1_000_000_000L;}
 public void reset(){started=until=downSince=0;direction=0;}
}

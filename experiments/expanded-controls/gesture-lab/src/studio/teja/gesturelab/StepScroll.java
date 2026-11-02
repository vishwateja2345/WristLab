package studio.teja.gesturelab;

/** One tilt excursion produces one scroll report. A held tilt never repeats. */
public final class StepScroll {
 private boolean ready=true;
 private long candidateSince,neutralSince,nextAllowed;
 private int candidateDirection;
 public void reset(){ready=true;candidateSince=neutralSince=nextAllowed=0;candidateDirection=0;}
 public boolean ready(){return ready;}
 public int update(double angle,long now){
  double magnitude=Math.abs(angle);
  if(magnitude<Math.toRadians(8)){
   candidateSince=0;candidateDirection=0;
   if(neutralSince==0)neutralSince=now;
   if(now-neutralSince>=200_000_000L&&now>=nextAllowed)ready=true;
   return 0;
  }
  neutralSince=0;
  if(!ready||now<nextAllowed||magnitude<Math.toRadians(18)){candidateSince=0;candidateDirection=0;return 0;}
  int direction=angle>0?1:-1;
  if(candidateSince==0||candidateDirection!=direction){candidateSince=now;candidateDirection=direction;return 0;}
  if(now-candidateSince<60_000_000L)return 0;
  ready=false;candidateSince=0;candidateDirection=0;nextAllowed=now+900_000_000L;
  return direction;
 }
}

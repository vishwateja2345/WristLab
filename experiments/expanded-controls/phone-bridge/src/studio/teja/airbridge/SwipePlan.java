package studio.teja.airbridge;
/** Complete, bounded strokes; every gesture ends with finger release. */
public final class SwipePlan {
 public final float x,startY,endY;public final int durationMs;
 public SwipePlan(int width,int height,int direction){this(width,height,direction,"REELS",false);}
 public SwipePlan(int width,int height,int direction,String mode,boolean forceShort){
  if(width<100||height<100||(direction!=1&&direction!=-1))throw new IllegalArgumentException("Invalid swipe");
  boolean shortStep=forceShort||mode.equals("COMMENTS");float start=shortStep?.73f:mode.equals("REELS")?.80f:.78f,end=shortStep?.55f:mode.equals("REELS")?.22f:.40f;
  x=width*.5f;startY=height*(direction>0?start:end);endY=height*(direction>0?end:start);durationMs=shortStep?300:260;
 }
}

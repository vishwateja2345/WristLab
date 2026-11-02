package studio.teja.airbridge;

public final class SwipePlan {
  public final float x, startY, endY;
  public final int durationMs = 260;

  public SwipePlan(int width, int height, int direction) {
    if (width < 100 || height < 100 || (direction != 1 && direction != -1))
      throw new IllegalArgumentException("Invalid swipe");
    x = width * .5f;
    startY = height * (direction > 0 ? .80f : .22f);
    endY = height * (direction > 0 ? .22f : .80f);
  }
}

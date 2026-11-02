package studio.teja.gesturelab;

/** Pure deterministic motion and report logic, independent of Android. */
public final class ControlEngine {
  public static final int POINTER = 0, SCROLL = 1, SLIDES = 2;
  public int mode = POINTER;
  public double sensitivity = 750, scrollGain = 5;
  public boolean invert = false, rightWrist = false;
  public int scrollNotches = 1;
  private final StepScroll stepScroll = new StepScroll();
  public double biasX, biasY, biasZ, neutralTilt;
  public boolean calibrated;
  private long lastTime, gestureUntil;
  private double sx, sy, rx, ry, rw;
  private double pitch;

  public void reset() {
    lastTime = 0;
    sx = sy = rx = ry = rw = 0;
    gestureUntil = 0;
    stepScroll.reset();
  }

  public boolean scrollReady() {
    return stepScroll.ready();
  }

  public void neutral(double tilt) {
    neutralTilt = tilt;
    pitch = 0;
    reset();
  }

  public void tilt(double radians) {
    pitch = wrap(radians - neutralTilt);
  }

  public static double wrap(double a) {
    return Math.atan2(Math.sin(a), Math.cos(a));
  }

  public int[] step(double gx, double gy, double gz, long nanos, boolean freeze) {
    double dt = lastTime == 0 ? 0 : Math.max(0, Math.min(.05, (nanos - lastTime) / 1e9));
    lastTime = nanos;
    if (dt == 0 || freeze) {
      rx = ry = rw = 0;
      sx = sy = 0;
      return new int[] {0, 0, 0, 0};
    }
    double yaw = -(gz - biasZ), vertical = (gx - biasX) * (rightWrist ? -1 : 1);
    double alpha = 1 - Math.exp(-dt * 2 * Math.PI * 3);
    sx += alpha * (yaw - sx);
    sy += alpha * (vertical - sy);
    int dx = 0, dy = 0, wheel = 0, page = 0;
    if (mode == POINTER) {
      rx += shape(sx) * sensitivity * dt;
      ry += shape(sy) * sensitivity * dt;
      dx = bounded(rx);
      dy = bounded(ry);
      rx -= dx;
      ry -= dy;
    }
    if (mode == SCROLL) {
      int direction = stepScroll.update(pitch, nanos);
      wheel = -direction * scrollNotches * (invert ? -1 : 1);
    }
    if (mode == SLIDES
        && nanos >= gestureUntil
        && Math.abs(gz - biasZ) > 2.2
        && Math.abs(gx - biasX) < 1.5
        && Math.abs(gy - biasY) < 1.5) {
      page = (gz - biasZ) > 0 ? -1 : 1;
      gestureUntil = nanos + 1_200_000_000L;
    }
    return new int[] {dx, dy, wheel, page};
  }

  private static double shape(double v) {
    double live = Math.max(0, Math.abs(v) - .04);
    return Math.signum(v)
        * live
        * (live < .15 ? .35 + .65 * Math.pow(live / .15, 2) : 1)
        * (1 + .3 * live);
  }

  private static int bounded(double v) {
    return (int) Math.max(-127, Math.min(127, v));
  }

  public static byte[] mouse(int buttons, int dx, int dy, int wheel) {
    return new byte[] {
      (byte) buttons,
      (byte) Math.max(-127, Math.min(127, dx)),
      (byte) Math.max(-127, Math.min(127, dy)),
      (byte) Math.max(-127, Math.min(127, wheel))
    };
  }

  public static byte[] key(int code) {
    return new byte[] {0, 0, (byte) code, 0, 0, 0, 0, 0};
  }
}

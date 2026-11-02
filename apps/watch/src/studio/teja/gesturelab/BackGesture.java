package studio.teja.gesturelab;

/** Deliberate small forearm rotation and return. Angle-gated, not a speed flick. */
public final class BackGesture {
  private long previous, started, until;
  private int direction, phase;
  private double outward, returned;

  public void reset() {
    previous = started = until = 0;
    direction = phase = 0;
    outward = returned = 0;
  }

  public void cancel(long muteUntil) {
    phase = direction = 0;
    outward = returned = 0;
    started = 0;
    until = Math.max(until, muteUntil);
  }

  public boolean inProgress() {
    return phase != 0;
  }

  public int sample(double rate, long now) {
    double dt = previous == 0 ? 0 : Math.max(0, Math.min(.05, (now - previous) / 1e9));
    previous = now;
    if (dt == 0 || now < until) return 0;
    if (phase != 0 && now - started > 1_800_000_000L) {
      phase = direction = 0;
      outward = returned = 0;
      return 0;
    }
    if (phase == 0) {
      if (Math.abs(rate) < .55) return 0;
      direction = rate > 0 ? 1 : -1;
      phase = 1;
      started = now;
      outward = Math.abs(rate) * dt;
      returned = 0;
      return 0;
    }
    double directed = rate * direction;
    if (phase == 1) {
      if (directed > .15) outward = Math.min(Math.toRadians(60), outward + directed * dt);
      else if (directed < -.2) {
        if (outward < Math.toRadians(14)) {
          phase = direction = 0;
          outward = returned = 0;
          return 0;
        }
        phase = 2;
        returned = -directed * dt;
      }
      return 0;
    }
    if (directed < -.15) returned += -directed * dt;
    if (returned >= Math.max(Math.toRadians(11), outward * .6)) {
      phase = direction = 0;
      outward = returned = 0;
      until = now + 1_200_000_000L;
      return 1;
    }
    return 0;
  }
}

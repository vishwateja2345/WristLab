package studio.teja.watchdeck;

/** Android-free state transitions so timer and step semantics can be verified on the JVM. */
public final class DeckState {
  public long sessionSteps;
  public int stepTarget = 2000;
  public int focusMinutes = 25;
  public boolean haptics = true;
  public boolean running;
  public long remaining = 25 * 60_000L;
  public long deadline;
  private long sensorBaseline = -1;

  public void beginForeground() {
    sensorBaseline = -1;
  }

  public void endForeground() {
    sensorBaseline = -1;
  }

  public void acceptStepCounter(long rawSinceBoot) {
    if (rawSinceBoot < 0) return;
    if (sensorBaseline >= 0 && rawSinceBoot >= sensorBaseline)
      sessionSteps += rawSinceBoot - sensorBaseline;
    sensorBaseline = rawSinceBoot;
  }

  public void resetSteps() {
    sessionSteps = 0;
    sensorBaseline = -1;
  }

  public long remainingAt(long elapsedNow) {
    return running ? Math.max(0, deadline - elapsedNow) : Math.max(0, remaining);
  }

  public boolean settle(long elapsedNow) {
    if (running && elapsedNow >= deadline) {
      running = false;
      remaining = 0;
      return true;
    }
    return false;
  }

  public void start(long elapsedNow) {
    if (running) return;
    if (remaining <= 0) remaining = focusMinutes * 60_000L;
    deadline = elapsedNow + remaining;
    running = true;
  }

  public void pause(long elapsedNow) {
    if (running) {
      remaining = remainingAt(elapsedNow);
      running = false;
    }
  }

  public void resetTimer() {
    running = false;
    remaining = focusMinutes * 60_000L;
    deadline = 0;
  }

  public void changeMinutes(int minutes) {
    focusMinutes = minutes;
    resetTimer();
  }

  public void deviceRebooted() {
    resetTimer();
    sensorBaseline = -1;
  }
}

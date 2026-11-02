import studio.teja.watchdeck.DeckState;

public final class DeckStateTest {
  static void eq(long a, long b, String what) {
    if (a != b) throw new AssertionError(what + ": " + a + " != " + b);
  }

  public static void main(String[] args) {
    DeckState s = new DeckState();
    s.beginForeground();
    s.acceptStepCounter(10000);
    eq(s.sessionSteps, 0, "first reading is a baseline, not daily history");
    s.acceptStepCounter(10031);
    eq(s.sessionSteps, 31, "foreground delta");
    s.endForeground();
    s.beginForeground();
    s.acceptStepCounter(10131);
    eq(s.sessionSteps, 31, "background gap must not inflate session");
    s.acceptStepCounter(10150);
    eq(s.sessionSteps, 50, "next foreground delta");
    s.acceptStepCounter(4);
    eq(s.sessionSteps, 50, "reboot/reset counter must not become negative");
    s.acceptStepCounter(8);
    eq(s.sessionSteps, 54, "new baseline after counter reset");
    s.resetSteps();
    s.acceptStepCounter(99);
    eq(s.sessionSteps, 0, "reset starts a fresh baseline");
    s.start(1000);
    eq(s.remainingAt(61000), 24 * 60000L, "monotonic running timer");
    s.pause(61000);
    eq(s.remainingAt(90000), 24 * 60000L, "paused duration is stable");
    s.start(90000);
    eq(s.remainingAt(150000), 23 * 60000L, "resume uses remaining duration");
    if (!s.settle(2_000_000) || s.running)
      throw new AssertionError("completion settles exactly once");
    if (s.settle(2_000_001)) throw new AssertionError("completion must not repeat");
    s.changeMinutes(15);
    eq(s.remaining, 15 * 60000L, "duration preference resets timer");
    s.start(1);
    s.deviceRebooted();
    eq(s.remaining, 15 * 60000L, "reboot resets elapsed deadline safely");
    System.out.println("PASS: 14 step/timer state checks");
  }
}

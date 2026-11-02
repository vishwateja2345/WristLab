import studio.teja.gesturelab.*;

public class EngineTest {
  static int count;

  static void check(boolean ok, String why) {
    count++;
    if (!ok) throw new AssertionError(why);
  }

  public static void main(String[] a) {
    byte[] b = ControlEngine.mouse(1, 300, -200, 2);
    check(b.length == 4 && b[1] == 127 && b[2] == -127, "signed/clamped mouse report");
    check(ControlEngine.key(79)[2] == 79 && ControlEngine.key(0)[2] == 0, "keyboard key + release");
    ControlEngine e = new ControlEngine();
    long t = 1_000_000_000L;
    for (int i = 0; i < 100; i++) {
      int[] o = e.step(.001, 0, .001, t + i * 20_000_000L, false);
      check(o[0] == 0 && o[1] == 0, "rest deadzone");
    }
    e.reset();
    e.step(0, 0, 0, t, false);
    int mx = 0, my = 0;
    for (int i = 1; i <= 10; i++) {
      int[] o = e.step(.4, 0, .5, t + i * 20_000_000L, false);
      mx += o[0];
      my += o[1];
    }
    check(mx < 0 && my > 0, "pointer directions over a movement");
    int[] frozen = e.step(10, 0, 10, t + 240_000_000L, true);
    check(frozen[0] == 0 && frozen[1] == 0, "touch freezes pointer");
    e = new ControlEngine();
    e.mode = 1;
    e.neutral(0);
    e.tilt(Math.toRadians(5));
    for (int i = 0; i < 100; i++)
      check(e.step(0, 0, 0, t + i * 20_000_000L, false)[2] == 0, "scroll deadzone");
    e.reset();
    e.tilt(Math.toRadians(30));
    int sum = 0, events = 0;
    for (int i = 0; i < 1000; i++) {
      int n = e.step(0, 0, 0, t + i * 20_000_000L, false)[2];
      sum += n;
      if (n != 0) events++;
    }
    check(sum == -1 && events == 1, "holding tilt 20 seconds emits exactly one forward step");
    e.tilt(0);
    for (int i = 1000; i < 1020; i++)
      check(
          e.step(0, 0, 0, t + i * 20_000_000L, false)[2] == 0, "neutral rearms without scrolling");
    e.tilt(Math.toRadians(30));
    int again = 0;
    for (int i = 1020; i < 1120; i++) again += e.step(0, 0, 0, t + i * 20_000_000L, false)[2];
    check(again == -1, "second excursion emits one further step");
    e.invert = true;
    e.reset();
    e.tilt(Math.toRadians(30));
    int reversed = 0;
    for (int i = 0; i < 100; i++) reversed += e.step(0, 0, 0, t + i * 20_000_000L, false)[2];
    check(reversed == 1, "reverse single step");
    StepScroll st = new StepScroll();
    check(st.update(.5, t) == 0, "step debounce begins");
    check(st.update(.5, t + 80_000_000L) == 1, "first excursion fires");
    check(st.update(-.5, t + 2_000_000_000L) == 0, "opposite tilt without neutral cannot repeat");
    st.update(0, t + 2_100_000_000L);
    st.update(0, t + 2_400_000_000L);
    st.update(-.5, t + 2_500_000_000L);
    check(st.update(-.5, t + 2_600_000_000L) == -1, "return to neutral permits reverse step");
    st.reset();
    st.update(.5, t);
    st.update(.5, t + 80_000_000L);
    st.update(0, t + 200_000_000L);
    st.update(0, t + 400_000_000L);
    st.update(-.5, t + 1_100_000_000L);
    check(
        st.update(-.5, t + 1_200_000_000L) == -1,
        "neutral before cooldown still rearms reverse scrolling");
    st.reset();
    st.update(.5, t);
    check(st.update(0, t + 20_000_000L) == 0, "brief spike does not fire");

    e = new ControlEngine();
    e.mode = 2;
    e.step(0, 0, 0, t, false);
    check(e.step(0, 0, -3, t + 20_000_000L, false)[3] == 1, "slide forward flick");
    check(e.step(0, 0, -3, t + 40_000_000L, false)[3] == 0, "flick cooldown");
    check(e.step(0, 0, 3, t + 1_400_000_000L, false)[3] == -1, "reverse flick");
    PulseDetector p = new PulseDetector();
    p.threshold = 200;
    p.sample(0, 0, 9.8, t);
    check(p.sample(4, 0, 9.8, t + 10_000_000L) == 0, "single pulse no action");
    p.sample(4, 0, 9.8, t + 20_000_000L);
    p.sample(4, 0, 9.8, t + 250_000_000L);
    p.sample(8, 0, 9.8, t + 260_000_000L);
    check(p.tick(t + 1_000_000_000L) == 2, "double pulse waits for triple window");
    p = new PulseDetector();
    p.threshold = 200;
    p.sample(0, 0, 9.8, t);
    p.sample(4, 0, 9.8, t + 10_000_000L);
    p.sample(4, 0, 9.8, t + 250_000_000L);
    p.sample(8, 0, 9.8, t + 260_000_000L);
    p.sample(8, 0, 9.8, t + 500_000_000L);
    check(p.sample(12, 0, 9.8, t + 510_000_000L) == 3, "triple pulse distinct action");
    p = new PulseDetector();
    p.turn = 2;
    p.threshold = 200;
    p.sample(0, 0, 9.8, t);
    p.sample(8, 0, 9.8, t + 10_000_000L);
    check(p.tick(t + 1_000_000_000L) == 0, "reject arm swing");
    p = new PulseDetector();
    p.threshold = 200;
    p.sample(0, 0, 9.8, t);
    p.mute(t + 2_000_000_000L);
    p.sample(8, 0, 9.8, t + 10_000_000L);
    check(p.tick(t + 1_000_000_000L) == 0, "haptic mute rejects spikes");
    AirGestures air = new AirGestures();
    check(air.turn(3, t) == 0, "air twist starts without firing");
    check(air.turn(-3, t + 200_000_000L) == 1, "out and back changes mode");
    check(air.turn(3, t + 300_000_000L) == 0, "air twist cooldown");
    air.reset();
    check(!air.faceDown(-9, t), "face down requires hold");
    check(air.faceDown(-9, t + 1_100_000_000L), "face down pauses after hold");
    air.reset();
    check(!air.faceDown(9, t + 2_000_000_000L), "normal face up does not pause");
    BackGesture back = new BackGesture();
    for (int i = 0; i < 100; i++)
      check(back.sample(.03, t + i * 20_000_000L) == 0, "stationary wrist does not go back");
    back.reset();
    back.sample(0, t);
    for (int i = 1; i < 4; i++)
      check(back.sample(.6, t + i * 20_000_000L) == 0, "small pinch-like movement rejected");
    for (int i = 4; i < 9; i++)
      check(back.sample(-.6, t + i * 20_000_000L) == 0, "small return does not go back");
    back.reset();
    back.sample(0, t);
    int backEvents = 0;
    for (int i = 1; i <= 18; i++) backEvents += back.sample(.9, t + i * 20_000_000L);
    for (int i = 19; i <= 36; i++) backEvents += back.sample(-.9, t + i * 20_000_000L);
    check(backEvents == 1, "gentle 18 degree rotation and return triggers once");
    for (int i = 37; i < 70; i++)
      check(back.sample(-.9, t + i * 20_000_000L) == 0, "holding after return never repeats");
    back.reset();
    back.sample(0, t);
    for (int i = 1; i <= 18; i++) back.sample(.9, t + i * 20_000_000L);
    back.cancel(t + 2_000_000_000L);
    for (int i = 19; i < 60; i++)
      check(back.sample(-.9, t + i * 20_000_000L) == 0, "pinch cancels pending wrist back");
    check(
        SessionPolicy.motion(true, true, true, true, true, 20),
        "motion allowed inside awake controller");
    check(
        !SessionPolicy.motion(true, true, false, true, true, 20),
        "home screen never runs phone gesture sensors");
    check(!SessionPolicy.motion(true, true, true, false, true, 20), "sleep stops motion sensors");
    check(!SessionPolicy.motion(true, true, true, true, false, 20), "locked watch blocks motion");
    check(!SessionPolicy.motion(true, true, true, true, true, 4), "low battery stops motion");
    check(!SessionPolicy.send(true, true, false, true), "hidden controller cannot send swipes");
    check(SessionPolicy.send(true, true, true, true), "visible standby target can send");
    System.out.println(
        count
            + " deterministic motion/report assertions passed. Human recognition accuracy is not"
            + " established.");
  }
}

import studio.teja.airbridge.*;

public class BridgeTest {
  static int n;

  static void check(boolean c, String m) {
    n++;
    if (!c) throw new AssertionError(m);
  }

  public static void main(String[] a) {
    for (int[] size : new int[][] {{1080, 2400}, {1272, 2800}, {2800, 1272}, {100, 100}}) {
      SwipePlan up = new SwipePlan(size[0], size[1], 1), down = new SwipePlan(size[0], size[1], -1);
      check(up.startY > up.endY, "next is upward swipe");
      check(down.startY < down.endY, "previous is downward swipe");
      check(up.x > 0 && up.x < size[0] && up.startY < size[1] && up.endY > 0, "within screen");
      check(up.durationMs == 260, "bounded duration");
    }
    SwipeGate g = new SwipeGate();
    check(g.request(1, 1000).equals("ACCEPT"), "first accepted");
    check(g.request(2, 2000).equals("BUSY"), "no concurrent swipe");
    g.complete();
    check(g.request(1, 3000).equals("DUPLICATE"), "no duplicate replay");
    check(g.request(2, 3000).equals("DUPLICATE"), "busy command is dropped, not queued");
    check(g.request(3, 3000).equals("ACCEPT"), "fresh command accepted");
    g.complete();
    check(g.request(4, 3100).equals("COOLDOWN"), "rapid repeats dropped");
    check(g.request(5, 4000).equals("ACCEPT"), "later deliberate gesture accepted");
    g.complete();
    check(g.request(6, 4550).equals("ACCEPT"), "completed swipe permits another after 550ms");
    boolean rejected = false;
    try {
      new SwipePlan(0, 2400, 1);
    } catch (IllegalArgumentException e) {
      rejected = true;
    }
    check(rejected, "invalid geometry rejected");
    System.out.println(n + " swipe geometry / command gate assertions passed");
  }
}

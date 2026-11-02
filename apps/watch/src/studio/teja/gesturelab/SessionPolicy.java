package studio.teja.gesturelab;

/** The watch home screen and other apps never issue phone gestures. */
public final class SessionPolicy {
  public static boolean motion(
      boolean active,
      boolean connected,
      boolean visible,
      boolean awake,
      boolean unlocked,
      int battery) {
    return active && connected && visible && awake && unlocked && battery >= 5;
  }

  public static boolean send(boolean active, boolean connected, boolean visible, boolean unlocked) {
    return active && connected && visible && unlocked;
  }
}

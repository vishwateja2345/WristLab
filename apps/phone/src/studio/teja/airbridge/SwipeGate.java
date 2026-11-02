package studio.teja.airbridge;

/** Reject duplicate, concurrent and rapid commands. Never queue or replay them. */
public final class SwipeGate {
  private long lastId, lastStart = Long.MIN_VALUE;
  private boolean busy;

  public synchronized String request(long id, long now) {
    if (id <= lastId || id <= 0) return "DUPLICATE";
    lastId = id;
    if (busy) return "BUSY";
    if (lastStart != Long.MIN_VALUE && now - lastStart < 500) return "COOLDOWN";
    busy = true;
    lastStart = now;
    return "ACCEPT";
  }

  public synchronized void complete() {
    busy = false;
  }
}

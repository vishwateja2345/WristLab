package studio.teja.airbridge;

import android.bluetooth.*;
import android.os.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.*;

/**
 * Blocking Bluetooth listener owned by the system-bound Accessibility service. No polling or
 * sensors.
 */
public final class LinkReceiver {
  static final UUID SERVICE = UUID.fromString("2fb8f7c0-a624-4bc1-a9f4-e21e12d349b7");
  private final SwipeService owner;
  private volatile int generation;
  private volatile BluetoothServerSocket server;
  private volatile BluetoothSocket client;
  private final ExecutorService writes =
      new ThreadPoolExecutor(
          1,
          1,
          0L,
          TimeUnit.MILLISECONDS,
          new ArrayBlockingQueue<Runnable>(8),
          new ThreadPoolExecutor.DiscardOldestPolicy());
  public volatile boolean listening, connected;
  public volatile String lastDirection = "";
  public volatile String status = "Not ready", lastResult = "";
  public volatile int completed;

  LinkReceiver(SwipeService service) {
    owner = service;
  }

  public synchronized void start() {
    if (listening) return;
    stop();
    final int gen = generation;
    String allowed = owner.getSharedPreferences("bridge", 0).getString("watch", null);
    if (allowed == null) {
      state("Choose your watch once");
      return;
    }
    listening = true;
    state("Ready · waiting for watch");
    new Thread(() -> serve(gen, allowed), "gesture-lab-receiver").start();
  }

  private void serve(int gen, String allowed) {
    BluetoothServerSocket local = null;
    try {
      BluetoothAdapter a = owner.getSystemService(BluetoothManager.class).getAdapter();
      if (a == null || !a.isEnabled()) throw new IOException("Bluetooth off");
      local = a.listenUsingRfcommWithServiceRecord("Gesture Lab", SERVICE);
      if (gen != generation) {
        local.close();
        return;
      }
      server = local;
      while (gen == generation) {
        BluetoothSocket socket = local.accept();
        if (gen != generation) {
          socket.close();
          return;
        }
        BluetoothDevice peer = socket.getRemoteDevice();
        if (peer.getBondState() != BluetoothDevice.BOND_BONDED
            || !allowed.equals(peer.getAddress())) {
          socket.close();
          continue;
        }
        client = socket;
        boolean established = false;
        try {
          InputStream input = socket.getInputStream();
          OutputStream output = socket.getOutputStream();
          if (!"GLAB/1".equals(readLine(input))) continue;
          write(output, "READY");
          established = true;
          connected = true;
          state("Connected · one gesture, one swipe");
          SwipeGate gate = new SwipeGate();
          String line;
          while (gen == generation && (line = readLine(input)) != null) {
            String[] parts = line.split(" ");
            if (parts.length != 2 || (!parts[0].equals("NEXT") && !parts[0].equals("PREV")))
              continue;
            long id;
            try {
              id = Long.parseLong(parts[1]);
            } catch (NumberFormatException bad) {
              continue;
            }
            String decision = gate.request(id, SystemClock.elapsedRealtime());
            if (!decision.equals("ACCEPT")) {
              ack(output, decision, id);
              continue;
            }
            lastDirection = parts[0];
            int direction = parts[0].equals("NEXT") ? 1 : -1;
            owner.swipe(
                direction,
                () -> gen == generation && client == socket && connected,
                result -> {
                  gate.complete();
                  lastResult = result;
                  if (result.equals("DONE")) completed++;
                  ack(output, result, id);
                });
          }
        } catch (IOException ignored) {
        } finally {
          try {
            socket.close();
          } catch (IOException ignored) {
          }
          if (client == socket) {
            client = null;
            connected = false;
          }
          if (established) owner.controllerGone();
        }
        if (gen == generation) state("Ready · waiting for watch");
      }
    } catch (Exception e) {
      if (gen == generation) {
        listening = connected = false;
        state("Bluetooth unavailable · turn it on and retry");
      }
    } finally {
      if (local != null)
        try {
          local.close();
        } catch (IOException ignored) {
        }
    }
  }

  private void state(String text) {
    status = text;
    new Handler(Looper.getMainLooper()).post(() -> owner.updateNotification());
  }

  private void ack(OutputStream out, String result, long id) {
    try {
      writes.execute(() -> {
        try {
          write(out, result + " " + id);
        } catch (IOException ignored) {
        }
      });
    } catch (RejectedExecutionException ignored) {
    }
  }

  private static synchronized void write(OutputStream out, String line) throws IOException {
    out.write((line + "\n").getBytes(StandardCharsets.US_ASCII));
    out.flush();
  }

  private static String readLine(InputStream in) throws IOException {
    StringBuilder s = new StringBuilder();
    for (int i = 0; i < 64; i++) {
      int b = in.read();
      if (b < 0) return null;
      if (b == '\n') return s.toString();
      if (b < 32 || b > 126) throw new IOException("Bad command");
      s.append((char) b);
    }
    throw new IOException("Command too long");
  }

  public synchronized void stop() {
    generation++;
    listening = connected = false;
    BluetoothSocket c = client;
    client = null;
    BluetoothServerSocket s = server;
    server = null;
    try {
      if (c != null) c.close();
    } catch (IOException ignored) {
    }
    try {
      if (s != null) s.close();
    } catch (IOException ignored) {
    }
    status = "Paused";
  }

  public void destroy() {
    stop();
    writes.shutdownNow();
  }
}

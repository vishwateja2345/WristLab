package studio.teja.gesturelab;

import android.bluetooth.*;
import android.content.Context;
import android.os.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.*;

/** Paired Bluetooth swipes only. No command queues or automatic resends. */
public final class PhoneBridgeClient {
  public interface Listener {
    void state(String text);

    void completed(String result);
  }

  private static final UUID SERVICE = UUID.fromString("2fb8f7c0-a624-4bc1-a9f4-e21e12d349b7");
  private final BluetoothAdapter adapter;
  private final Listener listener;
  private final Handler main = new Handler(Looper.getMainLooper());
  private final ExecutorService writes = Executors.newSingleThreadExecutor();
  private volatile BluetoothSocket socket;
  private volatile OutputStream output;
  private volatile int generation;
  public volatile boolean connected, pending;
  public volatile long lastLatencyMs;
  public volatile String lastResult = "";
  private long sequence, pendingId, lastSend;

  public PhoneBridgeClient(Context c, Listener l) {
    adapter = c.getSystemService(BluetoothManager.class).getAdapter();
    listener = l;
  }

  private void state(String s, int gen) {
    main.post(() -> {
      if (gen == generation) listener.state(s);
    });
  }

  public void connect(String address) {
    close();
    final int gen = generation;
    if (address == null) {
      state("Choose your phone once", gen);
      return;
    }
    state("Connecting to phone…", gen);
    new Thread(
            () -> {
              BluetoothSocket local = null;
              try {
                BluetoothDevice phone = adapter.getRemoteDevice(address);
                if (phone.getBondState() != BluetoothDevice.BOND_BONDED)
                  throw new IOException("Not paired");
                local = phone.createRfcommSocketToServiceRecord(SERVICE);
                if (gen != generation) {
                  local.close();
                  return;
                }
                socket = local;
                final BluetoothSocket timeout = local;
                main.postDelayed(
                    () -> {
                      if (gen == generation && !connected)
                        try {
                          timeout.close();
                        } catch (IOException ignored) {
                        }
                    },
                    10_000);
                local.connect();
                if (gen != generation) return;
                output = local.getOutputStream();
                output.write("GLAB/1\n".getBytes(StandardCharsets.US_ASCII));
                output.flush();
                InputStream in = local.getInputStream();
                if (!"READY".equals(readLine(in))) throw new IOException("Not ready");
                connected = true;
                state("Connected · " + phone.getName(), gen);
                String line;
                while (gen == generation && (line = readLine(in)) != null) {
                  String[] p = line.split(" ");
                  if (p.length != 2) continue;
                  long id;
                  try {
                    id = Long.parseLong(p[1]);
                  } catch (NumberFormatException bad) {
                    continue;
                  }
                  final String result = p[0];
                  main.post(() -> {
                    if (gen == generation && pending && id == pendingId) {
                      pending = false;
                      lastLatencyMs = SystemClock.elapsedRealtime() - lastSend;
                      lastResult = result;
                      listener.completed(result);
                    }
                  });
                }
              } catch (Exception ignored) {
              } finally {
                if (local != null)
                  try {
                    local.close();
                  } catch (IOException ignored) {
                  }
                if (gen == generation) {
                  connected = pending = false;
                  output = null;
                  state("Disconnected · reopen to reconnect", gen);
                }
              }
            },
            "phone-swipe-link")
        .start();
  }

  public boolean swipe(int direction) {
    long now = SystemClock.elapsedRealtime();
    if (!connected || pending || now - lastSend < 650) return false;
    final int gen = generation;
    final long id = ++sequence;
    pendingId = id;
    pending = true;
    lastSend = now;
    writes.execute(() -> {
      try {
        OutputStream out = output;
        if (gen != generation || out == null) return;
        out.write(
            ((direction > 0 ? "NEXT " : "PREV ") + id + "\n").getBytes(StandardCharsets.US_ASCII));
        out.flush();
      } catch (IOException e) {
        main.post(() -> {
          if (gen == generation) {
            close();
            listener.state("Disconnected · reopen to reconnect");
          }
        });
      }
    });
    main.postDelayed(
        () -> {
          if (gen == generation && pending && pendingId == id) {
            pending = false;
            lastResult = "TIMEOUT";
            listener.completed("TIMEOUT");
          }
        },
        2000);
    return true;
  }

  public void close() {
    generation++;
    connected = pending = false;
    output = null;
    BluetoothSocket old = socket;
    socket = null;
    if (old != null)
      try {
        old.close();
      } catch (IOException ignored) {
      }
  }

  public void destroy() {
    close();
    writes.shutdownNow();
  }

  private static String readLine(InputStream in) throws IOException {
    StringBuilder s = new StringBuilder();
    for (int i = 0; i < 64; i++) {
      int b = in.read();
      if (b < 0) return null;
      if (b == '\n') return s.toString();
      if (b < 32 || b > 126) throw new IOException("Bad reply");
      s.append((char) b);
    }
    throw new IOException("Reply too long");
  }
}

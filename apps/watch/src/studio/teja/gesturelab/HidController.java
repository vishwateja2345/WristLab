package studio.teja.gesturelab;

import android.bluetooth.*;
import android.content.Context;
import java.util.*;

public final class HidController {
  public interface Listener {
    void state(String text);
  }

  private final Context context;
  private final Listener listener;
  private final BluetoothAdapter adapter;
  private boolean foreground, proxyPending, registerPending;

  public void foreground(boolean value) {
    foreground = value;
  }

  private BluetoothHidDevice hid;
  private BluetoothDevice host;
  private boolean registered;
  private String selected;
  private int batteryLevel = 255;

  public void setBatteryLevel(int percentage) {
    batteryLevel = Math.round(Math.max(0, Math.min(100, percentage)) * 255f / 100f);
  }

  public boolean connected;
  public int attempted, accepted;
  public String lastError = "";
  public int protocol = 1;
  public static final byte[] DESCRIPTOR = CompatDescriptor.VALUE;

  public HidController(Context c, Listener l) {
    context = c;
    listener = l;
    BluetoothManager m = (BluetoothManager) c.getSystemService(Context.BLUETOOTH_SERVICE);
    adapter = m.getAdapter();
  }

  public void remember(String address) {
    selected = address;
  }

  public void open() {
    if (adapter == null) {
      listener.state("Bluetooth unavailable");
      return;
    }
    try {
      if (!adapter.isEnabled()) {
        listener.state("Enable Bluetooth first");
        return;
      }
      if (hid != null) {
        register();
        return;
      }
      if (proxyPending) return;
      proxyPending = true;
      adapter.getProfileProxy(
          context,
          new BluetoothProfile.ServiceListener() {
            public void onServiceConnected(int p, BluetoothProfile proxy) {
              proxyPending = false;
              hid = (BluetoothHidDevice) proxy;
              register();
            }

            public void onServiceDisconnected(int p) {
              proxyPending = registerPending = false;
              hid = null;
              registered = false;
              connected = false;
              listener.state("Bluetooth service disconnected");
            }
          },
          BluetoothProfile.HID_DEVICE);
    } catch (SecurityException e) {
      listener.state("Allow Bluetooth access");
    }
  }

  private void register() {
    if (hid == null || registered || registerPending || !foreground) return;
    try {
      boolean ok =
          hid.registerApp(
              new BluetoothHidDeviceAppSdpSettings(
                  "Gesture Lab",
                  "Wrist pointer, scroll and slides",
                  "Teja",
                  BluetoothHidDevice.SUBCLASS1_COMBO,
                  DESCRIPTOR),
              null,
              null,
              context.getMainExecutor(),
              new BluetoothHidDevice.Callback() {
                @Override
                public void onAppStatusChanged(BluetoothDevice plugged, boolean r) {
                  registerPending = false;
                  registered = r;
                  android.util.Log.i(
                      "GestureLab", "HID registered=" + r + " plugged=" + (plugged != null));
                  if (r) {
                    if (selected != null
                        && (plugged == null || !selected.equals(plugged.getAddress()))) {
                      if (plugged != null) hid.disconnect(plugged);
                      host = null;
                      connected = false;
                      connect(selected);
                    } else if (plugged != null) {
                      host = plugged;
                      connected =
                          hid.getConnectionState(plugged) == BluetoothProfile.STATE_CONNECTED;
                      android.util.Log.i(
                          "GestureLab", "Plugged actual state=" + hid.getConnectionState(plugged));
                      listener.state(
                          connected ? "Connected · " + safeName(plugged) : "Connecting…");
                      if (!connected) connect(plugged.getAddress());
                    } else {
                      listener.state("Ready · choose your computer");
                      if (selected != null) connect(selected);
                    }
                  } else {
                    connected = false;
                    listener.state("Paused · reopen to reconnect");
                    if (foreground)
                      new android.os.Handler(android.os.Looper.getMainLooper())
                          .postDelayed(
                              () -> {
                                if (foreground) register();
                              },
                              350);
                  }
                }

                @Override
                public void onGetReport(BluetoothDevice d, byte type, byte id, int size) {
                  if (type != BluetoothHidDevice.REPORT_TYPE_INPUT) {
                    hid.reportError(d, BluetoothHidDevice.ERROR_RSP_UNSUPPORTED_REQ);
                    return;
                  }
                  byte[] data =
                      id == 1
                          ? ControlEngine.key(0)
                          : id == 2
                              ? ControlEngine.mouse(0, 0, 0, 0)
                              : id == 32 ? new byte[] {(byte) batteryLevel} : null;
                  if (data == null) hid.reportError(d, BluetoothHidDevice.ERROR_RSP_INVALID_RPT_ID);
                  else hid.replyReport(d, type, id, data);
                  android.util.Log.i(
                      "GestureLab",
                      "Host GET_REPORT id=" + id + " length=" + (data == null ? 0 : data.length));
                }

                @Override
                public void onSetReport(BluetoothDevice d, byte type, byte id, byte[] data) {
                  hid.reportError(d, BluetoothHidDevice.ERROR_RSP_SUCCESS);
                }

                @Override
                public void onSetProtocol(BluetoothDevice d, byte p) {
                  protocol = p;
                  android.util.Log.i("GestureLab", "HID protocol=" + p);
                }

                @Override
                public void onConnectionStateChanged(BluetoothDevice d, int state) {
                  android.util.Log.i("GestureLab", "HID connection state=" + state);
                  if (host != null && !host.equals(d)) {
                    if (state == BluetoothProfile.STATE_CONNECTED) hid.disconnect(d);
                    return;
                  }
                  host = d;
                  connected = state == BluetoothProfile.STATE_CONNECTED;
                  listener.state(
                      connected
                          ? "Connected · " + safeName(d)
                          : state == BluetoothProfile.STATE_CONNECTING
                              ? "Connecting…"
                              : "Disconnected · tap Connect");
                }
              });
      registerPending = ok;
      if (!ok) listener.state("Mouse profile busy · close WearMouse");
    } catch (SecurityException e) {
      listener.state("Bluetooth permission needed");
    }
  }

  private String safeName(BluetoothDevice d) {
    try {
      String n = d.getName();
      return n == null ? "Computer" : n;
    } catch (SecurityException e) {
      return "Computer";
    }
  }

  public List<BluetoothDevice> bonded() {
    try {
      return adapter == null ? new ArrayList<>() : new ArrayList<>(adapter.getBondedDevices());
    } catch (SecurityException e) {
      return new ArrayList<>();
    }
  }

  public void connect(String address) {
    selected = address;
    listener.state("Connecting…");
    if (hid == null || !registered) {
      open();
      return;
    }
    try {
      BluetoothDevice next = adapter.getRemoteDevice(address);
      if (host != null && connected && host.equals(next)) return;
      if (host != null && connected && !host.equals(next)) {
        release();
        hid.disconnect(host);
      }
      host = next;
      if (!hid.connect(host)) listener.state("Could not connect · check computer Bluetooth");
    } catch (Exception e) {
      listener.state("Connection failed · try pairing again");
    }
  }

  public void mouse(int buttons, int x, int y, int wheel) {
    if (hid == null || host == null || !connected) return;
    try {
      attempted++;
      if (hid.sendReport(host, 2, ControlEngine.mouse(buttons, x, y, wheel))) accepted++;
      else lastError = "Mouse report rejected";
    } catch (Exception e) {
      lastError = e.getClass().getSimpleName();
      connected = false;
      listener.state("Connection lost · " + lastError);
    }
  }

  public void key(int code) {
    if (hid == null || host == null || !connected) return;
    try {
      hid.sendReport(host, 1, ControlEngine.key(code));
      hid.sendReport(host, 1, ControlEngine.key(0));
    } catch (Exception e) {
      connected = false;
      listener.state("Connection lost");
    }
  }

  public void click(int buttons) {
    mouse(buttons, 0, 0, 0);
    mouse(0, 0, 0, 0);
  }

  public boolean phone() {
    try {
      return host != null
          && host.getBluetoothClass() != null
          && host.getBluetoothClass().getMajorDeviceClass() == BluetoothClass.Device.Major.PHONE;
    } catch (Exception e) {
      return false;
    }
  }

  public void release() {
    mouse(0, 0, 0, 0);
    key(0);
  }

  public void close() {
    foreground = false;
    proxyPending = registerPending = false;
    release();
    try {
      if (hid != null) {
        hid.unregisterApp();
        adapter.closeProfileProxy(BluetoothProfile.HID_DEVICE, hid);
      }
    } catch (Exception ignored) {
    }
    hid = null;
    registered = false;
    connected = false;
  }
}

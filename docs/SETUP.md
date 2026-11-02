# Controller setup

## Install

Download the watch and phone APKs from [Releases](https://github.com/vishwateja2345/WristLab/releases/latest), or build them from source. Watch package: `studio.teja.gesturelab`. Phone package: `studio.teja.airbridge`. The launcher labels remain Gesture Lab and Gesture Lab Link.

Install the phone APK using Android's package installer. Android may require you to allow installation from your chosen file manager. Accessibility permission is separate from installation.

For the watch, pair your computer using Android wireless debugging. Install with a **specific device selector**, especially when both devices are connected:

```sh
adb pair WATCH_IP:PAIRING_PORT
adb connect WATCH_IP:CONNECTION_PORT
adb -s WATCH_IP:CONNECTION_PORT install -r PATH_TO_WATCH_APK
```

The pairing and connection ports are different and can change. Enter pairing codes interactively; never put them in issues. Turn Wireless debugging off after installation. Runtime communication uses Bluetooth, not ADB. No root or Mac receiver is needed.

## First session

1. Pair the watch and phone normally in Galaxy Wearable / Bluetooth first. Unlock both and turn Bluetooth on.
2. Open **Gesture Lab Link** on the phone. Grant Nearby devices access and choose your paired watch.
3. Tap **Start session** and enable **Gesture Lab phone controls** in Android Accessibility. If Android blocks this for a sideloaded app, inspect its App info menu for **Allow restricted settings**. Availability and wording vary; do not enable unrelated services.
4. Open Instagram Reels, or the scrollable screen you intend to control, on the phone. The helper does not recognize foreground apps or read their contents.
5. Open **Gesture Lab** on the watch. Grant Nearby devices access. In Setup choose your paired phone, then return to the controller. Hold your wrist still briefly until ready.
6. Tap NEXT for one upward screen swipe. Turn the watch face gently away about 15–20 degrees and return within roughly 1.5 seconds for the reverse swipe. Return fully and pause between commands.

If first-time setup takes longer than the one-minute phone waiting timeout, start the phone session again after choosing devices.

## Optional air pinch

Samsung's recognition supplies the pinch; WristLab does not infer individual finger movement from a raw gyroscope. On a supported Galaxy Watch, the wearer can enable **Settings → Accessibility → Universal gestures** and map **Pinch → Tap**. The original setup also used Double pinch → Tap and disabled Samsung's own back-and-forth action to avoid a conflict.

Use Samsung's activation gesture if required by your firmware, then focus the app's NEXT target. Samsung may draw its own focus indicator. Turning Universal Gestures off stops pinch recognition; touch NEXT still works. The original user's final configuration left Universal Gestures off by preference. No setup tool in this repo toggles it for you.

## Stop

Tap STOP or leave the watch controller. This closes its service and socket. An established disconnect makes the phone helper disable itself. If you enabled the phone helper but no watch connects, it expires after one minute. An idle watch session expires after five minutes.

You can also end a session in the phone app or turn its Accessibility service off directly. Android can still show its own permission/Accessibility notices during a deliberately enabled session; the apps post no recurring status notification.

Closing the phone UI alone is **not** a stop action during a connected session: it must be able to perform the requested swipe while Reels is visible. For idle battery savings, end the session via the watch or phone controls.

## Faces

Install one selected face APK on the watch with the same explicit ADB selector. Select it using the watch's native face picker; long-press → Customize exposes its colors, hands and complication slots. A face does not require the gesture apps. Do not install the entire collection just to try the controller.

## Undo

Turn off Gesture Lab phone controls in phone Accessibility and Samsung Universal Gestures if you enabled it only for this trial. Uninstall Gesture Lab and Gesture Lab Link from normal App info. Select a native face before uninstalling any custom face you no longer want. Other health, account and notification settings do not need to change.

References: [wireless ADB](https://developer.android.com/tools/adb#connect-to-a-device-over-wi-fi), [restricted settings](https://support.google.com/android/answer/12623953).

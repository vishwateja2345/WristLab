# Compatibility

| Component | Declared minimum / requirements | Actual evidence |
|---|---|---|
| Gesture Lab watch | API 31+, gyroscope, bonded Bluetooth; target API 36 | One Samsung Galaxy Watch7 44 mm LTE, 480×480; device-side testing on Android 36 / Wear OS 6 |
| Air pinch | Samsung Universal Gestures, wearer enabled and activated, Tap mapping | Worked in dedicated app on that Watch7 before the owner turned it off |
| Gesture Lab Link phone | Android API 31+, bonded Bluetooth, manually enabled Accessibility; target API 35 | One OnePlus 15R / CPH2767; Reels NEXT/PREV user tested |
| Faces | Watch Face Format 2 capable renderer; packaged manifests min API 34 | The tested Watch7 rendered selected designs; remaining faces are previews |
| Watch Deck | API 30+; optional step-counter access | Build/JVM tests only; not device tested |

The original inquiry mentioned One UI Watch 6, but later device inspection used Android 36 / Wear OS 6. Do not infer coverage of the earlier firmware from the initial user description. Check your actual watch software and report it without serial numbers.

No iPhone companion, Tizen support, Pixel Watch pinch promise, Samsung Health integration or Play Store approval is claimed. Gyroscope direction depends on wearing orientation; left-wrist/right-wrist and different watch rotations need more data. Custom swipe distances, other feeds and phone display modes also need testing.

For a new report, use [the device report template](../examples/DEVICE_REPORT.md). Do not share Bluetooth addresses, ADB endpoints, pairing codes, raw notifications or account information.

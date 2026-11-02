# Gesture Lab watch controller

Source baseline 0.22-manual. See [setup](../../docs/SETUP.md) and [architecture](../../docs/ARCHITECTURE.md). Compile with `python3 tools/build.py watch` from the root.

The five active classes are MainActivity, SessionService, SessionPolicy, PhoneBridgeClient and BackGesture. Other src helpers remain for deterministic regression tests and historical HID research; they are not included in this APK. Pinch requires optional Samsung native gestures; there is no hidden watch-face target. Leaving the activity stops the session.

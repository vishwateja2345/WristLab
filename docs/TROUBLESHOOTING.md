# Troubleshooting

| Symptom | Check |
|---|---|
| Phone waiting / watch disconnected | Bluetooth on and normally paired, Nearby devices granted, each app has the correct bonded peer selected. Start phone session, then open the watch controller before the 60-second setup timer expires. |
| Pinch does nothing | Native Samsung Universal Gestures is optional and may be off. Tap NEXT to separate transport trouble from recognition. Enable/activate the native feature only if you want its indicator and behavior. |
| Pinch opens other watch apps | Stay inside Gesture Lab. It provides no home-screen overlay. Samsung gestures outside this app remain Samsung navigation. |
| Previous too sensitive / absent | Hold still briefly after connection; use a gentle 15–20 degree turn away and return, not a finger twitch. Try Setup → Recenter. Watch orientation may require tuning. |
| Half a reel / wrong app moves | The phone must show the intended unlocked scrolling view. This is a completed touch swipe, not app-aware item navigation. Device size, layout and app behavior may need a calibrated SwipePlan contribution. |
| Session stops when watch closes | Intentional manual battery policy. Open a new phone session and watch controller when needed. |
| Recurring notification | Current apps have no notification permission/status-posting code. Check whether it is Android's Accessibility notice or a different/older app. End the phone session and check App info. Do not disable unrelated notifications globally. |
| Accessibility toggle blocked | Review Android's Allow restricted settings option for this sideloaded package. Do not change other services. See the linked Android help in SETUP. |
| APK update signature mismatch | Local builds, public CI and the original private installation can have different signing keys. Use your own consistent key or uninstall that specific old app first; this clears its settings. |
| Debugging address changed | Pairing and connect ports differ and rotate. Read the current watch debugging screen. Runtime does not need debugging. |
| Calendar text is unexpected | A next-event complication is the selected provider's data, not a hardcoded sample. Account sync must be checked in that provider separately. |

For maintainers: standard `uiautomator` automation can suppress other Accessibility services while it owns automation. Do not interpret an in-test native pinch failure as normal behavior without rechecking after automation disconnects. Do not upload device dumps to issues without redaction.

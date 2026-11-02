# Watch Deck preview

A separate native watch experiment: foreground step-counter session totals and a monotonic focus timer. Not installed or device tested. Compile with `python3 tools/build.py deck`.

The optional activity-recognition permission is only needed for session steps. These are sensor deltas, not Samsung Health daily totals. Background gaps are excluded. Focus tracks a persisted monotonic deadline; no alarm, notification or background service guarantees an alert while closed. Completion haptic is checked only while foreground or on return. Reboot safely resets timing state.

Fourteen JVM state assertions cover step baselines/gaps/resets and countdown/pause/reboot handling. Physical UI, sensor, rotary input, haptic and battery checks remain pending. No network, AI, health database, account or location integration is present.

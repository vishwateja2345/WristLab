# Battery findings and a repeatable check

The original tester reported faster drain, without a measured starting percentage/time interval. Earlier watch statistics showed repeated hotword microphone activity and display/AOD consumption. Hotword detection was switched off with the owner's authorization. The native watch-face CPU readings alone could not rule out display-related face costs.

We did not reset battery statistics or disable health tracking, essential services or unrelated notifications. No default debloat list is provided: a TV package-disable list is not an appropriate starting point for a health watch.

## Changes shipped

- Watch controller runs only inside its visible activity. Closing it stops its normal service, gyroscope and Bluetooth socket. It does not run from the watch face.
- Phone helper is a temporary user-enabled session. It disables itself on established watch disconnect; unconnected setup expires after one minute.
- No foreground-service status notification, boot receiver, wake lock, forced screen-on or background sensor mode.
- Single-flight swipes, cooldowns and no automatic command replay; bounded replies and a limited number of watch connection attempts.
- Sensor registration stops for hidden, asleep, locked, disconnected and low-battery states.
- No app network, microphone, location, account, analytics or health permission. No persistent gesture history or disk logs.
- Meridian and Instrument economy updates default seconds off. Faces use native updates, cropped hand bitmaps and dark ambient layouts. Photo faces hide full imagery in ambient mode.
- Development wireless debugging was turned off after device work. It is not needed for normal Bluetooth use.

Leaving the phone UI while connected is expected: it must dispatch requested swipes behind Reels. **End the watch session** to stop both. Force-stopping is a troubleshooting action, not the ordinary workflow.

## What was verified

The final device test connected both controllers, closed the watch activity, and found no remaining watch service, phone helper service or enabled phone helper. A subsequent check found neither app process and no app notification records. This verifies that particular lifecycle test; Android may cache processes in other circumstances. It is not a measurement of all-day endurance.

## How to measure

Record battery percentage, elapsed time, AOD/brightness, face, LTE/Wi-Fi/Bluetooth, workouts, voice assistant settings and controller session length. Compare the same face and workload with the controllers stopped versus a deliberately timed session. Repeat comparable periods and include both watch and phone. Compare a native face separately while keeping display settings constant.

If drain persists while the controller is off, inspect the device's own battery usage and app list before blaming any one face. Do not publish raw bugreports: they can contain account, notification and device identifiers.

Battery endurance and recognition accuracy remain unmeasured. See [validation](VALIDATION.md) and [sensor guidance](https://developer.android.com/develop/sensors-and-location/sensors/sensors_overview).

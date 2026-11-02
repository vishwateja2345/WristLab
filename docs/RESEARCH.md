# Research and decisions

This is the condensed project notebook as of 2026-11-02. It records source-backed constraints, observed results and ideas separately. It is not a claim that wrist control or any suggested feature has never existed.

## What survived testing

| Question | Finding | Decision |
|---|---|---|
| Can a watch move a computer cursor? | WearMouse already provides this; an earlier custom HID prototype also moved the tester's Mac cursor. | Credit prior art. Mac control is not in the current manual phone release. |
| Can a wheel event advance one reel? | HID wheel steps moved partial screens, sometimes leaving the previous video playing. Larger wheel values still behaved poorly. | Use one completed Android touch swipe through an explicitly enabled phone helper. |
| Can tilt keep scrolling? | Continuous tilt scrolling caused runaway movement. | One action per excursion/return, guards and no replay. |
| Can pinch click? | Samsung Universal Gestures successfully delivered clicks in the dedicated controller. | Optional native recognition; do not claim raw IMU finger tracking. |
| Can it work from the watch face? | A temporary floating target interacted with battery complications and unrelated watch navigation. | Removed. Visible controller only. |
| Can it stay ready all day without background work? | A phone helper must be active while controlling an app in front of it. Persistent readiness conflicted with the user's notification/battery preference. | Manual sessions; close watch activity to stop both. |
| Do faces need a custom background app? | Native WFF resources supply time, steps, battery and complications without custom Java services. | Use resource-only faces with dark ambient layouts. |

## Watch-face design

Paid faces such as [MD242](https://play.google.com/store/apps/details?id=com.watchfacestudio.md242) and [BALLOZI LUXOS](https://play.google.com/store/apps/details?id=com.watchfacestudio.ballozi.luxos.watchface) informed general layout research: legible hierarchy, recessed panels, analog/digital combinations, material contrast, edge progress and restrained color. Their code, logos and assets were not copied.

The custom hybrid puts steps in a left subdial, battery at right, weather above, month and weekday/day in distinct windows, and a native next-event slot below. Sunrise and heart rate were removed following the user's feedback. A calendar slot is provider data; it does not prove Google Calendar account sync. Weather/calendar availability depends on installed providers and permissions.

Photo designs use watch-sized local images, adjustable clock color and brightness, and a black ambient state. Alpine adds a foreground mountain mask so the native clock can pass behind the ridge. Anime stills were explored for personal previews; those assets and their APKs are not redistributed here. See [ASSETS.md](ASSETS.md).

## Sensor and platform limits

The stock Watch7 prototype uses its exposed gyroscope and Samsung's optional Accessibility recognition. It has no camera, electromyography band or custom ultrasonic hardware. A conventional inertial sensor does not provide a reliable direct measurement of arbitrary individual finger positions. Research using extra sensors does not establish that the same result is available on this stock watch.

Native gesture settings and focus visuals can vary by firmware. No hidden Samsung API is required in the shipping controller. The optional Samsung settings intent falls back to instructions if unavailable. Device-specific activation/focus behavior needs real testing.

## Useful next experiments

These are ideas, not implemented promises or novelty claims. [ROADMAP.md](../ROADMAP.md) tracks their status.

- **Gesture workbench:** a short user-started calibration session with local sample capture, confidence feedback and immediate deletion. Evaluate left/right wrist differences before adding more actions.
- **Presentation clutch:** explicit arm/disarm for slide next/previous, with a visible target application. Build on the old HID experiment rather than claiming new cursor technology.
- **Reading mode:** finite page steps and an explicit short-scroll mode; never continuous tilt by default.
- **Comments mode:** deliberate entry/exit and a distinct tactile cue. Earlier coordinate-based Instagram experiments were brittle; do not silently restore them.
- **Step World:** turn native steps into a small offline world/map that updates when opened, with no always-on location or game loop. Concept only, separate from ordinary walking tracking.
- **Theme toolkit:** original depth masks, accessible contrast presets and matching AOD previews, with per-face provenance and measured asset footprints.
- **Measured battery bench:** same brightness/AOD/radio settings, comparable wear periods and repeated runs; report workload and uncertainty.

## References

- [Android Watch Face Format](https://developer.android.com/training/wearables/wff)
- [WFF specification and tooling](https://github.com/google/watchface)
- [Android sensor behavior and background limits](https://developer.android.com/develop/sensors-and-location/sensors/sensors_overview)
- [AccessibilityService and dispatchGesture](https://developer.android.com/reference/android/accessibilityservice/AccessibilityService)
- [Wear OS always-on behavior](https://developer.android.com/training/wearables/always-on)
- [WearMouse](https://github.com/ginkage/wearmouse), Apache 2.0 prior art
- [Drift / wristcursor](https://github.com/varaddorle007/wristcursor), related wrist-cursor work
- [MIT paper reviewed during the project](https://dspace.mit.edu/entities/publication/53f16edb-b8fb-486b-88ca-89297219a99f); background reading, not a reproduced result or dependency
- [CleanYourTV](https://github.com/Gathin23/cleanyourtv), community setup/documentation inspiration

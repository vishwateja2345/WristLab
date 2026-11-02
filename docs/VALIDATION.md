# Validation record

## Deterministic and build checks

The original project passed 437 watch/motion/report assertions, 25 phone swipe/protocol assertions and 14 Watch Deck state assertions. The public packaging reruns them. All public build targets are resource-compiled, linked, aligned, signed and verified. CI records results independently. A compiler success is not a device rendering, battery or recognition result.

Current app source versions are watch `0.22-manual` / code 22, phone `0.15-manual` / code 15. Watch Deck remains `0.1-preview` / code 1. Public APKs are rebuilt from this source using a new development signing key; they cannot update the original privately signed installation in place.

Only five watch classes enter the current APK. The motion suite also covers historical helper algorithms, which remain in source. Experimental expansion classes are not part of the release build.

## Observed on physical devices

- User confirmed wrist cursor movement on Mac in the earlier HID prototype, then pinch clicking.
- User confirmed NEXT pinch and gently tuned turn-and-return PREV in the phone Reels workflow. Later confirmed both worked correctly on the dedicated controller screen.
- Watch-home control caused interference and was removed. Background readiness was subsequently removed following the user's battery/notification feedback.
- Manual-session lifecycle test: active Bluetooth link established; watch activity closed; watch service absent; phone helper service absent; helper disabled.
- Final idle inspection: no controller processes, services or app notification records; watch timeout restored to 15 seconds. Native Samsung gestures and phone helper left off. Development debugging then disabled.
- Instrument Hybrid, Meridian and Atelier had device-side work; not every customization, provider, AOD state or firmware combination was systematically checked. See face-specific notes.

No private raw logs, serial numbers, local network endpoints or personal watch screenshots are included. Documentation previews use sample values.

## Still needed

Normal-use battery comparisons, quantitative false-positive/false-negative rates, wrist/orientation variants, accessibility focus differences across firmware, additional phones and apps, and official per-face WFF memory/Play validation. Earlier source XML passed the published WFF 2 schema; current CI checks XML/resources/builds, not the complete official WFF validation pipeline.

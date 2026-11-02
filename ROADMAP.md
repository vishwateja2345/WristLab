# Roadmap

## Available now

- Manual Bluetooth NEXT/PREV controller with optional Samsung pinch and watch turn-and-return.
- Ten editable native faces, source builds, experimental APK downloads and setup/undo documentation.
- Public build checks and a device-report workflow.

## Good first contributions

- Report a second phone/watch pair with software versions and actual observations.
- Improve large-text, contrast, screen-reader and left/right wrist behavior.
- Add measured idle/active battery comparisons with workload and display settings recorded.
- Make a face with original art, matching black AOD and asset credits.
- Add the official WFF schema/memory validation tools to CI with pinned provenance.

## Experiments on hold

| Idea | Existing work | What would make it usable |
|---|---|---|
| Comments, reading, media, navigation shortcuts | Archived source under experiments/expanded-controls | Explicit modes, robust app behavior, permission review and new device tests. No automatic reactivation. |
| Mac pointer / slide control | Legacy HID classes; prior prototype had user-observed cursor movement | A dedicated explicit session, ergonomic clutch, macOS compatibility and false-positive testing. |
| Watch Deck focus/session app | Compilable source and 14 state assertions | Device UI/haptic checks; no background alert promise. |
| Gesture workbench | Concept | Opt-in local calibration, labeled data, deletion and repeatable accuracy metrics. |
| Step World | Concept | An offline world driven by native steps, foreground-only updates and original assets. |
| Photo/depth face editor | Existing WFF layouts and masked mountain example | Portable authoring tools and export/memory checks; rights-aware image inputs. |

No roadmap item is claimed to be unprecedented. Background readiness is deliberately out of the current release; a future mode must remain opt-in and show its battery/lifecycle tradeoffs.

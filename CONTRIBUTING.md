# Contributing

Start with a [device report](examples/DEVICE_REPORT.md), a focused issue, or a small pull request. New integrations should describe a concrete action and how the wearer starts and stops it.

WristLab is a baseline for independent projects, not just changes to its existing controller. To add a separate app, face, tool or experiment, follow [projects/README.md](projects/README.md). You can choose your own architecture and dependencies; declare them and document setup/stop/undo steps. Existing controller-specific rules below apply when changing that controller, rather than requiring every future project to use it.

## Local checks

```sh
python3 tools/test.py
python3 tools/check_publication.py
python3 tools/catalog.py check
python3 tools/test_projects.py
python3 tools/build.py all
```

Compilation needs the Android SDK; JVM tests only need a JDK. See [BUILD](docs/BUILD.md). Keep tests meaningful: replay a failed gesture/protocol sequence or check lifecycle/permission behavior rather than mirroring UI text.

## Controller changes

- Preserve manual sessions, one action per gesture, no replay after reconnect, bonded-peer selection, and lock/visibility guards.
- Do not add always-on services, boot starts, network, screen-content access or more permissions without documenting the concrete need and tradeoffs.
- Keep raw sensor/notification/device data out of Git and issue reports. Do not add hardcoded addresses, serials, private paths, signing keys or tokens.
- Separate protocol/recognizer changes from interface redesign where possible. Report actual devices, firmware, wearing orientation and failure cases.
- The legacy HID helpers and expanded controls are experiments, not a reason to re-enable them in the stable target.

## Face changes

Add a self-contained `faces/<name>` directory, manifest, WFF resources, a sample-value preview, catalog entry and credits. Supply a matching black ambient layout and explain seconds/animation choices. Native steps and user-editable complications are preferred to app polling. Do not claim battery improvements from bitmap size alone.

Alternatively add an independent face under `projects/<id>` using the generator; this avoids editing the bundled gallery. Update its metadata and README and it will be discovered automatically.

Only contribute work you can redistribute. Retain OFL/font and image notices. Paid face assets, copyrighted anime stills, vendor APKs and private screenshots are not accepted. Original anime-inspired artwork is welcome with clear provenance.

## Pull requests

State the user-visible change, validation performed, limitations and any new permission. Label mockups as mockups. Do not describe a build-only check as a device test. Contributions are licensed under Apache 2.0 for code/original artwork, subject to documented third-party asset terms; no CLA is required.

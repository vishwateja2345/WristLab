# Repository guidance

Use README.md and docs/BUILD.md as the source of truth. Run tools/test.py and tools/check_publication.py for code changes; build changed targets with tools/build.py. No build command should install or change devices. Do not add private paths, device identifiers or credentials.

Current release is manual foreground watch control plus a temporary phone Accessibility session. Preserve closure cleanup, no idle auto-start, no home-screen overlay, no recurring app notification, no network permission, bounded commands and no replay. Samsung native gestures are optional and wearer-controlled.

Keep experiments out of active build targets. New face assets need documented redistribution rights. No copyrighted film stills, paid face assets or manufacturer APKs. Distinguish sample renders, build checks, device observations and measurements in docs.

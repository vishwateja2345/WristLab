# Assistant setup prompt

Use this repository to help me set up WristLab. My watch model/software: ____. My phone model/software: ____. I want: controller / selected faces / source development.

Read README.md, docs/SETUP.md, docs/COMPATIBILITY.md, docs/BUILD.md and docs/BATTERY.md first. Use this repo's build tools rather than improvising a new controller. Inspect actual connected devices with explicit selectors. Never assume the first ADB device is the watch.

Prepare and show selected designs before installing faces. Keep a record of only the settings you change so they can be restored. Do not disable health services, notifications, assistants or other apps as generic debloating. Do not bypass a lock screen, re-enable Samsung gestures without my choice, or change my existing watch face without asking.

For controller use, explain the temporary phone Accessibility helper and optional Samsung pinch. Keep default manual sessions. Test NEXT and PREV with the intended phone view unlocked, then close the watch app and verify both services stop. Do not restore the removed home-screen overlay or paused expanded controls.

Do not collect unnecessary personal data or publish raw logs. Never put pairing codes, IP/MAC addresses, serial numbers, passwords or private signing keys in a commit. After installation and testing, stop the session and turn wireless debugging off. Report what you actually observed separately from build results; do not claim battery endurance or novelty without evidence.

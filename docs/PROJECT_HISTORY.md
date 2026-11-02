# Project history

This repository is a curated public handoff of a personal Watch7 build, rather than a dump of device logs and every temporary patch.

1. **Custom faces:** explored original analog, hybrid, photo and depth layouts. Steps became the primary activity metric. Instrument was revised around native steps, battery, weather, month/date and next-event data, with dark ambient rendering and user-selectable materials.
2. **Less useful directions paused:** the wearer rejected generic walking features and asked for experiments with a more distinctive use case. AI integration was set aside. Watch Deck remains an optional standalone preview, not the main product.
3. **Air input research:** existing wrist mice such as WearMouse established prior art. A custom HID prototype moved the Mac cursor and accepted native Samsung pinch clicks.
4. **Phone swipe work:** continuous tilt scrolled indefinitely; wheel bursts moved partial reels. The phone Accessibility companion delivered completed swipes, leading to user-confirmed NEXT/PREV behavior.
5. **Expansion and simplification:** comments, media, app shortcuts and contextual menus were explored, then paused after regressions. Their historical source is included separately, not activated.
6. **Home-screen experiment removed:** a floating control allowed watch-face input but conflicted with other watch targets. The dedicated app screen became the supported surface.
7. **Manual battery policy:** persistent readiness and notifications were removed. Closing the watch activity stops the watch controller and disables the temporary connected phone helper. The final device state left both stopped and Samsung native gestures off at the wearer's request.
8. **Public handoff:** portable builds, readable active Java, contribution templates, source credits, known limitations, and development APKs. Private keys, logs and device identifiers are excluded. Personal film-still prototypes stay local because their redistribution rights are not established.

The research is in [RESEARCH.md](RESEARCH.md); observed checks are in [VALIDATION.md](VALIDATION.md); future ideas and paused work are in [ROADMAP.md](../ROADMAP.md). No all-day endurance or numerical recognition accuracy result was established by this work.

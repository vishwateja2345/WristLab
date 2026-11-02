# Gesture Lab Link phone companion

Source baseline 0.15-manual. See [setup](../../docs/SETUP.md), [architecture/protocol](../../docs/ARCHITECTURE.md), and [battery policy](../../docs/BATTERY.md). Compile with `python3 tools/build.py phone`.

This is a temporary Accessibility gesture dispatcher, not a notification reader or screen scraper. Only NEXT/PREV from the selected paired watch are accepted. It needs to remain system-bound behind the intended foreground app during a session; established watch disconnect ends the helper. It does not know which app or view is in front.

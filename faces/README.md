# Watch-face gallery

Ten native Watch Face Format 2 designs for a 480×480 round display. All are resource-only: no executable app code, network permission or custom polling service. Availability and exact rendering depend on the native renderer and complication providers.

Previews contain sample values. Device status below is historical evidence, not a guarantee that every customization/AOD state was checked. No battery endurance result is claimed.

| Face | Preview | Device status |
|---|---|---|
| [Meridian](meridian/) | <img src="meridian/preview.png" width="170" alt="Meridian sample render"> | Installed; economy update installed |
| [Instrument Hybrid](instrument/) | <img src="instrument/preview.png" width="170" alt="Instrument Hybrid sample render"> | Installed; economy update installed |
| [Atelier Graphite](atelier/) | <img src="atelier/preview.png" width="170" alt="Atelier Graphite sample render"> | Installed; interactive rendering checked |
| [Terminal](terminal/) | <img src="terminal/preview.png" width="170" alt="Terminal sample render"> | Build checked; device acceptance not recorded |
| [Orbit](orbit/) | <img src="orbit/preview.png" width="170" alt="Orbit sample render"> | Build checked; device acceptance not recorded |
| [Alpine / Dusk](alpine/) | <img src="alpine/preview.png" width="170" alt="Alpine / Dusk sample render"> | Preview; not installed |
| [Dolomites](dolomites/) | <img src="dolomites/preview.png" width="170" alt="Dolomites sample render"> | Preview; not installed |
| [Blue Marble](earth/) | <img src="earth/preview.png" width="170" alt="Blue Marble sample render"> | Preview; not installed |
| [Milky Way](milkyway/) | <img src="milkyway/preview.png" width="170" alt="Milky Way sample render"> | Preview; not installed |
| [Tokyo after dark](tokyo/) | <img src="tokyo/preview.png" width="170" alt="Tokyo after dark sample render"> | Preview; not installed |

## Design notes

- **Instrument:** left steps/progress, right battery, weather, month, weekday/date and native next-event slot; dark carbon/titanium materials, palette and hand options. Economy seconds default off.
- **Meridian:** original analog design; economy resources and seconds default off.
- **Atelier:** graphite/platinum default with marine/emerald alternatives and material/hand accents. Replacing a complication does not change the independent steps/battery background gauges.
- **Photo series:** clock color/size, scene dimming, steps/split/clean edge progress and editable short-text widget. No seconds or animation.
- **Alpine:** mountain foreground mask can put the clock behind the ridge; dark ambient drops imagery.

Build all faces with `python3 tools/build.py faces`. Photo/AOD rendering and compatibility need further device reports. [Assets and licensing](../docs/ASSETS.md) explain why the personal anime prototypes are absent.

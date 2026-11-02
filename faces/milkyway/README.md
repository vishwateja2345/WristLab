# Milky Way

Status: **Preview; not installed**. Preview uses sample data; runtime and endurance testing are incomplete.

Build: `python3 tools/build.py milkyway` from the repository root. Install only this APK if this is the face you want. Select it in the native watch face picker; long-press → Customize exposes the configuration declared in `res/raw/watchface.xml`.

Artwork credit: Nathan Anderson / Unsplash. Font and image terms are listed in [asset provenance](../../docs/ASSETS.md). The root license does not replace third-party image/font terms.

The native renderer supplies time/steps/battery/complication values. Providers have their own permissions and account setup. Selecting a calendar complication does not verify Google Calendar sync. AOD is defined by native ambient variants; compare on the physical watch before relying on a preview.

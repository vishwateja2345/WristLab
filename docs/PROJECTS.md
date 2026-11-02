# Project metadata contract — version 1

Bundled examples are listed in `catalog/projects.json`. Community projects are discovered from `projects/*/project.json` automatically. Discovery validates metadata and paths; it does not execute code or install anything.

| Field | Meaning |
|---|---|
| `id` | Unique lowercase letter/digit/hyphen id, starting with a letter |
| `name`, `summary` | Display name and concrete use case |
| `kind` | `watch-app`, `phone-app`, `watch-face`, `tool`, `integration`, `research` |
| `status` | `draft`, `build-tested`, `device-tested`, `paused`; document evidence in README |
| `readme` | README path inside this project's directory |
| `license` | Code/asset license description; document mixed terms separately |
| `permissions` | Explicit list, including `[]` if none; keep it aligned with manifests |
| `build_system` | `android-sdk` for the shared builder, `manual` for documented external tools, `none` for non-buildable research |
| `build_target` | For shared Android builds: community target must be `project-<id>` |
| `path` | Bundled registry only; for community projects it is derived from the discovered directory |

The Android builder expects `AndroidManifest.xml`, `res/`, and optionally Java under `src/` inside the project directory. It compiles resources and Java, aligns/signs/verifies an APK. It does not assume a phone/watch connection or depend on Gesture Lab. Other languages or frameworks should use `manual` and their own documented process.

Catalog validation compares Android manifest permissions with the metadata list, so a newly added permission must be declared in the project record too.

`python3 tools/catalog.py list --json` produces the combined machine-readable catalog. `check` validates it. This is a small repository contract, not a promised runtime binary/plugin API.

# WristLab as a growing platform

WristLab is an open workshop and contribution baseline for Wear OS: shared build/check tools, reproducible setup notes, a project catalog and independently maintained projects. The original apps and faces are its first examples.

It is not currently a runtime plugin host, app store, cloud service or one universal sensor SDK. A project can be useful without using Gesture Lab, Bluetooth, Samsung pinch, or any of the original faces.

## Three layers

| Layer | Role | Where to start |
|---|---|---|
| Shared foundation | SDK-only APK builds, project discovery, publication checks, docs and contribution workflow | `tools/`, `catalog/`, `docs/` |
| Bundled examples | The tested/manual gesture controller, custom faces and known historical experiments | `apps/`, `faces/`, `experiments/` |
| Community projects | Independent apps, faces, companion tools, hardware ideas or research, each with its own scope and evidence | `projects/<id>/` |

## Pick a contribution path

- **New capability:** use the starter generator and create an independent project. A focus tool, novel interaction, native tile or original face does not have to become a Gesture Lab feature.
- **Improve an example:** edit that example directly and preserve its documented lifecycle/permissions. Keep disruptive experiments separate.
- **Research or device support:** add repeatable findings, a device report, or a documented experiment. A careful failed result can be useful.
- **Improve the foundation:** make project creation, validation, accessibility, build portability or documentation easier for everyone.

## Minimal project contract

One project directory, a `project.json`, source (when applicable), a README with build/setup/stop/undo instructions, declared permissions, asset terms, and an honest maturity level. Discovery does not execute contributed commands. Native Android targets can opt into the shared compiler; other stacks keep their own documented build process.

```sh
python3 tools/catalog.py list
python3 tools/new_project.py my-idea --name 'My Idea' --kind watch-app
python3 tools/build.py project-my-idea
```

See [the contribution steps](../projects/README.md) and [metadata contract](PROJECTS.md).

## Expansion priorities

Better creation templates, more device reports, official WFF validation, measured battery tooling, and well-scoped independent experiments come before a large plugin framework. The [roadmap](../ROADMAP.md) is open to proposals; existing ideas are examples rather than a closed feature list.

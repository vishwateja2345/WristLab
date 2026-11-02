# Community projects

This is where independent contributions live. A new idea does **not** need to extend Gesture Lab or use its Bluetooth protocol. A watch app, face, Android companion, desktop tool or research prototype can have its own architecture and lifecycle.

## Make a starter

```sh
python3 tools/new_project.py my-project --name 'My Project' --kind watch-app
python3 tools/catalog.py list
python3 tools/catalog.py check
python3 tools/build.py project-my-project
```

Kinds: `watch-app`, `phone-app`, `watch-face`, `tool`, `research`. The Android app starter is a minimal dark foreground-only screen with a Close button and no permissions/services. The face starter copies the project's original Terminal design and retains its font/asset notices. Tool/research starters are documentation/source folders; write their own instructions.

The generator refuses to overwrite an existing directory. No build or scaffold command installs an app or changes a device.

## How discovery works

Each `projects/<id>/project.json` is discovered by `tools/catalog.py`. There is no central list to edit for a community project. Directories beginning with `_` are templates and are skipped. [Metadata fields](../docs/PROJECTS.md) describe the contract.

An `android-sdk` project with its own manifest/resources can use the shared builder under target `project-<id>`. It compiles that project's own Java/resources; it does not link Gesture Lab classes into it. `all` includes registered Android community projects. Other build systems are documented per project and never run by catalog discovery.

## Submit

1. Fork the repository and create a branch.
2. Generate or copy a project skeleton. Explain the user problem, supported devices, setup, stop/undo steps, permissions, battery behavior and asset rights.
3. Implement your idea independently. Record tests and observed limits. Keep the initial status `draft` until evidence supports another status.
4. Run catalog/publication checks, relevant JVM tests, and your project's build. Do not submit keys, device dumps, addresses, credentials, paid assets or unlicensed film stills.
5. Open a focused pull request using the supplied template. The project becomes discoverable from the catalog once merged.

Existing apps and faces are bundled examples, not the boundary of WristLab. See the [platform guide](../docs/PLATFORM.md) and [contribution guide](../CONTRIBUTING.md).

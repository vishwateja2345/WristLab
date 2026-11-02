# Build from source

Requirements: Python 3.9+, JDK 17+, Android SDK platform 36 or newer and build-tools 36.0.0. No Gradle, Maven or Python packages are required for normal compilation. The original machine used Android Studio's bundled JDK and SDK platform `android-37.0`; CI also checks platform `android-36`.

```sh
export JAVA_HOME=/absolute/path/to/jdk
export ANDROID_SDK_ROOT=/absolute/path/to/android/sdk
# If SDK packages are missing:
sdkmanager 'platforms;android-36' 'build-tools;36.0.0' 'platform-tools'
python3 tools/test.py
python3 tools/build.py all
```

Targets: `watch`, `phone`, `faces`, `deck`, `all`, or a face name such as `instrument`. The `all` target includes the separately marked Watch Deck preview, but not the paused expanded-controls source.

Outputs go to ignored `build/apks/`. Build tools are selected with `ANDROID_BUILD_TOOLS` (default `36.0.0`). Use `ANDROID_PLATFORM=android-36` to select an installed platform explicitly; otherwise the highest installed platform with an `android.jar` is used. JDK tools are read from `JAVA_HOME`, falling back to PATH.

The build compiles the small Java apps directly, converts bytecode with d8, compiles resources with aapt2, aligns, signs and verifies APKs. Watch-face targets contain resources only. No build command pairs, installs, grants permission or changes device settings.

## Signing

Without signing configuration, the build creates **its own development key** at `build/debug-signing.jks` using the standard public debug password `android`. That key is ignored by Git and is for local development only.

For your own persistent signing key, provide all four environment variables:

```sh
export WRISTLAB_KEYSTORE=/absolute/path/to/your-key.jks
export WRISTLAB_KEY_ALIAS=your-alias
# Set these privately in your shell/CI secret store:
# WRISTLAB_STORE_PASS
# WRISTLAB_KEY_PASS
python3 tools/build.py all
```

The project never includes the original installation signing key. Public APKs are development-signed experimental builds, not Play Store releases. APKs signed with a different key cannot update an existing installation in place. Uninstalling loses app settings; keep your own key for continued development. Reuse of the original package IDs preserves source history but does not grant update compatibility across keys.

## Checks

`python3 tools/test.py` runs 437 watch/motion/report assertions, 25 phone swipe/protocol assertions, and 14 Watch Deck state assertions. The watch suite includes retained experimental motion code; not every assertion describes a shipping feature. These are deterministic JVM checks, not measurements of recognition accuracy or battery life.

`python3 tools/check_publication.py` checks publication hygiene, XML syntax, the current permission contract, and face resources. CI builds and verifies all 13 APKs and uploads a short-lived development artifact. Runtime device checks remain separate; see [VALIDATION.md](VALIDATION.md).

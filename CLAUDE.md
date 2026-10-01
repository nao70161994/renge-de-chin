# Development guide

Single Activity Android app. Preserve the package `com.mycompany.myapp` and the three jokes:
レンジ → チン！ / オーブン → ブン！ / トースター → トゥース！

## Standard build (Linux / macOS / Windows / Android Studio)

Use JDK 17, Android SDK platform 34 and the checked-in Gradle 8.2.1 wrapper:

```bash
./gradlew clean assembleDebug lintDebug
```

Set `ANDROID_HOME` or `sdk.dir` in untracked `local.properties`.
Output: `app/build/outputs/apk/debug/app-debug.apk`.
GitHub Actions builds this APK from a clean checkout and uploads it as an artifact.

## Termux build

```bash
bash build.sh
cp app-debug.apk /storage/emulated/0/Download/
```

`build.sh` installs Termux tools, downloads the Android 34 platform jar if missing,
and uses aapt2 → javac → d8 → zipalign → apksigner. It builds resources, manifest
and assets directly from `app/src/main/`; no previous APK or `resources.ap_` is needed.
SDK versions in this script must match `app/build.gradle` (min 21, target 34).
Keep an existing `debug.keystore` to preserve compatibility with previously signed APKs.
Newly generated debug keys cannot update APKs signed with a different key.

## Implementation

- `MainActivity.java`: explicit button listeners, bounded SoundPool (three simultaneous streams).
- Bell and oven waveforms are generated once at startup, written as PCM WAVs to app cache,
  and loaded into SoundPool. The original waveform formulas and durations are retained.
- `assets/toos.ogg`: toaster audio; keep uncompressed for `AssetManager.openFd`.
- Sound playback waits for the asynchronous load completion callback. Failures are logged.
- The latest popup replaces the previous one. Popups/callbacks are cleared on stop;
  audio is paused on stop and SoundPool is released on destroy.
- `res/layout/main.xml`: the three buttons. Changes are included by both build paths.

Never commit build outputs, downloaded SDKs, or signing keys.

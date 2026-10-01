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
SDK and version settings are read from `app-config.properties` by both build paths.
Keep an existing `debug.keystore` to preserve compatibility with previously signed APKs.
Newly generated debug keys cannot update APKs signed with a different key.

## Implementation

- `MainActivity.java`: explicit button listeners and SoundPool (three simultaneous streams).
- `assets/bell.wav` / `oven.wav`: pre-generated original waveforms. No runtime synthesis
  or cache writes. Regenerate only when changing audio with `python tools/generate_sounds.py`.
- `assets/toos.ogg`: original toaster recording. WAV and OGG are uncompressed in both APK paths.
- Each button is disabled until its sound finishes loading. Failed loads and a 10-second
  timeout show a retry action; retry recreates the Activity and releases the previous pool.
- Playback failure is shown to the user and logged. Audio pauses on stop and releases on destroy.
- The answer appears in the screen's display panel with a brief scale animation.
- `res/layout/main.xml`: scrollable appliance buttons, vector icons, and dp/sp dimensions.
- `app-config.properties`: shared SDK and version settings for Gradle and build.sh.
  When changing COMPILE_SDK, also update SDK_ARCHIVE_URL and SDK_JAR_ENTRY for the new platform.
- Termux installs `aapt` as well as `aapt2`: `aapt` provides the required `zipalign` command.

Never commit build outputs, downloaded SDKs, or signing keys.

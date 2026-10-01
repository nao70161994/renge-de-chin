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
GitHub Actions validates the build and lint on every push and pull request.
To publish APK artifacts with the same signing identity across runs, configure the repository
Actions secret `APK_KEYSTORE_BASE64` with the base64 encoding of your existing `debug.keystore`
(alias `androiddebugkey`, store/key password `android`). Without that secret, CI performs
validation only and does not distribute an APK signed with a temporary runner key.
Gradle and Termux both use the root `debug.keystore` when present. Keep and back up that key;
never put the encoded value in a commit, issue, or chat. Existing APKs signed with another
key need a one-time uninstall before switching (which removes app data).

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
- The answer appears in the screen's display panel with a brief scale animation; playback
  scrolls back to the answer when lower buttons are used on a short screen.
- Navigation uses a dark background on older devices and dark icons on a light background
  on Android 8.1 and later. The launcher icon uses the same appliance colors as the screen.
- `res/layout/main.xml`: scrollable appliance buttons, vector icons, and dp/sp dimensions.
- `app-config.properties`: shared SDK and version settings for Gradle and build.sh.
  When changing COMPILE_SDK, also update SDK_ARCHIVE_URL and SDK_JAR_ENTRY for the new platform.
- Termux installs `aapt` as well as `aapt2`: `aapt` provides the required `zipalign` command.

Never commit build outputs, downloaded SDKs, or signing keys.

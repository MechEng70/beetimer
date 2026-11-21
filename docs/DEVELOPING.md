# Developing Bee Timer

Native Android: Kotlin, Jetpack Compose (Material 3), Room, AlarmManager. Built from the command line on
Fedora; Android Studio is not needed.

## Toolchain (Fedora)

- JDK 21: `sudo dnf install java-21-openjdk-devel` (or Temurin 21). Gradle finds it as a toolchain.
- Android command-line tools: download "Command line tools only" from
  https://developer.android.com/studio#command-tools, unzip to `~/Android/Sdk/cmdline-tools/latest`.
- In `~/.zshrc` or `~/.bashrc`:
  ```
  export ANDROID_HOME=$HOME/Android/Sdk
  export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools
  ```
- SDK packages:
  ```
  sdkmanager "platform-tools" "platforms;android-36" "build-tools;36.0.0"
  ```
- `adb`: comes with platform-tools (or `sudo dnf install android-tools`).
- Create `local.properties` with `sdk.dir=/home/<you>/Android/Sdk` if `ANDROID_HOME` is not set.

## Build

```
./gradlew :scheduler:test          # scheduler unit tests (pure Kotlin, fast)
./gradlew assembleDebug            # app/build/outputs/apk/debug/app-debug.apk
scripts/check.sh                   # the quality gate: all tests plus Android lint
```

## Install on a phone

```
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Or copy the APK to the phone and open it from the file manager (allow "Install unknown apps" for it once).

## Layout

- `scheduler/`: pure Kotlin module. The treatment scheduler, model, presets and validation. Kotlin stdlib and
  kotlinx.serialization only, so it can move to Kotlin Multiplatform for iOS. `MultiplatformGuardTest` fails
  the build on any `java.`, `javax.`, `android.` or JVM-only use.
- `app/`: the Android app, packaged by layer: `data` (Room), `alarms` (AlarmManager, notifications,
  receivers), `ui` (Compose screens and `ui/theme/Theme.kt`).
- `app/schemas/`: Room schema JSON, committed for every database version.
- `docs/design/`: TECH-DESIGN, TASKS, WBS.

## Versions

Pinned in `gradle/libs.versions.toml`. compileSdk is 36, so Compose, core-ktx and lifecycle are pinned to
the last versions that build against 36 (Compose BOM 2026.06.01, core-ktx 1.18.0, lifecycle 2.10.0).
Moving to compileSdk 37 unlocks the newer ones.

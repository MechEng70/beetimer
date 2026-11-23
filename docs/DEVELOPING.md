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
scripts/check.sh                   # the gate: ktlint, detekt, lint, tests, py-tests, Semgrep, Snyk (3 min)
./gradlew ktlintFormat             # fix formatting before the gate
scripts/build-apk.sh               # gate + APK copied to release/beetimer-<version>.apk
```

`scripts/check.sh` does not build the APK. Run `./gradlew assembleDebug` before every install, or you install
a stale build.

## Install on a phone

```
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Or copy the APK to the phone and open it from the file manager (allow "Install unknown apps" for it once).

Install over the old build with `-r`. Don't uninstall or clear data on a phone in use: that deletes the
hives and any running session.

## Emulator

The E2E suite and screenshots use an AVD named `beetimer-api36` (Android 16, Pixel 6 size). Start it with
`emulator -avd beetimer-api36`. `scripts/e2e/run.sh` refuses any device that isn't an emulator.

## Logs

```
adb logcat -s BeeTimerE2E BeeTimerInvariant    # debug builds: events, and any invariant violation (E/)
```

## Layout

- `scheduler/`: pure Kotlin module. The treatment scheduler, model, presets and validation. Kotlin stdlib and
  kotlinx.serialization only, so it can move to Kotlin Multiplatform for iOS. `MultiplatformGuardTest` fails
  the build on any `java.`, `javax.`, `android.` or JVM-only use.
- `app/`: the Android app, packaged by layer: `data` (Room), `alarms` (AlarmManager, notifications,
  receivers), `ui` (Compose screens and `ui/theme/Theme.kt`).
- `app/src/debug/`: `DevTools` (Settings, Developer card: time scale 1x/10x/60x, invariant checks, the
  `BeeTimerE2E` event log). `app/src/release/` has a no-op twin, so release builds contain none of it.
- `app/schemas/`: Room schema JSON, committed for every database version. A schema change bumps the
  version, adds a migration and a `MigrationTest` case, and commits the new JSON.
- `scripts/e2e/`: end-to-end suite on the emulator (Maestro). See [e2e/README.md](e2e/README.md).
- `scripts/icon/`: converts the icon SVGs to VectorDrawables and pixel-checks them. See
  [icon/README.md](icon/README.md).
- `docs/design/`: TECH-DESIGN, TASKS, WBS. `docs/screenshots/`: app and mockup captures for UI review.

## Versions

Pinned in `gradle/libs.versions.toml`. compileSdk is 36, so Compose, core-ktx and lifecycle are pinned to
the last versions that build against 36 (Compose BOM 2026.06.01, core-ktx 1.18.0, lifecycle 2.10.0). Tests use Robolectric 4.18.1 for the `:app`
unit tests (Room, migrations, the alarm screen, the icon render).
Moving to compileSdk 37 unlocks the newer ones.

## Usage tracking and dashboard (playbook section 9)

- **Status line.** `scripts/statusline.sh` reads the JSON Claude Code sends to its status line command
  (`rate_limits.five_hour` and `rate_limits.seven_day`) and prints model, 5-hour %, weekly % with today's
  weekly budget, reset countdowns and the current task. It writes the numbers to
  `~/.claude/beetimer-usage.json` and then prints the caveman badge it wraps. Wired in `~/.claude/settings.json`:
  `"statusLine": {"type": "command", "command": "bash /home/jason/Documents/git/beetimer/scripts/statusline.sh", "refreshInterval": 60}`.
  To undo: restore `~/.claude/settings.json.bak-2026-11-23-beetimer`.
- **Dashboard.** `python3 scripts/dashboard.py` rewrites `docs/dashboard.html` from TASKS.md, USAGE.md,
  HANDOFF.md (its "Waiting on Jason" list), `build/gate/last.tsv` (written by `scripts/check.sh`), the test
  results, `docs/e2e/REPORT.md` and the usage snapshot. Open it from disk. Never edit the HTML by hand.
- **Timer.** `scripts/dashboard-timer.sh start` creates a transient systemd user timer,
  `beetimer-dashboard.timer`, that runs `dashboard.py` every 15 minutes. `stop` removes it; `status` shows it.
  Transient units leave nothing installed and are gone after logout or reboot. `dashboard.py --timer` stops
  the timer itself if the repo has had no commit for 12 hours. The timer never commits.

## Security scanners (gate)

- `scripts/security.sh semgrep | snyk-oss | snyk-code`, run by `scripts/check.sh`. Semgrep uses the `p/kotlin`
  and `p/secrets` rulesets; Snyk open source tests all Gradle projects at high and above; Snyk Code at high.
- Each fails on any finding, and also when the tool is missing, Snyk isn't logged in (`snyk auth`), the scan
  errors out, or it covers less than exists (Kotlin in both modules; the root plus every Gradle project; every
  tracked `.kt` file). Network errors get one retry.
- Install: Semgrep with `pipx install semgrep` (here: `~/.local/bin/semgrep`); Snyk CLI from snyk.io, then
  `snyk auth`. The Snyk org lives in the Snyk CLI config, never in the repo.
- Build tools pull in old guava, commons-lang3, bouncycastle and logback (Android lint, Room's KSP processor,
  ktlint). `app/build.gradle.kts` and `build.gradle.kts` upgrade them; none of them ship in the APK.
- Open findings that can't be fixed: `docs/design/SECURITY-EXCEPTIONS.md` with scope and expiry.


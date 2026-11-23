# Handoff

Where Bee Timer stands on 2026-11-23, written so a fresh session (person or agent) can pick it up with no other
context. Update this file after each task.

## Start here

- **App:** Bee Timer, native Android (Kotlin, Jetpack Compose, Room, AlarmManager). App ID
  `io.github.mecheng70.beetimer` (permanent). Repo https://github.com/MechEng70/beetimer (public), branch
  `main`, local `~/Documents/git/beetimer`.
- **Version:** 0.1.0-alpha.2, versionCode 100002 (`appVersion` in `gradle.properties`; the code comes from
  `buildSrc/src/main/kotlin/AppVersion.kt`). Release-signed.
- **Releases:** GitHub pre-releases `v0.1.0-alpha.1` (100001) and `v0.1.0-alpha.2` (100002), each with the APK
  and its `.sha256`, same release certificate. Next release: bump `appVersion`, then `scripts/release.sh` with
  the emulator off, then `gh release create` with the APK and `.sha256`, then download it back and check.
- **Jason's phone:** OnePlus CPH2583, Android 16, OxygenOS, font scale 1.15. Runs 0.1.0-alpha.2 from GitHub,
  installed over alpha.1 (hives and settings kept). alpha.1 went on after an `adb uninstall` of the old
  debug-signed alpha (R13). Later release builds install over it; a debug build won't (different key).
- **Round in progress:** none. R11 (playbook adoption), R12 and R13 are done. Wait for Jason's next round.
- **Next:** Nothing scheduled; Jason starts the next round.
- **Working tree:** clean, nothing stashed, `main` level with `origin/main` (2026-11-23 close-out).
- **Gate:** `scripts/check.sh` passes: ktlint, detekt, Android lint, unit tests, buildSrc tests, py-tests,
  Semgrep, Snyk open source (4 of 4 projects), Snyk Code.
- **End to end:** 22 of 22 runs pass (s1 to s11 at font scale 1.15 and 1.3, 10x), 0 invariant errors
  (`docs/e2e/REPORT.md`).

### Waiting on Jason

- Alarm screen with the phone locked, on 0.1.0-alpha.2: let a step end with the screen locked. Readable at
  arm's length, nothing over the buttons, and a step that frees the vaporizer shows only "Then: <hive>,
  <step>" (T-013a, T-021). Compare with `docs/screenshots/app-3-alarm.png`.

Done by Jason on 2026-11-23: alpha.2 over alpha.1 (data kept), themed icons, countdowns (10:00 to 9:59 and
1:00 to 0:59 don't shift).

### Release keystore

- Keystore `~/keys/beetimer-release.jks`, alias `beetimer`, mode 600, outside the repo.
- `keystore.properties` in the repo root (git-ignored, mode 600): `storeFile`, `storePassword`, `keyAlias`,
  `keyPassword`. Never print or commit it.
- Backed up: the `.jks` file and both passwords are in Jason's password manager (Jason confirmed 2026-11-23).
- Certificate SHA-256 `83173e0e3f927356034bbbd6b8f05740ae5cec77d293eb9aba908c4e0bcd28b7`, recorded in
  `config/release-cert-sha256.txt`; `scripts/release.sh` checks every build against it.

### Restart prompt

```
Read CLAUDE.md and docs/HANDOFF.md ("Start here" first), then docs/design/TASKS.md. Run git status,
git log -5 and git stash list, and tell me what's committed and what isn't; tell me what's in any stash before
touching it. Start the dashboard timer (scripts/dashboard-timer.sh start). Then wait for my next round.
```

## Backlog (don't build without asking)

Details in `docs/design/TASKS.md`, "Backlog".
- **B1 Overlap timeline chart** on the All tab: one lane per hive over time, vaporizer spans amber, passive
  teal, from the planner forecast.
- **B2 Number of vaporizers:** the lock becomes a count; queue and planner follow.
- **B3 Dark theme** for the main screens: tokens exist in `Theme.kt`; needs a visual pass and screenshots.
- **B6 Footer with 1 hive done and 2 not started:** nothing running, so the footer is blank (tested current
  behavior in `FooterTest`). Decide what it should say first.
- Also listed: B4 add and delete steps in the step editor (needs UI not in the mockups), B5 session history.

## Recent rounds (2026-11-23)

- **R11** playbook adoption (T-001 to T-020): quality gate, Room migration checksums, corrupt-row quarantine,
  layout audit, E2E device rules and a full 10x run, alarm screen readability, versioning, release script and
  keystore, upgrade path, push, first pre-release. Work order: `docs/design/PLAYBOOK-ADOPTION.md`.
- **R12** (T-021 to T-023): alarm shows "Then: <hive>, <step>" instead of the device note; s1x10 and s2x10
  removed; Snyk covers `buildSrc` by file (CLAUDE.md).
- **R13** (T-024 to T-026): phone on the release build, alarm mockup updated, `v0.1.0-alpha.2` published.
- Earlier rounds R1 to R10 and tasks T1 to T10: `docs/design/TASKS.md`, "Done before playbook".

## Read first

1. `CLAUDE.md`: the rules for this repo (stack, KMP-ready scheduler, strings and theme, Room, copy, gate,
   devices, agents).
2. `SPEC.md` section 1: product decisions. They win over the mockups and the original brief.
3. `docs/design/TECH-DESIGN.md`: architecture, scheduler rules, alarm layer, UI, tests.
4. `docs/design/TASKS.md`: every task with status and commit, open checks, backlog.
5. `docs/mockups/varroa-timer-mockups.html` and `docs/screenshots/`: how it must look, and how it looks now.

## Commands

```
scripts/dashboard-timer.sh start | stop | status        # 15-minute dashboard refresh; start and stop each round
python3 scripts/dashboard.py                            # regenerate docs/dashboard.html (never edit it by hand)
scripts/check.sh                                        # the gate (about 3 min)
./gradlew assembleDebug                                 # always before a debug install
scripts/release.sh                                      # signed release APK in release/ (emulator off)
scripts/build-apk.sh                                    # gate + debug APK in release/ for testing
scripts/e2e/emulator.sh start | stop                    # emulator (stops Gradle daemons first; software GPU)
adb -s emulator-5554 install -r app/build/outputs/apk/debug/app-debug.apk
scripts/e2e/run.sh s1 s2 s3 s4 s5 s6 s7 s8 s9 s10 s11   # emulator only, both font scales, about 2 h
python3 scripts/e2e/analyze.py                          # writes docs/e2e/REPORT.md
scripts/e2e/screens.sh                                  # re-capture docs/screenshots/app-*.png (real time, 10 min)
adb logcat -s BeeTimerE2E BeeTimerInvariant             # debug builds: event log and invariant errors
```

Toolchain setup: `docs/DEVELOPING.md`. E2E details: `docs/e2e/README.md`.

## Usage tracking

- Status line `scripts/statusline.sh`, wired in `~/.claude/settings.json` (backup
  `~/.claude/settings.json.bak-2026-11-23-beetimer`). It writes `~/.claude/beetimer-usage.json`, which the
  throttle check (playbook section 9) and the dashboard read.
- Every task gets a row in `docs/design/USAGE.md` with the throttle decision.

## How it fits together

- `:scheduler` is a pure reducer, `(state, event, now) -> (state, effects)`, Kotlin stdlib only.
  `MultiplatformGuardTest` keeps it ready for KMP and iOS.
- `Planner` runs the same reducer forward to forecast the All tab. `Invariants` is checked by the property
  test and after every commit in debug builds.
- In `app/`, `SchedulerEngine` is the only dispatcher: it reduces, saves to Room, then hands effects to
  `EffectExecutor` (the alarm layer, which is the platform boundary).
- `RingService`, a foreground service of type `systemExempted` that runs only while something is ringing,
  plays sound and vibration on the alarm stream.
- `Delivery` decides foreground vs background behavior: in-app alarm screen and snackbar, or full-screen
  notification and "Vaporizer free" notification.
- Alarm text (`alarms/AlarmText.kt`): when Done frees the vaporizer, the "Then:" line if a hive is next, else
  "Vaporizer frees when you tap Done." (`releaseLines`).
- The low alarm volume check is in `BeeTimerApp.send` (`Event.Start`), using `alarms/VolumeCheck.kt` and
  `Prefs.volumeChecked` (cleared on `Event.NewSession`). Both bottom snackbars share one host,
  `MainActivity.AppSnackbars`.
- All tab display choices live in `ui/`, not the planner: `COMING_UP_COUNT` (3) in `MainScreen.kt`, and the
  footer variant in `ui/Footer.kt` (`footerFor`, `FooterTest`). Sizes and colors are tokens in `Theme.kt`.
- Corrupt rows are quarantined, never crash a read (`QuarantineTest`). Shipped Room schemas and migrations are
  frozen by a checksum test.
- Debug-only tools (time scale, invariant and E2E logging) live in `app/src/debug/DevTools.kt`. The release
  build has a no-op twin.

## Small, not urgent

- Kotlin warning `Condition is always 'true'` at `ui/MainScreen.kt:431` (Hive tab, `counting && ends != null`).
  It was there before R7 and is harmless.
- Exact alarm refused on Android 12 only (access revoked): logged, not shown. Not reachable on Android 13+.
  Fixing it would add UI (TASKS.md, "Found during R11").

## Rules from Jason that aren't obvious from the code

- **Devices:** never uninstall the app, clear its data, or do anything destructive on the phone or the
  emulator without an explicit yes (the harness's own `pm clear` on the emulator is fine). Install with
  `adb install -r` only. Only touch the phone when asked; E2E runs on the emulator.
- **Behavior and design changes:** stop and ask. Small bugs: fix them, add a regression test, and note them.
- **New UI:** nothing that isn't in the mockups or SPEC without asking.
- **E2E honesty:** never loosen or remove an assertion to make a run pass. If a check changes, say so in
  `docs/e2e/FIXES.md`.
- **Process death in tests:** press Home, then `am kill`, then `kill -9` the PID. Never `am force-stop`: it
  cancels all alarms, which doesn't happen in the field.
- **Screenshots:** crop off the status bar (the repo is public). They go in `docs/screenshots/`.
- **Releases and pushes:** outward-facing; do them when a round asks for them.
- **Wrap-up:** commit with the task or round ID, then stop and summarize with anything to test on the phone.

## Gotchas learned the hard way

Phone (OnePlus, OxygenOS):
- **Vibration:** OxygenOS cuts notification vibration to one buzz and ignores `FLAG_INSISTENT`. That's why
  `RingService` makes the noise, not the notification channel.
- **No default alarm tone:** `settings get system alarm_alert` is null, hence the bundled `alarm_beep.wav`
  fallback.
- **Force dark:** OxygenOS force-dark recolored the alarm window. Fixed with `forceDarkAllowed=false`.
- **Fonts:** the system font has no 600 weight, so Inter is bundled.
- **Alarm volume:** it was 1 of 16 on the test phone. Alarms use the alarm stream on purpose; the first
  Start of a session warns with a snackbar when it's under half.

Android:
- **Heads-ups in the foreground:** a full-screen intent shows as a heads-up while the phone is in use, and it
  covers the top bar. That's why the app opens the alarm screen itself in the foreground and puts its own
  notification on the quiet channel.
- **5 s alarm floor:** exact alarms won't fire less than about 5 s ahead. That's why the E2E suite runs at 10x,
  not 60x.
- **Signing:** a release APK won't install over a debug build (`INSTALL_FAILED_UPDATE_INCOMPATIBLE`).
- **Themed icons:** the Pixel launcher themes only the home screen, not the app drawer.

Build, scripts and tests:
- **Stale installs:** `check.sh` doesn't build the APK. Always run `assembleDebug` before an install.
- **Seeding race:** seeding must stay in one Room transaction. Two callers once seeded 6 hives
  (`SeedingTest`).
- **Alarm screen close:** the close logic must stay in `lifecycleScope`, not Compose (`AlarmActivityCloseTest`).
- **Icon render:** `IconRenderTest` needs Robolectric `GraphicsMode.NATIVE`.
- **Shell is zsh:** an unquoted `$VAR` holding `adb -s emulator-5554` is not split into words. Write commands
  out in full. `grep` is ugrep, which rejects some complex patterns; use Python for big scans.
- **Editing files:** never truncate a file and read it back in the same pipeline. Write to a temp file and move
  it into place, check it isn't empty, and `chmod` scripts after (a temp-file rewrite drops the executable bit).
- **pipefail:** under `set -o pipefail`, never pipe into `head`, `grep -m` or `grep -q` (SIGPIPE, exit 141).
  `scripts/tests/test_release_sh.py` guards `release.sh`.
- **Staging:** `git commit` takes the whole index; a `git rm` staged for one task rides along with the next
  commit. Check `git diff --cached --stat` first.

E2E harness:
- **Emulator:** boots on the software GPU (`-gpu swiftshader_indirect`); the host GPU segfaulted twice in long
  runs. Stop the Gradle and Kotlin daemons before booting; never a release build with the emulator up.
- **One batch at a time:** two `run.sh` batches at once kill the Maestro device server.
- **Killing a stuck run:** match the JVM path exactly (`pgrep -f '^/usr/lib/jvm/.*maestro'`). `pkill -f run.sh`
  from a shell whose own command line contains that text kills the shell (exit 144).
- **Scroll then tap:** at font scale 1.3 a scroll can stop with the target under the gesture bar, and the tap is
  lost. Use `centerElement: true`; `analyze.py` fails any run not at 10x.
- **Logcat:** stream it live, or the buffer rotates and loses the start of long runs.
- **Rename hives:** do it through the shell; Maestro text input had cursor problems.
- **Dialog buttons:** they need `testTagsAsResourceId` set on the dialog itself to be visible to Maestro.
- **Emulator keyboard:** Gboard can pop a "Try out your stylus" tutorial over a text field. Then `rename_hive`
  fails with `Argument expected after "tap"`. Tap Cancel on the tutorial and re-run.
- **Screenshot flows:** `scr-*` flows come from `gen_flows.py` (edit it, then run it; never hand-edit the
  YAML). `screens.sh` runs at 1x and waits for real step ends. The All tab scene (`scr-h0`, `scr-h`, `scr-h1`,
  `scr-h2`) clears data again; `scr-h1` is the alarm with the Then line (`app-3-alarm.png`).
- **Emulator alarm volume:** set it with `adb shell cmd audio set-volume 4 <n>` (range 1 to 7). It's at 6, so
  the volume snackbar doesn't show during flows.

## Doc map

| File | What |
|---|---|
| `README.md` | Public overview, features, screenshots |
| `SPEC.md` | Decisions (section 1), failure modes, safety numbers, original brief |
| `docs/design/TECH-DESIGN.md` | How it's built |
| `docs/design/TASKS.md`, `WBS.md` | Tasks with status and commits, open checks, backlog |
| `docs/design/USAGE.md` | Usage log per task |
| `docs/design/SECURITY-EXCEPTIONS.md` | Open security exceptions (none) |
| `docs/design/PLAYBOOK-ADOPTION.md`, `docs/PLAYBOOK.md` | R11 work order and the general process |
| `docs/dashboard.html` | Status dashboard (generated) |
| `docs/INSTALL.md` | Sideloading for users |
| `docs/DEVELOPING.md` | Toolchain, build, versions, releases, keystore |
| `docs/MANUAL-TEST.md` | 15-minute phone script |
| `docs/e2e/` | E2E how-to, REPORT, FIXES, latest screenshots |
| `docs/screenshots/` | App and mockup captures |
| `docs/icon/` | Icon sources, pixel check, launcher captures |

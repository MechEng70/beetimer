# Handoff

Where Bee Timer stands on 2026-11-22, for whoever picks it up next (a person or a fresh agent session).
Update this file at the end of each round.

## State

- **Version:** 0.1.0-alpha, debug-signed. Last round R10 (row layout and volume check) on `master`.
- **Remote:** none yet. The repo has never been pushed; GitHub is planned as `MechEng70/beetimer`.
- **Phone:** the R10 build is installed on Jason's phone (OnePlus CPH2583, Android 16, OxygenOS, font
  scale 1.15).
- **Gate:** `scripts/check.sh` passes, 101 tests.
- **End to end:** 12 of 13 emulator runs pass, with 0 invariant errors (`docs/e2e/REPORT.md`, R6 build).
  The one failure is a 60x timing limit, explained below. R7 to R10 were display or in-app
  notice changes and no flow reads what they changed, so the suite was not re-run.
- **Done:** everything in `docs/design/TASKS.md` up to R10. Jason checked R7 to R10 on the phone on
  2026-11-22: all good.
- **Waiting on:** nothing assigned. The next work is Jason's call.

## Latest rounds (2026-11-22, main screen only)

- **R7** `3f0c6fb`: Coming up has no times and shows the next 3 events (hive and action). Hive rows drop
  "in m:ss" from "Next:" and are larger for outdoors (name 20 sp, step 17, Next 16, countdown chip 20 bold).
- **R8** `6f91a23`: the next step name is an outlined pill, amber for a vaporizer step, teal for passive.
- **R9** `9e12b85`: footer "Estimated finish: 9:15 AM", "... (2 of 3 hives)", or "All hives done at 9:15 AM".
  New screenshot `app-1-all-teal.png`.
- **R10** `282cd5b`: hive rows put the chip or button on line 1 only; the step and Next lines run full width
  under it, and step names are never cut off on the All tab (they wrap, the pill grows). The "Alarm volume
  is low" banner is gone: the first Start of a session with the alarm volume under half shows a 6 s snackbar
  with Raise, once per session (`VolumeCheck`). All tab screenshots now come from the wand-comes-out scene.

## Read first

1. `CLAUDE.md`: the rules for this repo (stack, KMP-ready scheduler, strings and theme, Room, copy).
2. `SPEC.md` section 1: product decisions. They win over the original brief below them.
3. `docs/design/TECH-DESIGN.md`: architecture, scheduler rules, alarm layer, UI, tests.
4. `docs/design/TASKS.md`: task history (T1 to T10, review rounds R1 to R10), T11, open checks, backlog.
5. `docs/mockups/varroa-timer-mockups.html` and `docs/screenshots/`: how it must look, and how it looks now.

## Commands

```
scripts/check.sh                                        # gate: all unit tests + lint (does NOT build the APK)
./gradlew assembleDebug                                 # always before an install
adb install -r app/build/outputs/apk/debug/app-debug.apk
scripts/build-apk.sh                                    # gate + release/beetimer-<version>.apk + sha256
scripts/e2e/run.sh s1 ... s11 s1x10 s2x10               # emulator only; then python3 scripts/e2e/analyze.py
emulator -avd beetimer-api36                            # emulator for E2E and screenshots
scripts/e2e/screens.sh                                  # re-capture docs/screenshots/app-*.png (emulator, real time)
python3 scripts/icon/svg2vd.py && python3 scripts/icon/compare.py   # after changing the icon art
adb logcat -s BeeTimerE2E BeeTimerInvariant             # debug builds: event log and invariant errors
```

Toolchain setup: `docs/DEVELOPING.md`. E2E details: `docs/e2e/README.md`.

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
- The low alarm volume check is in `BeeTimerApp.send` (`Event.Start`), using `alarms/VolumeCheck.kt` and
  `Prefs.volumeChecked` (cleared on `Event.NewSession`). Both bottom snackbars share one host,
  `MainActivity.AppSnackbars`.
- All tab display choices live in `ui/`, not the planner: `COMING_UP_COUNT` (3) in `MainScreen.kt` (the
  planner's `comingUp` default stays 5), and the footer variant in `ui/Footer.kt` (`footerFor`, `FooterTest`).
  Row sizes and pill colors are tokens in `Theme.kt`.
- Debug-only tools (time scale, invariant and E2E logging) live in `app/src/debug/DevTools.kt`. The release
  build has a no-op twin.

## Open items

Needs Jason:
- **Themed icons on the phone:** turn them on (Settings, Wallpapers & style, Icons, Themed icons) and look at
  the home screen. They're verified on the emulator only.
- **Push to GitHub:** create the remote and push. Then upload `docs/icon/icon-1024.png` as the social preview
  under Settings, General, Social preview. GitHub has no API for that.
- **Phone cleanup:** "Hive 4" on the phone: Jason deletes it himself. Don't touch it.

Before the first public release:
- **T11 release keystore:** see TASKS.md. The keystore is never committed, and how to back it up goes in
  DEVELOPING.md.

Small, not urgent:
- Kotlin warning `Condition is always 'true'` at `ui/MainScreen.kt:324` (Hive tab, `counting && ends != null`).
  It was there before R7 and is harmless.

Backlog (don't build without asking): B1 timeline chart, B2 more than one vaporizer, B3 dark theme pass, B4
add and delete steps in the editor, B5 session history.

Deferred tooling: Semgrep, Snyk, ktlint and detekt until after alpha (Jason, 2026-11-21).

## Rules from Jason that aren't obvious from the code

- **Phone:** never uninstall the app, clear its data, or do anything destructive on the phone without an
  explicit yes. Install with `adb install -r` only. Only touch the phone when asked; E2E runs on the emulator.
- **Behavior and design changes:** stop and ask. Small bugs: fix them, add a regression test, and note them.
- **New UI:** nothing that isn't in the mockups or SPEC without asking.
- **E2E honesty:** never loosen or remove an assertion to make a run pass. If a check changes, say so in
  `docs/e2e/FIXES.md`.
- **Process death in tests:** press Home, then `am kill`, then `kill -9` the PID. Never `am force-stop`: it
  cancels all alarms, which doesn't happen in the field.
- **Screenshots:** crop off the status bar (the repo is public). They go in `docs/screenshots/`.
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
- **5 s alarm floor:** exact alarms won't fire less than about 5 s ahead. This only matters at the debug 60x
  scale, so s3 runs at 10x.
- **s2 at 60x:** fails the "sealed hold overlaps the next hive's heat" check because the 10 s hold is too
  short for the autopilot. It passes at 10x (s2x10). The check was left as a FAIL, not loosened.
- **Themed icons:** the Pixel launcher themes only the home screen, not the app drawer.

Build and tests:
- **Stale installs:** `check.sh` doesn't build the APK. A stale APK got installed once, so always run
  `assembleDebug` first.
- **Seeding race:** seeding must stay in one Room transaction. Two callers once seeded 6 hives
  (`SeedingTest`).
- **Alarm screen close:** the close logic must stay in `lifecycleScope`, not Compose. In Compose it was
  dispatcher-order dependent, and the in-app alarm closed to the launcher (`AlarmActivityCloseTest`).
- **Icon render:** `IconRenderTest` needs Robolectric `GraphicsMode.NATIVE`.

E2E harness:
- **One batch at a time:** two `run.sh` batches at once kill the Maestro device server.
- **Killing a stuck Maestro:** match the JVM path exactly (`pgrep -f '^/usr/lib/jvm/.*maestro'`). Looser
  `pkill` patterns matched the calling shell and killed it (exit 144).
- **Logcat:** stream it live, or the buffer rotates and loses the start of long runs.
- **Rename hives:** do it through the shell; Maestro text input had cursor problems.
- **Dialog buttons:** they need `testTagsAsResourceId` set on the dialog itself to be visible to Maestro.
- **Emulator keyboard:** Gboard on the emulator can pop a "Try out your stylus" tutorial over a text field.
  Then `rename_hive` fails with `Argument expected after "tap"`. Tap Cancel on the tutorial and re-run.
- **Screenshot flows:** `scr-*` flows come from `gen_flows.py` (edit it, then run it; never hand-edit the
  YAML). `screens.sh` runs at 1x and waits for real step ends. The All tab scene (`scr-h0`, `scr-h`,
  `scr-h2`) clears data again and takes about 6 minutes; it must reach `scr-h2` before Herb's 10 min Keep
  sealed ends. A hive sealed while queued goes straight to Load dose on Start (no Seal hive step).
- **Emulator alarm volume:** set it with `adb shell cmd audio set-volume 4 <n>` (range 1 to 7).
  `cmd media_session volume --set` reports success but doesn't change it. It's at 6, so the volume snackbar
  doesn't show during flows. To re-take only some shots, copy the part you need into a scratch
  script.

## Doc map

| File | What |
|---|---|
| `README.md` | Public overview, features, screenshots |
| `SPEC.md` | Decisions (section 1) and the original brief |
| `docs/design/TECH-DESIGN.md` | How it's built |
| `docs/design/TASKS.md`, `WBS.md` | Task history, open checks, backlog |
| `docs/INSTALL.md` | Sideloading for users |
| `docs/DEVELOPING.md` | Toolchain, build, layout, versions |
| `docs/MANUAL-TEST.md` | 15-minute phone script |
| `docs/e2e/` | E2E how-to, REPORT, FIXES, per-scenario logs |
| `docs/screenshots/` | App and mockup captures |
| `docs/icon/` | Icon sources, pixel check, launcher captures |

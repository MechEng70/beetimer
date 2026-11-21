# Tasks (alpha)

Target: a field-usable APK today.

Each task is sized to pick up cold: read `CLAUDE.md`, `docs/design/TECH-DESIGN.md`, and the task below. Commit with the task ID. Stop after each STOP task for review.

Status: [ ] todo, [~] in progress, [x] done.

## T1 [x] Scaffold the build
- Gradle wrapper 9.6.0 (bootstrap from the cached Gradle 9.4.1: `~/.gradle/wrapper/dists/gradle-9.4.1-bin/*/gradle-9.4.1/bin/gradle wrapper --gradle-version 9.6.0`).
- `settings.gradle.kts` with `:scheduler` (kotlin-jvm, KMP-ready) and `:app` (AGP 9.5.2, built-in Kotlin, Compose compiler plugin, KSP, kotlinx-serialization).
- `gradle/libs.versions.toml` with the versions pinned in TECH-DESIGN section 2.
- Empty MainActivity showing "Bee Timer". `scripts/check.sh` (tests, lint).
- Done when: `./gradlew :scheduler:test assembleDebug` succeeds.

## T1b [x] Alarm spike (STOP after): passed on the phone after the RingService fix
- Minimal APK: one button schedules an exact `setAlarmClock` alarm 60 s out. When it fires, post a high-importance alarm notification with a full-screen intent to a lock-screen activity, with insistent alarm sound and a Stop button.
- Permissions: `POST_NOTIFICATIONS` request, `USE_EXACT_ALARM`, `USE_FULL_SCREEN_INTENT` check with a settings link.
- STOP: you test it on the locked phone. This code is reused by T5 and T6.

## T2 [x] Scheduler model, presets, validation
- `:scheduler` model classes (TECH-DESIGN section 4), `@Serializable`, stdlib only. `Template` add/delete/move step functions with tests.
- Presets "OAV pan wand" and "OAV gun type" exactly as in the spec. `TemplateValidator` (section 6).
- Tests: presets validate; empty template rejected; duration bounds; split device block rejected; zero device steps allowed.

## T3 [x] Scheduler reducer and restore (STOP after): 46 tests green
- `Scheduler` interface and `DefaultScheduler.reduce` / `restore` implementing TECH-DESIGN section 5 rules 1 to 12.
- Write the tests first: every required test in TECH-DESIGN section 11, plus AddMinute, Snooze, Skip, and the TIMED-to-TIMED acknowledge path.
- Done when: `./gradlew :scheduler:test` is all green. STOP: summary of coverage for review.

## T4 [x] Persistence
- Room entities and DAOs (TECH-DESIGN section 7, no history table in alpha), Repository, seeding (the two presets, OAV pan wand active, 3 hives "Hive 1..3").
- `exportSchema = true`; commit `app/schemas/.../1.json`.
- Scheduler state saved as a JSON row, written in one transaction per dispatch.

## T5 [x] Engine, alarms, receivers
- `SchedulerEngine` (mutex, reduce, persist, effects). `AlarmScheduler` (`setAlarmClock` for step ends, exact-while-idle for snooze and re-ring).
- `AlarmReceiver`, `ActionReceiver`, `BootReceiver` (boot, time set, timezone, package replaced, all calling restore).
- Manifest permissions: `USE_EXACT_ALARM`, `POST_NOTIFICATIONS`, `USE_FULL_SCREEN_INTENT`, `RECEIVE_BOOT_COMPLETED`, `VIBRATE`, `WAKE_LOCK`, `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`.

## T6 [x] Notifications and alarm screen
- Channels `alarms` (high importance, alarm audio, vibration, insistent, full-screen intent) and `status` (low).
- Ringing notification with Done, +1 min, and Snooze 30 s actions. Ring for 2 minutes, then silent for 2 minutes, then ring again.
- `AlarmActivity` per mockup 3 (dark amber, 64 dp buttons, over the lock screen).
- Ongoing status notification, one line per active hive.

## T7 [x] Main UI
- Theme: amber, teal, and gray roles, light and dark, large type.
- Device status bar, scrollable tabs (All + one per hive), All tab (mockup 1), Hive tab (mockup 2). Contextual actions, and Skip/Remove on the queue head.
- Keep screen on while any run is active (when the setting is on).

## T8 [x] Settings (later reworked in R2 and R5: drag reorder, max 5, full step editor with markers)
- Hives: quick-add N, rename (free text, e.g. Emory, Herb, Sparky), note, up/down reorder, delete with confirmation.
- Template editor (mockup 4), alpha: preset toggle (Pan wand / Gun type) and duration edits on TIMED steps only. No add, delete, or reorder of steps. Reset to defaults. Footer text.
- Theme picker, keep-screen-on toggle, New session with confirmation. History is deferred.

## T9 [x] First-run permissions
- `POST_NOTIFICATIONS` request, exact alarm check, full-screen intent check with a one-screen explainer, and a one-time battery optimization ask.

## T10 [x] Build and docs (STOP after): field APK ready for phone test
- `./gradlew test lint assembleDebug` green.
- README: Fedora toolchain, build, and sideload (`adb install -r app/build/outputs/apk/debug/app-debug.apk` or "Install unknown apps").
- `docs/MANUAL-TEST.md`: a 10-minute phone script with a 10 s template and Emory, Herb, and Sparky.
- Known limitations, plus proposed features list. STOP for review with install steps.

## Review rounds after T10 (all done, on the phone)

Each round was a review from Jason with an explicit list. TECH-DESIGN and SPEC section 1 describe the result.

## R1 [x] UI rework to the mockups (a6a203a)
- Alarm sound on the alarm stream (USAGE_ALARM). Seeding in one transaction (regression: 6 hives). UI matched to the mockups with screenshot comparison.

## R2 [x] Planning view, step editor, pan wand fix, Inter (0f40d86)
- All tab planning view from `Planner`. Full step editor. Pan wand preset fix plus `MIGRATION_1_2`. Inter bundled. Hive drag, add and remove, max 5. Alarm screen and heads-up layout. Volume banner. Top bar icons. Backlog B1 to B3.

## R3 [x] Countdowns and copy (022ea21)
- All tab shows time remaining, "Now" for passed times, footer clock in the phone's 12/24-hour format. Heads-up title is the hive name. "Alarm volume is low" with Raise.

## R4 [x] Cancel and wording (0dc7c89)
- Cancel returns the hive to Not started. "Wand" became "Vaporizer" in UI copy (step names keep "wand").

## R5 [x] Take/release markers, planner firm fix, E2E suite (4350039)
- Marker step model replaces Device/Passive toggles, `MIGRATION_2_3`. Sealed-hold option for the pan wand. Planner firm rule fixed. Debug DevTools (time scale, invariants, E2E log). Maestro suite s1 to s10 on the emulator.

## R6 [x] Foreground alarms, snackbar, icon (7c6bc8e)
- App in the foreground: alarm screen opens directly, "Vaporizer free" is a snackbar. Scenario 11 (Home and lock while ringing). s2 at 10x with predicted vs actual. Fixed: in-app alarm screen closed to the launcher (`AlarmActivityCloseTest`). Stopwatch bee adaptive icon with monochrome layer, pixel-checked against the SVGs.

## After alpha

## T11 [ ] Release keystore before the first public GitHub Release
- Generate a release keystore stored outside the repo (never committed; `*.jks`, `*.keystore`, `keystore.properties` are git-ignored).
- Signing config reads `keystore.properties` from outside the repo or from environment variables.
- `docs/DEVELOPING.md`: how to generate it, where it lives, and how to back it up (keystore file plus passwords, kept somewhere other than GitHub). Losing it means users must uninstall to update.
- Must be done before the first public GitHub Release; the alpha APK is debug-signed.

## Open checks (need Jason)
- Themed icons on the OnePlus: Settings, Wallpapers & style, Icons, Themed icons, then look at Bee Timer on the home screen.
- After the first push: upload `docs/icon/icon-1024.png` as the GitHub social preview (no API).

## Backlog (not scheduled; do not build yet)

- **B1 Overlap timeline chart** on the All tab, Pitplanner style: one lane per hive across time, device-bound spans in amber, passive in teal, from the planner forecast.
- **B2 Number of vaporizers** setting: the device lock becomes a count (N runs may be in device steps at once); queue and planner follow.
- **B3 Dark theme for the main screens**: tokens exist in `Theme.kt`; needs a visual pass against the mockup semantics and its own screenshots.
- **B4 Add and delete steps in the step editor**: the model has `addStep` and `removeStep`; needs UI that isn't in the mockups, so ask first.
- **B5 Session history**: deferred from the brief.

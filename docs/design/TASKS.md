# Tasks (alpha)

Each task is sized to pick up cold: read `CLAUDE.md`, `docs/design/TECH-DESIGN.md`, and the task below. Commit with the task ID. Stop after each STOP task for review.

Status: [ ] todo, [~] in progress, [x] done.

## T1 [ ] Scaffold the build
- Gradle wrapper 9.6.0 (bootstrap from the cached Gradle 9.4.1: `~/.gradle/wrapper/dists/gradle-9.4.1-bin/*/gradle-9.4.1/bin/gradle wrapper --gradle-version 9.6.0`).
- `settings.gradle.kts` with `:core` (kotlin-jvm) and `:app` (AGP 9.5.2, built-in Kotlin, Compose compiler plugin, KSP, kotlinx-serialization).
- `gradle/libs.versions.toml` with the versions pinned in TECH-DESIGN section 2.
- Empty MainActivity showing "Varroa OAV Timer". `scripts/check.sh` (tests, lint, Semgrep, Snyk).
- Done when: `./gradlew :core:test assembleDebug` succeeds.

## T2 [ ] Core model, presets, validation
- `:core` model classes (TECH-DESIGN section 4), `@Serializable`.
- Presets A and B exactly as in the spec. `TemplateValidator` (section 6).
- Tests: presets validate; empty template rejected; duration bounds; split device block rejected; zero device steps allowed.

## T3 [ ] Scheduler reducer and restore (STOP after)
- `Scheduler.reduce` and `Scheduler.restore` implementing TECH-DESIGN section 5 rules 1 to 12.
- Write the tests first: every required test in TECH-DESIGN section 11, plus AddMinute, Snooze, Skip, and the TIMED-to-TIMED acknowledge path.
- Done when: `./gradlew :core:test` is all green. STOP: summary of coverage for review.

## T4 [ ] Persistence
- Room entities and DAOs (TECH-DESIGN section 7), Repository, seeding (the two presets, Pan wand active, 3 hives "Hive 1..3").
- Scheduler state saved as a JSON row, written in one transaction per dispatch.

## T5 [ ] Engine, alarms, receivers
- `SchedulerEngine` (mutex, reduce, persist, effects). `AlarmScheduler` (`setAlarmClock` for step ends, exact-while-idle for snooze and re-ring).
- `AlarmReceiver`, `ActionReceiver`, `BootReceiver` (boot, time set, timezone, package replaced, all calling restore).
- Manifest permissions: `USE_EXACT_ALARM`, `POST_NOTIFICATIONS`, `USE_FULL_SCREEN_INTENT`, `RECEIVE_BOOT_COMPLETED`, `VIBRATE`, `WAKE_LOCK`, `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`.

## T6 [ ] Notifications and alarm screen
- Channels `alarms` (high importance, alarm audio, vibration, insistent, full-screen intent) and `status` (low).
- Ringing notification with Done, +1 min, and Snooze 30 s actions. Ring for 2 minutes, then silent for 2 minutes, then ring again.
- `AlarmActivity` per mockup 3 (dark amber, 64 dp buttons, over the lock screen).
- Ongoing status notification, one line per active hive.

## T7 [ ] Main UI
- Theme: amber, teal, and gray roles, light and dark, large type.
- Device status bar, scrollable tabs (All + one per hive), All tab (mockup 1), Hive tab (mockup 2). Contextual actions, and Skip/Remove on the queue head.
- Keep screen on while any run is active (when the setting is on).

## T8 [ ] Settings
- Hives: quick-add N, rename (free text, e.g. Emory, Herb, Sparky), note, up/down reorder, delete with confirmation.
- Template editor (mockup 4): preset toggle; add, delete, and up/down steps; field editor; validation errors inline; reset to defaults; footer text.
- Theme picker, keep-screen-on toggle, New session with confirmation, History list.

## T9 [ ] First-run permissions
- `POST_NOTIFICATIONS` request, exact alarm check, full-screen intent check with a one-screen explainer, and a one-time battery optimization ask.

## T10 [ ] Build and docs (STOP after)
- `./gradlew test lint assembleDebug` and `scripts/check.sh` green, or exceptions logged.
- README: Fedora toolchain, build, and sideload (`adb install -r app/build/outputs/apk/debug/app-debug.apk` or "Install unknown apps").
- `docs/MANUAL-TEST.md`: a 10-minute phone script with a 10 s template and Emory, Herb, and Sparky.
- Known limitations, plus proposed features list. STOP for review with install steps.

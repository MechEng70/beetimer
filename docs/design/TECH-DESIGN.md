# Bee Timer: technical design (alpha)

Status: alpha built and on the phone (0.1.0-alpha). This doc describes the code as it is; where it and the code disagree, fix one of them. Current state and open items: `HANDOFF.md`.

Source of truth for requirements: `SPEC.md` (section 1 Decisions wins over the original brief). Approved screens: `docs/mockups/varroa-timer-mockups.html`. This doc records decisions and the answers to open questions, so a fresh session can pick up any task in `TASKS.md` cold.

## 1. Goal and alpha scope

Offline Android app that times oxalic acid vaporization across several hives sharing one vaporizer. One shared, editable step template. The vaporizer is a lock: only one hive may be in a device-bound step at a time.

Alpha must demo, on the phone:
- Hives with user-chosen names (e.g. Emory, Herb, Sparky): add, rename, reorder, delete, at most 5.
- Start several hives, vaporizer queue, manual handoff ("Vaporizer free: Herb is next"), passive overlap.
- Loud alarms that fire with the screen off and show over the lock screen.
- State survives app kill and reboot.
- Step editor with the two presets, take and release markers, and validation.
- A planning view (All tab) with countdowns from the planner.

Alpha cuts (deliberate; see section 10):
- No release build or keystore (debug APK only; T11).
- No ktlint, detekt, Semgrep, or Snyk yet.
- The step editor renames, retimes, moves steps and moves markers. No add or delete of steps in the UI (the model supports both).
- No history list.
- Ongoing notification shows end clock times per hive plus one live countdown to the next event, not a ticking countdown per line.

## 2. Toolchain (verified 2026-11-21)

Installed on the build host already, no sudo needed:
- JDK 21 (Temurin 21.0.13) for Gradle and the toolchain. AGP 9.5 needs 17 or newer.
- `ANDROID_HOME=~/Android/Sdk`: `platforms;android-36`, `build-tools;36.0.0`, `platform-tools` 37.0.1.
- `adb` at `/usr/bin/adb`.

Pinned versions (checked against Google Maven, Maven Central, the Gradle plugin portal and the AGP release notes):

| Item | Version | Note |
|---|---|---|
| Gradle wrapper | 9.6.0 | AGP 9.5 minimum and default |
| Android Gradle Plugin | 9.5.2 | Built-in Kotlin, so no `kotlin-android` plugin in `:app` |
| Kotlin (JVM plugin for `:scheduler`, Compose and serialization plugins) | 2.4.21 | |
| KSP | 2.3.14 | Room annotation processing |
| Room (+ `androidx.room` Gradle plugin) | 2.8.5 | `exportSchema` on, JSON in `app/schemas/`, committed from version 1. Room 3 exists; revisit after alpha. |
| Compose BOM | 2026.06.01 | Compose UI 1.11.4, Material 3 1.4.0. Newer BOMs (Compose 1.12+) need compileSdk 37. |
| activity-compose | 1.13.0 | |
| lifecycle-runtime-compose / viewmodel-compose | 2.10.0 | 2.11+ needs compileSdk 37 |
| core-ktx | 1.18.0 | 1.19+ needs compileSdk 37 |
| kotlinx-coroutines | 1.11.0 | `:app` only |
| kotlinx-serialization-json | 1.13.0 | Multiplatform; used by `:scheduler` |
| lifecycle-process | 2.10.0 | `ProcessLifecycleOwner` for the foreground check |
| JUnit | 4.13.2 | |
| Robolectric | 4.18.1 | `:app` unit tests (Room, migrations, alarm screen close, icon render) |
| androidx.test core-ktx | 1.7.0 | |
| room-testing | 2.8.5 | `MigrationTest` |

SDK levels: `minSdk 31`, `targetSdk 36`, `compileSdk 36`. compileSdk only affects the build; moving it to 37 later unlocks the newer libraries without changing behaviour on the phone.

App ID `io.github.mecheng70.beetimer` (permanent). Display name "Bee Timer".

Build commands:
- `./gradlew :scheduler:test`: scheduler tests (pure Kotlin, fast).
- `./gradlew test lint assembleDebug`: everything, output `app/build/outputs/apk/debug/app-debug.apk`.
- `scripts/check.sh`: the alpha quality gate (tests, lint).

## 3. Module layout and platform boundary

```
settings.gradle.kts, gradle/libs.versions.toml, gradle/wrapper/
scheduler/   (kotlin-jvm today, KMP-ready; package io.github.mecheng70.beetimer.scheduler)
  Model.kt        Step, StepKind, Template (derive, add/delete/move/update ops), HiveRun, Ringing, RunState, SchedulerState
  Presets.kt      pan wand (stays in, comes out), gun type, panWandHoldOf
  Validation.kt   TemplateValidator
  Scheduler.kt    interface Scheduler { reduce; restore }, Event, Effect, DefaultScheduler, SchedulerTiming
  Planner.kt      forecast, nextUp, deviceFreeAt (All tab)
  Invariants.kt   shared by PropertyTest and the debug app
  test/           SchedulerTest, RestoreTest, TemplateTest, PlannerTest, PropertyTest, MultiplatformGuardTest
app/         (Android; package io.github.mecheng70.beetimer, by layer)
  BeeTimerApp.kt  hand-built object graph, foreground observer
  data/       Db (Room v3, migrations), Repository, Prefs
  alarms/     SchedulerEngine, EffectExecutor, Delivery, AlarmScheduler, Receivers, RingService, AlarmSound,
              Notifier, AlarmText, Channels
  ui/         MainActivity, MainScreen, SettingsScreen, AlarmActivity, Permissions, Common, theme/ (Theme.kt, Icons.kt)
  src/debug/  DevTools (time scale, invariant and E2E logging); src/release/ has a no-op twin
  res/values/strings.xml   every UI string
scripts/     check.sh (gate), build-apk.sh, e2e/ (Maestro suite), icon/ (SVG to VectorDrawable, pixel compare)
```

**KMP-ready scheduler (iOS later).** KMP is not set up now, but `:scheduler` is written so it can become a multiplatform `commonMain` with no code changes:
- Kotlin stdlib and kotlinx.serialization only. No `java.*`, `javax.*`, `android.*`, `androidx.*`, no Java time APIs, no `@Jvm*` or `@Synchronized`, no threads, no JVM-only collections or concurrency.
- Time is always an epoch-millisecond `Long` passed in as `now`. The scheduler never reads a clock.
- IDs are `Long`s from the app layer (no `UUID`).
- `MultiplatformGuardTest` scans `scheduler/src/main` and fails `./gradlew test` on any of the above.
- The app depends on the `Scheduler` interface, not the implementation.

**Platform boundary: the alarm layer.** Everything platform-specific sits behind the effect executor in `alarms/`: `ScheduleAlarm`, `CancelAlarm`, `Ring`, `StopRing`, `NotifyDeviceFree`. On Android this is AlarmManager (`setAlarmClock`) plus notifications with a full-screen intent. On iOS 26+ it would be AlarmKit, which provides system alarms that break through silent mode and Focus. The scheduler and its effects are the same on both.

## 4. Domain model (`:scheduler`)

```kotlin
enum class StepKind { TIMED, MANUAL }
data class Step(val name: String, val kind: StepKind, val durationSeconds: Int?, val deviceBound: Boolean,
                val instruction: String, val takes: Boolean, val releases: Boolean)  // deviceBound derived from markers
data class Template(val id: String, val name: String, val steps: List<Step>)

enum class RunState { NOT_STARTED, WAITING_FOR_DEVICE, RUNNING, AWAITING_CONFIRM, DONE, CANCELLED }  // CANCELLED: old saved state only

data class HiveRun(
  val hiveId: Long,
  val steps: List<Step>,          // template snapshot taken at Start; later edits never touch it
  val currentStepIndex: Int,
  val state: RunState,
  val stepStartedAt: Long?,       // epoch ms
  val stepEndsAt: Long?,          // epoch ms, TIMED + RUNNING only
  val ringing: Ringing?,          // non-null while an alarm needs acknowledging
  val startedAt: Long?,
  val completedAt: Long?,
)
data class Ringing(val finishedStepIndex: Int, val since: Long, val overdue: Boolean, val nextRingAt: Long)

data class SchedulerState(
  val runs: Map<Long, HiveRun>,   // a hive with no run is NOT_STARTED
  val lockHolder: Long?,          // hive in a device-bound step
  val queue: List<Long>,          // FIFO of hives waiting for the device
  val template: Template,         // used by the next Start
  val timeScale: Int = 1,         // debug only; scaled(ms) divides timed durations, +1 min, Snooze
)
```

Hives (id, label, note, position) live in Room, not in the scheduler. Hive labels are free text and are what every message shows ("Vaporizer free: Herb is next"). "+ Add hive" creates a "Hive N" default, which you can rename.

## 5. Scheduler rules (pure reducer)

`Scheduler.reduce(state, event, now): Result(newState, effects)`. No clocks, no I/O. `now` is always passed in.

Events: `Start(hive)`, `Done(hive)`, `TimerFired(hive)`, `AddMinute(hive)`, `Snooze(hive)`, `Skip(hive)`, `Cancel(hive)`, `QueueSkipHead`, `QueueRemoveHead`, `NewSession`, `TemplateChanged(template)`, `TimeScaleChanged(scale)` (debug). Done covers acknowledging too: on a finished timed step it starts the next step, on a manual step it completes it, otherwise it just silences the alarm.

Effects: `ScheduleAlarm(hive, atMs)`, `CancelAlarm(hive)`, `Ring(hive, finishedStepIndex, overdue)`, `StopRing(hive)`, `NotifyDeviceFree(hive)`. Alarm effects are derived: each run needs at most one alarm (`ringing.nextRingAt` if ringing, else `stepEndsAt` if RUNNING), and the reducer emits Schedule or Cancel when that changes. A ringing run re-rings every 4 minutes (2 min ringing plus 2 min quiet) until acknowledged; Snooze moves the next ring to now + 30 s. Snooze and +1 min are divided by `timeScale`; the ring cycle is not. After every reduce, the engine also refreshes the ongoing notification and the UI.

There is one pending alarm per hive (request code = hive id). Scheduling a new one replaces the old one.

Rules:
1. **Lock.** The lock is acquired on entering the first device-bound step and released on leaving the last one, or on cancel or skip out of the block. Invariant: `lockHolder` is the only run whose current step is device-bound.
2. **Start (NOT_STARTED).** The run snapshots the active template and enters step 0. Leading non-device steps run normally (MANUAL waits for Done, TIMED counts down).
3. **Reaching the device block.**
   - If `lockHolder == null` and the queue is empty: take the lock and enter the step.
   - Otherwise: append to the queue and go to `WAITING_FOR_DEVICE`.
4. **Release.** The lock clears. If the queue is non-empty, emit `NotifyDeviceFree(head)`. The head stays `WAITING_FOR_DEVICE` until `Start(head)`, because you move the device by hand. While the queue is non-empty, nobody else may take the lock (the lock is reserved for the head). `QueueSkipHead` moves the head to the back and notifies the new head. `QueueRemoveHead` resets the head's run to NOT_STARTED (approved) and notifies the next head.
5. **TIMED step end (`TimerFired`).** Decided by question 4:
   - Next step is MANUAL: advance right away to that step (`AWAITING_CONFIRM`) and `Ring`. Alarm Done = Done on that MANUAL step (question 5). For example, after Cool in hive ends, Done on the alarm completes Remove wand and frees the device.
   - Next step is TIMED: stay on the finished step in `AWAITING_CONFIRM` with `ringing` set, and `Ring`. `Acknowledge` starts the next TIMED step at the moment of the tap (e.g. Cool starts after you switch power off).
   - No next step: `DONE`.
6. **AddMinute.**
   - On a RUNNING TIMED step: `stepEndsAt += 60 s`.
   - While ringing: reopen the finished step with `stepEndsAt = now + 60 s`, go back to RUNNING, and `StopRing`. If we had already advanced to a MANUAL step, step back.
7. **Snooze.** `StopRing` now and `ScheduleAlarm(now + 30 s)`. When it fires it rings again with no state change.
8. **Skip.** Behaves like the step finishing, with no ring. Lock rules apply, so skipping the last device step releases the lock.
9. **Cancel.** `CancelAlarm`, `StopRing`, release the lock if held (then rule 4), remove from the queue if queued (lock untouched), and remove the run, so the hive is NOT_STARTED again (Jason, 2026-11-21).
10. **Template edits.** These change `template` only. Runs keep their snapshot.
11. **NewSession.** Clears all runs and the queue. Hives keep their names.
12. **Restore(state, now).** Used on app start, boot, and time change.
    - For each RUNNING TIMED run with `stepEndsAt <= now`: apply rule 5 as if it fired at `stepEndsAt`, with `overdue = true` (the alarm shows "overdue", nothing is silently skipped).
    - For future ends: `ScheduleAlarm`.
    - Ringing runs ring again only when `nextRingAt` is due, so a snooze survives process death.
    - An alarm that fires within 60 s of its time is not overdue (same grace as a live alarm).
    - Old saved `CANCELLED` runs are dropped.
    - Because a TIMED-to-TIMED handoff waits for acknowledgement, missed alarms never cascade through several steps.


## 6. Step model and validation

**Take and release markers** (Jason, 2026-11-21; replaces per-step Device/Passive toggles). Each template has exactly one step marked "takes the vaporizer" and one marked "releases the vaporizer" (`Step.takes`, `Step.releases`). Device-bound steps are derived (`Template.derived()`): every step from the take step through the release step, inclusive, is Device; every other step is Passive. The scheduler still reads `Step.deviceBound`, which is always the derived value. Moving a step carries its markers and duration with it and re-derives Device/Passive; renaming never changes durations.

Validation:
- At least one step.
- TIMED steps need a duration from 1 s to 86,400 s.
- Exactly one take and one release (`MISSING_MARKER`).
- The release step must not be above the take step (`RELEASE_BEFORE_TAKE`). This is the only invalid arrangement; a split device block can no longer happen.

**Presets.**
- OAV pan wand, wand stays in: Seal hive; Load dose (takes); Heat and vaporize 150 s; Cool in hive 120 s; Keep sealed 600 s ("Wand stays in. Keep hive sealed."); Remove wand (releases); Unseal.
- OAV pan wand, wand comes out: Seal hive; Load dose (takes); Heat and vaporize 150 s; Cool in hive 120 s; Remove wand (releases, "Pull the wand and close the entrance."); Keep sealed 600 s ("Wand is out. Keep hive sealed."); Unseal.
- OAV gun type: Seal hive; Load cap and inject (takes); Vaporize 20 s; Withdraw (releases); Keep sealed 600 s; Unseal.

The sealed-hold choice in Settings is read from the step list's shape (`Presets.panWandHoldOf`: names, kinds and markers in order), so reordering by hand into either default shape selects it. Choosing applies that default list, with a confirm when the steps are not exactly a default list. Room v3 (`MIGRATION_2_3`) rewrote both preset rows to this model.

## 7. Persistence (Room)

To keep it simple and atomic:
- `hives(id, label, note, position)`
- `templates(id, name, stepsJson, isActive)`: two rows.
- `scheduler_state(id = 1, json)`: the whole `SchedulerState` (minus the template) serialized with kotlinx.serialization. One row, written in the same transaction after every reduce.

UI preferences (theme, keep screen on, battery prompt already shown) live in SharedPreferences.

Schema versions (JSON committed in `app/schemas/`; never edit a committed one):
- v1: first release of the tables.
- v2 (`MIGRATION_1_2`): reseeds the pan wand preset (fixed step list).
- v3 (`MIGRATION_2_3`): rewrites both preset rows to the take/release marker model.
`MigrationTest` runs both migrations.

## 8. Android layer

**Wiring.** `BeeTimerApp` builds the graph by hand (no DI library): Room `BeeTimerDb`, `Repository`, `Prefs`, `AlarmScheduler`, `Notifier`, `EffectExecutor`, `SchedulerEngine`. Startup seeds once and loads state, which runs `restore`. Seeding runs in one Room transaction and only when the templates table is empty, so presets and the 3 default hives go in together or not at all, and a second caller sees them (regression: two callers raced and seeded 6 hives; `SeedingTest`).

**SchedulerEngine** (app singleton) is the only place events are dispatched. It talks to the platform through `EffectRunner` (Android: `EffectExecutor`), so tests can run it with a no-op runner. It does mutex, load state, `reduce(now = System.currentTimeMillis())`, persist, then execute effects. It exposes a `StateFlow<SchedulerState>` for the UI.

**Alarms**
- Step ends use `AlarmManager.setAlarmClock()`. The show intent opens MainActivity.
- Snooze and re-ring use `setExactAndAllowWhileIdle()`.
- Manifest: `USE_EXACT_ALARM`. Check `canScheduleExactAlarms()` at startup anyway, and route to settings if it returns false.
- `AlarmReceiver` uses `goAsync()` and dispatches `TimerFired` or the re-ring.

**Boot and time changes.** `BootReceiver` handles `BOOT_COMPLETED`, `TIME_SET`, `TIMEZONE_CHANGED`, and `MY_PACKAGE_REPLACED`, and calls `restore`. Restore re-rings a ringing alarm only when its next ring is due (a snooze survives process death), and an alarm that starts the process on time is not overdue (60 s grace, same as a live alarm).

**Multiple hives ringing.** `RingService` keeps one sound loop for all ringing hives, one notification per hive (the newest is the foreground one), and stops when the last is acknowledged. `AlarmActivity` shows the hive it was opened for, then any other ringing hive, then closes.

**Vaporizer free.** Channel `device_v1` (high importance, default notification sound): "Vaporizer free: Herb is next", "Move the vaporizer, then tap Start.", with a Start button. Cleared when the vaporizer is taken or the queue empties. Only posted when the app is in the background; in the foreground it is a snackbar (section 9).

**Ringing (revised after the T1b phone test, OnePlus CPH2583, Android 16)**
- Findings: (1) OxygenOS replaced the channel's vibration and ignored `FLAG_INSISTENT`, so the phone buzzed once (`OplusVibrationEffectSegment ... repeat=-1`). (2) The system default alarm tone is unset on this phone (`settings get system alarm_alert` is null), so a player set to "default alarm" never started.
- So the notification no longer makes the noise. `RingService` (a foreground service that runs only while something is ringing) plays the sound and vibration itself through `AlarmSound`.
- Service type `systemExempted`: Android allows it for apps that hold `USE_EXACT_ALARM` and keep an alarm going in the background. It is started from the exact-alarm receiver, which is exempt from background-start limits. The spec allows a service when testing shows alarms are unreliable without one; this test showed that.
- Sound: the phone's alarm tone if one is set and plays, otherwise the bundled `res/raw/alarm_beep.wav` (two 880 Hz beeps, looped). It plays on the **alarm stream** (`USAGE_ALARM`, alarm volume), so it rings in silent mode and through Do Not Disturb's default alarm exception (Jason, 2026-11-21; this replaced an earlier ringer-volume trial). No setting.
- Vibration: a repeating waveform with alarm usage.
- Channel `alarms_v2`: importance HIGH, silent, full-screen intent to `AlarmActivity`. `alarms_v1` (channel sound) is deleted at startup.
- 2-minute cap: the service stops after 2 minutes. T6 schedules the re-ring 2 minutes later, repeating until acknowledged.
- Notification actions (Done, +1 min, Snooze 30 s) go to the engine; each one stops the service.

**AlarmActivity**
- `setShowWhenLocked(true)`, `setTurnScreenOn(true)`. Dark amber in both themes.
- Shows the hive label huge, "<step> finished", "Next" and the next step, and the vaporizer hint ("Vaporizer frees when you tap Done. Herb is next.").
- Closes when nothing is ringing. The close logic runs in `lifecycleScope` (not Compose). When the app opened it (`returnToApp`), closing goes back to MainActivity instead of the launcher (regression: `AlarmActivityCloseTest`).
- Buttons Done / +1 min / Snooze 30 s, each at least 64 dp.

**Full-screen intent.** Manifest declares `USE_FULL_SCREEN_INTENT`. Check `NotificationManager.canUseFullScreenIntent()`. If it returns false, show one explanation screen with a button to `ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT`.

**Ongoing notification** (channel `status`, low importance, silent). It is posted while any run is active and has one line per active hive, e.g. "Emory: Keep sealed, ends 14:32" or "Herb: Load dose, waiting for Done". The header countdown is a chronometer to the soonest end. The only foreground service is `RingService` (above), and it runs only while ringing.

**First run**
- Request `POST_NOTIFICATIONS`.
- Check exact alarms and full-screen intent.
- Ask once for the battery optimization exemption (`REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`) with one plain sentence. Never ask again.

## 9. UI (Compose, Material 3 underneath), matching the mockups

The mockups are the spec. Side-by-side captures: `docs/screenshots/` (mockup-*.png rendered from the HTML at 412 px, app-*.png from the phone), with the differences we kept in `docs/screenshots/README.md`.

- **Tokens.** Everything visual lives in `ui/theme/Theme.kt`: `AppColors` (light and dark), `AlarmColors`, `AppType`, `Dimens`. No dynamic color; the Material scheme is built from the same tokens so dialogs and text fields match. All text in `strings.xml`.
- **Colors (light).** Page #F4F4F2, cards white with a 1 dp #D3D1C7 border and 16 dp corners, text #1F1F1D and #5F5E5A. Device-bound #FAEEDA / #633806 (strong chip #FAC775), passive #E1F5EE / #085041, queued or not started #F1EFE8 / #5F5E5A, danger #A32D2D. Alarm #412402 with #FAEEDA, #FAC775 and #BA7517 in both themes.
- **Type.** Inter (below). Body 14 sp, labels 13, chips 12, hive names 15 bold, status bar 14 (sub line 12). Only the Hive tab countdown (52 sp) and the alarm hive name (48 sp) are large.
- **Sizes.** Buttons 44 dp tall and as wide as their text (`AppButton`). 56 dp for the Hive tab action grid (+1 min, Skip, and Done or Snooze when shown there). 64 dp only for the alarm screen's Done, +1 min and Snooze.
- **Components** (`ui/Common.kt`): `AppCard`, `Chip`, `AppButton` (outlined, filled, danger), `TextAction`, `TabStrip` (underlined, scrollable), `RowDivider`.
- **Font.** Inter (regular, medium, semibold, bold) bundled in `res/font`, licence in `assets/licenses/Inter-OFL.txt`, used for every text style so weights render the same on OnePlus and other phones.
- **Chips.** Countdown and clock-time chips 15 sp bold, 10 x 4 dp padding, at least 28 dp tall; state chips (Device, Passive, Queued) 14 sp.
- **Main screen.** Top bar: "Bee Timer" and a gear icon (48 dp, "Settings"). Low alarm volume banner ("Alarm volume is low", one line, with a Raise button to sound settings) while a run is active and the alarm stream is under 50%. Then one white card: vaporizer bar, tab strip, and the All tab or a Hive tab. The page scrolls. Compose test tags are exposed as resource IDs for Maestro (`row-<id>`, `row-action-<id>`, `row-sub-<id>`, `drag-<id>`, `alarm-done` and so on).
- **All tab = planning view.** Vaporizer bar on one line (ellipsized, never wraps) with "Next: Sparky" under it (queue head, else the next not-started hive in list order). "Coming up": the next 5 step starts across hives from `Planner.forecast` Only the first entry, at the end of a running (not ringing) timed step, is firm; everything after it is an estimate, prefixed "~". Hive rows have a drag handle (long-press), a fixed-width name column, current step with countdown chip, and a "Next" line ("Waiting" and "Starts in ~16:24" when queued). Coming up and the row lines show time remaining ("2:10", "~14:09", "Next: Cool in hive in 2:10", "Starts in ~15:09"); the vaporizer bar shows "Vaporizer: Herb, free in ~15:09". "~" means it depends on a manual tap. Projected times are computed only when the state changes; the countdowns tick from them, and a passed one shows "now". Only the footer is a clock time, "Started hives done ~1:05 PM", in the phone's 12/24-hour format. Not-started hives are not projected.
- **Planner** (`scheduler/Planner.kt`, pure): runs the real reducer forward with a simulated person (timers on time, timed-to-timed acknowledged at once, manual steps 1 min, handoff 1 min, both divided by `timeScale`; not-started hives are not projected). Exposes `deviceFreeAt` for the vaporizer bar. Tested in `PlannerTest`; the firm rule is also checked by `Invariants` on every commit in debug builds.
- **Wording.** UI copy says "Vaporizer" (the gun type has no wand); preset step names keep "wand".
- **Cancel** removes the run, so the hive is Not started with a Start button again (no Cancelled state).
- **Hive order** is the treatment order: drag on the All tab or in Settings, saved as `position` in Room.
- **Vaporizer free.** When the vaporizer is free for the queue head, the bar shows "Vaporizer free: Sparky is next" with Start, Skip to back and Remove.
- **Hive tab.** "Step 3 of 7, heat and vaporize", 52 sp countdown, instruction. Step list with ticks and Device / Passive chips; the current step is tinted with the strong chip. Action grid, then Cancel run.
- **Settings.** Top app bar with a back arrow (48 dp, "Back"); system back also returns. Cards: Steps, Hives, Display, Alerts, Session.
- **Step editor.** Tap a step to edit its name, (timed) duration, and "Takes the vaporizer here" / "Releases the vaporizer here" (moves the marker). Device/Passive chips are display only. A divider "Vaporizer frees here" follows the release step. Arrows move steps and re-derive Device/Passive live. Release above take shows inline ("Not saved") with Undo. Reset to defaults follows the sealed-hold choice. Active runs keep their snapshot.
- **Hive list.** Drag handle, tap to rename, trash icon with confirm, "+ Add hive" (adds one and opens its name), at most `Repository.MAX_HIVES` = 5 ("5 hives max per session"). Names persist across sessions (`HivesAndSessionTest`).
- **Alarm screen.** Hive name 48 sp bold, "Cool in hive finished" 16 sp, divider, "Next" 14 sp, the next step 26 sp (2 lines max), "10:00 timer starts when you tap Done" for a timed next step, vaporizer note 14 sp; spread over the space above Done (64 dp) and +1 min / Snooze 30 s (64 dp).
- **Heads-up** (unlocked, app in the background): title is the hive name ("Herb") so it never truncates; body "Heat and vaporize finished. Next: Cool in hive" (BigText); actions Done and +1 min.
- **App in the foreground** (`alarms/Delivery.kt`, Jason 2026-11-21). Android shows a full-screen intent as a heads-up while the phone is in use, and heads-ups cover the top bar and the vaporizer bar's buttons. So when an app activity is on screen (`ProcessLifecycleOwner` at least STARTED):
  - A timed step's end opens `AlarmActivity` directly. The ring service's notification goes on the quiet `status` channel (no heads-up, no full-screen intent); the app plays sound and vibration as always.
  - "Vaporizer free" is a snackbar at the bottom of the main screen ("Vaporizer free: Herb is next", with Start), never a notification.
  - When the app leaves the screen while something rings (Home, lock), `RingService.unquiet` re-posts it on the alarm channel with the full-screen intent, so the lock screen and the notification bring the alarm screen back. Background and lock-screen behavior is otherwise unchanged.
- **App icon.** Stopwatch bee (`docs/icon/`). Adaptive icon (`mipmap-anydpi/ic_launcher` and `ic_launcher_round`) with background, foreground and monochrome layers. The monochrome layer is for Android 13+ themed icons. The layers are VectorDrawables generated from the SVGs by `scripts/icon/svg2vd.py`, which maps each SVG transform to nested groups exactly. `IconRenderTest` (Robolectric native graphics) and `scripts/icon/compare.py` check them pixel by pixel against the SVGs under circle, squircle and rounded-square masks (`docs/icon/check.png`).
- **Theme.** System, Light or Dark (default System). The alarm screen is dark amber in both. Window themes set `forceDarkAllowed=false`: the app draws its own dark theme, and OxygenOS otherwise recolored the alarm window (found in the screenshot pass).

## 9b. Debug tools and end-to-end verification

Debug builds only (`app/src/debug/.../DevTools.kt`; the release twin in `src/release` is a no-op, so release APKs contain none of it):
- **Time scale** (Settings, Developer): 1x, 10x or 60x. Stored in `SchedulerState.timeScale`; divides timed steps, +1 min, Snooze and planner estimates. The ring cycle and alarm cap stay real. Note: Android will not fire an app's exact alarm less than about 5 s ahead, so at 60x the 2.5 s heat and 2 s cool steps ring at about 5 s.
- **Invariant check** after every scheduler commit (`scheduler/Invariants.kt`, shared with the property test): more than one holder; a run in a device step that does not hold the vaporizer; a waiting run not in the queue; an alarm for a run with no timed step or ringing alarm; a planner time shown firm behind a pending tap. Each violation logs `E/BeeTimerInvariant`.
- **Event log** (`I/BeeTimerE2E`): events, step entries, lock and queue changes, rings, and the forecast after each commit.

End-to-end suite: `scripts/e2e/` (Maestro flows generated by `gen_flows.py`, `run.sh` emulator-only, `analyze.py` writes `docs/e2e/REPORT.md`). See `docs/e2e/README.md`.

## 10. Known limitations and deferrals (alpha)

- Debug build only, debug-signed. No release keystore yet (T11).
- After a reboot, alarms come back only after the first unlock (state lives in credential-protected storage). An alarm due before you unlock rings right after unlocking, marked overdue.
- Alarm sound uses the alarm volume. If the alarm volume is turned down (it was 1 of 16 on the test phone), the alarm is quiet; the main screen banner and the permissions screen remind you.
- The alarm tone is the phone's alarm tone if one is set, otherwise the bundled beep. No tone picker.
- The ongoing notification shows end times per hive plus one countdown to the next end, not a ticking countdown per line.
- The step editor can't add or delete steps, or change a step between timed and manual. The model supports add and delete.
- No session history. No ktlint, detekt, Semgrep or Snyk yet.
- Main screens are designed for light; the dark theme works but has not had a visual pass (backlog B3).
- compileSdk 36 holds Compose, core-ktx and lifecycle at older versions (section 2).
- Text follows the phone's font size setting (the test phone is at 1.15); checked at 1.3 in `docs/screenshots/app-1-all-large.png`.
- Removing a hive from the queue resets its run (approved).
- Android won't fire an exact alarm less than about 5 s ahead. Real steps are much longer, so this only matters for the debug 60x time scale.
- Themed icons are verified on the emulator's Pixel launcher, not yet on the OnePlus (themed icons are off there).

## 11. Testing

Gate: `scripts/check.sh` (`./gradlew test lint`), 91 tests at the icon round.

`:scheduler` (JUnit 4, pure):
- `SchedulerTest`: every required test in the spec (two hives started together, passive overlap, cancel while holding and while queued, snapshot isolation), plus AddMinute, Snooze, Skip, TIMED-to-TIMED acknowledge, re-ring cycle, cancel returns to Not started, old CANCELLED runs dropped, both pan wand hold options end to end, time scale.
- `RestoreTest`: restore at an arbitrary `now`, overdue, no cascade through several steps, snooze surviving process death, the 60 s grace.
- `TemplateTest`: presets match the spec, markers derive Device/Passive, swapping Keep sealed and Remove wand flips the hold choice, renames keep durations and markers, release above take and missing markers rejected, duration bounds.
- `PlannerTest`: forecasts, the firm rule, deviceFreeAt, not-started hives.
- `PropertyTest`: 4 templates x 20 seeds x 1,000 random events over 6 hives, checking `Invariants` after every event.
- `MultiplatformGuardTest`: no JVM or Android APIs in `scheduler/src/main`.

`:app` (Robolectric):
- `SeedingTest` (regression: duplicate seeding), `HivesAndSessionTest` (cap of 5, reorder, names survive New session), `MigrationTest` (1 to 2, 2 to 3), `DeliveryTest` (foreground vs background), `AlarmActivityCloseTest` (regression: in-app alarm closed to the launcher), `IconRenderTest` (renders the launcher layers for `scripts/icon/compare.py`).

End to end: 13 Maestro runs on the emulator with real alarms (`docs/e2e/README.md`, results in `docs/e2e/REPORT.md`, fixes in `docs/e2e/FIXES.md`).

Manual phone script: `docs/MANUAL-TEST.md` (about 10 minutes).

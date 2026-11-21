# Bee Timer: technical design (alpha)

Status: draft for review. Phase: alpha proof of concept, target a working sideloadable APK in about a day of agent time, not days.

Source of truth for requirements: `SPEC.md` (section 1 Decisions wins over the original brief). Approved screens: `docs/mockups/varroa-timer-mockups.html`. This doc records decisions and the answers to open questions, so a fresh session can pick up any task in `TASKS.md` cold.

## 1. Goal and alpha scope

Offline Android app that times oxalic acid vaporization across several hives sharing one vaporizer. One shared, editable step template. The vaporizer is a lock: only one hive may be in a device-bound step at a time.

Alpha must demo, on the phone:
- Hives with user-chosen names (e.g. Emory, Herb, Sparky): add, rename, reorder, delete.
- Start several hives, device queue, manual handoff ("Device free: Herb is next"), passive overlap.
- Loud alarms that fire with the screen off and show over the lock screen.
- State survives app kill and reboot.
- Template editor with the two presets and validation.

Alpha cuts (deliberate; see section 10):
- No release build or keystore (debug APK only).
- No ktlint, detekt, Semgrep, or Snyk yet.
- The template editor UI edits durations of TIMED steps on the two presets only. The model supports add, delete and reorder.
- No history list. Hive reordering uses up/down buttons.
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
| JUnit | 4.13.2 | |

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
  Model.kt        Step, StepKind, Template (add/delete/move ops), Hive, HiveRun, RunState, SchedulerState
  Presets.kt      "OAV pan wand", "OAV gun type"
  Validation.kt   TemplateValidator
  Scheduler.kt    interface Scheduler { reduce; restore }, Event, Effect, DefaultScheduler
  test/           SchedulerTest, PropertyTest, RestoreTest, ValidatorTest, MultiplatformGuardTest
app/         (Android; package io.github.mecheng70.beetimer, by layer)
  data/       Room db, DAOs, entities, Repository
  alarms/     AlarmScheduler (AlarmManager), receivers, Channels, RingingNotifier, OngoingNotifier, SchedulerEngine
  ui/         MainActivity, AlarmActivity, screens, ui/theme/Theme.kt (all colors, type, sizes)
  res/values/strings.xml   every UI string
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
data class Step(val name: String, val kind: StepKind, val durationSeconds: Int?, val deviceBound: Boolean, val instruction: String)
data class Template(val id: String, val name: String, val steps: List<Step>)
data class Hive(val id: Long, val label: String, val note: String?, val position: Int)

enum class RunState { NOT_STARTED, WAITING_FOR_DEVICE, RUNNING, AWAITING_CONFIRM, DONE, CANCELLED }

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
data class Ringing(val finishedStepIndex: Int, val since: Long, val overdue: Boolean)

data class SchedulerState(
  val runs: Map<Long, HiveRun>,
  val lockHolder: Long?,          // hive in a device-bound step
  val queue: List<Long>,          // FIFO of hives waiting for the device
  val activeTemplate: Template,
)
```

Hive labels are free text and are what every message shows ("Device free: Herb is next"). Quick-add creates "Hive N" defaults, which you can rename.

## 5. Scheduler rules (pure reducer)

`Scheduler.reduce(state, event, now): Result(newState, effects)`. No clocks, no I/O. `now` is always passed in.

Events: `Start(hive)`, `Done(hive)`, `TimerFired(hive)`, `AddMinute(hive)`, `Snooze(hive)`, `Skip(hive)`, `Cancel(hive)`, `QueueSkipHead`, `QueueRemoveHead`, `NewSession`, `TemplateChanged(template)`. Done covers acknowledging too: on a finished timed step it starts the next step, on a manual step it completes it, otherwise it just silences the alarm.

Effects: `ScheduleAlarm(hive, atMs)`, `CancelAlarm(hive)`, `Ring(hive, finishedStepIndex, overdue)`, `StopRing(hive)`, `NotifyDeviceFree(hive)`. Alarm effects are derived: each run needs at most one alarm (`ringing.nextRingAt` if ringing, else `stepEndsAt` if RUNNING), and the reducer emits Schedule or Cancel when that changes. A ringing run re-rings every 4 minutes (2 min ringing plus 2 min quiet) until acknowledged; Snooze moves the next ring to now + 30 s. After every reduce, the engine also refreshes the ongoing notification and the UI.

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
9. **Cancel.** `CancelAlarm`, `StopRing`, release the lock if held (then rule 4), remove from the queue if queued (lock untouched), set `CANCELLED`.
10. **Template edits.** These change `activeTemplate` only. Runs keep their snapshot.
11. **NewSession.** Clears all runs and the queue. Hives keep their names.
12. **Restore(state, now).** Used on app start, boot, and time change.
    - For each RUNNING TIMED run with `stepEndsAt <= now`: apply rule 5 as if it fired at `stepEndsAt`, with `overdue = true` (the alarm shows "overdue", nothing is silently skipped).
    - For future ends: `ScheduleAlarm`.
    - Ringing runs ring again.
    - Because a TIMED-to-TIMED handoff waits for acknowledgement, missed alarms never cascade through several steps.


## 6. Template validation

- At least one step.
- TIMED steps need a duration from 1 s to 86,400 s.
- Device-bound steps must form one contiguous block, or there must be none. A split block is refused with the message: "Device steps must be next to each other. A gap would let another hive take the vaporizer mid-treatment."

Presets "OAV pan wand" and "OAV gun type" are seeded exactly as in the spec. Both are saved. The alpha UI edits TIMED durations only; the `Template` model already supports add, delete and reorder (pure functions, tested) for the full editor later. The toggle picks which preset is active for new runs, and each has "Reset to defaults".

## 7. Persistence (Room)

To keep it simple and atomic:
- `hives(id, label, note, position)`
- `templates(id, name, stepsJson, isActive)`: two rows.
- `scheduler_state(id = 1, json)`: the whole `SchedulerState` (minus the template) serialized with kotlinx.serialization. One row, written in the same transaction after every reduce.

UI preferences (theme, keep screen on, battery prompt already shown) live in SharedPreferences.

## 8. Android layer

**SchedulerEngine** (app singleton) is the only place events are dispatched. It does mutex, load state, `reduce(now = System.currentTimeMillis())`, persist, then execute effects. It exposes a `StateFlow<SchedulerState>` for the UI.

**Alarms**
- Step ends use `AlarmManager.setAlarmClock()`. The show intent opens MainActivity.
- Snooze and re-ring use `setExactAndAllowWhileIdle()`.
- Manifest: `USE_EXACT_ALARM`. Check `canScheduleExactAlarms()` at startup anyway, and route to settings if it returns false.
- `AlarmReceiver` uses `goAsync()` and dispatches `TimerFired` or the re-ring.

**Boot and time changes.** `BootReceiver` handles `BOOT_COMPLETED`, `TIME_SET`, `TIMEZONE_CHANGED`, and `MY_PACKAGE_REPLACED`, and calls `restore`.

**Ringing (revised after the T1b phone test, OnePlus CPH2583, Android 16)**
- Findings: (1) OxygenOS replaced the channel's vibration and ignored `FLAG_INSISTENT`, so the phone buzzed once (`OplusVibrationEffectSegment ... repeat=-1`). (2) The system default alarm tone is unset on this phone (`settings get system alarm_alert` is null), so a player set to "default alarm" never started.
- So the notification no longer makes the noise. `RingService` (a foreground service that runs only while something is ringing) plays the sound and vibration itself through `AlarmSound`.
- Service type `systemExempted`: Android allows it for apps that hold `USE_EXACT_ALARM` and keep an alarm going in the background. It is started from the exact-alarm receiver, which is exempt from background-start limits. The spec allows a service when testing shows alarms are unreliable without one; this test showed that.
- Sound: the phone's alarm tone if one is set and plays, otherwise the bundled `res/raw/alarm_beep.wav` (two 880 Hz beeps, looped). It plays at the **ringer** volume (`USAGE_NOTIFICATION_RINGTONE`), Jason's choice (2026-11-21) since he keeps alarm volume low. Trade-off: silent mode and Do Not Disturb mute the sound. Changing one constant in `AlarmSound` switches it to the alarm volume.
- Vibration: a repeating waveform with alarm usage, so it keeps going on silent and vibrate.
- Channel `alarms_v2`: importance HIGH, silent, full-screen intent to `AlarmActivity`. `alarms_v1` (channel sound) is deleted at startup.
- 2-minute cap: the service stops after 2 minutes. T6 schedules the re-ring 2 minutes later, repeating until acknowledged.
- Notification actions (Done, +1 min, Snooze 30 s) go to the engine; each one stops the service.

**AlarmActivity**
- `setShowWhenLocked(true)`, `setTurnScreenOn(true)`. Dark amber in both themes.
- Shows the hive label huge, "<step> finished", "Next: <instruction>", and the device hint ("Device frees when you tap Done. Herb is next.").
- Buttons Done / +1 min / Snooze 30 s, each at least 64 dp.

**Full-screen intent.** Manifest declares `USE_FULL_SCREEN_INTENT`. Check `NotificationManager.canUseFullScreenIntent()`. If it returns false, show one explanation screen with a button to `ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT`.

**Ongoing notification** (channel `status`, low importance, silent). It is posted while any run is active and has one line per active hive, e.g. "Emory: Keep sealed, ends 14:32" or "Herb: Load dose, waiting for Done". The header countdown is a chronometer to the soonest end. The only foreground service is `RingService` (above), and it runs only while ringing.

**First run**
- Request `POST_NOTIFICATIONS`.
- Check exact alarms and full-screen intent.
- Ask once for the battery optimization exemption (`REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`) with one plain sentence. Never ask again.

## 9. UI (Compose, Material 3), following the mockups

- **Color semantics.** Amber = device-bound and holding the vaporizer. Teal = passive sealed wait. Gray = queued or not started. These map to custom M3 color roles with light and dark variants. High contrast and large type throughout.
- **Device status bar (top).**
  - "Device: free", or "Device: Herb, Heat and vaporize, 1:42 left".
  - Second line: "Next: Sparky, Emory".
  - When the head is ready: "Device free: Sparky is next" with a Start button.
- **All tab.**
  - One row per hive: name, current step or state, and the one action you can take: Start (not started or queue head ready), Done (MANUAL waiting), otherwise a countdown or status pill.
  - Queue head row also gets Skip to back / Remove.
- **Hive tab.**
  - "Step 3 of 7, Heat and vaporize", a big countdown, and the instruction.
  - Step list with ticks, the current step highlighted, and Device / Passive pills.
  - Contextual buttons: Start; Done; +1 min / Skip on TIMED; Skip on MANUAL; Cancel run.
- **Settings.**
  - Hives: quick-add N, rename, note, up/down reorder, delete with confirmation.
  - Template editor: preset toggle; edit TIMED durations; reset to defaults. Footer text exactly as in the spec.
  - Theme: System / Light / Dark.
  - Keep screen on while a run is active (on by default).
  - New session, with confirmation.
- Navigation is a top app bar with a settings icon. There is no nav library; a simple state-based screen switch keeps it fast.

## 10. Known limitations and deferrals (alpha)

- Debug build only. A release keystore comes after alpha.
- No ktlint or detekt yet.
- Up/down reordering, no drag.
- The ongoing notification shows end times, not a ticking countdown per hive.
- Removing a hive from the queue restarts its run (section 5 note).
- No instrumented UI tests. Alarm and notification behaviour is verified with the manual phone script.
- Audio uses the default alarm ringtone. There is no ringtone picker.

## 11. Testing

`:scheduler` unit tests (JUnit 4) cover every required test in the spec:
- Two hives started together.
- Passive overlap.
- 1,000-event randomized property test over 6 hives with a seeded RNG. Invariants: at most one lock holder; the holder is in a device-bound step; queued runs are WAITING_FOR_DEVICE; no run is both queued and holding the lock.
- Cancel while holding the lock, and cancel while queued.
- Restore at an arbitrary `now`, including overdue.
- Template snapshot isolation.
- Rejection of a split device block.

Plus tests for AddMinute, Snooze, Skip, and the TIMED-to-TIMED acknowledge path.

Manual phone script (about 10 minutes, delivered in T10). It uses a 10 s test template with Emory, Herb, and Sparky and covers overlap, queue handoff, cancel, a locked screen, and a reboot mid-run.

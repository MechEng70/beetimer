# Bee Timer: alpha spec

Android app (sideloaded APK) that times oxalic acid vaporization (OAV) treatments across several hives that share one vaporizer, and rings a real alarm at the end of each timed step.

Owner: Jason. Users: Jason and anyone he gives the APK to. Each phone runs its own session; nothing is shared.

Mockups (source of truth for layout and look): `docs/mockups/varroa-timer-mockups.html`.
How it is built: `docs/design/TECH-DESIGN.md`. Work items: `docs/design/TASKS.md`.

Where section 1 and the original brief (section 2) disagree, section 1 wins.

---

## 1. Decisions

| Topic | Decision |
|---|---|
| Name | Bee Timer. |
| Platform | Android first: native Kotlin, Jetpack Compose (Material 3), Room, AlarmManager. iOS later: the scheduler stays Kotlin stdlib only so it can move to Kotlin Multiplatform; the alarm layer is the platform boundary (AlarmManager on Android, AlarmKit on iOS 26+). KMP is not set up now. |
| Distribution | App ID io.github.mecheng70.beetimer (permanent). APK on GitHub Releases (MechEng70/beetimer). Alpha ships a debug-signed APK. A release keystore (T11) comes before the first public release. |
| Accounts / backend | None. No network, analytics or ads. All data on the phone. |
| SDK levels | minSdk 31, targetSdk 36, compileSdk 36. JDK 21. |
| Presets | "OAV pan wand" and "OAV gun type". Durations are reference defaults; the product label governs. The pan wand has a sealed-hold choice: "Wand stays in" (the vaporizer is held through Keep sealed) or "Wand comes out" (Remove wand moves above Keep sealed and frees the vaporizer for the next hive). Switching applies that default step list, with a confirm when the steps were edited. |
| Step model | Replaces the per-step deviceBound flag in the brief. Each template has one step that takes the vaporizer and one that releases it. Every step from take through release is Device; the rest are Passive. The only invalid arrangement is release above take, so a split device block can't happen. |
| Template editing (alpha UI) | Tap a step to rename it, change its time (timed steps), or move the take or release marker there. Arrows move steps. Reset to defaults. Add and delete steps are in the model, not the UI. Active runs keep the steps they started with. |
| History | Deferred past alpha. |
| Hive names | Free text (for example Emory, Herb, Sparky). Every message uses the name. "+ Add hive" makes a "Hive N" name you can rename. At most 5 hives. List order is treatment order; drag to reorder on the All tab or in Settings. |
| Wording | UI copy says "Vaporizer" (the gun type has no wand). Step names and instructions keep "wand". |
| Timed step end | Next step MANUAL: the run moves to it right away and the alarm rings; Done on the alarm completes that manual step. Next step TIMED: the run waits on the finished step until you acknowledge, so the next timer starts when you tap (for example Cool starts after you switch power off). |
| Alarm buttons | Done, +1 min (reopens the finished step for 60 s), Snooze 30 s (silence, ring again in 30 s, no state change). |
| Alarm sound | Plays on the alarm stream (alarm volume), so it rings on silent and in Do Not Disturb. The phone's alarm tone if set, else a bundled beep. Rings up to 2 minutes, silent 2 minutes, rings again, until acknowledged. Tapping Start on the first hive of a session with the alarm volume under half shows the bottom snackbar "Alarm volume is low" with Raise (sound settings) for about 6 s, at most once per session (New session resets it). No banner on the main screen. |
| App in the foreground | A timed step's end opens the alarm screen directly (no heads-up). "Vaporizer free" is a snackbar at the bottom with Start, not a notification. If you leave the app while it rings, the normal lock-screen alarm comes back. |
| Queue | FIFO. Handoff is manual: the head is told "Vaporizer free: Herb is next" and waits for Start. Skip to back of queue is kept. Remove from queue resets that hive's run. |
| Cancel | Cancel run returns the hive to Not started with a Start button (no Cancelled state). |
| Vaporizer bar | "Vaporizer: free" or "Vaporizer: Herb, free in ~15:09", and "Next: Sparky". |
| All tab | Planning view. "Coming up" lists the next 3 step starts across hives in time order, hive name and action only (no times; the rows and the vaporizer bar carry them). Each hive row: line 1 is the drag handle, hive name and, right-aligned on that line only, the action (Start, Done) or a countdown or status chip; line 2 the current step and line 3 "Next: <step>" with no time (the chip shows it), both full width under the chip. Step names are never cut off on the All tab: a long one wraps, and the pill grows. The step name is an outlined pill (no fill, 16 sp semibold, 1.5 dp border, 8 dp corners): amber #BA7517 border and #633806 text for a vaporizer step, teal #1D9E75 and #085041 for a passive one, so the countdown chip stays the strongest element. Queued rows show "Starts in ~16:24". Rows are sized to read outdoors: hive name 20 sp, current step 17 sp, Next line 16 sp, countdown chip 20 sp bold. Not-started hives are not projected. Footer, a clock time in the phone's 12/24-hour format with no "~" ("Estimated" says it): "Estimated finish: 9:15 AM" when every hive is started, "Estimated finish: 9:15 AM (2 of 3 hives)" when some are not (started of total; a cancelled hive counts as not started), and "All hives done at 9:15 AM" once every hive's run is complete. No footer when nothing is running and not every hive is done. Projections are recomputed only when something happens; countdowns tick from them. |
| Theme | System, Light or Dark (default System). Alarm screen is dark amber in both. Inter font bundled. |
| App icon | Stopwatch bee, adaptive icon with a monochrome layer for themed icons (`docs/icon/`). |
| Quality gate (alpha) | Unit tests plus Android lint (`scripts/check.sh`), plus the emulator E2E suite for behavior changes. Semgrep, Snyk, ktlint and detekt are deferred. |

## 2. Original brief

### Goal

Build a small, offline Android app called **Bee Timer** (originally "Varroa OAV Timer") that I'll sideload as a debug APK. It times oxalic acid vaporization (OAV) treatments across multiple hives when there is only ONE vaporizer. Each hive runs the same editable step sequence with its own alarms. The single device is a shared lock: two hives can never be in a device-bound step at the same time, but a hive in a passive step (sealed and waiting) does not hold the device, so the next hive can start.

Work in this order and stop for my review at each checkpoint marked STOP:
1. Confirm toolchain and write the plan (STOP).
2. Scheduler core as pure Kotlin with unit tests, all green (STOP).
3. Data layer, alarms, notifications.
4. UI.
5. Build the debug APK and give me install steps (STOP).

Keep scope tight. No accounts, no network, no analytics, no cloud sync, no ads. Do not add features I didn't list; propose them at the end instead.

### Target and toolchain

- Phone: Android 16 (API 36). `minSdk 31`, `targetSdk 36`, `compileSdk 36`.
- Kotlin, Jetpack Compose (Material 3), Room for persistence, AlarmManager for timing. No WorkManager for the timers (not exact enough).
- Build host: Fedora Linux, command line only, no Android Studio required.
- First, check what's installed (`java -version`, `$ANDROID_HOME`, `sdkmanager --list_installed`) and print the exact commands to install anything missing: a JDK 17 or 21 (`dnf`), Android command-line tools, `platform-tools`, `platforms;android-36`, and the matching `build-tools`. Ask before running `sudo` anything.
- Gradle wrapper with Kotlin DSL and a version catalog. Pin versions; verify each against current official docs instead of guessing.
- Output: `app/build/outputs/apk/debug/app-debug.apk`, debug-signed. Also produce a release build signed with a local keystore I generate, if it's under 10 minutes of extra work; otherwise skip and say so.

### Domain model

**StepTemplate** (one ordered list, editable in the app, shared by all hives):
- `name` (string)
- `kind`: `TIMED` (counts down, alarm fires at end, then auto-advances) or `MANUAL` (no timer; waits for me to tap Done)
- `durationSeconds` (TIMED only)
- `deviceBound` (bool): true if the vaporizer is physically in use on this hive during the step
- `instruction` (short free text shown on screen and in the notification)

**Hive**: `id`, `label` (default "Hive 1", "Hive 2", ...), optional `note`. I can add, rename, reorder, and delete hives.

**HiveRun**: one treatment pass of the template on one hive for today's session. Fields: `hiveId`, `currentStepIndex`, `state` (`NOT_STARTED`, `WAITING_FOR_DEVICE`, `RUNNING`, `AWAITING_CONFIRM`, `DONE`, `CANCELLED`), `stepStartedAt`, `stepEndsAt` (wall-clock epoch ms), `completedAt`.

**Session**: groups today's runs. Starting a new session resets all runs. Keep a simple history list of completed runs (hive, start, end); no analytics.

### Default template (seed data, all editable)

Seed two presets and let me pick or edit. Times come from manufacturer and extension material and are defaults only; the product label governs.

Preset A, "OAV pan wand":
1. Seal hive (MANUAL, deviceBound=false): close entrance and cracks, insert wand.
2. Load dose (MANUAL, deviceBound=true): dose per label into cold pan.
3. Heat and vaporize (TIMED 150 s, deviceBound=true): power on.
4. Cool in hive (TIMED 120 s, deviceBound=true): power off, leave wand in.
5. Remove wand (MANUAL, deviceBound=true): remove wand, reseal entrance. Completing this step releases the device.
6. Keep sealed (TIMED 600 s, deviceBound=false).
7. Unseal (MANUAL, deviceBound=false).

Preset B, "OAV gun type":
1. Seal hive (MANUAL, deviceBound=false)
2. Load cap and inject (MANUAL, deviceBound=true)
3. Vaporize (TIMED 20 s, deviceBound=true)
4. Withdraw (MANUAL, deviceBound=true): releases the device on Done.
5. Keep sealed (TIMED 600 s, deviceBound=false)
6. Unseal (MANUAL, deviceBound=false)

Validation: a template must have at least one step, TIMED steps need duration of 1 s to 24 h, and device-bound steps must be one contiguous block (warn and refuse to save otherwise, since a split block would let another hive grab the device mid-treatment).

### Scheduler rules (the core; implement as a pure Kotlin class with no Android imports)

1. Device lock: at most one HiveRun may be in a deviceBound step at any time. The lock is acquired when a run enters its first deviceBound step and released when it leaves the last one (or is cancelled).
2. Starting a hive: tapping Start on a hive runs its leading non-device steps immediately. When it reaches the device block: if the lock is free, it takes the lock; if not, it goes to `WAITING_FOR_DEVICE` and joins a FIFO queue.
3. Handoff is NOT automatic. When the lock frees and a run is queued, the app alerts "Device free: Hive N is next" and that run sits at its device block until I tap Start, because I have to physically move the device. While unstarted, the lock is reserved for the queue head so nothing jumps ahead. I can skip the queue head to the back or remove it.
4. TIMED steps auto-advance when their alarm fires. MANUAL steps wait for Done. If the next step after a TIMED step is MANUAL, the alarm says what to do next.
5. Passive overlap is expected: Hive 1 can be in "Keep sealed" while Hive 2 heats.
6. Cancel a run at any point: releases the lock if held, cancels its pending alarm, marks CANCELLED.
7. Pause is not required. Skip-step and +1 minute on the current TIMED step are required.
8. Time math uses absolute end timestamps, never in-memory countdowns, so state survives process death, screen off, and reboot.
9. All transitions are pure functions: `(state, event, now) -> (newState, effects)`, where effects are things like ScheduleAlarm, CancelAlarm, Notify. The Android layer executes the effects. This is what makes it testable.

#### Required unit tests (write first, make them pass before any UI)

- Two hives started together: the second waits; it gets the lock only after the first completes its device block and I tap Start.
- Passive overlap: Hive 1 in Keep sealed, Hive 2 can take the lock and run.
- Lock is never held by two runs, checked across a randomized sequence of 1,000 events over 6 hives (property-style test).
- Cancel while holding the lock releases it and alerts the queue head.
- Cancel while queued removes from the queue without touching the lock.
- Restoring from persisted state at an arbitrary `now` (including after a step's end time has passed) yields the correct current step and reschedules only future alarms; any missed alarms are shown as overdue, not silently skipped.
- Template edit while runs are active: active runs keep the template snapshot they started with.
- Template validation rejects a split device block.

### Android alarms and notifications (Android 16 specifics; verify against current docs)

- Use `AlarmManager.setAlarmClock()` for step-end alarms so they fire in Doze. Declare `USE_EXACT_ALARM` (this is a timer app and it's sideloaded, so Play policy isn't a factor). Check `canScheduleExactAlarms()` at startup anyway and route to settings if false.
- `POST_NOTIFICATIONS` runtime permission on first launch.
- A high-importance notification channel with alarm audio attributes, vibration, and a full-screen intent so it shows over the lock screen. Request `USE_FULL_SCREEN_INTENT`, check `NotificationManager.canUseFullScreenIntent()`, and if denied, walk me to the settings toggle with one screen of explanation.
- Alarm screen: hive label in huge text, the step that just finished, the next instruction, and big buttons: Done / +1 min / Snooze 30 s. Must work with gloves on: minimum 64 dp touch targets.
- Alarm sound keeps going until acknowledged, capped at 2 minutes, then repeats every 2 minutes until acknowledged.
- `RECEIVE_BOOT_COMPLETED` (and time-change) receiver that reloads state from Room and reschedules alarms through the scheduler's restore function.
- An ongoing notification while any run is active, showing each active hive and its countdown (one line per hive), so I can glance at the lock screen.
- Ask once to exempt the app from battery optimization, with a plain explanation; don't nag.
- No foreground service unless testing shows alarms are unreliable without one. If you add one, justify the service type.

### UI (Compose)

- Top: a device status bar: "Device: free" or "Device: Hive 3, Heat and vaporize, 1:42 left", plus the queue ("Next: Hive 4, Hive 2").
- Tabs: one tab per hive (scrollable tab row), plus a first "All" tab: a compact list of every hive with state, current step, and countdown, each with a Start button.
- Hive tab: step list with the current step highlighted, big countdown, instruction text, and Start / Done / Skip / +1 min / Cancel.
- Settings: hive count (quick add N hives), rename and reorder, template editor (add, delete, reorder steps, edit fields, choose preset), and a "New session" button with confirmation.
- Readable in bright sun: high contrast, large type, dark theme option. Keep screen on while a run is active (toggle in settings).
- A one-line footer on the template screen: "Defaults are reference values. Follow your product label and vaporizer manual; the label is the legal use spec." No other safety text in the app.

### Mockups (approved; follow them)

`docs/mockups/varroa-timer-mockups.html`. Open and read it before writing any UI. It shows four approved screens: (1) All hives tab with device status bar and queue, (2) single Hive tab with big countdown and step list, (3) full-screen alarm over the lock screen, (4) template editor with preset toggle. Match layout, hierarchy, copy, and the color semantics: amber = device-bound step holding the vaporizer, teal = passive sealed wait, gray = queued or not started. Map these to a Material 3 color scheme with a dark-theme equivalent; keep the alarm screen dark amber with light text in both themes. Where the mockup and this prompt disagree, ask me.

### Deliverables

1. Source tree with a README covering toolchain install on Fedora, build commands, and sideload steps (enable "Install unknown apps" for the file manager or use `adb install -r app/build/outputs/apk/debug/app-debug.apk`).
2. Passing unit tests (`./gradlew test`) with a summary of what's covered.
3. A manual test script I can run on the phone in about 10 minutes using a test template with 10 s steps: three hives, overlap, queue handoff, cancel, screen locked, reboot mid-run.
4. A short list of known limitations and anything you deliberately left out.

Don't use em dashes in UI strings, README, or comments.

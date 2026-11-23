# Bee Timer: spec

Owner: Jason. Mockups (source of truth for layout and look): `docs/mockups/varroa-timer-mockups.html`.
How it is built: `docs/design/TECH-DESIGN.md`. Work items: `docs/design/TASKS.md`.

Where section 1 (Decisions) and the original brief (section 7) disagree, section 1 wins.

## What it does

Bee Timer is an offline Android app (sideloaded APK) that times oxalic acid vaporization (OAV) treatments
across several hives that share one vaporizer. It rings a real alarm at the end of each timed step, keeps the
vaporizer to one hive at a time, and queues the next hive until you move the vaporizer by hand.

**Users and phone.** Jason and anyone he gives the APK to, treating a few hives in an apiary, outdoors, often
with gloves on and the phone in a pocket. Reference phone: OnePlus CPH2583, Android 16 (OxygenOS), font scale
1.15. Each phone runs its own session; nothing is shared. No accounts, network, analytics or ads.

**The one job.** Never let a hive's treatment step end unnoticed, and never let two hives use the vaporizer at
once.

---

## 1. Decisions

| Topic | Decision |
|---|---|
| Name | Bee Timer. |
| Platform | Android first: native Kotlin, Jetpack Compose (Material 3), Room, AlarmManager. iOS later: the scheduler stays Kotlin stdlib only so it can move to Kotlin Multiplatform; the alarm layer is the platform boundary (AlarmManager on Android, AlarmKit on iOS 26+). KMP is not set up now. |
| Distribution | App ID io.github.mecheng70.beetimer (permanent). APK on GitHub Releases (MechEng70/beetimer). The alpha on Jason's phone is debug-signed. From 0.1.0-alpha.1 on, releases are signed with a release keystore kept outside the repo (R11 T-016). A release-signed APK can't install over the debug one: one uninstall on Jason's phone, done by him. |
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
| Quality gate | `scripts/check.sh`: ktlint, detekt (zero warnings, no baseline), Android lint, unit tests, Semgrep, Snyk open source and code at high and above (R11, replaces the alpha gate of tests and lint). Plus the emulator E2E suite for behavior changes. |
| Process (R11, 2026-11-23) | The New App Playbook (`docs/PLAYBOOK.md`), adapted to Kotlin: ktlint and detekt for Prettier and ESLint, JUnit and Robolectric for Jest, Room for expo-sqlite. Stack stays native; no Expo port. |
| iOS path | Kotlin Multiplatform later: `:scheduler` moves to `commonMain` unchanged; UI in Compose Multiplatform or SwiftUI; alarm layer on AlarmKit (iOS 26+). `MultiplatformGuardTest` keeps the scheduler ready. |
| Branch | `main` (renamed from `master` on 2026-11-22, before the first push). Where the R11 brief says `master`, read `main`. |
| Behavior oracle (R11) | Tag `alpha-baseline` (`571c8ee`). R11 changes no user-visible behavior; anything that looks wrong in the baseline is logged in TASKS.md under "Found during R11", not fixed. |
| Dashboard timer (R11) | Transient systemd user timer from `scripts/dashboard-timer.sh` (playbook section 9), not installed unit files in `scripts/systemd/` (the brief). The playbook is newer and Jason's prompt says start and stop it; transient units leave nothing installed. |

## 2. Screens

The mockups win on layout and look; this is what each screen is for. Details are in TECH-DESIGN section 9.

| Screen | What it does |
|---|---|
| Main, All tab | Planning view. Vaporizer bar (who holds it, when it frees, who is next), Coming up (next 3 step starts), one row per hive with Start or Done, a countdown chip, the current step and the next step pill. Footer with the estimated finish. Drag to reorder hives. |
| Main, Hive tab | One hive: "Step 3 of 7", big countdown, instruction, step list with Device and Passive chips, and the actions (Start, Done, +1 min, Skip, Snooze when ringing, Cancel run). Queue head gets Skip to back and Remove. |
| Alarm screen | Full screen over the lock screen, dark amber. Hive name, the step that finished, what's next, the vaporizer note. Done, +1 min, Snooze 30 s, all 64 dp. |
| Settings | Steps (preset, sealed-hold choice, step editor with take and release markers, Reset to defaults, label footer), Hives (rename, drag, delete, add, max 5), Display (theme, keep screen on), Alerts (permissions, alarm volume), Session (New session). Debug builds add Developer (time scale). |
| Permissions | Notifications, exact alarms, full-screen alarms (with a one-screen explainer), and a one-time battery optimization ask. Shown at launch while a required one is missing. |
| Notifications | Ongoing status (one line per active hive), the ringing alarm (full-screen intent, Done, +1 min, Snooze 30 s), and "Vaporizer free: Herb is next" with Start. In the foreground the alarm opens the alarm screen directly and "Vaporizer free" is a snackbar. |

## 3. Rules

Full rules with numbers: TECH-DESIGN section 5 (scheduler) and section 6 (steps and validation).

- **One vaporizer.** At most one hive is in a vaporizer step at a time. A hive that reaches its vaporizer
  steps while the vaporizer is busy, or while others are queued, joins a first-in, first-out queue.
- **Manual handoff.** When the vaporizer frees, the queue head is told "Vaporizer free: Herb is next" and
  waits for Start, because you carry the vaporizer over. Nobody else can jump ahead. Skip to back and Remove
  work on the head; Remove resets that hive to Not started.
- **Take and release markers.** Each step list has one step that takes the vaporizer and one that releases it.
  Every step from take through release is a vaporizer (Device) step; the rest are Passive. Release above take
  is the only invalid arrangement, so the vaporizer steps are always one block.
- **Sealed-hold option (pan wand).** "Wand stays in": the vaporizer is held through Keep sealed, and Remove
  wand releases it after the hold. "Wand comes out": Remove wand moves above Keep sealed and frees the
  vaporizer for the next hive while this one stays sealed.
- **Timed step end.** If the next step is manual, the run moves to it and the alarm rings; Done on the alarm
  completes that manual step. If the next step is timed, the run waits until you acknowledge, so the next
  timer starts at your tap. A missed alarm rings as overdue; nothing is skipped silently, and missed alarms
  never cascade through several steps.
- **Snapshots.** A run keeps the step list it started with. Editing steps or switching presets only affects
  the next Start.
- **Absolute times.** Every end time is a stored wall-clock timestamp, so state survives process death, screen
  off and reboot.
- **Planner: firm versus estimated.** The All tab runs the real scheduler forward with a simulated person
  (timers on time, timed-to-timed acknowledged at once, each manual step 1 min, each handoff 1 min).
  A time is firm (no "~") only when nothing but timers lies ahead of it. A time that depends on a manual tap
  is estimated and shows "~". Not-started hives are not projected. Projections are recomputed when something
  happens, and countdowns tick from them.

## 4. Failure modes

FMEA style. "Tested" names the automated test, E2E scenario (`docs/e2e/README.md`) or manual check that covers
it. "Real phone" means only a real phone can show it; those are on Jason's list.

| Failure | What breaks | What the user sees | What the app does | Tested |
|---|---|---|---|---|
| Phone in Doze (screen off, still) | Ordinary alarms and jobs are deferred. | The alarm rings on time. | Step ends use `AlarmManager.setAlarmClock()`, which fires in Doze. Snooze and re-ring use `setExactAndAllowWhileIdle()`. | E2E s7 (screen off); `docs/MANUAL-TEST.md` (locked phone). Real phone for deep Doze. |
| OEM battery manager kills the app (OxygenOS) | The process dies; on some OEMs pending work is dropped. | The alarm still rings; the app reopens on the right step. | AlarmManager alarms survive process death (only force-stop cancels them). One-time ask for the battery optimization exemption. Sound runs in a `systemExempted` foreground service only while ringing. | E2E s8 (process death). Real phone for OEM behavior. |
| Exact-alarm access missing | Exact alarms can't be scheduled. | The permissions screen at launch, with a link to the setting. | `USE_EXACT_ALARM` (Android 13+) is granted at install and the user can't revoke it; `SCHEDULE_EXACT_ALARM` covers Android 12. `canScheduleExactAlarms()` is checked at every launch. If scheduling still throws, the error is logged and that alarm is not set (no on-screen warning). | None automated (not reachable on Android 13+). |
| Alarm volume low or muted | The alarm is quiet or silent. | On the first Start of a session with alarm volume under half: snackbar "Alarm volume is low" with Raise, about 6 s. | Sound plays on the alarm stream, so ringer mute doesn't affect it. Vibration runs with alarm usage. | `VolumeCheckTest`. Real phone for loudness. |
| Do Not Disturb on | Sounds that DND blocks are silent. | The alarm rings, because DND allows alarms by default. If the user has turned alarms off in DND, the alarm screen and notification still show, but may be silent. | Alarm stream and alarm usage throughout. | Real phone. |
| Full-screen intent denied | The alarm can't take over the lock screen. | A heads-up notification with Done and +1 min instead of the alarm screen; sound and vibration still play. The permissions screen explains and links to the setting. | `canUseFullScreenIntent()` is checked at launch. | Real phone. |
| Notifications denied | No alarm notification, status notification or "Vaporizer free" notification in the background. | The permissions screen at launch. In the foreground, the alarm screen and snackbars still work. | `POST_NOTIFICATIONS` requested on first run and checked at launch. | Real phone. |
| Process killed mid-run | In-memory state is gone. | Nothing, if no alarm is due. On reopening, the hive is on the right step; a step that ended meanwhile rings, marked overdue. | State is saved to Room after every event. Alarms stay in AlarmManager. `restore` runs on start; ringing alarms ring again only when due, so a snooze survives. | `RestoreTest`; E2E s8. |
| Two hives need the vaporizer at once | Both would use it. | The second hive shows Waiting; "Vaporizer free: Herb is next" when it frees; Start to continue. | FIFO queue with manual handoff; lock reserved for the queue head. | `SchedulerTest`, `PropertyTest` (1,000 random events, 6 hives, invariants after each); E2E s1 to s4. |
| User edits steps or switches preset mid-run | Running hives could change under the user. | Running hives keep their steps; the edit applies to the next Start. | Each run snapshots the step list at Start. | `SchedulerTest` (snapshot isolation); E2E s9. |
| Corrupt database row | A read fails. | The app opens normally. A corrupt state row: running hives return to Not started. A corrupt step list: that preset's default steps (which preset is active is kept). Before R11, a corrupt step list stopped the app from opening. | A bad row never crashes a read. It is copied to the quarantine table (kept for export) before anything overwrites it (R11 T-010). | `QuarantineTest`, `MigrationTest` (3 to 4). |
| Clock changed mid-run | Wall-clock end times move relative to now. | Set forward: steps whose end has passed ring as overdue. Set back: the countdown grows by the change and the alarm rings that much later. | `TIME_SET` runs `restore`. End times are absolute epoch ms. | `RestoreTest` (restore at an arbitrary now). No device test. |
| Time zone changed mid-run | Nothing (epoch ms don't depend on zone). | Clock times (footer, notification) show in the new zone. | `TIMEZONE_CHANGED` runs `restore`. | No device test. |
| Phone reboots mid-run | Pending alarms are cleared by the system. | After the first unlock, alarms come back. A step that ended before the unlock rings right away, marked overdue. | `BOOT_COMPLETED` runs `restore`, which reschedules future alarms. State is in credential-protected storage, so this waits for the first unlock. | `RestoreTest`; `docs/MANUAL-TEST.md` (reboot). Real phone. |
| No phone alarm tone set (OnePlus) | The system default alarm sound is null. | A two-beep tone. | Falls back to the bundled `alarm_beep.wav`. | Phone test in T1b. Real phone. |
| Alarm while the app is on screen | A heads-up would cover the top bar. | The alarm screen opens directly; "Vaporizer free" is a snackbar. | `Delivery` picks foreground or background delivery; leaving the app while ringing brings the lock-screen alarm back. | `DeliveryTest`, `AlarmActivityCloseTest`; E2E s11. |

## 5. Safety-related numbers

Treatment durations and doses are reference defaults; the product label governs. The app shows: "Defaults
are reference values. Follow your product label and vaporizer manual; the label is the legal use spec." It
sets no dose.

| Default | Value | Source |
|---|---|---|
| Keep sealed after vaporizing | 600 s (10 min) | Field study in Georgia and Alabama, 99 colonies, 2019 and 2020: seven ProVap 110 vaporizations 5 days apart, entrances kept sealed 10 min after each (https://pmc.ncbi.nlm.nih.gov/articles/PMC8825467; the same group in Bee Culture, https://beeculture.com/?p=56351). Found 2026-11-23 from a search summary; the paper itself was not read, so authors and journal are not given here (Jason to confirm). |
| Pan wand heat | 150 s | citation needed (Jason) |
| Pan wand cool in hive | 120 s | citation needed (Jason) |
| Gun type vaporize | 20 s | citation needed (Jason) |
| Dose | Not set by the app | Product label |

## 6. Definition of done (0.1.0-alpha.1)

- `scripts/check.sh` passes: ktlint, detekt, Android lint, unit tests, Semgrep, Snyk. No open high or critical
  finding without an entry in `docs/design/SECURITY-EXCEPTIONS.md`.
- The full E2E suite at 10x on the emulator, with zero invariant errors. Screenshots re-captured.
- No user-visible behavior change from `alpha-baseline`.
- A release-signed APK and its sha256 on a GitHub pre-release `v0.1.0-alpha.1`, built by `scripts/release.sh`,
  with the keystore backed up off GitHub.
- Upgrade from alpha.1 to alpha.2 tested on the emulator.
- Docs current: SPEC, TECH-DESIGN, TASKS, WBS, HANDOFF, README, INSTALL.
- Jason's real-phone list checked by him.

## 7. Original brief (history)

Kept as written. Section 1 wins where they disagree.

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

# Working on Bee Timer

## Project rules

- Stack: native Android. Kotlin, Jetpack Compose (Material 3), Room, AlarmManager. JDK 21. This overrides the
  global Expo default in `~/.claude/CLAUDE.md` for this repo. No Expo, no React Native. iOS comes later through
  Kotlin Multiplatform (see SPEC section 1).
- SPEC.md is the source of truth; its decisions table wins over the original brief at the bottom. The mockups
  in `docs/mockups/` are the source of truth for layout and look; open them before building a screen.
- Keep `scheduler/` pure and KMP-ready (iOS later): Kotlin stdlib and kotlinx.serialization only. No `java.*`,
  `javax.*`, `android.*`, no Java time APIs, no `@Jvm*`, no JVM-only collections or concurrency. Times are
  epoch-millisecond `Long`s, passed in, never read from a clock. `MultiplatformGuardTest` enforces this.
- The scheduler is a pure reducer: `(state, event, now) -> (newState, effects)`. The app executes effects. No
  scheduling logic in `app/`. Add a unit test for every scheduler change and a regression test for every bug.
- The alarm layer (`app/.../alarms`) is the platform boundary: AlarmManager on Android, AlarmKit on iOS 26+.
- `app/` is packaged by layer: `data`, `alarms`, `ui`. All UI strings in `res/values/strings.xml`. All colors,
  type and sizes in `ui/theme/Theme.kt`; nothing inline in screens.
- Hive names are free text (Emory, Herb, Sparky). Never hardcode "Hive N" in copy; use the label.
- UI copy: plain, short, straight apostrophes, no em dashes. Same for README and comments.

## Working rules

- Reviewed mode: one task at a time, stop after each task for review, never start the next task unasked.
  Autonomous mode: follow the round's brief and its hard stops (R11 is autonomous).
- After every task: commit with the task ID, update its status in `docs/design/TASKS.md`, update
  `docs/HANDOFF.md`, regenerate `docs/dashboard.html` (`python3 scripts/dashboard.py`), and log it in
  `docs/design/USAGE.md` (task, model, agent or main, start, end, size, 5-hour and weekly % at start and end,
  throttle decision, notes).
- Start the dashboard timer at the start of a session (`scripts/dashboard-timer.sh start`); stop it before
  the final handoff of a round.
- Uninstalling the app, clearing app data, or any other destructive action on a device or in the repo
  (deleting tracked files, rewriting pushed history, force push) needs an explicit yes from Jason. Don't read
  consent into an unclear reply.
- No UI element that isn't in `docs/mockups` or SPEC without asking Jason first. Mockups win over any older
  version of the app unless SPEC says otherwise.
- Behavior and design changes: stop and ask. Small bugs: fix them with a regression test and note them.
- "Device testing (Jason)" in a task says exactly what to judge and what to compare it against, or "none".
- Every bug fix gets a regression test that fails before the fix.
- Never report a check as passing without having run it (a Gradle UP-TO-DATE result is not a run). Say what
  wasn't verified.
- Never loosen or remove a test assertion to make a run pass. Log any changed E2E check in `docs/e2e/FIXES.md`.
- Never edit a file by truncating it and reading it back in the same pipeline. Write to a temp file, move it
  into place, and check it isn't empty.

## Quality gate

- `scripts/check.sh` is the gate. A task is not done until it passes. It runs, in order: ktlint (format),
  detekt (lint, zero warnings, no baseline file), Android lint, all unit tests (JUnit and Robolectric),
  Semgrep (Kotlin and secrets), Snyk open source and Snyk code (high and above). Until R11 T-008 lands, it
  runs unit tests and Android lint only.
- Android lint warnings about newer library versions are expected while compileSdk is 36.
- Formatting-only changes go in their own commit so behavior changes stay reviewable.
- `snyk monitor --all-projects` after a release or a push to `main`, not per task.

## Security

- Never commit tokens, keys, keystores (`*.jks`, `*.keystore`), `keystore.properties` or `local.properties`.
  Never print secrets.
- No high or critical finding left open without an entry in `docs/design/SECURITY-EXCEPTIONS.md` with scope
  and an expiry date.
- A security scanner that is skipped, or scans fewer projects than exist, fails the gate.
- Device tests: clear logcat (`adb logcat -c`) before and after each run. Never save logcat unless a test
  fails, and then only with token patterns redacted.
- Device screenshots always crop off the status bar (the repo is public). Mockup and app captures for UI
  review go in `docs/screenshots/` (`mockup-*.png`, `app-*.png`).

## Devices

- The emulator is the default for device tests. Use the real phone only for tasks marked Real phone (alarms in
  Doze, lock screen, OEM battery, real upgrades), and only when asked. Install with `adb install -r` only.
- Process death in tests: Home, then `am kill`, then `kill -9` the PID. Never `am force-stop`: it cancels all
  alarms, which doesn't happen in the field.
- Stop Gradle and Kotlin daemons before booting the emulator. Never run a release build with the emulator up.
- Device tests run at 1.15 and 1.3 font scale and put every phone setting back afterward.

## Data

- Room: `exportSchema` is on and `app/schemas/` is committed. Shipped migrations and schema files are frozen
  (checksum test). A schema change bumps the version, adds a new migration and commits the new JSON. Never
  edit a committed schema file or a shipped migration.
- Corrupt data never crashes a read, and is quarantined (kept for export) before anything overwrites it.
- Treatment durations and doses are reference defaults; the product label governs. Every treatment or
  safety number cites a primary source, or is marked "citation needed (Jason)".

## Layout

- Changing a value never moves the control being tapped or anything above it: tabular digits, fixed-height
  rows for computed values, warnings below the control, reserve bold label widths, no auto-scroll.
- Never ellipsize the text the user needs to act on (step names, next actions). Wrap instead.

## Models and agents

- Main session: the strongest model available. It plans, merges, runs the gate and talks to Jason.
- Agents: the cheapest model that does the job well. Strongest: design judgment, tricky native or debugging
  work, security decisions. Opus: implementation from a clear spec, tests, refactors, code review. Sonnet or
  Haiku: mechanical work (doc edits, renames, log scraping).
- Parallel tasks each get their own worktree off local `main`, with no emulator and no gate in the worktree,
  and stop with a report. Never run two agents that edit the same file. At most 5 agents at once.
- Device work is serial: one emulator job at a time. The main session owns the emulator and the phone.
- Merge one branch at a time, run the gate after each merge, then delete the worktree and branch.
- Stack UI reviews for Jason into one list.
- Before starting any task or agent, check the usage throttle (playbook section 9) against
  `~/.claude/beetimer-usage.json` and log the decision in `docs/design/USAGE.md`. The stricter of the weekly
  budget table and the 5-hour table wins.
- Never spawn an agent just to refresh the dashboard or poll for status; that's the timer's job.

## Plan

- `docs/HANDOFF.md`: where things stand, open items and gotchas. Read it first in a new session, and update it
  after each task.
- `docs/design/TECH-DESIGN.md` (architecture, scheduler rules, alarms), `docs/design/TASKS.md` (T-001...),
  `docs/design/WBS.md`, `docs/design/USAGE.md`, `docs/design/SECURITY-EXCEPTIONS.md`.
- `docs/PLAYBOOK.md` is the general process; `docs/design/PLAYBOOK-ADOPTION.md` is the R11 work order.

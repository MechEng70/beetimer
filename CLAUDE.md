# Working on Bee Timer

- Stack: native Android. Kotlin, Jetpack Compose (Material 3), Room, AlarmManager. JDK 21. This overrides the
  global Expo default in `~/.claude/CLAUDE.md` for this repo. No Expo, no React Native.
- SPEC.md is the source of truth; its section 1 (Decisions) wins over the original brief below it. The mockups
  in `docs/mockups/` are the source of truth for layout and look; open them before building a screen.
- Keep `scheduler/` pure and KMP-ready (iOS later): Kotlin stdlib and kotlinx.serialization only. No `java.*`,
  `javax.*`, `android.*`, no Java time APIs, no `@Jvm*`, no JVM-only collections or concurrency. Times are
  epoch-millisecond `Long`s, passed in, never read from a clock. `MultiplatformGuardTest` enforces this.
- The scheduler is a pure reducer: `(state, event, now) -> (newState, effects)`. The app executes effects. No
  scheduling logic in `app/`. Add a unit test for every scheduler change and a regression test for every bug.
- The alarm layer (`app/.../alarms`) is the platform boundary: AlarmManager on Android, AlarmKit on iOS 26+.
- `app/` is packaged by layer: `data`, `alarms`, `ui`. All UI strings in `res/values/strings.xml`. All colors,
  type and sizes in `ui/theme/Theme.kt`; nothing inline in screens.
- Room: `exportSchema` is on and `app/schemas/` is committed. A schema change bumps the version, adds a
  migration and commits the new JSON. Never edit a committed schema file.
- Hive names are free text (Emory, Herb, Sparky). Never hardcode "Hive N" in copy; use the label.
- Never commit signing keys (`*.jks`, `*.keystore`, `keystore.properties`) or `local.properties`.
- Destructive actions need an explicit yes from Jason first: uninstalling the app, clearing app data, or
  anything destructive on a device or in the repo.
- Device screenshots crop off the status bar (the repo is public). Mockup and app captures for UI review go in `docs/screenshots/` (`mockup-*.png`, `app-*.png`).
- No UI element that isn't in `docs/mockups` or SPEC without asking Jason first.
- UI copy: plain, short, straight apostrophes, no em dashes. Same for README and comments.

# Plan

- `HANDOFF.md`: where things stand, open items and gotchas. Read it first in a new session, and update it
  at the end of each round.
- `docs/design/TECH-DESIGN.md` (architecture, scheduler rules, alarms), `docs/design/TASKS.md` (T1...),
  `docs/design/WBS.md`. Work one task at a time, commit with the task ID, update its status line when done,
  and stop at each STOP task.

# Quality gate (alpha)

- `scripts/check.sh`: all unit tests plus Android lint. A task is not done until it passes. Lint warnings
  about newer library versions are expected while compileSdk is 36.
- Semgrep, Snyk, ktlint and detekt are deferred until after alpha (Jason, 2026-11-21).

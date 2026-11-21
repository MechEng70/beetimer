# Varroa OAV Timer: project rules

This file overrides the global stack in ~/.claude/CLAUDE.md for this project only.

## Stack (overrides global Expo default)
- Native Android: Kotlin, Jetpack Compose (Material 3), Room, AlarmManager. No Expo, no React Native.
- `:core` is a pure Kotlin JVM module (scheduler, model, validation). No Android imports, ever.
- `:app` is the Android layer. It executes scheduler effects; it holds no scheduling logic.
- Spec: `varroa-timer-claude-code-prompt.md`. Approved mockups: `varroa-timer-mockups.html`.
- Design: `docs/design/TECH-DESIGN.md`. Work items: `docs/design/TASKS.md`.

## Phase: ALPHA (proof of concept, speed first)
- Quality gate for alpha: `scripts/check.sh` = `./gradlew test lint` + Semgrep + Snyk (high).
- ktlint and detekt are deferred until after alpha (detekt 2 is still alpha and detekt 1.x lags Kotlin 2.4).
- Open high or critical findings go in `docs/design/SECURITY-EXCEPTIONS.md`.

## Rules
- Every scheduler change gets a unit test. Every bug fix gets a regression test.
- Commit per task with the task ID, e.g. `T3: scheduler reducer`.
- Never commit keystores, `local.properties`, or credentials.
- Straight apostrophes, no em dashes in UI strings, README, or comments. Plain short UI copy.
- Hive names are free text chosen by the user (e.g. Emory, Herb, Sparky). Never hardcode "Hive N" in copy; always use the label.

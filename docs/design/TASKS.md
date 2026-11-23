# Tasks

Each task is written to pick up cold: read `CLAUDE.md`, `docs/HANDOFF.md`, `docs/design/TECH-DESIGN.md` and the
task. Commit with the task ID, update its status here, update `docs/HANDOFF.md`, and log it in
`docs/design/USAGE.md`.

Status: [ ] todo, [~] in progress, [x] done, [!] waiting on Jason.

Each R11 task has Goal, Files, Depends on, Size, Model, Parallel-safe, Acceptance (pass/fail), Tests and
Device testing (Jason), per the playbook planning prompt.

## Done before the playbook (alpha, 2026-11-21 to 2026-11-22)

TECH-DESIGN and SPEC section 1 describe the result. Commit IDs are on `main`; the tag `alpha-baseline`
(`571c8ee`) marks the end of this work.

| ID | What | Commit |
|---|---|---|
| T0 | Plan docs: tech design, tasks, WBS | `44917df` |
| T1 | Gradle scaffold, KMP-ready `:scheduler` module | `10fa45f` |
| T1b | Alarm spike; ring from a foreground service with a bundled tone (passed on the phone) | `10fa45f`, `91769ad` |
| T2 | Scheduler model, presets, template validation | `68f5126` |
| T3 | Pure scheduler reducer, restore, property test (46 tests) | `4add257` |
| T4 to T9 | Room data layer, scheduler engine, alarms and receivers, notifications and alarm screen, main UI, settings, first-run permissions | `0c030d7` |
| T10 | Build script, manual phone test, known limitations | `3e27d9d` |
| R1 | UI rework to the mockups; alarm stream; seeding regression test | `a6a203a` |
| R2 | Planning view, step editor, pan wand fix (`MIGRATION_1_2`), Inter, hive drag, max 5 | `0f40d86` |
| R3 | All tab countdowns, footer clock, heads-up and banner copy | `022ea21` |
| R4 | Cancel returns the hive to Not started; "Vaporizer" in UI copy | `0dc7c89` |
| R5 | Take and release markers (`MIGRATION_2_3`), sealed-hold option, planner firm fix, debug DevTools, E2E suite s1 to s10 | `4350039` |
| R6 | Foreground alarms and snackbar, alarm-close fix (`AlarmActivityCloseTest`), scenario 11, stopwatch bee icon | `7c6bc8e` |
| Docs | All docs brought up to date with R1 to R6, HANDOFF.md added | `01ace13` |
| R7 | All tab GUI cleanup: Coming up shows 3 events with no times; larger rows for outdoors | `3f0c6fb` |
| R8 | Next-step pill, amber or teal outline | `6f91a23` |
| R9 | Footer copy (`footerFor`, `FooterTest`) | `9e12b85` |
| R10 | Row layout (chip on line 1 only, step names never cut off); volume snackbar replaces the banner (`VolumeCheck`) | `282cd5b` |
| Docs | HANDOFF restored and updated; first push to GitHub; social preview | `337e5ae` to `571c8ee` |

T11 (release keystore) was never started. R11 replaces it with T-014 to T-017.

## Round R11: playbook adoption

Work order: `docs/design/PLAYBOOK-ADOPTION.md`. No user-visible behavior change; `alpha-baseline` is the
oracle. Run through all tasks in order and stop only at the brief's hard stops.

### Phase 0: baseline

#### T-001 [x] Baseline and tracking (M)
- **Goal:** a fixed reference point, the files R11 needs, and live usage tracking (playbook section 9).
- **Files:** tag `alpha-baseline`; `HANDOFF.md` to `docs/HANDOFF.md`; links in README, CLAUDE.md,
  TECH-DESIGN; `docs/PLAYBOOK.md`, `docs/design/PLAYBOOK-ADOPTION.md`, `docs/design/USAGE.md`;
  `scripts/statusline.sh` wired in `~/.claude/settings.json` (backup `settings.json.bak-2026-11-23-beetimer`);
  `scripts/dashboard.py`, `docs/dashboard.html`, `scripts/dashboard-timer.sh`; `scripts/check.sh` writes
  `build/gate/last.tsv`.
- **Depends on:** none.
- **Size:** M. **Model:** main (Opus 5.5). **Parallel-safe:** no.
- **Acceptance:** tag exists on `571c8ee`; no link to the old HANDOFF path; status bar shows 5-hour and weekly
  % and the dashboard shows them; timer running; `git status` clean; gate passes.
- **Tests:** gate (101 tests and lint, `--rerun-tasks`); status line on a fake payload and a real one;
  dashboard loads in headless Chrome with 0 page errors.
- **Device testing (Jason):** none. Look at the status bar and open `docs/dashboard.html`.
- Done: `135e619` (baseline), tracking commit after the brief was updated (see git log).

### Phase 1: documents

#### T-002 [x] CLAUDE.md (S)
- **Goal:** playbook sections 6 and 6b in CLAUDE.md, adapted to Kotlin, keeping every project rule.
- **Files:** `CLAUDE.md`.
- **Depends on:** T-001.
- **Size:** S. **Model:** main (Opus 5.5). **Parallel-safe:** yes.
- **Acceptance:** working, security, devices, data, layout and models sections present; ktlint and detekt
  named instead of Prettier and ESLint; Room migrations rule; no "tools deferred" section; every old rule kept.
- **Tests:** none (docs).
- **Device testing (Jason):** none.
- Done: `1df14c3`.

#### T-003 [x] SPEC.md (M)
- **Goal:** SPEC in the playbook shape.
- **Files:** `SPEC.md`.
- **Depends on:** T-001.
- **Size:** M. **Model:** main (Opus 5.5). **Parallel-safe:** yes.
- **Acceptance:** what it does, users and phone, one job, screens, rules, decisions table (kept), failure-mode
  table with every case in the brief, safety numbers with sources or "citation needed (Jason)", definition
  of done, original brief at the bottom.
- **Tests:** none (docs).
- **Device testing (Jason):** none.
- Done: `6ba1749`.

#### T-004 [x] TASKS.md and WBS.md (S)
- **Goal:** history condensed with commit IDs; R11 tasks in playbook format; WBS by phase.
- **Files:** `docs/design/TASKS.md`, `docs/design/WBS.md`.
- **Depends on:** T-001.
- **Size:** S. **Model:** main (Opus 5.5). **Parallel-safe:** yes.
- **Acceptance:** every old task and round listed with its commit; T-001 to T-020 each have Goal, Files,
  Depends on, Size, Model, Parallel-safe, Acceptance, Tests, Device testing (Jason).
- **Tests:** none (docs).
- **Device testing (Jason):** none.
- Done: `730b788`.

#### T-005 [x] TECH-DESIGN.md and SECURITY-EXCEPTIONS.md (M)
- **Goal:** TECH-DESIGN in the playbook outline with the iOS path; an empty exceptions table.
- **Files:** `docs/design/TECH-DESIGN.md`, `docs/design/SECURITY-EXCEPTIONS.md`.
- **Depends on:** T-003.
- **Size:** M. **Model:** main (Opus 5.5). **Parallel-safe:** yes.
- **Acceptance:** sections for layers and what each may import, data model and schema, native pieces,
  security, test strategy, risks, iOS path; exceptions table with columns finding, scope, reason, owner,
  expiry.
- **Tests:** none (docs).
- **Device testing (Jason):** none.
- Done: `31e66c5`.

### Phase 2: quality gate

#### T-006 [x] Format and lint (L)
- **Goal:** ktlint and detekt with zero warnings and no baseline file.
- **Files:** `build.gradle.kts`, `gradle/libs.versions.toml`, `.editorconfig`, `config/detekt/detekt.yml`,
  Kotlin sources.
- **Depends on:** T-001.
- **Size:** L. **Model:** main (Opus 5.5). **Parallel-safe:** no (touches every Kotlin file).
- **Acceptance:** `ktlintCheck` and `detekt` pass on both modules with no baseline; formatting-only changes in
  their own commit; all tests still pass.
- **Tests:** full unit suite unchanged and green.
- **Device testing (Jason):** none.
- Done: `9e7c239` (format), `e45cb25` (detekt).

#### T-007 [x] Security scanners (M)
- **Goal:** Semgrep (Kotlin, secrets) and Snyk (open source and code, high and above) in the gate.
- **Files:** `scripts/check.sh` or `scripts/security.sh`, `docs/design/SECURITY-EXCEPTIONS.md`.
- **Depends on:** T-005.
- **Size:** M. **Model:** main (Opus 5.5): security decisions. **Parallel-safe:** no.
- **Acceptance:** the gate fails if a scanner is missing, not logged in, skipped, or scans fewer Gradle
  projects than exist; findings fixed or recorded with scope and expiry.
- **Tests:** run the gate; show it fails when the scanner is absent or scans too few projects.
- **Device testing (Jason):** none.
- Done: `12dea94`.

#### T-008 [x] Gate script (S)
- **Goal:** one `scripts/check.sh`: ktlint, detekt, Android lint, unit tests, Semgrep, Snyk.
- **Files:** `scripts/check.sh`, `docs/DEVELOPING.md`.
- **Depends on:** T-006, T-007.
- **Size:** S. **Model:** main (Opus 5.5). **Parallel-safe:** no.
- **Acceptance:** each step runs in that order and a failure in any fails the gate; tests are re-run, not
  taken from the Gradle cache.
- **Tests:** the gate itself, run end to end.
- **Device testing (Jason):** none.
- Done: `828fb55`.

### Phase 3: data and layout rules

#### T-009 [x] Migrations frozen (S)
- **Goal:** shipped Room migrations and schema files can't change unnoticed.
- **Files:** `app/src/test/.../data/FrozenSchemaTest.kt`.
- **Depends on:** T-008.
- **Size:** S. **Model:** Opus agent or main. **Parallel-safe:** yes, with T-010 (separate files).
- **Acceptance:** a checksum test over `app/schemas/` versions 1 to current and the shipped migration SQL
  fails if any of them changes.
- **Tests:** the new test; show it fails on a changed file.
- **Device testing (Jason):** none.
- Done: `9033a70`.

#### T-010 [x] Corrupt data (M)
- **Goal:** a corrupt or unreadable row never crashes a read, and is quarantined before anything overwrites it.
- **Files:** `data/Repository.kt`, `data/Db.kt` (new quarantine table, Room v4 and `MIGRATION_3_4`),
  `app/schemas/.../4.json`, tests.
- **Depends on:** T-009.
- **Size:** M. **Model:** Opus agent or main. **Parallel-safe:** yes, with T-009.
- **Acceptance:** a corrupt state row and a corrupt step-list row each load without a crash and are kept in
  the quarantine table; healthy data behaves exactly as `alpha-baseline`.
- **Tests:** corrupt run row, corrupt preset row, `MIGRATION_3_4`, healthy round trip.
- **Device testing (Jason):** none.
- Done: `b459c89`.

#### T-011 [x] Layout audit (M)
- **Goal:** check the playbook layout rule and fix only real violations.
- **Files:** `ui/` screens and `ui/theme/Theme.kt` as needed.
- **Depends on:** T-008.
- **Size:** M. **Model:** main (Opus 5.5). **Parallel-safe:** no (screenshots on the emulator).
- **Acceptance:** tabular digits on every countdown; fixed heights for computed values; no auto-scroll;
  every fix compared against `alpha-baseline` screenshots, nothing else moved.
- **Tests:** unit tests for any changed pure code; screenshots before and after for any fix.
- **Device testing (Jason):** watch a countdown on the All tab and the Hive tab tick from 10:00 to 9:59 and
  1:00 to 0:59: the text next to it must not shift.
- Done: `3fd10f4`.

### Phase 4: device test hygiene

#### T-012 [x] E2E rules (M)
- **Goal:** the Maestro harness follows the playbook device rules.
- **Files:** `scripts/e2e/run.sh`, `scripts/e2e/screens.sh`, `docs/e2e/README.md`.
- **Depends on:** T-008.
- **Size:** M. **Model:** main (Opus 5.5). **Parallel-safe:** no (emulator).
- **Acceptance:** `adb logcat -c` before and after each run; logcat saved only on failure, with token
  patterns redacted; status bar cropped from every capture; runs at font scale 1.15 and 1.3 with every
  setting restored; Gradle and Kotlin daemons stopped before booting the emulator.
- **Tests:** one short scenario run on the emulator, showing settings restored afterward.
- **Device testing (Jason):** none.
- Done: `92d015f`.

#### T-013 [~] Full E2E at 10x (L)
- **Goal:** the whole suite at 10x on the emulator, zero invariant errors, screenshots re-captured.
- **Files:** `docs/e2e/REPORT.md`, `docs/e2e/*`, `docs/screenshots/app-*.png`.
- **Depends on:** T-010, T-011, T-012.
- **Size:** L. **Model:** main (Opus 5.5). **Parallel-safe:** no (emulator).
- **Acceptance:** zero invariant errors; every difference from `alpha-baseline` reported.
- **Tests:** the suite.
- **Device testing (Jason):** compare the new `docs/screenshots/app-*.png` with the previous commit's: they
  should match except the status bar crop.

#### T-013a [ ] Alarm screen readability (M)
- **Goal:** Jason's approved design change (2026-11-23), display only: finished line 24 sp, "Next for <hive>"
  18 sp, next action 36 sp semibold (two lines, never cut off), timer line 20 sp, "Then: <hive>, <step>" 20 sp
  when Done frees the vaporizer, hive name 56 sp, tighter space around the divider; buttons unchanged.
- **Files:** `ui/AlarmActivity.kt`, `alarms/AlarmText.kt`, `ui/theme/Theme.kt`, `strings.xml`, tests.
- **Depends on:** T-013.
- **Size:** M. **Model:** main (Opus 5.5). **Parallel-safe:** no.
- **Acceptance:** fits at font scale 1.15 and 1.3 without reaching the buttons; next action never ellipsized;
  sizes are tokens; s6 and s11 pass at 10x; `app-3-alarm.png` re-captured at 1.15 and 1.3; SPEC section 1.
- **Tests:** `AlarmLayoutTest`, `AlarmTextTest`; E2E s6 and s11.
- **Device testing (Jason):** on the phone, let a step end with the screen locked; compare the alarm screen
  with `docs/screenshots/app-3-alarm.png`: everything readable at arm's length, nothing over the buttons.

### Phase 5: release

#### T-014 [ ] Versioning (S)
- **Goal:** versionCode = (major*10000 + minor*100 + patch) * 1000 + stage (alpha.N = N, beta.N = 300+N,
  rc.N = 600+N, final = 999). First signed build 0.1.0-alpha.1 = 100001.
- **Files:** `app/build.gradle.kts`, a pure version helper, its test.
- **Depends on:** T-008.
- **Size:** S. **Model:** Opus agent or main. **Parallel-safe:** yes, with T-015.
- **Acceptance:** the APK reports versionName `0.1.0-alpha.1` and versionCode 100001.
- **Tests:** unit test for the formula (all four stages, edge values).
- **Device testing (Jason):** none.

#### T-015 [ ] Release script (M)
- **Goal:** `scripts/release.sh` builds and checks a signed APK.
- **Files:** `scripts/release.sh`, `app/build.gradle.kts` (signing config), `.gitignore`.
- **Depends on:** T-014.
- **Size:** M. **Model:** Opus agent or main. **Parallel-safe:** yes, with T-014.
- **Acceptance:** runs the gate, builds, signs, checks the certificate fingerprint, app ID and versionCode,
  writes `release/beetimer-<version>.apk` and its sha256; fails if the keystore or `keystore.properties` is
  missing. `.gitignore` covers `*.jks`, `*.keystore`, `keystore.properties`, `release/` and device test output.
- **Tests:** run without a keystore and show it fails.
- **Device testing (Jason):** none.

#### T-016 [ ] Keystore (hard stop 2) (S)
- **Goal:** the exact steps for Jason to make and back up the release keystore.
- **Files:** `docs/DEVELOPING.md`.
- **Depends on:** T-015.
- **Size:** S. **Model:** main (Opus 5.5). **Parallel-safe:** no.
- **Acceptance:** `keytool`, `chmod 600` and backup steps written; keystore outside the repo
  (`~/keys/beetimer-release.jks`). Stop for Jason to run them and confirm the backup.
- **Tests:** none.
- **Device testing (Jason):** none. Jason runs the commands and confirms the backup.

#### T-017 [ ] Upgrade path (M)
- **Goal:** release build tested fresh and as an upgrade from itself.
- **Files:** `docs/e2e/` notes, `docs/INSTALL.md`.
- **Depends on:** T-016.
- **Size:** M. **Model:** main (Opus 5.5). **Parallel-safe:** no (emulator).
- **Acceptance:** on the emulator, alpha.1 installs fresh and alpha.2 installs over alpha.1 with data kept.
  Jason's phone is not touched; the uninstall and reinstall step goes on his list.
- **Tests:** emulator install and upgrade.
- **Device testing (Jason):** uninstall the debug alpha (hive names are lost), install alpha.1, rename hives.

### Phase 6: GitHub

#### T-018 [ ] Push (S)
- **Goal:** `main` and tags on GitHub, nothing that shouldn't be public.
- **Files:** none.
- **Depends on:** T-017.
- **Size:** S. **Model:** main (Opus 5.5). **Parallel-safe:** no.
- **Acceptance:** no status-bar screenshots, keystores, `local.properties` or secrets in any commit; `main`
  and the tags pushed. Anything bad in history is a stop, not a rewrite.
- **Tests:** history scan.
- **Device testing (Jason):** none.

#### T-019 [ ] Release (S)
- **Goal:** GitHub pre-release `v0.1.0-alpha.1` with the APK and sha256.
- **Files:** none in the repo.
- **Depends on:** T-016, T-018.
- **Size:** S. **Model:** main (Opus 5.5). **Parallel-safe:** no.
- **Acceptance:** release published with notes from `docs/INSTALL.md`; README has the icon header.
- **Tests:** download the asset and check its sha256.
- **Device testing (Jason):** none.

### Phase 7: wrap-up

#### T-020 [ ] Final report (S)
- **Goal:** one list for Jason.
- **Files:** `docs/HANDOFF.md`.
- **Depends on:** all.
- **Size:** S. **Model:** main (Opus 5.5). **Parallel-safe:** no.
- **Acceptance:** gate and E2E pass, docs current; one list with every real-phone test, every hard stop still
  waiting, everything under "Found during R11", and the USAGE summary.
- **Tests:** the gate.
- **Device testing (Jason):** the list.

## Found during R11 (for Jason; not fixed in R11 unless a task above covers it)

- **Corrupt rows (baseline).** A corrupt step-list row crashed app start. A corrupt state row was read as
  empty and overwritten on the next save. Fixed in T-010 (quarantine; regression tests in `QuarantineTest`).
- **Exact alarm refused (baseline).** If scheduling throws `SecurityException` (Android 12 only, access
  revoked), `AlarmScheduler.guarded` logs it and the alarm is silently not set. Not reachable on Android 13+,
  where `USE_EXACT_ALARM` can't be revoked. Not fixed: it would add UI.
- **Layout rule: vaporizer bar and Coming up (baseline, design).** The bar grew by a button row when the
  vaporizer freed, and Coming up appeared on the first Start; both moved the control just tapped. Jason,
  2026-11-23: change the design. Done: the bar is two fixed lines with no buttons, Coming up is always 3 rows,
  Skip to back moved to every queued hive's tab (`Event.SkipToBack`), Remove is Cancel run (`FixedLayoutTest`).
- **Layout audit, the rest (T-011).** Countdowns now use tabular digits (fixed). No auto-scroll anywhere. Chips
  have fixed minimum heights. A row grows when a long step name wraps (R10 decision), which happens on a state
  change, not a tick, and below the tapped control.
- **Citations.** Answered by Jason 2026-11-23 (SPEC section 5): 10 min hold (Berry et al. 2022), heat and cool
  (Nashville Area Beekeepers handout, secondary), gun 20 s (Dadant ProVap 110 page, secondary).

## Open checks (need Jason)

- Themed icons on the OnePlus: Settings, Wallpapers & style, Icons, Themed icons, then look at Bee Timer on
  the home screen.
- [x] GitHub social preview set to `docs/icon/icon-1024.png` (Jason, 2026-11-22).

## Backlog (not scheduled; don't build without asking)

- **B1 Overlap timeline chart** on the All tab: one lane per hive across time, vaporizer spans in amber,
  passive in teal, from the planner forecast.
- **B2 Number of vaporizers** setting: the lock becomes a count; queue and planner follow.
- **B3 Dark theme for the main screens**: tokens exist in `Theme.kt`; needs a visual pass and screenshots.
- **B4 Add and delete steps in the step editor**: the model has `addStep` and `removeStep`; needs UI that
  isn't in the mockups, so ask first.
- **B5 Session history**: deferred from the brief.

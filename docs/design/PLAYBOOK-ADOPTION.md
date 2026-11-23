# Playbook adoption brief (round R11)

Written in Claude chat with Jason on 2026-11-23. Claude Code: this is your work order. Read it fully before starting.

## Goal

Bring Bee Timer up to the New App Playbook (docs/PLAYBOOK.md) without changing how the app behaves. The alpha on `master` works and Jason likes it. This round is process, docs, quality gate, release and GitHub, not features.

## Decisions already made (do not reopen)

| Decision | Value | Why |
|---|---|---|
| Stack | Stay native Kotlin, Jetpack Compose, Room, AlarmManager. Do not port to Expo or React Native. | The app works; alarms are the hard part and are native either way. |
| iOS path | Kotlin Multiplatform later: `:scheduler` moves to commonMain unchanged; UI in Compose Multiplatform or SwiftUI; alarm layer on AlarmKit (iOS 26+). | Already enforced by `MultiplatformGuardTest`. Keep it that way. |
| App ID | `io.github.mecheng70.beetimer`. Permanent. | Already installed on Jason's phone. |
| Display name | Bee Timer | |
| Repo | GitHub `MechEng70/beetimer`, local `~/Documents/git/beetimer` | |
| Playbook stack items that don't apply | Expo, React Native, TypeScript, NativeWind, Jest, Prettier, ESLint, expo-sqlite, expo-secure-store | Kotlin equivalents below. |
| Kotlin equivalents | ktlint (format), detekt (lint, zero warnings), Android lint, JUnit and Robolectric (unit and UI), Maestro (device), Room (data) | |

## The oracle rule

Before any change, tag the current `master` as `alpha-baseline`. Any question about how something should behave is answered by that build: install it on the emulator and look. If a change alters behavior visible to the user, it's a bug; revert it. If the baseline itself looks wrong, don't fix it in this round: log it in TASKS.md under "Found during R11" for Jason.

## Operating mode: run everything, stop only at hard stops

Work through all tasks below in order without waiting for review between tasks. Commit after each task with its ID. Update `docs/HANDOFF.md` after each task (what's done, what's next, anything odd). This makes any interruption harmless.

Hard stops. Stop, explain, and wait for Jason only for these:
1. Anything destructive on Jason's phone or in the repo (uninstall, clear data, delete tracked files, rewrite pushed history, force push).
2. Creating the release keystore or handling its passwords. Prepare everything, then stop and hand Jason the exact commands to run himself.
3. A security tool that needs Jason's login (Snyk not authenticated, for example).
4. A test that only a real phone can do. Collect them into one list for the end instead of stopping mid-run, unless the next task depends on the answer.
5. A high or critical security finding you can't fix without changing behavior.
6. The playbook and this brief conflict in a way the oracle rule can't settle.

Everything else: decide using the oracle rule and the playbook, note the decision in SPEC.md section 1, and keep going.

## Usage discipline

You can't see the plan's usage meter reliably, so make limits harmless instead:
- One task at a time, committed and written into HANDOFF.md before the next starts.
- Log each task in `docs/design/USAGE.md`: task ID, model, start and end time, size (S, M or L), notes. This shows Jason where usage goes.
- Use the cheapest model that does the job well (playbook section 6b). Mechanical doc edits and renames: Sonnet or Haiku. Implementation and tests: Opus. Design judgment and tricky debugging: the strongest model.
- No parallel agents this round. The app is small, and device work is serial anyway.
- If you see a usage-limit warning, finish the current task, commit, update HANDOFF.md, and stop. Don't start an L task in that state.
- Jason restarts with the section 5 restart prompt from the playbook. HANDOFF.md must make that work cold.

## Tasks

Number them T-001 onward in TASKS.md, in the playbook format: Goal, Files, Depends on, Acceptance (pass/fail), Tests, Device testing (Jason). Suggested set; adjust sizes but keep the order.

### Phase 0: baseline
- **T-001 Baseline.** Tag `alpha-baseline`. Move `HANDOFF.md` to `docs/HANDOFF.md` and fix every link to it. `docs/PLAYBOOK.md` and this brief are already in place (Jason's chat put them there); commit them in this task. Create `docs/design/USAGE.md`. Confirm `git status` is clean and the gate passes before anything else.

### Phase 1: documents
- **T-002 CLAUDE.md.** Merge playbook sections 6 and 6b into CLAUDE.md, adapted to Kotlin (ktlint and detekt instead of Prettier and ESLint, Room migrations instead of generic ones). Keep every existing project rule and gotcha. Remove the "ALPHA phase, tools deferred" section; the tools come back in Phase 2.
- **T-003 SPEC.md.** Restructure to the playbook shape: what it does, users and phone, the one job, screens, rules (scheduler rules, take and release markers, sealed-hold option, planner firm versus estimated times), decisions table, failure-mode table, definition of done. Keep section 1 decisions. The original brief stays at the bottom as history.
  - Failure-mode table (FMEA style), at least: alarm doesn't fire (Doze, OEM battery kill, exact-alarm permission revoked), alarm silent (low alarm volume, DND), full-screen intent denied, process killed mid-run, two hives need the vaporizer at once, user edits the preset mid-run, corrupt database row, clock or time zone change mid-run, phone reboot mid-run. For each: what breaks, what the user sees, what the app does, and how it's tested.
  - Safety-related numbers: treatment durations and doses are reference defaults; the product label governs. Cite a source for each default you keep. The 10-minute sealed hold came from a published ProVap study, not the video. If you can't verify a citation, mark it "citation needed (Jason)" rather than inventing one.
- **T-004 TASKS.md and WBS.md.** Convert history: T1 to T10 and R1 to R10 become a "Done before playbook" section with their commit IDs. New tasks from this brief go below in T-format. WBS.md gets the phases.
- **T-005 TECH-DESIGN.md and SECURITY-EXCEPTIONS.md.** Refresh TECH-DESIGN to the playbook outline (layers and what each may import, data model and schema, native pieces, security, test strategy, risks) and add the iOS path from the decisions table. Create `docs/design/SECURITY-EXCEPTIONS.md` with an empty table: finding, scope, reason, owner, expiry.

### Phase 2: quality gate
- **T-006 Format and lint.** Add ktlint and detekt. Zero warnings: fix the code rather than adding a baseline file. Formatting-only changes go in their own commit so behavior changes stay reviewable.
- **T-007 Security scanners.** Add Semgrep (Kotlin, secrets) and Snyk (open source and code, high and above) to `scripts/check.sh`. The gate fails if any scanner is skipped or scans fewer projects than exist. If Snyk isn't logged in on this machine, that's hard stop 3. Fix findings or record them in SECURITY-EXCEPTIONS.md with scope and expiry.
- **T-008 Gate script.** `scripts/check.sh` runs: ktlint, detekt, Android lint, unit tests, Semgrep, Snyk. Every bug fix still needs a regression test that fails before the fix. Never report a check as passing without running it.

### Phase 3: data and layout rules
- **T-009 Migrations frozen.** Add a checksum test over shipped Room migrations and exported schemas (versions 1 to current). Future changes go in new migrations only.
- **T-010 Corrupt data.** A corrupt or unreadable row never crashes a read. Quarantine it (keep it for export) before anything overwrites it. Tests for a corrupt run row and a corrupt preset row. No visible behavior change on healthy data.
- **T-011 Layout audit.** Check the playbook layout rule (a changing value never moves the control being tapped or anything above it): tabular digits on every countdown, fixed-height rows for computed values, no auto-scroll. Fix only real violations, and confirm against `alpha-baseline` that nothing else moved.

### Phase 4: device test hygiene
- **T-012 E2E rules.** Update the Maestro harness: `adb logcat -c` before and after each run; logcat saved only on failure, with token patterns redacted; status bar cropped from every capture; runs at font scale 1.15 and 1.3 with every setting restored afterward. Stop Gradle and Kotlin daemons before booting the emulator.
- **T-013 Full E2E at 10x.** Run the whole suite at 10x on the emulator. Zero invariant errors. Re-capture all screenshots. Report anything that differs from `alpha-baseline`.

### Phase 5: release
- **T-014 Versioning.** versionCode = (major*10000 + minor*100 + patch) * 1000 + stage, where alpha.N = N, beta.N = 300+N, rc.N = 600+N, final = 999. First signed build: 0.1.0-alpha.1 = 100001. Add a unit test for the formula.
- **T-015 Release script.** `scripts/release.sh`: runs the gate, builds, signs, checks the certificate fingerprint, app ID and versionCode, writes `release/beetimer-<version>.apk` plus its sha256, and fails if the signing key or `keystore.properties` is missing. `.gitignore` covers `*.jks`, `*.keystore`, `keystore.properties`, `release/` and device test output.
- **T-016 Keystore (hard stop 2).** Write the exact `keytool` commands, `chmod 600` steps and backup steps into `docs/DEVELOPING.md`. Store the keystore outside the repo (for example `~/keys/beetimer-release.jks`). Then stop and ask Jason to run them and confirm the backup is in his password manager.
- **T-017 Upgrade path.** Jason's phone has the debug-signed alpha. A release-signed APK can't install over it: Android rejects a different signing key. Moving to release signing means one uninstall on his phone, which loses his hive names. Test the release build fresh and as an upgrade from itself (alpha.1 to alpha.2) on the emulator. Don't touch his phone; add the uninstall-and-reinstall step to the final list for Jason (hard stop 1).

### Phase 6: GitHub
- **T-018 Push.** Check `git remote -v`. If there's no origin, add `https://github.com/MechEng70/beetimer.git`. Before pushing, confirm no status-bar screenshots, keystores, `local.properties` or secrets are tracked anywhere in history. Push `master` and the tags. If history contains anything that shouldn't be public, stop (hard stop 1) rather than rewrite it.
- **T-019 Release.** After T-016 is done, run `scripts/release.sh` and create a GitHub Release `v0.1.0-alpha.1` with the APK and sha256, using docs/INSTALL.md for the notes. The README has the icon header. Jason uploads `docs/icon/icon-1024.png` as the social preview himself (GitHub has no API for it).

### Phase 7: wrap-up
- **T-020 Final report.** Gate passes, E2E passes, docs current. One list for Jason with every real-phone test, every hard-stop item still waiting on him, anything logged under "Found during R11", and the USAGE.md summary.

## Restart prompt for Jason

```
Read CLAUDE.md, docs/HANDOFF.md, docs/design/TASKS.md and docs/design/PLAYBOOK-ADOPTION.md. Run git status and git log -5 and tell me what's committed and what isn't. Check uncommitted work before building on it. Then continue round R11 from the next unfinished task, under the same rules.
```

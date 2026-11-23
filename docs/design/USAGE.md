# Usage log

One row per task (and per throttle decision), so Jason can see where plan usage goes. Times are local
(Eastern). Size: S (under 30 min), M (30 to 90 min), L (over 90 min). 5h and Wk are the 5-hour and weekly
used % from the status line snapshot (`~/.claude/beetimer-usage.json`) at the start and end of the task.
Throttle is the decision taken before the task (playbook section 9). "n/a": tracking wasn't set up yet.

| Task | Model | Agent or main | Start | End | Size | 5h start | 5h end | Wk start | Wk end | Throttle | Notes |
|---|---|---|---|---|---|---|---|---|---|---|---|
| T-001 | Opus 5.5 | main | 2026-11-23 10:50 | 2026-11-23 10:51 | S | n/a | n/a | n/a | n/a | n/a | Baseline part: tag, file move, gate re-run with `--rerun-tasks` |
| T-002 | Opus 5.5 | main | 2026-11-23 10:51 | 2026-11-23 10:52 | S | n/a | n/a | n/a | n/a | n/a | CLAUDE.md rewrite; docs only |
| T-003 | Opus 5.5 | main | 2026-11-23 10:52 | 2026-11-23 10:54 | M | n/a | n/a | n/a | n/a | n/a | SPEC restructure, failure modes from the code, 2 web searches for citations |
| T-004 | Opus 5.5 | main | 2026-11-23 10:54 | 2026-11-23 10:55 | S | n/a | n/a | n/a | n/a | n/a | TASKS history with commits, T-001 to T-020, WBS |
| T-005 | Opus 5.5 | main | 2026-11-23 10:55 | 2026-11-23 10:56 | S | n/a | n/a | n/a | n/a | n/a | TECH-DESIGN outline, security, risks, iOS path |
| T-001 | Opus 5.5 | main | 2026-11-23 10:57 | 2026-11-23 11:02 | M | 4 | 5 | 61 | 61 | normal (first snapshot) | Tracking part, after Jason updated the brief: status line, dashboard, timer |
| T-006 | Opus 5.5 | main | 2026-11-23 11:03 | 2026-11-23 11:11 | L | 5 | 7 | 61 | 61 | normal | ktlint 1.8.0 and detekt 1.23.8 as CLIs; 1,707 ktlint and 180 detekt findings to 0; refactors by extraction only |
| T-007 | Opus 5.5 | main | 2026-11-23 11:12 | 2026-11-23 11:19 | M | 7 | 9 | 61 | 61 | normal | Semgrep 0; Snyk OSS 26 tool-only findings fixed by upgrades; Snyk Code 0 (one 504 retry) |
| T-008 | Opus 5.5 | main | 2026-11-23 11:19 | 2026-11-23 11:24 | S | 9 | 10 | 61 | 62 | normal | Gate order fixed, --rerun-tasks for lint and tests, failure probe |
| T-009 | Opus 5.5 | main | 2026-11-23 11:24 | 2026-11-23 11:28 | S | 10 | 10 | 62 | 62 | normal | Checksum test for schemas 1-3 and both migrations; negative check done |
| T-010 | Opus 5.5 | main | 2026-11-23 11:28 | 2026-11-23 11:32 | M | 10 | 11 | 62 | 62 | normal | Room v4 quarantine, safe reads, 7 tests; regression shown failing on old decode |
| T-011 | Opus 5.5 | main | 2026-11-23 11:32 | 2026-11-23 11:36 | S | 11 | 12 | 62 | 62 | normal | tnum on ticking text, glyph-width test; 2 design cases logged for Jason |
| T-012 | Opus 5.5 | main | 2026-11-23 11:37 | 2026-11-23 11:43 | M | 12 | 0 | 62 | 62 | normal | Harness rules, emulator.sh, redact.py; s5 tried at 2 font scales (1.3 timing race at 60x) |
| T-013 | Opus 5.5 | main | 2026-11-23 11:43 | 2026-11-23 16:25 | L | 12 | 10 | 62 | 64 | normal (restart 13:38: 5h 5, Wk 63, day 7, budget 100: wide open) | Paused 12:45 to 13:38. 22 of 22 runs pass, 0 invariant errors. Harness fixes: autopilot race, scale tap lost at 1.3, analyzer 60x numbers, scale check, software GPU after 2 emulator crashes |
| T-008 fix | Opus 5.5 | main | 2026-11-23 15:55 | 2026-11-23 16:22 | S | 9 | 10 | 64 | 64 | normal | Dashboard missed T-013a (regression test); py-tests gate step (Jason) |
| T-013a | Opus 5.5 | main | 2026-11-23 16:24 | 2026-11-23 16:55 | M | 10 | 0 (reset) | 64 | 64 | normal (wide open) | Written in the earlier session; this session: test setup fix, detekt fold, gate, s6 and s11 at both scales, screenshots |
| T-014 | Opus 5.5 | main | 2026-11-23 16:56 | 2026-11-23 17:06 | S | 0 | 1 | 64 | 64 | normal (wide open) | Written in the earlier session; split from T-015; ktlint and detekt fixes in AppVersion; Snyk scans buildSrc |
| T-015 | Opus 5.5 | main | 2026-11-23 17:06 | 2026-11-23 17:12 | M | 1 | 2 | 64 | 64 | normal (wide open) | Written in the earlier session; ~/ expansion added; refusal shown three ways; backup wording marked pending |
| T-016 | Opus 5.5 | main | 2026-11-23 17:12 | 2026-11-23 17:20 | S | 2 | 3 | 64 | 64 | normal (wide open) | Signed build, cert recorded, release.sh end to end; SIGPIPE bug fixed with regression test; waits on Jason's backup confirmation |
| T-017 | Opus 5.5 | main | 2026-11-23 17:21 | 2026-11-23 17:40 | M | 3 | 4 | 64 | 64 | normal (wide open) | Emulator uninstall blocked by the classifier; asked Jason (yes, emulator only). Upgrade alpha.1 to alpha.2 keeps data |
| T-018 | Opus 5.5 | main | 2026-11-23 17:40 | 2026-11-23 17:46 | S | 4 | 4 | 64 | 64 | normal (wide open) | History scan (names, secret patterns, password values, PNG heights), push, snyk monitor |
| T-019 | Opus 5.5 | main | 2026-11-23 17:47 | 2026-11-23 17:52 | S | 4 | 4 | 64 | 65 | normal (wide open) | Asked Jason first (yes); release.sh at HEAD, pre-release published and verified |
| T-020 | Opus 5.5 | main | 2026-11-23 17:52 | 2026-11-23 17:58 | S | 4 | 4 | 65 | 65 | normal (wide open) | Final report in HANDOFF, gate, push, timer stopped |

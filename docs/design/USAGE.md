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

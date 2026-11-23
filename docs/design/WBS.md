# Work breakdown

Sizes: S under 30 min, M 30 to 90 min, L over 90 min of agent time. Actual times are in `USAGE.md`.

## Done before the playbook (alpha)

```
Bee Timer alpha (2026-11-21 to 2026-11-22), see TASKS.md for commits
1. Foundation ........ T0 plan, T1 scaffold, T1b alarm spike
2. Core .............. T2 model and validation, T3 reducer, restore, property test
3. Platform .......... T4 Room, T5 engine and alarms, T6 notifications and alarm screen, T9 permissions
4. UI ................ T7 main UI, T8 settings
5. Delivery .......... T10 APK, README, manual test
6. Review rounds ..... R1 to R10
```

## Round R11: playbook adoption

```
R11 (from 2026-11-23), work order docs/design/PLAYBOOK-ADOPTION.md
0. Baseline
   0.1 T-001 Baseline tag, HANDOFF move, usage log ............ S
1. Documents
   1.1 T-002 CLAUDE.md ........................................ S
   1.2 T-003 SPEC.md .......................................... M
   1.3 T-004 TASKS.md and WBS.md .............................. S
   1.4 T-005 TECH-DESIGN.md, SECURITY-EXCEPTIONS.md ........... M
2. Quality gate
   2.1 T-006 ktlint and detekt, zero warnings ................. L
   2.2 T-007 Semgrep and Snyk ................................. M   hard stop 3 if Snyk not logged in
   2.3 T-008 Gate script ...................................... S
3. Data and layout rules
   3.1 T-009 Migrations frozen ................................ S
   3.2 T-010 Corrupt data, quarantine ......................... M
   3.3 T-011 Layout audit ..................................... M
4. Device test hygiene
   4.1 T-012 E2E rules ........................................ M
   4.2 T-013 Full E2E at 10x .................................. L
5. Release
   5.1 T-014 Versioning ....................................... S
   5.2 T-015 Release script ................................... M
   5.3 T-016 Keystore ......................................... S   hard stop 2: Jason runs keytool
   5.4 T-017 Upgrade path ..................................... M
6. GitHub
   6.1 T-018 Push ............................................. S
   6.2 T-019 Release v0.1.0-alpha.1 ........................... S
7. Wrap-up
   7.1 T-020 Final report ..................................... S
```

Dependencies: Phase 1 needs T-001. T-006 and T-007 feed T-008; every later task needs the full gate.
T-013 needs T-010 to T-012. T-017 and T-019 need the keystore from T-016 (Jason). T-018 follows T-017 in
the brief's order.

Risks:
- ktlint and detekt on existing code: many findings, and fixes must not change behavior. Mitigation:
  formatting in its own commit; full tests after each step; the E2E suite in T-013.
- Snyk or Semgrep findings that can only be fixed by changing behavior: hard stop 5.
- Room v4 for the quarantine table: a new migration on top of three shipped ones. Mitigation: T-009 freezes
  the old ones first; `MigrationTest` covers 3 to 4.
- Release signing: a lost keystore means no updates ever. Mitigation: Jason backs it up before the first APK
  leaves the laptop.

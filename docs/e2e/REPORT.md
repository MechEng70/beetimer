# End-to-end results

Emulator: beetimer-api36 (Android 16, API 36), debug build of the T-013 tree, 2026-11-23. Time scale 10x for every scenario (analyze.py fails any run that wasn't). Raw runs in build/e2e (not committed); screenshots and failed-run logs in docs/e2e/latest/. Three batches: the first full run (host GPU; it crashed during s7 at 1.3), then s2 and s3 at both scales after the autopilot fix, then s2, s7, s9, s10 and s11 at 1.3 after the scale-tap fix (software GPU). Flow changes between batches: FIXES.md, R11 T-013. s1x10 and s2x10 are now the same as s1 and s2 and were not run. Differences from alpha-baseline: only Jason's approved vaporizer bar change (s4 drives Skip to back and Cancel run from the hive's tab); no other behavior differed.

## Font scale 1.15

| Scenario | Result | Invariant errors | Maestro | Time | Problems |
|---|---|---|---|---|---|
| s1 Pan wand, wand stays in, 3 hives | PASS | 0 | ok | 349 s | - |
| s2 Pan wand, wand comes out, 3 hives | PASS | 0 | ok | 337 s | - |
| s3 Gun type, 3 hives | PASS | 0 | ok | 259 s | - |
| s4 Queue: skip to back, remove, fourth hive | PASS | 0 | ok | 91 s | - |
| s5 Cancel the vaporizer holder mid-block | PASS | 0 | ok | 48 s | - |
| s6 Alarms: +1 min, Snooze, two close together | PASS | 0 | ok | 294 s | - |
| s7 Screen off through a timed step | PASS | 0 | ok | 138 s | - |
| s8 Process death mid-run | PASS | 0 | ok | 145 s | - |
| s9 Editing while runs are active | PASS | 0 | ok | 83 s | - |
| s10 Hive management: cap, drag order, New session | PASS | 0 | ok | 92 s | - |
| s11 App open: alarm screen in app, Home, lock, snackbar | PASS | 0 | ok | 123 s | - |

## Font scale 1.3

| Scenario | Result | Invariant errors | Maestro | Time | Problems |
|---|---|---|---|---|---|
| s1 Pan wand, wand stays in, 3 hives | PASS | 0 | ok | 346 s | - |
| s2 Pan wand, wand comes out, 3 hives | PASS | 0 | ok | 334 s | - |
| s3 Gun type, 3 hives | PASS | 0 | ok | 259 s | - |
| s4 Queue: skip to back, remove, fourth hive | PASS | 0 | ok | 97 s | - |
| s5 Cancel the vaporizer holder mid-block | PASS | 0 | ok | 47 s | - |
| s6 Alarms: +1 min, Snooze, two close together | PASS | 0 | ok | 295 s | - |
| s7 Screen off through a timed step | PASS | 0 | ok | 140 s | - |
| s8 Process death mid-run | PASS | 0 | ok | 148 s | - |
| s9 Editing while runs are active | PASS | 0 | ok | 89 s | - |
| s10 Hive management: cap, drag order, New session | PASS | 0 | ok | 96 s | - |
| s11 App open: alarm screen in app, Home, lock, snackbar | PASS | 0 | ok | 124 s | - |

Invariant errors across all runs: **0**.

## Planner predicted vs actual: s1 (Pan wand, wand stays in, 3 hives, 10x, font 1.15)

Forecast taken right after the third Start; real seconds from the first Start.

| Hive | Gets vaporizer: predicted | actual | Finishes: predicted | actual |
|---|---|---|---|---|
| Hive 1 | 6.0 s | 6.5 s | 111.0 s | 114.9 s |
| Hive 2 | 111.0 s | 117.5 s | 216.0 s | 218.5 s |
| Hive 3 | 216.0 s | 220.4 s | 321.0 s | 321.4 s |

## Planner predicted vs actual: s2 (Pan wand, wand comes out, 3 hives, 10x, font 1.15)

Forecast taken right after the third Start; real seconds from the first Start.

| Hive | Gets vaporizer: predicted | actual | Finishes: predicted | actual |
|---|---|---|---|---|
| Hive 1 | 5.9 s | 7.2 s | 110.9 s | 156.7 s |
| Hive 2 | 50.9 s | 82.5 s | 155.9 s | 244.1 s |
| Hive 3 | 95.9 s | 172.4 s | 200.9 s | 298.0 s |

## Planner predicted vs actual: s1 (Pan wand, wand stays in, 3 hives, 10x, font 1.3)

Forecast taken right after the third Start; real seconds from the first Start.

| Hive | Gets vaporizer: predicted | actual | Finishes: predicted | actual |
|---|---|---|---|---|
| Hive 1 | 6.0 s | 5.6 s | 111.0 s | 112.7 s |
| Hive 2 | 111.0 s | 115.4 s | 216.0 s | 216.5 s |
| Hive 3 | 216.0 s | 218.5 s | 321.0 s | 319.5 s |

## Planner predicted vs actual: s2 (Pan wand, wand comes out, 3 hives, 10x, font 1.3)

Forecast taken right after the third Start; real seconds from the first Start.

| Hive | Gets vaporizer: predicted | actual | Finishes: predicted | actual |
|---|---|---|---|---|
| Hive 1 | 5.9 s | 7.3 s | 110.9 s | 156.5 s |
| Hive 2 | 50.9 s | 81.9 s | 155.9 s | 240.9 s |
| Hive 3 | 95.9 s | 171.8 s | 200.9 s | 294.0 s |


## Bugs found and fixes

### App bugs

**1. Closing an in-app alarm screen could land on the home screen (found 2026-11-21, fixed).** This came in with the foreground change. The alarm screen runs in its own task (the usual pattern for lock-screen alarms). When the app opened it while on screen and you tapped Done, Android returned to whichever task came before it, sometimes the launcher. The scenario 2 autopilot hung on the home screen; scenario 1 happened to pass. Fix: an alarm screen opened by the app goes back to the main screen when it closes (`AlarmActivity.close`, a `returnToApp` flag that a later notification tap never clears). Lock-screen and notification opens just finish, as before. The close check also moved from a Compose effect to the activity's lifecycle scope. Regression test: `AlarmActivityCloseTest`, which fails with the fix removed.

No other app or scheduler defects were found: every other failure traced to the test flows or the analyzer, with the app log showing correct behavior. The invariant check logged **zero** errors in every run.

**Usability finding, now fixed (approved 2026-11-21):** while the app was open, the "Vaporizer free" heads-up covered the top of the screen and the vaporizer bar's buttons (scenario 4 hit it twice). In the foreground, "Vaporizer free" is now a snackbar at the bottom, and a step's end opens the alarm screen directly on a quiet notification channel. Scenario 11 checks both, plus Home and lock while ringing.

**Scenario 2 at 60x: one check fails, left as FAIL (not loosened).** "Hive 1's sealed hold overlaps Hive 2's heat" can't hold at 60x under the autopilot. Hive 1's hold is 10 s, but Hive 2 loaded at 29 s and heated at 50 s, because each autopilot pass takes a few seconds and short steps ring at the 5 s alarm floor. At 10x (scenario 2 at 10x, PASS) Hive 2 heats at 36 s inside Hive 1's 60 s hold. The scheduler rules and the other s2 checks pass at both scales.

**Test-environment limits (not bugs):**
- Android won't fire an app's exact alarm less than about 5 s ahead. At 60x, Heat (2.5 s) and Cool (2 s) rang at about 5 s, so 60x actual times run long. The shortest real step is 20 s, so this never applies in the field.
- At 60x, Maestro taps take 2 to 3 s against the planner's 1 s manual-step estimate, which also stretches 60x actuals. At 10x the estimate is 6 s and predictions land within about 4%.

### Flow and analyzer fixes (all made before the final runs)

| # | Where | What changed | Assertions loosened or removed? |
|---|---|---|---|
| 1 | Flows s2, s6, s8 (`pan_comes_out_block`) | Waited for "Cool in hive finished", which never shows in the wand-comes-out preset: Cool is followed by a manual step, so the run goes straight to Remove wand. Now waits for "Remove wand". | No. The wait now names the state the app is actually in; nothing stopped being checked. |
| 2 | Flow s3 | Moved the on-screen overlap check from "after Hive 2's whole block" to "while Hive 2 vaporizes", then ran s3 at 10x. At 60x the 10 s hold is shorter than the next hive's block (5 s alarm floor), so two holds can't overlap. | No. The on-screen check is still made. The analyzer's strict check (Hive 2's hold starts before Hive 1's ends) is unchanged and passes at 10x. |
| 3 | Flow s4 | Waits for the "Vaporizer free" heads-up to go before tapping the bar's Skip / Remove and the Settings gear (taps were landing on the heads-up). Failing step before the fix: `Scrolling DOWN until id: add-hive is visible... FAILED` after a "Settings" tap; log showed correct queue states `[Hive 2, Hive 3]` → `[Hive 3, Hive 2]` → Hive 3 `NOTSTARTED` → `[Hive 2]`. Verdict: flow, not app. | No. Only waits were added. |
| 4 | Flow s10 + `lib.sh` | Hive rename now done from the shell (cursor to end, delete, type). Maestro's tap left the cursor mid-text and produced "Emorye 1". | No. The "Next: Emory" assertion still runs, now in `s10a2`. |
| 5 | App test hook (Settings) | The New session dialog's confirm button exposes its test tag (`testTagsAsResourceId` on that button; dialogs are a separate window). No visible change. | No. |
| 6 | Flows, all | Waits scale with the time scale: at least 30 s, or the 600 s hold ÷ scale + 30 s (90 s at 10x). s1x10 timed out waiting for its 60 s hold. | These are waits, not timing checks. The timing checks (s6 +1 min ≥ 10.5 s, s7 alarm under 2 s late) are unchanged. |
| 7 | Analyzer s5 | Event filter changed from strictly after the Cancel to at-or-after it: the release and the cancel are logged at the same millisecond by design. | No. It's stricter in effect: the release is now checked rather than missed. |
| 8 | Runner | Streams the app's log lines live during a scenario. In s7 the shared log buffer rotated during screen-off Doze and lost earlier lines before the dump. | No. |
| 9 | s8 kill | Presses Home, waits 5 s for the app to go to the background, then `am kill`; falls back to `kill -9` of the PID only if the process survives. Never force-stop. The first attempt ran `am kill` 1 s after Home and the process survived. **In the final run, `am kill` killed it** (pid 15873 gone; the alarm restarted the app as 16049). | No. |
| 10 | Analyzer | Skips unfinished scenarios, handles a missing value, parses forecast lines with spaces in hive names, reads the time scale in effect at forecast time. | No. |
| 11 | Flows s1, s2, s3, s6, s7, s8 | The alarm screen now opens in the app when a step ends, so flows acknowledge alarms on the alarm screen (`ack`), not on the hive rows. s2, s2x10 and s3 use an autopilot that taps whatever comes up (alarm Done, row Start or Done) until the last hive is Done. Their on-screen mid-run overlap screenshot was dropped, because overlapping alarms make its timing nondeterministic. | The analyzer checks for these scenarios are unchanged and still enforce the rules (handoff order, overlapping holds, all hives finish). The one mid-run on-screen assertion in s2 and s3 was removed; the same fact is checked from the log. |
| 12 | Flow s6 | Rebuilt so the two alarms really ring together: Hive 2's Unseal alarm is left ringing while Hive 3 finishes, then both are cleared on the alarm screen. New debug log line `ALARMSCREEN` records which hive the alarm screen shows. | Stricter: a new check requires the alarm screen to show Hive 3 then Hive 2 in turn. |
| 13 | s11 notification tap | The first attempt dumped the UI while the shade was still opening and tapped a stale position. It now waits for the shade, taps the notification body, and retries up to 3 times. | No. The check (alarm screen in front after the tap) is unchanged. |
| 14 | s11 checks | Snackbar position now uses the lowest matching text (the vaporizer bar repeats the same sentence at the top). The main-screen pixel check samples the middle of the top bar (the first version hit the dark gear icon). Added a check that no "Vaporizer free" notification was posted. | The pixel sample is narrower, offset by the new no-notification check. |
| 15 | Autopilot loop | Ran out of passes: capped at 600, and with nothing to do a pass takes about 0.15 s, so it stopped after about 90 s, before the last hive finished. Now pauses 0.5 s per pass, with a 3000-pass safety cap. | No. Same exit condition; only the cap changed. |
| 16 | s1x10 (first rerun) | Maestro's connection to the emulator died at startup (`DeviceServerDiedException`), before any step ran. Rerun unchanged. | No. |
| 17 | Runner | Each scenario has a 25-minute timeout, so a stuck flow fails instead of hanging (the scenario 2 autopilot hung for an hour on the home screen before bug 1 was found). | No. |
| 18 | Autopilot | Tapped a finished hive's "Done" status chip, which falls through to the row and opens that hive's tab, where the loop could see no rows (s2 timed out at 25 min). Now skips finished hives and taps the All tab at the start of each pass if visible. | No. Same exit condition. |

## R11 T-012: harness rules (2026-11-23)

No check or assertion changed. What changed is where output goes and how runs are set up: raw logs in
`build/e2e/` (local), cropped screenshots and failed-run logs (redacted) in `docs/e2e/latest/`, logcat cleared
before and after each run, every scenario at font scale 1.15 and 1.3, emulator settings restored on exit.
`analyze.py` reads `build/e2e/fs<scale>/` and writes one table per font scale.

First try (s5 only, 60x): passed at 1.15; at 1.3 the flow failed to tap the All tab because Hive 1's 2.5 s
heat (rings at about 5 s, the exact-alarm floor) ended while the flow was still on its way there, so the alarm
screen was up. A timing race at 60x, not an app fault; the full suite runs at 10x in T-013.

## R11: vaporizer bar without buttons (Jason, 2026-11-23)

The bar lost its button row (layout rule). Scenario 4 now skips the head to the back from that hive's tab
(`hive-skip-to-back`) and removes it with Cancel run (`hive-cancel`), which Jason confirmed covers Remove. Same
assertions as before (who is next, Hive 3 back to Not started), plus the bar's new line 2, "Next: Hive 2. Tap
Start on its row." Nothing was loosened.

## R11 T-013: the whole suite at 10x

`gen_flows.py` sets every scenario to 10x (`SUITE_SCALE`); waits scale with it as before. The s7 and s8 shell
parts wait 600 s / scale + 3 s instead of a fixed 13 s. `analyze.py` names drop the "(60x)" and "(10x)" tags.
s1x10 and s2x10 are now the same flows as s1 and s2, so they aren't run (the files stay until Jason decides).
No assertion changed.

## R11 T-013: alarm opening during a row tap; emulator crash

- s2 failed at both font scales at about 79 s: Hive 1's heat (15 s at 10x) rang between the autopilot's "row
  button visible" check and its tap, so the tap found the alarm screen instead. The event log was correct (0
  invariant errors). The autopilot's row tap is now optional; the next pass taps Done on the alarm. The loop
  still runs until the last hive is Done, and analyze.py checks every rule as before. No assertion changed.
- The emulator segfaulted twice in long runs (`qemu-system-x86_64` SIGSEGV in gfxstream's host GL path,
  `ColorBufferGl` and `TextureResize`, after "null ctx" GL errors). `emulator.sh` now boots with
  `-gpu swiftshader_indirect` (override with `EMULATOR_GPU`).

## R11 T-013: lost time scale tap at font 1.3; 60x numbers in the analyzer

- At font scale 1.3, s2, s7, s9, s10 and s11 ran at 1x. The scroll to the time scale buttons stopped with them
  under the gesture bar, and Maestro's tap there was lost (it reported COMPLETED). Probe on the emulator: 6 of
  10 taps took; with `centerElement: true` on that scroll, 10 of 10. `setup()` now centers it. s9 and s10
  "passed" at 1x; those runs don't count.
- New check (stricter): `analyze.py` fails any scenario whose first Start didn't run at 10x.
- s7's lateness check subtracted 10 s, Keep sealed at 60x, so at 10x an on-time alarm read "50 s late". It now
  subtracts 600 s / scale. Same limit (under 2 s late).
- s6's "+1 min extended the hold" threshold was 10.5 s, right for 60x but a no-op at 10x (the hold alone is
  60 s). It is now (600 + 30) s / scale: the hold plus half the minute, as before.



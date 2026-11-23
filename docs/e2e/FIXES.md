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


## Bugs found and fixes

### App bugs

None. No scenario exposed a defect in the app or the scheduler: every flow failure traced to the test flows or the analyzer, with the app log showing correct behavior. The invariant check logged **zero** errors in every run.

**Usability finding (not changed, needs your decision):** when the vaporizer frees while the app is open, the "Vaporizer free: <hive> is next" heads-up covers the top of the screen for several seconds, including the vaporizer bar's Start / Skip to back / Remove buttons and the Settings gear. Scenario 4 hit this twice. Not posting that notification while the app is in the foreground would fix it, but that changes notification behavior, so it's your call.

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

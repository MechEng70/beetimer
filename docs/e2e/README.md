# End-to-end tests

Real app, real AlarmManager alarms, on an emulator (never a phone), driven by Maestro.

Run:

```
python3 scripts/e2e/gen_flows.py          # regenerate flows after changing the generator
scripts/e2e/run.sh s1 s2 s3 s4 s5 s6 s7 s8 s9 s10 s1x10
python3 scripts/e2e/analyze.py            # writes docs/e2e/REPORT.md
```

Needs: a booted emulator (`emulator-5554`, AVD `beetimer-api36`, Android 16), the debug APK installed, and Maestro. `run.sh` refuses any serial that is not an emulator.

Each scenario clears app data, grants notification and full-screen permissions, sets the debug time scale in Settings (60x; s1x10 uses 10x), and runs its flow. Scenarios 7, 8 and 10 have a shell part (`flows/s7.sh` and so on) for screen off, process kill and the long-press drag. Output per scenario in `docs/e2e/<s>/`: `log.txt` (the app's `BeeTimerE2E` event log and any `BeeTimerInvariant` errors), Maestro output, and screenshots.

`analyze.py` fails a scenario on any invariant error, a failed flow, a crash, the vaporizer moving between hives without being freed, or a failed scenario check. It also reports the planner's predicted vs actual times for scenarios 1 and 2.

#!/usr/bin/env bash
# Scenario 7: screen off through Keep sealed (10 s at 60x); the full-screen alarm must appear on time.
set -u
m() { maestro --device "$SERIAL" test "scripts/e2e/flows/$1.yaml" --test-output-dir "$OUT/maestro"; }
m s7a || exit 1
adb -s "$SERIAL" shell input keyevent 26
echo "screen off at $(date +%s%3N)"
adb -s "$SERIAL" shell dumpsys power | grep -m1 mWakefulness=
sleep 13
adb -s "$SERIAL" shell dumpsys power | grep -m1 mWakefulness=
adb -s "$SERIAL" shell dumpsys window | grep -m1 mCurrentFocus
adb -s "$SERIAL" exec-out screencap -p > "$OUT/s7-00-after-screen-off.png"
m s7b

#!/usr/bin/env bash
# Scenario 7: screen off through Keep sealed (60 s at 10x); the full-screen alarm must appear on time.
set -u
m() { maestro --device "$SERIAL" test "scripts/e2e/flows/$1.yaml" --test-output-dir "$OUT/maestro"; }
m s7a || exit 1
adb -s "$SERIAL" shell input keyevent 26
echo "screen off at $(date +%s%3N)"
adb -s "$SERIAL" shell dumpsys power | grep -m1 mWakefulness=
# Wait out Keep sealed (600 s / time scale) plus 3 s.
sleep $((600 / ${E2E_SCALE:-10} + 3))
adb -s "$SERIAL" shell dumpsys power | grep -m1 mWakefulness=
adb -s "$SERIAL" shell dumpsys window | grep -m1 mCurrentFocus
adb -s "$SERIAL" exec-out screencap -p > "$OUT/s7-00-after-screen-off.png"
m s7b

#!/usr/bin/env bash
# Scenario 8: kill the process mid-run (Keep sealed running), let the alarm fire, reopen.
set -u
m() { maestro --device "$SERIAL" test "scripts/e2e/flows/$1.yaml" --test-output-dir "$OUT/maestro"; }
m s8a || exit 1
adb -s "$SERIAL" shell input keyevent KEYCODE_HOME
pid=$(adb -s "$SERIAL" shell pidof "$APP")
sleep 5   # let the process become cached so am kill is allowed
adb -s "$SERIAL" shell am kill "$APP"
sleep 1
if [ -n "$(adb -s "$SERIAL" shell pidof "$APP")" ]; then
  # Same effect as a low-memory kill (alarms stay registered), unlike force-stop.
  echo "am kill left the process running; killing pid $pid with run-as"
  adb -s "$SERIAL" shell run-as "$APP" kill -9 "$pid"
  sleep 1
else
  echo "killed by am kill"
fi
echo "pid before: '$pid'"
echo "pid after kill: '$(adb -s "$SERIAL" shell pidof "$APP")'"
echo "killed at $(date +%s%3N)"
sleep 13
echo "pid after alarm: '$(adb -s "$SERIAL" shell pidof "$APP")'"
m s8b

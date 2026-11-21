#!/usr/bin/env bash
# End-to-end scenarios on the emulator (never a phone). Usage: scripts/e2e/run.sh s1 s2 ...
# Each scenario: clear app data, grant permissions, run the Maestro flow (or flows/<s>.sh
# when a scenario needs adb between flow parts), then save the Maestro output, screenshots
# and the app's debug event log (BeeTimerE2E / BeeTimerInvariant) to docs/e2e/<s>/.
set -uo pipefail
export SERIAL="${SERIAL:-emulator-5554}"
export APP=io.github.mecheng70.beetimer
cd "$(dirname "$0")/../.."
case "$SERIAL" in emulator-*) ;; *) echo "refusing to run on $SERIAL (emulator only)"; exit 2 ;; esac
for s in "$@"; do
  export OUT="docs/e2e/$s"
  rm -rf "$OUT"; mkdir -p "$OUT"
  adb -s "$SERIAL" shell pm clear "$APP" >/dev/null
  adb -s "$SERIAL" shell pm grant "$APP" android.permission.POST_NOTIFICATIONS
  adb -s "$SERIAL" shell appops set "$APP" USE_FULL_SCREEN_INTENT allow
  adb -s "$SERIAL" shell input keyevent KEYCODE_WAKEUP
  adb -s "$SERIAL" shell wm dismiss-keyguard 2>/dev/null
  adb -s "$SERIAL" logcat -c
  # Stream the app's lines live: a full buffer (screen off, Doze) can rotate them out before a dump.
  adb -s "$SERIAL" logcat -v epoch -s BeeTimerE2E:I BeeTimerInvariant:E AndroidRuntime:E > "$OUT/log.txt" &
  logpid=$!
  start=$(date +%s)
  # A stuck flow fails after 25 minutes instead of hanging.
  if [ -x "scripts/e2e/flows/$s.sh" ]; then
    timeout 1500 "scripts/e2e/flows/$s.sh" > "$OUT/maestro.txt" 2>&1
  else
    timeout 1500 maestro --device "$SERIAL" test "scripts/e2e/flows/$s.yaml" --test-output-dir "$OUT/maestro" > "$OUT/maestro.txt" 2>&1
  fi
  echo $? > "$OUT/maestro-exit"
  echo $(( $(date +%s) - start )) > "$OUT/seconds"
  sleep 1; kill "$logpid" 2>/dev/null; wait "$logpid" 2>/dev/null
  find "$OUT/maestro" -name '*.png' -path '*takeScreenshot*' -exec cp {} "$OUT/" \; 2>/dev/null
  echo "$s: exit $(cat "$OUT/maestro-exit"), $(grep -c 'BeeTimerInvariant' "$OUT/log.txt") invariant errors, $(cat "$OUT/seconds") s"
done

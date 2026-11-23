#!/usr/bin/env bash
# End-to-end scenarios on the emulator (never a phone). Usage: scripts/e2e/run.sh s1 s2 ...
# FONT_SCALES (default "1.15 1.3"): every scenario runs once per font scale.
#
# Per run (playbook device rules, R11 T-012):
# - logcat is cleared before and after the run;
# - raw output (Maestro, the app's BeeTimerE2E / BeeTimerInvariant event log) goes to build/e2e/fs<scale>/<s>/,
#   which is git-ignored; analyze.py reads it there;
# - docs/e2e/latest/fs<scale>/<s>/ gets the screenshots, status bar cropped, and the Maestro transcript; the
#   event log is copied there only if the run failed, with token patterns redacted;
# - every emulator setting the run changes (font scale, screen timeout, stay awake) is put back on exit.
set -uo pipefail
export SERIAL="${SERIAL:-emulator-5554}"
export APP=io.github.mecheng70.beetimer
cd "$(dirname "$0")/../.."
case "$SERIAL" in emulator-*) ;; *) echo "refusing to run on $SERIAL (emulator only)"; exit 2 ;; esac
source scripts/e2e/lib.sh

save_settings
trap restore_settings EXIT

for scale in ${FONT_SCALES:-1.15 1.3}; do
  adb -s "$SERIAL" shell settings put system font_scale "$scale"
  for s in "$@"; do
    export OUT="build/e2e/fs$scale/$s"
    pub="docs/e2e/latest/fs$scale/$s"
    rm -rf "$OUT" "$pub"; mkdir -p "$OUT" "$pub"
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
    adb -s "$SERIAL" logcat -c
    # Key screenshots, status bar cropped (raw ones stay in build/).
    sb=$(statusbar_height)
    find "$OUT" -name '*.png' \( -path '*takeScreenshot*' -o -path "$OUT/*.png" \) ! -name 'top-check.png' -print0 |
      while IFS= read -r -d '' f; do crop_top "$f" "$sb" "$pub/$(basename "$f")"; done
    cp "$OUT/maestro.txt" "$pub/maestro.txt"
    inv=$(grep -c 'BeeTimerInvariant' "$OUT/log.txt")
    if [ "$(cat "$OUT/maestro-exit")" != 0 ] || [ "$inv" != 0 ] || grep -q 'CHECK .* FAIL' "$OUT/maestro.txt" ||
       grep -q 'FATAL EXCEPTION' "$OUT/log.txt"; then
      python3 -I scripts/e2e/redact.py < "$OUT/log.txt" > "$pub/log-failed.txt"
      echo "$s (font $scale): FAILED, redacted log in $pub/log-failed.txt"
    fi
    echo "$s (font $scale): exit $(cat "$OUT/maestro-exit"), $inv invariant errors, $(cat "$OUT/seconds") s"
  done
done

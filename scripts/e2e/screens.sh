#!/usr/bin/env bash
# Re-captures docs/screenshots/app-*.png on the emulator (never a phone), status bar cropped.
set -euo pipefail
SERIAL="${SERIAL:-emulator-5554}"; APP=io.github.mecheng70.beetimer
cd "$(dirname "$0")/../.."
case "$SERIAL" in emulator-*) ;; *) echo "emulator only"; exit 2 ;; esac
TMP="$(mktemp -d)"
m() { maestro --device "$SERIAL" test "scripts/e2e/flows/$1.yaml" --test-output-dir "$TMP/m" >> "$TMP/maestro.txt" 2>&1; }
cap() {
  sb=$(adb -s "$SERIAL" shell dumpsys window | grep -oE "statusBars.*frame=\[0,0\]\[[0-9]+,[0-9]+\]" | grep -oE "[0-9]+\]$" | tr -d ']' | head -1)
  adb -s "$SERIAL" exec-out screencap -p > "$TMP/raw.png"
  python3 -I -c "from PIL import Image; im=Image.open('$TMP/raw.png'); im.crop((0,${sb:-80},im.width,im.height)).save('docs/screenshots/$1.png')"
  echo "captured $1"
}
adb -s "$SERIAL" shell pm clear "$APP" >/dev/null
adb -s "$SERIAL" shell pm grant "$APP" android.permission.POST_NOTIFICATIONS
adb -s "$SERIAL" shell appops set "$APP" USE_FULL_SCREEN_INTENT allow
adb -s "$SERIAL" shell settings put system font_scale 1.15
source scripts/e2e/lib.sh
started=$(date +%s)
m scr-a0
for pair in "Hive 1:Emory" "Hive 2:Sparky" "Hive 3:Herb"; do rename_hive "${pair%%:*}" "${pair##*:}"; done
m scr-a
m scr-b; cap app-2-hive
m scr-c; cap app-4-template
m scr-d; cap app-7-sealed-hold-option
m scr-e; cap app-5-settings-hives
m scr-f
# In the app, a step end opens the alarm screen directly; the heads-up shows when the app is
# in the background. Go Home, let Herb's 150 s heat end, capture the heads-up, then open it.
adb -s "$SERIAL" shell input keyevent KEYCODE_HOME
adb -s "$SERIAL" shell 'i=0; while [ $i -lt 240 ] && ! dumpsys activity services '"$APP"' | grep -q RingService; do sleep 1; i=$((i+1)); done'
sleep 2; cap app-6-headsup
adb -s "$SERIAL" shell input tap 540 200
m scr-g; sleep 2; cap app-3-alarm
# All tab: fresh data, wand comes out. Herb in Cool in hive ("Next: Remove wand"), then Herb in Keep sealed
# (teal pill) with Sparky on Load dose ("Next: Heat and vaporize"). Each at font scale 1.15 and 1.3.
adb -s "$SERIAL" shell pm clear "$APP" >/dev/null
adb -s "$SERIAL" shell pm grant "$APP" android.permission.POST_NOTIFICATIONS
adb -s "$SERIAL" shell appops set "$APP" USE_FULL_SCREEN_INTENT allow
m scr-h0
for pair in "Hive 1:Emory" "Hive 2:Sparky" "Hive 3:Herb"; do rename_hive "${pair%%:*}" "${pair##*:}"; done
m scr-h; cap app-1-all
adb -s "$SERIAL" shell settings put system font_scale 1.3; sleep 2; cap app-1-all-large
adb -s "$SERIAL" shell settings put system font_scale 1.15; sleep 2
m scr-h2; cap app-1-all-teal
adb -s "$SERIAL" shell settings put system font_scale 1.3; sleep 2; cap app-1-all-teal-large
adb -s "$SERIAL" shell settings put system font_scale 1.15
echo "done in $(( $(date +%s) - started )) s"

# Shared shell helpers for E2E scripts (emulator only). Needs SERIAL.
_bounds() {
  adb -s "$SERIAL" shell uiautomator dump /sdcard/ui.xml >/dev/null
  adb -s "$SERIAL" shell cat /sdcard/ui.xml | tr '>' '\n' | grep -E "$1" | head -1 |
    sed -E 's/.*bounds="\[([0-9]+),([0-9]+)\]\[([0-9]+),([0-9]+)\]".*/\1 \2 \3 \4/'
}
center_of() { _bounds "$1" | awk '{ printf "%d %d", ($1+$3)/2, ($2+$4)/2 }'; }
tap_match() { read -r x y <<< "$(center_of "$1")"; adb -s "$SERIAL" shell input tap "$x" "$y"; sleep 0.8; }
# Rename a hive from Settings (its row visible). Puts the cursor at the end before deleting.
rename_hive() {
  tap_match "resource-id=\"hive-row-$1\""
  tap_match 'class="android.widget.EditText"'
  adb -s "$SERIAL" shell input keyevent KEYCODE_MOVE_END
  for _ in $(seq 1 20); do adb -s "$SERIAL" shell input keyevent KEYCODE_DEL; done
  adb -s "$SERIAL" shell input text "$2"
  tap_match 'text="Save"'
}

# CHECK lines are read by analyze.py; any FAIL fails the scenario.
check() { if [ "$2" = 0 ]; then echo "CHECK $1 PASS ${3:-}"; else echo "CHECK $1 FAIL ${3:-}"; fi; }

app_uid() { adb -s "$SERIAL" shell cmd package list packages -U "$APP" | grep -oE 'uid:[0-9]+' | cut -d: -f2 | head -1; }

# Ringing = the ring service is running and the app's audio player is started.
is_ringing() {
  local uid; uid=$(app_uid)
  adb -s "$SERIAL" shell dumpsys activity services "$APP" | grep -q RingService || return 1
  adb -s "$SERIAL" shell dumpsys audio | grep -E "u/pid:$uid/[0-9]+ state:started" | grep -q .
}

focus_is() { adb -s "$SERIAL" shell dumpsys window | grep -m1 mCurrentFocus | grep -q "$1"; }

# Nothing covers the top: sample the band under the status bar at both edges.
# mode alarm: every sample is the alarm's dark amber. mode main: every sample is light (page,
# card or vaporizer bar), never a heads-up's grey.
top_clear() {
  adb -s "$SERIAL" exec-out screencap -p > "$OUT/top-check.png"
  python3 -I - "$OUT/top-check.png" "$1" <<'PY'
import sys
from PIL import Image
im = Image.open(sys.argv[1]).convert("RGB"); mode = sys.argv[2]
w, h = im.size
if mode == "alarm":
    # Alarm screen edges: dark amber only.
    pts = [(x, y) for x in (int(w * 0.06), int(w * 0.94)) for y in range(int(h * 0.06), int(h * 0.2), int(h * 0.02))]
else:
    # Middle of the main screen's top bar, clear of the title and the gear icon.
    pts = [(w // 2, y) for y in range(int(h * 0.055), int(h * 0.09), int(h * 0.005))]
bad = []
for p in pts:
    r, g, b = im.getpixel(p)
    ok = (r < 100 and g < 60 and b < 40) if mode == "alarm" else (min(r, g, b) > 200)
    if not ok:
        bad.append((p, (r, g, b)))
print("ok" if not bad else f"covered at {bad[:3]}")
sys.exit(0 if not bad else 1)
PY
}

# The snackbar's top edge is in the bottom third of the screen.
snackbar_low() {
  adb -s "$SERIAL" shell uiautomator dump /sdcard/ui.xml >/dev/null
  # The vaporizer bar shows the same sentence at the top; the snackbar is the lowest match.
  local line; line=$(adb -s "$SERIAL" shell cat /sdcard/ui.xml | tr '>' '\n' | grep 'text="Vaporizer free: Hive 2 is next"' | tail -1)
  local top; top=$(echo "$line" | sed -E 's/.*bounds="\[[0-9]+,([0-9]+)\].*/\1/')
  local h; h=$(adb -s "$SERIAL" shell wm size | grep -oE '[0-9]+$')
  [ -n "$top" ] && [ "$top" -gt $(( h * 2 / 3 )) ]
}

no_device_free_notification() {
  [ "$(adb -s "$SERIAL" shell dumpsys notification --noredact | grep -c 'channel=device_v1')" = 0 ]
}

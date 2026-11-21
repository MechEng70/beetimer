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

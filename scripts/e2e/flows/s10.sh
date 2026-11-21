#!/usr/bin/env bash
# Scenario 10: 5-hive cap, rename, drag Hive 3 to the top (long-press its handle), New session.
set -u
source scripts/e2e/lib.sh
m() { maestro --device "$SERIAL" test "scripts/e2e/flows/$1.yaml" --test-output-dir "$OUT/maestro"; }
m s10a || exit 1
adb -s "$SERIAL" shell input keyevent KEYCODE_BACK   # close the dialog the flow opened
sleep 0.5
rename_hive "Hive 1" "Emory"
m s10a2 || exit 1
read -r x y <<< "$(center_of 'resource-id="drag-Hive 3"')"
read -r tx ty <<< "$(center_of 'resource-id="drag-Emory"')"
echo "drag Hive 3 from $x,$y to $tx,$ty"
adb -s "$SERIAL" shell input motionevent DOWN "$x" "$y"
sleep 1.2
steps=12
for i in $(seq 1 $steps); do
  adb -s "$SERIAL" shell input motionevent MOVE "$x" $(( y + (ty - 40 - y) * i / steps ))
done
adb -s "$SERIAL" shell input motionevent UP "$x" $(( ty - 40 ))
sleep 1
m s10b

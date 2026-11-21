#!/usr/bin/env bash
# Scenario 11: app open and unlocked when steps end (wand comes out, 60x).
set -u
source scripts/e2e/lib.sh
m() { maestro --device "$SERIAL" test "scripts/e2e/flows/$1.yaml" --test-output-dir "$OUT/maestro"; }
m s11a || exit 1
# Heat ends with the app open: the alarm screen opens in the app; nothing covers the top.
m s11b || exit 1
focus_is AlarmActivity; check alarm_screen_in_front $?
r=$(top_clear alarm); check top_not_covered_by_alarm_headsup $? "$r"
is_ringing; check ringing_in_app $?
# Home while ringing: keeps ringing; tapping the notification returns to the alarm screen.
adb -s "$SERIAL" shell input keyevent KEYCODE_HOME; sleep 3
is_ringing; check ringing_after_home $?
# Tap the notification body once the shade has settled (a dump during the open animation
# returns stale positions); retry up to 3 times.
for _ in 1 2 3; do
  adb -s "$SERIAL" shell cmd statusbar expand-notifications; sleep 3
  tap_match 'text="Heat and vaporize finished. Next'; sleep 2
  focus_is AlarmActivity && break
done
focus_is AlarmActivity; check notification_returns_to_alarm_screen $?
# +1 min there works (the step reopens and rings again), then Snooze works.
m s11c || exit 1
sleep 1; is_ringing; s=$?; [ $s -ne 0 ]; check snooze_silences $?
sleep 7; is_ringing; check rings_again_after_snooze $?
# Lock while ringing: keeps ringing; waking shows the alarm screen; Done works.
adb -s "$SERIAL" shell input keyevent 26; sleep 3
is_ringing; check ringing_while_locked $?
adb -s "$SERIAL" shell input keyevent KEYCODE_WAKEUP; sleep 2
focus_is AlarmActivity; check waking_returns_to_alarm_screen $?
m s11d || exit 1
adb -s "$SERIAL" shell wm dismiss-keyguard; sleep 2
# Cool ends with the app open; Done completes Remove wand: snackbar at the bottom, no heads-up.
m s11e || exit 1
snackbar_low; check snackbar_at_bottom $?
no_device_free_notification; check no_vaporizer_free_notification_in_app $?
r=$(top_clear main); check top_not_covered_by_vaporizer_free $? "$r"

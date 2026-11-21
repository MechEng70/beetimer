# Manual phone test (about 10 minutes)

Checks what unit tests can't: real alarms, the lock screen, notifications, and a reboot. Run it after
installing a new build. Ringer on, volume up.

## Setup (2 minutes)

1. Install: `adb install -r app/build/outputs/apk/debug/app-debug.apk`, or open the APK on the phone.
2. Open Bee Timer. On "Make sure alarms get through", every row should say On (tap Allow where it doesn't).
   Tap Continue.
3. Settings, Hives: rename the hives to **Emory**, **Herb** and **Sparky** with Edit. Delete any others.
4. Settings, Steps: pick **OAV pan wand**. With Edit, set Heat and vaporize, Cool in hive and Keep sealed to
   **10** seconds each.
5. Back to the main screen.

## 1. Queue (1 minute)

1. All tab: tap Start on Emory, Herb and Sparky. Each shows "Seal hive" and a Done button.
2. Tap Done on Emory. Expect: the bar turns amber, "Device: Emory, Load dose".
3. Tap Done on Herb, then Sparky. Expect: both show "Waiting for device" and "Queued", and the bar says
   "Next: Herb, Sparky".

## 2. Alarms on the lock screen (2 minutes)

1. Tap Done on Emory (Load dose). Heat starts at 0:10. **Lock the phone** and put it down.
2. At 10 s, expect: the screen wakes to the dark amber alarm: "Emory", "Heat and vaporize finished",
   "Next: Cool in hive. Starts when you tap Done." The beep loops at ringer volume and the phone keeps
   vibrating.
3. Tap Done. Cool starts at 0:10 from your tap. Lock the phone again.
4. At 10 s, expect: "Cool in hive finished", "Next: Remove wand and reseal the entrance.", and "Device frees
   when you tap Done. Herb is next."
5. Tap Done. Expect: the alarm stops, and a "Device free: Herb is next" notification pops up.

## 3. Handoff and overlap (1 minute)

1. Unlock and open the app. Expect: the bar says "Device free: Herb is next" with Start, Skip to back and
   Remove. Emory shows Keep sealed with a teal countdown. Herb has not started by itself.
2. Tap Skip to back. Expect: "Device free: Sparky is next", and the queue now lists Herb.
3. Tap Start in the bar. Expect: Sparky takes the device (amber, Load dose) while Emory counts down Keep
   sealed in teal.

## 4. Snooze and +1 min (1 minute)

1. When Emory's Keep sealed ends, the alarm rings: "Next: Open the entrance."
2. Tap Snooze 30 s. Expect: silence, then it rings again about 30 s later.
3. Tap +1 min. Expect: the alarm stops and Emory is back on Keep sealed with 1:00 left.

## 5. Cancel and remove (1 minute)

1. Open Sparky's tab and tap Cancel run. Expect: the device frees and "Device free: Herb is next" appears.
2. In the bar, tap Remove. Expect: Herb goes back to Not started and the bar says "Device: free".

## 6. Lock screen status (30 seconds)

1. With Emory still in its 1:00 Keep sealed, lock the phone and look at the notifications. Expect: "1 hive
   active" with "Emory: Keep sealed, ends <time>" and a countdown.

## 7. App killed, then reboot (2 minutes)

1. Start Herb, tap Done (seal) and Done (load) so Heat runs. Open recent apps and swipe Bee Timer away.
   Expect: the alarm still rings at 10 s. Tap Done so Cool starts, then tap +1 min twice for about
   2:10 left.
2. Restart the phone right away, and unlock it as soon as it is back.
3. Expect: if Cool ended while the phone was off, the alarm rings soon after you unlock, marked
   "(overdue)". If it hasn't ended yet, it rings on time. Either way Herb is still on the right step.

## Pass

All the expected results above happen, sound and vibration keep going until you tap a button (or for 2
minutes), and nothing crashes. Note anything different with the step number.

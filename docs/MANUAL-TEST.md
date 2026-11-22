# Manual phone test (about 15 minutes)

Checks what unit tests and the emulator suite can't: real alarms on your phone, the lock screen,
notifications, and a reboot. Run it after installing a new build. Alarm volume up (the main screen says
"Alarm volume is low" if it isn't).

## Setup (2 minutes)

1. Build and install over the old build: `./gradlew assembleDebug`, then
   `adb install -r app/build/outputs/apk/debug/app-debug.apk`. Don't uninstall first.
2. Open Bee Timer. On "Make sure alarms get through", every row should say On (tap Allow where it doesn't).
   Tap Continue.
3. Settings, Hives: tap a name to rename it. Name three hives **Emory**, **Herb** and **Sparky**, in that
   order. Delete any others with the trash icon.
4. Settings, steps card: pick **Pan wand**, then "During the sealed hold: **Wand comes out**" (tap Replace if
   asked). Tap Heat and vaporize and set **10** seconds; do the same for Cool in hive. Leave Keep sealed at
   10:00.
5. If Developer shows a time scale, set it to 1x. Back to the main screen.

## 1. Queue (1 minute)

1. All tab: tap Start on Emory, Herb and Sparky. Each shows Seal hive and a Done button.
2. Tap Done on Emory. Expect: Emory takes the vaporizer on Load dose; the bar shows "Vaporizer: Emory".
3. Tap Done on Herb, then Sparky. Expect: both show "Waiting" and "Queued", and the bar says "Next: Herb".

## 2. Alarms on the lock screen (2 minutes)

1. Tap Done on Emory (Load dose). Heat starts at 0:10. **Lock the phone** and put it down.
2. At 10 s, expect: the screen wakes to the dark amber alarm: "Emory", "Heat and vaporize finished", Next
   "Cool in hive", "0:10 timer starts when you tap Done". It sounds at alarm volume and keeps vibrating.
3. Tap Done. Cool starts at 0:10 from your tap. Lock the phone again.
4. At 10 s, expect: "Cool in hive finished", Next "Remove wand", and "Vaporizer frees when you tap Done.
   Herb is next."
5. Tap Done. Expect: the alarm stops, and a "Vaporizer free: Herb is next" notification appears.

## 3. Handoff and overlap (1 minute)

1. Unlock and open the app. Expect: the bar says "Vaporizer free: Herb is next" with Start, Skip to back and
   Remove. Emory shows Keep sealed counting down. Herb has not started by itself.
2. Tap Skip to back. Expect: "Vaporizer free: Sparky is next".
3. Tap Start in the bar. Expect: Sparky takes the vaporizer (Load dose) while Emory counts down Keep sealed.
   Coming up lists the next 3 steps by hive and action, no times. Each row's chip shows its countdown.

## 4. Alarm with the app open, Snooze and +1 min (3 minutes)

Keep the app open on the All tab for this whole section.

1. Tap Done on Sparky (Load dose). At 10 s, expect: the alarm screen opens straight away, with no banner at
   the top first.
2. Tap Snooze 30 s. Expect: silence, then it rings again about 30 s later.
3. Tap +1 min. Expect: the alarm stops, you are back in Bee Timer (not the home screen), and Sparky is back on
   Heat and vaporize with 1:00 left.
4. When it rings, press **Home**. Expect: it keeps ringing. Tap the notification: the alarm screen comes
   back. Tap +1 min again, then open Bee Timer.
5. When it rings, tap Done. Expect: back in Bee Timer, and Cool starts. When Cool ends, the alarm screen
   opens again; tap Done. Expect: a snackbar at the bottom, "Vaporizer free: Herb is next" with Start, and
   no "Vaporizer free" notification.

## 5. Remove and cancel (1 minute)

1. In the bar, tap Remove. Expect: Herb goes back to Not started and the bar says "Vaporizer: free".
2. Start Herb, tap Done (Seal hive) and Done (Load dose) so Heat runs. Open Herb's tab and tap Cancel run.
   Expect: Herb is Not started with a Start button, the bar says "Vaporizer: free", and nothing rings 10 s
   later.

## 6. Lock screen status (30 seconds)

1. Emory and Sparky are in Keep sealed. Lock the phone and look at the notifications. Expect: "2 hives
   active" with lines like "Emory: Keep sealed, ends <time>".

## 7. App killed, then reboot (3 minutes)

1. Start Herb, tap Done (seal) and Done (load) so Heat runs. Open recent apps and swipe Bee Timer away.
   Expect: the alarm still rings at 10 s. Tap Done so Cool starts.
2. Open Herb's tab and tap +1 min twice for about 2:10 left.
3. Restart the phone right away, and unlock it as soon as it is back.
4. Expect: if Cool ended while the phone was off, the alarm rings soon after you unlock, marked
   "(overdue)". If it hasn't ended yet, it rings on time. Either way Herb is still on the right step.

Emory's and Sparky's Keep sealed alarms ring around now too ("Next: Unseal"). Tap Done on each.

## Pass

All the expected results above happen, sound and vibration keep going until you tap a button (or for 2
minutes), and nothing crashes. Note anything different with the section and step number.

# UI screenshots

Captured 2026-11-21 on a OnePlus CPH2583 (Android 16, 360 dp wide, font scale 1.15, light theme), after the second review round. The status bar is cropped from every capture. The `mockup-*.png` files are the approved mockups, rendered from `docs/mockups/varroa-timer-mockups.html` at 412 px.

| File | What it shows |
|---|---|
| `app-1-all.png` | All tab as the planning view. Herb holds the wand (heating), Sparky is queued, Emory is not started. Shows the low alarm volume banner, the wand bar, Coming up, the hive rows with next times and drag handles, and the "Started hives done" footer. |
| `app-1-all-large.png` | Same screen at font scale 1.3. Nothing clips or overlaps: long step names end in "...", and the banner wraps to four lines. |
| `app-2-hive.png` | Herb's tab: 52 sp countdown, the corrected 6-step pan wand (Keep sealed holds the device), and the 56 dp action grid. |
| `app-3-alarm.png` | Alarm screen for Herb's heat step: name, finished line, divider, Next, "Cool in hive", "2:00 timer starts when you tap Done", and 64 dp buttons at the bottom. |
| `app-4-template.png` | Step editor: preset toggle, tap a step to edit name and duration, tap the chip to switch device and passive, up and down arrows, Reset to defaults, footer. |
| `app-5-settings-hives.png` | Hives card (drag handle, tap to rename, trash), "+ Add hive", Display, Alerts and Session. Back-arrow top bar. |
| `app-6-headsup.png` | Heads-up while unlocked: "Herb: heat and vaporize finished" with "Next: cool in hive" and the Done and +1 min actions. Behind it, Coming up shows "Now" for the passed 12:35. |

## Changed after these captures (not re-captured)

- The All tab shows time remaining instead of clock times ("~14:09", "Next: Cool in hive in 2:10", "Starts in ~15:09", "Wand: Herb, free in ~15:09"). Only the footer is a clock time.
- The heads-up title is now the hive name ("Herb"), with the body "Heat and vaporize finished. Next: Cool in hive". This resolves note 1.
- The volume banner reads "Alarm volume is low" with a Raise button, on one line. This resolves note 4.

## Notes

1. **The heads-up title truncates while collapsed** ("Herb: heat and vapo..."). Android shows heads-ups collapsed, and the BigText style only applies once the notification is expanded (swiping down shows the full text). If the full title must show collapsed, a shorter title such as the hive name alone, with the finished step moved into the body, would fit. That needs your call, since it changes the copy you specified.
2. **Alarm screen colors.** The app draws #412402 and #FAEEDA, and an earlier capture measured exactly #412402. This capture reads about 3% off (#3F230A; Done #EDE2DC). The main screen samples exactly at the same moment (#F4F4F2, wand bar #FAEEDA), so this looks like OxygenOS display processing of dark content, not the app. Forced dark is still opted out.
3. **Clock format.** Times follow the phone's 12 or 24-hour setting. This phone is set to 24-hour, so the footer reads "~13:05" rather than "1:05 PM".
4. **The volume banner** is one message, but it wraps to three lines at 1.15 and four at 1.3 with the requested copy.
5. **Hive 4** isn't in these captures because you deleted it.

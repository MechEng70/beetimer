# UI screenshots

Captured 2026-11-21 on a OnePlus CPH2583 (Android 16, 360 dp wide, font scale 1.15, light theme), after the second review round. The status bar is cropped from every capture. The `mockup-*.png` files are the approved mockups, rendered from `docs/mockups/varroa-timer-mockups.html` at 412 px.

| File | What it shows |
|---|---|
| `app-1-all.png` | All tab as the planning view (current build). Herb holds the vaporizer (heating), Sparky is queued, Emory is not started. One-line volume banner with Raise; "Vaporizer: Herb, free in ~15:17"; Coming up as countdowns ("2:17", "~16:17"); rows with "Next: Cool in hive in 2:17" and "Starts in ~16:17"; footer "Started hives done ~13:19" (the phone is set to 24-hour). |
| `app-1-all-large.png` | Same screen at font scale 1.3 (current build). Nothing clips or overlaps; long text ends in "...". |
| `app-2-hive.png` | Herb's tab: 52 sp countdown, the 6-step pan wand (Keep sealed holds the device), and the 56 dp action grid. Captured before the "Wand" to "Vaporizer" rename; the bar reads "Wand: Herb, 2:02 left". |
| `app-3-alarm.png` | Alarm screen for Herb's heat step: name, finished line, divider, Next, "Cool in hive", "2:00 timer starts when you tap Done", and 64 dp buttons at the bottom. |
| `app-4-template.png` | Step editor: preset toggle, tap a step to edit name and duration, tap the chip to switch device and passive, up and down arrows, Reset to defaults, footer. |
| `app-5-settings-hives.png` | Hives card (drag handle, tap to rename, trash), "+ Add hive", Display, Alerts and Session. Back-arrow top bar. |
| `app-6-headsup.png` | Heads-up while unlocked (current build): title "Herb", body "Heat and vaporize finished. Next: Cool in hive", Done and +1 min. Behind it, Coming up shows "Now" for Cool in hive. |

## Notes

1. **Alarm screen colors** in `app-3-alarm.png` read about 3% off (OxygenOS display processing of dark content). You chose to ignore this.
2. **Clock format.** The footer follows the phone's 12/24-hour setting. This phone is set to 24-hour, so it reads "~13:19"; with 12-hour it reads "~1:19 PM".
3. **Cancel** now returns a hive to Not started with a Start button, so no "Cancelled" rows appear.

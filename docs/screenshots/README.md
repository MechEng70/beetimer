# UI screenshots: mockup vs app

Captured 2026-11-21 for review against `docs/mockups/varroa-timer-mockups.html`.

| Screen | Mockup | App (OnePlus CPH2583, Android 16, light theme) |
|---|---|---|
| 1. All hives | `mockup-1-all.png` | `app-1-all.png` |
| 2. Hive tab | `mockup-2-hive.png` | `app-2-hive.png` |
| 3. Alarm | `mockup-3-alarm.png` | `app-3-alarm.png` |
| 4. Template editor | `mockup-4-template.png` | `app-4-template.png` |

How they were made:
- **Mockups:** headless Chrome, one screen per render, 412 px wide at device scale 3.5. The label and legend were hidden and the frame corners squared off.
- **App:** `adb exec-out screencap -p`, with the top 160 px (status bar) cropped off. The phone is 1440 px wide, which is 360 dp at density 4.0, the same width as the mockup screens.

App state in the captures:
- Emory in Keep sealed (teal)
- Herb heating with the device (amber)
- Sparky queued
- Hive 4 not started

The alarm capture is a later alarm with the screen already on. See the color note below.

## Differences kept, and why

1. **Header row above the card** ("Bee Timer" and "Settings"; "Back" on Settings). The mockup has no app bar, but the app needs a way to reach Settings. It's plain text on the page background, so the card stays the focus.
2. **Hive names** are the user's own (Emory, Herb, Sparky, Hive 4), not "Hive 1" to "Hive 4". The order is the user's hive order.
3. **Queue line** reads "Next: Sparky". The mockup says "Next in queue: Hive 3", but the "Next: <list>" form was approved during planning (SPEC section 1).
4. **The status line wraps** ("left" on a second line). The phone's font size is set to 1.15, and every size is in sp so it follows that setting. At the default size it fits on one line, as in the mockup.
5. **Text is about 15% larger** than the mockup's px throughout, for the same reason as item 4. Relative sizes match the spec: 14 body, 13 labels, 12 chips, 15 hive names, 52 countdown, 48 alarm name.
6. **Bold, not semibold**, for hive names, the countdown and the alarm name and buttons. The phone's system font has no 600 weight and draws semibold as regular. The mockup's bold rendering is the target.
7. **The card wraps its content** and the page background shows below it. The mockup frame is a phone outline, not a card on a page. This matches the requested page and card colors.
8. **Hive tab action grid** has extra buttons the mockup doesn't show: Done (filled) on manual steps, and Snooze while ringing. They sit in the same 56 dp grid as +1 min and Skip so the gloved-hand targets stay together. Start on a not-started hive is full width at 44 dp, following the mockup's full-width Cancel run.
9. **Template editor** has no drag handles and no "+ Add step", because the alpha only edits timed durations. Showing controls that do nothing would mislead. It lists all 7 steps (the mockup shows 3) with Edit on timed steps only, and keeps "Reset to defaults" (approved in planning). Settings also has Hives, Display, Alerts and Session cards in the same style; none of these are in the mockups.
10. **The alarm screen centers its content** on a full-height screen. The mockup is cropped to its content. The instruction is the preset's full text, "Next: remove wand and reseal the entrance", which wraps to two lines (the mockup says "remove wand and reseal").
11. **Device-free state** (when the queue head is waiting): the status bar shows "Device free: Sparky is next" with Start, Skip to back and Remove. The mockups don't cover this state.
12. **Alarm text**: the capture shows Herb's Keep sealed alarm ("Next: open the entrance"). The mockup shows a Cool in hive alarm. Same layout either way; the device hint line only appears when Done frees the device.
13. **The alarm name and buttons look closer to regular weight than bold** on this phone, even though the style is Bold (the same setting that makes the hive names bold on the main screen). This is still open; I'm noting it rather than guessing at a fix.

## Alarm screen colors: the bug found while capturing

The first alarm captures came out purple-grey (background #3A201C instead of #412402). The main screen sampled correct at the same moment, even while the alarm rang. The cause was OxygenOS force-darkening the alarm window, whose base theme was dark. The fix is `android:forceDarkAllowed=false` on the app theme, with the alarm window now using the same light base and its own dark amber background. `app-3-alarm.png` is from after the fix and samples #412402 exactly.

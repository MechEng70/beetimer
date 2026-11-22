# UI screenshots

Captured 2026-11-21 on the emulator (`beetimer-api36`, Android 16, Pixel 6 size, 411 dp wide, font scale 1.15, light theme) from the current debug build by `scripts/e2e/screens.sh`. The four `app-1-all*.png` files were re-captured 2026-11-22 for R10. The status bar is cropped. The `mockup-*.png` files are the approved mockups rendered from `docs/mockups/varroa-timer-mockups.html`.

Scenes (real 1x timing). `app-1-all*`: a separate run on fresh data with "wand comes out", so Herb holds the vaporizer through Cool in hive. `app-2` to `app-7`: wand stays in; Herb holds the vaporizer (Heat and vaporize), Sparky is queued, Emory is not started.

| File | What it shows |
|---|---|
| `app-1-all.png` | All tab (R10): no volume banner. "Vaporizer: Herb, free in ~2:55"; Coming up, the next 3 events, hive and action only; hive rows with the name and the chip or button on line 1, the step and the Next line full width under it; Herb in Cool in hive, 1:55 chip, "Next:" with "Remove wand" in an amber pill, not cut off; Sparky "Queued", "Waiting", "Starts in ~3:55"; footer "Estimated finish: 9:52 AM (2 of 3 hives)". |
| `app-1-all-large.png` | Same at font scale 1.3: no "...", nothing clips or overlaps. |
| `app-1-all-teal.png` | Later in the same run: Herb in Keep sealed with the teal chip and "Next:" with "Unseal" in a teal pill (passive step); Sparky on Load dose with Done and "Heat and vaporize" in an amber pill, in full. |
| `app-1-all-teal-large.png` | Same at font scale 1.3: "Heat and vaporize" still fits on one line. |
| `app-2-hive.png` | Herb's tab with the 7-step pan wand (wand stays in): Device from Load dose through Remove wand. |
| `app-3-alarm.png` | Alarm screen: Herb, "Heat and vaporize finished", Next "Cool in hive", "2:00 timer starts when you tap Done". |
| `app-4-template.png` | Step editor, wand stays in: "takes the vaporizer" on Load dose, "releases the vaporizer" on Remove wand, the "Vaporizer frees here" divider, display-only chips. |
| `app-5-settings-hives.png` | Hives card, the Developer card (debug builds only; absent from release), Display. |
| `app-6-headsup.png` | Heads-up with the app in the background (in the app the alarm screen opens directly instead): title "Herb", body "Heat and vaporize finished. Next: Cool in hive", Done and +1 min. The emulator has themed icons on, so the home screen shows Bee Timer's monochrome icon. |
| `app-7-sealed-hold-option.png` | "During the sealed hold: Wand comes out" selected: Remove wand releases, the divider sits above Keep sealed, and Keep sealed shows Passive. |

"Wand" appears only in step names and instructions and in the two sealed-hold option labels.

Launcher icon captures are in `docs/icon/launcher/`:
- `phone-drawer-normal.png`: OnePlus app drawer, squircle mask.
- `emulator-drawer-themed.png`: Pixel launcher app drawer (full color; the Pixel launcher themes the home screen only).
- `emulator-home-themed.png`: Pixel launcher home screen with themed icons on (monochrome layer).

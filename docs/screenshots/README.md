# UI screenshots

Captured 2026-11-21 on the emulator (`beetimer-api36`, Android 16, Pixel 6 size, 411 dp wide, font scale 1.15, light theme) from the current debug build by `scripts/e2e/screens.sh`. `app-1-all.png` and `app-1-all-large.png` were re-captured and `app-1-all-teal.png` added 2026-11-22 for R9. The status bar is cropped. The `mockup-*.png` files are the approved mockups rendered from `docs/mockups/varroa-timer-mockups.html`.

Scene: Herb holds the vaporizer (Heat and vaporize, real 1x timing), Sparky is queued, Emory is not started.

| File | What it shows |
|---|---|
| `app-1-all.png` | All tab (R9): "Vaporizer: Herb, free in ~15:25"; Coming up shows the next 3 events, hive and action only, in a fixed-width hive column; larger hive rows (name 20 sp, step 17, Next 16, chip 20 bold); Herb "Next:" with "Cool in hive" in an amber outlined pill (a vaporizer step) and the 2:25 chip; Sparky "Waiting" and "Starts in ~16:25"; footer "Estimated finish: 9:20 AM (2 of 3 hives)" in 12-hour format. |
| `app-1-all-large.png` | Same at font scale 1.3: nothing clips, wraps or overlaps, and the step line and Next pill stay clear of the chip. |
| `app-1-all-teal.png` | Wand comes out (separate scene, fresh data): Herb in Keep sealed with the teal chip and "Next:" with "Unseal" in a teal pill (passive step); Sparky on Load dose with Done and "Heat and vaporize" in an amber pill; footer "Estimated finish: 9:10 AM (2 of 3 hives)". |
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

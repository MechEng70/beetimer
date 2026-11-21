# UI screenshots

Captured 2026-11-21 on the emulator (`beetimer-api36`, Android 16, Pixel 6 size, 411 dp wide, font scale 1.15, light theme) from the current debug build by `scripts/e2e/screens.sh`. The status bar is cropped. The `mockup-*.png` files are the approved mockups rendered from `docs/mockups/varroa-timer-mockups.html`.

Scene: Herb holds the vaporizer (Heat and vaporize, real 1x timing), Sparky is queued, Emory is not started.

| File | What it shows |
|---|---|
| `app-1-all.png` | All tab: "Vaporizer: Herb, free in ~15:24"; Coming up with only the running step's end firm ("2:24 Cool in hive") and everything after it "~"; fixed-width hive column; Sparky "Waiting" and "Starts in ~16:24"; footer clock in 12-hour format. |
| `app-1-all-large.png` | Same at font scale 1.3: nothing clips or overlaps, and "Waiting" stays clear of the Queued chip. |
| `app-2-hive.png` | Herb's tab with the 7-step pan wand (wand stays in): Device from Load dose through Remove wand. |
| `app-3-alarm.png` | Alarm screen: Herb, "Heat and vaporize finished", Next "Cool in hive", "2:00 timer starts when you tap Done". |
| `app-4-template.png` | Step editor, wand stays in: "takes the vaporizer" on Load dose, "releases the vaporizer" on Remove wand, the "Vaporizer frees here" divider, display-only chips. |
| `app-5-settings-hives.png` | Hives card, the Developer card (debug builds only; absent from release), Display. |
| `app-6-headsup.png` | Heads-up: title "Herb", body "Heat and vaporize finished. Next: Cool in hive", Done and +1 min. |
| `app-7-sealed-hold-option.png` | "During the sealed hold: Wand comes out" selected: Remove wand releases, the divider sits above Keep sealed, and Keep sealed shows Passive. |

"Wand" appears only in step names and instructions and in the two sealed-hold option labels.

# Mockups

`varroa-timer-mockups.html` holds the four approved screens. Open it in a browser.

1. All hives tab, with the device status bar and the queue
2. One hive's tab, with the big countdown and the step list
3. Full-screen alarm over the lock screen
4. Template editor with the preset toggle

Colors: amber = a device step holding the vaporizer, teal = sealed and waiting (device free), gray = queued or not started.

Changes from the mockups (SPEC.md section 1 wins):
- Mockup 1, vaporizer bar: always two lines, no buttons. Line 2 is "Next: Sparky", or "Next: Sparky. Tap Start
  on its row." when the vaporizer is free for the queue head. Start is on the hive row and in the snackbar
  (Jason, 2026-11-23, playbook layout rule).
- Mockup 1, Coming up: always shown at 3 rows, from first launch. Before any run, row 1 reads "Start a hive to
  see what's next" (Jason, 2026-11-23).
- Mockup 2, Hive tab: a queued hive has Skip to back under Start (Jason, 2026-11-23).
- Mockup 3, alarm: when Done frees the vaporizer, "Then: Hive 3, load dose" (the hive that gets it next and the
  step it starts on) replaces "Device frees when you tap Done. Hive 3 is next." With no hive next, the app shows
  "Vaporizer frees when you tap Done." Text sizes follow SPEC section 1 (T-013a), not the mockup (Jason, R12).

The mockups show "Hive 1" and so on; in the app every label is the name you give the hive. Where the mockups and `SPEC.md` section 1 disagree, SPEC.md wins.

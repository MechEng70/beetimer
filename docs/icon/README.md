# App icon: stopwatch bee

| File | Use |
|---|---|
| `icon-background.svg`, `icon-foreground.svg`, `icon-monochrome.svg` | Android adaptive icon layers (108 dp; foreground art inside the 66 dp safe zone). Converted to `app/src/main/res/drawable/ic_launcher_{background,foreground,monochrome}.xml` by `scripts/icon/svg2vd.py`. |
| `icon-full-bleed.svg` | Master artwork, kept for iOS later. |
| `icon-1024.png` | README header and GitHub social preview. |
| `icon-preview-sheet.png` | Approved concept sheet. |
| `check.png` | Pixel comparison of the converted VectorDrawables (rendered by Android) against the SVGs (rendered by Chrome) under circle, squircle and rounded-square masks. |

Regenerate and check after changing the art:

```
python3 scripts/icon/svg2vd.py
./gradlew :app:testDebugUnitTest --tests '*IconRenderTest*'
python3 scripts/icon/compare.py      # exits non-zero if any pixel differs by more than 8/255
```

**GitHub social preview:** GitHub has no API for this. Upload `icon-1024.png` by hand in the repo's Settings, General, Social preview, after the first push.

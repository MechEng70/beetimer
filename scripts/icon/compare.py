#!/usr/bin/env python3
"""Pixel-compares the launcher icon's VectorDrawables (rendered by Android in
IconRenderTest, app/build/icon-render/) with the source SVGs (rendered by headless Chrome)
at 108 dp x 4 = 432 px, under circle, squircle and rounded-square masks applied to the
72 dp area launchers show. Writes docs/icon/check.png and prints the differences.

Usage: ./gradlew :app:testDebugUnitTest --tests '*IconRenderTest*' && python3 scripts/icon/compare.py
"""
import os
import subprocess
import sys
import tempfile
from PIL import Image, ImageChops, ImageDraw, ImageFont

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
SIZE = 432
VISIBLE = SIZE * 72 // 108  # launchers show the middle 72 dp
OFF = (SIZE - VISIBLE) // 2


def chrome_render(svg, out):
    html = os.path.join(tempfile.mkdtemp(), "r.html")
    with open(html, "w") as f:
        f.write(f'<html><body style="margin:0;background:transparent"><img src="file://{svg}" style="width:{SIZE}px;height:{SIZE}px;display:block"></body></html>')
    subprocess.run(["google-chrome", "--headless=new", "--disable-gpu", "--hide-scrollbars", "--default-background-color=00000000",
                    f"--window-size={SIZE},{SIZE}", f"--screenshot={out}", f"file://{html}"], check=True, capture_output=True)
    return Image.open(out).convert("RGBA").crop((0, 0, SIZE, SIZE))


def mask(kind):
    m = Image.new("L", (SIZE, SIZE), 0)
    d = ImageDraw.Draw(m)
    box = (OFF, OFF, OFF + VISIBLE - 1, OFF + VISIBLE - 1)
    if kind == "circle":
        d.ellipse(box, fill=255)
    elif kind == "rounded square":
        d.rounded_rectangle(box, radius=VISIBLE * 0.18, fill=255)
    else:  # squircle: superellipse |x|^4 + |y|^4 <= 1
        r = VISIBLE / 2
        cx = cy = SIZE / 2
        for y in range(OFF, OFF + VISIBLE):
            for x in range(OFF, OFF + VISIBLE):
                if (abs((x + 0.5 - cx) / r) ** 4 + abs((y + 0.5 - cy) / r) ** 4) <= 1:
                    m.putpixel((x, y), 255)
    return m


def masked(img, m):
    out = Image.new("RGBA", (SIZE, SIZE), (255, 255, 255, 0))
    out.paste(img, (0, 0), m)
    return out


def stats(a, b, m):
    da = ImageChops.difference(a.convert("RGBA"), b.convert("RGBA"))
    data = lambda im: im.get_flattened_data() if hasattr(im, "get_flattened_data") else im.getdata()
    px = [max(p[:3] + (p[3],)) for p, k in zip(data(da), data(m)) if k]
    big = sum(1 for v in px if v > 64)
    return sum(px) / len(px), max(px), 100 * big / len(px), da


def main():
    tmp = tempfile.mkdtemp()
    src = os.path.join(ROOT, "docs", "icon")
    ren = os.path.join(ROOT, "app", "build", "icon-render")
    svg = {k: chrome_render(os.path.join(src, f"icon-{k}.svg"), os.path.join(tmp, f"{k}.png")) for k in ("background", "foreground", "monochrome")}
    vd = {k: Image.open(os.path.join(ren, f"{k}.png")).convert("RGBA") for k in ("background", "foreground", "monochrome")}
    svg_icon = Image.alpha_composite(svg["background"], svg["foreground"])
    vd_icon = Image.alpha_composite(vd["background"], vd["foreground"])
    # Monochrome: themed icons use only the layer's alpha.
    alpha = lambda im: Image.merge("RGBA", [Image.new("L", (SIZE, SIZE), 0)] * 3 + [im.getchannel("A")])
    rows, worst, worst_max = [], 0.0, 0
    for kind in ("circle", "squircle", "rounded square"):
        m = mask(kind)
        mean, mx, big, diff = stats(masked(svg_icon, m), masked(vd_icon, m), m)
        rows.append((f"{kind}", masked(svg_icon, m), masked(vd_icon, m), diff, mean, mx, big))
        worst, worst_max = max(worst, big), max(worst_max, mx)
    m = mask("circle")
    mean, mx, big, diff = stats(masked(alpha(svg["monochrome"]), m), masked(alpha(vd["monochrome"]), m), m)
    rows.append(("monochrome (alpha), circle", masked(alpha(svg["monochrome"]), m), masked(alpha(vd["monochrome"]), m), diff, mean, mx, big))
    worst, worst_max = max(worst, big), max(worst_max, mx)

    pad, head = 16, 40
    sheet = Image.new("RGB", (pad + 3 * (SIZE + pad), head + len(rows) * (SIZE + head + pad)), (244, 244, 242))
    d = ImageDraw.Draw(sheet)
    font = ImageFont.load_default(size=18) if hasattr(ImageFont, "load_default") else None
    for i, label in enumerate(("SVG (Chrome)", "VectorDrawable (Android)", "difference x8")):
        d.text((pad + i * (SIZE + pad), 10), label, fill=(31, 31, 29), font=font)
    for r, (name, a, b, diff, mean, mx, big) in enumerate(rows):
        y = head + r * (SIZE + head + pad)
        d.text((pad, y), f"{name}: mean diff {mean:.2f}/255, max {mx}, pixels off by >64: {big:.3f}%", fill=(31, 31, 29), font=font)
        y += head - 12
        for c, im in enumerate((a, b)):
            bg = Image.new("RGBA", (SIZE, SIZE), (255, 255, 255, 255))
            sheet.paste(Image.alpha_composite(bg, im).convert("RGB"), (pad + c * (SIZE + pad), y))
        amp = diff.convert("RGB").point(lambda v: min(255, v * 8))
        sheet.paste(amp, (pad + 2 * (SIZE + pad), y))
        print(f"{name:28s} mean {mean:6.2f}  max {mx:3d}  >64: {big:.3f}%")
    sheet.save(os.path.join(src, "check.png"))
    print("wrote docs/icon/check.png")
    # Fail on any pixel off by more than 8/255 (a 1 px shift or a color change shows far more).
    return 0 if worst == 0 and worst_max <= 8 else 1


if __name__ == "__main__":
    sys.exit(main())

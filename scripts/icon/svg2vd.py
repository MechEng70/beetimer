#!/usr/bin/env python3
"""Converts the icon SVG layers in docs/icon/ to Android VectorDrawables.

Supports what the icon uses: rect (with rx), circle, ellipse, line, path (any SVG path data;
VectorDrawable pathData accepts the same syntax, arcs included), fill, stroke, stroke-width,
stroke-linecap. Each SVG transform (translate, scale, rotate about a point) becomes one nested
<group>, outermost first, which is exact: no flattening or approximation. Metadata is dropped.

Usage: python3 scripts/icon/svg2vd.py
"""
import os
import re
import xml.etree.ElementTree as ET

ROOT = os.path.join(os.path.dirname(__file__), "..", "..")
SRC = os.path.join(ROOT, "docs", "icon")
OUT = os.path.join(ROOT, "app", "src", "main", "res", "drawable")
SVG = "{http://www.w3.org/2000/svg}"
LAYERS = {"icon-background.svg": "ic_launcher_background.xml",
          "icon-foreground.svg": "ic_launcher_foreground.xml",
          "icon-monochrome.svg": "ic_launcher_monochrome.xml"}


def num(v):
    return f"{float(v):g}"


def color(c):
    c = c.strip()
    if c.lower() == "none":
        return None
    if re.fullmatch(r"#[0-9a-fA-F]{3}", c):
        c = "#" + "".join(ch * 2 for ch in c[1:])
    if not re.fullmatch(r"#[0-9a-fA-F]{6}", c):
        raise ValueError(f"unsupported color {c}")
    return "#FF" + c[1:].upper()


def transforms(t):
    """'translate(54 54) scale(0.78) rotate(45 85 27.5)' -> list of group attribute dicts."""
    out = []
    for name, args in re.findall(r"(\w+)\(([^)]*)\)", t or ""):
        a = [float(x) for x in re.split(r"[\s,]+", args.strip()) if x]
        if name == "translate":
            out.append({"translateX": a[0], "translateY": a[1] if len(a) > 1 else 0.0})
        elif name == "scale":
            out.append({"scaleX": a[0], "scaleY": a[1] if len(a) > 1 else a[0]})
        elif name == "rotate":
            g = {"rotation": a[0]}
            if len(a) == 3:
                g.update({"pivotX": a[1], "pivotY": a[2]})
            out.append(g)
        else:
            raise ValueError(f"unsupported transform {name}")
    return out


def ellipse_path(cx, cy, rx, ry):
    return (f"M{num(cx - rx)},{num(cy)} A{num(rx)},{num(ry)} 0 1,1 {num(cx + rx)},{num(cy)} "
            f"A{num(rx)},{num(ry)} 0 1,1 {num(cx - rx)},{num(cy)} Z")


def shape_path(el):
    tag = el.tag.replace(SVG, "")
    g = lambda k, d=0.0: float(el.get(k, d))
    if tag == "rect":
        x, y, w, h = g("x"), g("y"), g("width"), g("height")
        rx = g("rx", el.get("ry", 0)); ry = g("ry", rx)
        if rx == 0:
            return f"M{num(x)},{num(y)} H{num(x + w)} V{num(y + h)} H{num(x)} Z"
        return (f"M{num(x + rx)},{num(y)} H{num(x + w - rx)} A{num(rx)},{num(ry)} 0 0,1 {num(x + w)},{num(y + ry)} "
                f"V{num(y + h - ry)} A{num(rx)},{num(ry)} 0 0,1 {num(x + w - rx)},{num(y + h)} "
                f"H{num(x + rx)} A{num(rx)},{num(ry)} 0 0,1 {num(x)},{num(y + h - ry)} "
                f"V{num(y + ry)} A{num(rx)},{num(ry)} 0 0,1 {num(x + rx)},{num(y)} Z")
    if tag == "circle":
        return ellipse_path(g("cx"), g("cy"), g("r"), g("r"))
    if tag == "ellipse":
        return ellipse_path(g("cx"), g("cy"), g("rx"), g("ry"))
    if tag == "line":
        return f"M{num(g('x1'))},{num(g('y1'))} L{num(g('x2'))},{num(g('y2'))}"
    if tag == "path":
        return re.sub(r"\s+", " ", el.get("d").strip())
    raise ValueError(f"unsupported element {tag}")


def emit(el, indent, lines):
    tag = el.tag.replace(SVG, "")
    if tag in ("metadata", "title", "desc"):
        return
    groups = transforms(el.get("transform"))
    for gattrs in groups:
        attrs = " ".join(f'android:{k}="{num(v)}"' for k, v in gattrs.items())
        lines.append(f"{'    ' * indent}<group {attrs}>")
        indent += 1
    if tag == "g":
        for child in el:
            emit(child, indent, lines)
    else:
        attrs = [f'android:pathData="{shape_path(el)}"']
        fill = el.get("fill", "#000000" if tag != "line" else "none")
        if color(fill):
            attrs.append(f'android:fillColor="{color(fill)}"')
        stroke = el.get("stroke")
        if stroke and color(stroke):
            attrs.append(f'android:strokeColor="{color(stroke)}"')
            attrs.append(f'android:strokeWidth="{num(el.get("stroke-width", 1))}"')
            if el.get("stroke-linecap"):
                attrs.append(f'android:strokeLineCap="{el.get("stroke-linecap")}"')
        pad = "    " * indent
        lines.append(f"{pad}<path\n{pad}    " + f"\n{pad}    ".join(attrs) + " />")
    for _ in groups:
        indent -= 1
        lines.append(f"{'    ' * indent}</group>")


def convert(src, dst):
    root = ET.parse(src).getroot()
    vb = [float(v) for v in root.get("viewBox").split()]
    lines = [
        '<?xml version="1.0" encoding="utf-8"?>',
        f"<!-- Generated from docs/icon/{os.path.basename(src)} by scripts/icon/svg2vd.py. Do not edit by hand. -->",
        '<vector xmlns:android="http://schemas.android.com/apk/res/android"',
        f'    android:width="{num(vb[2])}dp"',
        f'    android:height="{num(vb[3])}dp"',
        f'    android:viewportWidth="{num(vb[2])}"',
        f'    android:viewportHeight="{num(vb[3])}">',
    ]
    for child in root:
        emit(child, 1, lines)
    lines.append("</vector>")
    with open(dst, "w") as f:
        f.write("\n".join(lines) + "\n")


def main():
    for src, dst in LAYERS.items():
        convert(os.path.join(SRC, src), os.path.join(OUT, dst))
        print(f"{src} -> res/drawable/{dst}")


if __name__ == "__main__":
    main()

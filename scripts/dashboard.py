#!/usr/bin/env python3
"""Writes docs/dashboard.html from the docs. Never edit the HTML by hand.

Reads docs/design/TASKS.md (R11 tasks), docs/design/USAGE.md, docs/HANDOFF.md (Next line and the
"Waiting on Jason" list), build/gate/last.tsv and the test results (last scripts/check.sh run),
docs/e2e/REPORT.md, gradle.properties (appVersion) and ~/.claude/beetimer-usage.json (status line
snapshot). Standard library only. Run by the main session after each task and by the dashboard timer
(scripts/dashboard-timer.sh) every 15 minutes. Safety net: if the repo has had no commit for 12 hours,
it stops its own timer.
"""

from __future__ import annotations

import base64
import glob
import html
import json
import os
import re
import subprocess
import sys
import time
import xml.etree.ElementTree as ET
from datetime import datetime

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT = os.path.join(REPO, "docs", "dashboard.html")
USAGE_SNAPSHOT = os.path.expanduser("~/.claude/beetimer-usage.json")
TIMER_UNIT = "beetimer-dashboard"
STALE_SECONDS = 30 * 60
IDLE_STOP_SECONDS = 12 * 3600


def read(rel: str) -> str:
    try:
        with open(os.path.join(REPO, rel), encoding="utf-8") as f:
            return f.read()
    except OSError:
        return ""


def esc(s: object) -> str:
    return html.escape(str(s), quote=True)


def inline_md(s: str) -> str:
    """Escapes text, then renders `code` and **bold**."""
    s = esc(s)
    s = re.sub(r"`([^`]+)`", r"<code>\1</code>", s)
    return re.sub(r"\*\*([^*]+)\*\*", r"<b>\1</b>", s)


# ---- sources ----------------------------------------------------------------------------------


def parse_tasks(text: str) -> list[dict]:
    """R11 tasks: '### Phase N: name' then '#### T-001 [x] Title (S)' with '- **Field:** value' lines."""
    phases: list[dict] = []
    start = text.find("## Round R11")
    if start < 0:
        return phases
    end = text.find("\n## ", start + 5)
    body = text[start : end if end > 0 else len(text)]
    task = None
    for line in body.splitlines():
        m = re.match(r"^### (Phase [^:]+): (.+)$", line)
        if m:
            phases.append({"id": m.group(1), "name": m.group(2), "tasks": []})
            continue
        m = re.match(r"^#### (T-\d{3}[a-z]?) \[(.)\] (.+?)(?: \((S|M|L)\))?\s*$", line)
        if m and phases:
            task = {"id": m.group(1), "mark": m.group(2), "title": m.group(3), "size": m.group(4) or "", "fields": {}}
            phases[-1]["tasks"].append(task)
            continue
        if task is None:
            continue
        m = re.match(r"^- \*\*([^:*]+):\*\*\s*(.*)$", line)
        if m:
            task["fields"][m.group(1).strip()] = m.group(2).strip()
            continue
        m = re.match(r"^- Done: (.*)$", line)
        if m:
            task["fields"]["Done"] = m.group(1).strip()
    return phases


STATUS = {"x": ("done", "Done"), "~": ("doing", "In progress"), "!": ("wait", "Waiting on Jason"), " ": ("todo", "Not started")}


def parse_table(text: str, first_header: str) -> list[dict]:
    rows: list[dict] = []
    header = None
    for line in text.splitlines():
        if not line.startswith("|"):
            if header is not None and rows:
                break
            continue
        cells = [c.strip() for c in line.strip().strip("|").split("|")]
        if header is None:
            if cells and cells[0] == first_header:
                header = cells
            continue
        if set("".join(cells)) <= set("-: "):
            continue
        rows.append(dict(zip(header, cells)))
    return rows


def handoff_next(text: str) -> str:
    m = re.search(r"^- \*\*Next:\*\* (.+)$", text, re.M)
    return m.group(1).strip() if m else ""


def handoff_waiting(text: str) -> list[str]:
    m = re.search(r"^### Waiting on Jason\s*\n(.*?)(?=^#|\Z)", text, re.M | re.S)
    if not m:
        return []
    items: list[str] = []
    for line in m.group(1).splitlines():
        if line.startswith("- "):
            items.append(line[2:].strip())
        elif line.startswith("  ") and items:
            items[-1] += " " + line.strip()
    return [i for i in items if i and i.lower() != "nothing."]


def gate_status() -> dict:
    steps = []
    finished = ""
    try:
        with open(os.path.join(REPO, "build", "gate", "last.tsv"), encoding="utf-8") as f:
            for line in f:
                parts = line.rstrip("\n").split("\t")
                if parts[0] == "finished":
                    finished = parts[1] if len(parts) > 1 else ""
                elif len(parts) >= 2:
                    steps.append({"name": parts[0], "status": parts[1], "secs": parts[2] if len(parts) > 2 else ""})
    except OSError:
        pass
    tests = fails = 0
    for p in glob.glob(os.path.join(REPO, "*", "build", "test-results", "**", "TEST-*.xml"), recursive=True):
        try:
            r = ET.parse(p).getroot()
            tests += int(r.get("tests", 0))
            fails += int(r.get("failures", 0)) + int(r.get("errors", 0))
        except (ET.ParseError, ValueError, OSError):
            fails += 1
    return {"steps": steps, "finished": finished, "tests": tests, "test_fails": fails}


def e2e_status(text: str) -> dict:
    rows = parse_table(text, "Scenario")
    passed = sum(1 for r in rows if r.get("Result") == "PASS")
    inv = re.search(r"Invariant errors across all runs: \*\*(\d+)\*\*", text)
    head = re.search(r"^Emulator: (.+)$", text, re.M)
    return {"runs": len(rows), "passed": passed, "invariants": inv.group(1) if inv else "?", "note": head.group(1) if head else ""}


def version_from(gradle_properties: str) -> str:
    """The app version name: appVersion in gradle.properties (R11 T-014)."""
    m = re.search(r"^appVersion=(\S+)\s*$", gradle_properties, re.M)
    return m.group(1) if m else "?"


def version() -> str:
    return version_from(read("gradle.properties"))


def usage_snapshot() -> dict | None:
    try:
        with open(USAGE_SNAPSHOT, encoding="utf-8") as f:
            return json.load(f)
    except (OSError, ValueError):
        return None


def last_commit() -> tuple[int, str]:
    try:
        out = subprocess.run(["git", "-C", REPO, "log", "-1", "--format=%ct %h %s"], capture_output=True, text=True, timeout=10).stdout.strip()
        ts, rest = out.split(" ", 1)
        return int(ts), rest
    except (OSError, ValueError, subprocess.SubprocessError):
        return 0, ""


def icon_data_uri() -> str:
    try:
        with open(os.path.join(REPO, "docs", "icon", "icon-full-bleed.svg"), "rb") as f:
            return "data:image/svg+xml;base64," + base64.b64encode(f.read()).decode()
    except OSError:
        return ""


# ---- throttle (playbook section 9) ------------------------------------------------------------


def throttle(snap: dict | None, now: float) -> tuple[str, str]:
    """Returns (level, text). Levels: open, normal, throttled, stop, unknown. The stricter table wins."""
    if not snap:
        return "unknown", "No usage snapshot yet. The status line writes one when Claude Code refreshes it."
    if now - snap.get("updated", 0) > STALE_SECONDS:
        return "unknown", "Usage snapshot is stale (over 30 min). Check /usage before starting a task."
    order = ["open", "normal", "throttled", "stop"]
    level, why = "normal", []
    wk = snap.get("seven_day") or {}
    used, budget = wk.get("used_percentage"), wk.get("budget_percentage")
    if used is not None and budget is not None:
        diff = used - budget
        if used > 95:
            wl = "stop"
            why.append(f"weekly {used:.0f}% is over 95%")
        elif diff >= 10:
            wl = "stop"
            why.append(f"weekly {used:.0f}% is {diff:.0f} points over today's {budget}% budget")
        elif diff >= 0:
            wl = "throttled"
            why.append(f"weekly {used:.0f}% is at or over today's {budget}% budget")
        elif diff <= -20:
            wl = "open"
            why.append(f"weekly {used:.0f}% is {-diff:.0f} points under today's {budget}% budget")
        else:
            wl = "normal"
            why.append(f"weekly {used:.0f}% is under today's {budget}% budget")
        level = wl
    h5 = (snap.get("five_hour") or {}).get("used_percentage")
    if h5 is not None:
        hl = "stop" if h5 > 85 else "throttled" if h5 >= 70 else "normal"
        why.append(f"5-hour {h5:.0f}%")
        if order.index(hl) > order.index(level):
            level = hl
    text = {
        "open": "Wide open: parallel agents allowed, normal models.",
        "normal": "Normal: agents allowed.",
        "throttled": "Throttled: no new agents, cheapest capable model, S and M tasks only.",
        "stop": "Stop: finish the current task, commit, update HANDOFF and the dashboard, and report.",
    }[level]
    return level, text + " (" + "; ".join(why) + ")"


# ---- rendering --------------------------------------------------------------------------------

CSS = """
:root { --bg:#F4F4F2; --card:#FFFFFF; --line:#D3D1C7; --ink:#1F1F1D; --ink2:#5F5E5A;
  --amber-bg:#FAEEDA; --amber:#633806; --amber-strong:#FAC775; --amber-line:#BA7517;
  --teal-bg:#E1F5EE; --teal:#085041; --teal-line:#1D9E75; --gray-bg:#F1EFE8; --gray:#5F5E5A; --danger:#A32D2D;
  --sans: 'Inter', -apple-system, system-ui, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;
  --mono: 'JetBrains Mono', 'SFMono-Regular', Consolas, monospace; }
* { box-sizing: border-box; }
body { margin:0; background:var(--bg); color:var(--ink); font:15px/1.5 var(--sans); }
main { max-width:980px; margin:0 auto; padding:20px 16px 48px; }
header { display:flex; flex-wrap:wrap; gap:12px; align-items:center; justify-content:space-between; }
.titlebox { display:flex; gap:12px; align-items:center; }
.titlebox img { width:48px; height:48px; border-radius:12px; }
h1 { font-size:24px; font-weight:600; margin:0; }
h2 { font:600 12px/1.5 var(--mono); letter-spacing:1.5px; text-transform:uppercase; color:var(--ink2); margin:24px 0 8px; }
.sub { color:var(--ink2); font-size:13px; }
.num { font-family:var(--mono); font-variant-numeric:tabular-nums; white-space:nowrap; }
code { font-family:var(--mono); font-size:0.9em; background:var(--gray-bg); padding:0 4px; border-radius:4px; }
button { font:inherit; font-weight:600; color:#fff; background:var(--teal); border:0; border-radius:999px; padding:8px 18px; min-height:40px; cursor:pointer; }
button.quiet { background:transparent; color:var(--ink); border:1px solid var(--line); }
.card { background:var(--card); border:1px solid var(--line); border-radius:16px; padding:14px 16px; }
.status { margin-top:12px; font-size:16px; }
.progress { display:flex; height:14px; border-radius:7px; overflow:hidden; background:var(--gray-bg); border:1px solid var(--line); margin:8px 0 4px; }
.progress span { display:block; height:100%; }
.p-done { background:var(--teal-line); } .p-doing { background:var(--amber-line); } .p-wait { background:var(--danger); }
.wait { border:2px solid var(--danger); }
.wait ul { margin:6px 0 0; padding-left:20px; }
.grid { display:grid; gap:12px; grid-template-columns:repeat(auto-fit, minmax(260px, 1fr)); }
.meter { position:relative; height:18px; border-radius:9px; background:var(--gray-bg); border:1px solid var(--line); overflow:hidden; margin:6px 0; }
.meter .fill { position:absolute; left:0; top:0; bottom:0; }
.meter .budget { position:absolute; top:-2px; bottom:-2px; width:2px; background:var(--ink); }
.fill.ok { background:var(--teal-line); } .fill.warn { background:var(--amber-line); } .fill.bad { background:var(--danger); }
.stale { color:var(--danger); font-weight:600; }
.chip { display:inline-block; font-size:12px; font-weight:600; border-radius:999px; padding:1px 10px; white-space:nowrap; }
.s-done { background:var(--teal-bg); color:var(--teal); } .s-doing { background:var(--amber-bg); color:var(--amber); }
.s-wait { background:#F7E1E1; color:var(--danger); } .s-todo { background:var(--gray-bg); color:var(--gray); }
.s-pass { background:var(--teal-bg); color:var(--teal); } .s-fail { background:#F7E1E1; color:var(--danger); }
.s-none { background:var(--gray-bg); color:var(--gray); }
table { width:100%; border-collapse:collapse; font-size:14px; }
th { text-align:left; font-weight:600; color:var(--ink2); font-size:12px; padding:4px 6px; border-bottom:1px solid var(--line); }
td { padding:6px; border-bottom:1px solid var(--gray-bg); vertical-align:top; }
tr.doing td { background:var(--amber-bg); }
table.tasks { table-layout:fixed; }
.phase { margin-top:10px; }
.phase h3 { font-size:14px; margin:12px 0 4px; }
details.sec { margin-top:16px; background:var(--card); border:1px solid var(--line); border-radius:16px; padding:10px 16px; }
details.sec summary { cursor:pointer; font-weight:600; }
footer { margin-top:28px; color:var(--ink2); font-size:12px; }
@media (max-width:600px) { .hide-sm { display:none; } }
"""

JS = """
const written = new Date(document.body.dataset.written);
function tick() {
  const mins = Math.round((Date.now() - written.getTime()) / 60000);
  document.getElementById('age').textContent = mins < 1 ? 'just now' : mins + ' min ago';
}
tick(); setInterval(tick, 30000);
let auto = false; try { auto = localStorage.getItem('bt-auto') === '1'; } catch (e) {}
function paintAuto() { document.getElementById('auto').textContent = 'Auto reload: ' + (auto ? 'on' : 'off'); }
function toggleAuto() { auto = !auto; try { localStorage.setItem('bt-auto', auto ? '1' : '0'); } catch (e) {} paintAuto(); }
paintAuto();
setInterval(() => { if (auto) location.reload(); }, 60000);
"""


def fmt_time(epoch: float | None) -> str:
    if not epoch:
        return "?"
    return datetime.fromtimestamp(epoch).strftime("%a %b %-d, %-I:%M %p")


def fmt_left(epoch: float | None, now: float) -> str:
    if not epoch:
        return "?"
    s = max(0, int(epoch - now))
    if s >= 86400:
        return f"{s // 86400}d {s % 86400 // 3600}h"
    return f"{s // 3600}h {s % 3600 // 60:02d}m"


def meter(label: str, used: float | None, resets: float | None, now: float, budget: float | None = None, extra: str = "") -> str:
    if used is None:
        return f'<div class="card"><b>{esc(label)}</b><div class="sub">No data in the snapshot.</div></div>'
    cls = "ok" if used < 70 else "warn" if used <= 85 else "bad"
    if budget is not None:
        cls = "bad" if used - budget >= 10 or used > 95 else "warn" if used >= budget else "ok"
    budget_mark = f'<div class="budget" style="left:{min(budget, 100):.1f}%" title="today\'s budget"></div>' if budget is not None else ""
    budget_txt = f" &middot; today's budget <span class=num>{budget:.0f}%</span>" if budget is not None else ""
    return (
        f'<div class="card"><b>{esc(label)}</b> <span class="num" style="float:right">{used:.0f}%</span>'
        f'<div class="meter"><div class="fill {cls}" style="width:{min(used, 100):.1f}%"></div>{budget_mark}</div>'
        f'<div class="sub">Resets {esc(fmt_time(resets))} (in <span class=num>{esc(fmt_left(resets, now))}</span>){budget_txt}{extra}</div></div>'
    )


def render() -> str:
    now = time.time()
    tasks_md = read("docs/design/TASKS.md")
    usage_md = read("docs/design/USAGE.md")
    handoff = read("docs/HANDOFF.md")
    phases = parse_tasks(tasks_md)
    all_tasks = [t for p in phases for t in p["tasks"]]
    usage_rows = parse_table(usage_md, "Task")
    by_task = {}
    for r in usage_rows:
        by_task.setdefault(r.get("Task", ""), r)
    gate = gate_status()
    e2e = e2e_status(read("docs/e2e/REPORT.md"))
    snap = usage_snapshot()
    level, level_text = throttle(snap, now)
    commit_ts, commit_txt = last_commit()
    nxt = handoff_next(handoff)
    waiting = handoff_waiting(handoff)

    counts = {k: 0 for k in ("done", "doing", "wait", "todo")}
    for t in all_tasks:
        counts[STATUS.get(t["mark"], STATUS[" "])[0]] += 1
    total = max(1, len(all_tasks))
    current = next((t for t in all_tasks if t["mark"] in "~!"), None) or next((t for t in all_tasks if t["mark"] == " "), None)

    written = datetime.now().astimezone().isoformat(timespec="seconds")
    h = []
    h.append(f'<!doctype html><html lang="en"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1">')
    h.append(f"<title>Bee Timer status</title><style>{CSS}</style></head><body data-written=\"{esc(written)}\"><main>")
    icon = icon_data_uri()
    h.append('<header><div class="titlebox">' + (f'<img src="{icon}" alt="">' if icon else ""))
    h.append(f'<div><h1>Bee Timer status</h1><div class="sub">Version <span class=num>{esc(version())}</span> &middot; round R11, playbook adoption &middot; '
             f'written <span class=num>{esc(datetime.now().strftime("%Y-%m-%d %-I:%M %p"))}</span> (<span id="age"></span>)</div></div></div>')
    h.append('<div><button onclick="location.reload()">Reload</button> <button class="quiet" id="auto" onclick="toggleAuto()">Auto reload: off</button></div></header>')

    cur = f'<b>{esc(current["id"])}</b> {esc(current["title"])}' if current else "All tasks done"
    h.append(f'<div class="status">Now: {cur}. {inline_md(nxt) if nxt and not nxt.startswith(current["id"] if current else "~") else ""}</div>')
    h.append('<div class="progress">' + "".join(
        f'<span class="p-{k}" style="width:{counts[k] * 100 / total:.2f}%"></span>' for k in ("done", "doing", "wait")) + "</div>")
    h.append(f'<div class="sub"><span class=num>{counts["done"]} of {len(all_tasks)}</span> tasks done'
             + (f', {counts["doing"]} in progress' if counts["doing"] else "")
             + (f', {counts["wait"]} waiting on Jason' if counts["wait"] else "") + "</div>")

    h.append("<h2>Waiting on Jason</h2>")
    if waiting:
        h.append('<div class="card wait"><ul>' + "".join(f"<li>{inline_md(w)}</li>" for w in waiting) + "</ul></div>")
    else:
        h.append('<div class="card sub">Nothing right now.</div>')

    h.append("<h2>Usage</h2>")
    stale = ""
    if snap:
        age = now - snap.get("updated", 0)
        stale_cls = ' class="stale"' if age > STALE_SECONDS else ""
        stale = f' &middot; snapshot <span{stale_cls}>{int(age // 60)} min old{" (stale)" if age > STALE_SECONDS else ""}</span>'
        fh, wk = snap.get("five_hour") or {}, snap.get("seven_day") or {}
        h.append('<div class="grid">')
        h.append(meter("5-hour session", fh.get("used_percentage"), fh.get("resets_at"), now, extra=stale))
        day = wk.get("day")
        h.append(meter("Week (all models)", wk.get("used_percentage"), wk.get("resets_at"), now, budget=wk.get("budget_percentage"),
                       extra=(f" &middot; day <span class=num>{day}</span> of 7" if day else "") + stale))
        h.append("</div>")
    lvl_cls = {"open": "s-done", "normal": "s-done", "throttled": "s-doing", "stop": "s-wait"}.get(level, "s-none")
    h.append(f'<div class="card" style="margin-top:12px"><span class="chip {lvl_cls}">Throttle: {esc(level)}</span> {esc(level_text)}'
             '<div class="sub" style="margin-top:4px">The status line sees the 5-hour and the all-models weekly limit only. '
             "A model-specific weekly bar (for example Fable) shows only in <code>/usage</code>: glance at it once a day.</div></div>")

    h.append("<h2>Gate and end to end</h2><div class=\"grid\">")
    rows = []
    for s in gate["steps"]:
        st = "pass" if s["status"] == "pass" else "fail"
        extra = f' &middot; <span class=num>{gate["tests"]}</span> tests, {gate["test_fails"]} failed' if s["name"] == "tests" else ""
        rows.append(f'<tr><td>{esc(s["name"])}</td><td><span class="chip s-{st}">{esc(s["status"])}</span>{extra}</td><td class="num">{esc(s["secs"])} s</td></tr>')
    for planned in ("ktlint", "detekt", "semgrep", "snyk"):
        if not any(s["name"].startswith(planned) for s in gate["steps"]):
            rows.append(f'<tr><td>{planned}</td><td><span class="chip s-none">not in the gate yet</span></td><td></td></tr>')
    fin = gate["finished"]
    h.append('<div class="card"><b>scripts/check.sh</b>' + (f'<div class="sub">Last run {esc(fin.replace("T", " ")[:16])}</div>' if fin else '<div class="sub">No run recorded yet.</div>')
             + "<table>" + "".join(rows) + "</table></div>")
    e2e_cls = "s-pass" if e2e["runs"] and e2e["passed"] == e2e["runs"] else "s-doing"
    h.append(f'<div class="card"><b>E2E (docs/e2e/REPORT.md)</b><div style="margin-top:6px"><span class="chip {e2e_cls}">'
             f'{e2e["passed"]} of {e2e["runs"]} runs pass</span> &middot; invariant errors <span class=num>{esc(e2e["invariants"])}</span></div>'
             f'<div class="sub">{esc(e2e["note"])}</div></div>')
    h.append("</div>")

    h.append("<h2>Tasks by phase</h2><div class=\"card\">")
    for p in phases:
        h.append(f'<div class="phase"><h3>{esc(p["id"])}: {esc(p["name"])}</h3><table class="tasks">'
                 '<colgroup><col style="width:9%"><col><col style="width:17%"><col style="width:11%"><col style="width:6%">'
                 '<col class="hide-sm" style="width:16%"><col class="hide-sm" style="width:9%"></colgroup>'
                 '<tr><th>ID</th><th>Task</th><th>Status</th>'
                 '<th>Commit</th><th>Size</th><th class="hide-sm">Model</th><th class="hide-sm">Agent</th></tr>')
        for t in p["tasks"]:
            key, label = STATUS.get(t["mark"], STATUS[" "])
            u = by_task.get(t["id"], {})
            commit = re.findall(r"`([0-9a-f]{7,})`", t["fields"].get("Done", ""))
            planned = re.search(r"\*\*Model:\*\* ([^.]+)", t["fields"].get("Size", ""))
            model = u.get("Model") or (planned.group(1) if planned else "")
            agent = u.get("Agent or main") or ""
            h.append(f'<tr class="{key}"><td class="num">{esc(t["id"])}</td><td>{esc(t["title"])}</td><td><span class="chip s-{key}">{label}</span></td>'
                     f'<td class="num">{esc(", ".join(commit))}</td><td>{esc(t["size"])}</td><td class="hide-sm">{esc(model)}</td><td class="hide-sm">{esc(agent)}</td></tr>')
        h.append("</table></div>")
    h.append("</div>")

    # Usage log summary
    def summarize(key: str) -> str:
        agg: dict[str, int] = {}
        for r in usage_rows:
            agg[r.get(key, "") or "?"] = agg.get(r.get(key, "") or "?", 0) + 1
        return ", ".join(f"{esc(k)}: <span class=num>{v}</span>" for k, v in sorted(agg.items())) or "none yet"

    h.append('<details class="sec"><summary>Usage log (docs/design/USAGE.md)</summary>')
    h.append(f'<div class="sub" style="margin-top:6px">Tasks by model: {summarize("Model")}<br>By agent or main: {summarize("Agent or main")}<br>By size: {summarize("Size")}</div>')
    if usage_rows:
        cols = list(usage_rows[0].keys())
        h.append("<table><tr>" + "".join(f"<th>{esc(c)}</th>" for c in cols) + "</tr>")
        for r in usage_rows:
            h.append("<tr>" + "".join(f"<td>{inline_md(r.get(c, ''))}</td>" for c in cols) + "</tr>")
        h.append("</table>")
    h.append("</details>")

    h.append(f'<footer>Generated by <code>scripts/dashboard.py</code> from TASKS.md, USAGE.md, HANDOFF.md, the last gate run and '
             f'<code>~/.claude/beetimer-usage.json</code>. Never edit by hand. Last commit: <span class=num>{esc(commit_txt)}</span>'
             f'{" (" + esc(fmt_time(commit_ts)) + ")" if commit_ts else ""}.</footer>')
    h.append(f"</main><script>{JS}</script></body></html>\n")
    return "\n".join(h)


def stop_timer_if_idle() -> None:
    ts, _ = last_commit()
    if ts and time.time() - ts > IDLE_STOP_SECONDS:
        subprocess.run(["systemctl", "--user", "stop", f"{TIMER_UNIT}.timer"], capture_output=True, timeout=10)
        print(f"No commit for 12 hours: stopped {TIMER_UNIT}.timer", file=sys.stderr)


def main() -> int:
    page = render()
    tmp = OUT + ".tmp"
    with open(tmp, "w", encoding="utf-8") as f:
        f.write(page)
    if os.path.getsize(tmp) == 0:
        os.remove(tmp)
        print("dashboard.py: empty output, not written", file=sys.stderr)
        return 1
    os.replace(tmp, OUT)
    if "--timer" in sys.argv:
        stop_timer_if_idle()
    print(f"Wrote {os.path.relpath(OUT, REPO)}")
    return 0


if __name__ == "__main__":
    sys.exit(main())

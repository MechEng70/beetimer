#!/usr/bin/env python3
"""Checks the E2E runs in build/e2e/fs<font scale>/<scenario>/ and writes docs/e2e/REPORT.md.

Every scenario: Maestro passed, zero BeeTimerInvariant errors, no crash, and the vaporizer
only ever moves free -> holder -> free (never straight from one hive to another).
Plus scenario-specific checks from the BeeTimerE2E event log, and planner predicted vs
actual times for scenarios 1 and 2.
"""
import os
import re
import sys

REPO = os.path.join(os.path.dirname(__file__), "..", "..")
ROOT = os.path.join(REPO, "docs", "e2e")
# Raw runs (git-ignored): build/e2e/fs<scale>/<scenario>/, written by run.sh.
RUNS = os.path.join(REPO, "build", "e2e")
LINE = re.compile(r"BeeTimerE2E: (\w+) t=(\d+) ?(.*)$")
HEADER = os.environ.get(
    "E2E_HEADER",
    "Emulator: beetimer-api36 (Android 16, API 36), debug build. Raw runs in build/e2e (not committed); "
    "screenshots and failed-run logs in docs/e2e/latest/.",
)
NAMES = {
    "s1": "Pan wand, wand stays in, 3 hives (60x)",
    "s1x10": "Scenario 1 repeated at 10x",
    "s2": "Pan wand, wand comes out, 3 hives",
    "s3": "Gun type, 3 hives (10x)",
    "s4": "Queue: skip to back, remove, fourth hive",
    "s5": "Cancel the vaporizer holder mid-block",
    "s6": "Alarms: +1 min, Snooze, two close together",
    "s7": "Screen off through a timed step",
    "s8": "Process death mid-run",
    "s9": "Editing while runs are active",
    "s10": "Hive management: cap, drag order, New session",
    "s11": "App open: alarm screen in app, Home, lock, snackbar",
    "s2x10": "Scenario 2 repeated at 10x",
}


def load(s, fs):
    d = os.path.join(RUNS, f"fs{fs}", s)
    if not os.path.isfile(os.path.join(d, "log.txt")) or not os.path.isfile(os.path.join(d, "maestro-exit")):
        return None
    log = open(os.path.join(d, "log.txt"), errors="replace").read().splitlines()
    events = []
    for line in log:
        m = LINE.search(line)
        if m:
            events.append((m.group(1), int(m.group(2)), m.group(3)))
    return {
        "dir": d,
        "exit": open(os.path.join(d, "maestro-exit")).read().strip(),
        "seconds": open(os.path.join(d, "seconds")).read().strip(),
        "invariant": [l for l in log if "BeeTimerInvariant" in l],
        "crash": [l for l in log if "FATAL EXCEPTION" in l and "beetimer" in l],
        "events": events,
        "maestro": open(os.path.join(d, "maestro.txt"), errors="replace").read(),
    }


def kv(rest, key):
    m = re.search(key + r"=(.+?)(?= \w+=|$)", rest)
    return m.group(1) if m else None


def steps(ev, hive=None, step=None, kind="STEP"):
    out = []
    for k, t, rest in ev:
        if k != kind:
            continue
        if hive and kv(rest, "hive") != hive:
            continue
        if step and kv(rest, "step") != step:
            continue
        out.append(t)
    return out


def first(ev, **kw):
    v = steps(ev, **kw)
    return v[0] if v else None


def events_named(ev, prefix):
    return [(t, rest) for k, t, rest in ev if k == "EVENT" and rest.split(" ", 1)[-1].startswith(prefix)]


def lock_transitions_ok(ev):
    prev = "none"
    for k, t, rest in ev:
        if k != "LOCK":
            continue
        holder = kv(rest, "holder").split(" queue")[0]
        if prev != "none" and holder != "none" and holder != prev:
            return False, f"vaporizer moved {prev} -> {holder} without being freed"
        prev = holder
    return True, ""


def check(cond, msg, problems):
    if not cond:
        problems.append(msg)


def scenario_checks(s, r):
    ev = r["events"]
    p = []
    if s in ("s1", "s1x10"):
        l1, l2, l3 = (first(ev, hive=h, step="Load dose") for h in ("Hive 1", "Hive 2", "Hive 3"))
        rel1, rel2 = first(ev, hive="Hive 1", step="Unseal"), first(ev, hive="Hive 2", step="Unseal")
        check(l1 and l2 and l3 and l1 < l2 < l3, "Load dose order is not Hive 1, 2, 3", p)
        check(rel1 and l2 and l2 > rel1, "Hive 2 got the vaporizer before Hive 1's Remove wand Done", p)
        check(rel2 and l3 and l3 > rel2, "Hive 3 got the vaporizer before Hive 2's Remove wand Done", p)
        check(len(steps(ev, kind="WAIT")) >= 2, "Hive 2 and 3 did not wait", p)
        seals = [first(ev, hive=h, step="Seal hive") for h in ("Hive 1", "Hive 2", "Hive 3")]
        check(all(seals) and l2 is not None and max(seals) < l2, "hives did not all seal before the first handoff", p)
        check(len(steps(ev, kind="DONE")) == 3, "not all three hives finished", p)
    if s in ("s2", "s2x10"):
        ks1 = first(ev, hive="Hive 1", step="Keep sealed")
        l2 = first(ev, hive="Hive 2", step="Load dose")
        un1 = first(ev, hive="Hive 1", step="Unseal")
        heat2 = first(ev, hive="Hive 2", step="Heat and vaporize")
        check(ks1 and l2 and l2 > ks1, "Hive 2 got the vaporizer before Hive 1's Remove wand Done", p)
        check(ks1 and heat2 and un1 and ks1 < heat2 < un1, "Hive 1's sealed hold did not overlap Hive 2's heat", p)
        check(len(steps(ev, kind="DONE")) == 3, "not all three hives finished", p)
    if s == "s3":
        ks1 = first(ev, hive="Hive 1", step="Keep sealed")
        l2 = first(ev, hive="Hive 2", step="Load cap and inject")
        ks2 = first(ev, hive="Hive 2", step="Keep sealed")
        un1 = first(ev, hive="Hive 1", step="Unseal")
        check(ks1 and l2 and l2 > ks1, "Hive 2 got the vaporizer before Hive 1's Withdraw Done", p)
        check(ks1 and ks2 and un1 and ks2 < un1, "sealed holds did not overlap", p)
        check(len(steps(ev, kind="DONE")) == 3, "not all three hives finished", p)
    if s == "s4":
        queues = [kv(rest, "queue") for k, t, rest in ev if k == "LOCK"]
        check("[Hive 2, Hive 3]" in queues, "queue never was [Hive 2, Hive 3]", p)
        check("[Hive 3, Hive 2]" in queues, "skip to back did not move Hive 2 behind Hive 3", p)
        check(bool(steps(ev, hive="Hive 3", kind="NOTSTARTED")), "Remove did not reset Hive 3", p)
        check("[Hive 2, Hive 4]" in queues, "the fourth hive did not join the queue behind Hive 2", p)
        check(first(ev, hive="Hive 2", step="Load dose") is not None, "Hive 2 never got the vaporizer", p)
    if s == "s5":
        cancel = events_named(ev, "Cancel")
        ct = cancel[0][0] if cancel else None
        check(ct is not None, "no Cancel event", p)
        if ct:
            after = [(k, t, rest) for k, t, rest in ev if t >= ct]
            check(any(k == "LOCK" and "holder=none" in rest and "Hive 2" in rest for k, t, rest in after), "vaporizer not freed with Hive 2 at the head", p)
            check(not any(k == "RING" and kv(rest, "hive") == "Hive 1" for k, t, rest in after), "an alarm rang for the cancelled hive", p)
            check(not any(k == "EVENT" and "TimerFired(hiveId=1)" in rest for k, t, rest in after), "an orphan alarm fired for the cancelled hive", p)
            check(any(k == "STEP" and kv(rest, "hive") == "Hive 1" and kv(rest, "step") == "Seal hive" for k, t, rest in after), "cancelled hive could not be restarted", p)
    if s == "s6":
        add = events_named(ev, "AddMinute")
        snooze = events_named(ev, "Snooze")
        check(bool(add), "no +1 min", p)
        check(bool(snooze), "no Snooze", p)
        if snooze:
            st = snooze[0][0]
            check(any(t > st and "TimerFired(hiveId=1)" in rest for t, rest in events_named(ev, "TimerFired")), "snoozed alarm did not ring again", p)
        ks1 = first(ev, hive="Hive 1", step="Keep sealed")
        ring1 = [t for t in steps(ev, hive="Hive 1", kind="RING") if ks1 and t > ks1]
        if ks1 and ring1:
            check(ring1[0] - ks1 >= 10_500, f"+1 min did not extend the hold ({(ring1[0]-ks1)/1000:.1f} s)", p)
        r2 = [t for t in steps(ev, hive="Hive 2", kind="RING")]
        r3 = [t for t in steps(ev, hive="Hive 3", kind="RING")]
        d2 = first(ev, hive="Hive 2", kind="DONE")
        check(bool(r3) and d2 is not None and r3[-1] < d2, "Hive 2 and Hive 3 were never ringing at the same time", p)
        check(len(steps(ev, kind="DONE")) == 3, "an alarm was lost (not all hives finished)", p)
    if s == "s7":
        ks = first(ev, hive="Hive 1", step="Keep sealed")
        rings = [t for t in steps(ev, hive="Hive 1", kind="RING") if ks and t > ks]
        check(bool(rings), "no alarm with the screen off", p)
        if ks and rings:
            late = (rings[0] - ks) / 1000 - 10
            check(late < 2, f"alarm {late:.1f} s late", p)
        check("AlarmActivity" in r["maestro"], "full-screen alarm was not in front", p)
        check("mWakefulness=Awake" in r["maestro"].split("screen off at")[-1], "screen did not wake", p)
    if s == "s8":
        m = r["maestro"]
        check("pid after kill: ''" in m, "process was not killed", p)
        check(re.search(r"pid after alarm: '\d+'", m) is not None, "alarm did not restart the process", p)
        kill = re.search(r"killed at (\d+)", m)
        if kill:
            kt = int(kill.group(1))
            after = [(k, t, rest) for k, t, rest in ev if t > kt]
            check(any(k == "EVENT" and rest.endswith("Load") for k, t, rest in after), "state was not reloaded after the kill", p)
            check(any(k == "RING" for k, t, rest in after), "no alarm after the kill", p)
        check(first(ev, hive="Hive 1", kind="DONE") is not None, "run did not finish after restore", p)
    if s == "s6":
        shown = [kv(rest, "hive") for k, t, rest in ev if k == "ALARMSCREEN"]
        check(shown[-2:] == ["Hive 3", "Hive 2"], f"the two close alarms were not shown in turn (alarm screen showed {shown[-4:]})", p)
    if s == "s11":
        m = r["maestro"]
        checks = re.findall(r"CHECK (\S+) (PASS|FAIL)", m)
        check(len(checks) >= 12, f"only {len(checks)} of 12 checks ran", p)
        for name, res in checks:
            check(res == "PASS", f"{name} failed", p)
        for e in ("AddMinute", "Snooze", "Done"):
            check(bool(events_named(ev, e)), f"no {e} from the alarm screen", p)
        sn = events_named(ev, "Snooze")
        if sn:
            check(any(t > sn[0][0] and "TimerFired(hiveId=1)" in rest for t, rest in events_named(ev, "TimerFired")), "snoozed alarm did not ring again", p)
        check(len([1 for k, t, rest in ev if k == "ALARMSCREEN"]) >= 3, "alarm screen was not shown each time", p)
    if s == "s9":
        check(len(events_named(ev, "TemplateChanged")) >= 2, "edits were not applied", p)
    return p


def predicted_vs_actual(s, r):
    ev = r["events"]
    starts = [t for t, rest in events_named(ev, "Start")]
    if len(starts) < 3:
        return None
    t0 = starts[0]
    third = starts[2]
    fc = next((rest for k, t, rest in ev if k == "FORECAST" and t >= third), None)
    if fc is None:
        return None
    scales = [int(re.search(r"scale=(\d+)", rest).group(1)) for k, t, rest in ev if k == "EVENT" and t <= third]
    scale = scales[-1] if scales else 1
    planned = {}
    m = re.search(r"dones=((?:[^@,]+@(?:\d+|null),?)*)\s(.*)$", fc)
    dones = {}
    if m:
        for x in m.group(1).split(","):
            if "@" in x:
                name, val = x.rsplit("@", 1)
                dones[name] = val
        for item in m.group(2).split(";"):
            parts = item.split("|")
            if len(parts) == 4:
                planned.setdefault((parts[0], parts[1]), int(parts[2]))
    rows = []
    for h in ("Hive 1", "Hive 2", "Hive 3"):
        pl = planned.get((h, "Load dose"))
        al = first(ev, hive=h, step="Load dose")
        pd = dones.get(h)
        ad = first(ev, hive=h, kind="DONE")
        rows.append((h, pl, al, int(pd) if pd and pd != "null" else None, ad))
    return t0, scale, rows


def fmt(t, t0, scale):
    if t is None:
        return "n/a"
    return f"{(t - t0) / 1000:.1f} s"


def scales_run():
    if not os.path.isdir(RUNS):
        return []
    return sorted(d[2:] for d in os.listdir(RUNS) if d.startswith("fs"))


def main():
    order = ["s1", "s2", "s3", "s4", "s5", "s6", "s7", "s8", "s9", "s10", "s11", "s1x10", "s2x10"]
    out = ["# End-to-end results", "", HEADER, ""]
    total_inv = 0
    all_passed = True
    results = {}
    for fs in scales_run():
        out += [f"## Font scale {fs}", "", "| Scenario | Result | Invariant errors | Maestro | Time | Problems |",
                "|---|---|---|---|---|---|"]
        for s in order:
            r = load(s, fs)
            if r is None:
                continue
            problems = scenario_checks(s, r)
            ok_lock, why = lock_transitions_ok(r["events"])
            if not ok_lock:
                problems.append(why)
            if r["exit"] != "0":
                problems.append("Maestro flow failed")
            if r["crash"]:
                problems.append("app crashed")
            total_inv += len(r["invariant"])
            passed = not problems and not r["invariant"]
            all_passed = all_passed and passed
            results[(s, fs)] = r
            out.append(
                f"| {s} {NAMES[s]} | {'PASS' if passed else 'FAIL'} | {len(r['invariant'])} | "
                f"{'ok' if r['exit'] == '0' else 'failed'} | {r['seconds']} s | {'; '.join(problems) or '-'} |"
            )
        out.append("")
    out += [f"Invariant errors across all runs: **{total_inv}**.", ""]
    for (s, fs), r in results.items():
        if s not in ("s1", "s1x10", "s2", "s2x10"):
            continue
        pva = predicted_vs_actual(s, r)
        if not pva:
            continue
        t0, scale, rows = pva
        out += [f"## Planner predicted vs actual: {s} ({NAMES[s]}, {scale}x, font {fs})", "",
                "Forecast taken right after the third Start; real seconds from the first Start.", "",
                "| Hive | Gets vaporizer: predicted | actual | Finishes: predicted | actual |", "|---|---|---|---|---|"]
        for h, pl, al, pd, ad in rows:
            out.append(f"| {h} | {fmt(pl, t0, scale)} | {fmt(al, t0, scale)} | {fmt(pd, t0, scale)} | {fmt(ad, t0, scale)} |")
        out.append("")
    fixes = os.path.join(ROOT, "FIXES.md")
    if os.path.isfile(fixes):
        out += ["", open(fixes).read()]
    open(os.path.join(ROOT, "REPORT.md"), "w").write("\n".join(out) + "\n")
    print("\n".join(out))
    return 0 if results and all_passed else 1


if __name__ == "__main__":
    sys.exit(main())

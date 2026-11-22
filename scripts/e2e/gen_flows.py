#!/usr/bin/env python3
"""Generates the Maestro flows in scripts/e2e/flows/ for the end-to-end scenarios.

Run: python3 scripts/e2e/gen_flows.py   (then scripts/e2e/run.sh s1 s2 ...)
Hives are the fresh-install defaults "Hive 1", "Hive 2", "Hive 3". Times assume the debug
time scale chosen in setup (60x: Heat 2.5 s, Cool 2 s, Keep sealed 10 s, Vaporize 0.33 s).
"""
import os
import yaml

APP = "io.github.mecheng70.beetimer"
OUT = os.path.join(os.path.dirname(__file__), "flows")
H1, H2, H3, H4 = "Hive 1", "Hive 2", "Hive 3", "Hive 4"
WAIT = 30000  # set per scenario in scaled(); the longest step (Keep sealed) is 600 s / scale
HEADSUP_TEXT = "Move the vaporizer, then tap Start."


def scaled(scale):
    global WAIT
    WAIT = max(30000, 600000 // scale + 30000)


def exact(text):
    return "^" + text.replace("+", "\\+") + "$"


def setup(preset="oav-pan-wand", scale=60, hold=None):
    steps = [
        "launchApp",
        {"tapOn": {"text": "Continue", "optional": True}},
        {"tapOn": "Settings"},
        {"tapOn": {"id": f"preset-{preset}"}},
    ]
    if hold:
        steps.append({"tapOn": {"id": f"hold-{hold}"}})
    steps += [
        {"scrollUntilVisible": {"element": {"id": f"dev-scale-{scale}"}, "direction": "DOWN"}},
        {"tapOn": {"id": f"dev-scale-{scale}"}},
        "back",
        "waitForAnimationToEnd",
    ]
    return steps


def tap(hive):
    return [{"tapOn": {"id": f"row-action-{hive}"}}]


def wait(hive, text, timeout=None):
    return [{"extendedWaitUntil": {"visible": {"text": exact(text), "childOf": {"id": f"row-{hive}"}}, "timeout": timeout or WAIT}}]


def wait_action(hive, text, timeout=None):
    return [{"extendedWaitUntil": {"visible": {"text": exact(text), "childOf": {"id": f"row-action-{hive}"}}, "timeout": timeout or WAIT}}]


def see(hive, text):
    return [{"assertVisible": {"text": exact(text), "childOf": {"id": f"row-{hive}"}}}]


def see_text(text, timeout=None):
    return [{"extendedWaitUntil": {"visible": {"text": exact(text)}, "timeout": timeout or WAIT}}]


def headsup_gone():
    """The "Vaporizer free" heads-up covers the top of the screen (and the bar's buttons) for a few seconds."""
    return [{"extendedWaitUntil": {"notVisible": {"text": exact(HEADSUP_TEXT)}, "timeout": 20000}}]


def shot(name):
    return [{"takeScreenshot": name}]


def open_tab(hive):
    return [{"tapOn": {"id": f"row-{hive}"}}, "waitForAnimationToEnd"]


def all_tab():
    return [{"tapOn": {"text": "^All$"}}, "waitForAnimationToEnd"]


def tag(t):
    return [{"tapOn": {"id": t}}]


def alarm(hive, timeout=None):
    """The alarm screen is showing this hive (opened in the app, or switched to it)."""
    return [{"extendedWaitUntil": {"visible": {"id": "alarm-hive", "text": exact(hive)}, "timeout": timeout or WAIT}}]


def ack(hive):
    """Wait for this hive's alarm screen and tap Done."""
    return alarm(hive) + [{"tapOn": {"id": "alarm-done"}}, "waitForAnimationToEnd"]


def pause(ms):
    """Maestro has no sleep: wait for an element that never exists, and ignore the timeout."""
    return [{"extendedWaitUntil": {"visible": {"id": "never-present"}, "timeout": ms, "optional": True}}]


def autopilot(hives, last):
    """Do whatever comes up until the last hive is Done: Done on the alarm screen, Start or Done
    on rows. The scheduler rules are checked from the event log by analyze.py."""
    cmds = [
        {"runFlow": {"when": {"visible": {"id": "alarm-done"}}, "commands": [{"tapOn": {"id": "alarm-done"}}, "waitForAnimationToEnd"]}},
        # Back to the All tab if a tap opened a hive's tab.
        {"runFlow": {"when": {"visible": {"text": "^All$"}}, "commands": [{"tapOn": {"text": "^All$"}}]}},
    ]
    for h in hives:
        # Skip finished hives: their "Done" is a status chip, and tapping it opens the hive's tab.
        cmds.append({"runFlow": {
            "when": {"notVisible": {"id": f"row-sub-{h}", "text": "^Done$"}},
            "commands": [{"runFlow": {
                "when": {"visible": {"text": "^(Start|Done)$", "childOf": {"id": f"row-action-{h}"}}},
                "commands": [{"tapOn": {"id": f"row-action-{h}"}}, "waitForAnimationToEnd"],
            }}],
        }})
    # A 0.5 s pause per pass keeps the loop from spinning; 3000 passes (about 25 min) is only a
    # safety cap. (The first version capped at 600 passes, which ran out in about 90 s.)
    cmds += pause(500)
    return [{"repeat": {"times": 3000, "while": {"notVisible": {"id": f"row-sub-{last}", "text": "^Done$"}}, "commands": cmds}}]


def start_and_seal(hives):
    s = []
    for h in hives:
        s += tap(h)  # Start
    for h in hives:
        s += wait(h, "Seal hive") + tap(h)  # Done on Seal hive
    return s


def pan_stays_in_block(h, others_waiting=()):
    """Holder h: Load dose -> Heat -> Cool -> Keep sealed (holds) -> Remove wand -> Unseal.
    Each timed step's end opens the alarm screen in the app."""
    s = wait(h, "Load dose") + tap(h)
    s += ack(h)  # Heat and vaporize finished -> Cool starts on Done
    s += ack(h)  # Cool in hive finished -> Keep sealed starts on Done
    s += wait(h, "Keep sealed")
    for o in others_waiting:
        s += see(o, "Waiting")  # the hold keeps the vaporizer
    s += ack(h)  # Keep sealed finished; Done completes Remove wand and frees the vaporizer
    s += wait(h, "Unseal") + tap(h)
    s += wait(h, "Done")
    return s


def pan_comes_out_block(h):
    """Holder h up to releasing: Load dose -> Heat (alarm) -> Cool (alarm; the run is on
    Remove wand) -> Done frees the vaporizer -> Keep sealed."""
    s = wait(h, "Load dose") + tap(h)
    s += ack(h)  # Heat finished -> Cool on Done
    s += ack(h)  # Cool finished; Done completes Remove wand
    s += wait(h, "Keep sealed")
    return s


def gun_block(h):
    s = wait(h, "Load cap and inject") + tap(h)
    s += wait(h, "Withdraw") + tap(h)  # Vaporize ends -> Withdraw (manual, releases)
    s += wait(h, "Keep sealed")
    return s


def handoff(h):
    return wait_action(h, "Start") + tap(h)


def finish_unseal(h):
    return wait(h, "Unseal") + tap(h) + wait(h, "Done")


def flow(name, steps):
    os.makedirs(OUT, exist_ok=True)
    with open(os.path.join(OUT, f"{name}.yaml"), "w") as f:
        f.write(f"# Generated by scripts/e2e/gen_flows.py. Do not edit by hand.\nappId: {APP}\n---\n")
        yaml.safe_dump(steps, f, sort_keys=False, allow_unicode=True, width=200)


def scenario1(scale):
    scaled(scale)
    s = setup(scale=scale, hold="stays_in")
    s += start_and_seal([H1, H2, H3])
    s += wait(H2, "Waiting") + wait(H3, "Waiting") + shot("s1-01-one-holder-two-waiting")
    s += pan_stays_in_block(H1, others_waiting=[H2, H3])
    s += shot("s1-02-handoff-offered")
    s += handoff(H2) + pan_stays_in_block(H2, others_waiting=[H3])
    s += handoff(H3) + pan_stays_in_block(H3)
    s += shot("s1-03-all-done")
    return s


def scenario2(scale=60):
    scaled(scale)
    s = setup(scale=scale, hold="comes_out")
    s += start_and_seal([H1, H2, H3]) + shot("s2-01-sealed")
    s += autopilot([H1, H2, H3], last=H3)
    s += see(H1, "Done") + see(H2, "Done") + see(H3, "Done") + shot("s2-02-all-done")
    return s


def scenario3():
    # 10x: at 60x the 10 s hold is shorter than the next hive's block (5 s exact-alarm floor),
    # so the two holds cannot overlap; at 10x (60 s hold) they do, as at real timing.
    scaled(10)
    s = setup(preset="oav-gun-type", scale=10)
    s += start_and_seal([H1, H2, H3]) + shot("s3-01-sealed")
    s += autopilot([H1, H2, H3], last=H3)
    s += see(H1, "Done") + see(H2, "Done") + see(H3, "Done") + shot("s3-02-all-done")
    return s


def scenario4():
    s = setup(hold="stays_in")
    s += start_and_seal([H1, H2, H3])
    s += wait(H2, "Waiting") + wait(H3, "Waiting")
    # Free the vaporizer by skipping Hive 1 through its device block.
    s += open_tab(H1)
    for _ in range(5):  # Load dose, Heat, Cool, Keep sealed, Remove wand
        s += tag("hive-skip") + ["waitForAnimationToEnd"]
    s += all_tab()
    s += see_text("Vaporizer free: Hive 2 is next") + shot("s4-01-head-offered")
    s += headsup_gone() + tag("bar-skip") + see_text("Vaporizer free: Hive 3 is next") + shot("s4-02-skipped-to-back")
    s += headsup_gone() + tag("bar-remove") + see_text("Vaporizer free: Hive 2 is next") + see(H3, "Not started") + shot("s4-03-removed")
    # A fourth hive mid-session. Remove posted a new "Vaporizer free" heads-up over the gear.
    s += headsup_gone() + [{"tapOn": "Settings"}, {"scrollUntilVisible": {"element": {"id": "add-hive"}, "direction": "DOWN"}}]
    s += tag("add-hive") + [{"tapOn": "Save"}, "back", "waitForAnimationToEnd"]
    s += tap(H4) + wait(H4, "Seal hive") + tap(H4) + wait(H4, "Waiting")
    s += see_text("Vaporizer free: Hive 2 is next") + shot("s4-04-fourth-hive-queued")
    s += handoff(H2) + wait(H2, "Load dose") + see(H4, "Waiting")
    return s


def scenario5():
    s = setup(hold="stays_in")
    s += start_and_seal([H1, H2])
    s += wait(H1, "Load dose") + tap(H1) + wait(H2, "Waiting")
    s += open_tab(H1) + tag("hive-cancel") + all_tab()  # cancel mid-Heat, alarm pending
    s += see(H1, "Not started") + see_text("Vaporizer free: Hive 2 is next") + shot("s5-01-cancelled-holder")
    s += [{"waitForAnimationToEnd": {"timeout": 5000}}, {"assertNotVisible": {"text": exact("Heat and vaporize finished")}}]
    s += handoff(H2) + wait(H2, "Load dose")
    s += tap(H1) + wait(H1, "Seal hive") + shot("s5-02-restarted")  # restart works
    return s


def scenario6():
    s = setup(hold="comes_out")
    s += start_and_seal([H1, H2, H3])
    # Hive 1 alone: +1 min on the running hold, then Snooze its alarm, then Done.
    s += pan_comes_out_block(H1)
    s += open_tab(H1) + tag("hive-add-minute") + all_tab()
    s += alarm(H1) + [{"tapOn": {"id": "alarm-snooze"}}] + pause(7000) + alarm(H1)
    s += [{"tapOn": {"id": "alarm-done"}}, "waitForAnimationToEnd"] + wait(H1, "Done")
    # Hive 2 then Hive 3: Hive 2's hold ends while Hive 3 works; Hive 2 is left ringing until
    # Hive 3 rings too, then both are cleared in turn on the alarm screen.
    s += handoff(H2) + pan_comes_out_block(H2)
    s += handoff(H3) + wait(H3, "Load dose") + tap(H3)
    s += alarm(H3) + [{"tapOn": {"id": "alarm-done"}}]  # Heat finished
    s += alarm(H3) + [{"tapOn": {"id": "alarm-done"}}]  # Cool finished; completes Remove wand
    s += alarm(H3) + shot("s6-01-two-alarms-ringing")   # Hive 3's hold ends; Hive 2 still ringing
    s += [{"tapOn": {"id": "alarm-done"}}] + alarm(H2) + shot("s6-02-second-alarm-shown")
    s += [{"tapOn": {"id": "alarm-done"}}, "waitForAnimationToEnd"]
    s += wait(H2, "Done") + wait(H3, "Done")
    return s


def scenario7_parts():
    a = setup(hold="stays_in") + start_and_seal([H1])
    a += wait(H1, "Load dose") + tap(H1) + ack(H1) + ack(H1) + wait(H1, "Keep sealed")
    b = [{"extendedWaitUntil": {"visible": {"id": "alarm-done"}, "timeout": WAIT}}, {"takeScreenshot": "s7-01-alarm-over-lock"}, {"tapOn": {"id": "alarm-done"}}]
    return a, b


def scenario8_parts():
    a = setup(hold="comes_out") + start_and_seal([H1])
    a += pan_comes_out_block(H1)
    b = ["launchApp", "waitForAnimationToEnd"] + wait(H1, "Unseal") + shot("s8-01-restored-after-kill") + tap(H1) + wait(H1, "Done")
    return a, b


def scenario9():
    s = setup(scale=60, hold="stays_in")
    s += start_and_seal([H1]) + wait(H1, "Load dose")  # Hive 1 snapshots "wand stays in"
    s += [{"tapOn": "Settings"}, {"tapOn": "Move Remove wand up"}, "waitForAnimationToEnd"]
    # Keep sealed is now below the release: Passive, divider moved, choice reads "comes out".
    s += [{"assertVisible": {"text": "^Passive$", "childOf": {"id": "step-chip-5"}}}]
    s += [{"assertVisible": {"id": "frees-here", "below": {"id": "step-4"}}}]
    s += shot("s9-01-remove-wand-above-keep-sealed")
    s += [{"tapOn": {"id": "step-2"}}, {"tapOn": {"text": "Seconds"}}, {"eraseText": 10}, {"inputText": "300"}, {"tapOn": "Save"}]
    s += [{"assertVisible": "Timed, 5:00"}, "back", "waitForAnimationToEnd"]
    s += tap(H2) + wait(H2, "Seal hive") + tap(H2) + wait(H2, "Waiting")  # Hive 2 snapshots the edit
    s += open_tab(H2) + [{"assertVisible": "Heat and vaporize, 5:00"}] + shot("s9-02-new-run-uses-edit")
    s += all_tab() + open_tab(H1) + [{"assertVisible": "Heat and vaporize, 2:30"}] + shot("s9-03-active-run-keeps-snapshot")
    s += all_tab()
    # Switch the sealed-hold option back (edited, so it asks).
    s += [{"tapOn": "Settings"}, {"tapOn": {"id": "hold-stays_in"}}, {"tapOn": "Replace"}, "waitForAnimationToEnd"]
    s += [{"assertVisible": {"text": "^Device$", "childOf": {"id": "step-chip-4"}}}]
    # Release above take is rejected.
    for _ in range(5):
        s += [{"tapOn": "Move Remove wand up"}]
    s += [{"assertVisible": {"id": "release-error"}}] + shot("s9-04-release-above-take-rejected")
    s += [{"tapOn": "Undo"}, {"assertNotVisible": {"id": "release-error"}}, "back"]
    return s


def scenario10_parts():
    a = setup(hold="stays_in")
    a += [{"tapOn": "Settings"}, {"scrollUntilVisible": {"element": {"id": "add-hive"}, "direction": "DOWN"}}]
    a += tag("add-hive") + [{"tapOn": "Save"}] + tag("add-hive") + [{"tapOn": "Save"}]
    a += [{"assertVisible": "5 hives max per session"}, {"assertNotVisible": {"id": "add-hive"}}] + shot("s10-01-five-hive-cap")
    a += [{"tapOn": {"id": "hive-row-Hive 1"}}]  # the shell part renames it to Emory
    a2 = ["back", "waitForAnimationToEnd"] + see_text("Next: Emory") + shot("s10-02-renamed")
    b = see_text("Next: Hive 3") + shot("s10-03-next-follows-drag-order")
    b += tap("Emory") + wait("Emory", "Seal hive")
    b += [{"tapOn": "Settings"}, {"scrollUntilVisible": {"element": {"id": "new-session"}, "direction": "DOWN"}}]
    b += tag("new-session") + tag("confirm-new-session") + ["back", "waitForAnimationToEnd"]
    b += see("Emory", "Not started") + see(H3, "Not started") + shot("s10-04-new-session-keeps-names")
    return a, a2, b


def scenario11_parts():
    """App open and unlocked when steps end: the alarm screen opens in the app, nothing covers
    the top; Home and lock keep it ringing; the notification and waking bring it back."""
    a = setup(hold="comes_out") + start_and_seal([H1, H2])
    a += wait(H1, "Load dose") + tap(H1)
    b = alarm(H1) + [{"assertVisible": "Heat and vaporize finished"}] + shot("s11-01-alarm-opened-in-app")
    c = alarm(H1) + [{"tapOn": {"id": "alarm-add-minute"}}, "waitForAnimationToEnd", {"launchApp": {"stopApp": False}}]  # after the notification tap
    c += alarm(H1) + shot("s11-02-reopened-after-plus-minute") + [{"tapOn": {"id": "alarm-snooze"}}]
    d = alarm(H1) + [{"tapOn": {"id": "alarm-done"}}, "waitForAnimationToEnd"]  # after waking from lock
    e = ack(H1)  # Cool finished; Done completes Remove wand and frees the vaporizer
    e += see_text("Vaporizer free: Hive 2 is next") + shot("s11-03-snackbar-at-bottom")
    return {"s11a": a, "s11b": b, "s11c": c, "s11d": d, "s11e": e}


def rename(old, new):
    return [{"tapOn": {"id": f"hive-row-{old}"}}, {"tapOn": {"text": "Name"}}, {"eraseText": 12}, {"inputText": new}, {"tapOn": "Save"}]


def screens_parts():
    """docs/screenshots scene at real time (1x): Herb heating, Sparky queued, Emory not started."""
    a = ["launchApp", {"tapOn": {"text": "Continue", "optional": True}}, {"tapOn": "Settings"}]
    a += [{"scrollUntilVisible": {"element": {"id": "add-hive"}, "direction": "DOWN"}}]
    a0 = a
    a = ["back", "waitForAnimationToEnd"]  # the shell renames Hive 1-3 between scr-a0 and scr-a
    a += tap("Herb") + wait("Herb", "Seal hive") + tap("Herb") + wait("Herb", "Load dose") + tap("Herb")
    a += tap("Sparky") + wait("Sparky", "Seal hive") + tap("Sparky") + wait("Sparky", "Waiting")
    hive_tab = open_tab("Herb")
    settings = all_tab() + [{"tapOn": "Settings"}, "waitForAnimationToEnd"]
    hold_out = [{"tapOn": {"id": "hold-comes_out"}}, "waitForAnimationToEnd"]
    hold_in = [{"tapOn": {"id": "hold-stays_in"}}, "waitForAnimationToEnd", {"scrollUntilVisible": {"element": {"id": "add-hive"}, "direction": "DOWN"}}]
    back_all = ["back", "waitForAnimationToEnd"]
    alarm = [{"extendedWaitUntil": {"visible": {"id": "alarm-done"}, "timeout": 20000}}, "waitForAnimationToEnd"]
    # All tab scenes (fresh app data, real time, wand comes out). scr-h: Herb in Cool in hive with
    # "Next: Remove wand", Sparky queued. scr-h2: Herb in Keep sealed (passive, teal "Next: Unseal"), and
    # Sparky takes the vaporizer and sits on Load dose with "Next: Heat and vaporize".
    teal0 = ["launchApp", {"tapOn": {"text": "Continue", "optional": True}}, {"tapOn": "Settings"},
             {"tapOn": {"id": "hold-comes_out"}}, "waitForAnimationToEnd",
             {"scrollUntilVisible": {"element": {"id": "add-hive"}, "direction": "DOWN"}}]
    cool = ["back", "waitForAnimationToEnd"]
    cool += tap("Herb") + wait("Herb", "Seal hive") + tap("Herb") + wait("Herb", "Load dose") + tap("Herb")
    cool += tap("Sparky") + wait("Sparky", "Seal hive") + tap("Sparky") + wait("Sparky", "Waiting")
    cool += ack("Herb") + wait("Herb", "Cool in hive")  # Heat finished; Cool starts on Done
    cool += pause(3000)
    teal = ack("Herb") + wait("Herb", "Keep sealed")  # Cool finished; Done completes Remove wand
    teal += handoff("Sparky") + wait("Sparky", "Load dose")  # Sparky sealed while queued, so Start goes to Load dose
    teal += pause(12000)  # let the "Vaporizer free" snackbar time out
    return {"scr-a0": a0, "scr-a": a, "scr-b": hive_tab, "scr-c": settings, "scr-d": hold_out, "scr-e": hold_in, "scr-f": back_all,
            "scr-g": alarm, "scr-h0": teal0, "scr-h": cool, "scr-h2": teal}


def main():
    scaled(1)
    for name, steps in screens_parts().items():
        flow(name, steps)
    flow("s1x10", scenario1(10))
    flow("s1", scenario1(60))
    flow("s2", scenario2(60))
    flow("s3", scenario3())
    flow("s2x10", scenario2(10))
    scaled(60)
    for name, steps in scenario11_parts().items():
        flow(name, steps)
    flow("s4", scenario4())
    flow("s5", scenario5())
    flow("s6", scenario6())
    a, b = scenario7_parts()
    flow("s7a", a)
    flow("s7b", b)
    a, b = scenario8_parts()
    flow("s8a", a)
    flow("s8b", b)
    flow("s9", scenario9())
    a, a2, b = scenario10_parts()
    flow("s10a", a)
    flow("s10a2", a2)
    flow("s10b", b)


if __name__ == "__main__":
    main()

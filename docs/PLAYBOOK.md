New App Playbook (Claude chat + Claude Code)

How to take an app idea from a conversation to a signed APK, the way Pit Planner was built. Replace everything in <angle brackets>.

0. Before any code: design it in Claude chat

Talk it through in a regular Claude chat first. Code is the expensive part, so settle these before Claude Code ever starts:

Who uses it, on what phone, and what the one job is (Owner: Jason, due: YYYY-MM-DD)
Screen list and what each screen does (Owner: Jason, due: YYYY-MM-DD)
Mockups for every screen, exported as PNGs, at 100%, 115% and 130% text size (Owner: Jason, due: YYYY-MM-DD)
Ask Claude to critique the mockups and the flow, then fix what it finds (Owner: Jason, due: YYYY-MM-DD)
Decisions table: app ID, distribution, units, anything that's permanent (Owner: Jason, due: YYYY-MM-DD)
Failure-mode table (FMEA style): what breaks, what the user sees, what the app does (Owner: Jason, due: YYYY-MM-DD)
App name, icon and splash (Owner: Jason, due: YYYY-MM-DD)

Ask Claude chat for these files at the end:

SPEC.md: what the app does, screens, rules, decisions table, failure modes, definition of done
CLAUDE.md: house rules for Claude Code (start from section 6 below)
docs/mockups/: the PNG exports
assets/: icon and splash files
Any data files the app needs (like data/foods.json), with a cited source for every safety-related number

Permanent decisions. The app ID (io.github.mecheng70.<appname>) can never change once anyone installs the app. Pick it on day one.

1. Standard stack

Expo + React Native + TypeScript + NativeWind + React Native Reusables + Expo Router + expo-sqlite + expo-secure-store.

No server, so no Redis.
Native features go in local Expo modules (Kotlin), behind TypeScript interfaces, so iOS can be added later.
Tests: Jest for logic and UI, Robolectric/JUnit for native modules, Maestro for device tests.
2. Machine setup (one time, Fedora)
# Java 21
sudo dnf install temurin-21-jdk
echo 'export JAVA_HOME=/usr/lib/jvm/temurin-21-jdk' >> ~/.zshrc

# Maestro (device tests)
curl -fsSL "https://get.maestro.mobile.dev" | bash
echo 'export MAESTRO_CLI_NO_ANALYTICS=1' >> ~/.zshrc
echo 'export MAESTRO_CLI_ANALYSIS_NOTIFICATION_DISABLED=true' >> ~/.zshrc
source ~/.zshrc

# Check KVM for the emulator
ls -l /dev/kvm

Android SDK, the emulator and a system image: let Claude Code install these with the SDK manager in the first task, and have it check KVM first.

Security tools: Snyk CLI (logged in, org ID in your Snyk config, never in the repo) and Semgrep.

3. GitHub repo
Create MechEng70/<appname> on GitHub (public is fine if no secrets ever go in) (Owner: Jason, due: YYYY-MM-DD)
Clone to ~/Documents/git/<appname> (Owner: Jason, due: YYYY-MM-DD)
Copy in SPEC.md, CLAUDE.md, docs/mockups/, assets/ and data files, then commit and push (Owner: Jason, due: YYYY-MM-DD)
Add .gitignore entries for *.jks, *.keystore, keystore.properties, release/, device test output (Owner: Jason, due: YYYY-MM-DD)
4. Claude Code: the planning prompt (first session)

Paste this as the first message in Claude Code, in the repo folder:

Read SPEC.md, CLAUDE.md, docs/mockups/ and any data files. Before writing any app code, produce three documents for my review, then stop:

1. docs/design/TECH-DESIGN.md: architecture, layers and what each may import, data model and schema, native modules, security, test strategy, and risks.
2. docs/design/TASKS.md: numbered tasks (T-001, T-002...) grouped in phases. Each task has Goal, Files, Depends on, Acceptance (pass/fail), Tests, and "Device testing (Jason)" that says exactly what I judge and what I compare it against, or "none".
3. docs/design/WBS.md: the work breakdown by phase.

Also create docs/design/SECURITY-EXCEPTIONS.md (empty table) and docs/HANDOFF.md.

Stack: Expo, React Native, TypeScript, NativeWind, React Native Reusables, Expo Router, expo-sqlite, expo-secure-store. Native code in local Expo modules behind TypeScript interfaces.

Quality gate after every task (npm run check): Prettier, ESLint with zero warnings, TypeScript typecheck, unit and regression tests, Semgrep (typescript, kotlin, secrets), Snyk open source and code at high+. Every bug fix gets a regression test that fails before the fix.

Early tasks should be: scaffold, quality gate, device test tooling (Maestro on an emulator), release build and signing.

Review the three documents in Claude chat before approving. Ask it to critique them.

5. Claude Code: the task loop

Start or restart a session (fresh sessions are much cheaper than resuming):

Read CLAUDE.md, docs/HANDOFF.md and docs/design/TASKS.md. Run git status and git log -5 and tell me what's committed and uncommitted. Then start T-<NNN> and stop after it for my review.

After each task report:

Paste the report into Claude chat for a review.
Look at every screenshot it names (or have Claude chat look).
Approve with follow-ups, and end with: Then start T-<NNN> and stop after it.

Approval template:

T-<NNN> approved. Before T-<next>:
1. <follow-up>
2. <follow-up>
Then start T-<next> and stop after it.

If a session is canceled or hits a limit:

Read CLAUDE.md, docs/HANDOFF.md and docs/design/TASKS.md. Last session stopped partway. Run git status and git log -5, tell me what's committed and what isn't, and check uncommitted work before building on it. Then finish T-<NNN> and stop.
6. CLAUDE.md rules (learned the hard way)

Paste these into every new project's CLAUDE.md:

## Working rules
- One task at a time. Stop after each task for review. Never start the next task unasked.
- Uninstalling apps, deleting data, or any other destructive action on a device or in the repo needs an explicit yes from Jason. Don't read consent into an unclear reply.
- No UI element that isn't in docs/mockups or SPEC without asking Jason first. Mockups win over any older version of the app unless SPEC says otherwise.
- "Device testing for Jason" must say exactly what to judge and what to compare it against.
- Every bug fix gets a regression test that fails before the fix.
- Never report a check as passing without having run it. Say what wasn't verified.

## Security
- Never commit tokens, keys, keystores or keystore.properties. Never print secrets.
- No high or critical finding left open without an entry in docs/design/SECURITY-EXCEPTIONS.md with scope and an expiry date.
- Any security scanner skip must fail the gate if it scans fewer projects than exist.
- Device tests: clear logcat (adb logcat -c) before and after each run. Never save logcat unless a test fails, and then only with token patterns redacted.
- Device screenshots always crop off the status bar.

## Devices
- The emulator is the default for device tests. Use the real phone only for tasks marked Real phone (alarms in Doze, lock screen, OEM battery, Bluetooth, real upgrades).
- Stop Gradle and Kotlin daemons before booting the emulator. Never run a release build with the emulator up.
- Device tests run at 115% and 130% font scale and put every phone setting back afterward.

## Data
- Shipped database migrations are frozen (checksum test). Changes go in new migrations only.
- Corrupt data never crashes a read, and is quarantined (kept for export) before anything overwrites it.
- Every food-safety or medical-safety number cites a primary source.

## Layout
- Changing a value never moves the control being tapped or anything above it: tabular digits, fixed-height rows for computed values, warnings below the control, reserve bold label widths, no auto-scroll.
6b. Models and parallel agents (add to CLAUDE.md)
## Models and agents
- Main session: the strongest model available (Fable when included). It plans, merges, runs the gate and talks to Jason.
- Agents: pick the cheapest model that can do the job well.
  - Fable: design judgment, tricky native or debugging work, security decisions.
  - Opus: implementation from a clear spec, writing tests, refactors, code review.
  - Sonnet or Haiku: mechanical work (dashboard updates, doc edits, log scraping, renames).
- Run independent tasks in parallel, each in its own worktree off local main: no emulator, no gate in the worktree, stop with a report.
- Never run two agents that edit the same file. Sequence those.
- Device work is serial: one emulator job at a time (RAM limit). The main session owns the emulators and the phone.
- Merge one branch at a time, run the gate after each merge, then delete the worktree and branch.
- At most 5 agents at once. Stack UI reviews for Jason into one list.
- If the Fable allowance passes 80%, move agents to Opus first, then the main session.
7. Release and signing
Generate the release keystore once. Back up the .jks file and its passwords in the password manager, never the repo (Owner: Jason, due: YYYY-MM-DD)
chmod 600 the keystore and keystore.properties (Owner: Jason, due: YYYY-MM-DD)
versionCode scheme from day one: (major*10000 + minor*100 + patch) * 1000 + stage, with alpha.N = N, beta.N = 300+N, rc.N = 600+N, final = 999 (Owner: Jason, due: YYYY-MM-DD)
Release script: runs the gate, builds, signs, checks the certificate fingerprint, app ID and versionCode, and fails if the signing key is missing (Owner: Jason, due: YYYY-MM-DD)
Distribute through GitHub Releases. Test every upgrade on the emulator before it goes to anyone's phone (Owner: Jason, due: YYYY-MM-DD)

If the keystore is lost, nobody's installed copy can ever be updated again. Back it up before the first APK leaves the laptop.

8. Usage tips (Claude Pro)
Use a fresh session per task or two (/clear), not --resume on a long one. Long history burns the 5-hour window fast.
Don't switch models mid-task. Switch between tasks only.
Don't start a big task above about 80% of the 5-hour window. Wait for the reset.
If it stops mid-device-test: check phone font size and notification settings, and kill a leftover emulator with adb emu kill.
Decline usage-credit prompts. Keep auto-reload off.
An ANTHROPIC_API_KEY environment variable makes Claude Code bill the API instead of the subscription. Check /status shows the Pro login.
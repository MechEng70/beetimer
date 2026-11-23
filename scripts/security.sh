#!/usr/bin/env bash
# Security scanners for the gate (R11 T-007). Usage: scripts/security.sh semgrep | snyk-oss | snyk-code
# Each fails (exit 1) on any finding at the threshold, and also when the scanner is missing, not logged in,
# errors out, or scans less than exists: Semgrep must scan Kotlin in every module, Snyk open source must test
# the root plus every Gradle project in settings.gradle.kts, and Snyk Code must cover every tracked .kt file.
# Network errors get one retry. Open findings that can't be fixed go in docs/design/SECURITY-EXCEPTIONS.md.
set -uo pipefail
cd "$(dirname "$0")/.."

out=build/security
mkdir -p "$out"

die() {
    echo "security: $*" >&2
    exit 1
}

need() {
    command -v "$1" >/dev/null 2>&1 || die "$1 is not installed (see docs/DEVELOPING.md)"
}

# Gradle projects: the root plus every include(...) entry.
expected_projects() {
    local n
    n=$(grep -oE '"(:[A-Za-z0-9_-]+)"' settings.gradle.kts | sort -u | wc -l)
    # buildSrc is its own Gradle build (R11 T-014); --all-projects doesn't see it, so it is scanned separately.
    [ -f buildSrc/build.gradle.kts ] && n=$((n + 1))
    echo $((n + 1))
}

semgrep_scan() {
    need semgrep
    semgrep scan --config p/kotlin --config p/secrets --metrics=off --error --quiet \
        --exclude build --exclude .gradle --json -o "$out/semgrep.json" .
    local rc=$?
    [ -s "$out/semgrep.json" ] || die "semgrep wrote no results (exit $rc)"
    python3 - "$out/semgrep.json" <<'EOF' || exit 1
import json, sys
d = json.load(open(sys.argv[1]))
scanned = d["paths"]["scanned"]
for r in d["results"]:
    print(f'{r["path"]}:{r["start"]["line"]}: {r["check_id"]} ({r["extra"]["severity"]})')
missing = [m for m in ("app/", "scheduler/") if not any(p.startswith(m) and p.endswith(".kt") for p in scanned)]
if d["errors"]:
    print(f"semgrep errors: {len(d['errors'])}"); sys.exit(1)
if missing:
    print(f"semgrep scanned no Kotlin in: {missing}"); sys.exit(1)
print(f"semgrep: {len(d['results'])} findings, {len(scanned)} files scanned")
sys.exit(1 if d["results"] else 0)
EOF
    [ "$rc" -eq 0 ] || die "semgrep exit $rc"
}

snyk_logged_in() {
    need snyk
    snyk whoami >/dev/null 2>&1 || die "Snyk is not logged in: run 'snyk auth' (hard stop 3 for Jason)"
}

# One snyk test run with a retry: snyk_oss_run <name> <snyk args...>. Writes $out/<name>.json; exit 0 or 1 (found).
snyk_oss_run() {
    local name=$1 rc=2 try
    shift
    for try in 1 2; do
        snyk test "$@" --severity-threshold=high --json >"$out/$name.json" 2>"$out/$name.err"
        rc=$?
        [ "$rc" -le 1 ] && break
        echo "snyk test $* exit $rc (try $try): $(tail -3 "$out/$name.err")" >&2
    done
    [ "$rc" -le 1 ] || die "snyk test $* failed (exit $rc)"
    return "$rc"
}

snyk_oss() {
    snyk_logged_in
    local rc=0 files=("$out/snyk-oss.json")
    snyk_oss_run snyk-oss --all-projects || rc=1
    if [ -f buildSrc/build.gradle.kts ]; then
        snyk_oss_run snyk-oss-buildsrc --file=buildSrc/build.gradle.kts || rc=1
        files+=("$out/snyk-oss-buildsrc.json")
    fi
    python3 - "$(expected_projects)" "$rc" "${files[@]}" <<'EOF' || exit 1
import json, sys
expected, rc = int(sys.argv[1]), int(sys.argv[2])
d = []
for f in sys.argv[3:]:
    j = json.load(open(f))
    d += j if isinstance(j, list) else [j]
vulns = 0
for p in d:
    for v in p.get("vulnerabilities", []):
        vulns += 1
        print(f'{p.get("displayTargetFile")}: {v["severity"]} {v["packageName"]}@{v["version"]} {v["title"]}')
print(f"snyk open source: {len(d)} projects tested (expected {expected}), {vulns} high or critical")
if len(d) < expected:
    print("snyk tested fewer projects than exist"); sys.exit(1)
sys.exit(1 if vulns or rc else 0)
EOF
}

snyk_code() {
    snyk_logged_in
    local rc=2 try
    for try in 1 2; do
        snyk code test --severity-threshold=high --json >"$out/snyk-code.json" 2>"$out/snyk-code.err"
        rc=$?
        [ "$rc" -le 1 ] && break
        echo "snyk code test exit $rc (try $try): $(tail -3 "$out/snyk-code.err")" >&2
    done
    [ "$rc" -le 1 ] || die "snyk code test failed (exit $rc)"
    python3 - "$out/snyk-code.json" "$(git ls-files '*.kt' | wc -l)" "$rc" <<'EOF' || exit 1
import json, sys
d = json.load(open(sys.argv[1]))
expected, rc = int(sys.argv[2]), int(sys.argv[3])
run = d["runs"][0]
kt = sum(c["files"] for c in run.get("properties", {}).get("coverage", []) if c.get("lang") == ".kt")
for r in run["results"]:
    loc = r["locations"][0]["physicalLocation"]
    print(f'{loc["artifactLocation"]["uri"]}:{loc["region"]["startLine"]}: {r["ruleId"]} ({r["level"]})')
print(f"snyk code: {kt} Kotlin files analyzed (expected {expected}), {len(run['results'])} high findings")
if kt < expected:
    print("snyk code analyzed fewer Kotlin files than exist"); sys.exit(1)
sys.exit(1 if run["results"] or rc else 0)
EOF
}

case "${1:-}" in
semgrep) semgrep_scan ;;
snyk-oss) snyk_oss ;;
snyk-code) snyk_code ;;
*) die "usage: $0 semgrep | snyk-oss | snyk-code" ;;
esac

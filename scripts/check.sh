#!/usr/bin/env bash
# Quality gate (R11 T-008), in order: ktlint, detekt, Android lint, unit tests, Python script tests, Semgrep,
# Snyk open source, Snyk Code. Every step runs every time (Gradle steps with --rerun-tasks, so nothing is taken from the build cache) and
# the gate fails if any step fails.
# Writes per-step results to build/gate/last.tsv (step, pass or fail, seconds) for scripts/dashboard.py.
set -uo pipefail
cd "$(dirname "$0")/.."

out=build/gate
mkdir -p "$out"
res="$out/last.tsv.tmp"
: >"$res"
fail=0

step() {
    local name=$1
    shift
    local start
    start=$(date +%s)
    echo "== $name: $*"
    if "$@"; then
        printf '%s\tpass\t%s\n' "$name" $(($(date +%s) - start)) >>"$res"
    else
        printf '%s\tfail\t%s\n' "$name" $(($(date +%s) - start)) >>"$res"
        echo "FAIL: $name"
        fail=1
    fi
}

# Python tests for scripts/ (test_*.py, standard library only). Fails on any failure or if no test ran.
py_tests() {
    local log
    log=$(python3 -I -m unittest discover -s scripts -t scripts -p 'test_*.py' 2>&1)
    local rc=$?
    echo "$log" | tail -4
    local ran
    ran=$(echo "$log" | sed -nE 's/^Ran ([0-9]+) tests? in .*/\1/p' | tail -1)
    if [ "${ran:-0}" -eq 0 ]; then
        echo "py-tests: no tests found"
        return 1
    fi
    echo "py-tests: $ran tests"
    return "$rc"
}

step ktlint ./gradlew ktlintCheck
step detekt ./gradlew detekt
step lint ./gradlew lint --rerun-tasks
step tests ./gradlew test --rerun-tasks
step py-tests py_tests
step semgrep scripts/security.sh semgrep
step snyk-oss scripts/security.sh snyk-oss
step snyk-code scripts/security.sh snyk-code

printf 'finished\t%s\t%s\n' "$(date -Iseconds)" "$fail" >>"$res"
mv "$res" "$out/last.tsv"
if [ "$fail" -ne 0 ]; then
    echo "Gate FAILED"
else
    echo "Gate passed"
fi
exit "$fail"

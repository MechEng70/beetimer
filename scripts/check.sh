#!/usr/bin/env bash
# Quality gate. Every step runs; the gate fails if any step fails.
# Writes per-step results to build/gate/last.tsv (step, pass or fail, seconds) for scripts/dashboard.py.
# R11 T-006 to T-008 add ktlint, detekt, Semgrep and Snyk.
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

step lint ./gradlew lint
step tests ./gradlew test

printf 'finished\t%s\t%s\n' "$(date -Iseconds)" "$fail" >>"$res"
mv "$res" "$out/last.tsv"
if [ "$fail" -ne 0 ]; then
    echo "Gate FAILED"
else
    echo "Gate passed"
fi
exit "$fail"

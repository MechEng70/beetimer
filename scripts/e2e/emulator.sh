#!/usr/bin/env bash
# Emulator for E2E runs and screenshots: scripts/e2e/emulator.sh start | stop
# start: stops the Gradle and Kotlin daemons first (playbook: RAM; never a release build with the emulator up),
# then boots AVD beetimer-api36 and waits for boot. stop: shuts the emulator down.
set -euo pipefail
cd "$(dirname "$0")/../.."
SERIAL="${SERIAL:-emulator-5554}"
AVD="${AVD:-beetimer-api36}"
EMULATOR="${ANDROID_HOME:-$HOME/Android/Sdk}/emulator/emulator"

stop_daemons() {
    ./gradlew --stop >/dev/null 2>&1 || true
    # Match the Kotlin daemon's main class exactly, and never this shell (a loose pattern once killed it).
    local pids
    pids=$(pgrep -f '^/usr/lib/jvm/.* org\.jetbrains\.kotlin\.daemon\.KotlinCompileDaemon' | grep -vx "$$" || true)
    if [ -n "$pids" ]; then
        # shellcheck disable=SC2086
        kill $pids 2>/dev/null || true
    fi
    echo "Gradle and Kotlin daemons stopped"
}

case "${1:-}" in
start)
    if adb -s "$SERIAL" get-state >/dev/null 2>&1; then
        echo "$SERIAL already running"
        exit 0
    fi
    stop_daemons
    nohup "$EMULATOR" -avd "$AVD" -no-snapshot-save -no-boot-anim >"build/emulator.log" 2>&1 &
    adb -s "$SERIAL" wait-for-device
    for _ in $(seq 1 180); do
        [ "$(adb -s "$SERIAL" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" = 1 ] && break
        sleep 1
    done
    [ "$(adb -s "$SERIAL" shell getprop sys.boot_completed | tr -d '\r')" = 1 ] || { echo "boot timed out" >&2; exit 1; }
    echo "$SERIAL booted ($AVD)"
    ;;
stop)
    adb -s "$SERIAL" emu kill >/dev/null 2>&1 || true
    echo "$SERIAL stopped"
    ;;
*)
    echo "usage: $0 start | stop" >&2
    exit 2
    ;;
esac

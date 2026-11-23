#!/usr/bin/env bash
# Debug APK for testing (debug-signed; can't be upgraded by a release build). Runs the gate, builds the debug
# APK and copies it to release/beetimer-<version>-debug.apk (release/ is git-ignored). For a release, use
# scripts/release.sh.
set -euo pipefail
cd "$(dirname "$0")/.."
scripts/check.sh
./gradlew assembleDebug
version=$(grep -E '^appVersion=' gradle.properties | cut -d= -f2)
mkdir -p release
cp app/build/outputs/apk/debug/app-debug.apk "release/beetimer-${version}-debug.apk"
sha256sum "release/beetimer-${version}-debug.apk"

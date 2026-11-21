#!/usr/bin/env bash
# Runs the gate, builds the debug APK, and copies it to release/beetimer-<version>.apk
# for uploading to a GitHub Release. release/ is git-ignored.
set -euo pipefail
cd "$(dirname "$0")/.."
./gradlew test lint assembleDebug
version=$(grep -oP 'versionName = "\K[^"]+' app/build.gradle.kts)
mkdir -p release
cp app/build/outputs/apk/debug/app-debug.apk "release/beetimer-${version}.apk"
sha256sum "release/beetimer-${version}.apk"

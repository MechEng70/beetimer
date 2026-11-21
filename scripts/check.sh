#!/usr/bin/env bash
# Alpha quality gate: unit tests and Android lint.
set -euo pipefail
cd "$(dirname "$0")/.."
./gradlew test lint

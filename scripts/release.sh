#!/usr/bin/env bash
# Signed release build (R11 T-015, playbook section 7). Runs the gate, builds and signs the release APK, checks
# its certificate fingerprint, app ID, versionCode and versionName, and writes
# release/beetimer-<version>.apk plus release/beetimer-<version>.apk.sha256 (release/ is git-ignored).
#
# Fails, before building anything, if:
# - keystore.properties is missing (default: the repo root, git-ignored; or BEETIMER_KEYSTORE_PROPERTIES),
# - the keystore it names is missing,
# - config/release-cert-sha256.txt (the expected signing certificate) is missing,
# - an emulator is running (playbook: never a release build with the emulator up).
# Setup: docs/DEVELOPING.md, "Release keystore".
set -euo pipefail
cd "$(dirname "$0")/.."

die() {
    echo "release: $*" >&2
    exit 1
}

APP_ID=io.github.mecheng70.beetimer
PROPS="${BEETIMER_KEYSTORE_PROPERTIES:-$PWD/keystore.properties}"
CERT_FILE=config/release-cert-sha256.txt
BUILD_TOOLS="${ANDROID_HOME:-$HOME/Android/Sdk}/build-tools/36.0.0"

prop() { grep -E "^$1=" "$PROPS" | head -1 | cut -d= -f2-; }

[ -f "$PROPS" ] || die "keystore.properties not found at $PROPS"
store=$(prop storeFile)
store="${store/#\~/$HOME}"
[ -n "$store" ] || die "storeFile is not set in $PROPS"
[ -f "$store" ] || die "keystore not found at $store"
for k in storePassword keyAlias keyPassword; do
    [ -n "$(prop "$k")" ] || die "$k is not set in $PROPS"
done
[ -s "$CERT_FILE" ] || die "$CERT_FILE is missing: record the release certificate's SHA-256 first (DEVELOPING.md)"
expected_cert=$(tr -d ' :\n' <"$CERT_FILE" | tr 'a-f' 'A-F')
if adb devices 2>/dev/null | grep -qE '^emulator-[0-9]+\s+device'; then
    die "an emulator is running; stop it first (scripts/e2e/emulator.sh stop)"
fi

version=$(grep -E '^appVersion=' gradle.properties | cut -d= -f2)
[ -n "$version" ] || die "appVersion is not set in gradle.properties"

scripts/check.sh || die "the gate failed"

./gradlew :app:assembleRelease -PbeetimerKeystoreProperties="$PROPS" --rerun-tasks
apk=app/build/outputs/apk/release/app-release.apk
[ -f "$apk" ] || die "no signed APK at $apk (is the signing config set up?)"

# Signature and certificate.
"$BUILD_TOOLS/apksigner" verify --verbose "$apk" >/dev/null || die "apksigner verify failed"
cert=$("$BUILD_TOOLS/apksigner" verify --print-certs "$apk" | grep -m1 'certificate SHA-256 digest' |
    awk '{print $NF}' | tr 'a-f' 'A-F')
[ "$cert" = "$expected_cert" ] || die "signing certificate $cert is not the expected $expected_cert"

# App ID, versionCode and versionName from the APK itself.
badging=$("$BUILD_TOOLS/aapt2" dump badging "$apk" | head -1)
got_id=$(sed -nE "s/.*package: name='([^']+)'.*/\1/p" <<<"$badging")
got_code=$(sed -nE "s/.*versionCode='([^']+)'.*/\1/p" <<<"$badging")
got_name=$(sed -nE "s/.*versionName='([^']+)'.*/\1/p" <<<"$badging")
want_code=$(./gradlew -q :app:printVersionCode)
[ "$got_id" = "$APP_ID" ] || die "app ID is $got_id, expected $APP_ID"
[ "$got_name" = "$version" ] || die "versionName is $got_name, expected $version"
[ "$got_code" = "$want_code" ] || die "versionCode is $got_code, expected $want_code"

mkdir -p release
out="release/beetimer-$version.apk"
cp "$apk" "$out"
(cd release && sha256sum "beetimer-$version.apk" >"beetimer-$version.apk.sha256")
echo "Built $out"
echo "  app ID $got_id, versionName $got_name, versionCode $got_code"
echo "  certificate SHA-256 $cert"
cat "$out.sha256"

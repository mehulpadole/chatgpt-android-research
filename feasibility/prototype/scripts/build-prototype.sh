#!/usr/bin/env bash
set -euo pipefail

root="$(cd "$(dirname "$0")/.." && pwd)"
variant="${MOCHI_BUILD_VARIANT:-debug}"
case "$variant" in
  debug) version_name="0.11-debug" ;;
  internal) version_name="0.11-internal" ;;
  release) echo "Release profile is intentionally gated; this repository only builds debug/internal prototype APKs." >&2; exit 2 ;;
  *) echo "Unknown MOCHI_BUILD_VARIANT: $variant" >&2; exit 2 ;;
esac
sdk="${ANDROID_SDK_ROOT:-/home/mehul/Android/Sdk}"
build_tools="$sdk/build-tools/36.0.0"
android_jar="$sdk/platforms/android-37.0/android.jar"
out="$(mktemp -d /tmp/phase5-android-build-XXXXXX)"
mkdir -p "$out/classes" "$out/dex" "$out/flat" "$out/apk" "$root/build-output" "$root/signing"

mapfile -t sources < <(find "$root/src/main/java" -name '*.java' | sort)
javac --release 8 -classpath "$android_jar" -d "$out/classes" "${sources[@]}"
jar cf "$out/prototype-classes.jar" -C "$out/classes" .
"$build_tools/d8" --lib "$android_jar" --output "$out/dex" "$out/prototype-classes.jar"
"$build_tools/aapt2" compile --dir "$root/res" -o "$out/flat"

mapfile -t flat_files < <(find "$out/flat" -name '*.flat' | sort)
link_args=(link -o "$out/apk/local-stream-lab-unsigned.apk" -I "$android_jar"
    --manifest "$root/src/main/AndroidManifest.xml" --min-sdk-version 32
    --target-sdk-version 35 --version-code 1 --version-name "$version_name"
    --auto-add-overlay)
for flat_file in "${flat_files[@]}"; do link_args+=( -R "$flat_file" ); done
"$build_tools/aapt2" "${link_args[@]}"
jar uf "$out/apk/local-stream-lab-unsigned.apk" -C "$out/dex" classes.dex
"$build_tools/zipalign" -f 4 "$out/apk/local-stream-lab-unsigned.apk" "$out/apk/local-stream-lab-aligned.apk"

keystore="$root/signing/phase5-test.keystore"
password='phase5-test-only'
if [ ! -f "$keystore" ]; then
  keytool -genkeypair -keystore "$keystore" -storepass "$password" -keypass "$password" \
    -alias prototype -keyalg RSA -keysize 2048 -validity 3650 \
    -dname 'CN=Phase 5 Local Stream Lab, O=Local Prototype, C=IN' >/dev/null 2>&1
fi
signed="$root/build-output/local-stream-lab.apk"
"$build_tools/apksigner" sign --ks "$keystore" --ks-key-alias prototype \
  --ks-pass "pass:$password" --key-pass "pass:$password" --out "$signed" \
  "$out/apk/local-stream-lab-aligned.apk"
"$build_tools/apksigner" verify --verbose --print-certs "$signed"
printf 'ANDROID APK BUILD PASSED: %s\n' "$signed"

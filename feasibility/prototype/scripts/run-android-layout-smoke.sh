#!/usr/bin/env bash
set -euo pipefail

adb_bin="${ADB_BIN:-/home/mehul/Android/Sdk/platform-tools/adb}"
package_name="com.example.androidfeasibility"
remote_xml="/sdcard/mochi-layout-smoke.xml"
local_xml="$(mktemp /tmp/mochi-layout-smoke.XXXXXX.xml)"

if [[ "$(${adb_bin} get-state 2>/dev/null || true)" != "device" ]]; then
  echo "ANDROID LAYOUT SMOKE BLOCKED: adb target is not ready" >&2
  exit 2
fi

${adb_bin} shell am force-stop "$package_name"
${adb_bin} shell am start -W -n "$package_name/.MainActivity" >/dev/null
sleep 1
${adb_bin} shell uiautomator dump "$remote_xml" >/dev/null
${adb_bin} exec-out cat "$remote_xml" > "$local_xml"

frame_bottom="$(${adb_bin} shell dumpsys window displays | sed -n 's/.*overrideNonDecorFrame=\[0,0\]\[[0-9]*,\([0-9]*\)\].*/\1/p' | head -1 | tr -d '\r')"
if [[ -z "$frame_bottom" ]]; then
  echo "ANDROID LAYOUT SMOKE FAILED: could not determine visible app frame" >&2
  exit 1
fi

for descriptor in "Message composer" "Send message" "Cancel active response"; do
  line="$(rg -m1 "content-desc=\"${descriptor}\"" "$local_xml" || true)"
  if [[ -z "$line" ]]; then
    echo "ANDROID LAYOUT SMOKE FAILED: missing ${descriptor}" >&2
    exit 1
  fi
  bounds="$(sed -n 's/.*bounds="\[\([0-9]*\),\([0-9]*\)\]\[\([0-9]*\),\([0-9]*\)\]".*/\1 \2 \3 \4/p' <<<"$line")"
  read -r left top right bottom <<<"$bounds"
  center_y=$(( (top + bottom) / 2 ))
  if (( center_y >= frame_bottom )); then
    echo "ANDROID LAYOUT SMOKE FAILED: ${descriptor} center ${center_y} is outside visible frame ${frame_bottom}" >&2
    exit 1
  fi
done

echo "ANDROID LAYOUT SMOKE PASSED: composer, send, and stop controls are visible in frame ${frame_bottom}"

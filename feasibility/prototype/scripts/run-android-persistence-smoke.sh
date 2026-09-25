#!/usr/bin/env bash
set -euo pipefail

ADB_BIN="${ADB_BIN:-/home/mehul/Android/Sdk/platform-tools/adb}"
PACKAGE="com.example.androidfeasibility"
ACTIVITY="${PACKAGE}/.MainActivity"
WORK_DIR="$(mktemp -d /tmp/mochi-android-persistence.XXXXXX)"
trap 'rm -rf "$WORK_DIR"' EXIT

if ! "$ADB_BIN" get-state >/dev/null 2>&1; then
    printf 'ANDROID PERSISTENCE SMOKE BLOCKED: no connected adb device\n'
    exit 2
fi

prompt="runtime-persistence-$(date +%s)"

dump_ui() {
    "$ADB_BIN" shell input swipe 500 1600 500 700 600
    sleep 1
    "$ADB_BIN" shell uiautomator dump /sdcard/mochi-persistence-smoke.xml >/dev/null
    "$ADB_BIN" exec-out cat /sdcard/mochi-persistence-smoke.xml > "$WORK_DIR/ui.xml"
}

"$ADB_BIN" shell am force-stop "$PACKAGE"
"$ADB_BIN" shell am start -W -n "$ACTIVITY" >/dev/null
sleep 2
"$ADB_BIN" shell input tap 90 1790
"$ADB_BIN" shell input text "$prompt"
"$ADB_BIN" shell input keyevent KEYCODE_ESCAPE
sleep 0.8
"$ADB_BIN" shell input tap 700 1790
sleep 1.5
dump_ui

if ! rg -q "You · COMPLETED&#10;${prompt}" "$WORK_DIR/ui.xml" || \
   ! rg -q "Assistant · COMPLETED" "$WORK_DIR/ui.xml"; then
    printf 'ANDROID PERSISTENCE SMOKE FAILED: initial turn did not complete\n'
    exit 1
fi

"$ADB_BIN" shell am force-stop "$PACKAGE"
"$ADB_BIN" shell am start -W -n "$ACTIVITY" >/dev/null
sleep 2
dump_ui

if ! rg -q "You · COMPLETED&#10;${prompt}" "$WORK_DIR/ui.xml" || \
   ! rg -q "Assistant · COMPLETED" "$WORK_DIR/ui.xml"; then
    printf 'ANDROID PERSISTENCE SMOKE FAILED: restored turn was not rendered\n'
    exit 1
fi

printf 'ANDROID PERSISTENCE SMOKE PASSED: completed turn restored after process restart\n'

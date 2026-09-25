#!/usr/bin/env bash
set -euo pipefail

root="$(cd "$(dirname "$0")/.." && pwd)"
main="$root/src/main/java"

if rg -n '"sk-[A-Za-z0-9_-]{8,}"|sk-[A-Za-z0-9_-]{16,}' "$main"; then
  echo "FAIL: secret-shaped literal found in production prototype sources" >&2
  exit 1
fi
if ! rg -q 'Release profile is intentionally gated' "$root/scripts/build-prototype.sh"; then
  echo "FAIL: release build gate is missing" >&2
  exit 1
fi
if ! rg -q 'setAccessibilityLiveRegion|setContentDescription' "$main"; then
  echo "FAIL: accessibility affordance scan found no live/action descriptions" >&2
  exit 1
fi
if rg -n 'phase5_http_base_url|phase9_sync_base_url' "$main" | rg -v 'MainActivity.java'; then
  echo "FAIL: injectable debug endpoint leaked outside the shell boundary" >&2
  exit 1
fi
echo "RELEASE BOUNDARY AUDIT PASSED: no secret-shaped literals, release gate, and accessibility hooks present"

#!/usr/bin/env bash
set -euo pipefail

root="$(cd "$(dirname "$0")/.." && pwd)"
coordinator="$root/src/main/java/com/example/androidfeasibility/ConversationCoordinator.java"
adapter="$root/src/main/java/com/example/androidfeasibility/ProviderAdapter.java"

forbidden=(
  'MockScenario'
  'HttpURLConnection'
  'HttpStreamingProviderAdapter'
  'NdjsonCodec'
  'java\.net\.URL'
  'HTTP_[A-Z_]+'
  'ProviderResponse'
  'OpenRouter'
)

failed=0
for pattern in "${forbidden[@]}"; do
  if rg -n "$pattern" "$coordinator" "$adapter"; then
    echo "provider boundary leak: $pattern" >&2
    failed=1
  fi
done

if [ "$failed" -ne 0 ]; then
  exit 1
fi
echo "PROVIDER BOUNDARY AUDIT PASSED: coordinator and adapter are transport/mock neutral"

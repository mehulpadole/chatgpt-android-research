#!/usr/bin/env bash
set -euo pipefail

root="$(cd "$(dirname "$0")/.." && pwd)"
out="$(mktemp -d /tmp/phase5-core-tests-XXXXXX)"

mapfile -t main_sources < <(
  find "$root/src/main/java" -name '*.java' \
    ! -name 'MainActivity.java' \
    ! -name 'JsonConversationRepository.java' \
    ! -name 'AndroidConversationCodec.java' \
    | sort
)
mapfile -t test_sources < <(find "$root/src/test/java" -name '*.java' | sort)

javac --release 8 -d "$out" "${main_sources[@]}" "${test_sources[@]}"

tests=(
  com.example.androidfeasibility.ProviderContractTest
  com.example.androidfeasibility.CoordinatorContractTest
  com.example.androidfeasibility.HttpStreamingProviderAdapterTest
  com.example.androidfeasibility.ProviderConformanceTest
  com.example.androidfeasibility.PrototypeCoreTest
)
for test_class in "${tests[@]}"; do
  java -cp "$out" "$test_class"
done
echo "ALL PHASE 5 PURE-JAVA TESTS PASSED (${#tests[@]} test entry points)"

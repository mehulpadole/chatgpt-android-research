#!/usr/bin/env bash
set -euo pipefail

root="$(cd "$(dirname "$0")/.." && pwd)"
out="$(mktemp -d /tmp/phase5-core-tests-XXXXXX)"

mapfile -t main_sources < <(
  find "$root/src/main/java" -name '*.java' \
    ! -name 'MainActivity.java' \
    ! -name 'JsonConversationRepository.java' \
    ! -name 'AndroidConversationCodec.java' \
    ! -name 'AndroidCredentialStore.java' \
    | sort
)
mapfile -t test_sources < <(find "$root/src/test/java" -name '*.java' ! -name 'AndroidCodecCompatibilityTest.java' | sort)

javac --release 8 -d "$out" "${main_sources[@]}" "${test_sources[@]}"
javac --release 8 -cp "$out" -d "$out" \
  "$root/src/main/java/com/example/androidfeasibility/AndroidConversationCodec.java" \
  "$root/src/test/java/org/json/JSONObject.java" \
  "$root/src/test/java/org/json/JSONArray.java" \
  "$root/src/test/java/com/example/androidfeasibility/AndroidCodecCompatibilityTest.java"

tests=(
  com.example.androidfeasibility.OpenRouterCodecTest
  com.example.androidfeasibility.OpenRouterProviderAdapterTest
  com.example.androidfeasibility.CredentialBoundaryTest
  com.example.androidfeasibility.ProviderSettingsControllerTest
  com.example.androidfeasibility.ProviderContractTest
  com.example.androidfeasibility.CoordinatorContractTest
  com.example.androidfeasibility.ProviderRouterTest
  com.example.androidfeasibility.HttpStreamingProviderAdapterTest
  com.example.androidfeasibility.ProviderConformanceTest
  com.example.androidfeasibility.AndroidCodecCompatibilityTest
  com.example.androidfeasibility.PrototypeCoreTest
)
for test_class in "${tests[@]}"; do
  java -cp "$out" "$test_class"
done
echo "ALL PHASE 5 PURE-JAVA TESTS PASSED (${#tests[@]} test entry points)"

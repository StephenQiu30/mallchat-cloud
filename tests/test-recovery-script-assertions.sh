#!/usr/bin/env bash
set -euo pipefail

# Focused test: verify that verify-im-core-data-recovery.sh covers all required foreign-key assertions.
# This test runs the recovery script in --dry-run mode and checks that the output contains
# every required assertion name. If a required assertion is missing, the test fails (RED).

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SCRIPT="$ROOT_DIR/scripts/verify-im-core-data-recovery.sh"

if [[ ! -x "$SCRIPT" ]]; then
  echo "FAIL: recovery script not found or not executable: $SCRIPT" >&2
  exit 1
fi

OUTPUT="$(bash "$SCRIPT" --dry-run 2>&1)"

# Required assertions that MUST be present in the recovery script output.
# These cover all core IM table foreign-key relationships.
REQUIRED_ASSERTIONS=(
  "user_friend user relation"
  "user_friend friend relation"
  "user_friend_apply user relation"
  "user_friend_apply target relation"
  "chat_room creator relation"
  "chat_room_member user relation"
  "chat_room_member room relation"
  "chat_message room relation"
  "chat_message sender relation"
  "chat_session message relation"
  "chat_session room relation"
  "chat_session user relation"
  "chat_private_room room relation"
  "chat_private_room user_low relation"
  "chat_private_room user_high relation"
  "chat_group_info room relation"
  "chat_moment user relation"
  "chat_moment_media relation"
  "chat_moment_like relation"
  "chat_moment_like user relation"
  "chat_moment_comment relation"
  "chat_moment_comment user relation"
)

FAILED=0
for assertion in "${REQUIRED_ASSERTIONS[@]}"; do
  if ! echo "$OUTPUT" | grep -qF "$assertion"; then
    echo "MISSING: $assertion" >&2
    FAILED=1
  fi
done

if [[ "$FAILED" -eq 1 ]]; then
  echo "FAIL: recovery script is missing required assertions" >&2
  exit 1
fi

echo "PASS: all ${#REQUIRED_ASSERTIONS[@]} required assertions present in recovery script"

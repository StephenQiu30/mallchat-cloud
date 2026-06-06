#!/usr/bin/env bash
set -euo pipefail

MYSQL_HOST="${MYSQL_HOST:-127.0.0.1}"
MYSQL_PORT="${MYSQL_PORT:-3306}"
MYSQL_USER="${MYSQL_USER:-root}"
MYSQL_PASSWORD="${MYSQL_PASSWORD:-root}"
MYSQL_DATABASE="${MYSQL_DATABASE:-mallchat}"
RECOVERY_DATABASE="${RECOVERY_DATABASE:-mallchat_recovery_smoke_$$}"
BACKUP_FILE="${BACKUP_FILE:-}"
KEEP_RECOVERY_DB="${KEEP_RECOVERY_DB:-false}"

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

if [[ ! "$RECOVERY_DATABASE" =~ ^[A-Za-z0-9_]+$ ]]; then
  echo "RECOVERY_DATABASE 只能包含字母、数字和下划线" >&2
  exit 1
fi

mysql_args() {
  local args=(--host="$MYSQL_HOST" --port="$MYSQL_PORT" --user="$MYSQL_USER" --default-character-set=utf8mb4)
  if [[ -n "$MYSQL_PASSWORD" ]]; then
    args+=("--password=$MYSQL_PASSWORD")
  fi
  printf '%s\n' "${args[@]}"
}

ASSERTIONS=(
  "user_friend user relation|SELECT COUNT(*) FROM user_friend uf LEFT JOIN \`user\` u ON u.id = uf.user_id WHERE u.id IS NULL;"
  "user_friend friend relation|SELECT COUNT(*) FROM user_friend uf LEFT JOIN \`user\` u ON u.id = uf.friend_user_id WHERE u.id IS NULL;"
  "user_friend_apply user relation|SELECT COUNT(*) FROM user_friend_apply ufa LEFT JOIN \`user\` u ON u.id = ufa.user_id WHERE u.id IS NULL;"
  "user_friend_apply target relation|SELECT COUNT(*) FROM user_friend_apply ufa LEFT JOIN \`user\` u ON u.id = ufa.target_id WHERE u.id IS NULL;"
  "chat_room creator relation|SELECT COUNT(*) FROM chat_room r LEFT JOIN \`user\` u ON u.id = r.create_user WHERE u.id IS NULL;"
  "chat_room_member user relation|SELECT COUNT(*) FROM chat_room_member rm LEFT JOIN \`user\` u ON u.id = rm.user_id WHERE u.id IS NULL;"
  "chat_message room relation|SELECT COUNT(*) FROM chat_message m LEFT JOIN chat_room r ON r.id = m.room_id WHERE r.id IS NULL;"
  "chat_message sender relation|SELECT COUNT(*) FROM chat_message m LEFT JOIN \`user\` u ON u.id = m.from_user_id WHERE u.id IS NULL;"
  "chat_session message relation|SELECT COUNT(*) FROM chat_session s LEFT JOIN chat_message m ON m.id = s.last_message_id WHERE s.last_message_id IS NOT NULL AND m.id IS NULL;"
  "chat_session room relation|SELECT COUNT(*) FROM chat_session s LEFT JOIN chat_room r ON r.id = s.room_id WHERE r.id IS NULL;"
  "chat_session user relation|SELECT COUNT(*) FROM chat_session s LEFT JOIN \`user\` u ON u.id = s.user_id WHERE u.id IS NULL;"
  "chat_room_member room relation|SELECT COUNT(*) FROM chat_room_member rm LEFT JOIN chat_room r ON r.id = rm.room_id WHERE r.id IS NULL;"
  "chat_private_room room relation|SELECT COUNT(*) FROM chat_private_room pr LEFT JOIN chat_room r ON r.id = pr.room_id WHERE r.id IS NULL;"
  "chat_private_room user_low relation|SELECT COUNT(*) FROM chat_private_room pr LEFT JOIN \`user\` u ON u.id = pr.user_low WHERE u.id IS NULL;"
  "chat_private_room user_high relation|SELECT COUNT(*) FROM chat_private_room pr LEFT JOIN \`user\` u ON u.id = pr.user_high WHERE u.id IS NULL;"
  "chat_group_info room relation|SELECT COUNT(*) FROM chat_group_info gi LEFT JOIN chat_room r ON r.id = gi.room_id WHERE r.id IS NULL;"
  "chat_moment user relation|SELECT COUNT(*) FROM chat_moment m LEFT JOIN \`user\` u ON u.id = m.user_id WHERE u.id IS NULL;"
  "chat_moment_media relation|SELECT COUNT(*) FROM chat_moment_media mm LEFT JOIN chat_moment m ON m.id = mm.moment_id WHERE m.id IS NULL;"
  "chat_moment_like relation|SELECT COUNT(*) FROM chat_moment_like ml LEFT JOIN chat_moment m ON m.id = ml.moment_id WHERE m.id IS NULL;"
  "chat_moment_like user relation|SELECT COUNT(*) FROM chat_moment_like ml LEFT JOIN \`user\` u ON u.id = ml.user_id WHERE u.id IS NULL;"
  "chat_moment_comment relation|SELECT COUNT(*) FROM chat_moment_comment mc LEFT JOIN chat_moment m ON m.id = mc.moment_id WHERE m.id IS NULL;"
  "chat_moment_comment user relation|SELECT COUNT(*) FROM chat_moment_comment mc LEFT JOIN \`user\` u ON u.id = mc.user_id WHERE u.id IS NULL;"
)

if [[ "${1:-}" == "--dry-run" ]]; then
  echo "Core IM tables:"
  bash "$ROOT_DIR/scripts/backup-im-core-tables.sh" --print-tables
  echo "Recovery database: $RECOVERY_DATABASE"
  echo "Assertions:"
  printf '%s\n' "${ASSERTIONS[@]}"
  exit 0
fi

MYSQL_ARGS=()
while IFS= read -r arg; do
  MYSQL_ARGS+=("$arg")
done < <(mysql_args)

if [[ -z "$BACKUP_FILE" ]]; then
  BACKUP_FILE="$(BACKUP_OUTPUT="${BACKUP_OUTPUT:-}" bash "$ROOT_DIR/scripts/backup-im-core-tables.sh")"
fi

cleanup() {
  if [[ "$KEEP_RECOVERY_DB" != "true" ]]; then
    mysql "${MYSQL_ARGS[@]}" -e "DROP DATABASE IF EXISTS \`$RECOVERY_DATABASE\`;"
  fi
}
trap cleanup EXIT

mysql "${MYSQL_ARGS[@]}" -e "DROP DATABASE IF EXISTS \`$RECOVERY_DATABASE\`; CREATE DATABASE \`$RECOVERY_DATABASE\` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
mysql "${MYSQL_ARGS[@]}" "$RECOVERY_DATABASE" < "$BACKUP_FILE"

for assertion in "${ASSERTIONS[@]}"; do
  name="${assertion%%|*}"
  query="${assertion#*|}"
  count="$(mysql "${MYSQL_ARGS[@]}" --batch --skip-column-names "$RECOVERY_DATABASE" -e "$query")"
  if [[ "$count" != "0" ]]; then
    echo "Recovery assertion failed: $name, orphan count=$count" >&2
    exit 1
  fi
done

echo "IM core data recovery smoke passed: $RECOVERY_DATABASE"

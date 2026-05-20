#!/usr/bin/env bash
set -euo pipefail

CORE_TABLES=(
  user
  user_friend
  user_friend_apply
  chat_room
  chat_room_member
  chat_private_room
  chat_group_info
  chat_message
  chat_session
  chat_moment
  chat_moment_media
  chat_moment_like
  chat_moment_comment
)

MYSQL_HOST="${MYSQL_HOST:-127.0.0.1}"
MYSQL_PORT="${MYSQL_PORT:-3306}"
MYSQL_USER="${MYSQL_USER:-root}"
MYSQL_PASSWORD="${MYSQL_PASSWORD:-root}"
MYSQL_DATABASE="${MYSQL_DATABASE:-mallchat}"
BACKUP_OUTPUT="${BACKUP_OUTPUT:-backups/im-core-$(date +%Y%m%d%H%M%S).sql}"

if [[ "${1:-}" == "--print-tables" ]]; then
  printf '%s\n' "${CORE_TABLES[@]}"
  exit 0
fi

MYSQLDUMP_ARGS=(
  --host="$MYSQL_HOST"
  --port="$MYSQL_PORT"
  --user="$MYSQL_USER"
  --single-transaction
  --routines
  --triggers
  --default-character-set=utf8mb4
)

if [[ -n "$MYSQL_PASSWORD" ]]; then
  MYSQLDUMP_ARGS+=("--password=$MYSQL_PASSWORD")
fi

if [[ "${1:-}" == "--dry-run" ]]; then
  MYSQLDUMP_DISPLAY_ARGS=()
  for arg in "${MYSQLDUMP_ARGS[@]}"; do
    if [[ "$arg" == --password=* ]]; then
      MYSQLDUMP_DISPLAY_ARGS+=("--password=****")
    else
      MYSQLDUMP_DISPLAY_ARGS+=("$arg")
    fi
  done
  echo "mysqldump ${MYSQLDUMP_DISPLAY_ARGS[*]} $MYSQL_DATABASE ${CORE_TABLES[*]} > $BACKUP_OUTPUT"
  exit 0
fi

mkdir -p "$(dirname "$BACKUP_OUTPUT")"
mysqldump "${MYSQLDUMP_ARGS[@]}" "$MYSQL_DATABASE" "${CORE_TABLES[@]}" > "$BACKUP_OUTPUT"
echo "$BACKUP_OUTPUT"

#!/usr/bin/env bash
# Restore a dump made by backup-db.sh. See docs/BACKUP.md.
#
# Usage:  scripts/restore-db.sh <dump.sql.gz> [target_db]
#
# The target database defaults to DB_NAME and is created when missing. Restoring over a database
# that has tables asks for confirmation; set RESTORE_CONFIRM=yes to skip the prompt (cron, CI).
# Connection variables are the same as backup-db.sh (DB_URL or DB_HOST/DB_PORT/DB_NAME, DB_USERNAME, DB_PASSWORD).
set -euo pipefail

dump="${1:?Usage: restore-db.sh <dump.sql.gz> [target_db]}"
[[ -f "$dump" ]] || { echo "No such file: $dump" >&2; exit 1; }

if [[ -n "${DB_URL:-}" ]]; then
  url="${DB_URL#jdbc:}"
  url="${url#mysql://}"
  hostport="${url%%/*}"
  rest="${url#*/}"
  : "${DB_HOST:=${hostport%%:*}}"
  if [[ "$hostport" == *:* ]]; then : "${DB_PORT:=${hostport##*:}}"; fi
  : "${DB_NAME:=${rest%%\?*}}"
fi

DB_HOST="${DB_HOST:-127.0.0.1}"
DB_PORT="${DB_PORT:-3306}"
DB_NAME="${DB_NAME:-learning}"
: "${DB_USERNAME:?Set DB_USERNAME}"
: "${DB_PASSWORD:?Set DB_PASSWORD}"
target="${2:-$DB_NAME}"

[[ "$target" =~ ^[A-Za-z0-9_]+$ ]] || { echo "Invalid database name: $target" >&2; exit 1; }
command -v mysql >/dev/null || { echo "mysql client not found in PATH" >&2; exit 1; }
gzip -t "$dump"

mysql_cmd() {
  MYSQL_PWD="$DB_PASSWORD" mysql --host="$DB_HOST" --port="$DB_PORT" --user="$DB_USERNAME" \
    --default-character-set=utf8mb4 "$@"
}

mysql_cmd -e "CREATE DATABASE IF NOT EXISTS \`$target\` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci"
tables="$(mysql_cmd -N -e "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = '$target'")"

if [[ "$tables" != "0" && "${RESTORE_CONFIRM:-}" != "yes" ]]; then
  read -r -p "Database '$target' on $DB_HOST has $tables tables that will be overwritten. Type the database name to continue: " answer
  [[ "$answer" == "$target" ]] || { echo "Aborted."; exit 1; }
fi

echo "Restoring $dump -> $target@$DB_HOST:$DB_PORT"
gunzip -c "$dump" | mysql_cmd "$target"
echo "Restore finished. Tables in $target: $(mysql_cmd -N -e "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = '$target'")"

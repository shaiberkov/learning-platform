#!/usr/bin/env bash
# Restore test: proves the newest backup can actually be restored. See docs/BACKUP.md.
#
# Usage:  scripts/test-restore.sh [backup_dir]
#
# 1. picks the newest <DB_NAME>_*.sql.gz in the backup dir,
# 2. restores it into a scratch database <DB_NAME>_restore_test,
# 3. compares exact row counts per table with the live database (rows written after the dump
#    make the live side bigger, so counts are printed for review; the check fails when a table is missing),
# 4. checks flyway_schema_history came back, then drops the scratch database.
# Uses the same connection variables as backup-db.sh. Exit code 0 = restore test passed.
set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

if [[ -n "${DB_URL:-}" ]]; then
  url="${DB_URL#jdbc:}"
  url="${url#mysql://}"
  hostport="${url%%/*}"
  rest="${url#*/}"
  : "${DB_HOST:=${hostport%%:*}}"
  if [[ "$hostport" == *:* ]]; then : "${DB_PORT:=${hostport##*:}}"; fi
  : "${DB_NAME:=${rest%%\?*}}"
fi
export DB_HOST="${DB_HOST:-127.0.0.1}" DB_PORT="${DB_PORT:-3306}" DB_NAME="${DB_NAME:-learning}"
: "${DB_USERNAME:?Set DB_USERNAME}"
: "${DB_PASSWORD:?Set DB_PASSWORD}"
BACKUP_DIR="${1:-${BACKUP_DIR:-./backups}}"
scratch="${DB_NAME}_restore_test"

latest="$(ls -1t "$BACKUP_DIR"/"${DB_NAME}"_*.sql.gz 2>/dev/null | head -n 1 || true)"
[[ -n "$latest" ]] || { echo "No backups found in $BACKUP_DIR" >&2; exit 1; }
echo "Testing restore of $latest into $scratch"

mysql_cmd() {
  MYSQL_PWD="$DB_PASSWORD" mysql --host="$DB_HOST" --port="$DB_PORT" --user="$DB_USERNAME" -N "$@"
}
cleanup() { mysql_cmd -e "DROP DATABASE IF EXISTS \`$scratch\`" || true; }
trap cleanup EXIT

cleanup
RESTORE_CONFIRM=yes "$script_dir/restore-db.sh" "$latest" "$scratch"

failed=0
while read -r table; do
  live="$(mysql_cmd -e "SELECT COUNT(*) FROM \`$DB_NAME\`.\`$table\`")"
  restored="$(mysql_cmd -e "SELECT COUNT(*) FROM \`$scratch\`.\`$table\`" 2>/dev/null || echo MISSING)"
  printf '%-45s live=%-8s restored=%s\n' "$table" "$live" "$restored"
  if [[ "$restored" == "MISSING" ]]; then failed=1; fi
done < <(mysql_cmd -e "SELECT table_name FROM information_schema.tables WHERE table_schema = '$DB_NAME' AND table_type = 'BASE TABLE' ORDER BY table_name")

history="$(mysql_cmd -e "SELECT COUNT(*) FROM \`$scratch\`.flyway_schema_history" 2>/dev/null || echo 0)"
if [[ "$history" == "0" ]]; then
  echo "WARNING: flyway_schema_history is empty or missing in the restore (expected once Flyway has run)."
fi

if [[ "$failed" == "1" ]]; then
  echo "RESTORE TEST FAILED: tables missing from the restore." >&2
  exit 1
fi
echo "RESTORE TEST PASSED"

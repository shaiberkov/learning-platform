#!/usr/bin/env bash
# Daily MySQL backup with retention. See docs/BACKUP.md.
#
# Usage:  scripts/backup-db.sh [backup_dir]
#
# Connection (environment variables, nothing is hard-coded):
#   DB_URL                 jdbc:mysql://host:port/db?...  (optional; host/port/name are read from it)
#   DB_HOST, DB_PORT, DB_NAME   override / replace DB_URL (defaults 127.0.0.1, 3306, learning)
#   DB_USERNAME, DB_PASSWORD    required
#   BACKUP_DIR             where dumps go (default ./backups; the first argument wins)
#   RETENTION_DAYS         dumps older than this are deleted (default 14)
#
# Writes <dir>/<db>_<YYYY-MM-DD_HHMMSS>.sql.gz. The password is passed through MYSQL_PWD,
# never on the command line, so it does not show up in the process list.
set -euo pipefail

if [[ -n "${DB_URL:-}" ]]; then
  # jdbc:mysql://host:port/name?params
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
BACKUP_DIR="${1:-${BACKUP_DIR:-./backups}}"
RETENTION_DAYS="${RETENTION_DAYS:-14}"

command -v mysqldump >/dev/null || { echo "mysqldump not found in PATH" >&2; exit 1; }

mkdir -p "$BACKUP_DIR"
timestamp="$(date +%Y-%m-%d_%H%M%S)"
file="$BACKUP_DIR/${DB_NAME}_${timestamp}.sql.gz"
partial="$file.partial"

echo "Backing up $DB_NAME@$DB_HOST:$DB_PORT -> $file"
# --single-transaction: consistent InnoDB snapshot without locking the app out.
# --no-tablespaces: avoids needing the PROCESS privilege on managed MySQL.
MYSQL_PWD="$DB_PASSWORD" mysqldump \
  --host="$DB_HOST" --port="$DB_PORT" --user="$DB_USERNAME" \
  --single-transaction --quick --routines --triggers --events --no-tablespaces \
  --default-character-set=utf8mb4 \
  "$DB_NAME" | gzip -9 > "$partial"

# Only a complete dump gets the final name (pipefail stops us above if mysqldump failed).
gzip -t "$partial"
mv "$partial" "$file"
echo "Done: $(du -h "$file" | cut -f1)"

echo "Deleting backups older than $RETENTION_DAYS days"
find "$BACKUP_DIR" -maxdepth 1 -type f -name "${DB_NAME}_*.sql.gz" -mtime +"$RETENTION_DAYS" -print -delete
find "$BACKUP_DIR" -maxdepth 1 -type f -name "${DB_NAME}_*.sql.gz.partial" -mtime +1 -delete

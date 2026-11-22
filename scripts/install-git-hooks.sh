#!/bin/sh
# Installs the gitleaks pre-commit hook (scripts/git-hooks/pre-commit) into this clone.
# Copies it into git's hooks folder instead of changing core.hooksPath, so other hooks
# already there (for example graphify's post-commit) keep working.
set -e
src="$(git rev-parse --show-toplevel)/scripts/git-hooks/pre-commit"
dest="$(git rev-parse --git-common-dir)/hooks/pre-commit"

if [ -f "$dest" ] && ! cmp -s "$src" "$dest"; then
  echo "A different pre-commit hook already exists at $dest." >&2
  echo "Add this line to it instead:  sh \"$src\" || exit 1" >&2
  exit 1
fi
cp "$src" "$dest"
chmod +x "$dest"
echo "Installed gitleaks pre-commit hook at $dest"

# Secret scanning

The repository is public. A key that reaches a commit is leaked, even if a later commit deletes it.
Secrets live only in `backend/.env` (git-ignored) locally and in Render's environment in production,
and are read through `Environment.getProperty(...)` or `${...}` placeholders (`GPT_TOKEN`, `SMS_TOKEN`,
`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`).

## What is in place

| Layer | File | When it runs |
|---|---|---|
| Rules | `.gitleaks.toml` | gitleaks' built-in rules, plus modern OpenAI keys (`sk-proj-…`, `sk-svcacct-…`) and literal values for this project's env names |
| CI | `.github/workflows/gitleaks.yml` | every push and pull request; scans the full history and fails the run on a finding |
| Local | `scripts/git-hooks/pre-commit` | before each commit, on the staged changes only |

Install the local hook once per clone (needs the [gitleaks CLI](https://github.com/gitleaks/gitleaks#installing) on `PATH`;
without it the hook prints a warning and lets the commit through, and CI still scans):

```sh
sh scripts/install-git-hooks.sh
```

The installer copies the hook into `.git/hooks/` and refuses to overwrite a different existing `pre-commit`, so the graphify hooks keep working.

Run a scan by hand:

```sh
gitleaks git --config .gitleaks.toml --redact .          # whole history
gitleaks git --staged --config .gitleaks.toml --redact .  # what you are about to commit
```

A false positive goes in the `[allowlist]` of `.gitleaks.toml` (a path or a regex), never by weakening a rule.

## State of the repository

When the repositories were merged, an OpenAI key and the local MySQL password were scrubbed from the history.
A check of every tracked file and of `git log --all -p` for OpenAI key patterns found nothing on this branch.

## Manual steps (cloud, not automatable from the repo)

1. **Revoke the leaked OpenAI key** in the OpenAI dashboard (API keys page). Scrubbing history does not un-leak it: forks, clones and caches may still hold it. Create a new key and set it as `GPT_TOKEN` in Render and in `backend/.env`.
2. **Rotate the MySQL password** that was in the history, and update `DB_PASSWORD` in Render and in `backend/.env`.
3. **Enable GitHub Secret Scanning and Push Protection**: repository Settings → Code security → Secret Protection → enable "Secret scanning" and "Push protection".
4. Optionally make the `Secret scan (gitleaks)` check required on `main` (Settings → Branches → branch protection rule).

## If a secret is committed

1. Revoke it at the provider first. That is the only real fix.
2. Remove it from the code, read it from the environment instead, and add the new value to `backend/.env` / Render.
3. Rewriting history (`git filter-repo`) is optional cleanup after revocation, not a substitute for it.

"""PostToolUse hook (.claude/settings.json): after an edit to backend code that reads or writes data, remind Claude to
check docs/CACHING.md. It never blocks the edit."""
import json
import re
import sys

PKG = r"backend/src/main/java/org/example/learningprojectserver/"
DATA_CODE = re.compile(r"(^|/)(" + PKG + r"(service|repository|entities|jobs|notification)/|backend/src/main/resources/ehcache\.xml)")

try:
    event = json.load(sys.stdin)
except ValueError:
    sys.exit(0)
path = str((event.get("tool_input") or {}).get("file_path", "")).replace("\\", "/")
if DATA_CODE.search(path):
    print(json.dumps({"hookSpecificOutput": {
        "hookEventName": "PostToolUse",
        "additionalContext": (
            "Caching check (docs/CACHING.md): does this change read or write data that a cache holds? "
            "Ehcache: every @Cacheable / @CacheEvict / cacheManager.getCache name needs an alias in ehcache.xml, and "
            "every write to cached data evicts the same key the @Cacheable builds. In-memory maps: OTPs, tokenCache, "
            "SSE emitters, ChatGPT conversations. Update docs/CACHING.md with any change."
        ),
    }}))

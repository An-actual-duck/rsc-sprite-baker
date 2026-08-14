#!/usr/bin/env bash
set -euo pipefail

PROJECT_ROOT="$(git rev-parse --show-toplevel)"
PRIMARY_ROOT="$(git -C "$PROJECT_ROOT" worktree list --porcelain | sed -n 's/^worktree //p' | sed -n '1p')"
CORE_ROOT="${CORE_FRAMEWORK_ROOT:-$(dirname "$PRIMARY_ROOT")/Core-Framework}"

[[ -x "$CORE_ROOT/scripts/ai-manager.sh" ]] || {
  printf 'FAIL: Core manager workflow not found at %s\n' "$CORE_ROOT" >&2
  exit 1
}

ROOT_DIR="$PROJECT_ROOT" \
AI_PROJECT_NAME="rsc-sprite-baker" \
AI_REMOTE="origin" \
AI_MAIN_BRANCH="main" \
exec "$CORE_ROOT/scripts/ai-manager.sh" "$@"

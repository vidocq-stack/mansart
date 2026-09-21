#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
SPEC="${1:-}"
TIMEOUT="${OPENCODE_TIMEOUT:-1800}"

if [[ ! "$SPEC" =~ ^[A-Z]{3}$ ]]; then
    echo "usage: scripts/opencode-harness/review-plan.sh <SPEC>" >&2
    exit 2
fi
if [[ ! -d "$ROOT/tasks/$SPEC" ]]; then
    echo "plan not found: tasks/$SPEC" >&2
    exit 3
fi

cd "$ROOT"
exec timeout --foreground "$TIMEOUT" opencode run --print-logs --log-level INFO --agent lead --command review-plan "$SPEC"

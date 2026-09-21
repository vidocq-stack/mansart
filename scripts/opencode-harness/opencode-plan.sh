#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
SPEC="${1:-}"
TIMEOUT="${OPENCODE_TIMEOUT:-1800}"

if [[ ! "$SPEC" =~ ^[A-Z]{3}$ ]]; then
    echo "usage: scripts/opencode-harness/opencode-plan.sh <SPEC>" >&2
    exit 2
fi

if [[ ! -d "$ROOT/docs/spec-src/$SPEC/chapters" ]]; then
    echo "split specification material not found: docs/spec-src/$SPEC/chapters" >&2
    exit 3
fi

cd "$ROOT"
exec timeout --foreground "$TIMEOUT" opencode run --print-logs --log-level INFO --agent lead --command spec-plan "$SPEC"

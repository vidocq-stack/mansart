#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
SPEC="${1:-}"
TIMEOUT="${OPENCODE_TIMEOUT:-1800}"

if [[ ! "$SPEC" =~ ^[A-Z]{3}$ ]]; then
    echo "usage: scripts/opencode-harness/opencode-plan.sh <SPEC>" >&2
    exit 2
fi

if [[ ! -d "$ROOT/docs/spec-src/$SPEC" ]]; then
    echo "specification material not found: docs/spec-src/$SPEC" >&2
    exit 3
fi

cd "$ROOT"
exec timeout --foreground "$TIMEOUT" opencode run --agent lead --command spec-plan "$SPEC"

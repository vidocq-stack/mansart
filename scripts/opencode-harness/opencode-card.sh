#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
CARD="${1:-}"
TIMEOUT="${OPENCODE_TIMEOUT:-1800}"

if [[ ! "$CARD" =~ ^[A-Z]{3}/M[0-9]+-T[0-9]{3}$ ]]; then
    echo "usage: scripts/opencode-harness/opencode-card.sh <SPEC/Mn-Tnnn>" >&2
    exit 2
fi

cd "$ROOT"
exec timeout --foreground "$TIMEOUT" opencode run --print-logs --log-level INFO --agent lead --command card "$CARD"

#!/usr/bin/env bash
set -uo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
SPEC="${1:-}"
TIMEOUT="${OPENCODE_CARD_REVIEW_TIMEOUT:-900}"

if [[ ! "$SPEC" =~ ^[A-Z]{3}$ ]]; then
    echo "usage: scripts/opencode-harness/review-cards.sh <SPEC>" >&2
    exit 2
fi
if [[ ! -d "$ROOT/tasks/$SPEC" ]]; then
    echo "plan not found: tasks/$SPEC" >&2
    exit 3
fi

shopt -s nullglob
cards=("$ROOT/tasks/$SPEC"/M*-T*.md)
if (( ${#cards[@]} == 0 )); then
    echo "no cards found in tasks/$SPEC" >&2
    exit 4
fi

failed=0
total=0
for path in "${cards[@]}"; do
    card="$(basename "$path" .md)"
    total=$((total + 1))
    printf '\n=== review %s/%s (%d/%d) ===\n' "$SPEC" "$card" "$total" "${#cards[@]}"
    if ! (cd "$ROOT" && timeout --foreground "$TIMEOUT" \
        opencode run --print-logs --log-level INFO --agent lead \
        --command review-card-fix "$SPEC/$card"); then
        failed=$((failed + 1))
        printf 'FAILED %s/%s\n' "$SPEC" "$card" >&2
    fi
done

printf '\nreviewed=%d failed=%d\n' "$total" "$failed"
(( failed == 0 ))

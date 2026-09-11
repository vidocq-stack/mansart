#!/usr/bin/env bash
# Commit one card the way the workspace requires: Conventional Commits + card id,
# GPG-signed, DCO, Co-Authored-By naming the harness. A lead once committed
# "JKP: implement M1-T001 ..." — no type, no scope, no trailer — with STATUS
# edited by hand and code that never compiled. The message shape is not a
# judgement; a script owns it.
#
# Usage: scripts/card-commit.sh <XXX> <CARD> <type> <scope> "<what, imperative, lowercase>"
set -uo pipefail
H="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
CODE="${1:-}"; CARD="${2:-}"; TYPE="${3:-}"; SCOPE="${4:-}"; WHAT="${5:-}"
[ -n "$WHAT" ] || { echo "usage: card-commit.sh <XXX> <CARD> <feat|fix|test|refactor> <scope> \"<what>\""; exit 2; }
printf '%s' "$TYPE" | grep -qE '^(feat|fix|test|refactor|docs|chore)$' || { echo "type must be feat|fix|test|refactor|docs|chore"; exit 2; }
grep -qE "^\| $CARD \|" "$H/STATUS-$CODE.md" || { echo "$CARD has no Done row — run scripts/card-done.sh $CODE $CARD first"; exit 3; }
IMPL="$(python3 "$H/scripts/tck-module.py" "$CODE" --impl 2>/dev/null)"
cd "$H" || exit 3
git add -- "$IMPL" "STATUS-$CODE.md" 2>/dev/null
[ -n "$(git diff --cached --name-only)" ] || { echo "nothing staged under $IMPL or STATUS-$CODE.md"; exit 1; }
git diff --cached --name-only | grep -vE "^($IMPL/|STATUS-$CODE\.md$)" | grep -q . && { echo "staged files outside $IMPL: refusing"; git reset -q; exit 1; }
MSG="$TYPE($SCOPE): $WHAT [$CODE $CARD]

Co-Authored-By: mansart-harness (lead Qwen3.6-35B, impl Qwen3-Next-80B) <harness@vidocq.local>"
git commit -S -s -q -m "$MSG" || { echo "commit failed (signature?) — the card is NOT done"; exit 1; }
echo "committed $(git rev-parse --short HEAD): $TYPE($SCOPE): $WHAT [$CODE $CARD]"

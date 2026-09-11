#!/usr/bin/env bash
# Close an M1+ card: MEASURE the implementation module's tests, then record.
# A row enters STATUS only through this script, and only with numbers it read
# itself. A lead once edited the computed counts by hand and wrote a sentence
# as evidence for code that lived in a pom-packaged module and never compiled.
#
# Usage: scripts/card-done.sh <XXX> <CARD>        exit 0 = row written
set -uo pipefail
H="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
CODE="${1:-}"; CARD="${2:-}"
printf '%s' "$CODE" | grep -qE '^[A-Z]{3}$' || { echo "usage: card-done.sh <XXX> <CARD>"; exit 2; }
printf '%s' "$CARD" | grep -qE '^M[1-9][0-9]*-T[0-9]{3}$' || { echo "card must look like M1-T001 (M0 is closed by verify-m0.sh), got '$CARD'"; exit 2; }
IMPL="$(python3 "$H/scripts/tck-module.py" "$CODE" --impl 2>/dev/null)"
[ -n "$IMPL" ] && [ -f "$H/$IMPL/pom.xml" ] || { echo "no implementation module ($IMPL) — M0-T007 first"; exit 3; }
grep -q "^| $CARD |" "$H/TASKS-$CODE.md" || { echo "$CARD is not in TASKS-$CODE.md"; exit 2; }
grep -qE "^\| $CARD \|" "$H/STATUS-$CODE.md" && { echo "$CARD already has a Done row"; exit 0; }

# The test that closes the card must NAME it. A test nobody can tie to a card
# closes nothing.
if ! grep -rl "$CARD" "$H/$IMPL/src/test" 2>/dev/null | grep -q .; then
    echo "no test under $IMPL/src/test mentions $CARD — a card is closed by a test that names it (a comment or the test name is enough)"
    exit 4
fi
echo "== card-done $CODE $CARD: measuring $IMPL"
( cd "$H" && "$H/scripts/build.sh" -pl "$IMPL" test ) >/dev/null 2>&1; rc=$?
LOG="$(ls -t "$H"/target/build-logs/*.log 2>/dev/null | head -1)"
SUM="$(grep -oE 'Tests run: [0-9]+, Failures: [0-9]+, Errors: [0-9]+, Skipped: [0-9]+' "$LOG" 2>/dev/null | tail -1)"
RUN=$(echo "$SUM" | grep -oE 'run: [0-9]+' | grep -oE '[0-9]+'); FAIL=$(echo "$SUM" | grep -oE 'Failures: [0-9]+' | grep -oE '[0-9]+'); ERR=$(echo "$SUM" | grep -oE 'Errors: [0-9]+' | grep -oE '[0-9]+')
echo "   build exit $rc — ${SUM:-no test summary in $(basename "$LOG")}"
if [ $rc -ne 0 ] || [ -z "$RUN" ] || [ "$RUN" -eq 0 ] || [ "$FAIL" -ne 0 ] || [ "$ERR" -ne 0 ]; then
    echo "   NOT DONE: the card closes only on exit 0 with tests run>0, failures=0, errors=0. Log: $LOG"
    exit 1
fi
SHA="$(git -C "$H" rev-parse --short HEAD 2>/dev/null)"
ROW="| $CARD | $(date '+%Y-%m-%d %H:%M') | card-done.sh: tests run=$RUN failures=$FAIL errors=$ERR in $IMPL; base $SHA |"
python3 - "$H/STATUS-$CODE.md" "$ROW" <<'PY'
import sys,io
p,row=sys.argv[1],sys.argv[2]; s=io.open(p,encoding='utf-8').read()
marker="| — | — | *(none yet)* |"
if marker in s: s=s.replace(marker,row)
else:
    i=s.index("| Card | Date | Evidence |"); j=s.index("\n\n",i) if "\n\n" in s[i:] else len(s)
    s=s[:j]+"\n"+row+s[j:]
io.open(p,'w',encoding='utf-8').write(s)
PY
echo "   row written: $ROW"
VERIFY_M0_NO_RUN=1 VERIFY_M0_NO_SYNC=1 "$H/scripts/spec-tasks.sh" "$CODE" >/dev/null 2>&1   # recompute counts, keep rows
grep -E "^\| $CARD \|" "$H/STATUS-$CODE.md" >/dev/null && echo "   STATUS-$CODE.md refreshed" || { echo "   row LOST on refresh — report this"; exit 5; }

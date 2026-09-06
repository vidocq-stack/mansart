#!/usr/bin/env bash
#
# Measurement, not construction. Runs the build with tests and emits ONLY a
# numeric report, parsed from surefire/failsafe XML rather than from console
# prose — the console can say "BUILD SUCCESS" while a suite was skipped.
#
# This is what the `verify` agent runs, and the only source of the numbers
# that may be written into STATUS.md. Over 3 days, 24 cards were marked DONE
# while the conformance counter stayed frozen at 2/0/2; a measurement has to
# come from something with no stake in the result.
#
# Usage:
#   ./scripts/verify.sh                 # unit tests across the reactor
#   ./scripts/verify.sh -pl <module>    # extra Maven args pass through
#
# Exit code: Maven's (0 = build green; non-zero = build or tests red).
# Full output: target/agent-verify.log. Previous report, for the delta:
# target/agent-verify-last.txt.

set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
LOG_DIR="${ROOT}/target"
LOG="${LOG_DIR}/agent-verify.log"
LAST="${LOG_DIR}/agent-verify-last.txt"
TAIL_LINES="${VERIFY_TAIL_LINES:-60}"

mkdir -p "${LOG_DIR}"

if [[ -x "${ROOT}/mvnw" ]]; then
    MVN=("${ROOT}/mvnw")
else
    MVN=(mvn)
fi

if [[ $# -eq 0 ]]; then
    set -- test
fi

printf '=== %s :: %s %s\n' "$(date -u '+%Y-%m-%dT%H:%M:%SZ')" "${MVN[*]}" "$*" > "${LOG}"

status=0
"${MVN[@]}" -ntp "$@" >> "${LOG}" 2>&1 || status=$?

echo "--- last ${TAIL_LINES} lines of ${LOG#"${ROOT}/"} ---"
tail -n "${TAIL_LINES}" "${LOG}"
echo "--- end ---"

# Counts come from the XML reports, not from the console summary.
read -r TESTS FAILURES ERRORS SKIPPED <<<"$(
    find "${ROOT}" -path '*/target/*-reports/TEST-*.xml' -newer "${LOG}" -print0 2>/dev/null \
        | xargs -0 -r cat 2>/dev/null \
        | tr '>' '>\n' \
        | awk '
            /<testsuite / {
                t=f=e=s=0
                if (match($0, /tests="[0-9]+"/))      { t = substr($0, RSTART+7, RLENGTH-8) }
                if (match($0, /failures="[0-9]+"/))   { f = substr($0, RSTART+10, RLENGTH-11) }
                if (match($0, /errors="[0-9]+"/))     { e = substr($0, RSTART+8, RLENGTH-9) }
                if (match($0, /skipped="[0-9]+"/))    { s = substr($0, RSTART+9, RLENGTH-10) }
                T+=t; F+=f; E+=e; S+=s
            }
            END { printf "%d %d %d %d", T+0, F+0, E+0, S+0 }
        '
)"
PASSED=$(( TESTS - FAILURES - ERRORS - SKIPPED ))
[[ ${PASSED} -lt 0 ]] && PASSED=0

if [[ ${status} -eq 0 ]]; then BUILD=GREEN; else BUILD=RED; fi

REPORT="BUILD=${BUILD} exit_code=${status} tests=${TESTS} passed=${PASSED} failed=${FAILURES} errors=${ERRORS} skipped=${SKIPPED}"

echo "=== VERIFY REPORT ==="
echo "${REPORT}"

if [[ -f "${LAST}" ]]; then
    echo "PREVIOUS: $(cat "${LAST}")"
    prev_passed=$(sed -n 's/.*passed=\([0-9]*\).*/\1/p' "${LAST}")
    if [[ -n "${prev_passed}" ]]; then
        echo "DELTA_PASSED=$(( PASSED - prev_passed ))"
    fi
else
    echo "PREVIOUS: (none — first measured run)"
fi

if [[ ${FAILURES} -gt 0 || ${ERRORS} -gt 0 ]]; then
    echo "--- failing tests ---"
    grep -hoE '<testcase name="[^"]+" classname="[^"]+"' \
        $(find "${ROOT}" -path '*/target/*-reports/TEST-*.xml' -newer "${LOG}" 2>/dev/null) 2>/dev/null \
        | head -n 40 || true
fi

echo "${REPORT}" > "${LAST}"
exit "${status}"

#!/usr/bin/env bash
#
# Measurement, not construction. Runs the build with tests and emits ONLY a
# numeric report, parsed from surefire/failsafe XML rather than from console
# prose — the console can say "BUILD SUCCESS" while a suite was skipped.
#
# This is what the `verify` agent runs, and the only source of the numbers that
# may be written into STATUS.md. Over 3 days, 24 cards were marked DONE while
# the conformance counter stayed frozen at 2/0/2; a measurement has to come
# from something with no stake in the result.
#
# Usage:
#   ./scripts/verify.sh                 # unit tests across the reactor
#   ./scripts/verify.sh -pl <module>    # extra Maven args pass through
#
# Exit code: Maven's (0 = build green), or 124 on the wall-clock limit.
# Full output: .agent-logs/verify.log   Previous report: .agent-logs/last.txt
#
# NOTE ON TESTCONTAINERS: mansart-persistence-tests uses Testcontainers, so a
# full run needs a reachable Docker daemon. A sleeping or unreachable daemon is
# the most likely cause of a run that appears alive but never progresses —
# check `docker info` before blaming the build, and watch .agent-logs/verify.log.

set -euo pipefail

# shellcheck source=_agent_build_lib.sh
source "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/_agent_build_lib.sh"

LOG="$(agent_log_path verify)"
LAST="${LOG_DIR}/last.txt"
TAIL_LINES="${VERIFY_TAIL_LINES:-60}"

# Append the default goal unless one was supplied. Options alone are not a
# goal — see agent_has_goal.
if ! agent_has_goal "$@"; then
    set -- "$@" test
fi

agent_run_maven "${LOG}" "$@"

agent_print_tail "${LOG}" "${TAIL_LINES}"

# Counts come from the XML reports, not from the console summary. The log is
# created before Maven starts and lives outside target/, so it is a stable
# "written during this run" reference even across a `clean`.
read -r TESTS FAILURES ERRORS SKIPPED <<<"$(
    find "${ROOT}" -path '*/target/*-reports/TEST-*.xml' -newer "${AGENT_STAMP}" -print0 2>/dev/null \
        | xargs -0 -r cat 2>/dev/null \
        | tr '>' '>\n' \
        | awk '
            /<testsuite / {
                t=f=e=s=0
                if (match($0, /tests="[0-9]+"/))    { t = substr($0, RSTART+7,  RLENGTH-8)  }
                if (match($0, /failures="[0-9]+"/)) { f = substr($0, RSTART+10, RLENGTH-11) }
                if (match($0, /errors="[0-9]+"/))   { e = substr($0, RSTART+8,  RLENGTH-9)  }
                if (match($0, /skipped="[0-9]+"/))  { s = substr($0, RSTART+9,  RLENGTH-10) }
                T+=t; F+=f; E+=e; S+=s
            }
            END { printf "%d %d %d %d", T+0, F+0, E+0, S+0 }
        '
)"
PASSED=$(( TESTS - FAILURES - ERRORS - SKIPPED ))
[[ ${PASSED} -lt 0 ]] && PASSED=0

if [[ "${AGENT_TIMED_OUT}" == "1" ]]; then
    BUILD=TIMEOUT
elif [[ ${AGENT_STATUS} -eq 0 ]]; then
    BUILD=GREEN
else
    BUILD=RED
fi

REPORT="BUILD=${BUILD} exit_code=${AGENT_STATUS} tests=${TESTS} passed=${PASSED} failed=${FAILURES} errors=${ERRORS} skipped=${SKIPPED}"

echo "=== VERIFY REPORT ==="
echo "${REPORT}"

if [[ "${BUILD}" == "TIMEOUT" ]]; then
    echo "The run was KILLED at ${BUILD_TIMEOUT}s, not finished. These counts are"
    echo "partial and must NOT be recorded. Check that Docker is reachable"
    echo "(Testcontainers), then re-run — or raise BUILD_TIMEOUT deliberately."
fi

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
    find "${ROOT}" -path '*/target/*-reports/TEST-*.xml' -newer "${AGENT_STAMP}" -print0 2>/dev/null \
        | xargs -0 -r grep -hoE '<testcase name="[^"]+" classname="[^"]+"' 2>/dev/null \
        | head -n 40 || true
fi

# Only a run that actually measured something may become the new baseline.
# A killed run, or a build that died before any test executed, produces
# tests=0 — recording that would destroy the delta reference and make the next
# run look like a huge regression or a huge win, neither of which happened.
if [[ "${BUILD}" != "TIMEOUT" && ${TESTS} -gt 0 ]]; then
    echo "${REPORT}" > "${LAST}"
else
    echo "NOT RECORDED as the new baseline (no test executed) — ${LAST#"${ROOT}/"} left untouched."
fi

exit "${AGENT_STATUS}"

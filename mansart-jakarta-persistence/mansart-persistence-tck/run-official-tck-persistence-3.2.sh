#!/usr/bin/env bash
#
# Runs the official Jakarta Persistence 3.2 TCK against the Mansart
# implementation, and reports a number that can be trusted.
#
# WHY IT WAS REWRITTEN. Across 66 recorded invocations, this script has never
# once run the TCK suite. Maven's own "Total time" for those runs was 0.4s to
# 3.5s (median 1.5s) and the test counts were 0, 1 or 2 — never the ~1745 test
# methods the TCK contains. The usual output was `Could not find the selected
# project`. Both Maven calls ran under `-q`, so a startup failure and a passing
# run looked almost identical, and nothing distinguished "the TCK passed" from
# "the TCK never started".
#
# The old version also had no wall-clock limit, left stdin open, and ran
# without batch mode — the combination behind a run once left apparently alive
# for 90 minutes that completed in minutes after a restart.
#
# Usage:
#   ./run-official-tck-persistence-3.2.sh            # profile tck-run (H2)
#   ./run-official-tck-persistence-3.2.sh tck-pg     # PostgreSQL
#   ./run-official-tck-persistence-3.2.sh tck-sig    # signature test
#   BUILD_TIMEOUT=7200 ./run-official-tck-persistence-3.2.sh
#
# Exit code: 0 only if the build is green AND at least one test actually ran.
# Logs: .agent-logs/tck-install.log and .agent-logs/tck-<profile>.log

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "${SCRIPT_DIR}")"

# Shared plumbing: batch mode, stdin closed, wall-clock limit, logs outside
# target/ so `mvn clean` cannot delete them mid-run.
# shellcheck source=../../scripts/_agent_build_lib.sh
source "${PROJECT_DIR}/../scripts/_agent_build_lib.sh"

PROFILE="${1:-tck-run}"
# The TCK is ~1745 test methods across 269 client classes; it legitimately
# takes far longer than a unit-test run. Still bounded, so a stall is a
# reported failure rather than an open-ended wait.
: "${BUILD_TIMEOUT:=3600}"

echo "Jakarta Persistence 3.2 TCK — profile: ${PROFILE}"
echo "Wall-clock limit: ${BUILD_TIMEOUT}s. Follow progress with:"
echo "  tail -f ${LOG_DIR#"${ROOT}/"}/${PROFILE}.log"
echo

# --------------------------------------------------------------------------
# Step 1 — install the implementation the TCK will run against.
# --------------------------------------------------------------------------
INSTALL_LOG="$(agent_log_path tck-install)"
echo "[1/2] Installing Mansart modules…"
agent_run_maven "${INSTALL_LOG}" -f "${PROJECT_DIR}/pom.xml" install -DskipTests \
    -pl '!mansart-persistence-maven-plugin,!mansart-persistence-tests,!mansart-persistence-external-it'

if [[ "${AGENT_TIMED_OUT}" == "1" ]]; then
    agent_print_tail "${INSTALL_LOG}" 40
    echo "TCK_RESULT=TIMEOUT stage=install exit_code=${AGENT_STATUS} limit=${BUILD_TIMEOUT}s"
    exit "${AGENT_STATUS}"
fi
if [[ ${AGENT_STATUS} -ne 0 ]]; then
    agent_print_tail "${INSTALL_LOG}" 40
    echo "TCK_RESULT=INSTALL_FAILED exit_code=${AGENT_STATUS}"
    echo "The TCK did not run. This is not a conformance result — the"
    echo "implementation could not be installed for it to run against."
    exit "${AGENT_STATUS}"
fi

# --------------------------------------------------------------------------
# Step 2 — run the TCK.
# --------------------------------------------------------------------------
RUN_LOG="$(agent_log_path "${PROFILE}")"
echo "[2/2] Running the TCK with profile ${PROFILE}…"
agent_run_maven "${RUN_LOG}" -f "${SCRIPT_DIR}/pom.xml" verify "-P${PROFILE}"

agent_print_tail "${RUN_LOG}" "${TCK_TAIL_LINES:-60}"

# Counts come from the XML reports, never from console prose: a build can print
# BUILD SUCCESS while every suite was skipped.
read -r TESTS FAILURES ERRORS SKIPPED <<<"$(
    find "${SCRIPT_DIR}" -path '*/target/*-reports/TEST-*.xml' -newer "${AGENT_STAMP}" -print0 2>/dev/null \
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

echo "=== TCK REPORT ==="
echo "TCK_PROFILE=${PROFILE}"

if [[ "${AGENT_TIMED_OUT}" == "1" ]]; then
    echo "TCK_RESULT=TIMEOUT exit_code=${AGENT_STATUS} limit=${BUILD_TIMEOUT}s"
    echo "Killed, not finished. Partial counts below are NOT a conformance"
    echo "result and must not be recorded: tests=${TESTS} passed=${PASSED}"
    exit "${AGENT_STATUS}"
fi

echo "TCK_TESTS=${TESTS} passed=${PASSED} failed=${FAILURES} errors=${ERRORS} skipped=${SKIPPED}"

# The distinction this script exists to make. 66 recorded runs of the previous
# version reported nothing that separated these two cases.
if [[ ${TESTS} -eq 0 ]]; then
    echo "TCK_RESULT=DID_NOT_RUN exit_code=${AGENT_STATUS}"
    echo "Zero tests executed — the TCK did not start. Whatever the build"
    echo "status says, this is NOT '0 failures' and NOT progress. Check"
    echo "${RUN_LOG#"${ROOT}/"} for the real error, typically an unresolved TCK"
    echo "artifact or a persistence unit still pointing at a stub."
    exit 1
fi

if [[ ${AGENT_STATUS} -eq 0 && ${FAILURES} -eq 0 && ${ERRORS} -eq 0 ]]; then
    echo "TCK_RESULT=PASS exit_code=0"
else
    echo "TCK_RESULT=FAIL exit_code=${AGENT_STATUS}"
    find "${SCRIPT_DIR}" -path '*/target/*-reports/TEST-*.xml' -newer "${AGENT_STAMP}" -print0 2>/dev/null \
        | xargs -0 -r grep -hoE '<testcase name="[^"]+" classname="[^"]+"' 2>/dev/null \
        | head -n 40 || true
fi

exit "${AGENT_STATUS}"

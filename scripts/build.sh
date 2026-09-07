#!/usr/bin/env bash
#
# The ONLY sanctioned way for an agent to run Maven in this repository.
#
# Why this exists: over 3 days, 262 Maven invocations were logged with
# exit_code: 0 while 38 of their outputs contained BUILD FAILURE, 3 contained
# compilation errors and 17 contained test failures. The pattern used
# everywhere was `mvn ... 2>&1 | tail -50`, and in a pipeline `$?` is the
# status of the LAST command — `tail`, which always succeeds. Maven's status
# was discarded before anyone could read it, so the rule "only commit if the
# build passes" was not mechanically checkable.
#
# See scripts/_agent_build_lib.sh for the three further defects fixed after
# this script's first week in service — in particular, `mvn clean` used to
# delete this script's own log, which made it report failure on success.
#
# Usage:
#   ./scripts/build.sh                      # full build, tests skipped
#   ./scripts/build.sh test                 # any Maven goals/args pass through
#   ./scripts/build.sh -pl <module> -am test
#   ./scripts/build.sh -f /path/to/other/pom.xml clean compile
#
# Exit code: Maven's, or 124 on the wall-clock limit.
# Full output: .agent-logs/build.log  (follow it with tail -f)
# Console: last 60 lines, then an explicit BUILD_RESULT= line.

set -euo pipefail

# shellcheck source=_agent_build_lib.sh
source "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/_agent_build_lib.sh"

LOG="$(agent_log_path build)"
TAIL_LINES="${BUILD_TAIL_LINES:-60}"

if [[ $# -eq 0 ]]; then
    set -- clean install -DskipTests
fi

agent_run_maven "${LOG}" "$@"

agent_print_tail "${LOG}" "${TAIL_LINES}"

if [[ "${AGENT_TIMED_OUT}" == "1" ]]; then
    echo "BUILD_RESULT=TIMEOUT exit_code=${AGENT_STATUS} limit=${BUILD_TIMEOUT}s"
    echo "The build was killed, not finished. Do not treat this as a failure of"
    echo "the code: inspect ${LOG#"${ROOT}/"} for where it stopped."
elif [[ ${AGENT_STATUS} -eq 0 ]]; then
    echo "BUILD_RESULT=SUCCESS exit_code=0"
else
    echo "BUILD_RESULT=FAILURE exit_code=${AGENT_STATUS}"
    grep -nE '^\[ERROR\]|BUILD FAILURE|COMPILATION ERROR' "${LOG}" | head -n 20 || true
fi

exit "${AGENT_STATUS}"

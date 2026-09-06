#!/usr/bin/env bash
#
# The ONLY sanctioned way for an agent to run Maven in this repository.
#
# Why this exists: over 3 days on ybl/jpa-vibe, 262 Maven invocations were
# logged with exit_code: 0 while 38 of their outputs contained BUILD FAILURE,
# 3 contained compilation errors and 17 contained test failures. Cause: the
# pattern used everywhere was
#
#     mvn ... 2>&1 | tail -50
#
# In a pipeline, `$?` is the exit status of the LAST command — `tail`, which
# always succeeds. Maven's status was discarded before anyone could read it,
# so an agent had to parse prose to know whether it had just broken the build,
# and the rule "only commit if the build passes" was not mechanically
# checkable.
#
# The fix is `set -o pipefail` plus an explicit capture of Maven's own status.
#
# Usage:
#   ./scripts/build.sh                      # full build, tests skipped
#   ./scripts/build.sh test                 # any Maven goals/args pass through
#   ./scripts/build.sh -pl mansart-jakarta-persistence/mansart-persistence-core test
#   ./scripts/build.sh -f /path/to/other/pom.xml clean compile
#
# Exit code: Maven's. Full output: target/agent-build.log. Console: last 60
# lines, plus an explicit verdict line an agent can match on.

set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
LOG_DIR="${ROOT}/target"
LOG="${LOG_DIR}/agent-build.log"
TAIL_LINES="${BUILD_TAIL_LINES:-60}"

mkdir -p "${LOG_DIR}"

if [[ -x "${ROOT}/mvnw" ]]; then
    MVN=("${ROOT}/mvnw")
else
    MVN=(mvn)
fi

# Default goals when none are supplied.
if [[ $# -eq 0 ]]; then
    set -- clean install -DskipTests
fi

printf '=== %s :: %s %s\n' "$(date -u '+%Y-%m-%dT%H:%M:%SZ')" "${MVN[*]}" "$*" > "${LOG}"

# `|| status=$?` keeps `set -e` from killing us before the verdict is printed.
# Nothing is piped here, so the status is Maven's own.
status=0
"${MVN[@]}" -ntp "$@" >> "${LOG}" 2>&1 || status=$?

echo "--- last ${TAIL_LINES} lines of ${LOG#"${ROOT}/"} ---"
tail -n "${TAIL_LINES}" "${LOG}"
echo "--- end ---"

if [[ ${status} -eq 0 ]]; then
    echo "BUILD_RESULT=SUCCESS exit_code=0"
else
    echo "BUILD_RESULT=FAILURE exit_code=${status}"
    # Surface the actual cause without making the agent read the whole log.
    grep -nE '^\[ERROR\]|BUILD FAILURE|COMPILATION ERROR' "${LOG}" | head -n 20 || true
fi

exit "${status}"

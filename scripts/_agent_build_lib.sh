#!/usr/bin/env bash
#
# Shared plumbing for scripts/build.sh and scripts/verify.sh.
#
# Three defects fixed here, all observed in real sessions:
#
# 1. THE LOG LIVED IN target/ AND `mvn clean` DELETED IT.
#    The default goals are `clean install -DskipTests`; clean wipes the root
#    reactor's target/, which is where the script was writing its own log. The
#    consequences compounded:
#      - Maven's output kept being appended to an unlinked inode — invisible;
#      - `tail "$LOG"` then failed with "No such file or directory";
#      - under `set -e` that aborted the script BEFORE the BUILD_RESULT line;
#      - so the script exited 1 on a build that had actually SUCCEEDED.
#    That is the exact inverse of the bug these scripts exist to prevent.
#    Observed verbatim in a session transcript:
#      bash failed: './scripts/build.sh' Return code: 1
#      Stderr: tail: .../target/agent-build.log: No such file or directory
#    Fix: logs go to .agent-logs/, which no Maven goal touches.
#
# 2. NOTHING BOUNDED THE RUN, AND NOTHING CLOSED STDIN.
#    A run was once left apparently alive for 1h30 and completed in minutes
#    after a restart. Neither script could tell "slow" from "wedged". Maven now
#    runs with stdin closed (</dev/null — a build must never wait on input),
#    in batch mode (-B), and under a wall-clock limit that fails loudly.
#
# 3. A STALL PRODUCED NO OUTPUT AT ALL.
#    The log is written continuously, so `tail -f .agent-logs/<name>.log` from
#    another terminal shows whether a run is progressing or stuck.

set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
# Deliberately NOT under target/: `mvn clean` deletes that directory.
LOG_DIR="${ROOT}/.agent-logs"

# Wall-clock limit for a single Maven invocation. A build that exceeds it is
# reported as a failure with a distinct verdict rather than hanging silently.
# Override with BUILD_TIMEOUT=<seconds>, or 0 to disable.
: "${BUILD_TIMEOUT:=1800}"

agent_log_path() {
    mkdir -p "${LOG_DIR}"
    printf '%s/%s.log' "${LOG_DIR}" "$1"
}

agent_mvn_cmd() {
    if [[ -x "${ROOT}/mvnw" ]]; then printf '%s' "${ROOT}/mvnw"; else printf 'mvn'; fi
}

# Resolve a timeout wrapper if one exists. Not fatal when absent — the run is
# then unbounded and we say so, rather than pretending it is guarded.
_agent_timeout_prefix() {
    if [[ "${BUILD_TIMEOUT}" == "0" ]]; then return 0; fi
    if command -v timeout  >/dev/null 2>&1; then printf 'timeout %s' "${BUILD_TIMEOUT}"; return 0; fi
    if command -v gtimeout >/dev/null 2>&1; then printf 'gtimeout %s' "${BUILD_TIMEOUT}"; return 0; fi
}

# agent_run_maven <logfile> <maven args...>
# Echoes nothing; sets AGENT_STATUS, AGENT_TIMED_OUT and AGENT_STAMP.
#
# AGENT_STAMP is a file whose mtime is fixed at the moment the run starts. Use
# it — never the log — as the `find -newer` reference when collecting surefire
# XML: the log is appended to for the whole run, so its mtime ends up NEWER
# than every report, and `-newer "$log"` silently matches nothing. That defect
# made verify.sh report tests=0 on runs where surefire had plainly printed
# "Tests run: 2, Failures: 1".
agent_run_maven() {
    local log="$1"; shift
    local mvn; mvn="$(agent_mvn_cmd)"
    local -a prefix=()
    local p; p="$(_agent_timeout_prefix)"
    [[ -n "${p}" ]] && read -r -a prefix <<<"${p}"

    {
        printf '=== %s\n' "$(date -u '+%Y-%m-%dT%H:%M:%SZ')"
        printf '=== %s -B -ntp %s\n' "${mvn}" "$*"
        if [[ ${#prefix[@]} -gt 0 ]]; then
            printf '=== wall-clock limit: %ss\n' "${BUILD_TIMEOUT}"
        else
            printf '=== NO wall-clock limit (no timeout/gtimeout on PATH) — this run is unbounded\n'
        fi
        printf '=== follow with: tail -f %s\n\n' "${log}"
    } > "${log}"

    # Fixed reference for "written during this run". Must be created BEFORE
    # Maven starts and never touched again.
    AGENT_STAMP="${log}.stamp"
    : > "${AGENT_STAMP}"

    AGENT_STATUS=0
    # -B: batch mode, so no plugin can decide to prompt.
    # </dev/null: a build must never block waiting on input. Under an agent,
    # stdin is a pipe that never closes, so a read there hangs forever.
    "${prefix[@]}" "${mvn}" -B -ntp "$@" </dev/null >> "${log}" 2>&1 || AGENT_STATUS=$?

    AGENT_TIMED_OUT=0
    # GNU timeout reports 124 on expiry.
    if [[ ${#prefix[@]} -gt 0 && ${AGENT_STATUS} -eq 124 ]]; then
        AGENT_TIMED_OUT=1
        printf '\n=== KILLED after %ss by the wall-clock limit\n' "${BUILD_TIMEOUT}" >> "${log}"
    fi
}

# agent_has_goal <args...>
# True if the argument list already names a Maven goal or lifecycle phase.
#
# Without this, the documented usage `./scripts/verify.sh -pl <module>` passes
# only options, `$# -eq 0` is false, the default goal is never appended, and
# Maven fails with "No goals have been specified for this build" — a confusing
# error for a command straight out of the script's own header. Option VALUES
# are not goals, so the options that take a separate value are skipped.
agent_has_goal() {
    local skip_next=0 a
    for a in "$@"; do
        if [[ ${skip_next} -eq 1 ]]; then skip_next=0; continue; fi
        case "${a}" in
            -f|--file|-pl|--projects|-P|--activate-profiles|-s|--settings|-gs|\
            --global-settings|-T|--threads|-l|--log-file|-b|--builder|-rf|\
            --resume-from|-t|--toolchains)
                skip_next=1; continue ;;
            -*) continue ;;
            *) return 0 ;;
        esac
    done
    return 1
}

# agent_print_tail <logfile> [lines]
# Never aborts the caller: a missing or unreadable log must not swallow the
# verdict — that was defect 1.
agent_print_tail() {
    local log="$1" lines="${2:-60}"
    printf -- '--- last %s lines of %s ---\n' "${lines}" "${log#"${ROOT}/"}"
    if [[ -f "${log}" ]]; then
        tail -n "${lines}" "${log}" || true
    else
        printf '(log file missing: %s)\n' "${log}"
    fi
    printf -- '--- end ---\n'
}

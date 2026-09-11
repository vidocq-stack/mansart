#!/usr/bin/env bash
# Runner for the official Jakarta Persistence 3.2 TCK against the Mansart provider.
# See README.md for the complete prerequisites (TCK install, M2 setup, …).

set -euo pipefail

cd "$(dirname "$0")"

# Activate the toolchain pinned by ../.sdkmanrc — Java 25 + Maven 3.9.16
if [ -z "${SDKMAN_DIR:-}" ] && [ -d "$HOME/.sdkman" ]; then
    SDKMAN_DIR="$HOME/.sdkman"
fi
if [ -n "${SDKMAN_DIR:-}" ] && [ -s "$SDKMAN_DIR/bin/sdkman-init.sh" ]; then
    # shellcheck disable=SC1091
    source "$SDKMAN_DIR/bin/sdkman-init.sh"
    (cd .. && sdk env > /dev/null 2>&1) || true
fi

# jakarta.tck:persistence-tck-spec-tests:3.2.1 is on Maven Central — Maven will fetch it
# automatically when the `tck-run` profile is active. No local install needed.

# Verify the Mansart reactor is installed.
if [ ! -f "$HOME/.m2/repository/io/vidocq/mansart/mansart-jakarta-persistence/0.4.0-SNAPSHOT/mansart-jakarta-persistence-0.4.0-SNAPSHOT.jar" ]; then
    echo "[INFO] Mansart reactor not yet installed in local M2 — running 'mvn install -DskipTests' from ../"
    (cd .. && mvn -ntp install -DskipTests)
fi

case "${1:-}" in
    --all|"")
        # Default: run the official Jakarta Persistence 3.2 TCK on H2 in-memory.
        mvn -ntp -Ptck-run test -DfailIfNoTests=false
        ;;
    -Dtest=*)
        mvn -ntp -Ptck-run test "$@"
        ;;
    *)
        mvn -ntp -Ptck-run test "$@"
        ;;
esac

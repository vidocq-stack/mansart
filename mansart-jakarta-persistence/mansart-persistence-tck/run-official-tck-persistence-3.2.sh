#!/bin/bash
#
# run-official-tck-persistence-3.2.sh
#
# Runs the official Jakarta Persistence 3.2 TCK against Mansart implementation.
#

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"

PROFILE="${1:-tck-run}"

echo "Jakarta Persistence 3.2 TCK Runner - Profile: $PROFILE"

cd "$SCRIPT_DIR"
cd "$PROJECT_DIR"
mvn install -DskipTests -q -pl "!mansart-persistence-maven-plugin,!mansart-persistence-tests,!mansart-persistence-external-it"

cd "$SCRIPT_DIR"
echo "Running TCK with profile: $PROFILE"
mvn verify -P$PROFILE ${MVN_OPTS:--q}

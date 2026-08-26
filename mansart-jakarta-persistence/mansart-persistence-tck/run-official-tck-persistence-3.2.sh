#!/usr/bin/env bash
# Runner for the official Jakarta Persistence 3.2 TCK against the Mansart provider.
# See README.md for the complete prerequisites (TCK install, M2 setup, …).

set -euo pipefail

cd "$(dirname "$0")"

# Activate the toolchain pinned by ../.sdkmanrc — Java 25 + Maven 3.9.16
# (no-op when there is no .sdkmanrc — Java 25 + Maven 3.9.16 are already on PATH)
if [ -f "$HOME/.sdkman" ]; then
    SDKMAN_DIR="$HOME/.sdkman"
    source "$SDKMAN_DIR/bin/sdkman-init.sh" 2>/dev/null
    if [ -d .. -a -f ../.sdkmanrc ]; then
        (cd .. && sdk env > /dev/null 2>&1) || true
    fi
fi

# Jakarta Persistence TCK 3.2.1 is on Maven Central — Maven will fetch it
# automatically when the `tck-run` profile is active. No local install needed.

# Verify the Mansart reactor is installed.
if [ ! -f "$HOME/.m2/repository/io/vidocq/mansart/mansart-persistence-core/0.3.0-SNAPSHOT/mansart-persistence-core-0.3.0-SNAPSHOT.jar" ]; then
    echo "[INFO] Mansart reactor not yet installed in local M2 — running 'mvn install -DskipTests' from ../"
    (cd .. && mvn -ntp install -DskipTests)
fi

case "${1:-}" in
    --pg|--postgres|--postgresql)
        # M0-5 — Run the TCK against PostgreSQL via Testcontainers. Requires Docker on
        # the host.  Pulls postgres:17-alpine on first run.
        #
        # JP-06 — before Maven starts, manage a named PostgreSQL container and
        # execute the official DDL (extracted from the TCK distribution into
        # src/test/resources/sql/postgresql/) so that setup*Data succeeds.
        shift || true

        # Derive paths relative to this script's directory
        TCK_PG_SCRIPTS_DIR="src/test/resources/sql/postgresql"
        PG_CONTAINER="mansart-pg-tck"
        PG_DB="mansart_tck"
        PG_USER="mansart"
        PG_PASS="mansart"
        JDBC_URL="jdbc:postgresql://localhost:5432/${PG_DB}"

        # Start or reuse the PostgreSQL container.
        if ! docker ps --format '{{.Names}}' | grep "^${PG_CONTAINER}$" > /dev/null 2>&1; then
            echo "[INFO] Starting PostgreSQL container '${PG_CONTAINER}' ..."
            docker run -d \
                --name "${PG_CONTAINER}" \
                -e "POSTGRES_DB=${PG_DB}" \
                -e "POSTGRES_USER=${PG_USER}" \
                -e "POSTGRES_PASSWORD=${PG_PASS}" \
                -p 5432:5432 \
                postgres:17-alpine > /dev/null
            # Wait until the server accepts connections.
            echo "[INFO] Waiting for PostgreSQL to become ready ..."
            for _i in $(seq 1 30); do
                if docker exec "${PG_CONTAINER}" pg_isready -U "${PG_USER}" > /dev/null 2>&1; then
                    break
                fi
                sleep 1
            done
        fi

        # Execute the official DDL (schema + stored procedures) so that
        # every TCK Client's setup*Data method has tables to work with.
        DDL_FILE="${TCK_PG_SCRIPTS_DIR}/postgresql.ddl.persistence.sql"
        SPROC_FILE="${TCK_PG_SCRIPTS_DIR}/postgresql.ddl.persistence.sprocs.sql"

        if [ -f "${DDL_FILE}" ]; then
            echo "[INFO] Executing DDL against PostgreSQL container ..."
            docker exec -i "${PG_CONTAINER}" psql \
                -U "${PG_USER}" -d "${PG_DB}" \
                -f "/dev/stdin" < "${DDL_FILE}" > /dev/null 2>&1
        fi

        if [ -f "${SPROC_FILE}" ]; then
            echo "[INFO] Executing stored-procedure DDL ..."
            docker exec -i "${PG_CONTAINER}" psql \
                -U "${PG_USER}" -d "${PG_DB}" \
                -f "/dev/stdin" < "${SPROC_FILE}" > /dev/null 2>&1
        fi

        # Run Maven — the JDBC URL of the pre-started container is passed
        # so that the Arquillian DataSourceProducer connects to it instead
        # of creating its own Testcontainers instance.
        mvn -ntp -Ptck-run,tck-pg \
            -Dmansart.tck.pg.jdbc.url="${JDBC_URL}" \
            test -DfailIfNoTests=false "$@"
        ;;
    --sig|--signature)
        # Activate the SignatureTests subset on top of the entity TCK (H2 by default).
        shift || true
        mvn -ntp -Ptck-run,tck-sig test -DfailIfNoTests=false "$@"
        ;;
    --full|--all-suites)
        # Entity + Signature TCK. Choose H2 (default) or PG via env var:
        # PG=1 ./run-official-tck-persistence-3.2.sh --full
        shift || true
        if [ "${PG:-0}" = "1" ]; then
            mvn -ntp -Ptck-run,tck-pg,tck-sig test -DfailIfNoTests=false "$@"
        else
            mvn -ntp -Ptck-run,tck-sig test -DfailIfNoTests=false "$@"
        fi
        ;;
    --all|all|"")
        # Default: run the official Jakarta Persistence 3.2 TCK spec-tests on H2 in-memory.
        mvn -ntp -Ptck-run test -DfailIfNoTests=false
        ;;
    -Dtest=*)
        mvn -ntp -Ptck-run test "$@"
        ;;
    *)
        mvn -ntp -Ptck-run test "$@"
        ;;
esac

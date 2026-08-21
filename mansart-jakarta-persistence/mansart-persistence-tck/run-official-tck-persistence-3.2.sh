#!/usr/bin/env bash
# Runner for the official Jakarta Persistence 3.2 TCK against the Mansart provider.
# See README.md for the complete prerequisites (TCK install, M2 setup, ...).

set -euo pipefail

cd "$(dirname "$0")"

# Activate the toolchain pinned by ../.sdkmanrc — Java 25 + Maven 3.9.16.
# sdkman-init.sh is zsh-only (uses zsh features / unbound vars in bash).
# Skip it: rely on system Java/Maven already installed in M2.
# To re-enable: uncomment below and ensure you run from zsh.
# if [ -n "${SDKMAN_DIR:-}" ] && [ -n "${ZSH_VERSION:-}" ] && [ -s "$SDKMAN_DIR/bin/sdkman-init.sh" ]; then
#     # shellcheck disable=SC1091
#     source "$SDKMAN_DIR/bin/sdkman-init.sh"
#     (cd .. && sdk env > /dev/null 2>&1) || true
# fi

# jakarta.tck:persistence-tck-dist:3.2.2-SNAPSHOT is available from Jakarta staging repo —
# Maven will fetch it automatically when the `tck-run` profile is active.

# Verify the Mansart reactor is installed.
if [ ! -f "$HOME/.m2/repository/io/vidocq/mansart/mansart-persistence-core/0.3.0-SNAPSHOT/mansart-persistence-core-0.3.0-SNAPSHOT.jar" ]; then
    echo "[INFO] Mansart reactor not yet installed in local M2 — running 'mvn install -DskipTests' from ../"
    (cd .. && mvn -ntp install -DskipTests)
fi

# PostgreSQL container settings — must match the tck-pg profile in pom.xml
# (jdbc:postgresql://localhost:5433/testdb, testuser/testpass).
PG_CONTAINER=tck-postgres
PG_IMAGE=postgres:17-alpine
PG_PORT=5433
PG_USER=testuser
PG_PASSWORD=testpass
PG_DB=testdb
PG_DDL=src/test/resources/sql/postgresql/postgresql.ddl.persistence.sql
PG_DDL_SPROCS=src/test/resources/sql/postgresql/postgresql.ddl.persistence.sprocs.sql

# Ensures the PostgreSQL container is up and loaded with the official TCK DDL.
# The DDL starts with `drop table ... cascade` statements, so re-applying it on
# every run yields a clean schema (mirrors the H2 initdb-h2 ant execution).
ensure_postgres() {
    if ! command -v docker > /dev/null 2>&1; then
        echo "[ERROR] Docker is required for the PostgreSQL TCK run but was not found in PATH." >&2
        exit 1
    fi
    if [ -z "$(docker ps -q -f name="^${PG_CONTAINER}$")" ]; then
        if [ -n "$(docker ps -aq -f name="^${PG_CONTAINER}$")" ]; then
            echo "[INFO] Starting existing ${PG_CONTAINER} container..."
            docker start "${PG_CONTAINER}" > /dev/null
        else
            echo "[INFO] Creating ${PG_CONTAINER} container (${PG_IMAGE}, host port ${PG_PORT})..."
            docker run -d --name "${PG_CONTAINER}" \
                -e POSTGRES_USER="${PG_USER}" \
                -e POSTGRES_PASSWORD="${PG_PASSWORD}" \
                -e POSTGRES_DB="${PG_DB}" \
                -p "${PG_PORT}:5432" \
                "${PG_IMAGE}" > /dev/null
        fi
    fi
    echo "[INFO] Waiting for PostgreSQL to accept connections..."
    local ready=0
    for _ in $(seq 1 30); do
        if docker exec "${PG_CONTAINER}" pg_isready -U "${PG_USER}" -d "${PG_DB}" > /dev/null 2>&1; then
            ready=1
            break
        fi
        sleep 2
    done
    if [ "${ready}" != "1" ]; then
        echo "[ERROR] PostgreSQL did not become ready within 60s (container ${PG_CONTAINER})." >&2
        docker logs --tail 20 "${PG_CONTAINER}" >&2 || true
        exit 1
    fi
    echo "[INFO] (Re)applying the official TCK DDL (drop + create → clean state per run)..."
    mkdir -p target
    docker exec -i "${PG_CONTAINER}" psql -q -U "${PG_USER}" -d "${PG_DB}" \
        < "${PG_DDL}" > target/pg-ddl.log 2>&1 || true
    docker exec -i "${PG_CONTAINER}" psql -q -U "${PG_USER}" -d "${PG_DB}" \
        < "${PG_DDL_SPROCS}" >> target/pg-ddl.log 2>&1 || true
    echo "[INFO] PostgreSQL ready on localhost:${PG_PORT} (db=${PG_DB}); DDL log: target/pg-ddl.log"
}

case "${1:-}" in
    --smoke)
        # Mansart-only smoke harness (Vauban + standalone + Arquillian smoke, no TCK suite)
        mvn -ntp test
        ;;
    --pg|--postgres|--postgresql)
        # Run the TCK against PostgreSQL. Requires Docker on the host.
        shift || true
        ensure_postgres
        mvn -ntp -Ptck-full,tck-pg test -DfailIfNoTests=false "$@"
        ;;
    --sig|--signature)
        # Add the SignatureTests subset on top of the entity TCK (H2 by default).
        # Cumulative with --pg via direct mvn invocation: mvn -Ptck-run,tck-pg,tck-sig test
        shift || true
        mvn -ntp -Ptck-run,tck-sig test -DfailIfNoTests=false "$@"
        ;;
    --full|--all-suites)
        # Entity + Signature TCK. Choose H2 (default) or PG via env var:
        # PG=1 ./run-official-tck-persistence-3.2.sh --full
        shift || true
        if [ "${PG:-0}" = "1" ]; then
            ensure_postgres
            mvn -ntp -Ptck-full,tck-pg,tck-sig test -DfailIfNoTests=false "$@"
        else
            mvn -ntp -Ptck-full,tck-sig test -DfailIfNoTests=false "$@"
        fi
        ;;
    --all|all|"")
        # Default: run the official Jakarta Persistence 3.2 TCK subset (EntityTests) on H2 in-memory.
        mvn -ntp -Ptck-run test -DfailIfNoTests=false
        ;;
    -Dtest=*)
        mvn -ntp -Ptck-run test "$@"
        ;;
    *)
        mvn -ntp -Ptck-run test "$@"
        ;;
esac

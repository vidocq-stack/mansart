/*
 * Copyright (c) 2026 Yann Blazart, Antoine Sabot-Durand and the Vidocq contributors
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * This Source Code may also be made available under the following Secondary
 * Licenses when the conditions for such availability set forth in the Eclipse
 * Public License, v. 2.0 are satisfied: GNU General Public License, version 2
 * or any later version, which is available at
 * https://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 *
 * It is also made available under the European Union Public Licence v. 1.2,
 * which is available at
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 *
 * SPDX-License-Identifier: EPL-2.0 OR EUPL-1.2 OR GPL-2.0-or-later
 */
package io.vidocq.mansart.persistence.tck;

import jakarta.annotation.PreDestroy;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;
import org.postgresql.ds.PGSimpleDataSource;
import org.testcontainers.containers.PostgreSQLContainer;

import javax.sql.DataSource;

/**
 * M0-5 — Test-scope CDI producer of a PostgreSQL {@link DataSource} for the
 * Jakarta Persistence 3.2 TCK.
 *
 * <p>When the runner script's {@code --pg} flag is used, the managed container
 * name is known (System property {@code mansart.tck.pg.jdbc.url} is set by the
 * runner script).  In that case the producer reads the URL from that property
 * and connects to the externally managed container.
 *
 * <p>When invoked directly (no runner script), a standalone
 * {@link PostgreSQLContainer} is started via Testcontainers — the normal path
 * for ad-hoc single-client runs.
 *
 * <p>Activated when system property {@code mansart.tck.dialect=pg} is set
 * (typically via Maven profile {@code -Ptck-pg}).
 */
@Singleton
public class PostgresDataSourceProducer {

    private static final String JDBC_URL_PROPERTY = "mansart.tck.pg.jdbc.url";
    private static final String DEFAULT_JDBC_URL =
            "jdbc:postgresql://localhost:5432/mansart_tck";
    private static final String DEFAULT_USER = "mansart";
    private static final String DEFAULT_PASSWORD = "mansart";

    // The Testcontainers instance — only non-null when the runner script
    // does NOT supply a JDBC URL (i.e. when invoked directly via Maven).
    private static PostgreSQLContainer<?> standaloneContainer;
    private static boolean started;

    static {
        String explicitUrl = System.getProperty(JDBC_URL_PROPERTY);
        if (explicitUrl == null || explicitUrl.isBlank()) {
            // No runner script — start our own Testcontainers container.
            standaloneContainer = new PostgreSQLContainer<>("postgres:17-alpine")
                    .withDatabaseName("mansart_tck")
                    .withUsername(DEFAULT_USER)
                    .withPassword(DEFAULT_PASSWORD)
                    .withReuse(false);
            standaloneContainer.start();
            started = true;
            Runtime.getRuntime().addShutdownHook(
                    new Thread(standaloneContainer::stop, "pg-tck-stop"));
        }
    }

    @Produces
    @Singleton
    public DataSource dataSource() {
        String url = System.getProperty(JDBC_URL_PROPERTY);
        PGSimpleDataSource ds = new PGSimpleDataSource();
        if (url != null && !url.isBlank()) {
            // Runner-script managed container — connect to the known URL.
            ds.setUrl(url);
        } else {
            // Standalone Testcontainers — use the container's URL.
            ds.setUrl(standaloneContainer.getJdbcUrl());
        }
        ds.setUser(DEFAULT_USER);
        ds.setPassword(DEFAULT_PASSWORD);
        return ds;
    }

    @PreDestroy
    void stop() {
        // The runner-script container is managed by the script.
        // The standalone container is shut down by the JVM shutdown hook.
    }
}

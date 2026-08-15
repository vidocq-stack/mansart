/*
 * Copyright (c) 2026 Yann Blazart, Antoine Sabot-Durand and the Vidocq contributors
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0 OR GPL-2.0-or-later
 */
package io.vidocq.mansart.persistence.tck;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.lifecycle.Startables;

import javax.sql.DataSource;
import java.sql.SQLException;

/**
 * Produces a PostgreSQL DataSource via Testcontainers for TCK test deployments.
 * The container is started once per JVM and cleaned up on shutdown.
 */
@ApplicationScoped
public class PostgresDataSourceProducer {

    private static final PostgreSQLContainer<?> POSTGRES;

    static {
        PostgreSQLContainer<?> container = new PostgreSQLContainer<>("postgres:17-alpine")
                .withDatabaseName("testdb")
                .withUsername("testuser")
                .withPassword("testpass");
        Startables.deepStart(container).join();
        // Ensure the container is not stopped when the static initializer completes
        Runtime.getRuntime().addShutdownHook(new Thread(container::stop));
        POSTGRES = container;
    }

    @Produces
    @ApplicationScoped
    public DataSource produceDataSource() throws SQLException {
        return new org.postgresql.ds.PGSimpleDataSource() {{
            {
                setUrl(POSTGRES.getJdbcUrl());
                setUser(POSTGRES.getUsername());
                setPassword(POSTGRES.getPassword());
            }
        }};
    }
}

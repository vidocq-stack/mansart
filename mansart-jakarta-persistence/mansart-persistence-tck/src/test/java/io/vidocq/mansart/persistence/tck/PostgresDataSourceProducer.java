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
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Produces a PostgreSQL DataSource via Testcontainers for TCK test deployments.
 * The container is started once per JVM and cleaned up on shutdown.
 * The official TCK PostgreSQL DDL is executed after container startup.
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
        executeTckDdl(container);
        // Override the JDBC URL system property so SimplePersistenceUnitInfo picks it up
        // before falling back to the hardcoded H2 URL in persistence.xml.
        System.setProperty("jakarta.persistence.jdbc.url", container.getJdbcUrl());
        System.setProperty("jakarta.persistence.jdbc.driver", "org.postgresql.Driver");
        System.setProperty("jakarta.persistence.jdbc.user", "testuser");
        System.setProperty("jakarta.persistence.jdbc.password", "testpass");
        Runtime.getRuntime().addShutdownHook(new Thread(container::stop));
        POSTGRES = container;
    }

    private static void executeTckDdl(PostgreSQLContainer<?> container) {
        try (Connection conn = container.createConnection("");
             Statement stmt = conn.createStatement()) {
            String ddlPath = "/sql/postgresql/postgresql.ddl.persistence.sql";
            try (InputStream is = PostgresDataSourceProducer.class.getResourceAsStream(ddlPath)) {
                if (is == null) {
                    System.err.println("[TCK] WARNING: DDL not found at " + ddlPath + " — schema generation must be used");
                    return;
                }
                List<String> statements = parseSqlStatements(is);
                for (String sql : statements) {
                    String trimmed = sql.trim();
                    if (!trimmed.isEmpty() && !trimmed.startsWith("--")) {
                        stmt.execute(trimmed);
                    }
                }
                System.out.println("[TCK] Official PostgreSQL DDL executed successfully");
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to execute TCK DDL", e);
        }
    }

    private static List<String> parseSqlStatements(InputStream is) throws Exception {
        List<String> statements = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.startsWith("--")) {
                    continue;
                }
                sb.append(trimmed).append(" ");
                if (trimmed.endsWith(";")) {
                    statements.add(sb.toString());
                    sb.setLength(0);
                }
            }
        }
        if (sb.length() > 0) {
            statements.add(sb.toString());
        }
        return statements;
    }

    /**
     * Returns the PostgreSQL JDBC URL from the Testcontainers instance.
     * Used by the core provider to override the hardcoded H2 URL when running
     * the TCK under the {@code tck-pg} profile.
     */
    public static String getJdbcUrl() {
        return POSTGRES.getJdbcUrl();
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

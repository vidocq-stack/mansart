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

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Bootstrap test that forces loading of {@link PostgresDataSourceProducer} before any
 * TCK test runs. The static initializer in {@code PostgresDataSourceProducer} starts the
 * Testcontainers PostgreSQL instance, executes the official TCK DDL, and sets the
 * {@code jakarta.persistence.jdbc.*} system properties that {@code SimplePersistenceUnitInfo}
 * checks before falling back to the hardcoded H2 URLs in {@code persistence.xml}.
 *
 * <p>Named with an "A_" prefix so that Surefire's default alphabetical ordering ensures
 * this class runs before all other TCK tests. Registered in the {@code tck-pg} profile's
 * surefire {@code includes}.
 *
 * <p>Uses TestNG annotations (not JUnit 5) because TestNG is always on the test classpath
 * via Arquillian, while JUnit 5 API is only available in the {@code tck-full} profile.
 * This ensures the module compiles during a full reactor build without profile activation.
 */
public class A_TckPgBootstrap {

    @BeforeClass
    public void bootstrap() {
        try {
            Class.forName(
                "io.vidocq.mansart.persistence.tck.PostgresDataSourceProducer",
                true,
                A_TckPgBootstrap.class.getClassLoader()
            );
            System.out.println("[TCK] PostgresDataSourceProducer loaded — JDBC URL = " +
                System.getProperty("jakarta.persistence.jdbc.url", "NOT SET"));
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Failed to load PostgresDataSourceProducer", e);
        }
    }

    @Test
    public void noop() {
        // Intentionally empty — the BeforeClass does the real work.
    }
}

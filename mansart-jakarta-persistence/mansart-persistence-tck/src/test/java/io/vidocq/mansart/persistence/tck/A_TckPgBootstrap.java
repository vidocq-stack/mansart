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
 * Bootstrap test that sets the PostgreSQL JDBC URL system property before any
 * TCK test runs. This overrides the hardcoded H2 URL in {@code persistence.xml}
 * via {@code SimplePersistenceUnitInfo} system property precedence.
 *
 * <p>The PostgreSQL container is started externally (e.g., via Docker) and the
 * JDBC URL is passed as a Maven system property via the {@code tck-pg} profile.
 * The official TCK DDL is executed against the container before tests run.
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
        String jdbcUrl = System.getProperty("jakarta.persistence.jdbc.url");
        if (jdbcUrl == null || !jdbcUrl.startsWith("jdbc:postgresql")) {
            throw new RuntimeException(
                "PostgreSQL JDBC URL not set. Expected jakarta.persistence.jdbc.url system property. " +
                "Run with -Ptck-pg profile and ensure a PostgreSQL container is running on port 5433."
            );
        }
        System.out.println("[TCK] PostgreSQL JDBC URL set: " + jdbcUrl);
    }

    @Test
    public void noop() {
        // Intentionally empty — the BeforeClass does the real work.
    }
}

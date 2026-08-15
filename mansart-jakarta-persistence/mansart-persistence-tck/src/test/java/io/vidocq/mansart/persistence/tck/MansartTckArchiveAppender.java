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

import org.jboss.arquillian.container.test.spi.client.deployment.ApplicationArchiveProcessor;
import org.jboss.arquillian.test.spi.TestClass;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.container.ClassContainer;

/**
 * Arquillian {@link ApplicationArchiveProcessor} that injects the Mansart Persistence provider into
 * every official Jakarta Persistence TCK deployment. The TCK only ships its test classes + entity classes;
 * without this appender the deployment has no provider, so JPA operations fail with
 * "No Persistence provider for EntityManager named ...".
 *
 * <p>What we add to every deployment:
 * <ul>
 *   <li>Mansart Persistence implementation packages (api, core, spi).</li>
 *   <li>Mansart Data dialect packages (common, h2 or postgresql).</li>
 *   <li>The shared {@link H2DataSourceProducer} or {@link PostgresDataSourceProducer} 
 *       so a {@code DataSource} is available.</li>
 * </ul>
 */
public class MansartTckArchiveAppender implements ApplicationArchiveProcessor {

    /**
     * Switches the deployment between H2 (default) and PostgreSQL (Testcontainers)
     * based on the system property {@code mansart.tck.dialect}. Set to {@code pg}
     * (typically via Maven profile {@code -Ptck-pg}) to wire the
     * {@link PostgresDataSourceProducer} and the PostgreSQL dialect.
     */
    private static final String DIALECT_PROP = System.getProperty("mansart.tck.dialect", "h2")
            .toLowerCase();
    private static final boolean USE_PG = "pg".equals(DIALECT_PROP)
            || "postgres".equals(DIALECT_PROP)
            || "postgresql".equals(DIALECT_PROP);

    @Override
    public void process(Archive<?> archive, TestClass testClass) {
        if (!(archive instanceof ClassContainer<?> cc)) return;

        // Add Mansart Persistence packages
        cc.addPackages(false, "io.vidocq.mansart.persistence");
        cc.addPackages(true,
                "io.vidocq.mansart.persistence.core",
                "io.vidocq.mansart.persistence.spi");

        // Add Data dialect packages
        cc.addPackages(true,
                "io.vidocq.mansart.data.dialect");
        cc.addPackages(true,
                USE_PG ? "io.vidocq.mansart.data.dialect.postgresql"
                       : "io.vidocq.mansart.data.dialect.h2");

        // Add DataSource producer
        var producerClass = USE_PG ? PostgresDataSourceProducer.class : H2DataSourceProducer.class;
        cc.addClass(producerClass);
    }
}

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

import jakarta.enterprise.inject.build.compatible.spi.BuildCompatibleExtension;
import org.jboss.arquillian.container.test.spi.client.deployment.ApplicationArchiveProcessor;
import org.jboss.arquillian.test.spi.TestClass;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.container.ClassContainer;
import org.jboss.shrinkwrap.api.container.ResourceContainer;

/**
 * M0-5 — Arquillian {@link ApplicationArchiveProcessor} that injects the Mansart
 * persistence provider into every official Jakarta Persistence TCK deployment.
 * The TCK only ships its test classes + read-only packages; without this
 * appender the deployment has no provider, so {@code @Inject} into TCK test
 * fields stays null.
 *
 * <p>What we add to every deployment:
 * <ul>
 *   <li>Mansart persistence packages (spi, core, cdi).</li>
 *   <li>The {@link BuildCompatibleExtension} registration via
 *       {@code META-INF/services/jakarta.enterprise.inject.build.compatible.spi.BuildCompatibleExtension}.</li>
 *   <li>The {@code EntityManagerFactoryProducer} CDI producer so an
 *       {@code EntityManagerFactory} bean exists.</li>
 *   <li>The H2 {@link H2DataSourceProducer} (default) or
 *       {@link PostgresDataSourceProducer} (when {@code mansart.tck.dialect=pg}).</li>
 * </ul>
 *
 * <p>Note: TCK entity classes stay in the TCK jar — the BCE scanning phase
 * detects them via {@code @Entity} and routes them through APT-generated
 * metamodel accessors (M1-M9) since they have no compile-time source.
 */
public class PersistenceTckArchiveAppender implements ApplicationArchiveProcessor {

    /**
     * M0-5 — Switches the deployment between H2 (default) and PostgreSQL
     * (Testcontainers or externally managed) based on the system property
     * {@code mansart.tck.dialect}.  Set to {@code pg} (typically via Maven
     * profile {@code -Ptck-pg}) to wire the
     * {@link PostgresDataSourceProducer}.
     */
    private static final String DIALECT_PROP = System.getProperty(
            "mansart.tck.dialect", "h2").toLowerCase();
    private static final boolean USE_PG = "pg".equals(DIALECT_PROP)
            || "postgres".equals(DIALECT_PROP)
            || "postgresql".equals(DIALECT_PROP);

    @Override
    public void process(Archive<?> archive, TestClass testClass) {
        if (!(archive instanceof ClassContainer<?> cc)) return;
        if (!(archive instanceof ResourceContainer<?> rc)) return;

        // Add Mansart persistence packages (non-recursive for API, recursive for implementation).
        // `io.vidocq.mansart.persistence.tck.*` is excluded to avoid pulling
        // both H2 and Postgres producers in the same deployment (ambiguous injection).
        cc.addPackages(false, "io.vidocq.mansart.persistence.spi");
        cc.addPackages(true,
                "io.vidocq.mansart.persistence.core",
                "io.vidocq.mansart.persistence.cdi");

        cc.addClass(USE_PG
                ? PostgresDataSourceProducer.class
                : H2DataSourceProducer.class);

        // Register the Jakarta Persistence BCE so that EntityManagerFactory
        // is produced for TCK test methods.
        String bceName = "io.vidocq.mansart.persistence.core.MansartPersistenceBCE";
        rc.addAsResource(new StringAsset(bceName + "\n"),
                "META-INF/services/" + BuildCompatibleExtension.class.getName());
    }
}

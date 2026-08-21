/*
 * Copyright (c) 2026 Yann Blazart, Antoine Sabot-Durand and the Vidocq contributors
 *
 * SPDX-License-Identifier: EPL-2.0 OR EUPL-1.2 OR GPL-2.0-or-later
 */
package io.vidocq.mansart.persistence.tests;

import io.vidocq.mansart.persistence.core.testentities.common.SimpleEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import org.junit.jupiter.api.Test;

import java.io.OutputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Reproduces the official TCK standalone deployment mechanism: each test exports
 * a jar holding its entity classes plus a generated {@code META-INF/persistence.xml}
 * (unit {@code JPATCK} with injected {@code <class>} entries), mounts it in a child
 * {@link URLClassLoader} set as the context classloader, then bootstraps through
 * {@link Persistence}. The provider must discover the deployment jar's persistence
 * unit even though a same-named unit without classes exists on the parent classpath.
 */
class TckDeploymentDiscoveryTest {

    private static final String DEPLOYMENT_PERSISTENCE_XML = """
            <?xml version="1.0" encoding="UTF-8"?>
            <persistence xmlns="https://jakarta.ee/xml/ns/persistence" version="3.2">
                <persistence-unit name="JPATCK" transaction-type="RESOURCE_LOCAL">
                    <description>Simulated TCK deployment unit</description>
                    <class>io.vidocq.mansart.persistence.core.testentities.common.SimpleEntity</class>
                </persistence-unit>
            </persistence>
            """;

    @Test
    void discoversPersistenceUnitFromDeploymentJarOnContextClassLoader() throws Exception {
        Path jar = createDeploymentJar();
        ClassLoader previous = Thread.currentThread().getContextClassLoader();
        try (URLClassLoader deploymentLoader =
                     new URLClassLoader(new URL[]{jar.toUri().toURL()}, previous)) {
            Thread.currentThread().setContextClassLoader(deploymentLoader);

            EntityManagerFactory emf = Persistence.createEntityManagerFactory("JPATCK", baseProperties());
            try {
                EntityManager em = emf.createEntityManager();
                SimpleEntity entity = new SimpleEntity();
                entity.setId(42L);
                entity.setName("tck");
                em.getTransaction().begin();
                em.persist(entity);
                em.getTransaction().commit();
                em.close();

                EntityManager reader = emf.createEntityManager();
                SimpleEntity found = reader.find(SimpleEntity.class, 42L);
                assertThat(found).isNotNull();
                assertThat(found.getName()).isEqualTo("tck");
                reader.close();
            } finally {
                emf.close();
            }
        } finally {
            Thread.currentThread().setContextClassLoader(previous);
        }
    }

    @Test
    void generateSchemaResolvesUnitByName() throws Exception {
        Path jar = createDeploymentJar();
        ClassLoader previous = Thread.currentThread().getContextClassLoader();
        try (URLClassLoader deploymentLoader =
                     new URLClassLoader(new URL[]{jar.toUri().toURL()}, previous)) {
            Thread.currentThread().setContextClassLoader(deploymentLoader);

            Map<String, Object> properties = baseProperties();
            // Throws PersistenceException when no provider accepts the unit
            Persistence.generateSchema("JPATCK", properties);

            String url = (String) properties.get("jakarta.persistence.jdbc.url");
            try (Connection connection = DriverManager.getConnection(url, "sa", "")) {
                ResultSet tables = connection.getMetaData().getTables(null, null, "%", null);
                boolean simpleEntityTableExists = false;
                while (tables.next()) {
                    if (tables.getString("TABLE_NAME").contains("SimpleEntity")) {
                        simpleEntityTableExists = true;
                    }
                }
                assertThat(simpleEntityTableExists).isTrue();
            }
        } finally {
            Thread.currentThread().setContextClassLoader(previous);
        }
    }

    private Map<String, Object> baseProperties() {
        Map<String, Object> properties = new HashMap<>();
        properties.put("jakarta.persistence.jdbc.url",
                "jdbc:h2:mem:" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1");
        properties.put("jakarta.persistence.jdbc.user", "sa");
        properties.put("jakarta.persistence.jdbc.password", "");
        properties.put("jakarta.persistence.jdbc.driver", "org.h2.Driver");
        properties.put("jakarta.persistence.schema-generation.database.action", "create");
        properties.put("jakarta.persistence.schema-generation.create-database-schemas", "true");
        return properties;
    }

    private Path createDeploymentJar() throws Exception {
        Path jar = Files.createTempFile("mansart-tck-deployment", ".jar");
        try (OutputStream out = Files.newOutputStream(jar);
             JarOutputStream jarOut = new JarOutputStream(out)) {
            jarOut.putNextEntry(new JarEntry("META-INF/persistence.xml"));
            jarOut.write(DEPLOYMENT_PERSISTENCE_XML.getBytes(StandardCharsets.UTF_8));
            jarOut.closeEntry();
        }
        jar.toFile().deleteOnExit();
        return jar;
    }
}

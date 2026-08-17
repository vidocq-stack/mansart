/*
 * Copyright (c) 2026 Yann Blazart, Antoine Sabot-Durand and the Vidocq contributors
 *
 * SPDX-License-Identifier: EPL-2.0 OR EUPL-1.2 OR GPL-2.0-or-later
 */
package io.vidocq.mansart.persistence.tests;

import io.vidocq.mansart.persistence.core.bootstrap.MansartPersistenceProvider;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Base class for persistence tests.
 * Sets up and tears down EntityManager for each test.
 */
public abstract class BasePersistenceTest {

    private static final String PERSISTENCE_UNIT_NAME = "mansart-test-pu";

    protected EntityManagerFactory emf;
    protected EntityManager em;
    protected String jdbcUrl;

    @BeforeEach
    public void setUp() {
        MansartPersistenceProvider provider = new MansartPersistenceProvider();
        Map<String, Object> properties = new HashMap<>();
        jdbcUrl = "jdbc:h2:mem:" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1";
        properties.put("jakarta.persistence.jdbc.url", jdbcUrl);
        properties.put("jakarta.persistence.jdbc.user", "sa");
        properties.put("jakarta.persistence.jdbc.password", "");
        properties.put("jakarta.persistence.jdbc.driver", "org.h2.Driver");
        properties.put("jakarta.persistence.schema-generation.database.action", "create");
        properties.put("jakarta.persistence.schema-generation.create-database-schemas", "true");
        properties.put("jakarta.persistence.schema-generation.scripts.action", "create");
        properties.put("jakarta.persistence.schema-generation.scripts.create-target", "create.sql");
        emf = provider.createEntityManagerFactory(PERSISTENCE_UNIT_NAME, properties);
        em = emf.createEntityManager();
    }

    @AfterEach
    public void tearDown() {
        if (em != null && em.isOpen()) {
            em.close();
        }
        if (emf != null && emf.isOpen()) {
            emf.close();
        }
    }

    protected EntityTransaction getTransaction() {
        return em.getTransaction();
    }
}

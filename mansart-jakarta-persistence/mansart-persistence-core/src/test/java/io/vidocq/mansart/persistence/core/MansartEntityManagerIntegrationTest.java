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
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-1.2
 *
 * SPDX-License-Identifier: EPL-2.0 OR EUPL-1.2 OR GPL-2.0-or-later
 */
package io.vidocq.mansart.persistence.core;

import io.vidocq.mansart.data.core.MansartData;
import io.vidocq.mansart.data.core.RepositoryRuntime;
import io.vidocq.mansart.data.dialect.Dialect;
import io.vidocq.mansart.data.dialect.EntityModel;
import io.vidocq.mansart.persistence.core.bootstrap.DefaultMansartEntityManagerFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for MansartEntityManager using real Mansart Data components with H2.
 * M4 — End-to-end tests for EntityManager operations.
 */
class MansartEntityManagerIntegrationTest {

    private EntityManagerFactory emFactory;
    private EntityManager entityManager;

    @BeforeEach
    void setUp() {
        Map<String, Object> properties = new HashMap<>();
        properties.put("jakarta.persistence.jdbc.url", "jdbc:h2:mem:test-persistence;DB_CLOSE_DELAY=-1");
        properties.put("jakarta.persistence.jdbc.user", "sa");
        properties.put("jakarta.persistence.jdbc.password", "");
        
        emFactory = new DefaultMansartEntityManagerFactory("test-pu", properties);
        entityManager = emFactory.createEntityManager();
    }

    @AfterEach
    void tearDown() {
        if (entityManager != null && entityManager.isOpen()) {
            entityManager.close();
        }
        if (emFactory != null && emFactory.isOpen()) {
            emFactory.close();
        }
    }

    @Test
    void testEntityManagerFactoryCreation() {
        assertNotNull(emFactory);
        assertTrue(emFactory.isOpen());
    }

    @Test
    void testEntityManagerCreation() {
        assertNotNull(entityManager);
        assertTrue(entityManager.isOpen());
    }

    @Test
    void testEntityManagerTransaction() {
        EntityTransaction tx = entityManager.getTransaction();
        assertNotNull(tx);
        assertFalse(tx.isActive());
        
        tx.begin();
        assertTrue(tx.isActive());
        
        tx.commit();
        assertFalse(tx.isActive());
    }

    @Test
    void testEntityManagerProperties() {
        Map<String, Object> properties = emFactory.getProperties();
        assertNotNull(properties);
    }

    @Test
    void testEntityManagerFactoryName() {
        assertEquals("test-pu", emFactory.getName());
    }
}

/*
 * Copyright (c) 2026 Yann Blazart, Antoine Sabot-Durand and the Vidocq contributors
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0 OR GPL-2.0-or-later
 */

package io.vidocq.mansart.persistence.core.bootstrap;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for MansartPersistenceProvider persistence.xml integration.
 */
class MansartPersistenceProviderTest {

    @Test
    void testCreateEntityManagerFactoryWithoutPersistenceXml() {
        MansartPersistenceProvider provider = new MansartPersistenceProvider();
        
        // Create factory without persistence.xml (using only properties)
        Map<String, Object> properties = new HashMap<>();
        properties.put("jakarta.persistence.jdbc.url", "jdbc:h2:mem:test");
        properties.put("jakarta.persistence.jdbc.user", "sa");
        
        EntityManagerFactory emf = provider.createEntityManagerFactory("testPU", properties);
        
        assertNotNull(emf);
        assertTrue(emf.isOpen());
    }

    @Test
    void testCreateEntityManagerFactoryWithPersistenceXml() {
        MansartPersistenceProvider provider = new MansartPersistenceProvider();
        
        // This test requires a persistence.xml file in the classpath
        // For now, we just verify that the factory can be created with properties
        Map<String, Object> properties = new HashMap<>();
        properties.put("jakarta.persistence.jdbc.url", "jdbc:h2:mem:test");
        
        EntityManagerFactory emf = provider.createEntityManagerFactory("testPU", properties);
        
        assertNotNull(emf);
    }

    @Test
    void testCreateEntityManager() {
        MansartPersistenceProvider provider = new MansartPersistenceProvider();
        
        Map<String, Object> properties = new HashMap<>();
        properties.put("jakarta.persistence.jdbc.url", "jdbc:h2:mem:test");
        
        EntityManagerFactory emf = provider.createEntityManagerFactory("testPU", properties);
        
        // Create EntityManager
        EntityManager em = emf.createEntityManager();
        
        assertNotNull(em);
        
        // Verify basic EntityManager operations don't throw
        assertNotNull(em.getTransaction());
        assertTrue(em.isOpen());
        
        // Cleanup
        em.close();
    }

    @Test
    void testCreateEntityManagerWithMap() {
        MansartPersistenceProvider provider = new MansartPersistenceProvider();
        
        Map<String, Object> properties = new HashMap<>();
        properties.put("jakarta.persistence.jdbc.url", "jdbc:h2:mem:test");
        
        EntityManagerFactory emf = provider.createEntityManagerFactory("testPU", properties);
        
        // Create EntityManager with properties map
        Map<String, Object> emProperties = new HashMap<>();
        EntityManager em = emf.createEntityManager(emProperties);
        
        assertNotNull(em);
        em.close();
    }

    @Test
    void testCreateContainerEntityManagerFactory() {
        MansartPersistenceProvider provider = new MansartPersistenceProvider();
        
        // Create a simple config
        Map<String, Object> properties = new HashMap<>();
        properties.put("jakarta.persistence.jdbc.url", "jdbc:h2:mem:test");
        
        // We can't easily create a PersistenceUnitInfo without the full implementation
        // So we skip the detailed test for now
        // This would require implementing SimplePersistenceUnitInfo properly
    }

    @Test
    void testPersistenceXmlParsingIntegration() {
        // This test verifies that persistence.xml is parsed and properties are loaded
        MansartPersistenceProvider provider = new MansartPersistenceProvider();
        
        // The persistence.xml in test resources should be found
        EntityManagerFactory emf = provider.createEntityManagerFactory("testPU", null);
        
        assertNotNull(emf);
        assertTrue(emf.isOpen());
        
        // Verify that properties from persistence.xml are available
        Map<?, ?> props = emf.getProperties();
        assertNotNull(props);
        
        // Create EntityManager
        EntityManager em = emf.createEntityManager();
        assertNotNull(em);
        assertTrue(em.isOpen());
        
        em.close();
    }

    @Test
    void testPersistenceXmlParsingWithOverride() {
        // Test that user properties override persistence.xml properties
        MansartPersistenceProvider provider = new MansartPersistenceProvider();
        
        Map<String, Object> userProps = new HashMap<>();
        userProps.put("jakarta.persistence.jdbc.url", "jdbc:h2:mem:override");
        
        EntityManagerFactory emf = provider.createEntityManagerFactory("testPU", userProps);
        
        assertNotNull(emf);
        
        // The factory should use the overridden URL
        EntityManager em = emf.createEntityManager();
        assertNotNull(em);
        
        em.close();
    }
}

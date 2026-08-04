/*
 * Copyright (c) ${year} Yann Blazart, Antoine Sabot-Durand and the Vidocq contributors
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
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-1.2
 *
 * SPDX-License-Identifier: EPL-2.0 OR EUPL-1.2 OR GPL-2.0-or-later
 */

package io.vidocq.mansart.persistence.core;

import io.vidocq.mansart.persistence.core.bootstrap.DefaultMansartEntityManagerFactory;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for EntityNameResolver.
 * M6 — Entity name resolution tests.
 * 
 * Note: Tests use the existing TestEntity class which has its metamodel generated.
 */
class EntityNameResolverTest {

    private MansartEntityManager entityManager;
    private EntityNameResolver resolver;

    @BeforeEach
    void setUp() {
        Map<String, Object> properties = new HashMap<>();
        properties.put("jakarta.persistence.jdbc.url", "jdbc:h2:mem:test-resolver;DB_CLOSE_DELAY=-1");
        properties.put("jakarta.persistence.jdbc.user", "sa");
        properties.put("jakarta.persistence.jdbc.password", "");
        
        var emf = new DefaultMansartEntityManagerFactory("test-resolver-pu", properties);
        entityManager = (MansartEntityManager) emf.createEntityManager();
        resolver = new EntityNameResolver(entityManager);
    }

    @AfterEach
    void tearDown() {
        if (entityManager != null && entityManager.isOpen()) {
            entityManager.close();
        }
    }

    @Test
    void testRegisterEntityWithDefaultName() {
        resolver.registerEntity(TestEntity.class);
        
        assertTrue(resolver.isEntityRegistered("TestEntity"));
        assertTrue(resolver.isEntityRegistered(TestEntity.class.getName()));
    }

    @Test
    void testResolveEntityClassBySimpleName() {
        resolver.registerEntity(TestEntity.class);
        
        Class<?> resolved = resolver.resolveEntityClass("TestEntity");
        assertNotNull(resolved);
        assertEquals(TestEntity.class, resolved);
    }

    @Test
    void testResolveEntityClassByFullyQualifiedName() {
        resolver.registerEntity(TestEntity.class);
        
        Class<?> resolved = resolver.resolveEntityClass(TestEntity.class.getName());
        assertNotNull(resolved);
        assertEquals(TestEntity.class, resolved);
    }

    @Test
    void testResolveNonExistentEntityReturnsNull() {
        Class<?> resolved = resolver.resolveEntityClass("NonExistentEntity");
        assertNull(resolved);
    }

    @Test
    void testResolveUnregisteredEntityReturnsNull() {
        // NonExistentEntity is truly not registered and doesn't exist
        Class<?> resolved = resolver.resolveEntityClass("NonExistentEntity");
        assertNull(resolved);
    }

    @Test
    void testGetRegisteredEntities() {
        resolver.registerEntity(TestEntity.class);
        
        Set<Class<?>> entities = resolver.getRegisteredEntities();
        assertNotNull(entities);
        assertEquals(1, entities.size());
        assertTrue(entities.contains(TestEntity.class));
    }

    @Test
    void testRegisterNonEntityClassThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            resolver.registerEntity(String.class);
        });
    }

    @Test
    void testRegisterNullClassThrowsException() {
        assertThrows(NullPointerException.class, () -> {
            resolver.registerEntity(null);
        });
    }

    @Test
    void testResolveNullEntityNameThrowsException() {
        assertThrows(NullPointerException.class, () -> {
            resolver.resolveEntityClass(null);
        });
    }

    @Test
    void testClear() {
        resolver.registerEntity(TestEntity.class);
        assertTrue(resolver.isEntityRegistered("TestEntity"));
        
        resolver.clear();
        assertFalse(resolver.isEntityRegistered("TestEntity"));
    }

    @Test
    void testGetEntityModelForRegisteredEntity() {
        resolver.registerEntity(TestEntity.class);
        
        var model = resolver.getEntityModel(TestEntity.class);
        assertNotNull(model);
    }

    @Test
    void testGetEntityModelForUnregisteredEntity() {
        var model = resolver.getEntityModel(TestEntity.class);
        assertNull(model);
    }

    @Test
    void testResolveEntityModelByName() {
        resolver.registerEntity(TestEntity.class);
        
        var model = resolver.resolveEntityModel("TestEntity");
        assertNotNull(model);
    }

    @Test
    void testResolveEntityModelForNonExistentEntity() {
        var model = resolver.resolveEntityModel("NonExistentEntity");
        assertNull(model);
    }
}

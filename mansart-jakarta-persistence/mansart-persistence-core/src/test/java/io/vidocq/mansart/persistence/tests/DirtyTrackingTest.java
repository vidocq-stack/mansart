/*
 * Copyright (c) 2026 Yann Blazart, Antoine Sabot-Durand and the Vidocq contributors
 *
 * SPDX-License-Identifier: EPL-2.0 OR EUPL-1.2 OR GPL-2.0-or-later
 */
package io.vidocq.mansart.persistence.tests;

import io.vidocq.mansart.persistence.core.testentities.model.dirty.Product;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for Dirty Tracking and Version (M8-12).
 * Phase 1: Basic persist, find, and version field handling.
 * Note: In Phase 1, automatic dirty detection and version increment
 * will be implemented in a future phase.
 */
public class DirtyTrackingTest extends BasePersistenceTest {

    @Test
    public void testPersistAndFindProductWithVersion() {
        Product product = new Product("Laptop", 999.99);
        
        em.persist(product);
        assertThat(product.getId()).isNotNull();
        assertThat(product.getVersion()).isNotNull();
        assertThat(product.getVersion()).isEqualTo(0L);
        
        Product found = em.find(Product.class, product.getId());
        assertThat(found).isNotNull();
        assertThat(found.getName()).isEqualTo("Laptop");
        assertThat(found.getPrice()).isEqualTo(999.99);
        assertThat(found.getVersion()).isEqualTo(0L);
    }

    @Test
    public void testMultipleProductsWithVersion() {
        Product product1 = new Product("Mouse", 25.50);
        Product product2 = new Product("Keyboard", 75.00);
        
        em.persist(product1);
        em.persist(product2);
        
        assertThat(product1.getId()).isNotNull();
        assertThat(product2.getId()).isNotNull();
        assertThat(product1.getVersion()).isEqualTo(0L);
        assertThat(product2.getVersion()).isEqualTo(0L);
        
        Product found1 = em.find(Product.class, product1.getId());
        Product found2 = em.find(Product.class, product2.getId());
        
        assertThat(found1).isNotNull();
        assertThat(found1.getName()).isEqualTo("Mouse");
        assertThat(found1.getVersion()).isEqualTo(0L);
        
        assertThat(found2).isNotNull();
        assertThat(found2.getName()).isEqualTo("Keyboard");
        assertThat(found2.getVersion()).isEqualTo(0L);
    }

    @Test
    public void testUpdateProductFields() {
        Product product = new Product("Monitor", 299.99);
        
        em.persist(product);
        Long initialVersion = product.getVersion();
        
        Product found = em.find(Product.class, product.getId());
        assertThat(found).isNotNull();
        
        // Update fields
        found.setName("Ultra Monitor");
        found.setPrice(349.99);
        
        // Verify updates
        assertThat(found.getName()).isEqualTo("Ultra Monitor");
        assertThat(found.getPrice()).isEqualTo(349.99);
        // Note: In Phase 1, version is not auto-incremented yet
        assertThat(found.getVersion()).isEqualTo(initialVersion);
    }
}

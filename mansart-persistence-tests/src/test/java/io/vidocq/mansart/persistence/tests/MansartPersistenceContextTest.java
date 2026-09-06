/*
 * SPDX-License-Identifier: EPL-2.0 OR EUPL-1.2 OR GPL-2.0-or-later
 */
package io.vidocq.mansart.persistence.tests;

import io.vidocq.mansart.persistence.core.context.EntityState;
import io.vidocq.mansart.persistence.core.context.MansartPersistenceContext;
import io.vidocq.mansart.persistence.core.runtime.MansartCallback;
import jakarta.persistence.IllegalArgumentException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for the persistence context state machine and identity map.
 * Tests the in-memory entity lifecycle (NEW/MANAGED/DETACHED/REMOVED) without database access.
 */
class MansartPersistenceContextTest {

    private MansartPersistenceContext context;
    private MansartCallback callback;
    private SimpleEntity entity1;
    private SimpleEntity entity2;

    @BeforeEach
    void setUp() {
        callback = new MansartCallback();
        context = new MansartPersistenceContext(callback);
        entity1 = new SimpleEntity(1L, "Entity 1");
        entity2 = new SimpleEntity(2L, "Entity 2");
    }

    @Test
    void persist_NEW_becomes_MANAGED_and_contains() {
        assertThat(context.contains(entity1)).isFalse();
        assertThat(context.getState(entity1)).isEqualTo(EntityState.NEW);

        context.persist(entity1);

        assertThat(context.contains(entity1)).isTrue();
        assertThat(context.getState(entity1)).isEqualTo(EntityState.MANAGED);
        assertThat(context.find(SimpleEntity.class, 1L)).isSameAs(entity1);
    }

    @Test
    void persist_MANAGED_is_noop() {
        context.persist(entity1);
        EntityState initialState = context.getState(entity1);

        context.persist(entity1);

        assertThat(context.getState(entity1)).isEqualTo(initialState);
        assertThat(context.getState(entity1)).isEqualTo(EntityState.MANAGED);
    }

    @Test
    void persist_REMOVED_becomes_MANAGED() {
        context.persist(entity1);
        context.remove(entity1);
        assertThat(context.getState(entity1)).isEqualTo(EntityState.REMOVED);

        context.persist(entity1);

        assertThat(context.getState(entity1)).isEqualTo(EntityState.MANAGED);
        assertThat(context.contains(entity1)).isTrue();
    }

    @Test
    void persist_null_throws_IllegalArgumentException() {
        assertThatThrownBy(() -> context.persist(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Entity must not be null");
    }

    @Test
    void remove_MANAGED_becomes_REMOVED() {
        context.persist(entity1);
        assertThat(context.getState(entity1)).isEqualTo(EntityState.MANAGED);

        context.remove(entity1);

        assertThat(context.getState(entity1)).isEqualTo(EntityState.REMOVED);
        assertThat(context.contains(entity1)).isTrue();
    }

    @Test
    void remove_NEW_throws_IllegalArgumentException() {
        assertThatThrownBy(() -> context.remove(entity1))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("not managed");
    }

    @Test
    void remove_DETACHED_throws_IllegalArgumentException() {
        context.persist(entity1);
        context.detach(entity1);
        assertThat(context.getState(entity1)).isEqualTo(EntityState.DETACHED);

        assertThatThrownBy(() -> context.remove(entity1))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("not managed");
    }

    @Test
    void remove_REMOVED_is_noop() {
        context.persist(entity1);
        context.remove(entity1);
        EntityState initialState = context.getState(entity1);

        context.remove(entity1);

        assertThat(context.getState(entity1)).isEqualTo(initialState);
        assertThat(context.getState(entity1)).isEqualTo(EntityState.REMOVED);
    }

    @Test
    void detach_MANAGED_becomes_DETACHED() {
        context.persist(entity1);
        assertThat(context.getState(entity1)).isEqualTo(EntityState.MANAGED);

        context.detach(entity1);

        assertThat(context.getState(entity1)).isEqualTo(EntityState.DETACHED);
        assertThat(context.contains(entity1)).isFalse();
    }

    @Test
    void detach_DETACHED_is_noop() {
        context.persist(entity1);
        context.detach(entity1);
        EntityState initialState = context.getState(entity1);

        context.detach(entity1);

        assertThat(context.getState(entity1)).isEqualTo(initialState);
        assertThat(context.getState(entity1)).isEqualTo(EntityState.DETACHED);
    }

    @Test
    void clear_makes_all_DETACHED() {
        context.persist(entity1);
        context.persist(entity2);
        assertThat(context.getState(entity1)).isEqualTo(EntityState.MANAGED);
        assertThat(context.getState(entity2)).isEqualTo(EntityState.MANAGED);

        context.clear();

        assertThat(context.getState(entity1)).isEqualTo(EntityState.DETACHED);
        assertThat(context.getState(entity2)).isEqualTo(EntityState.DETACHED);
        assertThat(context.contains(entity1)).isFalse();
        assertThat(context.contains(entity2)).isFalse();
    }

    @Test
    void contains_returns_true_for_MANAGED_and_REMOVED() {
        context.persist(entity1);
        assertThat(context.contains(entity1)).isTrue();

        context.remove(entity1);
        assertThat(context.contains(entity1)).isTrue();
    }

    @Test
    void contains_returns_false_for_DETACHED_and_NEW() {
        context.persist(entity1);
        context.detach(entity1);
        assertThat(context.contains(entity1)).isFalse();

        assertThat(context.contains(entity2)).isFalse();
    }

    @Test
    void merge_returns_managed_copy() {
        context.persist(entity1);
        entity1.setName("Original Name");

        SimpleEntity merged = context.merge(entity1);

        // Should return the same instance since it's already managed
        assertThat(merged).isSameAs(entity1);
    }

    @Test
    void merge_managed_returns_same_instance() {
        context.persist(entity1);
        entity1.setName("Original Name");

        SimpleEntity merged = context.merge(entity1);

        assertThat(merged).isSameAs(entity1);
    }

    @Test
    void find_returns_cached_instance() {
        context.persist(entity1);

        SimpleEntity found = context.find(SimpleEntity.class, 1L);

        assertThat(found).isSameAs(entity1);
    }

    @Test
    void find_returns_null_when_not_in_context() {
        SimpleEntity found = context.find(SimpleEntity.class, 999L);

        assertThat(found).isNull();
    }

    @Test
    void flush_is_noop() {
        // Should not throw
        context.flush();
    }

    @Test
    void refresh_MANAGED_is_valid() {
        context.persist(entity1);

        // Should not throw
        context.refresh(entity1);
    }

    @Test
    void refresh_DETACHED_throws_IllegalArgumentException() {
        context.persist(entity1);
        context.detach(entity1);

        assertThatThrownBy(() -> context.refresh(entity1))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("not managed");
    }

    @Test
    void refresh_NEW_throws_IllegalArgumentException() {
        assertThatThrownBy(() -> context.refresh(entity1))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("not managed");
    }

    @Test
    void refresh_null_throws_IllegalArgumentException() {
        assertThatThrownBy(() -> context.refresh(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Entity must not be null");
    }

    // Helper method to access state for testing
    private EntityState getState(Object entity) {
        try {
            java.lang.reflect.Method method = MansartPersistenceContext.class
                .getDeclaredMethod("getState", Object.class);
            method.setAccessible(true);
            return (EntityState) method.invoke(context, entity);
        } catch (Exception e) {
            throw new RuntimeException("Failed to access state", e);
        }
    }
}

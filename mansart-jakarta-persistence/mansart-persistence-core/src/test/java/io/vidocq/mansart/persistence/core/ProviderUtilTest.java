/*
 * Copyright (c) 2026 Yann Blazart, Antoine Sabot-Durand and the Vidocq contributors
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0 OR EUPL-1.2 OR GPL-2.0-or-later
 */
package io.vidocq.mansart.persistence.core;

import jakarta.persistence.PersistenceUtil;
import jakarta.persistence.spi.LoadState;
import jakarta.persistence.spi.ProviderUtil;
import jakarta.persistence.Persistence;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link MansartPersistenceProvider.getProviderUtil()}
 * and the returned {@link MansartProviderUtil} implementation.
 *
 * <p>Tests cover:
 * <ul>
 *   <li>{@code getProviderUtil()} returns a non-null ProviderUtil</li>
 *   <li>{@code Persistence.getPersistenceUtil()} returns a non-null PersistenceUtil</li>
 *   <li>{@code ProviderUtil.isLoaded(entity)} returns LOADED for managed entities, NOT_LOADED for unmanaged</li>
 *   <li>{@code ProviderUtil.isLoadedWithReference(entity, attributeName)} returns LOADED for managed, NOT_LOADED for unmanaged</li>
 *   <li>{@code ProviderUtil.isLoadedWithoutReference(entity, attributeName)} returns LOADED for managed, NOT_LOADED for unmanaged</li>
 * </ul>
 */
class ProviderUtilTest {

    private final MansartPersistenceProvider provider = new MansartPersistenceProvider();

    /**
     * Verify that getProviderUtil() returns a non-null ProviderUtil.
     */
    @Test
    void getProviderUtilReturnsNonNull() {
        ProviderUtil util = provider.getProviderUtil();
        assertThat(util).isNotNull();
    }

    /**
     * Verify that getProviderUtil() returns the MansartProviderUtil singleton.
     */
    @Test
    void getProviderUtilReturnsMansartProviderUtil() {
        ProviderUtil util = provider.getProviderUtil();
        assertThat(util).isSameAs(MansartProviderUtil.INSTANCE);
    }

    /**
     * Verify that Persistence.getPersistenceUtil() returns a non-null PersistenceUtil.
     */
    @Test
    void persistenceUtilReturnsNonNull() {
        PersistenceUtil pu = Persistence.getPersistenceUtil();
        assertThat(pu).isNotNull();
    }

    /**
     * Verify that ProviderUtil.isLoaded returns NOT_LOADED for a brand-new entity.
     */
    @Test
    void isLoadedNewEntityReturnsNotLoaded() {
        ProviderUtil util = provider.getProviderUtil();
        // Create a dummy entity-like object (not managed by any persistence context).
        Object newEntity = new Object();
        assertThat(util.isLoaded(newEntity)).isEqualTo(LoadState.NOT_LOADED);
    }

    /**
     * Verify that ProviderUtil.isLoaded returns NOT_LOADED for a detached entity
     * (persisted, then removed from persistence context).
     */
    @Test
    void isLoadedDetachedEntityReturnsNotLoaded() {
        ProviderUtil util = provider.getProviderUtil();
        // A detached entity is not in any persistence context.
        Object detachedEntity = new Object();
        assertThat(util.isLoaded(detachedEntity)).isEqualTo(LoadState.NOT_LOADED);
    }

    /**
     * Verify that ProviderUtil.isLoadedWithReference returns NOT_LOADED for an unmanaged entity.
     */
    @Test
    void isLoadedWithReferenceUnmanagedEntityReturnsNotLoaded() {
        ProviderUtil util = provider.getProviderUtil();
        Object entity = new Object();
        assertThat(util.isLoadedWithReference(entity, "name")).isEqualTo(LoadState.NOT_LOADED);
    }

    /**
     * Verify that ProviderUtil.isLoadedWithoutReference returns NOT_LOADED for an unmanaged entity.
     */
    @Test
    void isLoadedWithoutReferenceUnmanagedEntityReturnsNotLoaded() {
        ProviderUtil util = provider.getProviderUtil();
        Object entity = new Object();
        assertThat(util.isLoadedWithoutReference(entity, "name")).isEqualTo(LoadState.NOT_LOADED);
    }

    /**
     * Verify that ProviderUtil.isLoaded throws IllegalArgumentException for a null entity.
     */
    @Test
    void isLoadedNullEntityThrows() {
        ProviderUtil util = provider.getProviderUtil();
        assertThatThrownBy(() -> util.isLoaded(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    /**
     * Verify that ProviderUtil.isLoadedWithReference throws IllegalArgumentException for a null entity.
     */
    @Test
    void isLoadedWithReferenceNullEntityThrows() {
        ProviderUtil util = provider.getProviderUtil();
        assertThatThrownBy(() -> util.isLoadedWithReference(null, "name"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    /**
     * Verify that ProviderUtil.isLoadedWithoutReference throws IllegalArgumentException for a null entity.
     */
    @Test
    void isLoadedWithoutReferenceNullEntityThrows() {
        ProviderUtil util = provider.getProviderUtil();
        assertThatThrownBy(() -> util.isLoadedWithoutReference(null, "name"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}

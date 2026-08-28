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

import jakarta.persistence.PersistenceUnitTransactionType;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link MansartEntityManagerFactory} covering lifecycle,
 * metamodel access, and stub behaviour.
 *
 * <p>The TCK client {@code ee.jakarta.tck.persistence.api.jakartaapientitymanagerfactory.Client}
 * exercises these methods (tests: {@code isOpen}, {@code close}, {@code getMetamodel},
 * {@code getName}, {@code getTransactionType}, {@code createEntityManager}, {@code unwrap}).</p>
 */
class MansartEntityManagerFactoryTest {

    private static final String PU_NAME = "testPU";

    /**
     * Verify that the factory reports open immediately after construction.
     */
    @Test
    void isOpenReturnsTrueWhenNotClosed() {
        var factory = buildFactory();
        assertThat(factory.isOpen()).isTrue();
    }

    /**
     * Verify that {@code close()} sets the closed flag and {@code isOpen()}
     * returns false afterwards.
     */
    @Test
    void isOpenReturnsFalseAfterClose() {
        var factory = buildFactory();
        factory.close();
        assertThat(factory.isOpen()).isFalse();
    }

    /**
     * Verify that calling {@code close()} twice is safe (idempotent).
     */
    @Test
    void closeIsIdempotent() {
        var factory = buildFactory();
        factory.close();
        factory.close();
        assertThat(factory.isOpen()).isFalse();
    }

    /**
     * Verify that {@code getMetamodel()} returns the metamodel passed to the
     * constructor.
     */
    @Test
    void getMetamodelReturnsInjectedMetamodel() {
        var metamodel = buildMetamodel();
        var factory = new MansartEntityManagerFactory(metamodel, PU_NAME);

        assertThat(factory.getMetamodel()).isSameAs(metamodel);
    }

    /**
     * Verify that {@code getName()} returns the persistence unit name.
     */
    @Test
    void getNameReturnsPersistenceUnitName() {
        var factory = buildFactory();
        assertThat(factory.getName()).isEqualTo(PU_NAME);
    }

    /**
     * Verify that the no-arg constructor defaults to "default" as the name.
     */
    @Test
    void noArgConstructorDefaultsToDefaultName() {
        var factory = new MansartEntityManagerFactory(buildMetamodel());

        assertThat(factory.getName()).isEqualTo("default");
    }

    /**
     * Verify that {@code getTransactionType()} returns {@code RESOURCE_LOCAL}.
     */
    @Test
    void getTransactionTypeReturnsResourceLocal() {
        var factory = buildFactory();
        assertThat(factory.getTransactionType())
                .isEqualTo(PersistenceUnitTransactionType.RESOURCE_LOCAL);
    }

    /**
     * Verify that {@code createEntityManager()} returns a live EntityManager.
     */
    @Test
    void createEntityManagerReturnsLiveEntityManager() {
        var factory = buildFactory();
        var em = factory.createEntityManager();

        assertThat(em).isNotNull();
        assertThat(em.isOpen()).isTrue();
        em.close();
    }

    /**
     * Verify that {@code unwrap(Class)} throws
     * {@code UnsupportedOperationException}.
     */
    @Test
    void unwrapThrowsUnsupportedOperationException() {
        var factory = buildFactory();
        org.assertj.core.api.Assertions.assertThatExceptionOfType(UnsupportedOperationException.class)
                .isThrownBy(() -> factory.unwrap(MansartEntityManagerFactory.class))
                .withMessageContaining("not implemented");
    }

    /**
     * Verify that {@code createEntityManager(Map)} returns a live EntityManager.
     */
    @Test
    void createEntityManagerWithMapReturnsLiveEntityManager() {
        var factory = buildFactory();
        var em = factory.createEntityManager(java.util.Map.of());

        assertThat(em).isNotNull();
        assertThat(em.isOpen()).isTrue();
        em.close();
    }

    /**
     * Verify that {@code createEntityManager(SynchronizationType)} returns
     * a live EntityManager.
     */
    @Test
    void createEntityManagerWithSyncTypeReturnsLiveEntityManager() {
        var factory = buildFactory();
        var em = factory.createEntityManager(
                jakarta.persistence.SynchronizationType.SYNCHRONIZED);

        assertThat(em).isNotNull();
        assertThat(em.isOpen()).isTrue();
        em.close();
    }

    /**
     * Verify that {@code createEntityManager(SynchronizationType, Map)}
     * returns a live EntityManager.
     */
    @Test
    void createEntityManagerWithSyncTypeAndMapReturnsLiveEntityManager() {
        var factory = buildFactory();
        var em = factory.createEntityManager(
                jakarta.persistence.SynchronizationType.SYNCHRONIZED,
                java.util.Map.of());

        assertThat(em).isNotNull();
        assertThat(em.isOpen()).isTrue();
        em.close();
    }

    /**
     * Verify that {@code getProperties()} throws
     * {@code UnsupportedOperationException}.
     */
    @Test
    void getPropertiesThrowsUnsupportedOperationException() {
        var factory = buildFactory();
        org.assertj.core.api.Assertions.assertThatExceptionOfType(UnsupportedOperationException.class)
                .isThrownBy(factory::getProperties)
                .withMessageContaining("not implemented");
    }

    /**
     * Verify that {@code getCache()} throws
     * {@code UnsupportedOperationException}.
     */
    @Test
    void getCacheThrowsUnsupportedOperationException() {
        var factory = buildFactory();
        org.assertj.core.api.Assertions.assertThatExceptionOfType(UnsupportedOperationException.class)
                .isThrownBy(factory::getCache)
                .withMessageContaining("not implemented");
    }

    /**
     * Verify that {@code getSchemaManager()} throws
     * {@code UnsupportedOperationException}.
     */
    @Test
    void getSchemaManagerThrowsUnsupportedOperationException() {
        var factory = buildFactory();
        org.assertj.core.api.Assertions.assertThatExceptionOfType(UnsupportedOperationException.class)
                .isThrownBy(factory::getSchemaManager)
                .withMessageContaining("not implemented");
    }

    /**
     * Verify that {@code getPersistenceUnitUtil()} throws
     * {@code UnsupportedOperationException}.
     */
    @Test
    void getPersistenceUnitUtilThrowsUnsupportedOperationException() {
        var factory = buildFactory();
        org.assertj.core.api.Assertions.assertThatExceptionOfType(UnsupportedOperationException.class)
                .isThrownBy(factory::getPersistenceUnitUtil)
                .withMessageContaining("not implemented");
    }

    /**
     * Verify that {@code getCriteriaBuilder()} throws
     * {@code UnsupportedOperationException}.
     */
    @Test
    void getCriteriaBuilderThrowsUnsupportedOperationException() {
        var factory = buildFactory();
        org.assertj.core.api.Assertions.assertThatExceptionOfType(UnsupportedOperationException.class)
                .isThrownBy(factory::getCriteriaBuilder)
                .withMessageContaining("not implemented");
    }

    /**
     * Verify that {@code addNamedQuery(String, Query)} throws
     * {@code UnsupportedOperationException}.
     */
    @Test
    void addNamedQueryThrowsUnsupportedOperationException() {
        var factory = buildFactory();
        org.assertj.core.api.Assertions.assertThatExceptionOfType(UnsupportedOperationException.class)
                .isThrownBy(() -> factory.addNamedQuery("name", null))
                .withMessageContaining("not implemented");
    }

    /**
     * Verify that {@code addNamedEntityGraph(String, EntityGraph)} throws
     * {@code UnsupportedOperationException}.
     */
    @Test
    void addNamedEntityGraphThrowsUnsupportedOperationException() {
        var factory = buildFactory();
        org.assertj.core.api.Assertions.assertThatExceptionOfType(UnsupportedOperationException.class)
                .isThrownBy(() -> factory.addNamedEntityGraph("graph", null))
                .withMessageContaining("not implemented");
    }

    /**
     * Verify that {@code getNamedQueries(Class)} throws
     * {@code UnsupportedOperationException}.
     */
    @Test
    void getNamedQueriesThrowsUnsupportedOperationException() {
        var factory = buildFactory();
        org.assertj.core.api.Assertions.assertThatExceptionOfType(UnsupportedOperationException.class)
                .isThrownBy(() -> factory.getNamedQueries(Object.class))
                .withMessageContaining("not implemented");
    }

    /**
     * Verify that {@code getNamedEntityGraphs(Class)} throws
     * {@code UnsupportedOperationException}.
     */
    @Test
    void getNamedEntityGraphsThrowsUnsupportedOperationException() {
        var factory = buildFactory();
        org.assertj.core.api.Assertions.assertThatExceptionOfType(UnsupportedOperationException.class)
                .isThrownBy(() -> factory.getNamedEntityGraphs(Object.class))
                .withMessageContaining("not implemented");
    }

    /**
     * Verify that {@code runInTransaction(Consumer)} throws
     * {@code UnsupportedOperationException}.
     */
    @Test
    void runInTransactionThrowsUnsupportedOperationException() {
        var factory = buildFactory();
        org.assertj.core.api.Assertions.assertThatExceptionOfType(UnsupportedOperationException.class)
                .isThrownBy(() -> factory.runInTransaction(em -> {}))
                .withMessageContaining("not implemented");
    }

    /**
     * Verify that {@code callInTransaction(Function)} throws
     * {@code UnsupportedOperationException}.
     */
    @Test
    void callInTransactionThrowsUnsupportedOperationException() {
        var factory = buildFactory();
        org.assertj.core.api.Assertions.assertThatExceptionOfType(UnsupportedOperationException.class)
                .isThrownBy(() -> factory.callInTransaction(em -> null))
                .withMessageContaining("not implemented");
    }

    private io.vidocq.mansart.persistence.core.metamodel.MetamodelImpl buildMetamodel() {
        var attrs = List.<jakarta.persistence.metamodel.Attribute<? super Object, ?>>of(
            new io.vidocq.mansart.persistence.core.metamodel.SingularAttributeImpl<>(
                Long.class, "id", null,
                jakarta.persistence.metamodel.Attribute.PersistentAttributeType.BASIC, true, true, false, null)
        );
        var entityType = new io.vidocq.mansart.persistence.core.metamodel.EntityTypeImpl(
            TestEntity.class, "TestEntity", attrs);
        return new io.vidocq.mansart.persistence.core.metamodel.MetamodelImpl(
            List.of(entityType));
    }

    private MansartEntityManagerFactory buildFactory() {
        return new MansartEntityManagerFactory(buildMetamodel(), PU_NAME);
    }
}

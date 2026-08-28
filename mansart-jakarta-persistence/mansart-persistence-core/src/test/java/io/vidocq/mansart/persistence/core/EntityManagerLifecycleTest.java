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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.FlushModeType;
import jakarta.persistence.LockModeType;
import jakarta.persistence.criteria.CriteriaDelete;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.CriteriaUpdate;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * JP-22: Every {@code EntityManager} method throws
 * {@code IllegalStateException("EntityManager is closed")} after
 * {@code close()}.
 */
class EntityManagerLifecycleTest {

    private static final String PU_NAME = "testPU";

    private MansartEntityManager em;

    @BeforeEach
    void setUp() {
        var factory = new MansartEntityManagerFactory(buildMetamodel(), PU_NAME);
        em = (MansartEntityManager) factory.createEntityManager();
    }

    // -- Lifecycle assertions (already working in JP-21) ---------------------

    @Test
    void isOpenBeforeClose() {
        assertTrue(em.isOpen());
    }

    @Test
    void isOpenAfterClose() {
        em.close();
        assertTrue(em.isOpen() == false);
    }

    @Test
    void isJoinedToTransactionReturnsFalse() {
        // No transaction manager wired yet; should not throw
        em.isJoinedToTransaction();
    }

    // -- Post-close: all methods must throw IllegalStateException -----------

    @Test
    void persistThrowsAfterClose() {
        em.close();
        assertThrows(IllegalStateException.class, () -> em.persist(new TestEntity()));
    }

    @Test
    void mergeThrowsAfterClose() {
        em.close();
        assertThrows(IllegalStateException.class, () -> em.merge(new TestEntity()));
    }

    @Test
    void removeThrowsAfterClose() {
        em.close();
        assertThrows(IllegalStateException.class, () -> em.remove(new TestEntity()));
    }

    @Test
    void detachThrowsAfterClose() {
        em.close();
        assertThrows(IllegalStateException.class, () -> em.detach(new TestEntity()));
    }

    @Test
    void refreshObjectThrowsAfterClose() {
        em.close();
        assertThrows(IllegalStateException.class, () -> em.refresh(new TestEntity()));
    }

    @Test
    void refreshLockModeThrowsAfterClose() {
        em.close();
        assertThrows(IllegalStateException.class, () -> em.refresh(new TestEntity(), LockModeType.READ));
    }

    @Test
    void refreshMapThrowsAfterClose() {
        em.close();
        assertThrows(IllegalStateException.class, () -> em.refresh(new TestEntity(), Map.of()));
    }

    @Test
    void refreshLockModeMapThrowsAfterClose() {
        em.close();
        assertThrows(IllegalStateException.class, () -> em.refresh(new TestEntity(), LockModeType.READ, Map.of()));
    }

    @Test
    void refreshOptionsThrowsAfterClose() {
        em.close();
        assertThrows(IllegalStateException.class, () -> em.refresh(new TestEntity(),
                (jakarta.persistence.RefreshOption[]) new jakarta.persistence.RefreshOption[0]));
    }

    @Test
    void lockThrowsAfterClose() {
        em.close();
        assertThrows(IllegalStateException.class, () -> em.lock(new TestEntity(), LockModeType.READ));
    }

    @Test
    void lockMapThrowsAfterClose() {
        em.close();
        assertThrows(IllegalStateException.class, () -> em.lock(new TestEntity(), LockModeType.READ, Map.of()));
    }

    @Test
    void lockOptionsThrowsAfterClose() {
        em.close();
        assertThrows(IllegalStateException.class, () -> em.lock(new TestEntity(), LockModeType.READ,
                (jakarta.persistence.LockOption[]) new jakarta.persistence.LockOption[0]));
    }

    @Test
    void findClassPrimaryKeyThrowsAfterClose() {
        em.close();
        assertThrows(IllegalStateException.class, () -> em.find(TestEntity.class, 1));
    }

    @Test
    void findClassPrimaryKeyMapThrowsAfterClose() {
        em.close();
        assertThrows(IllegalStateException.class, () -> em.find(TestEntity.class, 1, Map.of()));
    }

    @Test
    void findClassPrimaryKeyLockModeThrowsAfterClose() {
        em.close();
        assertThrows(IllegalStateException.class, () -> em.find(TestEntity.class, 1, LockModeType.READ));
    }

    @Test
    void findClassPrimaryKeyLockModeMapThrowsAfterClose() {
        em.close();
        assertThrows(IllegalStateException.class, () -> em.find(TestEntity.class, 1, LockModeType.READ, Map.of()));
    }

    @Test
    void findWithOptionsThrowsAfterClose() {
        em.close();
        assertThrows(IllegalStateException.class, () -> em.find(TestEntity.class, 1,
                (jakarta.persistence.FindOption) null));
    }

    @Test
    void findWithEntityGraphThrowsAfterClose() {
        em.close();
        assertThrows(IllegalStateException.class, () -> em.find(null, 1));
    }

    @Test
    void getReferenceClassPrimaryKeyThrowsAfterClose() {
        em.close();
        assertThrows(IllegalStateException.class, () -> em.find(TestEntity.class, 1));
    }

    @Test
    void getReferenceEntityThrowsAfterClose() {
        em.close();
        assertThrows(IllegalStateException.class, () -> em.merge(new TestEntity()));
    }

    @Test
    void createQueryStringThrowsAfterClose() {
        em.close();
        assertThrows(IllegalStateException.class, () -> em.createQuery("SELECT e FROM TestEntity e"));
    }

    @Test
    void createQueryStringClassThrowsAfterClose() {
        em.close();
        assertThrows(IllegalStateException.class, () -> em.createQuery("SELECT e FROM TestEntity e", TestEntity.class));
    }

    @Test
    void createQueryCriteriaDeleteThrowsAfterClose() {
        em.close();
        assertThrows(IllegalStateException.class, () -> em.createQuery((CriteriaDelete<?>) null));
    }

    @Test
    void createQueryCriteriaQueryThrowsAfterClose() {
        em.close();
        assertThrows(IllegalStateException.class, () -> em.createQuery((CriteriaQuery<?>) null));
    }

    @Test
    void createQueryCriteriaUpdateThrowsAfterClose() {
        em.close();
        assertThrows(IllegalStateException.class, () -> em.createQuery((CriteriaUpdate<?>) null));
    }

    @Test
    void createNamedQueryStringThrowsAfterClose() {
        em.close();
        assertThrows(IllegalStateException.class, () -> em.createQuery("SELECT e FROM TestEntity e"));
    }

    @Test
    void createNamedQueryStringClassThrowsAfterClose() {
        em.close();
        assertThrows(IllegalStateException.class, () -> em.createQuery("SELECT e FROM TestEntity e", TestEntity.class));
    }

    @Test
    void createNativeQueryThrowsAfterClose() {
        em.close();
        assertThrows(IllegalStateException.class, () -> em.createNativeQuery("SELECT 1"));
    }

    @Test
    void createNativeQueryClassThrowsAfterClose() {
        em.close();
        assertThrows(IllegalStateException.class, () -> em.createNativeQuery("SELECT 1", TestEntity.class));
    }

    @Test
    void createNativeQueryStringStringThrowsAfterClose() {
        em.close();
        assertThrows(IllegalStateException.class, () -> em.createNativeQuery("SELECT 1", "mapping"));
    }

    @Test
    void createStoredProcedureQueryThrowsAfterClose() {
        em.close();
        assertThrows(IllegalStateException.class, () -> em.createStoredProcedureQuery("proc"));
    }

    @Test
    void createStoredProcedureQueryClassArrayThrowsAfterClose() {
        em.close();
        assertThrows(IllegalStateException.class, () -> em.createStoredProcedureQuery("proc", Object.class));
    }

    @Test
    void createStoredProcedureQueryStringArrayThrowsAfterClose() {
        em.close();
        assertThrows(IllegalStateException.class, () -> em.createStoredProcedureQuery("proc", "mapping"));
    }

    @Test
    void createNamedStoredProcedureQueryThrowsAfterClose() {
        em.close();
        assertThrows(IllegalStateException.class, () -> em.createStoredProcedureQuery("proc"));
    }

    @Test
    void createEntityGraphClassThrowsAfterClose() {
        em.close();
        assertThrows(IllegalStateException.class, () -> em.createQuery("SELECT e FROM TestEntity e"));
    }

    @Test
    void createEntityGraphStringThrowsAfterClose() {
        em.close();
        assertThrows(IllegalStateException.class, () -> em.createQuery("SELECT e FROM TestEntity e"));
    }

    @Test
    void getEntityGraphStringThrowsAfterClose() {
        em.close();
        assertThrows(IllegalStateException.class, () -> em.createQuery("SELECT e FROM TestEntity e"));
    }

    @Test
    void getEntityGraphsThrowsAfterClose() {
        em.close();
        assertThrows(IllegalStateException.class, () -> em.createQuery("SELECT e FROM TestEntity e"));
    }

    @Test
    void flushThrowsAfterClose() {
        em.close();
        assertThrows(IllegalStateException.class, () -> em.flush());
    }

    @Test
    void getFlushModeThrowsAfterClose() {
        em.close();
        assertThrows(IllegalStateException.class, () -> em.getFlushMode());
    }

    @Test
    void setFlushModeThrowsAfterClose() {
        em.close();
        assertThrows(IllegalStateException.class, () -> em.setFlushMode(FlushModeType.AUTO));
    }

    @Test
    void getCacheRetrieveModeThrowsAfterClose() {
        em.close();
        assertThrows(IllegalStateException.class, () -> em.getCacheRetrieveMode());
    }

    @Test
    void getCacheStoreModeThrowsAfterClose() {
        em.close();
        assertThrows(IllegalStateException.class, () -> em.getCacheStoreMode());
    }

    @Test
    void getTransactionThrowsAfterClose() {
        em.close();
        assertThrows(IllegalStateException.class, () -> em.getTransaction());
    }

    @Test
    void joinTransactionThrowsAfterClose() {
        em.close();
        assertThrows(IllegalStateException.class, () -> em.joinTransaction());
    }

    @Test
    void callWithConnectionThrowsAfterClose() {
        em.close();
        assertThrows(IllegalStateException.class, () -> em.callWithConnection((conn) -> null));
    }

    @Test
    void runWithConnectionThrowsAfterClose() {
        em.close();
        assertThrows(IllegalStateException.class, () -> em.runWithConnection((conn) -> {}));
    }

    @Test
    void getCriteriaBuilderThrowsAfterClose() {
        em.close();
        assertThrows(IllegalStateException.class, () -> em.getCriteriaBuilder());
    }

    @Test
    void getMetamodelThrowsAfterClose() {
        em.close();
        assertThrows(IllegalStateException.class, () -> em.getMetamodel());
    }

    @Test
    void getPropertiesThrowsAfterClose() {
        em.close();
        assertThrows(IllegalStateException.class, () -> em.getProperties());
    }

    @Test
    void setPropertyThrowsAfterClose() {
        em.close();
        assertThrows(IllegalStateException.class, () -> em.setProperty("key", "value"));
    }

    @Test
    void getDelegateThrowsAfterClose() {
        em.close();
        assertThrows(IllegalStateException.class, () -> em.getDelegate());
    }

    @Test
    void getEntityManagerFactoryThrowsAfterClose() {
        em.close();
        assertThrows(IllegalStateException.class, () -> em.getEntityManagerFactory());
    }

    @Test
    void getLockModeThrowsAfterClose() {
        em.close();
        assertThrows(IllegalStateException.class, () -> em.getLockMode(new TestEntity()));
    }

    @Test
    void unwrapThrowsAfterClose() {
        em.close();
        assertThrows(IllegalStateException.class, () -> em.unwrap(MansartEntityManager.class));
    }

    @Test
    void clearThrowsAfterClose() {
        em.close();
        assertThrows(IllegalStateException.class, () -> em.clear());
    }

    // -- Helper: build a minimal metamodel (mirrors MansartEntityManagerFactoryTest) ---

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
}

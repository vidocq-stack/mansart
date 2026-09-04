/*
 * SPDX-License-Identifier: EPL-2.0 OR EUPL-1.2 OR GPL-2.0-or-later
 */
package io.vidocq.mansart.persistence.core;

import jakarta.persistence.PersistenceUnitUtil;
import jakarta.persistence.metamodel.Attribute;

/**
 * Mansart implementation of Jakarta Persistence PersistenceUnitUtil.
 *
 * <p>Entity-level operations (getIdentifier, getVersion, isLoaded, load)
 * require the entity metadata model which is built in a later milestone.
 * Until then, all methods throw {@link UnsupportedOperationException}.
 */
public class MansartPersistenceUnitUtil implements PersistenceUnitUtil {

    @Override
    public boolean isLoaded(Object entity, String attributeName) {
        throw new UnsupportedOperationException("not implemented: isLoaded(Object, String)");
    }

    @Override
    public <E> boolean isLoaded(E entity, Attribute<? super E, ?> attribute) {
        throw new UnsupportedOperationException("not implemented: isLoaded(E, Attribute)");
    }

    @Override
    public boolean isLoaded(Object entity) {
        throw new UnsupportedOperationException("not implemented: isLoaded(Object)");
    }

    @Override
    public void load(Object entity, String attributeName) {
        throw new UnsupportedOperationException("not implemented: load(Object, String)");
    }

    @Override
    public <E> void load(E entity, Attribute<? super E, ?> attribute) {
        throw new UnsupportedOperationException("not implemented: load(E, Attribute)");
    }

    @Override
    public void load(Object entity) {
        throw new UnsupportedOperationException("not implemented: load(Object)");
    }

    @Override
    public boolean isInstance(Object entity, Class<?> entityClass) {
        throw new UnsupportedOperationException("not implemented: isInstance");
    }

    @Override
    public <T> Class<? extends T> getClass(T entity) {
        throw new UnsupportedOperationException("not implemented: getClass");
    }

    @Override
    public Object getIdentifier(Object entity) {
        throw new UnsupportedOperationException("not implemented: getIdentifier");
    }

    @Override
    public Object getVersion(Object entity) {
        throw new UnsupportedOperationException("not implemented: getVersion");
    }
}

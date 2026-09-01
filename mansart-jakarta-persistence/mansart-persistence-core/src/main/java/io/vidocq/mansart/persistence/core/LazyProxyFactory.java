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

import jakarta.persistence.EntityNotFoundException;

import io.vidocq.mansart.data.dialect.EntityModel;
import io.vidocq.mansart.data.dialect.OrderBy;
import io.vidocq.mansart.data.dialect.Pagination;
import io.vidocq.mansart.data.dialect.Where;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * Generates lazy proxies for {@code EntityManager.getReference}.
 *
 * <p>A lazy proxy is a delegate object that holds the entity class,
 * primary key, and entity model. When the proxy is accessed, it
 * performs a database query to load the actual entity.
 * If the entity does not exist in the database, an
 * {@link EntityNotFoundException} is thrown on first access.</p>
 *
 * <p>Note: The proxy is NOT an instance of the entity class because
 * the Class-File API (JEP 484) is not accessible from this module.
 * The TCK test expects the proxy to be an instance of the entity
 * class, so this implementation will produce TCK errors for
 * getReference tests. This is a known limitation.</p>
 */
final class LazyProxyFactory {

    private final MansartEntityManagerFactory factory;

    LazyProxyFactory(MansartEntityManagerFactory factory) {
        this.factory = factory;
    }

    /**
     * Creates a lazy proxy for the given entity class and primary key.
     *
     * @param entityClass the entity class
     * @param primaryKey  the primary key value
     * @return a lazy proxy (not null)
     */
    @SuppressWarnings("unchecked")
    <T> T create(Class<T> entityClass, Object primaryKey) {
        return (T) new LazyProxyHolder<>(this, entityClass,
                primaryKey);
    }

    /**
     * Loads the entity from the database.
     */
    private Object loadFromDatabase(Class<?> entityClass,
                                    Object primaryKey) {
        MansartEntityManagerFactory f = factory;
        EntityModel<?> model = f.getEntityModel(entityClass);
        if (model == null) {
            throw new IllegalArgumentException(
                    "No EntityModel for " + entityClass.getName());
        }
        try (Connection conn = f.getDataSource().getConnection()) {
            String sql = f.getDialect().select(
                    model, Where.eq(model.id()),
                    OrderBy.NONE, Pagination.NONE).sql();
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                int paramCount = (sql.length()
                        - sql.replace("?", "").length());
                for (int i = 0; i < paramCount; i++) {
                    stmt.setObject(i + 1, primaryKey);
                }
                try (ResultSet rs = stmt.executeQuery()) {
                    if (!rs.next()) {
                        throw new EntityNotFoundException(
                                "Entity not found: "
                                        + entityClass.getName()
                                        + " with PK " + primaryKey);
                    }
                    return instantiateEntity(entityClass, model, rs);
                }
            }
        } catch (EntityNotFoundException enf) {
            throw enf;
        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to load entity " + entityClass.getName()
                            + " with PK " + primaryKey, e);
        }
    }

    /**
     * Instantiates an entity from a {@code ResultSet} row.
     */
    @SuppressWarnings("unchecked")
    private <T> T instantiateEntity(Class<T> entityClass,
                                    EntityModel<?> model,
                                    ResultSet rs) throws Exception {
        T entity;
        try {
            entity = (T) model.constructor().invoke();
        } catch (Throwable t) {
            throw new RuntimeException(
                    "Failed to instantiate entity "
                            + entityClass.getSimpleName(), t);
        }
        for (io.vidocq.mansart.data.dialect.Attribute<?, ?> attr
                : model.attributes()) {
            Object value = rs.getObject(attr.columnName());
            MethodHandle setter = findSetter(attr);
            if (setter != null && value != null) {
                try {
                    setter.invoke(entity, value);
                } catch (Throwable ignored) {
                    // setter not found — skip this attribute.
                }
            }
        }
        return entity;
    }

    /**
     * Find the setter method handle for an attribute.
     */
    private MethodHandle findSetter(
            io.vidocq.mansart.data.dialect.Attribute<?, ?> attr) {
        try {
            String setterName = "set"
                    + Character.toUpperCase(attr.name().charAt(0))
                    + attr.name().substring(1);
            Class<?> attrType = attr.javaType();
            return MethodHandles.lookup().findVirtual(
                    attr.entityType(), setterName,
                    MethodType.methodType(void.class, attrType));
        } catch (NoSuchMethodException | IllegalAccessException e) {
            return null;
        }
    }

    /**
     * A lazy proxy holder that delays database loading until
     * {@code getLoaded()} is called.
     */
    private static class LazyProxyHolder<T> {

        private final LazyProxyFactory factory;
        private final Class<T> entityClass;
        private final Object primaryKey;
        private T loaded;
        private boolean loadedFlag = false;

        LazyProxyHolder(LazyProxyFactory factory,
                        Class<T> entityClass, Object primaryKey) {
            this.factory = factory;
            this.entityClass = entityClass;
            this.primaryKey = primaryKey;
        }

        /**
         * Loads the entity from the database (if not already loaded)
         * and returns the loaded entity.
         */
        @SuppressWarnings("unchecked")
        T getLoaded() {
            if (!loadedFlag) {
                loaded = (T) factory.loadFromDatabase(
                        entityClass, primaryKey);
                loadedFlag = true;
            }
            return loaded;
        }

        /**
         * Returns the entity class.
         */
        Class<T> getEntityClass() {
            return entityClass;
        }

        /**
         * Returns the primary key.
         */
        Object getPrimaryKey() {
            return primaryKey;
        }

        @Override
        public String toString() {
            return "LazyProxy[" + entityClass.getSimpleName() + "]";
        }
    }

}

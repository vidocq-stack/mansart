/*
 * SPDX-License-Identifier: EPL-2.0 OR EUPL-1.2 OR GPL-2.0-or-later
 */
package io.vidocq.mansart.persistence.core.runtime;

import io.vidocq.mansart.persistence.spi.Attribute;
import io.vidocq.mansart.persistence.spi.EntityModel;

import java.util.ArrayList;
import java.util.List;

/**
 * In-memory {@link EntityModel} built at bootstrap by {@link RuntimeEntityModelBuilder}
 * for entities that neither APT nor the Maven plugin reached (Tier 3 fallback).
 *
 * @param <T> the entity type
 */
final class RuntimeEntityModel<T> implements EntityModel<T> {

    private final Class<T> entityClass;
    private final String tableName;
    private final String schema;
    private final String catalog;
    private final List<Attribute<?, ?>> attributes;

    RuntimeEntityModel(Class<T> entityClass, String tableName, String schema, String catalog,
                       List<Attribute<?, ?>> attributes) {
        this.entityClass = entityClass;
        this.tableName = tableName;
        this.schema = schema;
        this.catalog = catalog;
        this.attributes = List.copyOf(attributes);
    }

    @Override
    public Class<T> getEntityClass() {
        return entityClass;
    }

    @Override
    public String getTableName() {
        return tableName;
    }

    @Override
    public String getSchema() {
        return schema;
    }

    @Override
    public String getCatalog() {
        return catalog;
    }

    @Override
    public List<Attribute<?, ?>> getAttributes() {
        return attributes;
    }

    @Override
    public Attribute<?, ?> getAttribute(String name) {
        for (Attribute<?, ?> attr : attributes) {
            if (attr.getName().equals(name)) {
                return attr;
            }
        }
        return null;
    }

    @Override
    public List<Attribute<?, ?>> getIdAttributes() {
        List<Attribute<?, ?>> ids = new ArrayList<>();
        for (Attribute<?, ?> attr : attributes) {
            if (attr.isId()) {
                ids.add(attr);
            }
        }
        return ids;
    }

    @Override
    public Attribute<?, ?> getVersionAttribute() {
        for (Attribute<?, ?> attr : attributes) {
            if (attr.isVersion()) {
                return attr;
            }
        }
        return null;
    }
}

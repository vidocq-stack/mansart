/*
 * SPDX-License-Identifier: EPL-2.0 OR EUPL-1.2 OR GPL-2.0-or-later
 */
package io.vidocq.mansart.persistence.core.runtime;

import io.vidocq.mansart.persistence.spi.Attribute;
import io.vidocq.mansart.persistence.spi.EntityModel;

import java.lang.invoke.MethodHandle;

/**
 * In-memory {@link Attribute} built at bootstrap by {@link RuntimeEntityModelBuilder}.
 * Field access (get/set) is not supported — Tier 3 field access requires generated
 * hidden classes (M3-JP-20).
 *
 * @param <T> the entity type
 * @param <V> the attribute value type
 */
final class RuntimeAttribute<T, V> implements Attribute<T, V> {

    private final String name;
    private final String columnName;
    private final EntityModel<T> entityModel;
    private final Class<V> javaType;
    private final boolean nullable;
    private final boolean id;
    private final boolean version;
    private final boolean unique;
    private final boolean column;
    private final int length;
    private final int precision;
    private final int scale;
    private final boolean insertable;
    private final boolean updatable;
    private final String columnDefinition;

    RuntimeAttribute(String name, String columnName, EntityModel<T> entityModel,
                     Class<V> javaType, boolean nullable, boolean id, boolean version,
                     boolean unique, boolean column,
                     int length, int precision, int scale,
                     boolean insertable, boolean updatable, String columnDefinition) {
        this.name = name;
        this.columnName = columnName;
        this.entityModel = entityModel;
        this.javaType = javaType;
        this.nullable = nullable;
        this.id = id;
        this.version = version;
        this.unique = unique;
        this.column = column;
        this.length = length;
        this.precision = precision;
        this.scale = scale;
        this.insertable = insertable;
        this.updatable = updatable;
        this.columnDefinition = columnDefinition;
    }

    @Override
    public String getName() { return name; }

    @Override
    public String getColumnName() { return columnName; }

    @Override
    public EntityModel<T> getEntityModel() { return entityModel; }

    @Override
    public Class<V> getJavaType() { return javaType; }

    @Override
    public boolean isNullable() { return nullable; }

    @Override
    public boolean isId() { return id; }

    @Override
    public boolean isVersion() { return version; }

    @Override
    public boolean isUnique() { return unique; }

    @Override
    public boolean isColumn() { return column; }

    @Override
    public int getLength() { return length; }

    @Override
    public int getPrecision() { return precision; }

    @Override
    public int getScale() { return scale; }

    @Override
    public boolean isInsertable() { return insertable; }

    @Override
    public boolean isUpdatable() { return updatable; }

    @Override
    public String getColumnDefinition() { return columnDefinition; }

    @Override
    public MethodHandle getGetter() {
        throw new UnsupportedOperationException(
                "not implemented: tier-3 field access — use generated hidden class (M3-JP-20)");
    }

    @Override
    public MethodHandle getSetter() {
        throw new UnsupportedOperationException(
                "not implemented: tier-3 field access — use generated hidden class (M3-JP-20)");
    }

    @Override
    @SuppressWarnings("unchecked")
    public V get(T instance) {
        throw new UnsupportedOperationException(
                "not implemented: tier-3 field access — use generated hidden class (M3-JP-20)");
    }

    @Override
    public void set(T instance, V value) {
        throw new UnsupportedOperationException(
                "not implemented: tier-3 field access — use generated hidden class (M3-JP-20)");
    }
}

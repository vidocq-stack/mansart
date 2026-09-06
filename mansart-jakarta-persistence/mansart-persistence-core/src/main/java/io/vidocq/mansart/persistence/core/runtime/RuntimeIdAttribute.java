/*
 * SPDX-License-Identifier: EPL-2.0 OR EUPL-1.2 OR GPL-2.0-or-later
 */
package io.vidocq.mansart.persistence.core.runtime;

import io.vidocq.mansart.persistence.spi.EntityModel;
import io.vidocq.mansart.persistence.spi.IdAttribute;

import java.lang.invoke.MethodHandle;

/**
 * In-memory {@link IdAttribute} built at bootstrap by {@link RuntimeEntityModelBuilder}.
 * Field access (get/set) is not supported — Tier 3 field access requires generated
 * hidden classes (M3-JP-20).
 *
 * @param <T> the entity type
 * @param <V> the ID value type
 */
record RuntimeIdAttribute<T, V>(
        String name,
        String columnName,
        EntityModel<T> entityModel,
        Class<V> javaType,
        boolean nullable,
        boolean version,
        boolean unique,
        boolean column,
        IdAttribute.GenerationStrategy generationStrategy,
        String generator
) implements IdAttribute<T, V> {

    @Override
    public String getName() { return name(); }

    @Override
    public String getColumnName() { return columnName(); }

    @Override
    public EntityModel<T> getEntityModel() { return entityModel(); }

    @Override
    public Class<V> getJavaType() { return javaType(); }

    @Override
    public boolean isNullable() { return nullable(); }

    @Override
    public boolean isId() { return true; }

    @Override
    public boolean isVersion() { return version(); }

    @Override
    public boolean isUnique() { return unique(); }

    @Override
    public boolean isColumn() { return column(); }

    @Override
    public IdAttribute.GenerationStrategy getGenerationStrategy() {
        return generationStrategy();
    }

    @Override
    public String getGenerator() {
        return generator();
    }

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

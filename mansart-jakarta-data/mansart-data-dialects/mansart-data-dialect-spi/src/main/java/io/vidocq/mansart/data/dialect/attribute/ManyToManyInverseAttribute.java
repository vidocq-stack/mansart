/*
 * Copyright (c) 2026 Yann Blazart, Antoine Sabot-Durand and the Vidocq contributors
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * This Source Code may also be made available under the following Secondary
 * Licenses when the conditions for such availability set forth in the Eclipse
 * Public License, v. 2.0 are satisfied:
 * https://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 *
 * SPDX-License-Identifier: EPL-2.0 OR EUPL-1.2 OR GPL-2.0-or-later
 */
package io.vidocq.mansart.data.dialect.attribute;

import io.vidocq.mansart.data.dialect.Attribute;

import java.lang.invoke.MethodHandle;

/**
 * Many-to-many relationship attribute on the inverse side (mapped by another entity).
 *
 * <p>Stores join table metadata: {@code joinTableName} is the join table name,
 * {@code inverseJoinColumnName} is the FK column on the join table pointing to
 * this entity's primary key, and {@code joinColumnName} is the FK column pointing
 * to the owning entity's primary key.</p>
 *
 * <p>{@code elementClass} is the target entity class (the owning side entity).</p>
 *
 * <p>Getter returns the collection of owning-side entities. Setter accepts
 * a collection of owning-side entities.</p>
 */
public record ManyToManyInverseAttribute<E, V>(
        String name,
        String joinTableName,
        String joinColumnName,
        String inverseJoinColumnName,
        Class<V> elementClass,
        Class<E> entityType,
        boolean nullable,
        boolean inverseSide,
        String mappedBy,
        MethodHandle getter,
        MethodHandle setter
) implements Attribute<E, V> {

    @Override
    public String columnName() { return joinTableName; }

    @Override
    public Class<V> javaType() { return elementClass; }

    @Override
    public String javaTypeFqn() { return elementClass.getName(); }

    @Override
    public boolean unique() { return false; }
}

/*
 * Copyright (c) 2026 Yann Blazart, Antoine Sabot-Durand and the Vidocq contributors
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0 OR EUPL-1.2 OR GPL-2.0-or-later
 */
package io.vidocq.mansart.persistence.core.metamodel;

import jakarta.persistence.metamodel.MappedSuperclassType;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.IdentifiableType;
import jakarta.persistence.metamodel.Attribute;

import java.util.List;
import java.util.Set;

/**
 * Implementation of {@link MappedSuperclassType} for the Mansart persistence provider.
 *
 * <p>A mapped superclass is an identifiable type that cannot be queried directly.
 * Its {@code getIdClassAttributes()} throws {@link IllegalArgumentException} per the
 * Jakarta Persistence spec.</p>
 *
 * @param <X> the represented mapped superclass type
 */
@SuppressWarnings("unchecked")
public final class MappedSuperclassTypeImpl<X> extends IdentifiableTypeImpl<X>
        implements MappedSuperclassType<X> {

    /**
     * Creates a mapped superclass type.
     *
     * @param javaType the Java type of the mapped superclass
     * @param entityName the persistence unit entity name
     * @param declaredAttributes the declared attributes
     * @param supertype the supertype (may be null)
     * @param idAttribute the identifier attribute (may be null)
     * @param versionAttribute the version attribute (may be null)
     * @param hasSingleIdAttribute whether there is a single identifier attribute
     */
    @SuppressWarnings("rawtypes")
    public MappedSuperclassTypeImpl(Class<?> javaType, String entityName,
                                    List<Attribute<? super Object, ?>> declaredAttributes,
                                    IdentifiableType<? super X> supertype,
                                    SingularAttribute<? super X, ?> idAttribute,
                                    SingularAttribute<? super X, ?> versionAttribute,
                                    boolean hasSingleIdAttribute) {
        super(javaType, entityName, declaredAttributes, supertype,
              idAttribute, versionAttribute, hasSingleIdAttribute);
    }

    @Override
    public Set<SingularAttribute<? super X, ?>> getIdClassAttributes() {
        throw new IllegalArgumentException(
                "getIdClassAttributes() not supported on MappedSuperclassType");
    }
}

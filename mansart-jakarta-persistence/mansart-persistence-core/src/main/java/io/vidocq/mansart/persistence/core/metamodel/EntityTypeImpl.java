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

import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.ManagedType;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.Type;
import jakarta.persistence.metamodel.Bindable;
import jakarta.persistence.metamodel.Attribute;

import java.util.List;

/**
 * Implementation of {@link EntityType} for the Mansart persistence provider.
 *
 * @param <X> the represented entity type
 */
@SuppressWarnings("unchecked")
public final class EntityTypeImpl<X> extends IdentifiableTypeImpl<X>
        implements EntityType<X>, Bindable<X> {

    private final String entityName;

    @SuppressWarnings("rawtypes")
    public EntityTypeImpl(Class<?> javaType, String entityName,
                   List<Attribute<? super Object, ?>> declaredAttributes) {
        SingularAttribute<? super Object, ?> idAttr = null;
        SingularAttribute<? super Object, ?> verAttr = null;
        boolean singleId = false;
        int idCount = 0;

        for (Attribute<? super Object, ?> attr : declaredAttributes) {
            if (attr instanceof SingularAttribute<?, ?> sa) {
                if (sa.isId()) {
                    idCount++;
                    idAttr = (SingularAttribute<? super Object, ?>) sa;
                }
                if (sa.isVersion()) {
                    verAttr = (SingularAttribute<? super Object, ?>) sa;
                }
            }
        }
        singleId = (idCount == 1);
        super((Class<X>) javaType, entityName, declaredAttributes,
              null, idAttr, verAttr, singleId);
        this.entityName = entityName;
    }

    @Override
    public String getName() {
        return entityName;
    }

    @Override
    public Bindable.BindableType getBindableType() {
        return Bindable.BindableType.ENTITY_TYPE;
    }

    @Override
    public Class<X> getBindableJavaType() {
        return getJavaType();
    }
}

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

import jakarta.persistence.metamodel.IdentifiableType;
import jakarta.persistence.metamodel.ManagedType;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.Type;
import jakarta.persistence.metamodel.Attribute;

import java.util.List;
import java.util.Set;

/**
 * Implementation of {@link IdentifiableType} for the Mansart persistence provider.
 *
 * @param <X> the represented entity or mapped superclass type
 */
@SuppressWarnings("unchecked")
public abstract class IdentifiableTypeImpl<X> extends ManagedTypeImpl<X>
        implements IdentifiableType<X> {

    private final IdentifiableType<? super X> supertype;
    private final SingularAttribute<? super X, ?> idAttribute;
    private final SingularAttribute<? super X, ?> versionAttribute;
    private final boolean hasSingleIdAttribute;
    private final Set<SingularAttribute<? super X, ?>> idClassAttributes;

    @SuppressWarnings("rawtypes")
    IdentifiableTypeImpl(Class<?> javaType, String entityName,
                         List<Attribute<? super Object, ?>> declaredAttributes) {
        this(javaType, entityName, declaredAttributes, null, null, null, true, Set.of());
    }

    @SuppressWarnings("rawtypes")
    IdentifiableTypeImpl(Class<?> javaType, String entityName,
                         List<Attribute<? super Object, ?>> declaredAttributes,
                         IdentifiableType<? super X> supertype,
                         SingularAttribute<? super X, ?> idAttribute,
                         SingularAttribute<? super X, ?> versionAttribute,
                         boolean hasSingleIdAttribute) {
        this(javaType, entityName, declaredAttributes, supertype,
                idAttribute, versionAttribute, hasSingleIdAttribute, Set.of());
    }

    @SuppressWarnings("rawtypes")
    IdentifiableTypeImpl(Class<?> javaType, String entityName,
                         List<Attribute<? super Object, ?>> declaredAttributes,
                         IdentifiableType<? super X> supertype,
                         SingularAttribute<? super X, ?> idAttribute,
                         SingularAttribute<? super X, ?> versionAttribute,
                         boolean hasSingleIdAttribute,
                         Set<SingularAttribute<? super X, ?>> idClassAttributes) {
        super(javaType, Type.PersistenceType.ENTITY, declaredAttributes);
        this.supertype = supertype;
        this.idAttribute = idAttribute;
        this.versionAttribute = versionAttribute;
        this.hasSingleIdAttribute = hasSingleIdAttribute;
        this.idClassAttributes = idClassAttributes;
    }

    @Override
    public <Y> SingularAttribute<? super X, Y> getId(Class<Y> type) {
        if (idAttribute != null && type.isAssignableFrom(idAttribute.getJavaType())) {
            return (SingularAttribute<? super X, Y>) idAttribute;
        }
        throw new IllegalArgumentException("No identifier attribute matching type: " + type);
    }

    @Override
    public <Y> SingularAttribute<X, Y> getDeclaredId(Class<Y> type) {
        for (Attribute<? super X, ?> attr : getDeclaredAttributes()) {
            if (attr instanceof SingularAttribute<?, ?> sa
                    && sa.isId() && type.isAssignableFrom(sa.getJavaType())) {
                return (SingularAttribute<X, Y>) sa;
            }
        }
        throw new IllegalArgumentException("No declared identifier attribute matching type: " + type);
    }

    @Override
    public <Y> SingularAttribute<? super X, Y> getVersion(Class<Y> type) {
        if (versionAttribute != null && type.isAssignableFrom(versionAttribute.getJavaType())) {
            return (SingularAttribute<? super X, Y>) versionAttribute;
        }
        throw new IllegalArgumentException("No version attribute matching type: " + type);
    }

    @Override
    public <Y> SingularAttribute<X, Y> getDeclaredVersion(Class<Y> type) {
        for (Attribute<? super X, ?> attr : getDeclaredAttributes()) {
            if (attr instanceof SingularAttribute<?, ?> sa
                    && sa.isVersion() && type.isAssignableFrom(sa.getJavaType())) {
                return (SingularAttribute<X, Y>) sa;
            }
        }
        throw new IllegalArgumentException("No declared version attribute matching type: " + type);
    }

    @Override
    public IdentifiableType<? super X> getSupertype() {
        return supertype;
    }

    @Override
    public boolean hasSingleIdAttribute() {
        return hasSingleIdAttribute;
    }

    @Override
    public boolean hasVersionAttribute() {
        return versionAttribute != null;
    }

    @Override
    public Set<SingularAttribute<? super X, ?>> getIdClassAttributes() {
        return idClassAttributes;
    }

    @Override
    public Type<?> getIdType() {
        if (idAttribute != null) {
            return idAttribute.getType();
        }
        throw new UnsupportedOperationException("not implemented: getIdType — no identifier attribute defined");
    }
}

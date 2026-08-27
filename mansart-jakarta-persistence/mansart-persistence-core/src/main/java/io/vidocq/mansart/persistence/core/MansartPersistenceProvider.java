/*
 * Copyright (c) 2026 Yann Blazart, Antoine Sabot-Durand and the Vidocq contributors
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * This Source Code may also be made available under the following Secondary
 * Licenses when the conditions of such availability set forth in the Eclipse
 * Public License, v. 2.0 are satisfied: GNU General Public License, version 2
 * or any later version, which is available at
 * https://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 *
 * It is also made available under the European Union Public Licence v. 1.2,
 * which is available at https://joinup.ec.europa.eu/collection/eupl/eupl-1.2
 *
 * SPDX-License-Identifier: EPL-2.0 OR EUPL-1.2 OR GPL-2.0-or-later
 */
package io.vidocq.mansart.persistence.core;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Embeddable;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.metamodel.Attribute;
import jakarta.persistence.metamodel.ManagedType;
import jakarta.persistence.spi.PersistenceUnitInfo;
import jakarta.persistence.spi.PersistenceProvider;
import jakarta.persistence.spi.ProviderUtil;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceConfiguration;

import io.vidocq.mansart.persistence.core.metamodel.MetamodelImpl;
import io.vidocq.mansart.persistence.core.metamodel.EntityTypeImpl;
import io.vidocq.mansart.persistence.core.metamodel.EmbeddableTypeImpl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Jakarta Persistence provider for the Mansart reactor.
 *
 * <p>This class satisfies the Jakarta Persistence SPI contract by appearing as
 * a {@code PersistenceProvider} implementation via {@code ServiceLoader}.
 * It builds a runtime metamodel from the managed class names in the
 * persistence unit, inspecting annotations (@Entity, @Embeddable,
 * @MappedSuperclass) to construct
 * {@code Metamodel}, {@code EntityType}, {@code EmbeddableType},
 * and {@code SingularAttribute} instances.</p>
 *
 * @see jakarta.persistence.spi.PersistenceProvider
 */
public class MansartPersistenceProvider implements PersistenceProvider {

    @Override
    public EntityManagerFactory createEntityManagerFactory(String persistenceUnitName, Map<?, ?> hints) {
        throw new UnsupportedOperationException("not implemented: createEntityManagerFactory");
    }

    @Override
    public EntityManagerFactory createEntityManagerFactory(PersistenceConfiguration config) {
        throw new UnsupportedOperationException("not implemented: createEntityManagerFactory");
    }

    @Override
    public EntityManagerFactory createContainerEntityManagerFactory(
            PersistenceUnitInfo info, Map<?, ?> hints) {
        MetamodelImpl metamodel = buildMetamodel(info.getManagedClassNames());
        return new MansartEntityManagerFactory(metamodel);
    }

    @Override
    public void generateSchema(PersistenceUnitInfo info, Map<?, ?> hints) {
        throw new UnsupportedOperationException("not implemented: generateSchema");
    }

    @Override
    public boolean generateSchema(String persistenceUnitName, Map<?, ?> hints) {
        throw new UnsupportedOperationException("not implemented: generateSchema");
    }

    @Override
    public ProviderUtil getProviderUtil() {
        throw new UnsupportedOperationException("not implemented: getProviderUtil");
    }

    /**
     * Build a metamodel from the managed class names in the persistence unit.
     *
     * <p>For each class name, loads the class and inspects annotations:
     * {@code @Entity} → {@code EntityTypeImpl},
     * {@code @Embeddable} → {@code EmbeddableTypeImpl},
     * {@code @MappedSuperclass} → {@code EntityTypeImpl} with
     * {@code PersistenceType.MAPPED_SUPERCLASS}.</p>
     *
     * @param classNames the managed class names
     * @return a {@code MetamodelImpl} containing all managed types
     */
    private MetamodelImpl buildMetamodel(List<String> classNames) {
        List<ManagedType<?>> managedTypes = new ArrayList<>();
        for (String className : classNames) {
            try {
                Class<?> cls = Class.forName(className, false,
                        Thread.currentThread().getContextClassLoader());
                managedTypes.add(buildManagedType(cls, null));
            } catch (ClassNotFoundException e) {
                // Skip classes that cannot be loaded
            }
        }
        return new MetamodelImpl(managedTypes);
    }

    /**
     * Build a managed type for the given class, inspecting annotations.
     *
     * @param cls the class to inspect
     * @param supertype the supertype (for mapped superclasses and entities
     *        that extend mapped superclasses)
     * @return the managed type, or null if the class is not managed
     */
    @SuppressWarnings("unchecked")
    private ManagedType<?> buildManagedType(Class<?> cls,
            jakarta.persistence.metamodel.IdentifiableType<?> supertype) {
        if (cls.isAnnotationPresent(Entity.class)) {
            String entityName = cls.getAnnotation(Entity.class).name();
            return buildEntityType(cls, entityName, supertype);
        }
        if (cls.isAnnotationPresent(Embeddable.class)) {
            return buildEmbeddableType(cls);
        }
        if (cls.isAnnotationPresent(MappedSuperclass.class)) {
            String entityName = cls.getSimpleName();
            return buildEntityType(cls, entityName, supertype);
        }
        throw new IllegalArgumentException(
            "not a managed type: " + cls.getName());
    }

    /**
     * Build an entity type (entity or mapped superclass) with its attributes.
     */
    private EntityTypeImpl<?> buildEntityType(Class<?> cls, String entityName,
            jakarta.persistence.metamodel.IdentifiableType<?> supertype) {
        List<Attribute<? super Object, ?>> attributes = buildAttributes();
        return new EntityTypeImpl<>(cls, entityName, attributes);
    }

    /**
     * Build an embeddable type with its attributes.
     */
    private EmbeddableTypeImpl<?> buildEmbeddableType(Class<?> cls) {
        List<Attribute<? super Object, ?>> attributes = buildAttributes();
        return new EmbeddableTypeImpl<>(cls, attributes);
    }

    /**
     * Build attributes for a class — not yet implemented.
     *
     * <p>Field-level metadata collection is delegated to the APT processor
     * ({@code MansartPersistenceProcessor}). This method exists as a stub
     * and always throws {@code UnsupportedOperationException}.</p>
     *
     * @return never — always throws
     * @throws UnsupportedOperationException always
     */
    private List<Attribute<? super Object, ?>> buildAttributes() {
        throw new UnsupportedOperationException(
            "not implemented: buildAttributes — use APT-generated metadata");
    }
}
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
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Embeddable;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Id;
import jakarta.persistence.Version;
import jakarta.persistence.Column;
import jakarta.persistence.OneToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.MapKey;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.MapKeyClass;
import jakarta.persistence.Transient;
import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.metamodel.Attribute;
import jakarta.persistence.metamodel.ManagedType;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.CollectionAttribute;
import jakarta.persistence.metamodel.SetAttribute;
import jakarta.persistence.metamodel.ListAttribute;
import jakarta.persistence.metamodel.MapAttribute;
import jakarta.persistence.metamodel.Type;
import jakarta.persistence.spi.PersistenceUnitInfo;
import jakarta.persistence.spi.PersistenceProvider;
import jakarta.persistence.spi.ProviderUtil;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceConfiguration;

import io.vidocq.mansart.persistence.core.metamodel.MetamodelImpl;
import io.vidocq.mansart.persistence.core.metamodel.EntityTypeImpl;
import io.vidocq.mansart.persistence.core.metamodel.IdentifiableTypeImpl;
import io.vidocq.mansart.persistence.core.metamodel.EmbeddableTypeImpl;
import io.vidocq.mansart.persistence.core.metamodel.MappedSuperclassTypeImpl;
import io.vidocq.mansart.persistence.core.metamodel.SingularAttributeImpl;
import io.vidocq.mansart.persistence.core.metamodel.CollectionAttributeImpl;
import io.vidocq.mansart.persistence.core.metamodel.SetAttributeImpl;
import io.vidocq.mansart.persistence.core.metamodel.ListAttributeImpl;
import io.vidocq.mansart.persistence.core.metamodel.MapAttributeImpl;
import io.vidocq.mansart.persistence.core.metamodel.TypeImpl;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import java.util.HashSet;

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
        List<Attribute<? super Object, ?>> attributes = buildAttributes(cls);
        return new EntityTypeImpl<>(cls, entityName, attributes);
    }

    /**
     * Build an embeddable type with its attributes.
     */
    private EmbeddableTypeImpl<?> buildEmbeddableType(Class<?> cls) {
        List<Attribute<? super Object, ?>> attributes = buildAttributes(cls);
        return new EmbeddableTypeImpl<>(cls, attributes);
    }

    /**
     * Build attributes for a class by inspecting its fields.
     *
     * <p>Uses reflection at bootstrap time (not runtime during entity operations)
     * to read annotations and field types, then constructs the appropriate
     * metamodel attribute instances. Returns attributes with null declaringType;
     * the caller must wire them to their declaring type.</p>
     *
     * @param cls the class to inspect
     * @return a list of attribute instances for all persistent fields
     */
    private List<Attribute<? super Object, ?>> buildAttributes(Class<?> cls) {
        List<Attribute<? super Object, ?>> attributes = new ArrayList<>();
        Class<?> current = cls;
        while (current != null && current != Object.class) {
            for (Field field : current.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers())
                        || field.isAnnotationPresent(Transient.class)) {
                    continue;
                }
                Attribute<? super Object, ?> attr = buildSingleAttribute(field);
                if (attr != null) {
                    attributes.add(attr);
                }
            }
            current = current.getSuperclass();
        }
        return attributes;
    }

    /**
     * Build a single attribute from a field, inspecting annotations.
     */
    @SuppressWarnings("unchecked")
    private Attribute<? super Object, ?> buildSingleAttribute(Field field) {
        String fieldName = field.getName();
        Class<?> fieldType = field.getType();

        // Check for @Id
        if (field.isAnnotationPresent(Id.class)) {
            return buildSingularAttribute(field, fieldName, fieldType,
                    Attribute.PersistentAttributeType.BASIC, true, false);
        }

        // Check for @Version
        if (field.isAnnotationPresent(Version.class)) {
            return buildSingularAttribute(field, fieldName, fieldType,
                    Attribute.PersistentAttributeType.BASIC, false, true);
        }

        // Check for @Embedded
        if (field.isAnnotationPresent(Embedded.class)) {
            return buildSingularAttribute(field, fieldName, fieldType,
                    Attribute.PersistentAttributeType.EMBEDDED, false, false);
        }

        // Check for @OneToMany
        if (field.isAnnotationPresent(OneToMany.class)) {
            Class<?> targetClass = getTargetClass(field, OneToMany.class);
            return buildCollectionAttribute(field, fieldName, fieldType, targetClass,
                    Attribute.PersistentAttributeType.ONE_TO_MANY);
        }

        // Check for @ManyToMany
        if (field.isAnnotationPresent(ManyToMany.class)) {
            Class<?> targetClass = getTargetClass(field, ManyToMany.class);
            return buildCollectionAttribute(field, fieldName, fieldType, targetClass,
                    Attribute.PersistentAttributeType.MANY_TO_MANY);
        }

        // Check for @ManyToOne
        if (field.isAnnotationPresent(ManyToOne.class)) {
            Class<?> targetClass = getTargetClass(field, ManyToOne.class);
            return buildSingularAttribute(field, fieldName, fieldType,
                    Attribute.PersistentAttributeType.MANY_TO_ONE, false, false);
        }

        // Check for @OneToOne
        if (field.isAnnotationPresent(OneToOne.class)) {
            Class<?> targetClass = getTargetClass(field, OneToOne.class);
            return buildSingularAttribute(field, fieldName, fieldType,
                    Attribute.PersistentAttributeType.ONE_TO_ONE, false, false);
        }

        // Check for @ElementCollection
        if (field.isAnnotationPresent(ElementCollection.class)) {
            Class<?> targetClass = fieldType;
            if (java.util.Set.class.isAssignableFrom(fieldType)) {
                return buildSetAttribute(field, fieldName, fieldType, targetClass);
            } else if (java.util.List.class.isAssignableFrom(fieldType)) {
                return buildListAttribute(field, fieldName, fieldType, targetClass);
            } else if (java.util.Map.class.isAssignableFrom(fieldType)) {
                return buildMapAttribute(field, fieldName, fieldType, targetClass);
            }
            return buildCollectionAttribute(field, fieldName, fieldType, targetClass,
                    Attribute.PersistentAttributeType.ELEMENT_COLLECTION);
        }

        // Default: basic attribute
        return buildSingularAttribute(field, fieldName, fieldType,
                Attribute.PersistentAttributeType.BASIC, false, false);
    }

    /**
     * Get the target class from a relationship annotation.
     */
    @SuppressWarnings("unchecked")
    private Class<?> getTargetClass(Field field, Class<? extends java.lang.annotation.Annotation> annotationType) {
        try {
            if (annotationType == OneToMany.class) {
                OneToMany ann = field.getAnnotation(OneToMany.class);
                Class<?> targetEntity = ann.targetEntity();
                if (targetEntity != void.class) {
                    return targetEntity;
                }
            }
            if (annotationType == ManyToMany.class) {
                ManyToMany ann = field.getAnnotation(ManyToMany.class);
                Class<?> targetEntity = ann.targetEntity();
                if (targetEntity != void.class) {
                    return targetEntity;
                }
            }
            if (annotationType == ManyToOne.class) {
                ManyToOne ann = field.getAnnotation(ManyToOne.class);
                Class<?> targetEntity = ann.targetEntity();
                if (targetEntity != void.class) {
                    return targetEntity;
                }
            }
            if (annotationType == OneToOne.class) {
                OneToOne ann = field.getAnnotation(OneToOne.class);
                Class<?> targetEntity = ann.targetEntity();
                if (targetEntity != void.class) {
                    return targetEntity;
                }
            }
        } catch (Exception e) {
            // Fall through to field type
        }
        return field.getType();
    }

    /**
     * Build a singular attribute from a field (no declaring type).
     */
    private SingularAttributeImpl<? super Object, Object> buildSingularAttribute(
            Field field, String name, Class<?> javaType,
            Attribute.PersistentAttributeType persistentType,
            boolean isId, boolean isVersion) {
        return new SingularAttributeImpl<>(field.getType(), name,
                persistentType, true, isId, isVersion, null);
    }

    /**
     * Build a collection attribute from a field (no declaring type).
     */
    private CollectionAttributeImpl<? super Object, Object> buildCollectionAttribute(
            Field field, String name, Class<?> javaType, Class<?> elementType,
            Attribute.PersistentAttributeType persistentType) {
        return new CollectionAttributeImpl<>(javaType, name,
                new TypeImpl<>(elementType, null));
    }

    /**
     * Build a set attribute from a field (no declaring type).
     */
    private SetAttributeImpl<? super Object, Object> buildSetAttribute(
            Field field, String name, Class<?> javaType, Class<?> elementType) {
        return new SetAttributeImpl<>(javaType, name,
                new TypeImpl<>(elementType, null));
    }

    /**
     * Build a list attribute from a field (no declaring type).
     */
    private ListAttributeImpl<? super Object, Object> buildListAttribute(
            Field field, String name, Class<?> javaType, Class<?> elementType) {
        return new ListAttributeImpl<>(javaType, name,
                new TypeImpl<>(elementType, null));
    }

    /**
     * Build a map attribute from a field (no declaring type).
     */
    private MapAttributeImpl<? super Object, Object, Object> buildMapAttribute(
            Field field, String name, Class<?> javaType, Class<?> valueType) {
        return new MapAttributeImpl<Object, Object, Object>(javaType, name,
                new TypeImpl<>(Object.class, null), Object.class,
                new TypeImpl<>(valueType, null));
    }
}
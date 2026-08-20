/*
 * Copyright (c) 2026 Yann Blazart, Antoine Sabot-Durand and the Vidocq contributors
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * This Source Code may also be made available under the following Secondary
 * Licenses when the conditions for such availability set forth in the Eclipse
 * Public License, v. 2.0 are satisfied: GNU General Public License, version 2
 * or any later version, which is available at
 * https://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 *
 * It is also made available under the European Union Public Licence v. 1.2,
 * which is available at
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 *
 * SPDX-License-Identifier: EPL-2.0 OR EUPL-1.2 OR GPL-2.0-or-later
 */
package io.vidocq.mansart.persistence.spi;

import java.util.List;
import java.util.Set;

/**
 * Runtime metadata for an entity class.
 *
 * <p>This interface provides access to entity metadata such as:
 * <ul>
 *   <li>Entity name and class</li>
 *   <li>Table name and schema</li>
 *   <li>Identifier attribute information</li>
 *   <li>Version attribute (for optimistic locking)</li>
 *   <li>Persistent attribute names</li>
 *   <li>Relationship metadata</li>
 * </ul>
 *
 * <p>Instances are created at bootstrap time and used by the persistence
 * context to manage entity state and persistence operations.
 */
public interface EntityMetadata {

    /**
     * Returns the entity class.
     *
     * @return the entity class, never {@code null}
     */
    Class<?> entityClass();

    /**
     * Returns the entity name as specified by {@code @Entity(name)} or
     * the unqualified class name if not specified.
     *
     * @return the entity name, never {@code null}
     */
    String entityName();

    /**
     * Returns the database table name for this entity.
     *
     * @return the table name, never {@code null}
     */
    String tableName();

    /**
     * Returns the database schema name for this entity.
     *
     * @return the schema name, or {@code null} if not specified
     */
    String schemaName();

    /**
     * Returns the name of the identifier attribute.
     *
     * @return the identifier attribute name, never {@code null}
     */
    String getIdAttributeName();

    /**
     * Returns the type of the identifier attribute.
     *
     * @return the identifier type, never {@code null}
     */
    Class<?> getIdAttributeType();

    /**
     * Returns the primary key metadata for this entity.
     *
     * @return the primary key metadata, never {@code null}
     */
    PrimaryKeyMetadata getPrimaryKeyMetadata();

    /**
     * Returns whether this entity has a version attribute for optimistic locking.
     *
     * @return {@code true} if a version attribute exists, {@code false} otherwise
     */
    boolean hasVersionAttribute();

    /**
     * Returns the name of the version attribute, or {@code null} if none exists.
     *
     * @return the version attribute name, or {@code null}
     */
    String getVersionAttributeName();

    /**
     * Returns the set of persistent attribute names.
     *
     * @return unmodifiable set of persistent attribute names, never {@code null}
     */
    Set<String> getPersistentAttributeNames();

    /**
     * Returns the list of persistent attribute names in declaration order.
     *
     * @return unmodifiable list of persistent attribute names, never {@code null}
     */
    List<String> getPersistentAttributeOrder();

    /**
     * Returns the entity state for the given entity instance.
     *
     * @param entity the entity instance
     * @return the current state of the entity
     */
    EntityState getState(Object entity);

    /**
     * Checks if the given class is an entity.
     *
     * @param clazz the class to check
     * @return {@code true} if the class is an entity, {@code false} otherwise
     */
    boolean isEntity(Class<?> clazz);

    /**
     * Returns the relationship metadata for the given attribute.
     *
     * @param attributeName the name of the attribute
     * @return the relationship metadata, or {@code null} if the attribute is not a relationship
     */
    RelationshipMetadata getRelationshipMetadata(String attributeName);

    /**
     * Returns whether the given attribute is a relationship.
     *
     * @param attributeName the name of the attribute
     * @return {@code true} if the attribute is a relationship, {@code false} otherwise
     */
    boolean isRelationship(String attributeName);

    /**
     * Returns the set of relationship attribute names.
     *
     * @return unmodifiable set of relationship attribute names, never {@code null}
     */
    Set<String> getRelationshipAttributeNames();

    /**
     * Returns the inheritance type for this entity.
     *
     * @return the inheritance type, or {@code null} if this entity is not part of an inheritance hierarchy
     */
    default jakarta.persistence.InheritanceType getInheritanceType() {
        return null;
    }

    /**
     * Returns the name of the discriminator column for this entity.
     *
     * @return the discriminator column name, or {@code null} if not using discriminator
     */
    default String getDiscriminatorColumn() {
        return null;
    }

    /**
     * Returns the discriminator value for this entity.
     *
     * @return the discriminator value, or {@code null} if not specified
     */
    default String getDiscriminatorValue() {
        return null;
    }

    /**
     * Returns whether this entity is the root of an inheritance hierarchy.
     *
     * @return {@code true} if this is the root entity, {@code false} otherwise
     */
    default boolean isInheritanceRoot() {
        return false;
    }

    /**
     * Returns the parent entity class if this entity extends another entity.
     *
     * @return the parent entity class, or {@code null} if this is a root entity or not part of an inheritance hierarchy
     */
    default Class<?> getParentEntityClass() {
        return null;
    }

    /**
     * Returns the list of direct subclass entity classes.
     *
     * @return unmodifiable list of subclass entity classes, never {@code null}
     */
    default java.util.List<Class<?>> getSubclassEntityClasses() {
        return java.util.Collections.emptyList();
    }

    // ----- APT-generated typed accessors (DEBT-05) -----

    /**
     * Returns a typed accessor for the given persistent attribute.
     *
     * <p>The returned {@code AttributeAccessor} holds a pre-resolved
     * {@link java.lang.invoke.MethodHandle} (or {@link java.lang.invoke.VarHandle})
     * obtained at compile time by the Mansart APT processor.  The runtime never
     * uses {@code java.lang.reflect.Field} on entity state.
     *
     * @param attributeName the persistent attribute name
     * @return the accessor, never {@code null}
     * @throws IllegalArgumentException if the attribute name is unknown
     */
    AttributeAccessor accessor(String attributeName);

    /**
     * Reads the value of the given persistent attribute from the entity instance.
     *
     * @param entity the entity instance
     * @param attributeName the persistent attribute name
     * @return the attribute value
     */
    Object readAttribute(Object entity, String attributeName);

    /**
     * Writes a value to the given persistent attribute of the entity instance.
     *
     * @param entity the entity instance
     * @param attributeName the persistent attribute name
     * @param value the new value
     */
    void writeAttribute(Object entity, String attributeName, Object value);

    /**
     * Creates a new, empty instance of this entity class.
     *
     * <p>Uses a pre-resolved no-arg constructor handle generated at compile time.
     *
     * @return a new entity instance
     */
    Object createInstance();

    // ----- APT-generated lifecycle callback dispatcher (DEBT-06) -----

    /**
     * Returns the lifecycle-callback dispatcher for this entity type.
     *
     * <p>The dispatcher invokes {@code @PrePersist}, {@code @PostLoad}, … methods
     * generated at compile time by the Mansart APT processor.  No runtime
     * reflection or {@code MethodHandles} scanning is performed.
     *
     * @return the callback dispatcher, never {@code null}
     */
    LifecycleCallbackDispatcher lifecycleCallbacks();
}

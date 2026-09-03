/*
 * SPDX-License-Identifier: EPL-2.0 OR EUPL-1.2 OR GPL-2.0-or-later
 */
package io.vidocq.mansart.persistence.spi;

import java.lang.invoke.MethodHandle;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Base interface for entity attributes.
 * Represents a field or property of an entity and its mapping to a database column.
 *
 * <p>Attributes form a hierarchy with specialized subtypes for different JPA types:
 * <ul>
 *   <li>{@link IdAttribute} - ID fields</li>
 *   <li>{@link VersionAttribute} - version fields for optimistic locking</li>
 *   <li>{@link BasicAttribute} - basic types (String, Integer, etc.)</li>
 *   <li>{@link NumericAttribute} - numeric types</li>
 *   <li>{@link TemporalAttribute} - date/time types</li>
 *   <li>{@link EnumAttribute} - enum types</li>
 *   <li>{@link ReferenceAttribute} - entity references</li>
 *   <li>{@link EmbeddedAttribute} - embedded objects</li>
 * </ul>
 *
 * <p>Access to entity field values is performed via {@link MethodHandle} obtained at
 * class initialization time - never via runtime reflection.
 *
 * @param <T> the entity type
 * @param <V> the attribute value type
 */
public interface Attribute<T, V> {

    /**
     * Returns the name of this attribute (field name).
     *
     * @return the attribute name
     */
    String getName();

    /**
     * Returns the column name for this attribute.
     *
     * @return the column name
     */
    String getColumnName();

    /**
     * Returns the entity model this attribute belongs to.
     *
     * @return the entity model
     */
    EntityModel<T> getEntityModel();

    /**
     * Returns the Java type of this attribute.
     *
     * @return the attribute type
     */
    Class<V> getJavaType();

    /**
     * Returns true if this attribute is nullable.
     *
     * @return true if nullable
     */
    boolean isNullable();

    /**
     * Returns true if this attribute is part of the primary key.
     *
     * @return true if id attribute
     */
    boolean isId();

    /**
     * Returns true if this attribute is the version field.
     *
     * @return true if version attribute
     */
    boolean isVersion();

    /**
     * Returns true if this attribute is unique.
     *
     * @return true if unique
     */
    boolean isUnique();

    /**
     * Returns true if this attribute represents a database column.
     * False for transient fields, embedded objects without @Column, etc.
     *
     * @return true if mapped to a column
     */
    boolean isColumn();

    /**
     * Returns the getter method handle for this attribute.
     * Returns the value from the entity instance.
     *
     * @return method handle for getting the value
     */
    MethodHandle getGetter();

    /**
     * Returns the setter method handle for this attribute.
     * Sets the value on the entity instance.
     *
     * @return method handle for setting the value
     */
    MethodHandle getSetter();

    /**
     * Gets the value of this attribute from the given entity instance.
     *
     * @param instance the entity instance
     * @return the attribute value
     */
    V get(T instance);

    /**
     * Sets the value of this attribute on the given entity instance.
     *
     * @param instance the entity instance
     * @param value the value to set
     */
    void set(T instance, V value);

    /**
     * Returns the length for this attribute (for String types).
     * Returns -1 if not applicable.
     *
     * @return the length, or -1
     */
    default int getLength() {
        return -1;
    }

    /**
     * Returns the precision for this attribute (for numeric types).
     * Returns -1 if not applicable.
     *
     * @return the precision, or -1
     */
    default int getPrecision() {
        return -1;
    }

    /**
     * Returns the scale for this attribute (for numeric types).
     * Returns -1 if not applicable.
     *
     * @return the scale, or -1
     */
    default int getScale() {
        return -1;
    }

    /**
     * Returns the column definition, if specified.
     *
     * @return the column definition, or empty string
     */
    default String getColumnDefinition() {
        return "";
    }

    /**
     * Returns true if this attribute should be inserted.
     *
     * @return true if insertable
     */
    default boolean isInsertable() {
        return true;
    }

    /**
     * Returns true if this attribute should be updated.
     *
     * @return true if updatable
     */
    default boolean isUpdatable() {
        return true;
    }
}

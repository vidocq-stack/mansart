/*
 * Copyright (c) 2026 Yann Blazart, Antoine Sabot-Durand and the Vidocq contributors
 *
 * SPDX-License-Identifier: EPL-2.0 OR EUPL-1.2 OR GPL-2.0-or-later
 */
package io.vidocq.mansart.persistence.spi;

/**
 * Metadata for a persistent attribute.
 *
 * <p>This interface provides access to attribute metadata such as:
 * <ul>
 *   <li>Attribute name</li>
 *   <li>Java type</li>
 *   <li>Fetch type (EAGER or LAZY)</li>
 *   <li>Nullable flag</li>
 *   <li>Column name</li>
 * </ul>
 */
public interface AttributeMetadata {

    /**
     * Returns the name of the attribute.
     *
     * @return the attribute name, never {@code null}
     */
    String name();

    /**
     * Returns the Java type of the attribute.
     *
     * @return the Java type class, never {@code null}
     */
    Class<?> javaType();

    /**
     * Returns whether this is an identifier attribute.
     *
     * @return {@code true} if this is an ID attribute, {@code false} otherwise
     */
    boolean isId();

    /**
     * Returns whether this is a version attribute.
     *
     * @return {@code true} if this is a version attribute, {@code false} otherwise
     */
    boolean isVersion();

    /**
     * Returns the fetch type for this attribute.
     *
     * @return the fetch type, never {@code null}
     */
    FetchType getFetchType();

    /**
     * Returns whether this attribute is nullable.
     *
     * @return {@code true} if nullable, {@code false} otherwise
     */
    boolean isNullable();

    /**
     * Returns the column name for this attribute.
     *
     * @return the column name, or {@code null} if using default
     */
    String getColumnName();

    /**
     * Fetch types for persistent attributes.
     */
    enum FetchType {
        LAZY,
        EAGER
    }
}

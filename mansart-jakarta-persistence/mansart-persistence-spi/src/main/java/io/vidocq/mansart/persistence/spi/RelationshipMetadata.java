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

/**
 * Metadata for a relationship attribute (ManyToOne, OneToOne, etc.).
 *
 * <p>This interface provides access to relationship mapping information such as:
 * <ul>
 *   <li>Target entity class</li>
 *   <li>Relationship type (MANY_TO_ONE, ONE_TO_ONE, etc.)</li>
 *   <li>Fetch type (EAGER or LAZY)</li>
 *   <li>Cascade types</li>
 *   <li>Optional flag</li>
 *   <li>Join column information</li>
 *   <li>Orphan removal flag</li>
 * </ul>
 */
public interface RelationshipMetadata {

    /**
     * Returns the type of relationship.
     *
     * @return the relationship type, never {@code null}
     */
    RelationshipType getRelationshipType();

    /**
     * Returns the target entity class.
     *
     * @return the target entity class, never {@code null}
     */
    Class<?> getTargetEntity();

    /**
     * Returns the name of the join column, or null if using default.
     *
     * @return the join column name, or {@code null} if default naming applies
     */
    String getJoinColumnName();

    /**
     * Returns whether the join column is nullable.
     *
     * @return {@code true} if the join column is nullable, {@code false} otherwise
     */
    boolean isJoinColumnNullable();

    /**
     * Returns whether the relationship is optional.
     *
     * @return {@code true} if the relationship is optional, {@code false} otherwise
     */
    boolean isOptional();

    /**
     * Returns the fetch type for this relationship.
     *
     * @return the fetch type, never {@code null}
     */
    FetchType getFetchType();

    /**
     * Returns the cascade types for this relationship.
     *
     * @return array of cascade types, never {@code null}, may be empty
     */
    CascadeType[] getCascadeTypes();

    /**
     * Returns whether orphan removal is enabled.
     *
     * @return {@code true} if orphan removal is enabled, {@code false} otherwise
     */
    boolean isOrphanRemoval();

    /**
     * Returns the name of the referenced column in the target entity.
     *
     * @return the referenced column name, or {@code null} if using primary key
     */
    String getReferencedColumnName();

    /**
     * Types of JPA relationships.
     */
    enum RelationshipType {
        MANY_TO_ONE,
        ONE_TO_ONE,
        ONE_TO_MANY,
        MANY_TO_MANY
    }

    /**
     * Fetch types for relationships.
     */
    enum FetchType {
        LAZY,
        EAGER
    }

    /**
     * Cascade types for relationships.
     */
    enum CascadeType {
        ALL,
        PERSIST,
        MERGE,
        REMOVE,
        REFRESH,
        DETACH
    }
}

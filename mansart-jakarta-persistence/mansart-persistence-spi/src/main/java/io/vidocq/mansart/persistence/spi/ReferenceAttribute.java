/*
 * SPDX-License-Identifier: EPL-2.0 OR EUPL-1.2 OR GPL-2.0-or-later
 */
package io.vidocq.mansart.persistence.spi;

/**
 * Attribute for entity references (relationships).
 *
 * @param <T> the entity type (source)
 * @param <V> the referenced entity type (target)
 */
public interface ReferenceAttribute<T, V> extends Attribute<T, V> {

    /**
     * Returns the referenced entity model.
     *
     * @return the referenced entity model
     */
    EntityModel<V> getReferencedEntityModel();

    /**
     * Returns the foreign key column name(s).
     *
     * @return array of foreign key column names
     */
    String[] getForeignKeyColumns();

    /**
     * Returns the join columns.
     *
     * @return array of join column definitions
     */
    default JoinColumn[] getJoinColumns() {
        return new JoinColumn[0];
    }

    /**
     * Returns the fetch type.
     *
     * @return the fetch type
     */
    FetchType getFetchType();

    /**
     * Returns true if this reference is optional (nullable).
     *
     * @return true if optional
     */
    default boolean isOptional() {
        return isNullable();
    }

    /**
     * Returns true if this reference is the owning side of the relationship.
     *
     * @return true if owning side
     */
    boolean isOwningSide();

    /**
     * Returns true if this reference is the inverse (mapped by) side.
     *
     * @return true if inverse side
     */
    boolean isInverseSide();

    /**
     * Returns the mapped by attribute name, if this is the inverse side.
     *
     * @return the mapped by attribute name, or empty string
     */
    default String getMappedBy() {
        return "";
    }

    /**
     * Relationship types.
     */
    enum RelationshipType {
        ONE_TO_ONE,
        ONE_TO_MANY,
        MANY_TO_ONE,
        MANY_TO_MANY
    }

    /**
     * Returns the relationship type.
     *
     * @return the relationship type
     */
    RelationshipType getRelationshipType();

    /**
     * Fetch types.
     */
    enum FetchType {
        /** Lazy loading */
        LAZY,
        /** Eager loading */
        EAGER
    }

    /**
     * Join column definition.
     */
    record JoinColumn(
            String name,
            String referencedColumnName,
            boolean nullable,
            boolean unique,
            boolean insertable,
            boolean updatable
    ) {}

    @Override
    default boolean isColumn() {
        return isOwningSide();
    }
}

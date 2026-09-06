/*
 * SPDX-License-Identifier: EPL-2.0 OR EUPL-1.2 OR GPL-2.0-or-later
 */
package io.vidocq.mansart.persistence.core.context;

import java.util.Objects;

/**
 * Composite key for entity identity map lookup.
 * Keys entities by their class and primary key value.
 *
 * @param entityClass the entity class
 * @param id the primary key value (may be null for NEW entities without assigned id)
 */
record EntityKey(Class<?> entityClass, Object id) {

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EntityKey that)) return false;
        return entityClass.equals(that.entityClass) && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(entityClass, id);
    }
}

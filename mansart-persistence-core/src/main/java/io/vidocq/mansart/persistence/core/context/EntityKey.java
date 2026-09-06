/*
 * SPDX-License-Identifier: EPL-2.0 OR EUPL-1.2 OR GPL-2.0-or-later
 */
package io.vidocq.mansart.persistence.core.context;

/**
 * Identity map key for entities: composed of the entity class and its primary key.
 * <p>
 * Uses identity comparison semantics for the entity class (reference equality),
 * and equals/hashCode for the primary key value.
 */
record EntityKey(Class<?> entityClass, Object id) {
    // Compact constructor for record
}

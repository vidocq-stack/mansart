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
package io.vidocq.mansart.persistence.core.jpql;

import java.util.Objects;

/**
 * Parsed representation of the JPQL {@code FROM <Entity> <identifier>} clause.
 *
 * <p>Milestone: M7-13 — supports a single entity identification variable.
 */
public final class JPQLFromClause {

    private final Class<?> entityType;
    private final String identificationVariable;

    /**
     * Creates a new {@code JPQLFromClause}.
     *
     * @param entityType                the entity class (e.g. {@code Person.class})
     * @param identificationVariable    the identification variable / alias (e.g. {@code "p"})
     */
    public JPQLFromClause(Class<?> entityType, String identificationVariable) {
        this.entityType = Objects.requireNonNull(entityType, "entityType must not be null");
        this.identificationVariable = Objects.requireNonNull(identificationVariable,
                "identificationVariable must not be null");
    }

    public Class<?> entityType() { return entityType; }
    public String identificationVariable() { return identificationVariable; }

    @Override
    public String toString() {
        return "JPQLFromClause{" +
                "entityType=" + entityType.getSimpleName() +
                ", identificationVariable='" + identificationVariable + '\'' +
                '}';
    }
}

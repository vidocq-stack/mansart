/*
 * Copyright (c) 2026 Yann Blazart, Antoine Sabot-Durand and the Vidocq contributors
 *
 * This program and the accompanying materials are made available under
 * the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0 OR EUPL-1.2 OR GPL-2.0-or-later
 */
package io.vidocq.mansart.persistence.core;

import java.util.List;

/**
 * Represents a parsed JPQL SELECT query.
 *
 * <p>Fields: entity name, select alias, from alias, distinct flag,
 * WHERE predicates, and ORDER BY items.</p>
 */
record JpqlQuery(
        String entityName,
        String selectAlias,
        String fromAlias,
        boolean distinct,
        List<JpqlPredicate> predicates,
        List<JpqlOrderBy> orderBys) {
}

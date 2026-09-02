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
 * Represents a single WHERE predicate: {@code fieldName op literalValue}.
 *
 * <p>Supported operators: {@code =}, {@code <>}.</p>
 *
 * <p>When {@code function} is non-null, the predicate wraps a scalar
 * function call (e.g. {@code UPPER(e.name)}) around the field comparison.
 * The dialect translates this to a {@code Where.Func} node.</p>
 */
record JpqlPredicate(String fieldName, String op, String value, String function,
                     List<String> arguments) {

    /**
     * Convenience constructor for predicates without a function wrapper.
     */
    JpqlPredicate(String fieldName, String op, String value) {
        this(fieldName, op, value, null, List.of());
    }

    /**
     * Convenience constructor for predicates with a (unary) function wrapper.
     */
    JpqlPredicate(String fieldName, String op, String value, String function) {
        this(fieldName, op, value, function, List.of());
    }
}

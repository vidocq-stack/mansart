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

/**
 * Represents a single WHERE predicate: {@code fieldName op literalValue}.
 *
 * <p>Supported operators: {@code =}, {@code <>}.</p>
 */
record JpqlPredicate(String fieldName, String op, String value) {
}

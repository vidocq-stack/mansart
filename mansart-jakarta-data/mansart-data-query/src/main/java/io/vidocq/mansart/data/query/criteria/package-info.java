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
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-1.2
 *
 * SPDX-License-Identifier: EPL-2.0 OR EUPL-1.2 OR GPL-2.0-or-later
 */

/**
 * Criteria API implementation.
 *
 * <p>This package provides the implementation of Jakarta Persistence Criteria API,
 * including {@link jakarta.persistence.criteria.CriteriaBuilder} and
 * {@link jakarta.persistence.criteria.CriteriaQuery}.</p>
 *
 * <h2>Classes:</h2>
 * <ul>
 *   <li>{@link io.vidocq.mansart.data.query.criteria.MansartCriteriaBuilder} - CriteriaBuilder implementation</li>
 *   <li>{@link io.vidocq.mansart.data.query.criteria.MansartCriteriaQuery} - CriteriaQuery implementation</li>
 *   <li>{@link io.vidocq.mansart.data.query.criteria.MansartRoot} - Root implementation</li>
 *   <li>{@link io.vidocq.mansart.data.query.criteria.MansartPath} - Path implementation</li>
 *   <li>{@link io.vidocq.mansart.data.query.criteria.MansartPredicate} - Predicate implementations</li>
 *   <li>{@link io.vidocq.mansart.data.query.criteria.MansartExpression} - Expression implementations</li>
 * </ul>
 *
 * <h2>Design Principles:</h2>
 * <ul>
 *   <li>Reuse existing Mansart Data types (EntityModel, Attribute, Where, etc.)</li>
 *   <li>Type-safe query construction</li>
 *   <li>Conversion to JPQL AST for execution</li>
 *   <li>Integration with Dialect SPI for SQL generation</li>
 * </ul>
 *
 * @since 0.3.0-SNAPSHOT
 */
package io.vidocq.mansart.data.query.criteria;

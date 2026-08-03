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
 * JPQL Abstract Syntax Tree (AST) classes.
 *
 * <p>This package contains the sealed hierarchy of AST nodes for representing JPQL queries.
 * The AST is designed to mirror the JPQL grammar and support all JPA 3.2 query features.</p>
 *
 * <h2>Node Types:</h2>
 * <ul>
 *   <li>{@link io.vidocq.mansart.data.query.ast.JpqlNode} - Base interface for all AST nodes</li>
 *   <li>{@link io.vidocq.mansart.data.query.ast.JpqlStmt} - Base for all JPQL statements</li>
 *   <li>{@link io.vidocq.mansart.data.query.ast.JpqlSelectStmt} - SELECT statement</li>
 *   <li>{@link io.vidocq.mansart.data.query.ast.JpqlExpr} - Base for all expressions</li>
 *   <li>{@link io.vidocq.mansart.data.query.ast.JpqlPredicate} - Base for all predicates</li>
 * </ul>
 *
 * <h2>Design Principles:</h2>
 * <ul>
 *   <li>Sealed interfaces for type safety and exhaustiveness</li>
 *   <li>Immutable records for data classes</li>
 *   <li>Visitor pattern for SQL generation and transformation</li>
 *   <li>Inspired by existing {@link io.vidocq.mansart.data.core.JdqlAst} from Mansart Data</li>
 * </ul>
 *
 * @since 0.3.0-SNAPSHOT
 */
package io.vidocq.mansart.data.query.ast;

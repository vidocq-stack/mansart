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

package io.vidocq.mansart.data.query.ast;

/**
 * Base interface for all JPQL AST (Abstract Syntax Tree) nodes.
 *
 * <p>This sealed interface serves as the root of the JPQL AST hierarchy.
 * All JPQL constructs (statements, expressions, predicates, etc.) implement this interface.</p>
 *
 * <p>The hierarchy is organized as follows:</p>
 * <pre>
 * JpqlNode
 * ├── JpqlStmt      (statements: SELECT, UPDATE, DELETE)
 * ├── JpqlExpr      (expressions: path, literal, function, binary, unary, case)
 * └── JpqlPredicate  (predicates: comparison, like, in, between, and, or, not)
 * </pre>
 *
 * <p>This design is inspired by the existing {@link io.vidocq.mansart.data.core.JdqlAst} 
 * from Mansart Data, adapted for JPQL semantics.</p>
 *
 * @since 0.3.0-SNAPSHOT
 */
public sealed interface JpqlNode permits JpqlStmt, JpqlExpr, JpqlClause, JpqlPredicate {

    /**
     * Accepts a visitor for this node.
     *
     * <p>This method enables the visitor pattern for traversing and transforming the AST.</p>
     *
     * @param <R> the return type of the visitor
     * @param <P> the parameter type of the visitor
     * @param visitor the visitor to apply to this node
     * @param parameter a parameter to pass to the visitor
     * @return the result of visiting this node
     */
    <R, P> R accept(JpqlVisitor<R, P> visitor, P parameter);
}

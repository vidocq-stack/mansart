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
 * Visitor interface for JPQL AST nodes.
 *
 * <p>This interface defines the visitor pattern for traversing and transforming JPQL AST nodes.
 * Each visit method corresponds to a specific node type in the AST hierarchy.</p>
 *
 * <p>Typical use cases:</p>
 * <ul>
 *   <li>SQL generation (JpqlToSqlVisitor)</li>
 *   <li>Validation (JpqlValidationVisitor)</li>
 *   <li>Query optimization (JpqlOptimizationVisitor)</li>
 *   <li>Pretty printing (JpqlPrettyPrintVisitor)</li>
 * </ul>
 *
 * <p>This pattern is inspired by the existing {@link io.vidocq.mansart.data.core.JdqlAst.Visitor} 
 * from Mansart Data.</p>
 *
 * @param <R> the return type of the visitor methods
 * @param <P> the parameter type passed to the visitor methods
 * @since 0.3.0-SNAPSHOT
 */
public interface JpqlVisitor<R, P> {

    // ========== Statements ==========

    /**
     * Visits a SELECT statement.
     *
     * @param stmt the SELECT statement
     * @param parameter the parameter
     * @return the result
     */
    R visitSelect(JpqlSelectStmt stmt, P parameter);

    /**
     * Visits an UPDATE statement.
     *
     * @param stmt the UPDATE statement
     * @param parameter the parameter
     * @return the result
     */
    R visitUpdate(JpqlUpdateStmt stmt, P parameter);

    /**
     * Visits a DELETE statement.
     *
     * @param stmt the DELETE statement
     * @param parameter the parameter
     * @return the result
     */
    R visitDelete(JpqlDeleteStmt stmt, P parameter);

    // ========== Clauses ==========

    /**
     * Visits a SELECT clause.
     *
     * @param clause the SELECT clause
     * @param parameter the parameter
     * @return the result
     */
    R visitSelectClause(JpqlSelectClause clause, P parameter);

    /**
     * Visits a FROM clause.
     *
     * @param clause the FROM clause
     * @param parameter the parameter
     * @return the result
     */
    R visitFromClause(JpqlFromClause clause, P parameter);

    /**
     * Visits a WHERE clause.
     *
     * @param clause the WHERE clause
     * @param parameter the parameter
     * @return the result
     */
    R visitWhereClause(JpqlWhereClause clause, P parameter);

    /**
     * Visits a GROUP BY clause.
     *
     * @param clause the GROUP BY clause
     * @param parameter the parameter
     * @return the result
     */
    R visitGroupByClause(JpqlGroupByClause clause, P parameter);

    /**
     * Visits a HAVING clause.
     *
     * @param clause the HAVING clause
     * @param parameter the parameter
     * @return the result
     */
    R visitHavingClause(JpqlHavingClause clause, P parameter);

    /**
     * Visits an ORDER BY clause.
     *
     * @param clause the ORDER BY clause
     * @param parameter the parameter
     * @return the result
     */
    R visitOrderByClause(JpqlOrderByClause clause, P parameter);

    // ========== Expressions ==========

    /**
     * Visits a path expression.
     *
     * @param expr the path expression
     * @param parameter the parameter
     * @return the result
     */
    R visitPathExpr(JpqlPathExpr expr, P parameter);

    // Literals
    R visitStringLiteral(JpqlStringLiteral literal, P parameter);
    R visitNumericLiteral(JpqlNumericLiteral literal, P parameter);
    R visitBooleanLiteral(JpqlBooleanLiteral literal, P parameter);
    R visitNullLiteral(JpqlNullLiteral literal, P parameter);
    R visitTemporalLiteral(JpqlTemporalLiteral literal, P parameter);

    /**
     * Visits a function expression.
     *
     * @param expr the function expression
     * @param parameter the parameter
     * @return the result
     */
    R visitFunctionExpr(JpqlFunctionExpr expr, P parameter);

    /**
     * Visits a binary expression.
     *
     * @param expr the binary expression
     * @param parameter the parameter
     * @return the result
     */
    R visitBinaryExpr(JpqlBinaryExpr expr, P parameter);

    /**
     * Visits a unary expression.
     *
     * @param expr the unary expression
     * @param parameter the parameter
     * @return the result
     */
    R visitUnaryExpr(JpqlUnaryExpr expr, P parameter);

    /**
     * Visits a CASE expression.
     *
     * @param expr the CASE expression
     * @param parameter the parameter
     * @return the result
     */
    R visitCaseExpr(JpqlCaseExpr expr, P parameter);

    /**
     * Visits a TYPE expression.
     *
     * @param expr the TYPE expression
     * @param parameter the parameter
     * @return the result
     */
    R visitTypeExpr(JpqlTypeExpr expr, P parameter);

    /**
     * Visits a parameter expression.
     *
     * @param expr the parameter expression
     * @param parameter the parameter
     * @return the result
     */
    R visitParameterExpr(JpqlParameterExpr expr, P parameter);

    // ========== Predicates ==========

    /**
     * Visits a comparison predicate.
     *
     * @param predicate the comparison predicate
     * @param parameter the parameter
     * @return the result
     */
    R visitComparisonPredicate(JpqlComparisonPredicate predicate, P parameter);

    /**
     * Visits a LIKE predicate.
     *
     * @param predicate the LIKE predicate
     * @param parameter the parameter
     * @return the result
     */
    R visitLikePredicate(JpqlLikePredicate predicate, P parameter);

    /**
     * Visits an IN predicate.
     *
     * @param predicate the IN predicate
     * @param parameter the parameter
     * @return the result
     */
    R visitInPredicate(JpqlInPredicate predicate, P parameter);

    /**
     * Visits a BETWEEN predicate.
     *
     * @param predicate the BETWEEN predicate
     * @param parameter the parameter
     * @return the result
     */
    R visitBetweenPredicate(JpqlBetweenPredicate predicate, P parameter);

    /**
     * Visits an AND predicate.
     *
     * @param predicate the AND predicate
     * @param parameter the parameter
     * @return the result
     */
    R visitAndPredicate(JpqlAndPredicate predicate, P parameter);

    /**
     * Visits an OR predicate.
     *
     * @param predicate the OR predicate
     * @param parameter the parameter
     * @return the result
     */
    R visitOrPredicate(JpqlOrPredicate predicate, P parameter);

    /**
     * Visits a NOT predicate.
     *
     * @param predicate the NOT predicate
     * @param parameter the parameter
     * @return the result
     */
    R visitNotPredicate(JpqlNotPredicate predicate, P parameter);

    /**
     * Visits an EXISTS predicate.
     *
     * @param predicate the EXISTS predicate
     * @param parameter the parameter
     * @return the result
     */
    R visitExistsPredicate(JpqlExistsPredicate predicate, P parameter);

    /**
     * Visits an ALL/ANY/SOME predicate.
     *
     * @param predicate the ALL/ANY/SOME predicate
     * @param parameter the parameter
     * @return the result
     */
    R visitAllAnySomePredicate(JpqlAllAnySomePredicate predicate, P parameter);
}

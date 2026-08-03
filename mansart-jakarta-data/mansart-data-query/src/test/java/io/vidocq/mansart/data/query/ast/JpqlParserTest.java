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

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for {@link JpqlParser}.
 */
class JpqlParserTest {

    @Test
    void testSimpleSelect() {
        JpqlSelectStmt stmt = (JpqlSelectStmt) JpqlParser.parse("SELECT b FROM Book b");
        
        assertThat(stmt.isDistinct()).isFalse();
        assertThat(stmt.selectExpressions()).hasSize(1);
        assertThat(stmt.selectExpressions().get(0)).isInstanceOf(JpqlPathExpr.class);
        assertThat(((JpqlPathExpr) stmt.selectExpressions().get(0)).path()).isEqualTo("b");
        assertThat(stmt.fromItems()).hasSize(1);
        assertThat(stmt.fromItems().get(0).entityName()).isEqualTo("Book");
        assertThat(stmt.fromItems().get(0).identifier()).isEqualTo("b");
        assertThat(stmt.hasWhere()).isFalse();
    }

    @Test
    void testSelectDistinct() {
        JpqlSelectStmt stmt = (JpqlSelectStmt) JpqlParser.parse("SELECT DISTINCT b FROM Book b");
        
        assertThat(stmt.isDistinct()).isTrue();
    }

    @Test
    void testSelectMultipleProjections() {
        JpqlSelectStmt stmt = (JpqlSelectStmt) JpqlParser.parse("SELECT b.title, b.author.name FROM Book b");
        
        assertThat(stmt.selectExpressions()).hasSize(2);
    }

    @Test
    void testSelectAsterisk() {
        JpqlSelectStmt stmt = (JpqlSelectStmt) JpqlParser.parse("SELECT * FROM Book b");
        
        assertThat(stmt.selectExpressions()).hasSize(1);
        assertThat(((JpqlPathExpr) stmt.selectExpressions().get(0)).path()).isEqualTo("*");
    }

    @Test
    void testWhereClause() {
        JpqlSelectStmt stmt = (JpqlSelectStmt) JpqlParser.parse("SELECT b FROM Book b WHERE b.price > 100");
        
        assertThat(stmt.hasWhere()).isTrue();
        assertThat(stmt.whereClause().get().predicate()).isInstanceOf(JpqlComparisonPredicate.class);
    }

    @Test
    void testWhereClauseWithAnd() {
        JpqlSelectStmt stmt = (JpqlSelectStmt) JpqlParser.parse(
            "SELECT b FROM Book b WHERE b.price > 100 AND b.author.name LIKE 'J%'"
        );
        
        assertThat(stmt.hasWhere()).isTrue();
        assertThat(stmt.whereClause().get().predicate()).isInstanceOf(JpqlAndPredicate.class);
    }

    @Test
    void testWhereClauseWithOr() {
        JpqlSelectStmt stmt = (JpqlSelectStmt) JpqlParser.parse(
            "SELECT b FROM Book b WHERE b.category = 'Fiction' OR b.category = 'Sci-Fi'"
        );
        
        assertThat(stmt.hasWhere()).isTrue();
        assertThat(stmt.whereClause().get().predicate()).isInstanceOf(JpqlOrPredicate.class);
    }

    @Test
    void testWhereClauseWithNot() {
        JpqlSelectStmt stmt = (JpqlSelectStmt) JpqlParser.parse(
            "SELECT b FROM Book b WHERE NOT b.active = true"
        );
        
        assertThat(stmt.hasWhere()).isTrue();
        assertThat(stmt.whereClause().get().predicate()).isInstanceOf(JpqlNotPredicate.class);
    }

    @Test
    void testWhereClauseWithIsNull() {
        JpqlSelectStmt stmt = (JpqlSelectStmt) JpqlParser.parse("SELECT b FROM Book b WHERE b.author IS NULL");
        
        assertThat(stmt.hasWhere()).isTrue();
        JpqlComparisonPredicate pred = (JpqlComparisonPredicate) stmt.whereClause().get().predicate();
        assertThat(pred.isNull()).isTrue();
    }

    @Test
    void testWhereClauseWithIsNotNull() {
        JpqlSelectStmt stmt = (JpqlSelectStmt) JpqlParser.parse("SELECT b FROM Book b WHERE b.author IS NOT NULL");
        
        assertThat(stmt.hasWhere()).isTrue();
        JpqlComparisonPredicate pred = (JpqlComparisonPredicate) stmt.whereClause().get().predicate();
        assertThat(pred.isNotNull()).isTrue();
    }

    @Test
    void testWhereClauseWithLike() {
        JpqlSelectStmt stmt = (JpqlSelectStmt) JpqlParser.parse("SELECT b FROM Book b WHERE b.title LIKE 'J%'");
        
        assertThat(stmt.hasWhere()).isTrue();
        assertThat(stmt.whereClause().get().predicate()).isInstanceOf(JpqlLikePredicate.class);
    }

    @Test
    void testWhereClauseWithIn() {
        JpqlSelectStmt stmt = (JpqlSelectStmt) JpqlParser.parse(
            "SELECT b FROM Book b WHERE b.category IN ('Fiction', 'Sci-Fi')"
        );
        
        assertThat(stmt.hasWhere()).isTrue();
        assertThat(stmt.whereClause().get().predicate()).isInstanceOf(JpqlInPredicate.class);
    }

    @Test
    void testWhereClauseWithBetween() {
        JpqlSelectStmt stmt = (JpqlSelectStmt) JpqlParser.parse(
            "SELECT b FROM Book b WHERE b.price BETWEEN 10 AND 100"
        );
        
        assertThat(stmt.hasWhere()).isTrue();
        assertThat(stmt.whereClause().get().predicate()).isInstanceOf(JpqlBetweenPredicate.class);
    }

    @Test
    void testOrderByClause() {
        JpqlSelectStmt stmt = (JpqlSelectStmt) JpqlParser.parse("SELECT b FROM Book b ORDER BY b.title ASC");
        
        assertThat(stmt.hasOrderBy()).isTrue();
        assertThat(stmt.orderByClause().get().items()).hasSize(1);
        assertThat(stmt.orderByClause().get().items().get(0).direction()).isEqualTo("ASC");
    }

    @Test
    void testOrderByClauseMultiple() {
        JpqlSelectStmt stmt = (JpqlSelectStmt) JpqlParser.parse(
            "SELECT b FROM Book b ORDER BY b.title ASC, b.price DESC"
        );
        
        assertThat(stmt.hasOrderBy()).isTrue();
        assertThat(stmt.orderByClause().get().items()).hasSize(2);
    }

    @Test
    void testGroupByClause() {
        JpqlSelectStmt stmt = (JpqlSelectStmt) JpqlParser.parse("SELECT b.author, COUNT(b) FROM Book b GROUP BY b.author");
        
        assertThat(stmt.hasGroupBy()).isTrue();
        assertThat(stmt.groupByClause().get().expressions()).hasSize(1);
    }

    @Test
    void testHavingClause() {
        JpqlSelectStmt stmt = (JpqlSelectStmt) JpqlParser.parse(
            "SELECT b.author, COUNT(b) FROM Book b GROUP BY b.author HAVING COUNT(b) > 10"
        );
        
        assertThat(stmt.hasHaving()).isTrue();
    }

    @Test
    void testJoin() {
        JpqlSelectStmt stmt = (JpqlSelectStmt) JpqlParser.parse(
            "SELECT b FROM Book b JOIN b.author a"
        );
        
        assertThat(stmt.fromItems()).hasSize(2);
        assertThat(stmt.fromItems().get(0).entityName()).isEqualTo("Book");
        assertThat(stmt.fromItems().get(1).entityName()).isEqualTo("Author");
    }

    @Test
    void testLeftJoin() {
        JpqlSelectStmt stmt = (JpqlSelectStmt) JpqlParser.parse(
            "SELECT b FROM Book b LEFT JOIN b.author a"
        );
        
        assertThat(stmt.fromItems()).hasSize(2);
    }

    @Test
    void testInnerJoin() {
        JpqlSelectStmt stmt = (JpqlSelectStmt) JpqlParser.parse(
            "SELECT b FROM Book b INNER JOIN b.author a"
        );
        
        assertThat(stmt.fromItems()).hasSize(2);
    }

    @Test
    void testPathExpression() {
        JpqlSelectStmt stmt = (JpqlSelectStmt) JpqlParser.parse(
            "SELECT b.author.name FROM Book b"
        );
        
        JpqlPathExpr path = (JpqlPathExpr) stmt.selectExpressions().get(0);
        assertThat(path.path()).isEqualTo("b.author.name");
        assertThat(path.parts()).containsExactly("b", "author", "name");
    }

    @Test
    void testFunctionExpression() {
        JpqlSelectStmt stmt = (JpqlSelectStmt) JpqlParser.parse(
            "SELECT COUNT(b), UPPER(b.title) FROM Book b"
        );
        
        assertThat(stmt.selectExpressions()).hasSize(2);
        assertThat(stmt.selectExpressions().get(0)).isInstanceOf(JpqlFunctionExpr.class);
        JpqlFunctionExpr func = (JpqlFunctionExpr) stmt.selectExpressions().get(0);
        assertThat(func.functionName()).isEqualTo("COUNT");
    }

    @Test
    void testStringLiteral() {
        JpqlSelectStmt stmt = (JpqlSelectStmt) JpqlParser.parse(
            "SELECT b FROM Book b WHERE b.title = 'John'"
        );
        
        JpqlComparisonPredicate pred = (JpqlComparisonPredicate) stmt.whereClause().get().predicate();
        assertThat(pred.right()).isInstanceOf(JpqlStringLiteral.class);
    }

    @Test
    void testNumericLiteral() {
        JpqlSelectStmt stmt = (JpqlSelectStmt) JpqlParser.parse(
            "SELECT b FROM Book b WHERE b.price > 100"
        );
        
        JpqlComparisonPredicate pred = (JpqlComparisonPredicate) stmt.whereClause().get().predicate();
        assertThat(pred.right()).isInstanceOf(JpqlNumericLiteral.class);
    }

    @Test
    void testBooleanLiteral() {
        JpqlSelectStmt stmt = (JpqlSelectStmt) JpqlParser.parse(
            "SELECT b FROM Book b WHERE b.active = true"
        );
        
        JpqlComparisonPredicate pred = (JpqlComparisonPredicate) stmt.whereClause().get().predicate();
        assertThat(pred.right()).isInstanceOf(JpqlBooleanLiteral.class);
    }

    @Test
    void testNullLiteral() {
        JpqlSelectStmt stmt = (JpqlSelectStmt) JpqlParser.parse(
            "SELECT b FROM Book b WHERE b.value IS NULL"
        );
        
        assertThat(stmt.hasWhere()).isTrue();
    }

    @Test
    void testNamedParameter() {
        JpqlSelectStmt stmt = (JpqlSelectStmt) JpqlParser.parse(
            "SELECT b FROM Book b WHERE b.price > :minPrice"
        );
        
        JpqlComparisonPredicate pred = (JpqlComparisonPredicate) stmt.whereClause().get().predicate();
        assertThat(pred.right()).isInstanceOf(JpqlParameterExpr.class);
        JpqlParameterExpr param = (JpqlParameterExpr) pred.right();
        assertThat(param.isNamed()).isTrue();
        assertThat(param.getName()).isEqualTo("minPrice");
    }

    @Test
    void testPositionalParameter() {
        JpqlSelectStmt stmt = (JpqlSelectStmt) JpqlParser.parse(
            "SELECT b FROM Book b WHERE b.price > ?1"
        );
        
        JpqlComparisonPredicate pred = (JpqlComparisonPredicate) stmt.whereClause().get().predicate();
        assertThat(pred.right()).isInstanceOf(JpqlParameterExpr.class);
        JpqlParameterExpr param = (JpqlParameterExpr) pred.right();
        assertThat(param.isPositional()).isTrue();
        assertThat(param.getPosition()).isEqualTo(1);
    }

    @Test
    void testArithmeticExpression() {
        JpqlSelectStmt stmt = (JpqlSelectStmt) JpqlParser.parse(
            "SELECT b.price * 1.1 FROM Book b"
        );
        
        JpqlBinaryExpr expr = (JpqlBinaryExpr) stmt.selectExpressions().get(0);
        assertThat(expr.operator()).isEqualTo(JpqlBinaryExpr.BinaryOperator.MULTIPLY);
    }

    @Test
    void testUnaryMinus() {
        JpqlSelectStmt stmt = (JpqlSelectStmt) JpqlParser.parse(
            "SELECT -b.price FROM Book b"
        );
        
        JpqlUnaryExpr expr = (JpqlUnaryExpr) stmt.selectExpressions().get(0);
        assertThat(expr.isNegation()).isTrue();
    }

    @Test
    void testComparisonOperators() {
        String[] queries = {
            "SELECT b FROM Book b WHERE b.price = 100",
            "SELECT b FROM Book b WHERE b.price <> 100",
            "SELECT b FROM Book b WHERE b.price < 100",
            "SELECT b FROM Book b WHERE b.price <= 100",
            "SELECT b FROM Book b WHERE b.price > 100",
            "SELECT b FROM Book b WHERE b.price >= 100"
        };
        
        for (String query : queries) {
            JpqlSelectStmt stmt = (JpqlSelectStmt) JpqlParser.parse(query);
            assertThat(stmt.hasWhere()).isTrue();
            assertThat(stmt.whereClause().get().predicate()).isInstanceOf(JpqlComparisonPredicate.class);
        }
    }

    @Test
    void testComplexQuery() {
        String query = "SELECT DISTINCT b.title, b.author.name FROM Book b " +
                       "JOIN b.author a LEFT JOIN a.address addr " +
                       "WHERE b.price > 100 AND (b.category = 'Fiction' OR b.category = 'Sci-Fi') " +
                       "GROUP BY b.author " +
                       "HAVING COUNT(b) > 5 " +
                       "ORDER BY b.title ASC, b.price DESC";
        
        JpqlSelectStmt stmt = (JpqlSelectStmt) JpqlParser.parse(query);
        
        assertThat(stmt.isDistinct()).isTrue();
        assertThat(stmt.selectExpressions()).hasSize(2);
        assertThat(stmt.fromItems()).hasSize(3);
        assertThat(stmt.hasWhere()).isTrue();
        assertThat(stmt.hasGroupBy()).isTrue();
        assertThat(stmt.hasHaving()).isTrue();
        assertThat(stmt.hasOrderBy()).isTrue();
    }

    @Test
    void testUpdateStatement() {
        JpqlUpdateStmt stmt = (JpqlUpdateStmt) JpqlParser.parse(
            "UPDATE Book b SET b.price = b.price * 1.1 WHERE b.category = 'Fiction'"
        );
        
        assertThat(stmt.entityName()).isEqualTo("Book");
        assertThat(stmt.identifier()).isEqualTo("b");
        assertThat(stmt.setClauses()).hasSize(1);
        assertThat(stmt.hasWhere()).isTrue();
    }

    @Test
    void testDeleteStatement() {
        JpqlDeleteStmt stmt = (JpqlDeleteStmt) JpqlParser.parse(
            "DELETE FROM Book b WHERE b.price < 10"
        );
        
        assertThat(stmt.entityName()).isEqualTo("Book");
        assertThat(stmt.identifier()).isEqualTo("b");
        assertThat(stmt.hasWhere()).isTrue();
    }

    @Test
    void testExistsSubquery() {
        JpqlSelectStmt stmt = (JpqlSelectStmt) JpqlParser.parse(
            "SELECT b FROM Book b WHERE EXISTS (SELECT 1 FROM Order o WHERE o.book = b)"
        );
        
        assertThat(stmt.hasWhere()).isTrue();
        JpqlPredicate pred = stmt.whereClause().get().predicate();
        assertThat(pred).isInstanceOf(JpqlExistsPredicate.class);
        JpqlExistsPredicate exists = (JpqlExistsPredicate) pred;
        assertThat(exists.isExists()).isTrue();
        assertThat(exists.subquery()).isInstanceOf(JpqlSelectStmt.class);
    }

    @Test
    void testNotExistsSubquery() {
        JpqlSelectStmt stmt = (JpqlSelectStmt) JpqlParser.parse(
            "SELECT b FROM Book b WHERE NOT EXISTS (SELECT 1 FROM Order o WHERE o.book = b)"
        );
        
        assertThat(stmt.hasWhere()).isTrue();
        JpqlPredicate pred = stmt.whereClause().get().predicate();
        assertThat(pred).isInstanceOf(JpqlNotPredicate.class);
        JpqlNotPredicate not = (JpqlNotPredicate) pred;
        assertThat(not.operand()).isInstanceOf(JpqlExistsPredicate.class);
    }
}

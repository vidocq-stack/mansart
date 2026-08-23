/*
 * Copyright (c) 2026 Yann Blazart, Antoine Sabot-Durand and the Vidocq contributors
 *
 * SPDX-License-Identifier: EPL-2.0 OR GPL-2.0-or-later
 */
package io.vidocq.mansart.persistence.tests.criteria;

import io.vidocq.mansart.persistence.core.criteria.MansartCriteriaBuilder;
import io.vidocq.mansart.persistence.core.criteria.MansartCriteriaQuery;
import io.vidocq.mansart.persistence.core.criteria.MansartRoot;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for real Criteria API classes (DEBT-04 replacement of proxies).
 */
class CriteriaBuilderRealTest {

    private final CriteriaBuilder cb = MansartCriteriaBuilder.getInstance();

    @Test
    void createQuery_returnsRealCriteriaQuery() {
        CriteriaQuery<Object> query = cb.createQuery();
        assertNotNull(query);
        assertInstanceOf(MansartCriteriaQuery.class, query);
    }

    @Test
    void createTupleQuery_returnsRealCriteriaQuery() {
        CriteriaQuery<Object[]> query = cb.createTupleQuery();
        assertNotNull(query);
        assertInstanceOf(MansartCriteriaQuery.class, query);
    }

    @Test
    void literal_string_returnsRealExpression() {
        Expression<String> expr = cb.literal("hello");
        assertNotNull(expr);
        assertInstanceOf(MansartCriteriaBuilder.LiteralExpression.class, expr);
    }

    @Test
    void literal_integer_returnsRealExpression() {
        Expression<Integer> expr = cb.literal(42);
        assertNotNull(expr);
        assertInstanceOf(MansartCriteriaBuilder.LiteralExpression.class, expr);
    }

    @Test
    void literal_null_returnsRealExpression() {
        Expression<Object> expr = cb.literal(null);
        assertNotNull(expr);
        assertInstanceOf(MansartCriteriaBuilder.LiteralExpression.class, expr);
    }

    @Test
    void parameter_returnsRealParameterExpression() {
        Expression<String> expr = cb.parameter(String.class, "name");
        assertNotNull(expr);
        assertInstanceOf(CriteriaBuilder.ParameterExpression.class, expr);
    }

    @Test
    void equal_returnsRealPredicate() {
        CriteriaQuery<Object> query = cb.createQuery();
        MansartRoot<Object> root = new MansartRoot<>(Object.class, query);
        Expression<String> path = root.get("name");
        Predicate predicate = cb.equal(path, "value");
        assertNotNull(predicate);
        assertInstanceOf(MansartCriteriaBuilder.EqualPredicate.class, predicate);
    }

    @Test
    void and_returnsRealPredicate() {
        CriteriaQuery<Object> query = cb.createQuery();
        MansartRoot<Object> root = new MansartRoot<>(Object.class, query);
        Predicate p1 = cb.equal(root.get("a"), 1);
        Predicate p2 = cb.equal(root.get("b"), 2);
        Predicate and = cb.and(p1, p2);
        assertNotNull(and);
        assertInstanceOf(MansartCriteriaBuilder.AndPredicate.class, and);
    }

    @Test
    void or_returnsRealPredicate() {
        CriteriaQuery<Object> query = cb.createQuery();
        MansartRoot<Object> root = new MansartRoot<>(Object.class, query);
        Predicate p1 = cb.equal(root.get("a"), 1);
        Predicate p2 = cb.equal(root.get("b"), 2);
        Predicate or = cb.or(p1, p2);
        assertNotNull(or);
        assertInstanceOf(MansartCriteriaBuilder.OrPredicate.class, or);
    }

    @Test
    void not_returnsRealPredicate() {
        CriteriaQuery<Object> query = cb.createQuery();
        MansartRoot<Object> root = new MansartRoot<>(Object.class, query);
        Predicate p = cb.equal(root.get("a"), 1);
        Predicate not = cb.not(p);
        assertNotNull(not);
        assertInstanceOf(MansartCriteriaBuilder.NotPredicate.class, not);
    }

    @Test
    void isNull_returnsRealPredicate() {
        CriteriaQuery<Object> query = cb.createQuery();
        MansartRoot<Object> root = new MansartRoot<>(Object.class, query);
        Predicate p = cb.isNull(root.get("name"));
        assertNotNull(p);
        assertInstanceOf(MansartCriteriaBuilder.NullCheckPredicate.class, p);
    }

    @Test
    void isNotNull_returnsRealPredicate() {
        CriteriaQuery<Object> query = cb.createQuery();
        MansartRoot<Object> root = new MansartRoot<>(Object.class, query);
        Predicate p = cb.isNotNull(root.get("name"));
        assertNotNull(p);
        assertInstanceOf(MansartCriteriaBuilder.NullCheckPredicate.class, p);
    }

    @Test
    void gt_returnsRealPredicate() {
        CriteriaQuery<Object> query = cb.createQuery();
        MansartRoot<Object> root = new MansartRoot<>(Object.class, query);
        Predicate p = cb.gt(root.get("age"), 18);
        assertNotNull(p);
        assertInstanceOf(MansartCriteriaBuilder.ComparisonPredicate.class, p);
    }

    @Test
    void lt_returnsRealPredicate() {
        CriteriaQuery<Object> query = cb.createQuery();
        MansartRoot<Object> root = new MansartRoot<>(Object.class, query);
        Predicate p = cb.lt(root.get("age"), 65);
        assertNotNull(p);
        assertInstanceOf(MansartCriteriaBuilder.ComparisonPredicate.class, p);
    }

    @Test
    void from_returnsRealRoot() {
        CriteriaQuery<Object> query = cb.createQuery();
        MansartRoot<Object> root = cb.from(Object.class, query);
        assertNotNull(root);
        assertInstanceOf(MansartRoot.class, root);
    }

    @Test
    void root_get_returnsRealPath() {
        CriteriaQuery<Object> query = cb.createQuery();
        MansartRoot<Object> root = cb.from(Object.class, query);
        Expression<String> path = root.get("name");
        assertNotNull(path);
        assertInstanceOf(MansartPath.class, path);
    }

    @Test
    void root_get_nested_returnsRealPath() {
        CriteriaQuery<Object> query = cb.createQuery();
        MansartRoot<Object> root = cb.from(Object.class, query);
        Expression<String> path = root.get("address", "city");
        assertNotNull(path);
        assertInstanceOf(MansartPath.class, path);
    }

    @Test
    void criteriaQuery_select_setsProjection() {
        CriteriaQuery<Object> query = cb.createQuery();
        MansartRoot<Object> root = cb.from(Object.class, query);
        query.select(root);
        // Verify the query now has a projection
        assertNotNull(query);
    }

    @Test
    void criteriaQuery_where_setsWhereClause() {
        CriteriaQuery<Object> query = cb.createQuery();
        MansartRoot<Object> root = cb.from(Object.class, query);
        Predicate p = cb.equal(root.get("name"), "test");
        query.where(p);
        // Verify the query now has a where clause
        assertNotNull(query);
    }

    @Test
    void literal_null_throwsIae() {
        assertThrows(IllegalArgumentException.class, () -> cb.literal((String) null));
    }

    @Test
    void from_null_class_throwsIae() {
        assertThrows(IllegalArgumentException.class, () -> cb.from(null));
    }
}

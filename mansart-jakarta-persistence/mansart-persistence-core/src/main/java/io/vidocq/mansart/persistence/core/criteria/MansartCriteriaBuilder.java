/*
 * Copyright (c) 2026 Yann Blazart, Antoine Sabot-Durand and the Vidocq contributors
 *
 * SPDX-License-Identifier: EPL-2.0 OR GPL-2.0-or-later
 */
package io.vidocq.mansart.persistence.core.criteria;

import jakarta.persistence.criteria.*;
import jakarta.persistence.metamodel.Bindable;
import jakarta.persistence.metamodel.Metamodel;
import jakarta.persistence.metamodel.ManagedType;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalUnit;
import java.util.*;

/**
 * Mansart implementation of CriteriaBuilder for JPA 3.2.
 */
public class MansartCriteriaBuilder implements CriteriaBuilder {

    private static final CriteriaBuilder INSTANCE = new MansartCriteriaBuilder(null);

    private final Metamodel metamodel;

    protected MansartCriteriaBuilder(Metamodel metamodel) {
        this.metamodel = metamodel;
    }

    public static CriteriaBuilder getInstance() {
        return INSTANCE;
    }

    public Metamodel getMetamodel() {
        return metamodel;
    }

    // === Query Creation ===

    @Override public CriteriaQuery<Object> createQuery() {
        return new MansartCriteriaQuery<>(this);
    }

    @Override public <T> CriteriaQuery<T> createQuery(Class<T> resultClass) {
        return new MansartCriteriaQuery<>(this, resultClass);
    }

    @Override public CriteriaQuery<Tuple> createTupleQuery() {
        return new MansartCriteriaQuery<>(this);
    }

    @Override public <T> CriteriaUpdate<T> createCriteriaUpdate(Class<T> cls) {
        return new MansartCriteriaUpdate<>(this, cls);
    }

    @Override public <T> CriteriaDelete<T> createCriteriaDelete(Class<T> cls) {
        return new MansartCriteriaDelete<>(this, cls);
    }

    // === Compound Selection ===

    @Override public <Y> CompoundSelection<Y> construct(Class<Y> cls, Selection<?>... selections) {
        return null;
    }

    @Override public CompoundSelection<Tuple> tuple(Selection<?>... selections) {
        return null;
    }

    @Override public CompoundSelection<Tuple> tuple(List<Selection<?>> selections) {
        return null;
    }

    @Override public CompoundSelection<Object[]> array(Selection<?>... selections) {
        return null;
    }

    @Override public CompoundSelection<Object[]> array(List<Selection<?>> selections) {
        return null;
    }

    // === Ordering ===

    @Override public Order asc(Expression<?> x) { return null; }
    @Override public Order desc(Expression<?> x) { return null; }
    @Override public Order asc(Expression<?> x, Nulls nulls) { return null; }
    @Override public Order desc(Expression<?> x, Nulls nulls) { return null; }

    // === Aggregation ===

    @Override public <N extends Number> Expression<Double> avg(Expression<N> x) {
        return new LiteralExpression<>(0.0);
    }

    @Override public <N extends Number> Expression<N> sum(Expression<N> x) {
        return new LiteralExpression<>(null);
    }

    @Override public Expression<Long> sumAsLong(Expression<Integer> x) {
        return new LiteralExpression<>(0L);
    }

    @Override public Expression<Double> sumAsDouble(Expression<Float> x) {
        return new LiteralExpression<>(0.0);
    }

    @Override public <N extends Number> Expression<N> max(Expression<N> x) {
        return new LiteralExpression<>(null);
    }

    @Override public <N extends Number> Expression<N> min(Expression<N> x) {
        return new LiteralExpression<>(null);
    }

    @Override public <X extends Comparable<? super X>> Expression<X> greatest(Expression<X> x) {
        return new LiteralExpression<>(null);
    }

    @Override public <X extends Comparable<? super X>> Expression<X> least(Expression<X> x) {
        return new LiteralExpression<>(null);
    }

    @Override public Expression<Long> count(Expression<?> x) {
        return new LiteralExpression<>(0L);
    }

    @Override public Expression<Long> countDistinct(Expression<?> x) {
        return new LiteralExpression<>(0L);
    }

    // === Subquery ===

    @Override public Predicate exists(Subquery<?> subquery) { return null; }
    @Override public <Y> Expression<Y> all(Subquery<Y> subquery) { return null; }
    @Override public <Y> Expression<Y> some(Subquery<Y> subquery) { return null; }
    @Override public <Y> Expression<Y> any(Subquery<Y> subquery) { return null; }

    // === Boolean Predicates ===

    @Override public Predicate and(Expression<Boolean> x, Expression<Boolean> y) {
        return new AndPredicate(x, y);
    }

    @Override public Predicate and(Predicate... predicates) {
        List<Predicate> valid = new ArrayList<>();
        for (Predicate p : predicates) { if (p != null) valid.add(p); }
        if (valid.isEmpty()) return null;
        if (valid.size() == 1) return valid.get(0);
        return new AndPredicate(valid.toArray(new Predicate[0]));
    }

    @Override public Predicate and(List<Predicate> predicates) {
        List<Predicate> valid = new ArrayList<>();
        for (Predicate p : predicates) { if (p != null) valid.add(p); }
        if (valid.isEmpty()) return null;
        if (valid.size() == 1) return valid.get(0);
        return new AndPredicate(valid.toArray(new Predicate[0]));
    }

    @Override public Predicate or(Expression<Boolean> x, Expression<Boolean> y) {
        return new OrPredicate(x, y);
    }

    @Override public Predicate or(Predicate... predicates) {
        List<Predicate> valid = new ArrayList<>();
        for (Predicate p : predicates) { if (p != null) valid.add(p); }
        if (valid.isEmpty()) return null;
        if (valid.size() == 1) return valid.get(0);
        return new OrPredicate(valid.toArray(new Predicate[0]));
    }

    @Override public Predicate or(List<Predicate> predicates) {
        List<Predicate> valid = new ArrayList<>();
        for (Predicate p : predicates) { if (p != null) valid.add(p); }
        if (valid.isEmpty()) return null;
        if (valid.size() == 1) return valid.get(0);
        return new OrPredicate(valid.toArray(new Predicate[0]));
    }

    @Override public Predicate not(Expression<Boolean> x) {
        return new NotPredicate(x);
    }

    @Override public Predicate conjunction() { return null; }
    @Override public Predicate disjunction() { return null; }

    @Override public Predicate isTrue(Expression<Boolean> x) {
        return new EqualPredicate<>(x, literal(true));
    }

    @Override public Predicate isFalse(Expression<Boolean> x) {
        return new EqualPredicate<>(x, literal(false));
    }

    @Override public Predicate isNull(Expression<?> x) {
        return new NullCheckPredicate(x, Predicate.BooleanOperator.IS_NULL);
    }

    @Override public Predicate isNotNull(Expression<?> x) {
        return new NullCheckPredicate(x, Predicate.BooleanOperator.IS_NOT_NULL);
    }

    // === Equality ===

    @Override public Predicate equal(Expression<?> x, Expression<?> y) {
        return new EqualPredicate<>(x, y);
    }

    @Override public Predicate equal(Expression<?> x, Object y) {
        return new EqualPredicate<>(x, literal(y));
    }

    @Override public Predicate notEqual(Expression<?> x, Expression<?> y) {
        return new NotPredicate<>(equal(x, y));
    }

    @Override public Predicate notEqual(Expression<?> x, Object y) {
        return new NotPredicate<>(equal(x, y));
    }

    // === Comparison (Comparable) ===

    @Override public <Y extends Comparable<? super Y>> Predicate greaterThan(Expression<? extends Y> x, Expression<? extends Y> y) {
        return new ComparisonPredicate<>(x, y, Predicate.BooleanOperator.GT);
    }

    @Override public <Y extends Comparable<? super Y>> Predicate greaterThan(Expression<? extends Y> x, Y y) {
        return new ComparisonPredicate<>(x, literal(y), Predicate.BooleanOperator.GT);
    }

    @Override public <Y extends Comparable<? super Y>> Predicate greaterThanOrEqualTo(Expression<? extends Y> x, Expression<? extends Y> y) {
        return new ComparisonPredicate<>(x, y, Predicate.BooleanOperator.GE);
    }

    @Override public <Y extends Comparable<? super Y>> Predicate greaterThanOrEqualTo(Expression<? extends Y> x, Y y) {
        return new ComparisonPredicate<>(x, literal(y), Predicate.BooleanOperator.GE);
    }

    @Override public <Y extends Comparable<? super Y>> Predicate lessThan(Expression<? extends Y> x, Expression<? extends Y> y) {
        return new ComparisonPredicate<>(x, y, Predicate.BooleanOperator.LT);
    }

    @Override public <Y extends Comparable<? super Y>> Predicate lessThan(Expression<? extends Y> x, Y y) {
        return new ComparisonPredicate<>(x, literal(y), Predicate.BooleanOperator.LT);
    }

    @Override public <Y extends Comparable<? super Y>> Predicate lessThanOrEqualTo(Expression<? extends Y> x, Expression<? extends Y> y) {
        return new ComparisonPredicate<>(x, y, Predicate.BooleanOperator.LE);
    }

    @Override public <Y extends Comparable<? super Y>> Predicate lessThanOrEqualTo(Expression<? extends Y> x, Y y) {
        return new ComparisonPredicate<>(x, literal(y), Predicate.BooleanOperator.LE);
    }

    // === Comparison (Number) ===

    @Override public Predicate gt(Expression<? extends Number> x, Expression<? extends Number> y) {
        return new ComparisonPredicate<>(x, y, Predicate.BooleanOperator.GT);
    }

    @Override public Predicate gt(Expression<? extends Number> x, Number y) {
        return new ComparisonPredicate<>(x, literal(y), Predicate.BooleanOperator.GT);
    }

    @Override public Predicate ge(Expression<? extends Number> x, Expression<? extends Number> y) {
        return new ComparisonPredicate<>(x, y, Predicate.BooleanOperator.GE);
    }

    @Override public Predicate ge(Expression<? extends Number> x, Number y) {
        return new ComparisonPredicate<>(x, literal(y), Predicate.BooleanOperator.GE);
    }

    @Override public Predicate lt(Expression<? extends Number> x, Expression<? extends Number> y) {
        return new ComparisonPredicate<>(x, y, Predicate.BooleanOperator.LT);
    }

    @Override public Predicate lt(Expression<? extends Number> x, Number y) {
        return new ComparisonPredicate<>(x, literal(y), Predicate.BooleanOperator.LT);
    }

    @Override public Predicate le(Expression<? extends Number> x, Expression<? extends Number> y) {
        return new ComparisonPredicate<>(x, y, Predicate.BooleanOperator.LE);
    }

    @Override public Predicate le(Expression<? extends Number> x, Number y) {
        return new ComparisonPredicate<>(x, literal(y), Predicate.BooleanOperator.LE);
    }

    // === Between ===

    @Override public <Y extends Comparable<? super Y>> Predicate between(Expression<? extends Y> x, Expression<? extends Y> val1, Expression<? extends Y> val2) {
        return greaterThanOrEqualTo(x, val1).and(lessThanOrEqualTo(x, val2));
    }

    @Override public <Y extends Comparable<? super Y>> Predicate between(Expression<? extends Y> x, Y val1, Y val2) {
        return between(x, literal(val1), literal(val2));
    }

    // === Number Functions ===

    @Override public Expression<Integer> sign(Expression<? extends Number> x) {
        return new LiteralExpression<>(0);
    }

    @Override public <N extends Number> Expression<N> neg(Expression<N> x) {
        return new LiteralExpression<>(null);
    }

    @Override public <N extends Number> Expression<N> abs(Expression<N> x) {
        return new LiteralExpression<>(null);
    }

    @Override public <N extends Number> Expression<N> ceiling(Expression<N> x) {
        return new LiteralExpression<>(null);
    }

    @Override public <N extends Number> Expression<N> floor(Expression<N> x) {
        return new LiteralExpression<>(null);
    }

    @Override public <N extends Number> Expression<N> sum(Expression<? extends N> x, Expression<? extends N> y) {
        return new LiteralExpression<>(null);
    }

    @Override public <N extends Number> Expression<N> sum(Expression<? extends N> x, N y) {
        return new LiteralExpression<>(null);
    }

    @Override public <N extends Number> Expression<N> sum(N x, Expression<? extends N> y) {
        return new LiteralExpression<>(null);
    }

    @Override public <N extends Number> Expression<N> prod(Expression<? extends N> x, Expression<? extends N> y) {
        return new LiteralExpression<>(null);
    }

    @Override public <N extends Number> Expression<N> prod(Expression<? extends N> x, N y) {
        return new LiteralExpression<>(null);
    }

    @Override public <N extends Number> Expression<N> prod(N x, Expression<? extends N> y) {
        return new LiteralExpression<>(null);
    }

    @Override public <N extends Number> Expression<N> diff(Expression<? extends N> x, Expression<? extends N> y) {
        return new LiteralExpression<>(null);
    }

    @Override public <N extends Number> Expression<N> diff(Expression<? extends N> x, N y) {
        return new LiteralExpression<>(null);
    }

    @Override public <N extends Number> Expression<N> diff(N x, Expression<? extends N> y) {
        return new LiteralExpression<>(null);
    }

    @Override public Expression<Number> quot(Expression<? extends Number> x, Expression<? extends Number> y) {
        return new LiteralExpression<>(null);
    }

    @Override public Expression<Number> quot(Expression<? extends Number> x, Number y) {
        return new LiteralExpression<>(null);
    }

    @Override public Expression<Number> quot(Number x, Expression<? extends Number> y) {
        return new LiteralExpression<>(null);
    }

    @Override public Expression<Integer> mod(Expression<Integer> x, Expression<Integer> y) {
        return new LiteralExpression<>(0);
    }

    @Override public Expression<Integer> mod(Expression<Integer> x, Integer y) {
        return new LiteralExpression<>(0);
    }

    @Override public Expression<Integer> mod(Integer x, Expression<Integer> y) {
        return new LiteralExpression<>(0);
    }

    @Override public Expression<Double> sqrt(Expression<? extends Number> x) {
        return new LiteralExpression<>(0.0);
    }

    @Override public Expression<Double> exp(Expression<? extends Number> x) {
        return new LiteralExpression<>(0.0);
    }

    @Override public Expression<Double> ln(Expression<? extends Number> x) {
        return new LiteralExpression<>(0.0);
    }

    @Override public Expression<Double> power(Expression<? extends Number> x, Expression<? extends Number> y) {
        return new LiteralExpression<>(0.0);
    }

    @Override public Expression<Double> power(Expression<? extends Number> x, Number y) {
        return new LiteralExpression<>(0.0);
    }

    @Override public <T extends Number> Expression<T> round(Expression<T> x, Integer scale) {
        return new LiteralExpression<>(null);
    }

    @Override public Expression<Long> toLong(Expression<? extends Number> x) {
        return new LiteralExpression<>(0L);
    }

    @Override public Expression<Integer> toInteger(Expression<? extends Number> x) {
        return new LiteralExpression<>(0);
    }

    @Override public Expression<Float> toFloat(Expression<? extends Number> x) {
        return new LiteralExpression<>(0.0f);
    }

    @Override public Expression<Double> toDouble(Expression<? extends Number> x) {
        return new LiteralExpression<>(0.0);
    }

    @Override public Expression<BigDecimal> toBigDecimal(Expression<? extends Number> x) {
        return new LiteralExpression<>(null);
    }

    @Override public Expression<BigInteger> toBigInteger(Expression<? extends Number> x) {
        return new LiteralExpression<>(null);
    }

    @Override public Expression<String> toString(Expression<Character> x) {
        return new LiteralExpression<>("");
    }

    // === Literals & Parameters ===

    @Override public <T> Expression<T> literal(T value) {
        if (value == null) throw new IllegalArgumentException("literal argument cannot be null");
        return new LiteralExpression<>(value);
    }

    @Override public <T> Expression<T> nullLiteral(Class<T> type) {
        return new LiteralExpression<>(null);
    }

    @Override public ParameterExpression<?> parameter() {
        return new ParameterExpressionImpl<>(null, null);
    }

    @Override public <T> ParameterExpression<T> parameter(Class<T> type) {
        return new ParameterExpressionImpl<>(type, null);
    }

    @Override public <T> ParameterExpression<T> parameter(Class<T> type, String name) {
        return new ParameterExpressionImpl<>(type, name);
    }

    // === Collection Predicates ===

    @Override public <C extends Collection<?>> Predicate isEmpty(Expression<C> x) {
        return new EqualPredicate<>(size(x), literal(0));
    }

    @Override public <C extends Collection<?>> Predicate isNotEmpty(Expression<C> x) {
        return new NotPredicate<>(isEmpty(x));
    }

    @Override public <C extends Collection<?>> Expression<Integer> size(Expression<C> x) {
        return new LiteralExpression<>(0);
    }

    @Override public <C extends Collection<?>> Expression<Integer> size(C coll) {
        return new LiteralExpression<>(coll.size());
    }

    // === Member Predicates ===

    @Override public <E, C extends Collection<E>> Predicate isMember(Expression<E> elem, Expression<C> coll) {
        return in(elem, coll);
    }

    @Override public <E, C extends Collection<E>> Predicate isMember(E elem, Expression<C> coll) {
        return in(literal(elem), coll);
    }

    @Override public <E, C extends Collection<E>> Predicate isNotMember(Expression<E> elem, Expression<C> coll) {
        return notIn(elem, coll);
    }

    @Override public <E, C extends Collection<E>> Predicate isNotMember(E elem, Expression<C> coll) {
        return notIn(literal(elem), coll);
    }

    // === Map Predicates ===

    @Override public <V, M extends Map<?, V>> Expression<Collection<V>> values(M map) {
        return new LiteralExpression<>(null);
    }

    @Override public <K, M extends Map<K, ?>> Expression<Set<K>> keys(M map) {
        return new LiteralExpression<>(null);
    }

    // === Like Predicates ===

    @Override public Predicate like(Expression<String> expr, Expression<String> pattern) {
        return new ComparisonPredicate<>(expr, pattern, Predicate.BooleanOperator.LIKE);
    }

    @Override public Predicate like(Expression<String> expr, String pattern) {
        return new ComparisonPredicate<>(expr, literal(pattern), Predicate.BooleanOperator.LIKE);
    }

    @Override public Predicate like(Expression<String> expr, Expression<String> pattern, Expression<Character> escapeChar) {
        return new ComparisonPredicate<>(expr, pattern, Predicate.BooleanOperator.LIKE);
    }

    @Override public Predicate like(Expression<String> expr, Expression<String> pattern, char escapeChar) {
        return new ComparisonPredicate<>(expr, pattern, Predicate.BooleanOperator.LIKE);
    }

    @Override public Predicate like(Expression<String> expr, String pattern, Expression<Character> escapeChar) {
        return new ComparisonPredicate<>(expr, literal(pattern), Predicate.BooleanOperator.LIKE);
    }

    @Override public Predicate like(Expression<String> expr, String pattern, char escapeChar) {
        return new ComparisonPredicate<>(expr, literal(pattern), Predicate.BooleanOperator.LIKE);
    }

    @Override public Predicate notLike(Expression<String> expr, Expression<String> pattern) {
        return new NotPredicate<>(like(expr, pattern));
    }

    @Override public Predicate notLike(Expression<String> expr, String pattern) {
        return new NotPredicate<>(like(expr, pattern));
    }

    @Override public Predicate notLike(Expression<String> expr, Expression<String> pattern, Expression<Character> escapeChar) {
        return new NotPredicate<>(like(expr, pattern, escapeChar));
    }

    @Override public Predicate notLike(Expression<String> expr, Expression<String> pattern, char escapeChar) {
        return new NotPredicate<>(like(expr, pattern, escapeChar));
    }

    @Override public Predicate notLike(Expression<String> expr, String pattern, Expression<Character> escapeChar) {
        return new NotPredicate<>(like(expr, pattern, escapeChar));
    }

    @Override public Predicate notLike(Expression<String> expr, String pattern, char escapeChar) {
        return new NotPredicate<>(like(expr, pattern, escapeChar));
    }

    // === String Functions ===

    @Override public Expression<String> concat(List<Expression<String>> expressions) {
        return new LiteralExpression<>("");
    }

    @Override public Expression<String> concat(Expression<String> x, Expression<String> y) {
        return new LiteralExpression<>("");
    }

    @Override public Expression<String> concat(Expression<String> x, String y) {
        return new LiteralExpression<>("");
    }

    @Override public Expression<String> concat(String x, Expression<String> y) {
        return new LiteralExpression<>("");
    }

    @Override public Expression<String> substring(Expression<String> x, Expression<Integer> start) {
        return new LiteralExpression<>("");
    }

    @Override public Expression<String> substring(Expression<String> x, int start) {
        return new LiteralExpression<>("");
    }

    @Override public Expression<String> substring(Expression<String> x, Expression<Integer> start, Expression<Integer> len) {
        return new LiteralExpression<>("");
    }

    @Override public Expression<String> substring(Expression<String> x, int start, int len) {
        return new LiteralExpression<>("");
    }

    @Override public Expression<String> trim(Expression<String> x) {
        return new LiteralExpression<>("");
    }

    @Override public Expression<String> trim(Trimspec spec, Expression<String> x) {
        return new LiteralExpression<>("");
    }

    @Override public Expression<String> trim(Expression<Character> trimChar, Expression<String> x) {
        return new LiteralExpression<>("");
    }

    @Override public Expression<String> trim(Trimspec spec, Expression<Character> trimChar, Expression<String> x) {
        return new LiteralExpression<>("");
    }

    @Override public Expression<String> trim(char trimChar, Expression<String> x) {
        return new LiteralExpression<>("");
    }

    @Override public Expression<String> trim(Trimspec spec, char trimChar, Expression<String> x) {
        return new LiteralExpression<>("");
    }

    @Override public Expression<String> lower(Expression<String> x) {
        return new LiteralExpression<>("");
    }

    @Override public Expression<String> upper(Expression<String> x) {
        return new LiteralExpression<>("");
    }

    @Override public Expression<Integer> length(Expression<String> x) {
        return new LiteralExpression<>(0);
    }

    @Override public Expression<String> left(Expression<String> x, int len) {
        return new LiteralExpression<>("");
    }

    @Override public Expression<String> right(Expression<String> x, int len) {
        return new LiteralExpression<>("");
    }

    @Override public Expression<String> left(Expression<String> x, Expression<Integer> len) {
        return new LiteralExpression<>("");
    }

    @Override public Expression<String> right(Expression<String> x, Expression<Integer> len) {
        return new LiteralExpression<>("");
    }

    @Override public Expression<String> replace(Expression<String> x, Expression<String> target, Expression<String> replacement) {
        return new LiteralExpression<>("");
    }

    @Override public Expression<String> replace(Expression<String> x, String target, Expression<String> replacement) {
        return new LiteralExpression<>("");
    }

    @Override public Expression<String> replace(Expression<String> x, Expression<String> target, String replacement) {
        return new LiteralExpression<>("");
    }

    @Override public Expression<String> replace(Expression<String> x, String target, String replacement) {
        return new LiteralExpression<>("");
    }

    @Override public Expression<Integer> locate(Expression<String> expr, Expression<String> target) {
        return new LiteralExpression<>(0);
    }

    @Override public Expression<Integer> locate(Expression<String> expr, String target) {
        return new LiteralExpression<>(0);
    }

    @Override public Expression<Integer> locate(Expression<String> expr, Expression<String> target, Expression<Integer> start) {
        return new LiteralExpression<>(0);
    }

    @Override public Expression<Integer> locate(Expression<String> expr, String target, int start) {
        return new LiteralExpression<>(0);
    }

    // === Temporal Functions ===

    @Override public Expression<Date> currentDate() {
        return new LiteralExpression<>(null);
    }

    @Override public Expression<Timestamp> currentTimestamp() {
        return new LiteralExpression<>(null);
    }

    @Override public Expression<Time> currentTime() {
        return new LiteralExpression<>(null);
    }

    @Override public Expression<LocalDate> localDate() {
        return new LiteralExpression<>(null);
    }

    @Override public Expression<LocalDateTime> localDateTime() {
        return new LiteralExpression<>(null);
    }

    @Override public Expression<LocalTime> localTime() {
        return new LiteralExpression<>(null);
    }

    @Override public <N, T extends Temporal> Expression<N> extract(TemporalField<N, T> field, Expression<T> x) {
        return new LiteralExpression<>(null);
    }

    // === In Predicates ===

    @Override public <T> In<T> in(Expression<? extends T> x) {
        return new MansartIn<>(x);
    }

    @Override public <X> Predicate in(Expression<? extends X> x, Collection<? extends X> values) {
        MansartIn<X> in = new MansartIn<>(x);
        in.getValueList().addAll(values);
        return in;
    }

    @Override public <X> Predicate in(Expression<? extends X> x, Expression<? extends X>... values) {
        MansartIn<X> in = new MansartIn<>(x);
        for (Expression<? extends X> v : values) { in.getValueList().add((X) v); }
        return in;
    }

    @Override public <X> Predicate notIn(Expression<? extends X> x, Collection<? extends X> values) {
        MansartIn<X> in = new MansartIn<>(x);
        in.getValueList().addAll(values);
        in.setNotIn(true);
        return in;
    }

    @Override public <X> Predicate notIn(Expression<? extends X> x, Expression<? extends X>... values) {
        MansartIn<X> in = new MansartIn<>(x);
        for (Expression<? extends X> v : values) { in.getValueList().add((X) v); }
        in.setNotIn(true);
        return in;
    }

    // === Coalesce & NullIf ===

    @Override public <Y> Expression<Y> coalesce(Expression<? extends Y> x, Expression<? extends Y> y) {
        return new LiteralExpression<>(null);
    }

    @Override public <Y> Expression<Y> coalesce(Expression<? extends Y> x, Y y) {
        return new LiteralExpression<>(null);
    }

    @Override public <T> Coalesce<T> coalesce() {
        return new LiteralExpression<>(null);
    }

    @Override public <Y> Expression<Y> nullif(Expression<Y> x, Expression<?> y) {
        return new LiteralExpression<>(null);
    }

    @Override public <Y> Expression<Y> nullif(Expression<Y> x, Y y) {
        return new LiteralExpression<>(null);
    }

    // === Case ===

    @Override public <C, R> SimpleCase<C, R> selectCase(Expression<? extends C> x) {
        return new LiteralExpression<>(null);
    }

    @Override public <R> Case<R> selectCase() {
        return new LiteralExpression<>(null);
    }

    // === Function ===

    @Override public <T> Expression<T> function(String name, Class<T> returnType, Expression<?>... args) {
        return new LiteralExpression<>(null);
    }

    // === Treat ===

    @Override public <X, T, V extends T> Join<X, V> treat(Join<X, T> join, Class<V> type) {
        return new MansartJoin<>((From<X, ?>) join, "");
    }

    @Override public <X, T, E extends T> CollectionJoin<X, E> treat(CollectionJoin<X, T> join, Class<E> type) {
        return new MansartJoin<>((From<X, ?>) join, "");
    }

    @Override public <X, T, E extends T> SetJoin<X, E> treat(SetJoin<X, T> join, Class<E> type) {
        return new MansartJoin<>((From<X, ?>) join, "");
    }

    @Override public <X, T, E extends T> ListJoin<X, E> treat(ListJoin<X, T> join, Class<E> type) {
        return new MansartJoin<>((From<X, ?>) join, "");
    }

    @Override public <X, K, T, V extends T> MapJoin<X, K, V> treat(MapJoin<X, K, T> join, Class<V> type) {
        return new MansartJoin<>((From<X, ?>) join, "");
    }

    @Override public <X, T extends X> Path<T> treat(Path<X> path, Class<T> type) {
        return new MansartPath<>(null, null, "", type);
    }

    @Override public <X, T extends X> Root<T> treat(Root<X> root, Class<T> type) {
        return new MansartRoot<>(this, type);
    }

    // === Set Operations ===

    @Override public <T> CriteriaSelect<T> union(CriteriaSelect<? extends T> x, CriteriaSelect<? extends T> y) {
        return null;
    }

    @Override public <T> CriteriaSelect<T> unionAll(CriteriaSelect<? extends T> x, CriteriaSelect<? extends T> y) {
        return null;
    }

    @Override public <T> CriteriaSelect<T> intersect(CriteriaSelect<? super T> x, CriteriaSelect<? super T> y) {
        return null;
    }

    @Override public <T> CriteriaSelect<T> intersectAll(CriteriaSelect<? super T> x, CriteriaSelect<? super T> y) {
        return null;
    }

    @Override public <T> CriteriaSelect<T> except(CriteriaSelect<T> x, CriteriaSelect<?> y) {
        return null;
    }

    @Override public <T> CriteriaSelect<T> exceptAll(CriteriaSelect<T> x, CriteriaSelect<?> y) {
        return null;
    }

    // === Inner Classes ===

    public static class LiteralExpression<T> implements Expression<T>, ValueExpression<T> {
        private final T value;
        public LiteralExpression(T value) { this.value = value; }
        @Override public <R, C> R accept(ExpressionVisitor<R, C> visitor, C context) { return null; }
        @Override public Class<T> getJavaType() { return (Class<T>) (value == null ? Object.class : value.getClass()); }
        @Override public jakarta.persistence.criteria.Type<T> getExpressionType() { return null; }
        @Override public boolean isCompoundSelection() { return false; }
        @Override public List<? extends TupleElement<?>> getCompoundSelectionItems() { return Collections.emptyList(); }
        @Override public String toString() { return "Literal[" + value + "]"; }
    }

    public static class ParameterExpressionImpl<T> implements ParameterExpression<T> {
        private final java.lang.reflect.Type type;
        private final String name;
        public ParameterExpressionImpl(java.lang.reflect.Type type, String name) { this.type = type; this.name = name; }
        @Override public Class<T> getJavaType() { return type != null ? (Class<T>) type : Object.class; }
        @Override public String getName() { return name; }
        @Override public java.lang.reflect.Type getType() { return type; }
        @Override public String toString() { return "Parameter[" + name + "]"; }
    }

    public static class EqualPredicate<T> implements Predicate {
        private final Expression<?> left;
        private final Expression<?> right;
        public EqualPredicate(Expression<?> left, Expression<?> right) { this.left = left; this.right = right; }
        @Override public <R, C> R accept(ExpressionVisitor<R, C> visitor, C context) { return null; }
        @Override public String toString() { return left + " = " + right; }
    }

    public static class NotPredicate implements Predicate {
        private final Predicate predicate;
        public NotPredicate(Predicate predicate) { this.predicate = predicate; }
        @Override public <R, C> R accept(ExpressionVisitor<R, C> visitor, C context) { return null; }
        @Override public String toString() { return "NOT " + predicate; }
    }

    public static class AndPredicate implements Predicate {
        private final Predicate[] predicates;
        public AndPredicate(Predicate... predicates) { this.predicates = predicates; }
        public AndPredicate(Predicate[] predicates) { this.predicates = predicates; }
        @Override public <R, C> R accept(ExpressionVisitor<R, C> visitor, C context) { return null; }
        @Override public String toString() { return "AND " + Arrays.toString(predicates); }
    }

    public static class OrPredicate implements Predicate {
        private final Predicate[] predicates;
        public OrPredicate(Predicate... predicates) { this.predicates = predicates; }
        public OrPredicate(Predicate[] predicates) { this.predicates = predicates; }
        @Override public <R, C> R accept(ExpressionVisitor<R, C> visitor, C context) { return null; }
        @Override public String toString() { return "OR " + Arrays.toString(predicates); }
    }

    public static class NullCheckPredicate implements Predicate {
        private final Expression<?> expression;
        private final Predicate.BooleanOperator operator;
        public NullCheckPredicate(Expression<?> expression, Predicate.BooleanOperator operator) {
            this.expression = expression; this.operator = operator;
        }
        @Override public <R, C> R accept(ExpressionVisitor<R, C> visitor, C context) { return null; }
        @Override public String toString() { return expression + " " + operator; }
    }

    public static class ComparisonPredicate implements Predicate {
        private final Expression<?> left;
        private final Expression<?> right;
        private final Predicate.BooleanOperator operator;
        public ComparisonPredicate(Expression<?> left, Expression<?> right, Predicate.BooleanOperator operator) {
            this.left = left; this.right = right; this.operator = operator;
        }
        @Override public <R, C> R accept(ExpressionVisitor<R, C> visitor, C context) { return null; }
        @Override public String toString() { return left + " " + operator + " " + right; }
    }

    public static class MansartIn<X> implements In<X> {
        private final Expression<? extends X> expression;
        private final List<X> values = new ArrayList<>();
        private boolean notIn = false;

        public MansartIn(Expression<? extends X> expression) {
            this.expression = expression;
        }

        @Override public Expression<? extends X> getExpression() { return expression; }
        @Override public List<X> getValueList() { return values; }
        @Override public boolean isIn() { return !notIn; }
        @Override public In<X> value(X value) { values.add(value); return this; }
        @Override public In<X> value(Expression<? extends X> value) { values.add((X) value); return this; }
        @Override public <R, C> R accept(ExpressionVisitor<R, C> visitor, C context) { return null; }
        @Override public String toString() { return expression + (notIn ? " NOT IN " : " IN ") + values; }

        void setNotIn(boolean notIn) { this.notIn = notIn; }
    }

    public static class MansartJoin<X, Y> implements Join<X, Y>, Fetch<X, Y> {
        private final From<X, ?> parent;
        private final String attributeName;
        private JoinType joinType = JoinType.INNER;

        public MansartJoin(From<X, ?> parent, String attributeName) {
            this.parent = parent; this.attributeName = attributeName;
        }

        @Override public JoinType getJoinType() { return joinType; }
        @Override public boolean isAll() { return false; }
        @Override public void setAll(boolean all) { }
        @Override public boolean isDistinct() { return false; }
        @Override public void setDistinct(boolean distinct) { }
        @Override public <V> From<X, V> on(Expression<Boolean> restriction) { return this; }
        @Override public Path<?> getParent() { return parent; }
        @Override public BindableType getBindableType() { return Bindable.BindableType.ENTITY_TYPE; }
        @Override public Class<Y> getJavaType() { return Object.class; }
        @Override public String getAlias() { return attributeName; }
        @Override public void setAlias(String alias) { }
        @Override public Expression<?> get(SingularAttribute<? super X, ?> attr) { return get(attr.getName()); }
        @Override public <Z> Expression<Z> get(String attributeName) { return new MansartPath<>(null, this, attributeName, Object.class); }
        @Override public <Z> Expression<Z> get(String attributeName, Class<Z> type) { return new MansartPath<>(null, this, attributeName, type); }
        @Override public <K, V> MapExpression<X, K, V> getMap(String attributeName, Class<K> keyJavaType, Class<V> valueJavaType) { return new MansartPath<>(null, this, attributeName, Object.class); }
        @Override public <C extends Collection<?>> CollectionExpression<X, ?> getCollection(String attributeName) { return new MansartPath<>(null, this, attributeName, Object.class); }
        @Override public <E, C extends Collection<E>> CollectionExpression<X, C, E> getCollection(String attributeName, Class<C> collectionClass, Class<E> elementClass) { return new MansartPath<>(null, this, attributeName, Object.class); }
        @Override public <E, C extends Set<E>> SetExpression<X, C, E> getSet(String attributeName, Class<C> setClass, Class<E> elementClass) { return new MansartPath<>(null, this, attributeName, Object.class); }
        @Override public <E, C extends List<E>> ListExpression<X, C, E> getList(String attributeName, Class<C> listClass, Class<E> elementClass) { return new MansartPath<>(null, this, attributeName, Object.class); }
        @Override public <Z> Expression<Z> get(SingularAttribute<? super X, Z> attr) { return get(attr.getName()); }
        @Override public <E> Expression<E> get(PluralAttribute<? super X, ?, E> attr) { return get(attr.getName()); }
        @Override public <Z> Expression<Z> get(String attributeName, String... pathNames) {
            Expression<?> current = get(attributeName);
            for (String name : pathNames) { current = new MansartPath<>(null, current, name, Object.class); }
            return (Expression<Z>) current;
        }
        @Override public Set<Join<X, ?>> getJoins() { return Collections.emptySet(); }
        @Override public boolean isCompoundSelection() { return false; }
        @Override public List<? extends TupleElement<?>> getCompoundSelectionItems() { return Collections.emptyList(); }
        @Override public <N extends Number> Expression<N> getNumber() { return new MansartPath<>(null, this, null, Number.class); }
        @Override public Expression<String> getString() { return new MansartPath<>(null, this, null, String.class); }
        @Override public <Z> Expression<Z> as(Class<Z> type) { return new MansartPath<>(null, this, null, type); }
        @Override public <R, C> R accept(ExpressionVisitor<R, C> visitor, C context) { return null; }
    }
}

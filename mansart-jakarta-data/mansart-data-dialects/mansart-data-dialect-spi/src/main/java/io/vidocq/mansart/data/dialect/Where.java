/*
 * Copyright (c) ${year} Yann Blazart, Antoine Sabot-Durand and the Vidocq contributors
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
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-1.2
 *
 * SPDX-License-Identifier: EPL-2.0 OR EUPL-1.2 OR GPL-2.0-or-later
 */

package io.vidocq.mansart.data.dialect;

import java.util.List;

/**
 * Backend-neutral predicate AST. Lowered to SQL by {@link Dialect#select}.
 */
public sealed interface Where {

    Where ALWAYS_TRUE  = new AlwaysTrue();
    Where ALWAYS_FALSE = new AlwaysFalse();

    record Eq(Attribute<?, ?> attr) implements Where {}
    record NotEq(Attribute<?, ?> attr) implements Where {}
    record Lt(Attribute<?, ?> attr) implements Where {}
    record Lte(Attribute<?, ?> attr) implements Where {}
    record Gt(Attribute<?, ?> attr) implements Where {}
    record Gte(Attribute<?, ?> attr) implements Where {}
    record Like(Attribute<?, ?> attr) implements Where {}
    record In(Attribute<?, ?> attr, int arity) implements Where {}
    record Between(Attribute<?, ?> attr) implements Where {}
    record IsNull(Attribute<?, ?> attr) implements Where {}
    record IsNotNull(Attribute<?, ?> attr) implements Where {}
    record And(List<Where> children) implements Where {
        public And { children = List.copyOf(children); }
    }
    record Or(List<Where> children) implements Where {
        public Or { children = List.copyOf(children); }
    }
    record Not(Where child) implements Where {}
    /**
     * M7-27 — wraps a text comparator (Eq, NotEq, Like) so the dialect emits
     * {@code LOWER(col) <op> LOWER(?)} instead of relying on the caller to
     * lowercase the bound argument. Honors database collation/locale and lets
     * functional indexes on {@code LOWER(col)} kick in.
     */
    record IgnoreCase(Where inner) implements Where {}
    /**
     * M8-1 — wraps a comparator with a unary scalar function applied to the column.
     * {@code fn} is one of {@code "UPPER"}, {@code "LOWER"}, {@code "LENGTH"}, {@code "ABS"}.
     *
     * <p>For {@code UPPER}/{@code LOWER}/{@code ABS}, the bound parameter type matches the
     * column's Java type. For {@code LENGTH}, the bound parameter is forced to {@link Integer}
     * (returns the character count of a String). Dialects map {@code LENGTH} to the SQL
     * portable {@code CHAR_LENGTH(...)}.
     */
    record Func(String fn, Where inner) implements Where {}
    record AlwaysTrue() implements Where {}
    record AlwaysFalse() implements Where {}
    /**
     * M6 — EXISTS predicate with a subquery.
     * The SqlFragment represents the complete subquery (including SELECT, FROM, WHERE, etc.).
     */
    record Exists(SqlFragment subquery, boolean not) implements Where {}

    /**
     * M6 — ALL predicate: expression operator {ALL | ANY | SOME} (subquery).
     * The SqlFragment represents the complete subquery.
     */
    record All(Attribute<?, ?> attr, SqlFragment subquery, String operator) implements Where {}

    /**
     * M6 — ANY predicate: expression operator {ALL | ANY | SOME} (subquery).
     * ANY and SOME are semantically equivalent.
     */
    record Any(Attribute<?, ?> attr, SqlFragment subquery, String operator) implements Where {}

    /**
     * M6 — SOME predicate: expression operator {ALL | ANY | SOME} (subquery).
     * SOME is semantically equivalent to ANY.
     */
    record Some(Attribute<?, ?> attr, SqlFragment subquery, String operator) implements Where {}

    static Where eq(Attribute<?, ?> a)        { return new Eq(a); }
    static Where between(Attribute<?, ?> a)   { return new Between(a); }
    static Where in(Attribute<?, ?> a, int n) { return new In(a, n); }
    static Where and(Where... ws)             { return new And(List.of(ws)); }
    static Where or(Where... ws)              { return new Or(List.of(ws)); }
    static Where exists(SqlFragment subquery) { return new Exists(subquery, false); }
    static Where notExists(SqlFragment subquery) { return new Exists(subquery, true); }
    static Where all(Attribute<?, ?> attr, SqlFragment subquery, String operator) { return new All(attr, subquery, operator); }
    static Where any(Attribute<?, ?> attr, SqlFragment subquery, String operator) { return new Any(attr, subquery, operator); }
    static Where some(Attribute<?, ?> attr, SqlFragment subquery, String operator) { return new Some(attr, subquery, operator); }
}

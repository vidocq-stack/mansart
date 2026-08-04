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

package io.vidocq.mansart.data.core;

import io.vidocq.mansart.data.dialect.Dialect;
import io.vidocq.mansart.data.dialect.Where;
import io.vidocq.mansart.data.dialect.Where.AlwaysFalse;
import io.vidocq.mansart.data.dialect.Where.AlwaysTrue;
import io.vidocq.mansart.data.dialect.Where.And;
import io.vidocq.mansart.data.dialect.Where.Between;
import io.vidocq.mansart.data.dialect.Where.Eq;
import io.vidocq.mansart.data.dialect.Where.Exists;
import io.vidocq.mansart.data.dialect.Where.Func;
import io.vidocq.mansart.data.dialect.Where.Gt;
import io.vidocq.mansart.data.dialect.Where.Gte;
import io.vidocq.mansart.data.dialect.Where.In;
import io.vidocq.mansart.data.dialect.Where.IgnoreCase;
import io.vidocq.mansart.data.dialect.Where.IsNotNull;
import io.vidocq.mansart.data.dialect.Where.IsNull;
import io.vidocq.mansart.data.dialect.Where.Like;
import io.vidocq.mansart.data.dialect.Where.Lt;
import io.vidocq.mansart.data.dialect.Where.Lte;
import io.vidocq.mansart.data.dialect.Where.Not;
import io.vidocq.mansart.data.dialect.Where.NotEq;
import io.vidocq.mansart.data.dialect.Where.Or;

import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Walks a {@link Where} AST in render order and binds parameters from a flat {@code args[]} array.
 * The traversal MUST mirror the order the dialect's {@code SqlFragment} produces ({@code ?} marks).
 */
final class WhereBinder {

    private WhereBinder() {}

    static int bind(Dialect dialect, PreparedStatement ps, Where where,
                    Object[] args, int psIdx, int[] argCursor) throws SQLException {
        switch (where) {
            case Eq w        -> psIdx = bindOne(dialect, ps, psIdx, args, argCursor, w.attr().javaType());
            case NotEq w     -> psIdx = bindOne(dialect, ps, psIdx, args, argCursor, w.attr().javaType());
            case Lt w        -> psIdx = bindOne(dialect, ps, psIdx, args, argCursor, w.attr().javaType());
            case Lte w       -> psIdx = bindOne(dialect, ps, psIdx, args, argCursor, w.attr().javaType());
            case Gt w        -> psIdx = bindOne(dialect, ps, psIdx, args, argCursor, w.attr().javaType());
            case Gte w       -> psIdx = bindOne(dialect, ps, psIdx, args, argCursor, w.attr().javaType());
            case Like w      -> psIdx = bindOne(dialect, ps, psIdx, args, argCursor, w.attr().javaType());
            case Between w   -> {
                psIdx = bindOne(dialect, ps, psIdx, args, argCursor, w.attr().javaType());
                psIdx = bindOne(dialect, ps, psIdx, args, argCursor, w.attr().javaType());
            }
            case In w -> {
                for (int i = 0; i < w.arity(); i++) {
                    psIdx = bindOne(dialect, ps, psIdx, args, argCursor, w.attr().javaType());
                }
            }
            case IsNull ignored1 -> { /* no bind */ }
            case IsNotNull ignored2 -> { /* no bind */ }
            case And w -> {
                for (Where child : w.children()) psIdx = bind(dialect, ps, child, args, psIdx, argCursor);
            }
            case Or w -> {
                for (Where child : w.children()) psIdx = bind(dialect, ps, child, args, psIdx, argCursor);
            }
            case Not w -> psIdx = bind(dialect, ps, w.child(), args, psIdx, argCursor);
            // M7-27 — IgnoreCase wraps a single text comparator that consumes exactly one arg.
            // The dialect renders LOWER(col) <op> LOWER(?), so we still bind one parameter.
            case IgnoreCase w -> psIdx = bind(dialect, ps, w.inner(), args, psIdx, argCursor);
            // M8-1 — Func(fn, inner) wraps a comparator with a unary scalar function on the
            // column. UPPER/LOWER/ABS preserve the bound parameter type. LENGTH returns the
            // character count, so the bound parameter must be Integer instead of the column's
            // declared Java type.
            case Func w -> psIdx = bindFunc(dialect, ps, w, args, psIdx, argCursor);
            // M6 — EXISTS predicate with a subquery. The subquery's SqlFragment may have its own
            // bind sites, but they are bound as part of the overall query, not here.
            // EXISTS predicates don't consume additional bind parameters beyond what the subquery defines.
            case Exists w -> { /* subquery parameters are bound separately */ }
            case AlwaysTrue ignored3  -> { /* no bind */ }
            case AlwaysFalse ignored4 -> { /* no bind */ }
        }
        return psIdx;
    }

    private static int bindOne(Dialect dialect, PreparedStatement ps, int psIdx,
                               Object[] args, int[] cursor, Class<?> type) throws SQLException {
        Object value = args[cursor[0]++];
        dialect.bind(ps, psIdx, value, type);
        return psIdx + 1;
    }

    /**
     * M8-1 — bind for {@link Where.Func}. {@code LENGTH} forces Integer typing on bound
     * parameters because the SQL function returns a character count regardless of the column
     * type. Other unary functions ({@code UPPER}/{@code LOWER}/{@code ABS}) preserve the
     * column's Java type.
     */
    private static int bindFunc(Dialect dialect, PreparedStatement ps, Where.Func func,
                                Object[] args, int psIdx, int[] argCursor) throws SQLException {
        if (!"LENGTH".equals(func.fn())) {
            return bind(dialect, ps, func.inner(), args, psIdx, argCursor);
        }
        return bindFuncWithType(dialect, ps, func.inner(), args, psIdx, argCursor, Integer.class);
    }

    private static int bindFuncWithType(Dialect dialect, PreparedStatement ps, Where inner,
                                        Object[] args, int psIdx, int[] argCursor,
                                        Class<?> overrideType) throws SQLException {
        return switch (inner) {
            case Where.Eq ignored      -> bindOne(dialect, ps, psIdx, args, argCursor, overrideType);
            case Where.NotEq ignored   -> bindOne(dialect, ps, psIdx, args, argCursor, overrideType);
            case Where.Lt ignored      -> bindOne(dialect, ps, psIdx, args, argCursor, overrideType);
            case Where.Lte ignored     -> bindOne(dialect, ps, psIdx, args, argCursor, overrideType);
            case Where.Gt ignored      -> bindOne(dialect, ps, psIdx, args, argCursor, overrideType);
            case Where.Gte ignored     -> bindOne(dialect, ps, psIdx, args, argCursor, overrideType);
            case Where.Like ignored    -> bindOne(dialect, ps, psIdx, args, argCursor, overrideType);
            case Where.Between ignored -> bindOne(dialect, ps,
                    bindOne(dialect, ps, psIdx, args, argCursor, overrideType),
                    args, argCursor, overrideType);
            case Where.In w            -> {
                int p = psIdx;
                for (int i = 0; i < w.arity(); i++) {
                    p = bindOne(dialect, ps, p, args, argCursor, overrideType);
                }
                yield p;
            }
            case Where.IsNull ignored    -> psIdx;
            case Where.IsNotNull ignored -> psIdx;
            case Where.Not w             -> bindFuncWithType(dialect, ps, w.child(), args, psIdx, argCursor, overrideType);
            case Where.IgnoreCase w      -> bindFuncWithType(dialect, ps, w.inner(), args, psIdx, argCursor, overrideType);
            case Where.Exists ignored    -> psIdx;
            case Where.AlwaysTrue ignored -> psIdx;
            case Where.AlwaysFalse ignored -> psIdx;
            case Where.And ignored      -> throw new IllegalArgumentException(
                    "Func cannot wrap And, got: " + inner);
            case Where.Or ignored       -> throw new IllegalArgumentException(
                    "Func cannot wrap Or, got: " + inner);
            case Where.Func w           -> bindFuncWithType(dialect, ps, w.inner(), args, psIdx, argCursor, overrideType);
        };
    }
}

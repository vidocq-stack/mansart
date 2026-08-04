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

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

/**
 * Pluggable SQL dialect. One instance per {@link javax.sql.DataSource}; obtained from a
 * {@link DialectFactory} discovered via {@link java.util.ServiceLoader}.
 *
 * <p>Implementations must be stateless and thread-safe — the same {@code Dialect} is used by every
 * virtual thread serving a query against its data source.
 */
public interface Dialect {

    String name();

    SqlFragment select(EntityModel<?> model, Where where, GroupBy groupBy, Where having, OrderBy orderBy, Pagination pagination);

    /**
     * M8-3 — variant of {@link #select} that selects a custom column list (one or many) instead
     * of the entity's full row. Used by JDQL aggregates (passing a single {@code aggExpr}-shaped
     * column entry), single-attribute projections, and multi-attribute projections — all of
     * which may now contain {@link io.vidocq.mansart.data.dialect.attribute.JoinedAttribute}
     * leaves. The dialect handles join collection, aliasing, and SQL rendering uniformly.
     *
     * <p>Each {@link ProjectedColumn} carries either a leaf attribute (rendered as
     * {@code aliasedColumn}) or a raw SQL fragment (e.g. {@code "MAX(\"id\")"}, used for
     * aggregates). The fragment must already be SQL-safe — Mansart never mixes user input here.
     */
    default SqlFragment selectColumns(EntityModel<?> model, java.util.List<ProjectedColumn> columns,
                                      Where where, GroupBy groupBy, Where having, OrderBy orderBy, Pagination pagination) {
        // Default delegates rendering to a uniform helper that mirrors {@link #select}'s plan-aware
        // rendering. Concrete dialects may override for backend-specific behaviour. This default
        // does NOT support joined columns — concrete dialects must override to support M8-3 paths.
        throw new UnsupportedOperationException("selectColumns not implemented by " + name());
    }

    SqlFragment insert(EntityModel<?> model, boolean returningGeneratedKey);
    SqlFragment update(EntityModel<?> model, Where where);
    SqlFragment delete(EntityModel<?> model, Where where);
    SqlFragment merge(EntityModel<?> model);

    /**
     * M8-3 — describes a single SELECT-list entry for {@link #selectColumns}. Either a leaf
     * attribute (column name resolved by the dialect, possibly aliased through a join plan) or
     * a pre-formed SQL expression (used for aggregates like {@code COUNT(*)} or {@code MAX("id")}).
     */
    sealed interface ProjectedColumn {
        record Leaf(Attribute<?, ?> attr) implements ProjectedColumn {}
        record Expr(String sqlFragment) implements ProjectedColumn {}
    }

    /**
     * Targeted UPDATE for a JDQL {@code UPDATE … SET col = ?} statement — only the listed
     * attributes appear in the SET clause. Default delegates to {@link #update(EntityModel, Where)}
     * (which sets every attribute), which is suboptimal — dialects should override.
     */
    default SqlFragment updateSet(EntityModel<?> model, java.util.List<Attribute<?, ?>> attributes, Where where) {
        StringBuilder sb = new StringBuilder("UPDATE \"").append(model.tableName()).append("\" SET ");
        for (int i = 0; i < attributes.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append('"').append(attributes.get(i).columnName()).append("\" = ?");
        }
        if (!(where instanceof Where.AlwaysTrue)) {
            sb.append(" WHERE ");
            // Reuse the existing rendering by going through update().
            String selSql = update(model, where).sql();
            int idx = selSql.indexOf(" WHERE ");
            if (idx >= 0) sb.append(selSql.substring(idx + " WHERE ".length()));
        }
        return new SqlFragment(sb.toString(), java.util.List.of());
    }

    int sqlType(Class<?> javaType);
    void bind(PreparedStatement ps, int idx, Object value, Class<?> javaType) throws SQLException;
    <T> T extract(ResultSet rs, int idx, Class<T> javaType) throws SQLException;

    /** Maps a JDBC {@link SQLException} to a Jakarta-Data-shaped runtime exception. */
    RuntimeException translate(SQLException e);

    // ==================== M5: JPQL Support ====================

    /**
     * Renders a JPQL query to SQL.
     *
     * <p>This method is part of M5 (JPQL/Criteria API support).
     * Default implementation throws UnsupportedOperationException.
     * Dialect implementations should override this to support JPQL rendering.</p>
     *
     * @param jpql the JPQL query string
     * @param parameters the query parameters (named and positional)
     * @param resultType the expected result type
     * @return the SQL fragment for execution
     */
    default SqlFragment renderJpql(String jpql, Map<String, Object> parameters, Class<?> resultType) {
        throw new UnsupportedOperationException("JPQL rendering not implemented by " + name());
    }

    /**
     * Renders a Criteria API query to SQL.
     *
     * <p>This method is part of M5 (JPQL/Criteria API support).
     * Default implementation throws UnsupportedOperationException.
     * Dialect implementations should override this to support Criteria API rendering.</p>
     *
     * @param <T> the result type
     * @param criteriaQuery the CriteriaQuery to render
     * @return the SQL fragment for execution
     */
    default <T> SqlFragment renderCriteria(Object criteriaQuery) {
        throw new UnsupportedOperationException("Criteria API rendering not implemented by " + name());
    }

    /**
     * Returns the ExpressionRenderer for this dialect.
     *
     * <p>ExpressionRenderer is responsible for rendering JPQL expressions (path, literal, function, etc.)
     * to SQL fragments. This is part of M5 support.</p>
     *
     * @return the ExpressionRenderer for this dialect
     */
    default ExpressionRenderer getExpressionRenderer() {
        throw new UnsupportedOperationException("ExpressionRenderer not implemented by " + name());
    }

    // ==================== Expression Renderer ====================

    /**
     * Renderer for JPQL expressions.
     *
     * <p>This interface provides methods to render different types of JPQL expressions
     * to SQL fragments. It is used by the JPQL to SQL visitor.</p>
     *
     * @since 0.3.0-SNAPSHOT (M5)
     */
    interface ExpressionRenderer {
        /**
         * Renders a path expression (e.g., "b.author.name") to SQL.
         *
         * @param path the path expression
         * @return the SQL fragment
         */
        SqlFragment renderPath(String path);

        /**
         * Renders a literal value to SQL.
         *
         * @param value the literal value
         * @param type the type of the literal
         * @return the SQL fragment
         */
        SqlFragment renderLiteral(Object value, Class<?> type);

        /**
         * Renders a function call to SQL.
         *
         * @param functionName the name of the function
         * @param arguments the function arguments
         * @return the SQL fragment
         */
        SqlFragment renderFunction(String functionName, java.util.List<SqlFragment> arguments);

        /**
         * Renders a binary expression (e.g., "a + b", "a = b") to SQL.
         *
         * @param operator the binary operator
         * @param left the left operand
         * @param right the right operand
         * @return the SQL fragment
         */
        SqlFragment renderBinary(String operator, SqlFragment left, SqlFragment right);

        /**
         * Renders a unary expression (e.g., "-a", "NOT a") to SQL.
         *
         * @param operator the unary operator
         * @param operand the operand
         * @return the SQL fragment
         */
        SqlFragment renderUnary(String operator, SqlFragment operand);
    }
}

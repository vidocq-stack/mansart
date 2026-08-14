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
 * It is also made available under the European Union Public License v. 1.2,
 * which is available at
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 *
 * SPDX-License-Identifier: EPL-2.0 OR EUPL-1.2 OR GPL-2.0-or-later
 */
package io.vidocq.mansart.persistence.core.jpql;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import io.vidocq.mansart.data.dialect.Attribute;
import io.vidocq.mansart.data.dialect.Dialect;
import io.vidocq.mansart.data.dialect.EntityModel;
import io.vidocq.mansart.data.dialect.SqlFragment;
import io.vidocq.mansart.data.dialect.Where;

/**
 * Executes JPQL queries by converting them to SQL via Dialect and running them against JDBC.
 *
 * <p>Milestone: M7-13 — supports basic SELECT/FROM/WHERE with single entity projection.
 */
public final class JpqlExecutor {

    private final Dialect dialect;
    private final ConnectionProvider connectionProvider;
    private final Map<Class<?>, EntityModel<?>> entityModels;
    private final Map<String, Class<?>> entityClasses;

    /**
     * Creates a new JPQL executor.
     *
     * @param dialect the SQL dialect
     * @param connectionProvider provider for JDBC connections
     * @param entityModels map of entity classes to their EntityModel
     * @param entityClasses map of entity names to entity classes
     */
    public JpqlExecutor(Dialect dialect, ConnectionProvider connectionProvider,
                        Map<Class<?>, EntityModel<?>> entityModels, Map<String, Class<?>> entityClasses) {
        this.dialect = dialect;
        this.connectionProvider = connectionProvider;
        this.entityModels = entityModels;
        this.entityClasses = entityClasses;
    }

    /**
     * Executes a JPQL query and returns the results.
     *
     * @param query the parsed JPQL query
     * @param parameters named parameters to bind
     * @param positionParameters positional parameters to bind
     * @param maxResults maximum number of results (0 for no limit)
     * @param firstResult offset of first result (0-based)
     * @return list of entity instances
     */
    @SuppressWarnings("unchecked")
    public <T> List<T> execute(JPQLQuery<T> query, Map<String, Object> parameters,
                              List<Object> positionParameters, int maxResults, int firstResult) {
        // Get entity model
        EntityModel<T> entityModel = (EntityModel<T>) getEntityModel(query);
        if (entityModel == null) {
            throw new JPQLException("No EntityModel found for entity: " + query.resultClass().getName());
        }

        // Convert JPQL WHERE clause to Dialect.Where
        JpqlToSqlConverter converter = new JpqlToSqlConverter(entityModels, entityClasses);
        Where where = converter.toWhere(query);

        // Create pagination
        io.vidocq.mansart.data.dialect.Pagination pagination;
        if (maxResults > 0 || firstResult > 0) {
            pagination = new io.vidocq.mansart.data.dialect.Pagination.Offset(firstResult, maxResults);
        } else {
            pagination = io.vidocq.mansart.data.dialect.Pagination.NONE;
        }

        // Use dialect to generate SQL
        SqlFragment sqlFragment = dialect.select(entityModel, where, 
            io.vidocq.mansart.data.dialect.OrderBy.NONE, pagination);

        // Execute query
        try (Connection conn = connectionProvider.getConnection()) {
            java.sql.PreparedStatement ps = conn.prepareStatement(sqlFragment.sql());
            
            // Bind parameters
            bindParameters(ps, sqlFragment, parameters, positionParameters);
            
            // Execute and map results
            java.sql.ResultSet rs = ps.executeQuery();
            return mapResults(rs, entityModel);
        } catch (SQLException e) {
            throw dialect.translate(e);
        }
    }

    private EntityModel<?> getEntityModel(JPQLQuery<?> query) {
        Class<?> entityClass = query.specification().fromClause().entityType();
        return entityModels.get(entityClass);
    }

    private void bindParameters(java.sql.PreparedStatement ps, SqlFragment sqlFragment,
                               Map<String, Object> namedParameters, List<Object> positionParameters) throws SQLException {
        // This is a simplified binding - in a real implementation, we'd need to map
        // the parameter names to the bind sites in the SQL fragment
        int paramIndex = 1;
        
        // First, bind positional parameters
        for (Object value : positionParameters) {
            if (value != null) {
                dialect.bind(ps, paramIndex, value, value.getClass());
            } else {
                ps.setObject(paramIndex, null);
            }
            paramIndex++;
        }
        
        // Then, bind named parameters (order matters - should match SQL fragment order)
        for (Map.Entry<String, Object> entry : namedParameters.entrySet()) {
            Object value = entry.getValue();
            if (value != null) {
                dialect.bind(ps, paramIndex, value, value.getClass());
            } else {
                ps.setObject(paramIndex, null);
            }
            paramIndex++;
        }
    }

    @SuppressWarnings("unchecked")
    private <T> List<T> mapResults(java.sql.ResultSet rs, EntityModel<T> entityModel) throws SQLException {
        List<T> results = new java.util.ArrayList<>();
        java.sql.ResultSetMetaData metaData = rs.getMetaData();
        int columnCount = metaData.getColumnCount();
        
        while (rs.next()) {
            T entity;
            try {
                entity = (T) entityModel.constructor().invoke();
            } catch (Throwable e) {
                throw new SQLException("Failed to instantiate entity: " + entityModel.entityClass().getName(), e);
            }
            
            // Map each column to entity attribute
            for (int i = 1; i <= columnCount; i++) {
                String columnName = metaData.getColumnName(i);
                Object value = rs.getObject(i);
                
                // Find attribute by column name
                for (Attribute<?, ?> attr : entityModel.attributes()) {
                    if (attr.columnName().equals(columnName)) {
                        try {
                            if (value != null) {
                                Object convertedValue = dialect.extract(rs, i, attr.javaType());
                                attr.setter().invoke(entity, convertedValue);
                            }
                        } catch (Throwable e) {
                            // Ignore for now
                        }
                        break;
                    }
                }
            }
            
            results.add(entity);
        }
        
        return results;
    }

    /**
     * Functional interface for providing JDBC connections.
     */
    @FunctionalInterface
    public interface ConnectionProvider {
        Connection getConnection() throws SQLException;
    }
}

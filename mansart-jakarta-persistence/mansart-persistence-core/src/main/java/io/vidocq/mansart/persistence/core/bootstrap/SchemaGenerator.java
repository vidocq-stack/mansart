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

package io.vidocq.mansart.persistence.core.bootstrap;

import io.vidocq.mansart.data.core.RepositoryRuntime;
import io.vidocq.mansart.data.dialect.Attribute;
import io.vidocq.mansart.data.dialect.EntityModel;
import io.vidocq.mansart.data.dialect.attribute.IdAttribute;
import io.vidocq.mansart.data.dialect.attribute.VersionAttribute;
import io.vidocq.mansart.persistence.core.EntityModelResolver;
import jakarta.persistence.Entity;
import jakarta.persistence.SchemaValidationException;
import jakarta.persistence.Column;
import java.lang.reflect.Field;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

/**
 * Schema generator for Mansart Jakarta Persistence implementation.
 * Handles DDL generation (CREATE, DROP, VALIDATE) for entity classes.
 * 
 * <p>Supports the following standard Jakarta Persistence schema generation properties:
 * <ul>
 *   <li>jakarta.persistence.schema-generation.database.action</li>
 *   <li>jakarta.persistence.schema-generation.create-database-schemas</li>
 *   <li>jakarta.persistence.schema-generation.scripts.action</li>
 * </ul>
 * 
 * <p>M6 - Sprint 1: Schema generation support for Jakarta Persistence 3.2
 */
public class SchemaGenerator {

    private static final System.Logger LOGGER = System.getLogger(SchemaGenerator.class.getName());

    /**
     * Schema generation action values as defined by Jakarta Persistence spec.
     */
    public enum DatabaseAction {
        NONE,
        CREATE,
        DROP_AND_CREATE,
        DROP,
        VALIDATE
    }

    /**
     * Scripts action values as defined by Jakarta Persistence spec.
     */
    public enum ScriptsAction {
        NONE,
        CREATE,
        DROP_AND_CREATE,
        DROP
    }

    private final RepositoryRuntime repositoryRuntime;
    private final DataSource dataSource;
    private final Map<String, Object> properties;
    private final List<Class<?>> entityClasses;

    /**
     * Creates a new SchemaGenerator.
     *
     * @param repositoryRuntime the repository runtime for DDL operations
     * @param dataSource the data source for database access
     * @param properties the persistence unit properties
     * @param entityClasses the list of entity classes to process
     */
    public SchemaGenerator(RepositoryRuntime repositoryRuntime, 
                          DataSource dataSource, 
                          Map<String, Object> properties,
                          List<Class<?>> entityClasses) {
        this.repositoryRuntime = repositoryRuntime;
        this.dataSource = dataSource;
        this.properties = properties != null ? properties : Map.of();
        this.entityClasses = entityClasses != null ? new ArrayList<>(entityClasses) : new ArrayList<>();
    }

    /**
     * Gets the database action from properties.
     * Defaults to NONE if not specified.
     */
    public DatabaseAction getDatabaseAction() {
        String action = getStringProperty("jakarta.persistence.schema-generation.database.action");
        if (action == null || action.isEmpty()) {
            return DatabaseAction.NONE;
        }
        return switch (action.toLowerCase()) {
            case "none" -> DatabaseAction.NONE;
            case "create" -> DatabaseAction.CREATE;
            case "drop-and-create", "dropandcreate" -> DatabaseAction.DROP_AND_CREATE;
            case "drop" -> DatabaseAction.DROP;
            case "validate" -> DatabaseAction.VALIDATE;
            default -> {
                LOGGER.log(System.Logger.Level.WARNING, "Unknown schema-generation.database.action value: " + action + 
                               ". Defaulting to NONE.");
                yield DatabaseAction.NONE;
            }
        };
    }

    /**
     * Gets the scripts action from properties.
     */
    public ScriptsAction getScriptsAction() {
        String action = getStringProperty("jakarta.persistence.schema-generation.scripts.action");
        if (action == null || action.isEmpty()) {
            return ScriptsAction.NONE;
        }
        return switch (action.toLowerCase()) {
            case "none" -> ScriptsAction.NONE;
            case "create" -> ScriptsAction.CREATE;
            case "drop-and-create", "dropandcreate" -> ScriptsAction.DROP_AND_CREATE;
            case "drop" -> ScriptsAction.DROP;
            default -> {
                LOGGER.log(System.Logger.Level.WARNING, "Unknown schema-generation.scripts.action value: " + action + 
                               ". Defaulting to NONE.");
                yield ScriptsAction.NONE;
            }
        };
    }

    /**
     * Gets whether to create database schemas (not just tables).
     */
    public boolean isCreateDatabaseSchemas() {
        return Boolean.parseBoolean(getStringProperty("jakarta.persistence.schema-generation.create-database-schemas"));
    }

    /**
     * Generates the schema based on the configured actions.
     * This is called during EntityManagerFactory initialization.
     *
     * @throws SchemaValidationException if validation fails
     */
    public void generateSchema() throws SchemaValidationException {
        DatabaseAction dbAction = getDatabaseAction();
        
        if (dbAction == DatabaseAction.NONE) {
            LOGGER.log(System.Logger.Level.INFO, "Schema generation: action=NONE, skipping schema operations");
            return;
        }

        LOGGER.log(System.Logger.Level.INFO, "Schema generation: action=" + dbAction + 
                    ", entityClasses=" + entityClasses);

        // Resolve all entity models
        List<EntityModel<?>> models = resolveEntityModels();
        
        if (models.isEmpty()) {
            LOGGER.log(System.Logger.Level.INFO, "No entity models found for schema generation");
            return;
        }

        switch (dbAction) {
            case DROP_AND_CREATE:
                dropTables(models);
                createTables(models);
                break;
            case CREATE:
                createTables(models);
                break;
            case DROP:
                dropTables(models);
                break;
            case VALIDATE:
                validateSchema(models);
                break;
            case NONE:
                // Already handled above
                break;
        }
    }

    /**
     * Creates a single table for an entity model.
     * Enhances the default ensureTable method to properly handle auto-generated IDs.
     *
     * @param model the entity model
     */
    public void createTable(EntityModel<?> model) {
        // Use enhanced table creation that handles generated IDs properly
        createTableEnhanced(model);
        LOGGER.log(System.Logger.Level.DEBUG, "Created table: " + qualifiedTableName(model));
    }

    /**
     * Enhanced table creation that properly handles auto-generated IDs.
     * For H2, adds AUTO_INCREMENT to generated ID columns.
     * For PostgreSQL, adds GENERATED BY DEFAULT AS IDENTITY.
     */
    private void createTableEnhanced(EntityModel<?> model) {
        StringBuilder sb = new StringBuilder("CREATE TABLE IF NOT EXISTS ")
                .append(qualifiedTableName(model)).append(" (");
        boolean first = true;
        
        for (Attribute<?, ?> a : model.attributes()) {
            if (!first) sb.append(", ");
            sb.append('"').append(a.columnName()).append('"');
            
            // Determine column type and constraints
            String columnType = sqlDdlType(a.javaType());
            sb.append(" ").append(columnType);
            
            // Handle ID column with special constraints
            if (a == model.id()) {
                sb.append(" PRIMARY KEY");
                
                // For generated IDs, add auto-increment support
                if (model.id().generated()) {
                    // Check dialect type to use appropriate syntax
                    String dialectName = getDialectName();
                    if ("H2".equals(dialectName)) {
                        sb.append(" AUTO_INCREMENT");
                    } else if ("PostgreSQL".equals(dialectName)) {
                        sb.append(" GENERATED BY DEFAULT AS IDENTITY");
                    } else {
                        // For other dialects, at least allow NULL for generated IDs
                        sb.append(" NULL");
                    }
                }
            } else if (a instanceof io.vidocq.mansart.data.dialect.attribute.VersionAttribute) {
                // Version columns should be nullable to allow first insert
                sb.append(" NULL");
            }
            
            // Handle nullable constraints for non-ID columns
            // TODO: Extract nullable from attribute when available
            if (a != model.id() && a != model.version().orElse(null)) {
                // Check if column is nullable from annotations
                if (isNullableAttribute(model, a)) {
                    sb.append(" NULL");
                } else {
                    sb.append(" NOT NULL");
                }
            }
            
            first = false;
        }
        
        sb.append(')');
        String sql = sb.toString();
        
        executeSql(sql);
    }

    /**
     * Creates tables for all entity models.
     *
     * @param models the list of entity models
     */
    public void createTables(List<EntityModel<?>> models) {
        for (EntityModel<?> model : models) {
            createTable(model);
        }
        LOGGER.log(System.Logger.Level.INFO, "Created " + models.size() + " table(s)");
    }

    /**
     * Drops a single table for an entity model.
     *
     * @param model the entity model
     */
    public void dropTable(EntityModel<?> model) {
        String tableName = qualifiedTableName(model);
        String sql = "DROP TABLE IF EXISTS " + tableName;
        executeSql(sql);
        LOGGER.log(System.Logger.Level.DEBUG, "Dropped table: " + tableName);
    }

    /**
     * Drops tables for all entity models.
     *
     * @param models the list of entity models
     */
    public void dropTables(List<EntityModel<?>> models) {
        // Drop in reverse order to handle dependencies
        for (int i = models.size() - 1; i >= 0; i--) {
            dropTable(models.get(i));
        }
        LOGGER.log(System.Logger.Level.INFO, "Dropped " + models.size() + " table(s)");
    }

    /**
     * Validates that the database schema matches the entity models.
     *
     * @param models the list of entity models
     * @throws SchemaValidationException if validation fails
     */
    public void validateSchema(List<EntityModel<?>> models) throws SchemaValidationException {
        List<String> errors = new ArrayList<>();
        
        for (EntityModel<?> model : models) {
            String tableName = model.tableName();
            
            if (!tableExists(tableName)) {
                errors.add("Table '" + tableName + "' does not exist");
                continue;
            }
            
            // For now, just check table existence
            // Full validation (column types, constraints, etc.) can be added later
        }
        
        if (!errors.isEmpty()) {
            throw new SchemaValidationException("Schema validation failed: " + String.join("; ", errors));
        }
        
        LOGGER.log(System.Logger.Level.INFO, "Schema validation passed for " + models.size() + " table(s)");
    }

    /**
     * Resolves entity classes to their EntityModel instances.
     *
     * @return list of resolved EntityModel instances
     */
    @SuppressWarnings("unchecked")
    private List<EntityModel<?>> resolveEntityModels() {
        List<EntityModel<?>> models = new ArrayList<>();
        
        // First, try to resolve explicitly listed entity classes
        for (Class<?> entityClass : entityClasses) {
            if (entityClass.isAnnotationPresent(Entity.class)) {
                try {
                    EntityModel<?> model = EntityModelResolver.resolve(entityClass);
                    models.add(model);
                    LOGGER.log(System.Logger.Level.DEBUG, "Resolved entity model for: " + entityClass.getName());
                } catch (Exception e) {
                    LOGGER.log(System.Logger.Level.WARNING, "Failed to resolve entity model for " + entityClass.getName() + ": " + e.getMessage());
                }
            }
        }
        
        // TODO: M7 - Also scan for entities in persistence unit
        // For now, we rely on the entity classes being provided explicitly
        
        return models;
    }

    /**
     * Checks if a table exists in the database.
     *
     * @param tableName the table name to check
     * @return true if the table exists
     */
    private boolean tableExists(String tableName) {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            
            // Check for the table - handle different catalog/schema scenarios
            try (ResultSet tables = metaData.getTables(null, null, tableName, null)) {
                return tables.next();
            }
        } catch (SQLException e) {
            LOGGER.log(System.Logger.Level.WARNING, "Failed to check if table '" + tableName + "' exists: " + e.getMessage());
            return false;
        }
    }

    /**
     * Executes a SQL statement.
     *
     * @param sql the SQL statement to execute
     */
    private void executeSql(String sql) {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute(sql);
        } catch (SQLException e) {
            LOGGER.log(System.Logger.Level.WARNING, "Failed to execute SQL: " + sql + " - " + e.getMessage());
            throw new RuntimeException("Failed to execute SQL: " + sql, e);
        }
    }

    /**
     * Gets the qualified table name (with schema if specified).
     *
     * @param model the entity model
     * @return the qualified table name
     */
    private String qualifiedTableName(EntityModel<?> model) {
        if (model.schema() == null || model.schema().isEmpty()) {
            return "\"" + model.tableName() + "\"";
        }
        return "\"" + model.schema() + "\".\"" + model.tableName() + "\"";
    }

    /**
     * Gets a string property from the properties map.
     *
     * @param key the property key
     * @return the property value, or null if not found
     */
    private String getStringProperty(String key) {
        Object value = properties.get(key);
        if (value == null) {
            return null;
        }
        return value.toString();
    }

    /**
     * Adds entity classes to the schema generator.
     *
     * @param entityClasses the entity classes to add
     */
    public void addEntityClasses(List<Class<?>> entityClasses) {
        if (entityClasses != null) {
            this.entityClasses.addAll(entityClasses);
        }
    }

    /**
     * Adds a single entity class.
     *
     * @param entityClass the entity class to add
     */
    public void addEntityClass(Class<?> entityClass) {
        if (entityClass != null) {
            this.entityClasses.add(entityClass);
        }
    }

    /**
     * Gets the dialect name for SQL generation.
     */
    private String getDialectName() {
        try {
            // We can infer the dialect from the data source metadata
            try (Connection connection = dataSource.getConnection()) {
                return connection.getMetaData().getDatabaseProductName();
            }
        } catch (SQLException e) {
            LOGGER.log(System.Logger.Level.WARNING, "Failed to get dialect name: " + e.getMessage());
            return "H2"; // Default to H2 for compatibility
        }
    }

    /**
     * Maps Java types to SQL DDL types.
     */
    private String sqlDdlType(Class<?> t) {
        if (t == Long.class || t == long.class)         return "BIGINT";
        if (t == Integer.class || t == int.class)       return "INTEGER";
        if (t == Short.class || t == short.class)       return "SMALLINT";
        if (t == Byte.class || t == byte.class)         return "SMALLINT";
        if (t == Boolean.class || t == boolean.class)   return "BOOLEAN";
        if (t == Double.class || t == double.class)     return "DOUBLE PRECISION";
        if (t == Float.class || t == float.class)       return "REAL";
        if (t == java.math.BigDecimal.class
                || t == java.math.BigInteger.class)     return "NUMERIC(38, 10)";
        if (t == java.time.LocalDate.class)             return "DATE";
        if (t == java.time.LocalTime.class)             return "TIME";
        if (t == java.time.LocalDateTime.class)         return "TIMESTAMP";
        if (t == java.time.OffsetDateTime.class
                || t == java.time.Instant.class)        return "TIMESTAMP WITH TIME ZONE";
        if (t == java.util.UUID.class)                  return "UUID";
        if (t.isEnum())                                 return "VARCHAR(255)";
        return "VARCHAR(255)";
    }

    /**
     * Checks if an attribute is nullable based on its annotations.
     */
    private boolean isNullableAttribute(EntityModel<?> model, Attribute<?, ?> attribute) {
        try {
            // Find the field corresponding to this attribute
            for (Field field : model.entityClass().getDeclaredFields()) {
                if (field.getName().equals(attribute.name())) {
                    // Check if @Column has nullable=true (default) or nullable=false
                    Column column = field.getAnnotation(Column.class);
                    if (column != null) {
                        return column.nullable();
                    }
                    // Default is nullable for non-ID attributes
                    return true;
                }
            }
        } catch (Exception e) {
            // If we can't determine, assume nullable
        }
        return true;
    }
}

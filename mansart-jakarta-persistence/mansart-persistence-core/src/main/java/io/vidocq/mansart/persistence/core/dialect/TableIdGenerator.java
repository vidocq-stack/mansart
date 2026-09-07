package io.vidocq.mansart.persistence.core.dialect;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Generates IDs using a generator table (TABLE strategy).
 */
public final class TableIdGenerator implements IdGenerator {

    private final String tableName;
    private final String pkColumnName;
    private final String valueColumnName;
    private final String pkColumnValue;

    /**
     * Creates a new generator for the given table configuration.
     *
     * @param tableName the generator table name
     * @param pkColumnName the primary key column name in the generator table
     * @param valueColumnName the value column name in the generator table
     * @param pkColumnValue the value to use for the primary key column
     */
    public TableIdGenerator(String tableName, String pkColumnName, String valueColumnName, String pkColumnValue) {
        this.tableName = tableName;
        this.pkColumnName = pkColumnName;
        this.valueColumnName = valueColumnName;
        this.pkColumnValue = pkColumnValue;
    }

    @Override
    public Object generate(Connection conn, Class<?> idType) throws SQLException {
        // Standard TABLE generator pattern:
        // 1. Read current value
        // 2. Update to value + 1
        // 3. Return value + 1
        
        // Read current value
        long current;
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT \"" + valueColumnName + "\" FROM \"" + tableName + "\" WHERE \"" + pkColumnName + "\" = ?")) {
            ps.setString(1, pkColumnValue);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    // Auto-create the generator row if it does not exist
                    try (PreparedStatement is = conn.prepareStatement(
                            "INSERT INTO \"" + tableName + "\" (\"" + pkColumnName + "\", \"" + valueColumnName + "\") VALUES (?, 0)")) {
                        is.setString(1, pkColumnValue);
                        is.executeUpdate();
                    }
                    current = 0;
                } else {
                    current = rs.getLong(1);
                }
            }
        }

        long next = current + 1;

        // Update to next value
        try (PreparedStatement us = conn.prepareStatement(
                "UPDATE \"" + tableName + "\" SET \"" + valueColumnName + "\" = ? WHERE \"" + pkColumnName + "\" = ?")) {
            us.setLong(1, next);
            us.setString(2, pkColumnValue);
            int updated = us.executeUpdate();
            if (updated != 1) {
                throw new SQLException("Failed to update generator table for: " + pkColumnValue);
            }
        }

        return castToType(next, idType);
    }

    private Object castToType(long value, Class<?> idType) {
        if (idType == Long.class || idType == long.class) {
            return value;
        } else if (idType == Integer.class || idType == int.class) {
            return (int) value;
        } else if (idType == Short.class || idType == short.class) {
            return (short) value;
        } else if (idType == Byte.class || idType == byte.class) {
            return (byte) value;
        } else {
            // Fallback: return as Long
            return value;
        }
    }
}

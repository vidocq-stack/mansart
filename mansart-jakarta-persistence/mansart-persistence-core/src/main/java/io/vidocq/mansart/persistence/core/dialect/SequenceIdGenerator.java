package io.vidocq.mansart.persistence.core.dialect;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Generates IDs using a database sequence (SEQUENCE strategy).
 */
public final class SequenceIdGenerator implements IdGenerator {

    private final String sequenceName;

    /**
     * Creates a new generator for the given sequence name.
     *
     * @param sequenceName the name of the database sequence
     */
    public SequenceIdGenerator(String sequenceName) {
        this.sequenceName = sequenceName;
    }

    @Override
    public Object generate(Connection conn, Class<?> idType) throws SQLException {
        // H2: SELECT NEXT VALUE FOR <sequenceName>
        String sql = "SELECT NEXT VALUE FOR " + sequenceName;
        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            if (!rs.next()) {
                throw new SQLException("Sequence did not return a value: " + sequenceName);
            }
            long value = rs.getLong(1);
            return castToType(value, idType);
        }
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

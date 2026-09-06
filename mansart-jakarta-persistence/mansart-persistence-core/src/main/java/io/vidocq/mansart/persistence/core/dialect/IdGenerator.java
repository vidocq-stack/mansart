package io.vidocq.mansart.persistence.core.dialect;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Allocates an ID value before INSERT for pre-insert strategies (SEQUENCE, TABLE).
 */
@FunctionalInterface
public interface IdGenerator {
    /**
     * Generates a new ID value.
     *
     * @param conn the database connection
     * @param idType the expected ID Java type
     * @return the generated ID value
     * @throws SQLException if a database error occurs
     */
    Object generate(Connection conn, Class<?> idType) throws SQLException;
}

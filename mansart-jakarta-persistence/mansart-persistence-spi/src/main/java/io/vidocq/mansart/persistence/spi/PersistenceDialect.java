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
 * It is also made available under the European Union Public Licence v. 1.2,
 * which is available at
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 *
 * SPDX-License-Identifier: EPL-2.0 OR EUPL-1.2 OR GPL-2.0-or-later
 */
/**
 * SPI interface for JPA-specific dialect extensions.
 * Extends the Mansart Data Dialect interface with JPA-specific functionality.
 */
package io.vidocq.mansart.persistence.spi;

import io.vidocq.mansart.data.dialect.Dialect;

/**
 * Extension of the Mansart Data Dialect interface with JPA-specific functionality.
 * Implementations provide database-specific SQL generation for JPA operations.
 */
public interface PersistenceDialect extends Dialect {

    /**
     * Gets the SQL string for retrieving the next value from a sequence.
     *
     * @param sequenceName the name of the sequence
     * @return the SQL string to get the next sequence value
     */
    String getSequenceNextValString(String sequenceName);

    /**
     * Gets the SQL string for retrieving the current value from a sequence.
     *
     * @param sequenceName the name of the sequence
     * @return the SQL string to get the current sequence value
     */
    String getSequenceCurrentValString(String sequenceName);

    /**
     * Gets the SQL string for listing all sequences in the database.
     *
     * @return the SQL string to query all sequences
     */
    String getQuerySequencesString();

    /**
     * Gets the SQL string for creating a sequence.
     *
     * @param sequenceName the name of the sequence
     * @param incrementSize the increment size
     * @param startValue the starting value
     * @return the SQL string to create a sequence
     */
    String getCreateSequenceString(String sequenceName, int incrementSize, long startValue);

    /**
     * Gets the SQL string for dropping a sequence.
     *
     * @param sequenceName the name of the sequence
     * @return the SQL string to drop a sequence
     */
    String getDropSequenceString(String sequenceName);

    /**
     * Gets the SQL string for getting the next value from a table generator.
     *
     * @param tableName the name of the table
     * @param segmentColumnName the name of the segment column
     * @param segmentValue the segment value
     * @param valueColumnName the name of the value column
     * @return the SQL string to get the next table generator value
     */
    String getTableGeneratorNextValString(String tableName, String segmentColumnName, 
                                         String segmentValue, String valueColumnName);

    /**
     * Gets the SQL string for creating a table generator table.
     *
     * @param tableName the name of the table
     * @param segmentColumnName the name of the segment column
     * @param valueColumnName the name of the value column
     * @return the SQL string to create the table generator table
     */
    String getCreateTableGeneratorString(String tableName, String segmentColumnName, String valueColumnName);

    /**
     * Checks if the dialect supports sequences.
     *
     * @return true if sequences are supported, false otherwise
     */
    boolean supportsSequences();

    /**
     * Checks if the dialect supports table generators.
     *
     * @return true if table generators are supported, false otherwise
     */
    boolean supportsTableGenerators();

    /**
     * Gets the identity column string for auto-generated keys.
     *
     * @param columnName the name of the column
     * @return the identity column string
     */
    String getIdentityColumnString(String columnName);

    /**
     * Gets the identity insert string for auto-generated keys.
     *
     * @param columnName the name of the column
     * @return the identity insert string
     */
    String getIdentityInsertString(String columnName);
}

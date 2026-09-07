/*
 * SPDX-License-Identifier: EPL-2.0 OR EUPL-1.2 OR GPL-2.0-or-later
 */
package io.vidocq.mansart.persistence.tests;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import io.vidocq.mansart.persistence.tests.model._ColumnTestEntity;
import io.vidocq.mansart.persistence.tests.model._TableTestEntity;

/**
 * TDD test for M5-JP-32: verifies @Table name, schema and catalog are captured by APT.
 */
class TableMappingTest {

    @Test
    void tableWithNameSchemaCatalog() {
        _TableTestEntity model = new _TableTestEntity();
        assertThat(model.getTableName()).isEqualTo("custom_entities");
        assertThat(model.getSchema()).isEqualTo("app");
        assertThat(model.getCatalog()).isEqualTo("mydb");
    }

    @Test
    void tableDefaultsWhenNoTableAnnotation() {
        _ColumnTestEntity model = new _ColumnTestEntity();
        // ColumnTestEntity has no @Table annotation, so:
        // - table name defaults to the entity name snake_cased and pluralized
        // - schema defaults to empty string
        // - catalog defaults to empty string
        assertThat(model.getTableName()).isEqualTo("column_test_entities");
        assertThat(model.getSchema()).isEmpty();
        assertThat(model.getCatalog()).isEmpty();
    }
}

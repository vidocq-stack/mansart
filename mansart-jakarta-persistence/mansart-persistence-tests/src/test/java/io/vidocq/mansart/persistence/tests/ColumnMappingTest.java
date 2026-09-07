/*
 * SPDX-License-Identifier: EPL-2.0 OR EUPL-1.2 OR GPL-2.0-or-later
 */
package io.vidocq.mansart.persistence.tests;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import io.vidocq.mansart.persistence.tests.model._ColumnTestEntity;
import io.vidocq.mansart.persistence.spi.Attribute;

/**
 * TDD test for M5-JP-31: verifies @Column properties are captured by APT.
 */
class ColumnMappingTest {

    @Test
    void columnNameAndLength() {
        _ColumnTestEntity model = new _ColumnTestEntity();
        Attribute<?, ?> nameAttr = model.getAttribute("name");
        assertThat(nameAttr.getColumnName()).isEqualTo("full_name");
        assertThat(nameAttr.getLength()).isEqualTo(100);
        assertThat(nameAttr.isNullable()).isFalse();
        assertThat(nameAttr.isInsertable()).isTrue();
        assertThat(nameAttr.isUpdatable()).isTrue();
    }

    @Test
    void columnInsertableFalse() {
        _ColumnTestEntity model = new _ColumnTestEntity();
        Attribute<?, ?> descAttr = model.getAttribute("description");
        assertThat(descAttr.getColumnName()).isEqualTo("description");
        assertThat(descAttr.getLength()).isEqualTo(500);
        assertThat(descAttr.isInsertable()).isFalse();
        assertThat(descAttr.isUpdatable()).isTrue();
    }

    @Test
    void columnPrecisionScaleUpdatableFalse() {
        _ColumnTestEntity model = new _ColumnTestEntity();
        Attribute<?, ?> amountAttr = model.getAttribute("amount");
        assertThat(amountAttr.getColumnName()).isEqualTo("amount");
        assertThat(amountAttr.getPrecision()).isEqualTo(12);
        assertThat(amountAttr.getScale()).isEqualTo(2);
        assertThat(amountAttr.isInsertable()).isTrue();
        assertThat(amountAttr.isUpdatable()).isFalse();
    }

    @Test
    void columnDefinition() {
        _ColumnTestEntity model = new _ColumnTestEntity();
        Attribute<?, ?> codeAttr = model.getAttribute("code");
        assertThat(codeAttr.getColumnDefinition()).isEqualTo("VARCHAR(20)");
    }

    @Test
    void columnLengthOnly() {
        _ColumnTestEntity model = new _ColumnTestEntity();
        Attribute<?, ?> labelAttr = model.getAttribute("label");
        assertThat(labelAttr.getLength()).isEqualTo(50);
        assertThat(labelAttr.isInsertable()).isTrue();
        assertThat(labelAttr.isUpdatable()).isTrue();
    }

    @Test
    void noColumnAnnotationDefaults() {
        _ColumnTestEntity model = new _ColumnTestEntity();
        Attribute<?, ?> plainAttr = model.getAttribute("plain");
        assertThat(plainAttr.getLength()).isEqualTo(-1);
        assertThat(plainAttr.isInsertable()).isTrue();
        assertThat(plainAttr.isUpdatable()).isTrue();
        assertThat(plainAttr.getColumnDefinition()).isEmpty();
    }
}
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
package io.vidocq.mansart.persistence.processor.metamodel;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for AttributeModel.
 */
class AttributeModelTest {

    @Test
    void shouldCreateAttributeModelWithAllProperties() {
        AttributeModel model = new AttributeModel(
            "name", 
            "java.lang.String", 
            "java.lang.String",
            false,  // not plural
            false,  // not association
            true,   // basic
            "FIELD"
        );

        assertThat(model.getName()).isEqualTo("name");
        assertThat(model.getJavaType()).isEqualTo("java.lang.String");
        assertThat(model.getPersistentAttributeType()).isEqualTo("java.lang.String");
        assertThat(model.isPlural()).isFalse();
        assertThat(model.isAssociation()).isFalse();
        assertThat(model.isBasic()).isTrue();
        assertThat(model.getDeclarationType()).isEqualTo("FIELD");
    }

    @Test
    void shouldCreatePluralAssociationAttributeModel() {
        AttributeModel model = new AttributeModel(
            "children",
            "java.util.List<io.vidocq.mansart.persistence.processor.TestEntity>",
            "java.util.List<io.vidocq.mansart.persistence.processor.TestEntity>",
            true,   // plural
            true,   // association
            false,  // not basic
            "FIELD"
        );

        assertThat(model.isPlural()).isTrue();
        assertThat(model.isAssociation()).isTrue();
        assertThat(model.isBasic()).isFalse();
    }

    @Test
    void shouldHaveCorrectEqualsAndHashCode() {
        AttributeModel model1 = new AttributeModel("id", "java.lang.Long", "java.lang.Long", false, false, true, "FIELD");
        AttributeModel model2 = new AttributeModel("id", "java.lang.Long", "java.lang.Long", false, false, true, "FIELD");
        AttributeModel model3 = new AttributeModel("name", "java.lang.String", "java.lang.String", false, false, true, "FIELD");

        assertThat(model1).isEqualTo(model2);
        assertThat(model1.hashCode()).isEqualTo(model2.hashCode());
        assertThat(model1).isNotEqualTo(model3);
    }

    @Test
    void shouldHaveMeaningfulToString() {
        AttributeModel model = new AttributeModel("age", "java.lang.Integer", "java.lang.Integer", false, false, true, "FIELD");
        String str = model.toString();
        
        assertThat(str).contains("name='age'");
        assertThat(str).contains("java.lang.Integer");
        assertThat(str).contains("plural=false");
        assertThat(str).contains("association=false");
    }

    @Test
    void shouldThrowNullPointerExceptionForNullName() {
        assertThatThrownBy(() -> new AttributeModel(null, "java.lang.String", "java.lang.String", false, false, true, "FIELD"))
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    void shouldThrowNullPointerExceptionForNullJavaType() {
        assertThatThrownBy(() -> new AttributeModel("name", null, "java.lang.String", false, false, true, "FIELD"))
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    void shouldThrowNullPointerExceptionForNullPersistentAttributeType() {
        assertThatThrownBy(() -> new AttributeModel("name", "java.lang.String", null, false, false, true, "FIELD"))
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    void shouldThrowNullPointerExceptionForNullDeclarationType() {
        assertThatThrownBy(() -> new AttributeModel("name", "java.lang.String", "java.lang.String", false, false, true, null))
            .isInstanceOf(NullPointerException.class);
    }
}

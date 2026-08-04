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

package io.vidocq.mansart.persistence.processor.metamodel;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Basic unit tests for MetamodelTypeResolver that don't require compilation context.
 * <p>
 * These tests verify the string-based extraction methods.
 */
class MetamodelTypeResolverBasicTest {

    @Test
    void shouldExtractCollectionTypeFromParameterizedType() {
        String collectionType = MetamodelTypeResolver.extractCollectionType("java.util.List<java.lang.String>");
        assertThat(collectionType).isEqualTo("java.util.List");
    }

    @Test
    void shouldExtractCollectionTypeFromRawType() {
        String collectionType = MetamodelTypeResolver.extractCollectionType("java.util.List");
        assertThat(collectionType).isEqualTo("java.util.List");
    }

    @Test
    void shouldExtractElementTypeFromCollection() {
        String elementType = MetamodelTypeResolver.extractElementType("java.util.List<java.lang.String>");
        assertThat(elementType).isEqualTo("java.lang.String");
    }

    @Test
    void shouldExtractElementTypeFromMap() {
        // For Map, we want the value type (second parameter)
        String elementType = MetamodelTypeResolver.extractElementType("java.util.Map<java.lang.String, java.lang.Integer>");
        assertThat(elementType).isEqualTo("java.lang.Integer");
    }

    @Test
    void shouldExtractElementTypeFromArray() {
        String elementType = MetamodelTypeResolver.extractElementType("java.lang.String[]");
        assertThat(elementType).isEqualTo("java.lang.String");
    }

    @Test
    void shouldReturnObjectForNonCollectionType() {
        String elementType = MetamodelTypeResolver.extractElementType("java.lang.Object");
        assertThat(elementType).isEqualTo("java.lang.Object");
    }

    @Test
    void shouldReturnObjectForNonGenericCollection() {
        String elementType = MetamodelTypeResolver.extractElementType("java.util.List");
        assertThat(elementType).isEqualTo("java.lang.Object");
    }
}

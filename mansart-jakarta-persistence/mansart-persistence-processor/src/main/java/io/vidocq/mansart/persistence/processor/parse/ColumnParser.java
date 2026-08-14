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
package io.vidocq.mansart.persistence.processor.parse;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;

import java.util.Map;

/**
 * Parser for {@code @jakarta.persistence.Column} annotation.
 *
 * <p>This class extracts column mapping metadata from annotated fields or properties.
 */
public final class ColumnParser {

    private static final String COLUMN_ANNOTATION = "jakarta.persistence.Column";

    /**
     * Parses the {@code @Column} annotation from a field/property.
     *
     * @param element the element annotated with {@code @Column}
     * @return a ColumnInfo containing column name and nullability, or null if not annotated
     */
    public ColumnInfo parse(Element element) {
        AnnotationMirror columnMirror = getAnnotationMirror(element, COLUMN_ANNOTATION);
        if (columnMirror == null) {
            return null;
        }

        // Extract column name
        String name = null;
        boolean isNullable = true;

        Map<String, AnnotationValue> values = getAnnotationValues(columnMirror);
        
        AnnotationValue nameValue = values.get("name");
        if (nameValue != null) {
            name = nameValue.getValue().toString();
        }

        AnnotationValue nullableValue = values.get("nullable");
        if (nullableValue != null) {
            isNullable = Boolean.TRUE.equals(nullableValue.getValue());
        }

        return new ColumnInfo(name, isNullable);
    }

    private AnnotationMirror getAnnotationMirror(Element element, String annotationFqn) {
        for (AnnotationMirror mirror : element.getAnnotationMirrors()) {
            if (((javax.lang.model.element.TypeElement) mirror.getAnnotationType().asElement())
                    .getQualifiedName().toString().equals(annotationFqn)) {
                return mirror;
            }
        }
        return null;
    }

    private Map<String, AnnotationValue> getAnnotationValues(AnnotationMirror mirror) {
        Map<String, AnnotationValue> values = new java.util.HashMap<>();
        ExecutableElement[] keys = mirror.getElementValues().keySet().toArray(new ExecutableElement[0]);
        for (ExecutableElement key : keys) {
            values.put(key.getSimpleName().toString(), mirror.getElementValues().get(key));
        }
        return values;
    }

    /**
     * Records for column information extracted from {@code @Column} annotation.
     */
    public record ColumnInfo(String name, boolean isNullable) {
    }
}

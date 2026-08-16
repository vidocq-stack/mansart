/*
 * Copyright (c) 2026 Yann Blazart, Antoine Sabot-Durand and the Vidocq contributors
 *
 * SPDX-License-Identifier: EPL-2.0 OR EUPL-1.2 OR GPL-2.0-or-later
 */
package io.vidocq.mansart.persistence.processor.parse;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import java.util.Map;
import java.util.Set;

/**
 * Parser for {@code @jakarta.persistence.Basic} annotation.
 *
 * <p>This class extracts fetch type and optional flag from {@code @Basic} annotations.
 */
public final class BasicParser {

    private static final String BASIC_ANNOTATION = "jakarta.persistence.Basic";

    /**
     * Information extracted from {@code @Basic} annotation.
     */
    public record BasicInfo(
            io.vidocq.mansart.persistence.spi.AttributeMetadata.FetchType fetchType,
            boolean optional
    ) {
    }

    /**
     * Parses the {@code @Basic} annotation from an element.
     *
     * @param element the element annotated with {@code @Basic}
     * @return a BasicInfo containing fetch type and optional flag, or null if not annotated
     */
    public BasicInfo parse(Element element) {
        AnnotationMirror basicMirror = getAnnotationMirror(element, BASIC_ANNOTATION);
        if (basicMirror == null) {
            return null;
        }

        Map<String, AnnotationValue> values = getAnnotationValues(basicMirror);

        // Extract fetch type
        io.vidocq.mansart.persistence.spi.AttributeMetadata.FetchType fetchType = 
            extractFetchType(values);

        // Extract optional flag
        boolean optional = extractOptional(values);

        return new BasicInfo(fetchType, optional);
    }

    private io.vidocq.mansart.persistence.spi.AttributeMetadata.FetchType extractFetchType(
            Map<String, AnnotationValue> values) {
        AnnotationValue fetchValue = values.get("fetch");
        if (fetchValue != null) {
            Object fetch = fetchValue.getValue();
            if (fetch instanceof TypeElement) {
                String fetchTypeName = ((TypeElement) fetch).getSimpleName().toString();
                return "LAZY".equals(fetchTypeName) ? 
                    io.vidocq.mansart.persistence.spi.AttributeMetadata.FetchType.LAZY : 
                    io.vidocq.mansart.persistence.spi.AttributeMetadata.FetchType.EAGER;
            }
        }
        // Default is EAGER for @Basic
        return io.vidocq.mansart.persistence.spi.AttributeMetadata.FetchType.EAGER;
    }

    private boolean extractOptional(Map<String, AnnotationValue> values) {
        AnnotationValue optionalValue = values.get("optional");
        if (optionalValue != null) {
            Object optional = optionalValue.getValue();
            if (optional instanceof Boolean) {
                return (Boolean) optional;
            }
        }
        // Default is true for @Basic
        return true;
    }

    // Helper methods
    private static AnnotationMirror getAnnotationMirror(Element element, String annotationFqn) {
        for (AnnotationMirror mirror : element.getAnnotationMirrors()) {
            if (((TypeElement) mirror.getAnnotationType().asElement())
                    .getQualifiedName().toString().equals(annotationFqn)) {
                return mirror;
            }
        }
        return null;
    }

    private static Map<String, AnnotationValue> getAnnotationValues(AnnotationMirror mirror) {
        Map<String, AnnotationValue> values = new java.util.HashMap<>();
        Set<? extends javax.lang.model.element.ExecutableElement> keys = 
            mirror.getElementValues().keySet();
        for (javax.lang.model.element.ExecutableElement key : keys) {
            values.put(key.getSimpleName().toString(), mirror.getElementValues().get(key));
        }
        return values;
    }
}

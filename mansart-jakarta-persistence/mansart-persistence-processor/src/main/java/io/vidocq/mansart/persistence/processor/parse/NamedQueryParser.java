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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Parser for {@code @jakarta.persistence.NamedQuery} and {@code @NamedQueries} annotations.
 *
 * <p>This class extracts named query metadata from entity classes.
 */
public final class NamedQueryParser {

    private static final String NAMED_QUERY_ANNOTATION = "jakarta.persistence.NamedQuery";
    private static final String NAMED_QUERIES_ANNOTATION = "jakarta.persistence.NamedQueries";

    /**
     * Information about a single named query.
     */
    public record NamedQueryInfo(
            String name,
            String query,
            Class<?> resultClass,
            String resultSetMapping,
            boolean isNative
    ) {
    }

    /**
     * Parses named queries from an entity type element.
     *
     * @param type the entity type element
     * @return list of NamedQueryInfo for all named queries on this entity
     */
    public List<NamedQueryInfo> parse(TypeElement type) {
        List<NamedQueryInfo> queries = new ArrayList<>();

        // Check for @NamedQuery annotations
        List<AnnotationMirror> namedQueryMirrors = getAnnotationMirrors(type, NAMED_QUERY_ANNOTATION);
        for (AnnotationMirror mirror : namedQueryMirrors) {
            NamedQueryInfo info = parseNamedQuery(mirror);
            if (info != null) {
                queries.add(info);
            }
        }

        // Check for @NamedQueries annotation (container for multiple @NamedQuery)
        List<AnnotationMirror> namedQueriesMirrors = getAnnotationMirrors(type, NAMED_QUERIES_ANNOTATION);
        for (AnnotationMirror mirror : namedQueriesMirrors) {
            List<NamedQueryInfo> batch = parseNamedQueries(mirror);
            queries.addAll(batch);
        }

        return queries;
    }

    private NamedQueryInfo parseNamedQuery(AnnotationMirror mirror) {
        Map<String, AnnotationValue> values = getAnnotationValues(mirror);

        String name = extractStringValue(values, "name");
        String query = extractStringValue(values, "query");
        
        if (name == null || query == null) {
            return null;
        }

        // Extract resultClass
        Class<?> resultClass = extractResultClass(values, "resultClass");
        
        // Extract resultSetMapping
        String resultSetMapping = extractStringValue(values, "resultSetMapping");
        
        // Extract hints (not implemented in Phase 1)
        // Extract isNative (default false)
        boolean isNative = false;
        AnnotationValue isNativeValue = values.get("isNative");
        if (isNativeValue != null) {
            Object value = isNativeValue.getValue();
            if (value instanceof Boolean) {
                isNative = (Boolean) value;
            }
        }

        return new NamedQueryInfo(name, query, resultClass, resultSetMapping, isNative);
    }

    private List<NamedQueryInfo> parseNamedQueries(AnnotationMirror mirror) {
        List<NamedQueryInfo> queries = new ArrayList<>();
        
        Map<String, AnnotationValue> values = getAnnotationValues(mirror);
        
        // The @NamedQueries annotation has a "value" attribute which is an array of @NamedQuery
        AnnotationValue queriesValue = values.get("value");
        if (queriesValue != null) {
            Object value = queriesValue.getValue();
            if (value instanceof List) {
                @SuppressWarnings("unchecked")
                List<AnnotationValue> queryList = (List<AnnotationValue>) value;
                for (AnnotationValue av : queryList) {
                    Object q = av.getValue();
                    if (q instanceof AnnotationMirror) {
                        NamedQueryInfo info = parseNamedQuery((AnnotationMirror) q);
                        if (info != null) {
                            queries.add(info);
                        }
                    }
                }
            }
        }

        return queries;
    }

    private String extractStringValue(Map<String, AnnotationValue> values, String key) {
        AnnotationValue value = values.get(key);
        if (value != null) {
            Object val = value.getValue();
            if (val != null) {
                return val.toString();
            }
        }
        return null;
    }

    private Class<?> extractResultClass(Map<String, AnnotationValue> values, String key) {
        AnnotationValue value = values.get(key);
        if (value != null) {
            Object val = value.getValue();
            if (val instanceof TypeElement) {
                TypeElement typeElement = (TypeElement) val;
                try {
                    // Try to load the class
                    return Class.forName(typeElement.getQualifiedName().toString());
                } catch (ClassNotFoundException e) {
                    // Return null, will be handled at runtime
                    return null;
                }
            } else if (val instanceof Class) {
                return (Class<?>) val;
            }
        }
        return null;
    }

    private List<AnnotationMirror> getAnnotationMirrors(Element element, String annotationFqn) {
        List<AnnotationMirror> result = new ArrayList<>();
        for (AnnotationMirror mirror : element.getAnnotationMirrors()) {
            TypeElement annotationType = (TypeElement) mirror.getAnnotationType().asElement();
            if (annotationType.getQualifiedName().toString().equals(annotationFqn)) {
                result.add(mirror);
            }
        }
        return result;
    }

    private static Map<String, AnnotationValue> getAnnotationValues(AnnotationMirror mirror) {
        Map<String, AnnotationValue> values = new java.util.HashMap<>();
        Set<? extends javax.lang.model.element.ExecutableElement> keys = mirror.getElementValues().keySet();
        for (javax.lang.model.element.ExecutableElement key : keys) {
            values.put(key.getSimpleName().toString(), mirror.getElementValues().get(key));
        }
        return values;
    }
}

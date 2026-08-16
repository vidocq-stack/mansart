/*
 * Copyright (c) 2026 Yann Blazart, Antoine Sabot-Durand and the Vidocq contributors
 *
 * SPDX-License-Identifier: EPL-2.0 OR EUPL-1.2 OR GPL-2.0-or-later
 */
package io.vidocq.mansart.persistence.processor.parse;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Parser for {@code @jakarta.persistence.OneToMany} annotation.
 *
 * <p>This class extracts relationship metadata for OneToMany associations.
 */
public final class OneToManyParser {

    private static final String ONE_TO_MANY_ANNOTATION = "jakarta.persistence.OneToMany";
    private static final String JOIN_COLUMN_ANNOTATION = "jakarta.persistence.JoinColumn";
    private static final String CASCADE_ANNOTATION = "jakarta.persistence.CascadeType";
    private static final String MAP_KEY_JOIN_COLUMN_ANNOTATION = "jakarta.persistence.MapKeyJoinColumn";

    /**
     * Parses the {@code @OneToMany} annotation from a field/property.
     *
     * @param element the element annotated with {@code @OneToMany}
     * @param types the type utilities
     * @return a OneToManyInfo containing relationship metadata, or null if not annotated
     */
    public OneToManyInfo parse(Element element, javax.lang.model.util.Types types) {
        AnnotationMirror oneToManyMirror = getAnnotationMirror(element, ONE_TO_MANY_ANNOTATION);
        if (oneToManyMirror == null) {
            return null;
        }

        Map<String, AnnotationValue> values = getAnnotationValues(oneToManyMirror);

        // Extract target entity class
        TypeMirror targetEntityType = extractTargetEntityType(element);
        String targetEntityName = targetEntityType != null ? 
            targetEntityType.toString() : null;

        // Extract mappedBy attribute
        String mappedBy = extractMappedBy(values);

        // Extract fetch type
        io.vidocq.mansart.persistence.spi.RelationshipMetadata.FetchType fetchType = 
            extractFetchType(values);

        // Extract cascade types
        List<io.vidocq.mansart.persistence.spi.RelationshipMetadata.CascadeType> cascadeTypes = 
            extractCascadeTypes(element);

        // Extract orphan removal
        boolean orphanRemoval = extractOrphanRemoval(values);

        // Extract JoinColumn info (for cases where OneToMany uses join table)
        JoinColumnInfo joinColumnInfo = extractJoinColumn(element);

        return new OneToManyInfo(
            targetEntityName,
            mappedBy,
            fetchType,
            cascadeTypes.toArray(new io.vidocq.mansart.persistence.spi.RelationshipMetadata.CascadeType[0]),
            orphanRemoval,
            joinColumnInfo
        );
    }

    private TypeMirror extractTargetEntityType(Element element) {
        // For fields, get the type directly
        if (element instanceof javax.lang.model.element.VariableElement) {
            return element.asType();
        }
        // For methods (getters), get the return type
        if (element instanceof ExecutableElement) {
            return ((ExecutableElement) element).getReturnType();
        }
        return null;
    }

    private String extractMappedBy(Map<String, AnnotationValue> values) {
        AnnotationValue mappedByValue = values.get("mappedBy");
        if (mappedByValue != null) {
            return mappedByValue.getValue().toString();
        }
        return null;
    }

    private io.vidocq.mansart.persistence.spi.RelationshipMetadata.FetchType extractFetchType(
            Map<String, AnnotationValue> values) {
        AnnotationValue fetchValue = values.get("fetch");
        if (fetchValue != null) {
            Object fetch = fetchValue.getValue();
            if (fetch instanceof TypeElement) {
                String fetchTypeName = ((TypeElement) fetch).getSimpleName().toString();
                return "EAGER".equals(fetchTypeName) ? 
                    io.vidocq.mansart.persistence.spi.RelationshipMetadata.FetchType.EAGER : 
                    io.vidocq.mansart.persistence.spi.RelationshipMetadata.FetchType.LAZY;
            }
        }
        // Default is LAZY for OneToMany
        return io.vidocq.mansart.persistence.spi.RelationshipMetadata.FetchType.LAZY;
    }

    private List<io.vidocq.mansart.persistence.spi.RelationshipMetadata.CascadeType> extractCascadeTypes(
            Element element) {
        AnnotationMirror oneToManyMirror = getAnnotationMirror(element, ONE_TO_MANY_ANNOTATION);
        if (oneToManyMirror != null) {
            Map<String, AnnotationValue> values = getAnnotationValues(oneToManyMirror);
            AnnotationValue cascadeValue = values.get("cascade");

            if (cascadeValue != null) {
                Object cascade = cascadeValue.getValue();
                if (cascade instanceof java.util.List) {
                    @SuppressWarnings("unchecked")
                    List<? extends AnnotationValue> cascadeList = (List<? extends AnnotationValue>) cascade;
                    return cascadeList.stream()
                        .map(av -> {
                            Object val = av.getValue();
                            if (val instanceof TypeElement) {
                                return mapCascadeType(((TypeElement) val).getSimpleName().toString());
                            }
                            return null;
                        })
                        .filter(c -> c != null)
                        .collect(Collectors.toList());
                } else if (cascade instanceof TypeElement) {
                    // Single cascade type
                    String cascadeTypeName = ((TypeElement) cascade).getSimpleName().toString();
                    io.vidocq.mansart.persistence.spi.RelationshipMetadata.CascadeType single = 
                        mapCascadeType(cascadeTypeName);
                    return single != null ? List.of(single) : Collections.emptyList();
                }
            }
        }

        return Collections.emptyList();
    }

    private io.vidocq.mansart.persistence.spi.RelationshipMetadata.CascadeType mapCascadeType(String name) {
        return switch (name) {
            case "ALL" -> io.vidocq.mansart.persistence.spi.RelationshipMetadata.CascadeType.ALL;
            case "PERSIST" -> io.vidocq.mansart.persistence.spi.RelationshipMetadata.CascadeType.PERSIST;
            case "MERGE" -> io.vidocq.mansart.persistence.spi.RelationshipMetadata.CascadeType.MERGE;
            case "REMOVE" -> io.vidocq.mansart.persistence.spi.RelationshipMetadata.CascadeType.REMOVE;
            case "REFRESH" -> io.vidocq.mansart.persistence.spi.RelationshipMetadata.CascadeType.REFRESH;
            case "DETACH" -> io.vidocq.mansart.persistence.spi.RelationshipMetadata.CascadeType.DETACH;
            default -> null;
        };
    }

    private boolean extractOrphanRemoval(Map<String, AnnotationValue> values) {
        AnnotationValue orphanRemovalValue = values.get("orphanRemoval");
        if (orphanRemovalValue != null) {
            Object orphanRemoval = orphanRemovalValue.getValue();
            if (orphanRemoval instanceof Boolean) {
                return (Boolean) orphanRemoval;
            }
        }
        // Default is false
        return false;
    }

    private JoinColumnInfo extractJoinColumn(Element element) {
        AnnotationMirror joinColumnMirror = getAnnotationMirror(element, JOIN_COLUMN_ANNOTATION);

        if (joinColumnMirror == null) {
            // No explicit @JoinColumn, will use default
            return null;
        }

        Map<String, AnnotationValue> values = getAnnotationValues(joinColumnMirror);

        String name = null;
        AnnotationValue nameValue = values.get("name");
        if (nameValue != null) {
            name = nameValue.getValue().toString();
        }

        String referencedColumnName = null;
        AnnotationValue refValue = values.get("referencedColumnName");
        if (refValue != null) {
            referencedColumnName = refValue.getValue().toString();
        }

        boolean nullable = true;
        AnnotationValue nullableValue = values.get("nullable");
        if (nullableValue != null) {
            Object nullableObj = nullableValue.getValue();
            if (nullableObj instanceof Boolean) {
                nullable = (Boolean) nullableObj;
            }
        }

        String columnDefinition = null;
        AnnotationValue colDefValue = values.get("columnDefinition");
        if (colDefValue != null) {
            columnDefinition = colDefValue.getValue().toString();
        }

        return new JoinColumnInfo(name, referencedColumnName, nullable, columnDefinition);
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
        Set<? extends ExecutableElement> keys = mirror.getElementValues().keySet();
        for (ExecutableElement key : keys) {
            values.put(key.getSimpleName().toString(), mirror.getElementValues().get(key));
        }
        return values;
    }

    /**
     * Information extracted from {@code @OneToMany} annotation.
     */
    public record OneToManyInfo(
            String targetEntityName,
            String mappedBy,
            io.vidocq.mansart.persistence.spi.RelationshipMetadata.FetchType fetchType,
            io.vidocq.mansart.persistence.spi.RelationshipMetadata.CascadeType[] cascadeTypes,
            boolean orphanRemoval,
            JoinColumnInfo joinColumnInfo
    ) {
    }
}

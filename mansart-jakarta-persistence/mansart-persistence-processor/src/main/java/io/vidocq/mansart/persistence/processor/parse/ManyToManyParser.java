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
 * Parser for {@code @jakarta.persistence.ManyToMany} annotation.
 *
 * <p>This class extracts relationship metadata for ManyToMany associations.
 */
public final class ManyToManyParser {

    private static final String MANY_TO_MANY_ANNOTATION = "jakarta.persistence.ManyToMany";
    private static final String JOIN_TABLE_ANNOTATION = "jakarta.persistence.JoinTable";
    private static final String CASCADE_ANNOTATION = "jakarta.persistence.CascadeType";
    private static final String MAP_KEY_JOIN_COLUMN_ANNOTATION = "jakarta.persistence.MapKeyJoinColumn";

    /**
     * Parses the {@code @ManyToMany} annotation from a field/property.
     *
     * @param element the element annotated with {@code @ManyToMany}
     * @param types the type utilities
     * @return a ManyToManyInfo containing relationship metadata, or null if not annotated
     */
    public ManyToManyInfo parse(Element element, javax.lang.model.util.Types types) {
        AnnotationMirror manyToManyMirror = getAnnotationMirror(element, MANY_TO_MANY_ANNOTATION);
        if (manyToManyMirror == null) {
            return null;
        }

        Map<String, AnnotationValue> values = getAnnotationValues(manyToManyMirror);

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

        // Extract JoinTable info
        JoinTableInfo joinTableInfo = extractJoinTable(element);

        return new ManyToManyInfo(
            targetEntityName,
            mappedBy,
            fetchType,
            cascadeTypes.toArray(new io.vidocq.mansart.persistence.spi.RelationshipMetadata.CascadeType[0]),
            joinTableInfo
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
        // Default is LAZY for ManyToMany
        return io.vidocq.mansart.persistence.spi.RelationshipMetadata.FetchType.LAZY;
    }

    private List<io.vidocq.mansart.persistence.spi.RelationshipMetadata.CascadeType> extractCascadeTypes(
            Element element) {
        AnnotationMirror manyToManyMirror = getAnnotationMirror(element, MANY_TO_MANY_ANNOTATION);
        if (manyToManyMirror != null) {
            Map<String, AnnotationValue> values = getAnnotationValues(manyToManyMirror);
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

    private JoinTableInfo extractJoinTable(Element element) {
        AnnotationMirror joinTableMirror = getAnnotationMirror(element, JOIN_TABLE_ANNOTATION);

        if (joinTableMirror == null) {
            // No explicit @JoinTable, will use default
            return null;
        }

        Map<String, AnnotationValue> values = getAnnotationValues(joinTableMirror);

        String name = null;
        AnnotationValue nameValue = values.get("name");
        if (nameValue != null) {
            name = nameValue.getValue().toString();
        }

        String catalog = null;
        AnnotationValue catalogValue = values.get("catalog");
        if (catalogValue != null) {
            catalog = catalogValue.getValue().toString();
        }

        String schema = null;
        AnnotationValue schemaValue = values.get("schema");
        if (schemaValue != null) {
            schema = schemaValue.getValue().toString();
        }

        // Extract join columns
        List<JoinColumnInfo> joinColumns = extractJoinColumns(values.get("joinColumns"));
        List<JoinColumnInfo> inverseJoinColumns = extractJoinColumns(values.get("inverseJoinColumns"));

        return new JoinTableInfo(name, catalog, schema, 
                                 joinColumns.toArray(new JoinColumnInfo[0]), 
                                 inverseJoinColumns.toArray(new JoinColumnInfo[0]));
    }

    private List<JoinColumnInfo> extractJoinColumns(AnnotationValue joinColumnsValue) {
        if (joinColumnsValue == null) {
            return Collections.emptyList();
        }

        Object value = joinColumnsValue.getValue();
        if (value instanceof java.util.List) {
            @SuppressWarnings("unchecked")
            List<AnnotationValue> columns = (List<AnnotationValue>) value;
            return columns.stream()
                .map(av -> {
                    Object col = av.getValue();
                    if (col instanceof AnnotationMirror) {
                        return parseJoinColumnFromAnnotation((AnnotationMirror) col);
                    }
                    return null;
                })
                .filter(jc -> jc != null)
                .collect(Collectors.toList());
        }

        return Collections.emptyList();
    }

    private JoinColumnInfo parseJoinColumnFromAnnotation(AnnotationMirror joinColumnMirror) {
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
     * Information about a join table for ManyToMany relationships.
     */
    public record JoinTableInfo(
            String name,
            String catalog,
            String schema,
            JoinColumnInfo[] joinColumns,
            JoinColumnInfo[] inverseJoinColumns
    ) {
    }

    /**
     * Information extracted from {@code @ManyToMany} annotation.
     */
    public record ManyToManyInfo(
            String targetEntityName,
            String mappedBy,
            io.vidocq.mansart.persistence.spi.RelationshipMetadata.FetchType fetchType,
            io.vidocq.mansart.persistence.spi.RelationshipMetadata.CascadeType[] cascadeTypes,
            JoinTableInfo joinTableInfo
    ) {
    }
}

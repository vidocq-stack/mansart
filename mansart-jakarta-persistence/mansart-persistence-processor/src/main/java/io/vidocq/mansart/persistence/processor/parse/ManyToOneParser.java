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
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Parser for {@code @jakarta.persistence.ManyToOne} annotation.
 *
 * <p>This class extracts relationship metadata for ManyToOne associations.
 */
public final class ManyToOneParser {

    private static final String MANY_TO_ONE_ANNOTATION = "jakarta.persistence.ManyToOne";
    private static final String JOIN_COLUMN_ANNOTATION = "jakarta.persistence.JoinColumn";
    private static final String CASCADE_ANNOTATION = "jakarta.persistence.CascadeType";

    /**
     * Parses the {@code @ManyToOne} annotation from a field/property.
     *
     * @param element the element annotated with {@code @ManyToOne}
     * @param types the type utilities
     * @return a ManyToOneInfo containing relationship metadata, or null if not annotated
     */
    public ManyToOneInfo parse(Element element, javax.lang.model.util.Types types) {
        AnnotationMirror manyToOneMirror = getAnnotationMirror(element, MANY_TO_ONE_ANNOTATION);
        if (manyToOneMirror == null) {
            return null;
        }

        Map<String, AnnotationValue> values = getAnnotationValues(manyToOneMirror);

        // Extract target entity class
        TypeMirror targetEntityType = extractTargetEntityType(element);
        String targetEntityName = targetEntityType != null ? 
            targetEntityType.toString() : null;

        // Extract fetch type
        io.vidocq.mansart.persistence.spi.RelationshipMetadata.FetchType fetchType = 
            extractFetchType(values);

        // Extract cascade types
        List<io.vidocq.mansart.persistence.spi.RelationshipMetadata.CascadeType> cascadeTypes = 
            extractCascadeTypes(element);

        // Extract optional flag
        boolean isOptional = extractOptional(values);

        // Extract JoinColumn info
        JoinColumnInfo joinColumnInfo = extractJoinColumn(element);

        // Extract orphan removal (not applicable for ManyToOne, always false)
        boolean orphanRemoval = false;

        return new ManyToOneInfo(
            targetEntityName,
            fetchType,
            cascadeTypes.toArray(new io.vidocq.mansart.persistence.spi.RelationshipMetadata.CascadeType[0]),
            isOptional,
            joinColumnInfo,
            orphanRemoval
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
        // Default is EAGER for ManyToOne
        return io.vidocq.mansart.persistence.spi.RelationshipMetadata.FetchType.EAGER;
    }

    private List<io.vidocq.mansart.persistence.spi.RelationshipMetadata.CascadeType> extractCascadeTypes(
            Element element) {
        AnnotationMirror cascadeMirror = getAnnotationMirror(element, 
            "jakarta.persistence.Cascade");
        
        if (cascadeMirror != null) {
            Map<String, AnnotationValue> cascadeValues = getAnnotationValues(cascadeMirror);
            AnnotationValue valueValue = cascadeValues.get("value");
            
            if (valueValue != null) {
                Object value = valueValue.getValue();
                if (value instanceof java.util.List) {
                    @SuppressWarnings("unchecked")
                    List<? extends AnnotationValue> cascadeList = (List<? extends AnnotationValue>) value;
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
                }
            }
        }
        
        // Check for individual cascade annotations (e.g., @Cascade({CascadeType.PERSIST}) is not standard)
        // Standard JPA uses @ManyToOne(cascade = {CascadeType.PERSIST, ...})
        // So we need to check the ManyToOne annotation itself
        AnnotationMirror manyToOneMirror = getAnnotationMirror(element, MANY_TO_ONE_ANNOTATION);
        if (manyToOneMirror != null) {
            Map<String, AnnotationValue> values = getAnnotationValues(manyToOneMirror);
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

    private boolean extractOptional(Map<String, AnnotationValue> values) {
        AnnotationValue optionalValue = values.get("optional");
        if (optionalValue != null) {
            Object optional = optionalValue.getValue();
            if (optional instanceof Boolean) {
                return (Boolean) optional;
            }
        }
        // Default is true for ManyToOne
        return true;
    }

    /**
     * Extracts JoinColumn information from an element.
     * This method is public so it can be reused by other parsers.
     *
     * @param element the element to extract JoinColumn from
     * @return JoinColumnInfo, or null if no JoinColumn annotation is present
     */
    public static JoinColumnInfo extractJoinColumn(Element element) {
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
     * Information extracted from {@code @ManyToOne} annotation.
     */
    public record ManyToOneInfo(
            String targetEntityName,
            io.vidocq.mansart.persistence.spi.RelationshipMetadata.FetchType fetchType,
            io.vidocq.mansart.persistence.spi.RelationshipMetadata.CascadeType[] cascadeTypes,
            boolean optional,
            JoinColumnInfo joinColumnInfo,
            boolean orphanRemoval
    ) {
    }
}

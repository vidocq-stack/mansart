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
import javax.lang.model.type.TypeMirror;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Parser for {@code @jakarta.persistence.OneToOne} annotation.
 *
 * <p>This class extracts relationship metadata for OneToOne associations.
 */
public final class OneToOneParser {

    private static final String ONE_TO_ONE_ANNOTATION = "jakarta.persistence.OneToOne";
    private static final String JOIN_COLUMN_ANNOTATION = "jakarta.persistence.JoinColumn";

    /**
     * Parses the {@code @OneToOne} annotation from a field/property.
     *
     * @param element the element annotated with {@code @OneToOne}
     * @param types the type utilities
     * @return a OneToOneInfo containing relationship metadata, or null if not annotated
     */
    public OneToOneInfo parse(Element element, javax.lang.model.util.Types types) {
        AnnotationMirror oneToOneMirror = getAnnotationMirror(element, ONE_TO_ONE_ANNOTATION);
        if (oneToOneMirror == null) {
            return null;
        }

        Map<String, AnnotationValue> values = getAnnotationValues(oneToOneMirror);

        // Extract target entity class
        TypeMirror targetEntityType = extractTargetEntityType(element);
        String targetEntityName = targetEntityType != null ? 
            targetEntityType.toString() : null;

        // Extract fetch type
        io.vidocq.mansart.persistence.spi.RelationshipMetadata.FetchType fetchType = 
            extractFetchType(values);

        // Extract cascade types
        List<io.vidocq.mansart.persistence.spi.RelationshipMetadata.CascadeType> cascadeTypes = 
            extractCascadeTypes(values);

        // Extract optional flag
        boolean isOptional = extractOptional(values);

        // Extract JoinColumn info
        JoinColumnInfo joinColumnInfo = ManyToOneParser.extractJoinColumn(element);

        // Extract orphan removal
        boolean orphanRemoval = extractOrphanRemoval(values);

        // Extract mappedBy (for bidirectional relationships)
        String mappedBy = extractMappedBy(values);

        return new OneToOneInfo(
            targetEntityName,
            fetchType,
            cascadeTypes.toArray(new io.vidocq.mansart.persistence.spi.RelationshipMetadata.CascadeType[0]),
            isOptional,
            joinColumnInfo,
            orphanRemoval,
            mappedBy
        );
    }

    private javax.lang.model.type.TypeMirror extractTargetEntityType(Element element) {
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
        // Default is EAGER for OneToOne
        return io.vidocq.mansart.persistence.spi.RelationshipMetadata.FetchType.EAGER;
    }

    private List<io.vidocq.mansart.persistence.spi.RelationshipMetadata.CascadeType> extractCascadeTypes(
            Map<String, AnnotationValue> values) {
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
        // Default is true for OneToOne
        return true;
    }

    private boolean extractOrphanRemoval(Map<String, AnnotationValue> values) {
        AnnotationValue orphanRemovalValue = values.get("orphanRemoval");
        if (orphanRemovalValue != null) {
            Object orphanRemoval = orphanRemovalValue.getValue();
            if (orphanRemoval instanceof Boolean) {
                return (Boolean) orphanRemoval;
            }
        }
        return false;
    }

    private String extractMappedBy(Map<String, AnnotationValue> values) {
        AnnotationValue mappedByValue = values.get("mappedBy");
        if (mappedByValue != null) {
            Object mappedBy = mappedByValue.getValue();
            if (mappedBy instanceof String) {
                return (String) mappedBy;
            }
        }
        return null;
    }

    // Helper methods
    private AnnotationMirror getAnnotationMirror(Element element, String annotationFqn) {
        for (AnnotationMirror mirror : element.getAnnotationMirrors()) {
            if (((TypeElement) mirror.getAnnotationType().asElement())
                    .getQualifiedName().toString().equals(annotationFqn)) {
                return mirror;
            }
        }
        return null;
    }

    private Map<String, AnnotationValue> getAnnotationValues(AnnotationMirror mirror) {
        Map<String, AnnotationValue> values = new java.util.HashMap<>();
        Set<? extends ExecutableElement> keys = mirror.getElementValues().keySet();
        for (ExecutableElement key : keys) {
            values.put(key.getSimpleName().toString(), mirror.getElementValues().get(key));
        }
        return values;
    }

    /**
     * Information extracted from {@code @OneToOne} annotation.
     */
    public record OneToOneInfo(
            String targetEntityName,
            io.vidocq.mansart.persistence.spi.RelationshipMetadata.FetchType fetchType,
            io.vidocq.mansart.persistence.spi.RelationshipMetadata.CascadeType[] cascadeTypes,
            boolean optional,
            JoinColumnInfo joinColumnInfo,
            boolean orphanRemoval,
            String mappedBy
    ) {
    }
}

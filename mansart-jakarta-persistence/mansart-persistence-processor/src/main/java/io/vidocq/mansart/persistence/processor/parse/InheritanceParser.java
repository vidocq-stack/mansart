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
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;

import java.util.Map;

/**
 * Parser for JPA inheritance annotations.
 *
 * <p>This class extracts inheritance strategy and discriminator information
 * from {@code @Inheritance}, {@code @DiscriminatorColumn}, and {@code @DiscriminatorValue} annotations.
 */
public final class InheritanceParser {

    private static final String INHERITANCE_ANNOTATION = "jakarta.persistence.Inheritance";
    private static final String DISCRIMINATOR_COLUMN_ANNOTATION = "jakarta.persistence.DiscriminatorColumn";
    private static final String DISCRIMINATOR_VALUE_ANNOTATION = "jakarta.persistence.DiscriminatorValue";

    /**
     * Inheritance information extracted from annotations.
     */
    public record InheritanceInfo(
            InheritanceType strategy,
            String discriminatorColumn,
            String discriminatorValue,
            TypeElement parentEntity
    ) {}

    /**
     * Parses inheritance annotations from an entity type element.
     *
     * @param type the entity type element
     * @return InheritanceInfo containing inheritance configuration, or null if not an inherited entity
     */
    public InheritanceInfo parse(TypeElement type) {
        InheritanceType strategy = null;
        String discriminatorColumn = null;
        String discriminatorValue = null;
        TypeElement parentEntity = null;

        // Check for @Inheritance annotation
        AnnotationMirror inheritanceMirror = EntityScanner.getAnnotationMirror(type, INHERITANCE_ANNOTATION);
        if (inheritanceMirror != null) {
            Map<String, AnnotationValue> values = EntityScanner.getAnnotationValues(inheritanceMirror);
            AnnotationValue strategyValue = values.get("strategy");
            if (strategyValue != null) {
                Object strategyObj = strategyValue.getValue();
                if (strategyObj instanceof javax.lang.model.element.VariableElement) {
                    javax.lang.model.element.VariableElement enumValue = (javax.lang.model.element.VariableElement) strategyObj;
                    String simpleName = enumValue.getSimpleName().toString();
                    strategy = InheritanceType.valueOf(simpleName);
                }
            }
        }

        // Check for @DiscriminatorColumn annotation (on the entity or its superclass)
        AnnotationMirror discColMirror = EntityScanner.getAnnotationMirror(type, DISCRIMINATOR_COLUMN_ANNOTATION);
        if (discColMirror != null) {
            Map<String, AnnotationValue> values = EntityScanner.getAnnotationValues(discColMirror);
            AnnotationValue nameValue = values.get("name");
            if (nameValue != null) {
                discriminatorColumn = nameValue.getValue().toString();
            } else {
                // Default discriminator column name
                discriminatorColumn = "DTYPE";
            }
        }

        // Check for @DiscriminatorValue annotation
        AnnotationMirror discValMirror = EntityScanner.getAnnotationMirror(type, DISCRIMINATOR_VALUE_ANNOTATION);
        if (discValMirror != null) {
            Map<String, AnnotationValue> values = EntityScanner.getAnnotationValues(discValMirror);
            AnnotationValue value = values.get("value");
            if (value != null) {
                discriminatorValue = value.getValue().toString();
            }
        }

        // Check if this entity extends another entity
        TypeElement superclass = EntityScanner.getSuperclass(type);
        if (superclass != null && EntityScanner.hasAnnotation(superclass, "jakarta.persistence.Entity")) {
            parentEntity = superclass;
        }

        // If no explicit strategy but has parent entity, default to SINGLE_TABLE
        if (strategy == null && parentEntity != null) {
            strategy = InheritanceType.SINGLE_TABLE;
        }

        if (strategy == null && discriminatorColumn == null && discriminatorValue == null && parentEntity == null) {
            return null;
        }

        return new InheritanceInfo(strategy, discriminatorColumn, discriminatorValue, parentEntity);
    }
}

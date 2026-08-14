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

import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.TableGenerator;

import java.util.Map;
import java.util.HashMap;

/**
 * Parser for {@code @jakarta.persistence.GeneratedValue} annotation.
 *
 * <p>This class extracts generation strategy and generator configuration
 * for identifier generation.
 */
public final class GeneratedValueParser {

    private static final String GENERATED_VALUE_ANNOTATION = "jakarta.persistence.GeneratedValue";
    private static final String SEQUENCE_GENERATOR_ANNOTATION = "jakarta.persistence.SequenceGenerator";
    private static final String TABLE_GENERATOR_ANNOTATION = "jakarta.persistence.TableGenerator";

    /**
     * Parses the {@code @GeneratedValue} annotation from a field/property.
     *
     * @param element the element annotated with {@code @GeneratedValue}
     * @return a GeneratedValueInfo containing strategy and generator info, or null if not annotated
     */
    public GeneratedValueInfo parse(Element element) {
        AnnotationMirror genValueMirror = getAnnotationMirror(element, GENERATED_VALUE_ANNOTATION);
        if (genValueMirror == null) {
            return null;
        }

        // Extract strategy
        Map<String, AnnotationValue> values = getAnnotationValues(genValueMirror);
        AnnotationValue strategyValue = values.get("strategy");
        GenerationType generationType;
        if (strategyValue != null) {
            Object strategy = strategyValue.getValue();
            if (strategy instanceof VariableElement) {
                VariableElement enumValue = (VariableElement) strategy;
                String simpleName = enumValue.getSimpleName().toString();
                generationType = GenerationType.valueOf(simpleName);
            } else {
                generationType = GenerationType.AUTO;
            }
        } else {
            // Default strategy
            generationType = GenerationType.AUTO;
        }

        // Extract generator name
        String generatorName = null;
        AnnotationValue generatorValue = values.get("generator");
        if (generatorValue != null) {
            generatorName = generatorValue.getValue().toString();
        }

        // If generator name is present, extract generator configuration
        GeneratorConfig generatorConfig = null;
        if (generatorName != null && !generatorName.isEmpty()) {
            generatorConfig = extractGeneratorConfig(element, generatorName);
        }

        return new GeneratedValueInfo(generationType, generatorName, generatorConfig);
    }

    private GeneratorConfig extractGeneratorConfig(Element element, String generatorName) {
        // First check if the generator is on the same element
        AnnotationMirror seqGenMirror = getAnnotationMirror(element, SEQUENCE_GENERATOR_ANNOTATION);
        if (seqGenMirror != null) {
            Map<String, AnnotationValue> seqValues = getAnnotationValues(seqGenMirror);
            if (isGeneratorNameMatch(seqValues, generatorName)) {
                return parseSequenceGenerator(seqValues);
            }
        }

        AnnotationMirror tableGenMirror = getAnnotationMirror(element, TABLE_GENERATOR_ANNOTATION);
        if (tableGenMirror != null) {
            Map<String, AnnotationValue> tableValues = getAnnotationValues(tableGenMirror);
            if (isGeneratorNameMatch(tableValues, generatorName)) {
                return parseTableGenerator(tableValues);
            }
        }

        // If not found, search parent elements (for method access)
        if (element.getEnclosingElement() != null) {
            AnnotationMirror parentSeqMirror = getAnnotationMirror(element.getEnclosingElement(), SEQUENCE_GENERATOR_ANNOTATION);
            if (parentSeqMirror != null) {
                Map<String, AnnotationValue> seqValues = getAnnotationValues(parentSeqMirror);
                if (isGeneratorNameMatch(seqValues, generatorName)) {
                    return parseSequenceGenerator(seqValues);
                }
            }

            AnnotationMirror parentTableMirror = getAnnotationMirror(element.getEnclosingElement(), TABLE_GENERATOR_ANNOTATION);
            if (parentTableMirror != null) {
                Map<String, AnnotationValue> tableValues = getAnnotationValues(parentTableMirror);
                if (isGeneratorNameMatch(tableValues, generatorName)) {
                    return parseTableGenerator(tableValues);
                }
            }
        }

        return null;
    }

    private boolean isGeneratorNameMatch(Map<String, AnnotationValue> values, String generatorName) {
        AnnotationValue nameValue = values.get("name");
        if (nameValue != null) {
            String name = nameValue.getValue().toString();
            return generatorName.equals(name);
        }
        return false;
    }

    private GeneratorConfig parseSequenceGenerator(Map<String, AnnotationValue> values) {
        String name = null;
        String sequenceName = null;
        int initialValue = 1;
        int allocationSize = 20;

        AnnotationValue seqName = values.get("name");
        if (seqName != null) {
            name = seqName.getValue().toString();
        }

        AnnotationValue seqNameValue = values.get("sequenceName");
        if (seqNameValue != null) {
            sequenceName = seqNameValue.getValue().toString();
        }

        AnnotationValue initialValueValue = values.get("initialValue");
        if (initialValueValue != null) {
            initialValue = ((Number) initialValueValue.getValue()).intValue();
        }

        AnnotationValue allocationSizeValue = values.get("allocationSize");
        if (allocationSizeValue != null) {
            allocationSize = ((Number) allocationSizeValue.getValue()).intValue();
        }

        return new GeneratorConfig(name, sequenceName, null, null, null, null, initialValue, allocationSize, GenerationType.SEQUENCE);
    }

    private GeneratorConfig parseTableGenerator(Map<String, AnnotationValue> values) {
        String name = null;
        String tableName = null;
        String pkColumnName = null;
        String valueColumnName = null;
        String pkColumnValue = null;
        int initialValue = 1;
        int allocationSize = 20;

        AnnotationValue tableNameValue = values.get("tableName");
        if (tableNameValue != null) {
            tableName = tableNameValue.getValue().toString();
        }

        AnnotationValue pkColumnNameValue = values.get("pkColumnName");
        if (pkColumnNameValue != null) {
            pkColumnName = pkColumnNameValue.getValue().toString();
        }

        AnnotationValue valueColumnNameValue = values.get("valueColumnName");
        if (valueColumnNameValue != null) {
            valueColumnName = valueColumnNameValue.getValue().toString();
        }

        AnnotationValue pkColumnValueValue = values.get("pkColumnValue");
        if (pkColumnValueValue != null) {
            pkColumnValue = pkColumnValueValue.getValue().toString();
        }

        AnnotationValue initialValueValue = values.get("initialValue");
        if (initialValueValue != null) {
            initialValue = ((Number) initialValueValue.getValue()).intValue();
        }

        AnnotationValue allocationSizeValue = values.get("allocationSize");
        if (allocationSizeValue != null) {
            allocationSize = ((Number) allocationSizeValue.getValue()).intValue();
        }

        return new GeneratorConfig(name, null, tableName, pkColumnName, valueColumnName, pkColumnValue, initialValue, allocationSize, GenerationType.TABLE);
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
        Map<String, AnnotationValue> values = new HashMap<>();
        ExecutableElement[] keys = mirror.getElementValues().keySet().toArray(new ExecutableElement[0]);
        for (ExecutableElement key : keys) {
            values.put(key.getSimpleName().toString(), mirror.getElementValues().get(key));
        }
        return values;
    }

    /**
     * Records for generated value information.
     */
    public record GeneratedValueInfo(GenerationType generationType, String generatorName, GeneratorConfig generatorConfig) {
    }

    /**
     * Records for generator configuration.
     */
    public record GeneratorConfig(
            String name,
            String sequenceName,
            String tableName,
            String pkColumnName,
            String valueColumnName,
            String pkColumnValue,
            int initialValue,
            int allocationSize,
            GenerationType strategy
    ) {
        public GeneratorConfig {
            if (initialValue < 0) initialValue = 0;
            if (allocationSize <= 0) allocationSize = 20;
        }
    }
}

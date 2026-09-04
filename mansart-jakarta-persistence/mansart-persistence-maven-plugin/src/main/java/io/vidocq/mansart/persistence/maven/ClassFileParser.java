/*
 * SPDX-License-Identifier: EPL-2.0 OR EUPL-1.2 OR GPL-2.0-or-later
 */
package io.vidocq.mansart.persistence.maven;

import java.lang.classfile.ClassFile;
import java.lang.classfile.ClassModel;
import java.lang.classfile.FieldModel;
import java.lang.classfile.Attribute;
import java.lang.classfile.Annotation;
import java.lang.classfile.AnnotationElement;
import java.lang.classfile.AnnotationValue;
import java.lang.classfile.attribute.RuntimeVisibleAnnotationsAttribute;
import java.lang.classfile.attribute.RuntimeInvisibleAnnotationsAttribute;
import java.lang.classfile.constantpool.Utf8Entry;
import java.nio.file.Path;
import java.util.List;
import java.util.ArrayList;
import java.util.Optional;
import java.util.Map;
import java.util.HashMap;

/**
 * Class-File API parser for entity class scanning and processing.
 * Uses Java 26 Class-File API to read class file bytes and extract JPA annotations.
 */
public final class ClassFileParser {

    private static final String JAKARTA_PERSISTENCE_ENTITY = "jakarta/persistence/Entity";
    private static final String JAKARTA_PERSISTENCE_TABLE = "jakarta/persistence/Table";
    private static final String JAKARTA_PERSISTENCE_ID = "jakarta/persistence/Id";
    private static final String JAKARTA_PERSISTENCE_COLUMN = "jakarta/persistence/Column";
    private static final String JAKARTA_PERSISTENCE_VERSION = "jakarta/persistence/Version";
    private static final String JAKARTA_PERSISTENCE_MANY_TO_ONE = "jakarta/persistence/ManyToOne";
    private static final String JAKARTA_PERSISTENCE_ONE_TO_ONE = "jakarta/persistence/OneToOne";
    private static final String JAKARTA_PERSISTENCE_JOIN_COLUMN = "jakarta/persistence/JoinColumn";
    private static final String JAKARTA_PERSISTENCE_ENUMERATED = "jakarta/persistence/Enumerated";
    private static final String JAKARTA_PERSISTENCE_EMBEDDED = "jakarta/persistence/Embedded";
    private static final String JAKARTA_PERSISTENCE_TRANSIENT = "jakarta/persistence/Transient";

    private ClassFileParser() {
        // Utility class
    }

    /**
     * Checks if a class file has the @Entity annotation.
     */
    public static boolean hasEntityAnnotation(Path classFile) {
        try {
            ClassModel classModel = ClassFile.of().parse(classFile);
            return hasAnnotation(classModel, JAKARTA_PERSISTENCE_ENTITY);
        } catch (Exception e) {
            System.err.println("Failed to check @Entity annotation on " + classFile + ": " + e.getMessage());
            return false;
        }
    }

    /**
     * Parses an entity class and extracts metadata for code generation.
     */
    public static EntityMetadata parseEntityClass(Path classFile, String className) {
        try {
            ClassModel classModel = ClassFile.of().parse(classFile);
            
            // Extract package name from class name
            String packageName = extractPackageName(className);
            String simpleName = extractSimpleName(className);
            
            // Get table name from @Table annotation or default to pluralized class name
            String tableName = getAnnotationValue(classModel, JAKARTA_PERSISTENCE_TABLE, "name").orElse("");
            if (tableName == null || tableName.isEmpty()) {
                tableName = toSnakeCase(toPlural(simpleName));
            }
            
            // Extract fields from the class
            List<FieldMetadata> fields = new ArrayList<>();
            for (FieldModel fieldModel : classModel.fields()) {
                FieldMetadata field = parseField(fieldModel);
                if (field != null) {
                    fields.add(field);
                }
            }
            
            return new EntityMetadata(packageName, simpleName, tableName, "", "", fields);
            
        } catch (Exception e) {
            System.err.println("Failed to parse entity class " + className + ": " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    private static FieldMetadata parseField(FieldModel fieldModel) {
        // Check if field should be ignored (@Transient)
        if (hasAnnotation(fieldModel, JAKARTA_PERSISTENCE_TRANSIENT)) {
            return null;
        }
        
        String name = fieldModel.fieldName().toString();
        String type = fieldModel.fieldType().toString();
        
        // Extract annotations
        boolean isId = hasAnnotation(fieldModel, JAKARTA_PERSISTENCE_ID);
        boolean isVersion = hasAnnotation(fieldModel, JAKARTA_PERSISTENCE_VERSION);
        boolean isManyToOne = hasAnnotation(fieldModel, JAKARTA_PERSISTENCE_MANY_TO_ONE);
        boolean isOneToOne = hasAnnotation(fieldModel, JAKARTA_PERSISTENCE_ONE_TO_ONE);
        boolean isEnumerated = hasAnnotation(fieldModel, JAKARTA_PERSISTENCE_ENUMERATED);
        boolean isEmbedded = hasAnnotation(fieldModel, JAKARTA_PERSISTENCE_EMBEDDED);
        
        // Get column name
        String column = getAnnotationValue(fieldModel, JAKARTA_PERSISTENCE_COLUMN, "name").orElse(null);
        if (column == null || column.isEmpty()) {
            // For relationships without @JoinColumn, generate FK column name
            if (isManyToOne || isOneToOne) {
                column = toSnakeCase(type) + "_id";
            } else {
                column = toSnakeCase(name);
            }
        } else {
            // Check @JoinColumn
            String joinColumn = getAnnotationValue(fieldModel, JAKARTA_PERSISTENCE_JOIN_COLUMN, "name").orElse(null);
            if (joinColumn != null && !joinColumn.isEmpty()) {
                column = joinColumn;
            }
        }
        
        // Get nullable/unique from @Column
        boolean nullable = true; // default
        boolean unique = false; // default
        
        String nullableStr = getAnnotationValue(fieldModel, JAKARTA_PERSISTENCE_COLUMN, "nullable").orElse(null);
        if (nullableStr != null) {
            nullable = Boolean.parseBoolean(nullableStr);
        }
        
        String uniqueStr = getAnnotationValue(fieldModel, JAKARTA_PERSISTENCE_COLUMN, "unique").orElse(null);
        if (uniqueStr != null) {
            unique = Boolean.parseBoolean(uniqueStr);
        }
        
        // Determine attribute type
        AttributeType attrType = determineType(type, isId, isVersion, isManyToOne, isOneToOne, isEnumerated, isEmbedded);
        
        return new FieldMetadata(name, type, column, isId, isVersion, false, nullable, unique, attrType);
    }

    private static List<Annotation> getAnnotations(Attribute<?> attribute) {
        if (attribute instanceof RuntimeVisibleAnnotationsAttribute rva) {
            return rva.annotations();
        } else if (attribute instanceof RuntimeInvisibleAnnotationsAttribute ria) {
            return ria.annotations();
        }
        return List.of();
    }

    private static String toInternalForm(String annotationType) {
        // Convert from "jakarta/persistence/Entity" to "Ljakarta/persistence/Entity;"
        return "L" + annotationType + ";";
    }

    private static boolean hasAnnotation(ClassModel element, String annotationType) {
        String internalForm = toInternalForm(annotationType);
        return element.attributes().stream()
                .flatMap(attr -> getAnnotations(attr).stream())
                .anyMatch(ann -> ann.className().toString().equals(internalForm));
    }

    private static boolean hasAnnotation(FieldModel element, String annotationType) {
        String internalForm = toInternalForm(annotationType);
        return element.attributes().stream()
                .flatMap(attr -> getAnnotations(attr).stream())
                .anyMatch(ann -> ann.className().toString().equals(internalForm));
    }

    private static Optional<String> getAnnotationValue(ClassModel element, String annotationType, String attributeName) {
        String internalForm = toInternalForm(annotationType);
        return element.attributes().stream()
                .flatMap(attr -> getAnnotations(attr).stream())
                .filter(ann -> ann.className().toString().equals(internalForm))
                .findFirst()
                .flatMap(ann -> getAnnotationElementValue(ann, attributeName));
    }

    private static Optional<String> getAnnotationValue(FieldModel element, String annotationType, String attributeName) {
        String internalForm = toInternalForm(annotationType);
        return element.attributes().stream()
                .flatMap(attr -> getAnnotations(attr).stream())
                .filter(ann -> ann.className().toString().equals(internalForm))
                .findFirst()
                .flatMap(ann -> getAnnotationElementValue(ann, attributeName));
    }

    private static Optional<String> getAnnotationElementValue(Annotation annotation, String elementName) {
        for (AnnotationElement element : annotation.elements()) {
            if (element.name().toString().equals(elementName)) {
                AnnotationValue value = element.value();
                if (value instanceof AnnotationValue.OfString sv) {
                    return Optional.of(sv.stringValue());
                } else if (value instanceof AnnotationValue.OfInt iv) {
                    return Optional.of(String.valueOf(iv.intValue()));
                } else if (value instanceof AnnotationValue.OfLong lv) {
                    return Optional.of(String.valueOf(lv.longValue()));
                } else if (value instanceof AnnotationValue.OfBoolean bv) {
                    return Optional.of(String.valueOf(bv.booleanValue()));
                } else if (value instanceof AnnotationValue.OfEnum ev) {
                    return Optional.of(ev.constantName().toString());
                }
            }
        }
        return Optional.empty();
    }

    private static String extractPackageName(String className) {
        int lastDot = className.lastIndexOf('.');
        return lastDot >= 0 ? className.substring(0, lastDot) : "";
    }

    private static String extractSimpleName(String className) {
        int lastDot = className.lastIndexOf('.');
        return lastDot >= 0 ? className.substring(lastDot + 1) : className;
    }

    private static AttributeType determineType(String type, boolean isId, boolean isVersion,
            boolean isManyToOne, boolean isOneToOne, boolean isEnumerated, boolean isEmbedded) {
        if (isId) return AttributeType.ID;
        if (isVersion) return AttributeType.VERSION;
        if (isManyToOne || isOneToOne) return AttributeType.REFERENCE;
        if (isEnumerated) return AttributeType.ENUM;
        if (isEmbedded) return AttributeType.EMBEDDED;
        if (isTemporal(type)) return AttributeType.TEMPORAL;
        if (isNumeric(type)) return AttributeType.NUMERIC;
        return AttributeType.BASIC;
    }

    private static boolean isTemporal(String type) {
        return type.equals("java/util/Date") || type.equals("java/time/LocalDate") ||
               type.equals("java/time/LocalDateTime") || type.equals("java/time/LocalTime") ||
               type.equals("java/time/Instant") || type.equals("java/sql/Date") ||
               type.equals("java/sql/Time") || type.equals("java/sql/Timestamp");
    }

    private static boolean isNumeric(String type) {
        return type.equals("java/lang/Integer") || type.equals("java/lang/Long") ||
               type.equals("java/lang/Double") || type.equals("java/lang/Float") ||
               type.equals("int") || type.equals("long") || type.equals("double") ||
               type.equals("float") || type.equals("java/math/BigInteger") ||
               type.equals("java/math/BigDecimal");
    }

    /**
     * Converts an internal JVM type name to a Java class name.
     * For example: "Lio/vidocq/mansart/persistence/external/ExternalPerson;" -> "io.vidocq.mansart.persistence.external.ExternalPerson"
     *
     * @param internalType the internal JVM type name
     * @return the Java class name
     */
    public static String toJavaClassName(String internalType) {
        if (internalType == null || internalType.isEmpty()) {
            return "";
        }
        
        // Handle reference types: Ljava/lang/Long; -> java.lang.Long
        if (internalType.startsWith("L") && internalType.endsWith(";")) {
            String withoutPrefix = internalType.substring(1, internalType.length() - 1);
            return withoutPrefix.replace("/", ".");
        }
        
        // Handle primitive types - return as is
        return internalType;
    }

    private static String toSnakeCase(String s) {
        if (s == null || s.isEmpty()) return s;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '/' || c == '.') continue; // Skip package separators
            if (Character.isUpperCase(c) && i > 0 && sb.length() > 0) sb.append('_');
            sb.append(Character.toLowerCase(c));
        }
        return sb.toString();
    }

    private static String toPlural(String s) {
        if (s == null || s.isEmpty()) return s;
        
        // Handle common English pluralization rules
        if (s.endsWith("y") && s.length() > 1 && !isVowel(s.charAt(s.length() - 2))) {
            return s.substring(0, s.length() - 1) + "ies";
        }
        if (s.endsWith("s") || s.endsWith("sh") || s.endsWith("ch") || 
            s.endsWith("x") || s.endsWith("z")) {
            return s + "es";
        }
        if (s.endsWith("f") || s.endsWith("fe")) {
            return s.substring(0, s.length() - (s.endsWith("fe") ? 2 : 1)) + "ves";
        }
        if (s.endsWith("o") && s.length() > 1 && !isVowel(s.charAt(s.length() - 2))) {
            return s + "es";
        }
        if (s.endsWith("us")) {
            return s.substring(0, s.length() - 2) + "i";
        }
        if (s.endsWith("on") || s.endsWith("um")) {
            return s.substring(0, s.length() - 2) + "a";
        }
        if (s.endsWith("is")) {
            return s.substring(0, s.length() - 2) + "es";
        }
        
        // Default: add 's'
        return s + "s";
    }

    private static boolean isVowel(char c) {
        return c == 'a' || c == 'e' || c == 'i' || c == 'o' || c == 'u' || 
               c == 'A' || c == 'E' || c == 'I' || c == 'O' || c == 'U';
    }

    // Metadata classes
    public record EntityMetadata(
        String packageName, String entityName, String tableName,
        String schema, String catalog, List<FieldMetadata> fields
    ) {}

    public record FieldMetadata(
        String name, String type, String column,
        boolean isId, boolean isVersion, boolean isGenerated,
        boolean nullable, boolean unique, AttributeType attributeType
    ) {}

    public enum AttributeType {
        ID, VERSION, BASIC, NUMERIC, TEMPORAL, ENUM, REFERENCE, EMBEDDED
    }
}
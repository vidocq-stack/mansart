/*
 * SPDX-License-Identifier: EPL-2.0 OR EUPL-1.2 OR GPL-2.0-or-later
 */
package io.vidocq.mansart.persistence.core.runtime;

import io.vidocq.mansart.persistence.spi.Attribute;
import io.vidocq.mansart.persistence.spi.EntityModel;
import io.vidocq.mansart.persistence.spi.IdAttribute;
import io.vidocq.mansart.persistence.spi.ReferenceAttribute;

import java.io.IOException;
import java.io.InputStream;
import java.lang.classfile.Annotation;
import java.lang.classfile.AnnotationElement;
import java.lang.classfile.AnnotationValue;
import java.lang.classfile.ClassFile;
import java.lang.classfile.ClassModel;
import java.lang.classfile.FieldModel;
import java.lang.classfile.attribute.RuntimeVisibleAnnotationsAttribute;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Tier 3 fallback: builds {@link EntityModel} from class file bytes at bootstrap
 * for entities that neither APT (Tier 1) nor the Maven plugin (Tier 2) reached.
 * <p>
 * Uses the Java 26 Class-File API to parse annotations and field metadata.
 * Field access (get/set) is deferred to generated hidden classes (M3-JP-20).
 */
public final class RuntimeEntityModelBuilder {

    private static final String JPA_ENTITY = "Ljakarta/persistence/Entity;";
    private static final String JPA_TABLE = "Ljakarta/persistence/Table;";
    private static final String JPA_ID = "Ljakarta/persistence/Id;";
    private static final String JPA_COLUMN = "Ljakarta/persistence/Column;";
    private static final String JPA_VERSION = "Ljakarta/persistence/Version;";
    private static final String JPA_TRANSIENT = "Ljakarta/persistence/Transient;";
    private static final String JPA_GENERATED_VALUE = "Ljakarta/persistence/GeneratedValue;";
    private static final String JPA_SEQUENCE_GENERATOR = "Ljakarta/persistence/SequenceGenerator;";
    private static final String JPA_TABLE_GENERATOR = "Ljakarta/persistence/TableGenerator;";
    private static final String JPA_MANY_TO_ONE = "Ljakarta/persistence/ManyToOne;";
    private static final String JPA_ONE_TO_ONE = "Ljakarta/persistence/OneToOne;";
    private static final String JPA_JOIN_COLUMN = "Ljakarta/persistence/JoinColumn;";

    private static final String SEQUENCE_NAME = "SEQUENCE_NAME";
    private static final String SEQUENCE_NEXT_VAL = "SEQUENCE_NEXT_VAL";

    private final Tier3WarningCollector warningCollector;

    /**
     * Context for field parsing operations.
     */
    private static final class FieldParsingContext {
        private final FieldModel fieldModel;
        private final Class<?> entityClass;
        private final EntityModel<?> entityModel;
        private final java.util.Map<String, String[]> sequenceGenerators;
        private final java.util.Map<String, String[]> tableGenerators;

        FieldParsingContext(FieldModel fieldModel, Class<?> entityClass, EntityModel<?> entityModel,
                           java.util.Map<String, String[]> sequenceGenerators,
                           java.util.Map<String, String[]> tableGenerators) {
            this.fieldModel = fieldModel;
            this.entityClass = entityClass;
            this.entityModel = entityModel;
            this.sequenceGenerators = sequenceGenerators;
            this.tableGenerators = tableGenerators;
        }

        String getName() {
            return fieldModel.fieldName().toString();
        }

        String getInternalType() {
            return fieldModel.fieldType().toString();
        }

        Class<?> getJavaType() {
            return toJavaType(getInternalType(), entityClass.getClassLoader());
        }

        boolean isId() {
            return hasAnnotation(fieldModel, JPA_ID);
        }

        boolean isVersion() {
            return hasAnnotation(fieldModel, JPA_VERSION);
        }

        boolean isReference() {
            return hasAnnotation(fieldModel, JPA_MANY_TO_ONE) || hasAnnotation(fieldModel, JPA_ONE_TO_ONE);
        }

        String getColumnName() {
            // Check @JoinColumn first for relationship fields
            Optional<String> joinColumnName = getAnnotationValue(fieldModel, JPA_JOIN_COLUMN, "name");
            if (joinColumnName.isPresent() && !joinColumnName.get().isEmpty()) {
                return joinColumnName.get();
            }
            // Fall back to @Column
            return getAnnotationValue(fieldModel, JPA_COLUMN, "name")
                    .filter(n -> !n.isEmpty())
                    .orElseGet(() -> toSnakeCase(getName()));
        }

        boolean isNullable() {
            return getAnnotationValue(fieldModel, JPA_COLUMN, "nullable")
                    .map(Boolean::parseBoolean)
                    .orElse(true);
        }

        boolean isUnique() {
            return getAnnotationValue(fieldModel, JPA_COLUMN, "unique")
                    .map(Boolean::parseBoolean)
                    .orElse(false);
        }

        int getLength() {
            return getAnnotationValue(fieldModel, JPA_COLUMN, "length")
                    .map(Integer::parseInt)
                    .orElse(-1);
        }

        int getPrecision() {
            return getAnnotationValue(fieldModel, JPA_COLUMN, "precision")
                    .map(Integer::parseInt)
                    .orElse(-1);
        }

        int getScale() {
            return getAnnotationValue(fieldModel, JPA_COLUMN, "scale")
                    .map(Integer::parseInt)
                    .orElse(-1);
        }

        boolean isInsertable() {
            return getAnnotationValue(fieldModel, JPA_COLUMN, "insertable")
                    .map(Boolean::parseBoolean)
                    .orElse(true);
        }

        boolean isUpdatable() {
            return getAnnotationValue(fieldModel, JPA_COLUMN, "updatable")
                    .map(Boolean::parseBoolean)
                    .orElse(true);
        }

        String getColumnDefinition() {
            return getAnnotationValue(fieldModel, JPA_COLUMN, "columnDefinition")
                    .orElse("");
        }

        Optional<String> getGeneratedValueStrategy() {
            return RuntimeEntityModelBuilder.getGeneratedValueStrategy(fieldModel);
        }

        Optional<String> getGeneratedValueGenerator() {
            return RuntimeEntityModelBuilder.getGeneratedValueGenerator(fieldModel);
        }

    }

    /**
     * Creates a builder with no warning collection.
     */
    public RuntimeEntityModelBuilder() {
        this(null);
    }

    /**
     * Creates a builder that collects tier-3 warnings.
     *
     * @param warningCollector the warning collector, or null to disable
     */
    public RuntimeEntityModelBuilder(Tier3WarningCollector warningCollector) {
        this.warningCollector = warningCollector;
    }

    /**
     * Builds an EntityModel from the given entity class by reading its class file bytes.
     *
     * @param entityClass the entity class
     * @param <T>         the entity type
     * @return the entity model
     * @throws IllegalArgumentException if the class is not an @Entity or the class file cannot be read
     */
    public <T> EntityModel<T> build(Class<T> entityClass) {
        if (warningCollector != null) {
            warningCollector.warnTier3(entityClass.getName());
        }

        String resourceName = "/" + entityClass.getName().replace('.', '/') + ".class";
        byte[] classBytes;
        try (InputStream is = entityClass.getResourceAsStream(resourceName)) {
            if (is == null) {
                throw new IllegalArgumentException("Cannot read class file for " + entityClass.getName());
            }
            classBytes = is.readAllBytes();
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to read class file for " + entityClass.getName(), e);
        }

        ClassModel classModel = ClassFile.of().parse(classBytes);

        if (!hasAnnotation(classModel, JPA_ENTITY)) {
            throw new IllegalArgumentException(
                    "Class " + entityClass.getName() + " is not annotated with @Entity");
        }

        String tableName = getTableAnnotationValue(classModel, "name")
                .filter(n -> !n.isEmpty())
                .orElseGet(() -> toSnakeCase(toPlural(entityClass.getSimpleName())));

        String schema = getTableAnnotationValue(classModel, "schema").orElse("");
        String catalog = getTableAnnotationValue(classModel, "catalog").orElse("");

        // Parse generator annotations from class
        java.util.Map<String, String[]> sequenceGenerators = parseSequenceGenerators(classModel);
        java.util.Map<String, String[]> tableGenerators = parseTableGenerators(classModel);

        // Build entity model first without attributes, then set attributes with back-reference
        RuntimeEntityModel<T> model = new RuntimeEntityModel<>(
                entityClass, tableName, schema, catalog, List.of());

        // Build attributes with correct entity model reference and generator info
        List<Attribute<?, ?>> attrsWithModel = new ArrayList<>();
        for (FieldModel fieldModel : classModel.fields()) {
            if (hasAnnotation(fieldModel, JPA_TRANSIENT)) {
                continue;
            }
            Attribute<T, ?> attr = parseField(fieldModel, entityClass, model, sequenceGenerators, tableGenerators);
            attrsWithModel.add(attr);
        }

        return new RuntimeEntityModel<>(entityClass, tableName, schema, catalog, attrsWithModel);
    }

    @SuppressWarnings("unchecked")
    private <T> Attribute<T, ?> parseField(FieldModel fieldModel, Class<T> entityClass,
                                           EntityModel<T> entityModel,
                                           java.util.Map<String, String[]> sequenceGenerators,
                                           java.util.Map<String, String[]> tableGenerators) {
        return parseField(new FieldParsingContext(fieldModel, entityClass, entityModel, sequenceGenerators, tableGenerators));
    }

    @SuppressWarnings("unchecked")
    private <T> Attribute<T, ?> parseField(FieldParsingContext ctx) {
        String name = ctx.getName();
        Class<?> javaType = ctx.getJavaType();

        boolean isId = ctx.isId();
        boolean isVersion = ctx.isVersion();

        String columnName = ctx.getColumnName();
        boolean nullable = ctx.isNullable();
        boolean unique = ctx.isUnique();
        int length = ctx.getLength();
        int precision = ctx.getPrecision();
        int scale = ctx.getScale();
        boolean insertable = ctx.isInsertable();
        boolean updatable = ctx.isUpdatable();
        String columnDefinition = ctx.getColumnDefinition();

        if (isId) {
            return parseIdAttribute(ctx, name, columnName, javaType, nullable, isVersion, unique,
                    length, precision, scale, insertable, updatable, columnDefinition);
        }

        if (ctx.isReference()) {
            return parseReferenceAttribute(ctx, name, columnName, javaType, nullable, isId, isVersion, unique,
                    length, precision, scale, insertable, updatable, columnDefinition);
        }

        return new RuntimeAttribute<>(
                name, columnName, ctx.entityModel,
                (Class) javaType, nullable, isId, isVersion, unique, true,
                length, precision, scale,
                insertable, updatable, columnDefinition);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private <T> RuntimeIdAttribute<T, ?> parseIdAttribute(FieldParsingContext ctx, String name, String columnName,
                                                      Class<?> javaType, boolean nullable, boolean isVersion,
                                                      boolean unique, int length, int precision, int scale,
                                                      boolean insertable, boolean updatable, String columnDefinition) {
        Optional<String> strategyOpt = ctx.getGeneratedValueStrategy();
        IdAttribute.GenerationStrategy strategy = null;
        String generator = "";
        String sequenceName = "";
        String tableGeneratorTable = "";
        String tablePkColumnName = SEQUENCE_NAME;
        String tableValueColumnName = SEQUENCE_NEXT_VAL;
        String tablePkColumnValue = "";
        
        if (strategyOpt.isPresent()) {
            String strategyStr = strategyOpt.get();
            try {
                strategy = IdAttribute.GenerationStrategy.valueOf(strategyStr);
            } catch (IllegalArgumentException _) {
                strategy = IdAttribute.GenerationStrategy.AUTO;
            }
        }

        // Parse @GeneratedValue generator name
        generator = ctx.getGeneratedValueGenerator().orElse("");

        // For SEQUENCE and TABLE strategies, look up generator metadata
        if (strategy == IdAttribute.GenerationStrategy.SEQUENCE) {
            if (generator.isEmpty()) {
                // No named generator - derive sequence name from table name
                sequenceName = ctx.entityModel != null ? ctx.entityModel.getTableName().toUpperCase() + "_SEQ" : "";
            } else if (ctx.sequenceGenerators.containsKey(generator)) {
                sequenceName = ctx.sequenceGenerators.get(generator)[0];
            }
        } else if (strategy == IdAttribute.GenerationStrategy.TABLE && !generator.isEmpty() && ctx.tableGenerators.containsKey(generator)) {
            String[] tg = ctx.tableGenerators.get(generator);
            tableGeneratorTable = tg[0];
            tablePkColumnName = tg.length > 1 ? tg[1] : SEQUENCE_NAME;
            tableValueColumnName = tg.length > 2 ? tg[2] : SEQUENCE_NEXT_VAL;
            tablePkColumnValue = tg.length > 3 ? tg[3] : generator;
        }
        
        return new RuntimeIdAttribute<>(
                name, columnName, ctx.entityModel,
                (Class) javaType, nullable, isVersion, unique, true,
                length, precision, scale,
                insertable, updatable, columnDefinition,
                strategy, generator,
                sequenceName,
                tableGeneratorTable,
                tablePkColumnName,
                tableValueColumnName,
                tablePkColumnValue);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private <T, V> RuntimeReferenceAttribute<T, V> parseReferenceAttribute(FieldParsingContext ctx, String name, String columnName,
                                                                         Class<V> javaType, boolean nullable, boolean isId, boolean isVersion,
                                                                         boolean unique, int length, int precision, int scale,
                                                                         boolean insertable, boolean updatable, String columnDefinition) {
        // Determine relationship type
        ReferenceAttribute.RelationshipType relationshipType;
        if (hasAnnotation(ctx.fieldModel, JPA_MANY_TO_ONE)) {
            relationshipType = ReferenceAttribute.RelationshipType.MANY_TO_ONE;
        } else if (hasAnnotation(ctx.fieldModel, JPA_ONE_TO_ONE)) {
            relationshipType = ReferenceAttribute.RelationshipType.ONE_TO_ONE;
        } else {
            throw new IllegalStateException("Field " + name + " is marked as reference but has no @ManyToOne or @OneToOne annotation");
        }

        // Determine fetch type (default to EAGER)
        ReferenceAttribute.FetchType fetchType = ReferenceAttribute.FetchType.EAGER;
        Optional<String> fetchTypeOpt = getAnnotationValue(ctx.fieldModel, "Ljakarta/persistence/FetchType;", "value");
        if (fetchTypeOpt.isPresent()) {
            try {
                fetchType = ReferenceAttribute.FetchType.valueOf(fetchTypeOpt.get());
            } catch (IllegalArgumentException e) {
                // Default to EAGER if invalid
                fetchType = ReferenceAttribute.FetchType.EAGER;
            }
        }

        // Determine owning side (ManyToOne is always owning)
        boolean owningSide = relationshipType == ReferenceAttribute.RelationshipType.MANY_TO_ONE;

        // Get referenced entity type from the field's type
        Class<?> referencedClass = ctx.getJavaType();
        EntityModel<V> referencedEntityModel;
        try {
            // Build the referenced entity model using this builder
            referencedEntityModel = (EntityModel<V>) new RuntimeEntityModelBuilder(warningCollector)
                    .build((Class<V>) referencedClass);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to build entity model for referenced class " + referencedClass.getName(), e);
        }

        // Parse JoinColumn annotations
        ReferenceAttribute.JoinColumn[] joinColumns = parseJoinColumns(ctx.fieldModel);

        return new RuntimeReferenceAttribute<>(
                name, columnName, ctx.entityModel,
                (Class) javaType, nullable, unique, isId, isVersion, true,
                length, precision, scale,
                insertable, updatable, columnDefinition,
                relationshipType,
                fetchType,
                owningSide,
                referencedEntityModel,
                joinColumns);
    }

    // --- Annotation helpers ---

    private static List<Annotation> getAnnotations(ClassModel model) {
        List<Annotation> all = new ArrayList<>();
        for (var attr : model.attributes()) {
            if (attr instanceof RuntimeVisibleAnnotationsAttribute rva) {
                all.addAll(rva.annotations());
            }
        }
        return all;
    }

    private static List<Annotation> getAnnotations(FieldModel model) {
        List<Annotation> all = new ArrayList<>();
        for (var attr : model.attributes()) {
            if (attr instanceof RuntimeVisibleAnnotationsAttribute rva) {
                all.addAll(rva.annotations());
            }
        }
        return all;
    }

    private static boolean hasAnnotation(ClassModel model, String internalName) {
        return getAnnotations(model).stream()
                .anyMatch(ann -> ann.className().toString().equals(internalName));
    }

    private static boolean hasAnnotation(FieldModel model, String internalName) {
        return getAnnotations(model).stream()
                .anyMatch(ann -> ann.className().toString().equals(internalName));
    }

    private static Optional<String> getAnnotationValue(ClassModel model, String annotationType,
                                                       String elementName) {
        return getAnnotations(model).stream()
                .filter(ann -> ann.className().toString().equals(annotationType))
                .findFirst()
                .flatMap(ann -> getElementValue(ann, elementName));
    }

    private static Optional<String> getAnnotationValue(FieldModel model, String annotationType,
                                                       String elementName) {
        return getAnnotations(model).stream()
                .filter(ann -> ann.className().toString().equals(annotationType))
                .findFirst()
                .flatMap(ann -> getElementValue(ann, elementName));
    }

    private static Optional<String> getTableAnnotationValue(ClassModel model, String elementName) {
        return getAnnotationValue(model, JPA_TABLE, elementName);
    }

    private static Optional<String> getElementValue(Annotation annotation, String elementName) {
        for (AnnotationElement element : annotation.elements()) {
            if (element.name().toString().equals(elementName)) {
                AnnotationValue value = element.value();
                if (value instanceof AnnotationValue.OfString sv) {
                    return Optional.of(sv.stringValue());
                } else if (value instanceof AnnotationValue.OfBoolean bv) {
                    return Optional.of(String.valueOf(bv.booleanValue()));
                } else if (value instanceof AnnotationValue.OfInt iv) {
                    return Optional.of(String.valueOf(iv.intValue()));
                }
            }
        }
        return Optional.empty();
    }

    private static Optional<String> getGeneratedValueStrategy(FieldModel model) {
        return getAnnotations(model).stream()
                .filter(ann -> ann.className().toString().equals(JPA_GENERATED_VALUE))
                .findFirst()
                .map(ann -> {
                    for (AnnotationElement element : ann.elements()) {
                        if (element.name().toString().equals("strategy")) {
                            AnnotationValue value = element.value();
                            if (value instanceof AnnotationValue.OfEnum ev) {
                                return ev.constantName().toString();
                            }
                        }
                    }
                    // If @GeneratedValue is present but no strategy element, default is AUTO
                    return "AUTO";
                });
    }

    // --- Type conversion ---

    private static Class<?> toJavaType(String internalType, ClassLoader classLoader) {
        return switch (internalType) {
            case "B" -> byte.class;
            case "C" -> char.class;
            case "D" -> double.class;
            case "F" -> float.class;
            case "I" -> int.class;
            case "J" -> long.class;
            case "S" -> short.class;
            case "Z" -> boolean.class;
            case "V" -> void.class;
            default -> {
                if (internalType.startsWith("L") && internalType.endsWith(";")) {
                    String className = internalType.substring(1, internalType.length() - 1)
                            .replace('/', '.');
                    try {
                        yield Class.forName(className, false, classLoader);
                    } catch (ClassNotFoundException e) {
                        throw new IllegalArgumentException("Cannot load type: " + className, e);
                    }
                }
                if (internalType.startsWith("[")) {
                    throw new IllegalArgumentException("Array types not supported in tier-3: " + internalType);
                }
                throw new IllegalArgumentException("Unknown type: " + internalType);
            }
        };
    }

    // --- Naming ---

    private static String toSnakeCase(String s) {
        if (s == null || s.isEmpty()) return s;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (Character.isUpperCase(c) && i > 0 && sb.length() > 0) sb.append('_');
            sb.append(Character.toLowerCase(c));
        }
        return sb.toString();
    }

    private static String toPlural(String s) {
        if (s == null || s.isEmpty()) return s;
        if (s.endsWith("y") && s.length() > 1 && !isVowel(s.charAt(s.length() - 2))) {
            return s.substring(0, s.length() - 1) + "ies";
        }
        if (s.endsWith("s") || s.endsWith("sh") || s.endsWith("ch") ||
            s.endsWith("x") || s.endsWith("z")) {
            return s + "es";
        }
        return s + "s";
    }

    private static boolean isVowel(char c) {
        return c == 'a' || c == 'e' || c == 'i' || c == 'o' || c == 'u';
    }

    private static Optional<String> getGeneratedValueGenerator(FieldModel model) {
        return getAnnotations(model).stream()
                .filter(ann -> ann.className().toString().equals(JPA_GENERATED_VALUE))
                .findFirst()
                .flatMap(ann -> {
                    for (AnnotationElement element : ann.elements()) {
                        if (element.name().toString().equals("generator")) {
                            AnnotationValue value = element.value();
                            if (value instanceof AnnotationValue.OfString sv) {
                                return Optional.of(sv.stringValue());
                            }
                        }
                    }
                    return Optional.empty();
                });
    }

    private static java.util.Map<String, String[]> parseSequenceGenerators(ClassModel model) {
        java.util.Map<String, String[]> result = new java.util.HashMap<>();
        for (Annotation ann : getAnnotations(model)) {
            if (ann.className().toString().equals(JPA_SEQUENCE_GENERATOR)) {
                String name = "";
                String sequenceName = "";
                for (AnnotationElement el : ann.elements()) {
                    String elName = el.name().toString();
                    AnnotationValue val = el.value();
                    if (elName.equals("name") && val instanceof AnnotationValue.OfString sv) {
                        name = sv.stringValue();
                    } else if (elName.equals("sequenceName") && val instanceof AnnotationValue.OfString sv) {
                        sequenceName = sv.stringValue();
                    }
                }
                if (sequenceName.isEmpty()) sequenceName = name;
                result.put(name, new String[]{sequenceName});
            }
        }
        return result;
    }

    private static java.util.Map<String, String[]> parseTableGenerators(ClassModel model) {
        java.util.Map<String, String[]> result = new java.util.HashMap<>();
        for (Annotation ann : getAnnotations(model)) {
            if (ann.className().toString().equals(JPA_TABLE_GENERATOR)) {
                String name = "";
                String table = "";
                String pkColumnName = SEQUENCE_NAME;
                String valueColumnName = SEQUENCE_NEXT_VAL;
                String pkColumnValue = "";
                for (AnnotationElement el : ann.elements()) {
                    String elName = el.name().toString();
                    AnnotationValue val = el.value();
                    if (val instanceof AnnotationValue.OfString sv) {
                        String strVal = sv.stringValue();
                        switch (elName) {
                            case "name" -> name = strVal;
                            case "table" -> table = strVal;
                            case "pkColumnName" -> pkColumnName = strVal;
                            case "valueColumnName" -> valueColumnName = strVal;
                            case "pkColumnValue" -> pkColumnValue = strVal;
                        }
                    }
                }
                if (pkColumnValue.isEmpty()) pkColumnValue = name;
                if (table.isEmpty()) table = "sequence_generator";
                result.put(name, new String[]{table, pkColumnName, valueColumnName, pkColumnValue});
            }
        }
        return result;
    }

    private static ReferenceAttribute.JoinColumn[] parseJoinColumns(FieldModel fieldModel) {
        Optional<Annotation> joinColumnAnnotation = getAnnotations(fieldModel).stream()
                .filter(ann -> ann.className().toString().equals(JPA_JOIN_COLUMN))
                .findFirst();

        if (joinColumnAnnotation.isEmpty()) {
            // No @JoinColumn annotation, return empty array
            return new ReferenceAttribute.JoinColumn[0];
        }

        Annotation ann = joinColumnAnnotation.get();
        String name = "";
        String referencedColumnName = "";
        boolean nullable = true;
        boolean unique = false;
        boolean insertable = true;
        boolean updatable = true;

        for (AnnotationElement element : ann.elements()) {
            String elName = element.name().toString();
            AnnotationValue val = element.value();
            if (val instanceof AnnotationValue.OfString sv) {
                String strVal = sv.stringValue();
                switch (elName) {
                    case "name" -> name = strVal;
                    case "referencedColumnName" -> referencedColumnName = strVal;
                    case "columnDefinition" -> { /* ignore, not in JoinColumn */ }
                }
            } else if (val instanceof AnnotationValue.OfBoolean bv) {
                boolean boolVal = bv.booleanValue();
                switch (elName) {
                    case "nullable" -> nullable = boolVal;
                    case "unique" -> unique = boolVal;
                    case "insertable" -> insertable = boolVal;
                    case "updatable" -> updatable = boolVal;
                }
            }
        }

        ReferenceAttribute.JoinColumn joinColumn = new ReferenceAttribute.JoinColumn(
                name,
                referencedColumnName,
                nullable,
                unique,
                insertable,
                updatable
        );

        return new ReferenceAttribute.JoinColumn[] { joinColumn };
    }
}

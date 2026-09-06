/*
 * SPDX-License-Identifier: EPL-2.0 OR EUPL-1.2 OR GPL-2.0-or-later
 */
package io.vidocq.mansart.persistence.core.runtime;

import io.vidocq.mansart.persistence.spi.Attribute;
import io.vidocq.mansart.persistence.spi.EntityModel;
import io.vidocq.mansart.persistence.spi.IdAttribute;

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

    private static final String SEQUENCE_NAME = "SEQUENCE_NAME";
    private static final String SEQUENCE_NEXT_VAL = "SEQUENCE_NEXT_VAL";

    private final Tier3WarningCollector warningCollector;

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

        List<Attribute<?, ?>> attributes = new ArrayList<>();
        for (FieldModel fieldModel : classModel.fields()) {
            if (hasAnnotation(fieldModel, JPA_TRANSIENT)) {
                continue;
            }
            Attribute<T, ?> attr = parseField(fieldModel, entityClass);
            if (attr != null) {
                attributes.add(attr);
            }
        }

        // Parse generator annotations from class
        java.util.Map<String, String[]> sequenceGenerators = parseSequenceGenerators(classModel);
        java.util.Map<String, String[]> tableGenerators = parseTableGenerators(classModel);

        // Build entity model first without attributes, then set attributes with back-reference
        RuntimeEntityModel<T> model = new RuntimeEntityModel<>(
                entityClass, tableName, schema, catalog, List.of());

        // Rebuild attributes with correct entity model reference and generator info
        List<Attribute<?, ?>> attrsWithModel = new ArrayList<>();
        for (FieldModel fieldModel : classModel.fields()) {
            if (hasAnnotation(fieldModel, JPA_TRANSIENT)) {
                continue;
            }
            Attribute<T, ?> attr = parseField(fieldModel, entityClass, model, sequenceGenerators, tableGenerators);
            if (attr != null) {
                attrsWithModel.add(attr);
            }
        }

        return new RuntimeEntityModel<>(entityClass, tableName, schema, catalog, attrsWithModel);
    }

    @SuppressWarnings("unchecked")
    private <T> Attribute<T, ?> parseField(FieldModel fieldModel, Class<T> entityClass) {
        return parseField(fieldModel, entityClass, null, java.util.Map.of(), java.util.Map.of());
    }

    @SuppressWarnings("unchecked")
    private <T> Attribute<T, ?> parseField(FieldModel fieldModel, Class<T> entityClass,
                                           EntityModel<T> entityModel) {
        return parseField(fieldModel, entityClass, entityModel, java.util.Map.of(), java.util.Map.of());
    }

    @SuppressWarnings("unchecked")
    private <T> Attribute<T, ?> parseField(FieldModel fieldModel, Class<T> entityClass,
                                           EntityModel<T> entityModel,
                                           java.util.Map<String, String[]> sequenceGenerators,
                                           java.util.Map<String, String[]> tableGenerators) {
        String name = fieldModel.fieldName().toString();
        String internalType = fieldModel.fieldType().toString();

        Class<?> javaType = toJavaType(internalType, entityClass.getClassLoader());

        boolean isId = hasAnnotation(fieldModel, JPA_ID);
        boolean isVersion = hasAnnotation(fieldModel, JPA_VERSION);

        String columnName = getAnnotationValue(fieldModel, JPA_COLUMN, "name")
                .filter(n -> !n.isEmpty())
                .orElseGet(() -> toSnakeCase(name));

        boolean nullable = getAnnotationValue(fieldModel, JPA_COLUMN, "nullable")
                .map(Boolean::parseBoolean)
                .orElse(true);

        boolean unique = getAnnotationValue(fieldModel, JPA_COLUMN, "unique")
                .map(Boolean::parseBoolean)
                .orElse(false);

        if (isId) {
            Optional<String> strategyOpt = getGeneratedValueStrategy(fieldModel);
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
            generator = getGeneratedValueGenerator(fieldModel).orElse("");
            
            // For SEQUENCE and TABLE strategies, look up generator metadata
            if (strategy == IdAttribute.GenerationStrategy.SEQUENCE && generator.isEmpty()) {
                // No named generator - derive sequence name from table name
                sequenceName = entityModel != null ? entityModel.getTableName().toUpperCase() + "_SEQ" : "";
            } else if (strategy == IdAttribute.GenerationStrategy.SEQUENCE && !generator.isEmpty() && sequenceGenerators.containsKey(generator)) {
                sequenceName = sequenceGenerators.get(generator)[0];
            } else if (strategy == IdAttribute.GenerationStrategy.TABLE && !generator.isEmpty() && tableGenerators.containsKey(generator)) {
                String[] tg = tableGenerators.get(generator);
                tableGeneratorTable = tg[0];
                tablePkColumnName = tg.length > 1 ? tg[1] : SEQUENCE_NAME;
                tableValueColumnName = tg.length > 2 ? tg[2] : SEQUENCE_NEXT_VAL;
                tablePkColumnValue = tg.length > 3 ? tg[3] : generator;
            }
            
            return new RuntimeIdAttribute<>(
                    name, columnName, entityModel,
                    (Class) javaType, nullable, isVersion, unique, true,
                    strategy, generator,
                    sequenceName,
                    tableGeneratorTable,
                    tablePkColumnName,
                    tableValueColumnName,
                    tablePkColumnValue);
        }

        return new RuntimeAttribute<>(
                name, columnName, entityModel,
                (Class) javaType, nullable, isId, isVersion, unique, true);
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
}

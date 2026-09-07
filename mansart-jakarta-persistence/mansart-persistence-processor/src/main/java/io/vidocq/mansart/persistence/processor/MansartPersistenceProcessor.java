/*
 * SPDX-License-Identifier: EPL-2.0 OR EUPL-1.2 OR GPL-2.0-or-later
 */
package io.vidocq.mansart.persistence.processor;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Annotation processor for Mansart Jakarta Persistence 3.2.
 * Generates _Entity classes with typed Attribute subtypes.
 */
@SupportedAnnotationTypes({
    "jakarta.persistence.Entity",
    "jakarta.persistence.Id",
    "jakarta.persistence.GeneratedValue",
    "jakarta.persistence.Column",
    "jakarta.persistence.Table",
    "jakarta.persistence.Version",
    "jakarta.persistence.ManyToOne",
    "jakarta.persistence.OneToOne",
    "jakarta.persistence.JoinColumn",
    "jakarta.persistence.Enumerated",
    "jakarta.persistence.Embedded",
    "jakarta.persistence.Embeddable"
})
@SupportedSourceVersion(SourceVersion.RELEASE_25)
public class MansartPersistenceProcessor extends AbstractProcessor {

    private Elements elementUtils;
    private Types typeUtils;
    private Filer filer;
    private Messager messager;
    private final Set<TypeElement> processedEntities = new LinkedHashSet<>();
    private final Set<String> generatedClasses = new LinkedHashSet<>();

    @Override
    public synchronized void init(javax.annotation.processing.ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        elementUtils = processingEnv.getElementUtils();
        typeUtils = processingEnv.getTypeUtils();
        filer = processingEnv.getFiler();
        messager = processingEnv.getMessager();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (elementUtils == null || typeUtils == null || filer == null || messager == null) {
            return false;
        }

        TypeElement entityAnnotation = elementUtils.getTypeElement("jakarta.persistence.Entity");
        if (entityAnnotation == null) {
            return false;
        }

        Set<? extends Element> entityElements = roundEnv.getElementsAnnotatedWith(entityAnnotation);
        List<TypeElement> newEntities = new ArrayList<>();
        for (Element element : entityElements) {
            if (element instanceof TypeElement typeElement) {
                if (processedEntities.add(typeElement)) {
                    newEntities.add(typeElement);
                }
            }
        }

        for (TypeElement entity : newEntities) {
            EntityMetadata metadata = scanEntity(entity);
            if (metadata != null) {
                generateEntityClass(metadata);
            }
        }

        return !annotations.isEmpty();
    }

    private EntityMetadata scanEntity(TypeElement entity) {
        String packageName = elementUtils.getPackageOf(entity).getQualifiedName().toString();
        String entityName = entity.getSimpleName().toString();
        String tableName = getTableName(entity);
        String schema = getSchema(entity);
        String catalog = getCatalog(entity);

        List<FieldMetadata> fields = new ArrayList<>();
        for (Element enclosed : entity.getEnclosedElements()) {
            if (enclosed instanceof VariableElement field) {
                if (field.getModifiers().contains(Modifier.STATIC) ||
                    field.getModifiers().contains(Modifier.FINAL)) {
                    continue;
                }
                FieldMetadata fieldMetadata = scanField(field);
                if (fieldMetadata != null) {
                    fields.add(fieldMetadata);
                }
            }
        }

        return new EntityMetadata(packageName, entityName, entity.getQualifiedName().toString(), tableName, schema, catalog, fields);
    }

    private String getTableName(TypeElement entity) {
        TypeElement tableAnnot = elementUtils.getTypeElement("jakarta.persistence.Table");
        if (tableAnnot != null) {
            AnnotationMirror mirror = getAnnotationMirror(entity, tableAnnot);
            if (mirror != null) {
                String name = getAnnotationValue(mirror, "name");
                if (name != null && !name.isEmpty()) return name;
            }
        }
        return toSnakeCase(toPlural(entity.getSimpleName().toString()));
    }

    private String getSchema(TypeElement entity) {
        TypeElement tableAnnot = elementUtils.getTypeElement("jakarta.persistence.Table");
        if (tableAnnot != null) {
            AnnotationMirror mirror = getAnnotationMirror(entity, tableAnnot);
            if (mirror != null) {
                String schema = getAnnotationValue(mirror, "schema");
                if (schema != null && !schema.isEmpty()) return schema;
            }
        }
        return "";
    }

    private String getCatalog(TypeElement entity) {
        TypeElement tableAnnot = elementUtils.getTypeElement("jakarta.persistence.Table");
        if (tableAnnot != null) {
            AnnotationMirror mirror = getAnnotationMirror(entity, tableAnnot);
            if (mirror != null) {
                String catalog = getAnnotationValue(mirror, "catalog");
                if (catalog != null && !catalog.isEmpty()) return catalog;
            }
        }
        return "";
    }

    private String toSnakeCase(String s) {
        if (s == null || s.isEmpty()) return s;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (Character.isUpperCase(c) && i > 0) sb.append('_');
            sb.append(Character.toLowerCase(c));
        }
        return sb.toString();
    }

    private String toPlural(String s) {
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

    private boolean isVowel(char c) {
        return c == 'a' || c == 'e' || c == 'i' || c == 'o' || c == 'u' || 
               c == 'A' || c == 'E' || c == 'I' || c == 'O' || c == 'U';
    }

    private String determineGenerationStrategy(VariableElement field, boolean isId) {
        TypeElement generatedValueAnnot = elementUtils.getTypeElement("jakarta.persistence.GeneratedValue");
        if (generatedValueAnnot != null) {
            AnnotationMirror mirror = getAnnotationMirror(field, generatedValueAnnot);
            if (mirror != null) {
                String strategy = getAnnotationValue(mirror, "strategy");
                if (strategy != null && !strategy.isEmpty()) {
                    return strategy;
                }
            }
        }
        // If @GeneratedValue is absent but it's an @Id field, default to AUTO per JPA spec
        if (isId) {
            return "AUTO";
        }
        return null;
    }

    private String determineVersionStrategy(VariableElement field, String type) {
        if (isNumeric(type)) {
            return "NUMERIC";
        }
        if (isTemporal(type)) {
            return "TIMESTAMP";
        }
        return null;
    }

    private FieldMetadata scanField(VariableElement field) {
        String name = field.getSimpleName().toString();
        String type = field.asType().toString();
        String column = getColumnName(field);
        boolean nullable = isNullable(field);
        boolean unique = isUnique(field);
        boolean isId = hasAnnotation(field, "jakarta.persistence.Id");
        boolean isVersion = hasAnnotation(field, "jakarta.persistence.Version");
        boolean isGenerated = hasAnnotation(field, "jakarta.persistence.GeneratedValue");
        boolean isManyToOne = hasAnnotation(field, "jakarta.persistence.ManyToOne");
        boolean isOneToOne = hasAnnotation(field, "jakarta.persistence.OneToOne");
        boolean isEnumerated = hasAnnotation(field, "jakarta.persistence.Enumerated");
        boolean isEmbedded = hasAnnotation(field, "jakarta.persistence.Embedded");

        if (hasAnnotation(field, "jakarta.persistence.Transient")) {
            return null;
        }

        AttributeType attrType = determineType(type, isId, isVersion, isManyToOne, isOneToOne, isEnumerated, isEmbedded);
        int length = getColumnLength(field);
        int precision = getColumnPrecision(field);
        int scale = getColumnScale(field);
        boolean insertable = isInsertable(field);
        boolean updatable = isUpdatable(field);
        String columnDefinition = getColumnDefinition(field);
        String generationStrategy = determineGenerationStrategy(field, isId);
        String versionStrategy = determineVersionStrategy(field, type);
        return new FieldMetadata(name, type, column, isId, isVersion, isGenerated, nullable, unique, attrType, length, precision, scale, insertable, updatable, columnDefinition, generationStrategy, versionStrategy);
    }

    private String getColumnName(VariableElement field) {
        // Check @JoinColumn first for relationship fields
        TypeElement joinColAnnot = elementUtils.getTypeElement("jakarta.persistence.JoinColumn");
        if (joinColAnnot != null) {
            AnnotationMirror mirror = getAnnotationMirror(field, joinColAnnot);
            if (mirror != null) {
                String name = getAnnotationValue(mirror, "name");
                if (name != null && !name.isEmpty()) return name;
            }
        }
        
        // Check @Column for regular fields
        TypeElement colAnnot = elementUtils.getTypeElement("jakarta.persistence.Column");
        if (colAnnot != null) {
            AnnotationMirror mirror = getAnnotationMirror(field, colAnnot);
            if (mirror != null) {
                String name = getAnnotationValue(mirror, "name");
                if (name != null && !name.isEmpty()) return name;
            }
        }
        
        // For relationship fields without explicit @JoinColumn, generate FK column name
        if (hasAnnotation(field, "jakarta.persistence.ManyToOne") || 
            hasAnnotation(field, "jakarta.persistence.OneToOne")) {
            String fieldType = field.asType().toString();
            // Extract simple class name from fully qualified name
            String simpleType = fieldType;
            int lastDot = fieldType.lastIndexOf('.');
            if (lastDot >= 0) {
                simpleType = fieldType.substring(lastDot + 1);
            }
            return toSnakeCase(simpleType) + "_id";
        }
        
        // Default naming for regular fields
        return toSnakeCase(field.getSimpleName().toString());
    }

    private boolean isNullable(VariableElement field) {
        TypeElement colAnnot = elementUtils.getTypeElement("jakarta.persistence.Column");
        if (colAnnot != null) {
            AnnotationMirror mirror = getAnnotationMirror(field, colAnnot);
            if (mirror != null) {
                String nullableStr = getAnnotationValue(mirror, "nullable");
                if (nullableStr != null) return Boolean.parseBoolean(nullableStr);
            }
        }
        String type = field.asType().toString();
        return !type.equals("boolean") && !type.equals("byte") && !type.equals("char") &&
               !type.equals("short") && !type.equals("int") && !type.equals("long") &&
               !type.equals("float") && !type.equals("double");
    }

    private boolean isUnique(VariableElement field) {
        TypeElement colAnnot = elementUtils.getTypeElement("jakarta.persistence.Column");
        if (colAnnot != null) {
            AnnotationMirror mirror = getAnnotationMirror(field, colAnnot);
            if (mirror != null) {
                String uniqueStr = getAnnotationValue(mirror, "unique");
                if (uniqueStr != null) return Boolean.parseBoolean(uniqueStr);
            }
        }
        return false;
    }

    private int getColumnLength(VariableElement field) {
        TypeElement colAnnot = elementUtils.getTypeElement("jakarta.persistence.Column");
        if (colAnnot != null) {
            AnnotationMirror mirror = getAnnotationMirror(field, colAnnot);
            if (mirror != null) {
                String lengthStr = getAnnotationValue(mirror, "length");
                if (lengthStr != null) return Integer.parseInt(lengthStr);
                return 255; // spec default when @Column is present
            }
        }
        return -1; // @Column absent
    }

    private int getColumnPrecision(VariableElement field) {
        TypeElement colAnnot = elementUtils.getTypeElement("jakarta.persistence.Column");
        if (colAnnot != null) {
            AnnotationMirror mirror = getAnnotationMirror(field, colAnnot);
            if (mirror != null) {
                String precisionStr = getAnnotationValue(mirror, "precision");
                if (precisionStr != null) return Integer.parseInt(precisionStr);
                return 0; // spec default when @Column is present
            }
        }
        return -1; // @Column absent
    }

    private int getColumnScale(VariableElement field) {
        TypeElement colAnnot = elementUtils.getTypeElement("jakarta.persistence.Column");
        if (colAnnot != null) {
            AnnotationMirror mirror = getAnnotationMirror(field, colAnnot);
            if (mirror != null) {
                String scaleStr = getAnnotationValue(mirror, "scale");
                if (scaleStr != null) return Integer.parseInt(scaleStr);
                return 0; // spec default when @Column is present
            }
        }
        return -1; // @Column absent
    }

    private boolean isInsertable(VariableElement field) {
        TypeElement colAnnot = elementUtils.getTypeElement("jakarta.persistence.Column");
        if (colAnnot != null) {
            AnnotationMirror mirror = getAnnotationMirror(field, colAnnot);
            if (mirror != null) {
                String insertableStr = getAnnotationValue(mirror, "insertable");
                if (insertableStr != null) return Boolean.parseBoolean(insertableStr);
                return true; // spec default when @Column is present
            }
        }
        return true; // @Column absent
    }

    private boolean isUpdatable(VariableElement field) {
        TypeElement colAnnot = elementUtils.getTypeElement("jakarta.persistence.Column");
        if (colAnnot != null) {
            AnnotationMirror mirror = getAnnotationMirror(field, colAnnot);
            if (mirror != null) {
                String updatableStr = getAnnotationValue(mirror, "updatable");
                if (updatableStr != null) return Boolean.parseBoolean(updatableStr);
                return true; // spec default when @Column is present
            }
        }
        return true; // @Column absent
    }

    private String getColumnDefinition(VariableElement field) {
        TypeElement colAnnot = elementUtils.getTypeElement("jakarta.persistence.Column");
        if (colAnnot != null) {
            AnnotationMirror mirror = getAnnotationMirror(field, colAnnot);
            if (mirror != null) {
                String columnDefinition = getAnnotationValue(mirror, "columnDefinition");
                if (columnDefinition != null && !columnDefinition.isEmpty()) return columnDefinition;
                return ""; // spec default when @Column is present
            }
        }
        return ""; // @Column absent
    }

    private AttributeType determineType(String type, boolean isId, boolean isVersion,
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

    private boolean isTemporal(String type) {
        return type.equals("java.util.Date") || type.equals("java.time.LocalDate") ||
               type.equals("java.time.LocalDateTime") || type.equals("java.time.LocalTime") ||
               type.equals("java.time.Instant") || type.equals("java.sql.Date") ||
               type.equals("java.sql.Time") || type.equals("java.sql.Timestamp");
    }

    private boolean isNumeric(String type) {
        return type.equals("java.lang.Integer") || type.equals("java.lang.Long") ||
               type.equals("java.lang.Double") || type.equals("java.lang.Float") ||
               type.equals("int") || type.equals("long") || type.equals("double") ||
               type.equals("float") || type.equals("java.math.BigInteger") ||
               type.equals("java.math.BigDecimal");
    }

    private AnnotationMirror getAnnotationMirror(Element element, TypeElement annotationType) {
        if (annotationType == null) return null;
        String annotationTypeName = annotationType.getQualifiedName().toString();
        for (AnnotationMirror m : element.getAnnotationMirrors()) {
            if (m.getAnnotationType().asElement() instanceof TypeElement te
                    && te.getQualifiedName().contentEquals(annotationTypeName)) {
                return m;
            }
        }
        return null;
    }

    private String getAnnotationValue(AnnotationMirror mirror, String key) {
        return elementUtils.getElementValuesWithDefaults(mirror).entrySet().stream()
            .filter(e -> e.getKey().getSimpleName().toString().equals(key))
            .findFirst()
            .map(e -> e.getValue().getValue().toString())
            .orElse(null);
    }

    private boolean hasAnnotation(Element element, String name) {
        TypeElement annotType = elementUtils.getTypeElement(name);
        return annotType != null && getAnnotationMirror(element, annotType) != null;
    }

    private void generateEntityClass(EntityMetadata metadata) {
        String className = "_" + metadata.entityName();
        String pkg = metadata.packageName();
        String fullName = pkg + "." + className;
        if (!generatedClasses.add(fullName)) {
            return;
        }
        try {
            JavaFileObject file = filer.createSourceFile(fullName);
            try (Writer w = file.openWriter()) {
                generateEntityClassContent(w, className, pkg, metadata);
            }
        } catch (IOException e) {
            messager.printMessage(Diagnostic.Kind.ERROR, "Failed to generate _" + metadata.entityName() + ": " + e.getMessage());
        }
    }

    private void generateEntityClassContent(Writer w, String className, String pkg, EntityMetadata metadata) throws IOException {
        w.write("package " + pkg + ";\n\n");
        w.write("import io.vidocq.mansart.persistence.spi.*;\n");
        w.write("import java.util.*;\n");
        w.write("import java.util.stream.Collectors;\n\n");

        w.write("public final class " + className + "<T> implements EntityModel<T> {\n\n");

        // Fields
        w.write("    private final Class<T> entityClass;\n");
        w.write("    private final String tableName;\n");
        w.write("    private final String schema;\n");
        w.write("    private final String catalog;\n");
        w.write("    private final List<Attribute<T, ?>> attributes;\n\n");

        // Constructor
        w.write("    @SuppressWarnings(\"unchecked\")\n");
        w.write("    public " + className + "() {\n");
        w.write("        this((Class<T>) (Class<?>) " + metadata.entityQualifiedName() + ".class);\n");
        w.write("    }\n\n");
        w.write("    @SuppressWarnings(\"unchecked\")\n");
        w.write("    public " + className + "(Class<T> entityClass) {\n");
        w.write("        this.entityClass = Objects.requireNonNull(entityClass);\n");
        w.write("        this.tableName = \"" + metadata.tableName() + "\";\n");
        w.write("        this.schema = \"" + metadata.schema() + "\";\n");
        w.write("        this.catalog = \"" + metadata.catalog() + "\";\n");
        w.write("        this.attributes = buildAttributes();\n");
        w.write("    }\n\n");

        // buildAttributes
        w.write("    private List<Attribute<T, ?>> buildAttributes() {\n");
        w.write("        List<Attribute<T, ?>> attrs = new ArrayList<>();\n");

        for (FieldMetadata f : metadata.fields()) {
            w.write("        attrs.add(new " + getAttrImplClass(f) + "<T, " + boxType(f.type()) + ">(\n");
            w.write("            \"" + f.name() + "\", \"" + f.column() + "\", this,\n");
            w.write("            " + f.nullable() + ", " + f.unique() + ", " + f.isId() + ", " + f.isVersion() + ",\n");
            w.write("            " + f.length() + ", " + f.precision() + ", " + f.scale() + ",\n");
            w.write("            " + f.insertable() + ", " + f.updatable() + ", \"" + f.columnDefinition() + "\",\n");
            w.write("            \"" + (f.generationStrategy() != null ? f.generationStrategy() : "") + "\",\n");
            w.write("            \"" + (f.versionStrategy() != null ? f.versionStrategy() : "") + "\"\n");
            w.write("        ));\n");
        }

        w.write("        return Collections.unmodifiableList(attrs);\n");
        w.write("    }\n\n");

        // Generate inner attribute classes
        for (FieldMetadata f : metadata.fields()) {
            generateInnerAttr(w, f);
        }

        // EntityModel methods
        w.write("    @Override public Class<T> getEntityClass() { return entityClass; }\n");
        w.write("    @Override public String getTableName() { return tableName; }\n");
        w.write("    @Override public String getSchema() { return schema; }\n");
        w.write("    @Override public String getCatalog() { return catalog; }\n");
        w.write("    @Override public List<Attribute<?, ?>> getAttributes() { return (List) attributes; }\n");
        w.write("    @Override public Attribute<?, ?> getAttribute(String name) {\n");
        w.write("        return attributes.stream().filter(a -> a.getName().equals(name)).findFirst().orElse(null);\n");
        w.write("    }\n");
        w.write("    @Override public List<Attribute<?, ?>> getIdAttributes() {\n");
        w.write("        return attributes.stream().filter(a -> a.isId()).collect(Collectors.toList());\n");
        w.write("    }\n");
        w.write("    @Override public Attribute<?, ?> getVersionAttribute() {\n");
        w.write("        return attributes.stream().filter(a -> a.isVersion()).findFirst().orElse(null);\n");
        w.write("    }\n");

        w.write("}\n");
    }

    private void generateStandardMetamodelClass(EntityMetadata metadata) {
        String className = metadata.entityName() + "_";
        String pkg = metadata.packageName();
        String fullName = pkg + "." + className;

        try {
            JavaFileObject file = filer.createSourceFile(fullName);
            try (Writer w = file.openWriter()) {
                generateMetamodelClassContent(w, className, pkg, metadata);
            }
        } catch (IOException e) {
            messager.printMessage(Diagnostic.Kind.ERROR, "Failed to generate " + className + ": " + e.getMessage());
        }
    }

    private void generateMetamodelClassContent(Writer w, String className, String pkg, EntityMetadata metadata) throws IOException {
        String entityClassName = metadata.entityName();

        w.write("package " + pkg + ";\n\n");
        w.write("import jakarta.annotation.Generated;\n");
        w.write("import jakarta.persistence.metamodel.SingularAttribute;\n");
        w.write("import jakarta.persistence.metamodel.StaticMetamodel;\n\n");

        w.write("@Generated(value = \"io.vidocq.mansart.persistence.processor.MansartPersistenceProcessor\")\n");
        w.write("@StaticMetamodel(value = " + pkg + "." + entityClassName + ".class)\n");
        w.write("public abstract class " + className + " {\n\n");

        for (FieldMetadata f : metadata.fields()) {
            w.write("    public static volatile SingularAttribute<" + entityClassName + ", " + f.type() + "> " + f.name() + ";\n");
        }

        w.write("\n");
        w.write("    public static final String ID = \"" + getIdFieldName(metadata) + "\";\n\n");

        w.write("    @SuppressWarnings(\"all\")\n");
        w.write("    public static void setAccessors() {\n");
        w.write("        // MethodHandles will be set via reflection by the persistence provider\n");
        w.write("    }\n");

        w.write("}\n");
    }

    private String getIdFieldName(EntityMetadata metadata) {
        for (FieldMetadata f : metadata.fields()) {
            if (f.isId()) {
                return f.name();
            }
        }
        return "";
    }

    private String getAttrImplClass(FieldMetadata f) {
        String suffix = capitalize(f.name());
        if (f.isId()) return "IdAttr_" + suffix;
        if (f.isVersion()) return "VersionAttr_" + suffix;
        switch (f.attributeType()) {
            case REFERENCE: return "RefAttr_" + suffix;
            case ENUM: return "EnumAttr_" + suffix;
            case EMBEDDED: return "EmbeddedAttr_" + suffix;
            case TEMPORAL: return "TemporalAttr_" + suffix;
            case NUMERIC: return "NumericAttr_" + suffix;
            default: return "BasicAttr_" + suffix;
        }
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private String getAttrInterface(FieldMetadata f) {
        if (f.isId()) return "IdAttribute";
        if (f.isVersion()) return "VersionAttribute";
        switch (f.attributeType()) {
            case REFERENCE: return "ReferenceAttribute";
            case ENUM: return "EnumAttribute";
            case EMBEDDED: return "EmbeddedAttribute";
            case TEMPORAL: return "TemporalAttribute";
            case NUMERIC: return "NumericAttribute";
            default: return "BasicAttribute";
        }
    }

    private void generateInnerAttr(Writer w, FieldMetadata f) throws IOException {
        String name = getAttrImplClass(f);
        String iface = getAttrInterface(f);
        String fieldType = f.type();
        String fieldName = f.name();
        String vBound = "";
        if ("NumericAttribute".equals(iface)) {
            vBound = " extends Number";
        } else if ("EnumAttribute".equals(iface)) {
            vBound = " extends Enum<V>";
        }

        w.write("    private static final class " + name + "<T, V" + vBound + "> implements " + iface + "<T, V> {\n");
        w.write("        private final String name, columnName;\n");
        w.write("        private final EntityModel<T> entityModel;\n");
        w.write("        private final boolean nullable, unique, isId, isVersion;\n");
        w.write("        private final int length, precision, scale;\n");
        w.write("        private final boolean insertable, updatable;\n");
        w.write("        private final String columnDefinition;\n");
        w.write("        private final String generationStrategy;\n");
        w.write("        private final String versionStrategy;\n");
        w.write("        private final java.lang.invoke.MethodHandle getter, setter;\n\n");

        w.write("        @SuppressWarnings(\"unchecked\")\n");
        w.write("        public " + name + "(String name, String columnName, EntityModel<T> entityModel,\n");
        w.write("                boolean nullable, boolean unique, boolean isId, boolean isVersion,\n");
        w.write("                int length, int precision, int scale,\n");
        w.write("                boolean insertable, boolean updatable, String columnDefinition,\n");
        w.write("                String generationStrategy, String versionStrategy) {\n");
        w.write("            this.name = name; this.columnName = columnName;\n");
        w.write("            this.entityModel = entityModel;\n");
        w.write("            this.nullable = nullable; this.unique = unique;\n");
        w.write("            this.isId = isId; this.isVersion = isVersion;\n");
        w.write("            this.length = length; this.precision = precision; this.scale = scale;\n");
        w.write("            this.insertable = insertable; this.updatable = updatable;\n");
        w.write("            this.columnDefinition = columnDefinition;\n");
        w.write("            this.generationStrategy = generationStrategy;\n");
        w.write("            this.versionStrategy = versionStrategy;\n");
        w.write("            this.getter = resolveGetter();\n");
        w.write("            this.setter = resolveSetter();\n");
        w.write("        }\n\n");;

        w.write("        @Override public String getName() { return name; }\n");
        w.write("        @Override public String getColumnName() { return columnName; }\n");
        w.write("        @Override public EntityModel<T> getEntityModel() { return entityModel; }\n");
        w.write("        @Override @SuppressWarnings(\"unchecked\") public Class<V> getJavaType() { return (Class<V>) (Class<?>) " + fieldType + ".class; }\n");
        w.write("        @Override public boolean isNullable() { return nullable; }\n");
        w.write("        @Override public boolean isUnique() { return unique; }\n");
        w.write("        @Override public boolean isId() { return isId; }\n");
        w.write("        @Override public boolean isVersion() { return isVersion; }\n");
        w.write("        @Override public boolean isColumn() { return true; }\n");
        w.write("        @Override public int getLength() { return length; }\n");
        w.write("        @Override public int getPrecision() { return precision; }\n");
        w.write("        @Override public int getScale() { return scale; }\n");
        w.write("        @Override public boolean isInsertable() { return insertable; }\n");
        w.write("        @Override public boolean isUpdatable() { return updatable; }\n");
        w.write("        @Override public String getColumnDefinition() { return columnDefinition; }\n");
        
        // Generate getGenerationStrategy() for IdAttribute
        if (f.isId()) {
            String strategy = f.generationStrategy();
            if (strategy != null) {
                w.write("        @Override public IdAttribute.GenerationStrategy getGenerationStrategy() { return IdAttribute.GenerationStrategy." + strategy + "; }\n");
            }
        }
        
        // Generate getVersionStrategy() for VersionAttribute
        if (f.isVersion()) {
            String strategy = f.versionStrategy();
            if (strategy != null) {
                w.write("        @Override public VersionAttribute.VersionStrategy getVersionStrategy() { return VersionAttribute.VersionStrategy." + strategy + "; }\n");
            }
        }
        
        w.write("        @Override public java.lang.invoke.MethodHandle getGetter() { return getter; }\n");
        w.write("        @Override public java.lang.invoke.MethodHandle getSetter() { return setter; }\n");
        w.write("        @Override @SuppressWarnings(\"unchecked\") public V get(T instance) {\n");
        w.write("            try { return (V) getter.invoke(instance); }\n");
        w.write("            catch (Throwable e) { throw new RuntimeException(e); }\n");
        w.write("        }\n");
        w.write("        @Override public void set(T instance, V value) {\n");
        w.write("            try { setter.invoke(instance, value); }\n");
        w.write("            catch (Throwable e) { throw new RuntimeException(e); }\n");
        w.write("        }\n");
        w.write("\n");
        w.write("        private java.lang.invoke.MethodHandle resolveGetter() {\n");
        w.write("            try {\n");
        w.write("                java.lang.invoke.MethodHandles.Lookup lookup =\n");
        w.write("                    java.lang.invoke.MethodHandles.privateLookupIn(\n");
        w.write("                        entityModel.getEntityClass(),\n");
        w.write("                        java.lang.invoke.MethodHandles.lookup()\n");
        w.write("                    );\n");
        w.write("                return lookup.findGetter(\n");
        w.write("                    entityModel.getEntityClass(), \"" + fieldName + "\", " + fieldType + ".class\n");
        w.write("                );\n");
        w.write("            } catch (Exception e) {\n");
        w.write("                throw new RuntimeException(\"Failed to resolve getter for \" + name, e);\n");
        w.write("            }\n");
        w.write("        }\n\n");;
        w.write("        private java.lang.invoke.MethodHandle resolveSetter() {\n");
        w.write("            try {\n");
        w.write("                java.lang.invoke.MethodHandles.Lookup lookup =\n");
        w.write("                    java.lang.invoke.MethodHandles.privateLookupIn(\n");
        w.write("                        entityModel.getEntityClass(),\n");
        w.write("                        java.lang.invoke.MethodHandles.lookup()\n");
        w.write("                    );\n");
        w.write("                return lookup.findSetter(\n");
        w.write("                    entityModel.getEntityClass(), \"" + fieldName + "\", " + fieldType + ".class\n");
        w.write("                );\n");
        w.write("            } catch (Exception e) {\n");
        w.write("                throw new RuntimeException(\"Failed to resolve setter for \" + name, e);\n");
        w.write("            }\n");
        w.write("        }\n\n");

        w.write("    }\n\n");
    }

    private static String boxType(String type) {
        return switch (type) {
            case "int" -> "java.lang.Integer";
            case "long" -> "java.lang.Long";
            case "double" -> "java.lang.Double";
            case "float" -> "java.lang.Float";
            case "short" -> "java.lang.Short";
            case "byte" -> "java.lang.Byte";
            case "boolean" -> "java.lang.Boolean";
            case "char" -> "java.lang.Character";
            default -> type;
        };
    }

    private record EntityMetadata(
        String packageName, String entityName, String entityQualifiedName, String tableName,
        String schema, String catalog, List<FieldMetadata> fields
    ) {}

    private record FieldMetadata(
        String name, String type, String column,
        boolean isId, boolean isVersion, boolean isGenerated,
        boolean nullable, boolean unique, AttributeType attributeType,
        int length, int precision, int scale,
        boolean insertable, boolean updatable, String columnDefinition,
        String generationStrategy, String versionStrategy
    ) {}

    private enum AttributeType {
        ID, VERSION, BASIC, NUMERIC, TEMPORAL, ENUM, REFERENCE, EMBEDDED
    }
}

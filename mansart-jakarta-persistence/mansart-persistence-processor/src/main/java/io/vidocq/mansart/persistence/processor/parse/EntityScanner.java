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

import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Scans JPA entities and extracts persistence metadata.
 *
 * <p>This class walks an {@code @jakarta.persistence.Entity}-annotated {@link TypeElement}
 * and extracts information about:
 * <ul>
 *   <li>Entity name and table mapping</li>
 *   <li>Primary key attributes</li>
 *   <li>Version attributes (for optimistic locking)</li>
 *   <li>Persistent attributes</li>
 * </ul>
 */
public final class EntityScanner {

    private static final String ENTITY_ANNOTATION = "jakarta.persistence.Entity";
    private static final String TABLE_ANNOTATION = "jakarta.persistence.Table";
    private static final String ID_ANNOTATION = "jakarta.persistence.Id";
    private static final String EMBEDDED_ID_ANNOTATION = "jakarta.persistence.EmbeddedId";
    private static final String MAPPED_SUPERCLASS_ANNOTATION = "jakarta.persistence.MappedSuperclass";
    private static final String EMBEDDED_ANNOTATION = "jakarta.persistence.Embedded";
    private static final String GENERATED_VALUE_ANNOTATION = "jakarta.persistence.GeneratedValue";
    private static final String VERSION_ANNOTATION = "jakarta.persistence.Version";
    private static final String TRANSIENT_ANNOTATION = "jakarta.persistence.Transient";
    private static final String COLUMN_ANNOTATION = "jakarta.persistence.Column";
    private static final String MANY_TO_ONE_ANNOTATION = "jakarta.persistence.ManyToOne";
    private static final String ONE_TO_ONE_ANNOTATION = "jakarta.persistence.OneToOne";
    private static final String ONE_TO_MANY_ANNOTATION = "jakarta.persistence.OneToMany";
    private static final String MANY_TO_MANY_ANNOTATION = "jakarta.persistence.ManyToMany";

    private final Elements elements;
    private final Types types;
    private final Messager messager;

    private final IdParser idParser = new IdParser();
    private final GeneratedValueParser generatedValueParser = new GeneratedValueParser();
    private final BasicParser basicParser = new BasicParser();
    private final ColumnParser columnParser = new ColumnParser();
    private final ManyToOneParser manyToOneParser = new ManyToOneParser();
    private final OneToOneParser oneToOneParser = new OneToOneParser();
    private final OneToManyParser oneToManyParser = new OneToManyParser();
    private final ManyToManyParser manyToManyParser = new ManyToManyParser();
    private final InheritanceParser inheritanceParser = new InheritanceParser();
    private final NamedQueryParser namedQueryParser = new NamedQueryParser();

    /**
     * Creates a new EntityScanner.
     *
     * @param env the processing environment
     */
    public EntityScanner(ProcessingEnvironment env) {
        this(env.getElementUtils(), env.getTypeUtils(), env.getMessager());
    }

    /**
     * Creates a new EntityScanner with explicit dependencies.
     *
     * @param elements the elements utility
     * @param types the types utility
     * @param messager the messager for diagnostics
     */
    public EntityScanner(Elements elements, Types types, Messager messager) {
        this.elements = elements;
        this.types = types;
        this.messager = messager;
    }

    /**
     * Scans an entity type and extracts its metadata.
     *
     * @param type the entity type element
     * @return the extracted entity metadata, or null if not an entity or scan failed
     */
    public EntityMetadata scan(TypeElement type) {
        if (!hasAnnotation(type, ENTITY_ANNOTATION)) {
            return null;
        }

        String tableName = readTableName(type);
        String schema = readSchemaName(type);

        List<AttributeMetadata> attributes = new ArrayList<>();
        AttributeMetadata idAttribute = null;
        AttributeMetadata versionAttribute = null;

        // Access type is determined by the placement of @Id: on a getter → property access
        boolean propertyAccess = usesPropertyAccess(type);

        for (Element member : type.getEnclosedElements()) {
            if (!isPersistentMember(member, propertyAccess)) {
                continue;
            }

            AttributeMetadata attr = describe(member);
            if (attr == null) {
                continue;
            }

            attributes.add(attr);

            if (attr.isId()) {
                if (idAttribute != null) {
                    error(type, "Multiple @Id fields/methods are not supported.");
                    return null;
                }
                idAttribute = attr;
            }

            if (attr.isVersion()) {
                versionAttribute = attr;
            }
        }

        // Collect inherited members from parent entity classes. Only @Entity and
        // @MappedSuperclass ancestors contribute persistent attributes (JPA 2.11.3) —
        // this also stops the walk before java.lang.Object (whose getClass() would
        // otherwise leak in as a property-access attribute).
        TypeElement current = getSuperclass(type);
        while (current != null
                && (hasAnnotation(current, ENTITY_ANNOTATION)
                    || hasAnnotation(current, MAPPED_SUPERCLASS_ANNOTATION))) {
            for (Element member : current.getEnclosedElements()) {
                if (!isPersistentMember(member, propertyAccess)) {
                    continue;
                }

                AttributeMetadata attr = describe(member);
                if (attr == null) {
                    continue;
                }

                // Skip duplicates (child class may redeclare same member)
                if (attributes.stream().anyMatch(a -> a.name().equals(attr.name()))) {
                    continue;
                }

                attributes.add(attr);

                if (attr.isId()) {
                    if (idAttribute != null) {
                        error(type, "Multiple @Id fields/methods are not supported.");
                        return null;
                    }
                    idAttribute = attr;
                }

                if (attr.isVersion()) {
                    versionAttribute = attr;
                }
            }
            current = getSuperclass(current);
        }

        if (idAttribute == null) {
            error(type, "Entity " + type.getQualifiedName() + " has no @Id attribute.");
            return null;
        }

        // Parse inheritance information
        InheritanceParser.InheritanceInfo inheritanceInfo = inheritanceParser.parse(type);

        return new EntityMetadata(
                type,
                tableName,
                schema,
                idAttribute,
                versionAttribute,
                attributes,
                inheritanceInfo
        );
    }

    private AttributeMetadata describe(Element element) {
        String name;
        String getterName = null;
        javax.lang.model.type.TypeMirror attributeType;
        if (element instanceof VariableElement varElement) {
            name = varElement.getSimpleName().toString();
            attributeType = varElement.asType();
        } else if (element instanceof ExecutableElement exec && isGetter(exec)) {
            // Property access: the mapping annotations sit on the getter
            getterName = exec.getSimpleName().toString();
            name = propertyName(getterName);
            attributeType = exec.getReturnType();
        } else {
            return null;
        }
        String javaTypeFqn = attributeType.toString();

        // Not yet supported: @Embedded attributes (need column expansion) and
        // secondary-table columns (need multi-table INSERT). Skip them so the
        // primary-table DML stays valid — partial persistence beats a hard failure.
        if (hasAnnotation(element, EMBEDDED_ANNOTATION)) {
            return null;
        }
        AnnotationMirror columnMirror = getAnnotationMirror(element, COLUMN_ANNOTATION);
        if (columnMirror != null) {
            AnnotationValue tableValue = getAnnotationValues(columnMirror).get("table");
            if (tableValue != null && !tableValue.getValue().toString().isEmpty()) {
                return null;
            }
        }

        boolean isId = hasAnnotation(element, ID_ANNOTATION);
        boolean isVersion = hasAnnotation(element, VERSION_ANNOTATION);
        boolean isGenerated = hasAnnotation(element, GENERATED_VALUE_ANNOTATION);

        // Check for relationship annotations
        RelationshipInfo relationshipInfo = null;
        if (hasAnnotation(element, MANY_TO_ONE_ANNOTATION)) {
            ManyToOneParser.ManyToOneInfo manyToOneInfo = manyToOneParser.parse(element, types);
            if (manyToOneInfo != null) {
                relationshipInfo = RelationshipInfo.fromManyToOne(manyToOneInfo);
            }
        } else if (hasAnnotation(element, ONE_TO_ONE_ANNOTATION)) {
            OneToOneParser.OneToOneInfo oneToOneInfo = oneToOneParser.parse(element, types);
            if (oneToOneInfo != null) {
                relationshipInfo = RelationshipInfo.fromOneToOne(oneToOneInfo);
            }
        } else if (hasAnnotation(element, ONE_TO_MANY_ANNOTATION)) {
            OneToManyParser.OneToManyInfo oneToManyInfo = oneToManyParser.parse(element, types);
            if (oneToManyInfo != null) {
                relationshipInfo = RelationshipInfo.fromOneToMany(oneToManyInfo);
            }
        } else if (hasAnnotation(element, MANY_TO_MANY_ANNOTATION)) {
            ManyToManyParser.ManyToManyInfo manyToManyInfo = manyToManyParser.parse(element, types);
            if (manyToManyInfo != null) {
                relationshipInfo = RelationshipInfo.fromManyToMany(manyToManyInfo);
            }
        }

        // Extract @Id info
        IdParser.IdInfo idInfo = null;
        if (isId) {
            idInfo = idParser.parse(element);
            if (idInfo == null) {
                return null;
            }
        }

        // Extract @GeneratedValue info
        GeneratedValueParser.GeneratedValueInfo genValueInfo = null;
        if (isGenerated) {
            genValueInfo = generatedValueParser.parse(element);
        }

        // Extract @Column info
        String columnName = null;
        boolean isColumnNullable = true;
        if (hasAnnotation(element, COLUMN_ANNOTATION)) {
            ColumnParser.ColumnInfo columnInfo = columnParser.parse(element);
            if (columnInfo != null) {
                columnName = columnInfo.name();
                isColumnNullable = columnInfo.isNullable();
            }
        }

        // Extract @Basic info (for fetch type)
        BasicParser.BasicInfo basicInfo = basicParser.parse(element);
        io.vidocq.mansart.persistence.spi.AttributeMetadata.FetchType fetchType = 
            determineFetchType(element, basicInfo, relationshipInfo);

        // Determine attribute kind
        AttributeKind kind = determineAttributeKind(attributeType, javaTypeFqn, isId, isVersion, relationshipInfo);

        return new AttributeMetadata(
                element,
                name,
                javaTypeFqn,
                kind,
                isId,
                isVersion,
                isGenerated,
                idInfo,
                genValueInfo,
                columnName,
                isColumnNullable,
                relationshipInfo,
                basicInfo,
                fetchType,
                getterName
        );
    }

    /**
     * Returns whether the entity hierarchy uses property access, per the Jakarta
     * Persistence access rules: access is determined by the placement of {@code @Id}
     * (or {@code @EmbeddedId}) — on a getter means property access.
     */
    private boolean usesPropertyAccess(TypeElement type) {
        TypeElement current = type;
        while (current != null) {
            for (Element member : current.getEnclosedElements()) {
                if (hasAnnotation(member, ID_ANNOTATION)
                        || hasAnnotation(member, EMBEDDED_ID_ANNOTATION)) {
                    return member.getKind() == ElementKind.METHOD;
                }
            }
            current = getSuperclass(current);
        }
        return false;
    }

    /**
     * Returns whether {@code member} is a persistent attribute carrier for the
     * given access type: a non-static, non-transient field (field access) or a
     * non-static getter (property access).
     */
    private boolean isPersistentMember(Element member, boolean propertyAccess) {
        if (member.getModifiers().contains(Modifier.STATIC)) {
            return false;
        }
        if (hasAnnotation(member, TRANSIENT_ANNOTATION)) {
            return false;
        }
        if (propertyAccess) {
            return member instanceof ExecutableElement exec && isGetter(exec);
        }
        return member.getKind() == ElementKind.FIELD
                && !member.getModifiers().contains(Modifier.TRANSIENT);
    }

    /** A JavaBeans getter: no parameter, non-void return, named getX or isX (boolean). */
    private static boolean isGetter(ExecutableElement method) {
        if (method.getKind() != ElementKind.METHOD || !method.getParameters().isEmpty()) {
            return false;
        }
        if (method.getReturnType().getKind() == javax.lang.model.type.TypeKind.VOID) {
            return false;
        }
        String name = method.getSimpleName().toString();
        if (name.startsWith("get") && name.length() > 3) {
            return true;
        }
        return name.startsWith("is") && name.length() > 2
                && (method.getReturnType().getKind() == javax.lang.model.type.TypeKind.BOOLEAN
                    || "java.lang.Boolean".equals(method.getReturnType().toString()));
    }

    /** Derives the JavaBeans property name from a getter name (getFoo/isFoo → foo). */
    public static String propertyName(String getterName) {
        String raw = getterName.startsWith("is") ? getterName.substring(2) : getterName.substring(3);
        if (raw.length() > 1 && Character.isUpperCase(raw.charAt(1))) {
            return raw; // e.g. getURL → URL, per JavaBeans decapitalization rules
        }
        return Character.toLowerCase(raw.charAt(0)) + raw.substring(1);
    }

    private AttributeKind determineAttributeKind(javax.lang.model.type.TypeMirror attributeType,
                                                  String javaTypeFqn,
                                                  boolean isId, boolean isVersion,
                                                  RelationshipInfo relationshipInfo) {
        if (isId) return AttributeKind.ID;
        if (isVersion) return AttributeKind.VERSION;
        if (relationshipInfo != null) return AttributeKind.REFERENCE;

        if (isTextType(javaTypeFqn)) return AttributeKind.TEXT;
        if (isBoolean(javaTypeFqn)) return AttributeKind.BOOLEAN;
        if (isTemporal(javaTypeFqn)) return AttributeKind.TEMPORAL;
        if (isNumeric(javaTypeFqn)) return AttributeKind.NUMERIC;
        if (isEnumType(attributeType)) return AttributeKind.ENUM;

        return AttributeKind.OBJECT;
    }

    private io.vidocq.mansart.persistence.spi.AttributeMetadata.FetchType determineFetchType(
            Element element, BasicParser.BasicInfo basicInfo, RelationshipInfo relationshipInfo) {
        // If relationship info exists, use its fetch type
        if (relationshipInfo != null) {
            // Convert from RelationshipMetadata.FetchType to AttributeMetadata.FetchType
            return switch (relationshipInfo.fetchType()) {
                case EAGER -> io.vidocq.mansart.persistence.spi.AttributeMetadata.FetchType.EAGER;
                case LAZY -> io.vidocq.mansart.persistence.spi.AttributeMetadata.FetchType.LAZY;
            };
        }
        // If basic info exists, use its fetch type
        if (basicInfo != null) {
            return basicInfo.fetchType();
        }
        // Default is EAGER for attributes without explicit fetch type
        return io.vidocq.mansart.persistence.spi.AttributeMetadata.FetchType.EAGER;
    }

    private boolean isTextType(String fqn) {
        return "java.lang.String".equals(fqn);
    }

    private boolean isBoolean(String fqn) {
        return "boolean".equals(fqn) || "java.lang.Boolean".equals(fqn);
    }

    private boolean isTemporal(String fqn) {
        return fqn.startsWith("java.time.") || 
               "java.util.Date".equals(fqn) || 
               "java.sql.Date".equals(fqn) ||
               "java.sql.Timestamp".equals(fqn);
    }

    private boolean isNumeric(String fqn) {
        return switch (fqn) {
            case "byte", "short", "int", "long", "float", "double",
                 "java.lang.Byte", "java.lang.Short", "java.lang.Integer", "java.lang.Long",
                 "java.lang.Float", "java.lang.Double",
                 "java.math.BigInteger", "java.math.BigDecimal" -> true;
            default -> false;
        };
    }

    private boolean isEnumType(TypeMirror type) {
        if (type.getKind() != TypeKind.DECLARED) return false;
        Element el = types.asElement(type);
        return el != null && el.getKind() == ElementKind.ENUM;
    }

    private String readTableName(TypeElement type) {
        AnnotationMirror tableMirror = getAnnotationMirror(type, TABLE_ANNOTATION);
        if (tableMirror == null) {
            // Check if this entity extends another entity (inheritance)
            TypeElement current = getSuperclass(type);
            while (current != null) {
                if (hasAnnotation(current, ENTITY_ANNOTATION)) {
                    // Found a parent entity - check if it has @Table
                    AnnotationMirror parentTableMirror = getAnnotationMirror(current, TABLE_ANNOTATION);
                    if (parentTableMirror != null) {
                        Map<String, AnnotationValue> values = getAnnotationValues(parentTableMirror);
                        AnnotationValue nameValue = values.get("name");
                        if (nameValue != null) {
                            String name = nameValue.getValue().toString();
                            if (!name.isEmpty()) {
                                return name;
                            }
                        }
                    }
                    // Parent doesn't have @Table either - use the parent's entity name
                    return defaultTableName(current);
                }
                current = getSuperclass(current);
            }
            return defaultTableName(type);
        }

        Map<String, AnnotationValue> values = getAnnotationValues(tableMirror);
        AnnotationValue nameValue = values.get("name");
        if (nameValue != null) {
            String name = nameValue.getValue().toString();
            if (!name.isEmpty()) {
                return name;
            }
        }
        return defaultTableName(type);
    }

    /**
     * JPA default table name (2.13): the entity name — {@code @Entity(name)} when
     * present, otherwise the unqualified class name. Must match schemas created from
     * spec-conformant DDL such as the official TCK scripts.
     */
    private String defaultTableName(TypeElement type) {
        AnnotationMirror entityMirror = getAnnotationMirror(type, ENTITY_ANNOTATION);
        if (entityMirror != null) {
            AnnotationValue nameValue = getAnnotationValues(entityMirror).get("name");
            if (nameValue != null) {
                String name = nameValue.getValue().toString();
                if (!name.isEmpty()) {
                    return name;
                }
            }
        }
        return type.getSimpleName().toString();
    }

    private String readSchemaName(TypeElement type) {
        AnnotationMirror tableMirror = getAnnotationMirror(type, TABLE_ANNOTATION);
        if (tableMirror == null) {
            return "";
        }

        Map<String, AnnotationValue> values = getAnnotationValues(tableMirror);
        AnnotationValue schemaValue = values.get("schema");
        if (schemaValue != null) {
            String schema = schemaValue.getValue().toString();
            if (!schema.isEmpty()) {
                return schema;
            }
        }
        return "";
    }

    // Helper methods - package-private for use by parsers
    static boolean hasAnnotation(Element element, String annotationFqn) {
        return getAnnotationMirror(element, annotationFqn) != null;
    }

    static AnnotationMirror getAnnotationMirror(Element element, String annotationFqn) {
        for (AnnotationMirror mirror : element.getAnnotationMirrors()) {
            if (((javax.lang.model.element.TypeElement) mirror.getAnnotationType().asElement())
                    .getQualifiedName().toString().equals(annotationFqn)) {
                return mirror;
            }
        }
        return null;
    }

    static Map<String, AnnotationValue> getAnnotationValues(AnnotationMirror mirror) {
        Map<String, AnnotationValue> values = new java.util.HashMap<>();
        ExecutableElement[] keys = mirror.getElementValues().keySet().toArray(new ExecutableElement[0]);
        for (ExecutableElement key : keys) {
            values.put(key.getSimpleName().toString(), mirror.getElementValues().get(key));
        }
        return values;
    }

    private void error(Element element, String message) {
        if (messager != null) {
            messager.printMessage(Diagnostic.Kind.ERROR, "[mansart-persistence] " + message, element);
        }
    }

    /**
     * Returns the superclass of the given type element.
     */
    public static TypeElement getSuperclass(TypeElement type) {
        TypeMirror superclass = type.getSuperclass();
        if (superclass.getKind() == TypeKind.DECLARED) {
            return (TypeElement) ((DeclaredType) superclass).asElement();
        }
        return null;
    }

    /**
     * Metadata for a scanned entity.
     */
    public record EntityMetadata(
            TypeElement type,
            String tableName,
            String schema,
            AttributeMetadata idAttribute,
            AttributeMetadata versionAttribute,
            List<AttributeMetadata> attributes,
            InheritanceParser.InheritanceInfo inheritanceInfo
    ) {
        public EntityMetadata {
            // defensive copies
            attributes = List.copyOf(attributes);
        }
    }

    /**
     * Metadata for a persistent attribute.
     */
    public record AttributeMetadata(
            Element element,
            String name,
            String javaTypeFqn,
            AttributeKind kind,
            boolean isId,
            boolean isVersion,
            boolean isGenerated,
            IdParser.IdInfo idInfo,
            GeneratedValueParser.GeneratedValueInfo genValueInfo,
            String columnName,
            boolean isColumnNullable,
            RelationshipInfo relationshipInfo,
            BasicParser.BasicInfo basicInfo,
            io.vidocq.mansart.persistence.spi.AttributeMetadata.FetchType fetchType,
            String getterName
    ) {
        public AttributeMetadata {
            if (isId && idInfo == null) {
                throw new IllegalArgumentException("ID attribute must have idInfo");
            }
        }

        /** Whether this attribute is mapped with property access (annotations on the getter). */
        public boolean isPropertyAccess() {
            return getterName != null;
        }

        /** The matching JavaBeans setter name for a property-access attribute. */
        public String setterName() {
            String property = EntityScanner.propertyName(getterName);
            return "set" + Character.toUpperCase(property.charAt(0)) + property.substring(1);
        }
    }

    /**
     * Kinds of persistent attributes.
     */
    public enum AttributeKind {
        ID,
        VERSION,
        TEXT,
        NUMERIC,
        BOOLEAN,
        TEMPORAL,
        ENUM,
        OBJECT,
        REFERENCE
    }
}

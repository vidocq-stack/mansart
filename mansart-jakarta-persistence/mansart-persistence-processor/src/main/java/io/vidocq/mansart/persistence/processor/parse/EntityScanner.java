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
    private static final String GENERATED_VALUE_ANNOTATION = "jakarta.persistence.GeneratedValue";
    private static final String VERSION_ANNOTATION = "jakarta.persistence.Version";
    private static final String TRANSIENT_ANNOTATION = "jakarta.persistence.Transient";
    private static final String COLUMN_ANNOTATION = "jakarta.persistence.Column";
    private static final String MANY_TO_ONE_ANNOTATION = "jakarta.persistence.ManyToOne";
    private static final String ONE_TO_ONE_ANNOTATION = "jakarta.persistence.OneToOne";

    private final Elements elements;
    private final Types types;
    private final Messager messager;

    private final IdParser idParser = new IdParser();
    private final GeneratedValueParser generatedValueParser = new GeneratedValueParser();
    private final ManyToOneParser manyToOneParser = new ManyToOneParser();
    private final OneToOneParser oneToOneParser = new OneToOneParser();

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

        for (Element member : type.getEnclosedElements()) {
            if (member.getKind() != ElementKind.FIELD && member.getKind() != ElementKind.METHOD) {
                continue;
            }
            if (member.getModifiers().contains(Modifier.STATIC)) {
                continue;
            }
            if (member.getModifiers().contains(Modifier.TRANSIENT)) {
                continue;
            }
            if (hasAnnotation(member, TRANSIENT_ANNOTATION)) {
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

        if (idAttribute == null) {
            error(type, "Entity " + type.getQualifiedName() + " has no @Id attribute.");
            return null;
        }

        return new EntityMetadata(
                type,
                tableName,
                schema,
                idAttribute,
                versionAttribute,
                attributes
        );
    }

    private AttributeMetadata describe(Element element) {
        String name;
        String javaTypeFqn;
        
        if (element instanceof VariableElement varElement) {
            name = varElement.getSimpleName().toString();
            javaTypeFqn = varElement.asType().toString();
        } else if (element instanceof ExecutableElement execElement) {
            // For method access, derive name from getter/setter
            String methodName = execElement.getSimpleName().toString();
            if (methodName.startsWith("get") && methodName.length() > 3) {
                name = Character.toLowerCase(methodName.charAt(3)) + methodName.substring(4);
            } else if (methodName.startsWith("is") && methodName.length() > 2) {
                name = Character.toLowerCase(methodName.charAt(2)) + methodName.substring(3);
            } else {
                name = methodName;
            }
            javaTypeFqn = execElement.getReturnType().toString();
        } else {
            return null;
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
            ColumnParser columnParser = new ColumnParser();
            ColumnParser.ColumnInfo columnInfo = columnParser.parse(element);
            if (columnInfo != null) {
                columnName = columnInfo.name();
                // For now, just use the column name if specified
                // Nullability will be used later if needed
            }
        }

        // Determine attribute kind
        AttributeKind kind = determineAttributeKind(element, javaTypeFqn, isId, isVersion, relationshipInfo);

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
                relationshipInfo
        );
    }

    private AttributeKind determineAttributeKind(Element element, String javaTypeFqn,
                                                  boolean isId, boolean isVersion,
                                                  RelationshipInfo relationshipInfo) {
        if (isId) return AttributeKind.ID;
        if (isVersion) return AttributeKind.VERSION;
        if (relationshipInfo != null) return AttributeKind.REFERENCE;

        if (isTextType(javaTypeFqn)) return AttributeKind.TEXT;
        if (isBoolean(javaTypeFqn)) return AttributeKind.BOOLEAN;
        if (isTemporal(javaTypeFqn)) return AttributeKind.TEMPORAL;
        if (isNumeric(javaTypeFqn)) return AttributeKind.NUMERIC;
        if (isEnumType(element.asType())) return AttributeKind.ENUM;

        return AttributeKind.OBJECT;
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
            return elements.getBinaryName(type).toString().replace('.', '_');
        }

        Map<String, AnnotationValue> values = getAnnotationValues(tableMirror);
        AnnotationValue nameValue = values.get("name");
        if (nameValue != null) {
            String name = nameValue.getValue().toString();
            if (!name.isEmpty()) {
                return name;
            }
        }
        return elements.getBinaryName(type).toString().replace('.', '_');
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

    // Helper methods
    private boolean hasAnnotation(Element element, String annotationFqn) {
        return getAnnotationMirror(element, annotationFqn) != null;
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
     * Metadata for a scanned entity.
     */
    public record EntityMetadata(
            TypeElement type,
            String tableName,
            String schema,
            AttributeMetadata idAttribute,
            AttributeMetadata versionAttribute,
            List<AttributeMetadata> attributes
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
            RelationshipInfo relationshipInfo
    ) {
        public AttributeMetadata {
            if (isId && idInfo == null) {
                throw new IllegalArgumentException("ID attribute must have idInfo");
            }
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

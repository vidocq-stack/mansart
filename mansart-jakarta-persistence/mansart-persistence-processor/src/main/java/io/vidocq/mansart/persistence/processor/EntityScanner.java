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
 * SPDX-License-Identifier: EPL-2.0 OR EUPL-1.2 OR GPL-2.0-or-later
 */
package io.vidocq.mansart.persistence.processor;

import javax.annotation.processing.Messager;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
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

/**
 * Walks an {@code @jakarta.persistence.Entity}-annotated {@link TypeElement} and produces
 * an {@link EntityDescriptor} by reading the standard JPA mapping annotations
 * ({@code @Id}, {@code @Column}, {@code @Version}).
 *
 * <p>This is the persistence-specific variant of the Data processor's scanner,
 * scoped to JPA annotations only (no {@code @Repository}, no JDQL).</p>
 */
final class EntityScanner {

    private final Elements  elements;
    private final Types     types;
    private final Messager  messager;

    EntityScanner(Elements elements, Types types, Messager messager) {
        this.elements = elements;
        this.types    = types;
        this.messager = messager;
    }

    /**
     * Scans an entity type, returning a descriptor with all persistent attributes,
     * or {@code null} if the type is not a valid entity.
     */
    EntityDescriptor scan(TypeElement type) {
        if (!hasAnnotation(type, "jakarta.persistence.Entity")) {
            return null;
        }

        String tableName = readEntityTable(type);

        List<AttributeDescriptor> attributes = new ArrayList<>();
        AttributeDescriptor       id         = null;
        AttributeDescriptor       version    = null;

        for (Element member : type.getEnclosedElements()) {
            if (member.getKind() != ElementKind.FIELD) continue;
            if (member.getModifiers().contains(Modifier.STATIC)) continue;
            if (member.getModifiers().contains(Modifier.TRANSIENT)) continue;
            if (hasAnnotation(member, "jakarta.persistence.Transient")) continue;

            VariableElement field = (VariableElement) member;
            AttributeDescriptor a = describe(field);
            if (a == null) continue;

            attributes.add(a);
            if (a.kind == AttributeKind.ID) {
                if (id != null) {
                    error(field, "Multiple @Id fields are not supported.");
                    return null;
                }
                id = a;
            }
            if (a.kind == AttributeKind.VERSION) {
                version = a;
            }
        }

        if (id == null) {
            error(type, "Entity " + type.getQualifiedName() + " has no @Id field.");
            return null;
        }

        return new EntityDescriptor(type, tableName, id, version, attributes);
    }

    /**
     * Scans an embeddable type, returning a descriptor with all persistent attributes.
     * Embeddables do not require an {@code @Id} field.
     */
    EmbeddableDescriptor scanEmbeddable(TypeElement type) {
        if (!hasAnnotation(type, "jakarta.persistence.Embeddable")) {
            return null;
        }

        List<AttributeDescriptor> attributes = new ArrayList<>();

        for (Element member : type.getEnclosedElements()) {
            if (member.getKind() != ElementKind.FIELD) continue;
            if (member.getModifiers().contains(Modifier.STATIC)) continue;
            if (member.getModifiers().contains(Modifier.TRANSIENT)) continue;
            if (hasAnnotation(member, "jakarta.persistence.Transient")) continue;

            VariableElement field = (VariableElement) member;
            AttributeDescriptor a = describe(field);
            if (a == null) continue;

            attributes.add(a);
        }

        return new EmbeddableDescriptor(type, attributes);
    }

    /**
     * Scans a mapped-superclass type, returning a descriptor with all persistent attributes.
     * Mapped superclasses do not require an {@code @Id} field.
     */
    EntityDescriptor scanMappedSuperclass(TypeElement type) {
        if (!hasAnnotation(type, "jakarta.persistence.MappedSuperclass")) {
            return null;
        }

        String tableName = readEntityTable(type);

        List<AttributeDescriptor> attributes = new ArrayList<>();
        AttributeDescriptor       id         = null;
        AttributeDescriptor       version    = null;

        for (Element member : type.getEnclosedElements()) {
            if (member.getKind() != ElementKind.FIELD) continue;
            if (member.getModifiers().contains(Modifier.STATIC)) continue;
            if (member.getModifiers().contains(Modifier.TRANSIENT)) continue;
            if (hasAnnotation(member, "jakarta.persistence.Transient")) continue;

            VariableElement field = (VariableElement) member;
            AttributeDescriptor a = describe(field);
            if (a == null) continue;

            attributes.add(a);
            if (a.kind == AttributeKind.ID) {
                if (id != null) {
                    error(field, "Multiple @Id fields are not supported.");
                    return null;
                }
                id = a;
            }
            if (a.kind == AttributeKind.VERSION) {
                version = a;
            }
        }

        return new EntityDescriptor(type, tableName, id, version, attributes);
    }

    private AttributeDescriptor describe(VariableElement field) {
        String name = field.getSimpleName().toString();
        TypeMirror typeMirror = field.asType();
        String javaTypeFqn = typeMirror.toString();

        boolean isId     = hasAnnotation(field, "jakarta.persistence.Id");
        boolean isVersion = hasAnnotation(field, "jakarta.persistence.Version");
        boolean isElementCollection = hasAnnotation(field, "jakarta.persistence.ElementCollection");
        boolean isOneToMany = hasAnnotation(field, "jakarta.persistence.OneToMany");
        boolean isManyToMany = hasAnnotation(field, "jakarta.persistence.ManyToMany");

        AttributeKind kind;
        if (isId)       kind = AttributeKind.ID;
        else if (isVersion) kind = AttributeKind.VERSION;
        else if (isOneToMany) kind = classifyRelationship(field, "ONE_TO_MANY");
        else if (isManyToMany) kind = classifyRelationship(field, "MANY_TO_MANY");
        else if (isElementCollection) kind = classifyCollection(field);
        else if (isTextType(javaTypeFqn))  kind = AttributeKind.TEXT;
        else if (isBoolean(javaTypeFqn))   kind = AttributeKind.BOOLEAN;
        else if (isTemporal(javaTypeFqn))  kind = AttributeKind.TEMPORAL;
        else if (isNumeric(javaTypeFqn))   kind = AttributeKind.NUMERIC;
        else                               kind = AttributeKind.NUMERIC;

        Map<String, ? extends AnnotationValue> column = annotationValues(field, "jakarta.persistence.Column");

        String columnName;
        if (column != null && column.containsKey("name")
                && !column.get("name").getValue().toString().isEmpty()) {
            columnName = column.get("name").getValue().toString();
        } else {
            columnName = snakeCase(name);
        }

        boolean nullable = readBoolean(column, "nullable", true);

        return new AttributeDescriptor(field, name, columnName, javaTypeFqn, kind, nullable);
    }

    /**
     * Classifies an {@code @ElementCollection} field as COLLECTION, LIST, or MAP
     * based on the raw type name.
     */
    private AttributeKind classifyCollection(VariableElement field) {
        TypeMirror typeMirror = field.asType();
        String fqn = typeMirror.toString();

        if (fqn.startsWith("java.util.Map<")) {
            return AttributeKind.MAP;
        }
        if (fqn.startsWith("java.util.List<") || fqn.startsWith("java.util.SortedList<")) {
            return AttributeKind.LIST;
        }
        // Collection, Set, HashSet, TreeSet, ArrayList all map to COLLECTION
        return AttributeKind.COLLECTION;
    }

    /**
     * Classifies a relationship field as ONE_TO_MANY or MANY_TO_MANY
     * based on the collection type (Set → COLLECTION, List → LIST).
     */
    private AttributeKind classifyRelationship(VariableElement field, String relationshipType) {
        TypeMirror typeMirror = field.asType();
        String fqn = typeMirror.toString();

        if (fqn.startsWith("java.util.List<") || fqn.startsWith("java.util.SortedList<")) {
            return AttributeKind.LIST;
        }
        // Set, HashSet, TreeSet, Collection all map to COLLECTION
        return AttributeKind.COLLECTION;
    }

    /* ----- helpers ---- */

    private boolean hasAnnotation(Element e, String fqn) {
        for (AnnotationMirror a : e.getAnnotationMirrors()) {
            String annFqn = ((TypeElement) a.getAnnotationType().asElement())
                    .getQualifiedName().toString();
            if (annFqn.equals(fqn)) return true;
        }
        return false;
    }

    private Map<String, ? extends AnnotationValue> annotationValues(Element e, String fqn) {
        for (AnnotationMirror a : e.getAnnotationMirrors()) {
            String annFqn = ((TypeElement) a.getAnnotationType().asElement())
                    .getQualifiedName().toString();
            if (annFqn.equals(fqn)) {
                @SuppressWarnings("unchecked")
                Map<String, ? extends AnnotationValue> values =
                        (Map<String, ? extends AnnotationValue>)
                                (Map<?, ?>) elements.getElementValuesWithDefaults(a).entrySet().stream()
                                        .collect(java.util.stream.Collectors.toMap(
                                                en -> en.getKey().getSimpleName().toString(),
                                                Map.Entry::getValue));
                return values;
            }
        }
        return null;
    }

    private boolean readBoolean(Map<String, ? extends AnnotationValue> values, String key, boolean def) {
        if (values == null || !values.containsKey(key)) return def;
        return (Boolean) values.get(key).getValue();
    }

    private String readEntityTable(TypeElement type) {
        Map<String, ? extends AnnotationValue> table = annotationValues(type, "jakarta.persistence.Table");
        if (table != null && table.containsKey("name")) {
            String n = table.get("name").getValue().toString();
            if (!n.isEmpty()) return n;
        }
        return snakeCase(type.getSimpleName().toString());
    }

    /** Converts {@code camelCase} to {@code snake_case}. */
    private static String snakeCase(String camel) {
        StringBuilder sb = new StringBuilder(camel.length() + 4);
        for (int i = 0; i < camel.length(); i++) {
            char c = camel.charAt(i);
            if (Character.isUpperCase(c) && i > 0) sb.append('_');
            sb.append(Character.toLowerCase(c));
        }
        return sb.toString();
    }

    private boolean isTextType(String fqn) {
        return "java.lang.String".equals(fqn);
    }

    private boolean isBoolean(String fqn) {
        return "boolean".equals(fqn) || "java.lang.Boolean".equals(fqn);
    }

    private boolean isTemporal(String fqn) {
        return fqn.startsWith("java.time.")
                || "java.util.Date".equals(fqn)
                || "java.sql.Date".equals(fqn);
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

    private void error(Element element, String message) {
        if (messager != null) {
            messager.printMessage(Diagnostic.Kind.ERROR, "[mansart-persistence] " + message, element);
        }
    }

    /* ----- DTOs ---- */

    enum AttributeKind {
        ID, VERSION, TEXT, NUMERIC, BOOLEAN, TEMPORAL,
        COLLECTION, LIST, MAP,
        ONE_TO_MANY, MANY_TO_MANY
    }

    record AttributeDescriptor(
            VariableElement element,
            String name,
            String columnName,
            String javaTypeFqn,
            AttributeKind kind,
            boolean nullable
    ) {}

    record EntityDescriptor(
            TypeElement type,
            String tableName,
            AttributeDescriptor id,
            AttributeDescriptor version,
            List<AttributeDescriptor> attributes
    ) {}

    record EmbeddableDescriptor(
            TypeElement type,
            List<AttributeDescriptor> attributes
    ) {}
}

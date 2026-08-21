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
package io.vidocq.mansart.persistence.processor.metadata;

import io.vidocq.mansart.persistence.processor.SourceSink;
import io.vidocq.mansart.persistence.processor.parse.EntityScanner;
import io.vidocq.mansart.persistence.processor.parse.RelationshipInfo;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * Generates the entity metadata implementation class.
 *
 * <p>This class creates a metadata class that implements the SPI's
 * {@code io.vidocq.mansart.persistence.spi.EntityMetadata} interface,
 * providing runtime-accessible metadata about the entity.
 *
 * <p>Phase 1 (DEBT-05): also generates per-attribute {@code MethodHandle}
 * accessors (getter + setter) so the runtime never touches
 * {@code java.lang.reflect.Field} on entity state.
 *
 * <p>Phase 1 (DEBT-06): references a generated {@code _EntityCallbacks}
 * callback dispatcher for lifecycle invocation.
 */
public final class EntityMetadataGenerator {

    private static final String GENERATED_ANNOTATION = "javax.annotation.processing.Generated";

    private final SourceSink sink;

    /**
     * Creates a new EntityMetadataGenerator.
     *
     * @param sink the source sink for writing generated files
     */
    public EntityMetadataGenerator(SourceSink sink) {
        this.sink = sink;
    }

    /**
     * Generates entity metadata for the given scanned entity.
     *
     * @param entity the scanned entity metadata
     * @throws IOException if writing fails
     */
    public void generate(EntityScanner.EntityMetadata entity) throws IOException {
        String pkg = packageOf(entity.type().getQualifiedName().toString());
        String simpleName = entity.type().getSimpleName().toString();
        String className = simpleName + "Metadata";
        String fqn = pkg.isEmpty() ? className : pkg + "." + className;

        try (PrintWriter w = new PrintWriter(sink.createSource(fqn))) {
            if (!pkg.isEmpty()) {
                w.println("package " + pkg + ";");
                w.println();
            }

            w.println("import io.vidocq.mansart.persistence.spi.*;");
            w.println("import jakarta.persistence.InheritanceType;");
            w.println("import java.lang.invoke.MethodHandle;");
            w.println("import java.lang.invoke.MethodHandles;");
            w.println("import java.lang.invoke.MethodType;");
            w.println("import java.util.List;");
            w.println("import java.util.Set;");
            w.println("import java.util.HashMap;");
            w.println("import java.util.Map;");
            w.println("import java.util.ArrayList;");
            w.println("import java.util.Collections;");
            w.println();
            w.println("final class " + className + " implements EntityMetadata {");
            w.println();

            // ── Empty collections ──
            w.println("    private static final Set<String> EMPTY_SET = java.util.Collections.emptySet();");
            w.println("    private static final List<String> EMPTY_LIST = java.util.Collections.emptyList();");
            w.println();

            // ── Static LOOKUP (DEBT-05) ──
            writeLookupStatic(w, simpleName);

            // ── Accessor helper methods (must come before ACCESSORS) ──
            writeAccessorHelpers(w);

            // ── Attribute accessors (DEBT-05) ──
            writeAccessorsStatic(w, simpleName, entity);

            // ── Constructor handle (DEBT-05) ──
            writeConstructorHandleStatic(w, simpleName);

            // ── Callback dispatcher reference (DEBT-06) ──
            writeCallbacksStatic(w, simpleName);

            // ── Instance fields ──
            writeFields(w, entity);
            
            // ── Self-registration in static block ──
            writeRegistrationStatic(w, simpleName);

            // ── Constructor ──
            writeConstructor(w, entity);

            // ── Interface methods ──
            writeInterfaceMethods(w, entity);

            // ── Accessor methods (DEBT-05) ──
            writeAccessorMethods(w, entity);

            // ── Nested SimpleAccessor ──
            writeSimpleAccessorNested(w);

            // ── Nested GeneratedRelationshipMetadata (only when needed) ──
            writeRelationshipMetadataNested(w, entity);

            // ── Helper methods ──
            writeHelperMethods(w, entity);

            w.println("}");
        }
    }

    // ───────────────────── Static LOOKUP ─────────────────────

    private void writeLookupStatic(PrintWriter w, String simpleName) {
        w.println("    private static final MethodHandles.Lookup LOOKUP;");
        w.println("    static {");
        w.println("        try {");
        w.println("            LOOKUP = MethodHandles.privateLookupIn(" + simpleName + ".class, MethodHandles.lookup());");
        w.println("        } catch (java.lang.IllegalAccessException ex) {");
        w.println("            throw new java.lang.ExceptionInInitializerError(ex);");
        w.println("        }");
        w.println("    }");
        w.println();
    }

    // ───────────────────── Attribute accessors (DEBT-05) ─────────────────────

    private void writeAccessorsStatic(PrintWriter w, String simpleName, EntityScanner.EntityMetadata entity) {
        var attrs = entity.attributes();
        if (attrs.isEmpty()) {
            w.println("    private static final io.vidocq.mansart.persistence.spi.AttributeAccessor[] ACCESSORS = new io.vidocq.mansart.persistence.spi.AttributeAccessor[0];");
            w.println();
            return;
        }

        w.println("    private static final io.vidocq.mansart.persistence.spi.AttributeAccessor[] ACCESSORS;");
        w.println("    static {");
        w.println("        final java.util.Map<String, io.vidocq.mansart.persistence.spi.AttributeAccessor> _map = new java.util.HashMap<>();");

        for (int i = 0; i < attrs.size(); i++) {
            var attr = attrs.get(i);
            String name = attr.name();
            // Strip generics for MethodHandles (e.g. List<Course> → List)
            String rawType = stripGenerics(attr.javaTypeFqn());
            
            // For inherited fields, use the declaring class instead of the entity class
            String declaringClass = simpleName;
            if (attr.element() != null) {
                var enclosing = attr.element().getEnclosingElement();
                if (enclosing != null && enclosing instanceof javax.lang.model.element.TypeElement te) {
                    if (!te.getQualifiedName().toString().equals(entity.type().getQualifiedName().toString())) {
                        declaringClass = te.getSimpleName().toString();
                    }
                }
            }

            // The persistent-attribute lists are keyed by column name when @Column
            // renames the attribute — register the accessor under BOTH names
            List<String> accessorKeys = new ArrayList<>();
            accessorKeys.add(name);
            if (attr.columnName() != null && !attr.columnName().equals(name)) {
                accessorKeys.add(attr.columnName());
            }
            for (String key : accessorKeys) {
                if (attr.isPropertyAccess()) {
                    w.print("        _map.put(\"" + key + "\", new SimpleAccessor<>(\"" + key + "\", " + rawType);
                    w.print(".class, _propGetterMh(" + declaringClass + ".class, \"" + attr.getterName() + "\", " + rawType);
                    w.println(".class), _propSetterMh(" + declaringClass + ".class, \"" + attr.setterName() + "\", " + rawType + ".class)));");
                } else {
                    w.print("        _map.put(\"" + key + "\", new SimpleAccessor<>(\"" + key + "\", " + rawType);
                    w.print(".class, _getterMh(" + declaringClass + ".class, \"" + name + "\", " + rawType);
                    w.println(".class), _setterMh(" + declaringClass + ".class, \"" + name + "\", " + rawType + ".class)));");
                }
            }
        }

        w.println("        ACCESSORS = _map.values().toArray(new io.vidocq.mansart.persistence.spi.AttributeAccessor[0]);");
        w.println("    }");
        w.println();
    }

    private String fieldName(String name) {
        // Escape if it's a Java keyword
        if (name.equals("class") || name.equals("new") || name.equals("if") ||
            name.equals("else") || name.equals("for") || name.equals("while") ||
            name.equals("do") || name.equals("switch") || name.equals("case") ||
            name.equals("break") || name.equals("return") || name.equals("try") ||
            name.equals("catch") || name.equals("finally") || name.equals("throw") ||
            name.equals("throws") || name.equals("void") || name.equals("boolean") ||
            name.equals("byte") || name.equals("char") || name.equals("short") ||
            name.equals("int") || name.equals("long") || name.equals("float") ||
            name.equals("double") || name.equals("super") || name.equals("this")) {
            return "_" + name;
        }
        return name;
    }

    // ───────────────────── Constructor handle (DEBT-05) ─────────────────────

    private void writeConstructorHandleStatic(PrintWriter w, String simpleName) {
        w.println("    private static final MethodHandle INSTANCE_CONSTRUCTOR;");
        w.println("    static {");
        w.println("        try {");
        w.println("            INSTANCE_CONSTRUCTOR = LOOKUP.findConstructor(" + simpleName + ".class,");
        w.println("                MethodType.methodType(void.class));");
        w.println("        } catch (NoSuchMethodException | java.lang.IllegalAccessException ex) {");
        w.println("            throw new java.lang.ExceptionInInitializerError(");
        w.println("                \"" + simpleName + " must have a no-arg constructor\");");
        w.println("        }");
        w.println("    }");
        w.println();
    }

    // ───────────────────── Callback dispatcher reference (DEBT-06) ─────────────────────

    private void writeCallbacksStatic(PrintWriter w, String simpleName) {
        w.println("    private static final LifecycleCallbackDispatcher CALLBACKS;");
        w.println("    static {");
        w.println("        try {");
        w.println("            Object _cbObj = " + simpleName + "_LifecycleCallbacks.class.getDeclaredConstructor().newInstance();");
        w.println("            CALLBACKS = (LifecycleCallbackDispatcher) _cbObj;");
        w.println("        } catch (Exception ex) {");
        w.println("            throw new java.lang.ExceptionInInitializerError(ex);");
        w.println("        }");
        w.println("    }");
        w.println();
    }

    // ───────────────────── Self-registration (DEBT-05) ─────────────────────

    private void writeRegistrationStatic(PrintWriter w, String simpleName) {
        w.println("    static {");
        w.println("        EntityMetadataRegistry.register(" + simpleName + ".class, new " + simpleName + "Metadata());");
        w.println("    }");
        w.println();
    }

    // ───────────────────── Accessor helper methods ─────────────────────

    private void writeAccessorHelpers(PrintWriter w) {
        w.println("    private static java.lang.invoke.MethodHandle _getterMh(Class<?> clazz, String name, Class<?> type) {");
        w.println("        try {");
        w.println("            java.lang.invoke.MethodHandles.Lookup lookup = java.lang.invoke.MethodHandles.privateLookupIn(clazz, java.lang.invoke.MethodHandles.lookup());");
        w.println("            return lookup.findGetter(clazz, name, type);");
        w.println("        } catch (NoSuchFieldException | java.lang.IllegalAccessException e) { throw new java.lang.ExceptionInInitializerError(e); }");
        w.println("    }");
        w.println();
        w.println("    private static java.lang.invoke.MethodHandle _setterMh(Class<?> clazz, String name, Class<?> type) {");
        w.println("        try {");
        w.println("            java.lang.invoke.MethodHandles.Lookup lookup = java.lang.invoke.MethodHandles.privateLookupIn(clazz, java.lang.invoke.MethodHandles.lookup());");
        w.println("            return lookup.findSetter(clazz, name, type);");
        w.println("        } catch (NoSuchFieldException | java.lang.IllegalAccessException e) { throw new java.lang.ExceptionInInitializerError(e); }");
        w.println("    }");
        w.println();
        w.println("    private static java.lang.invoke.MethodHandle _propGetterMh(Class<?> clazz, String getterName, Class<?> type) {");
        w.println("        try {");
        w.println("            java.lang.invoke.MethodHandles.Lookup lookup = java.lang.invoke.MethodHandles.privateLookupIn(clazz, java.lang.invoke.MethodHandles.lookup());");
        w.println("            return lookup.findVirtual(clazz, getterName, MethodType.methodType(type));");
        w.println("        } catch (NoSuchMethodException | java.lang.IllegalAccessException e) { throw new java.lang.ExceptionInInitializerError(e); }");
        w.println("    }");
        w.println();
        w.println("    private static java.lang.invoke.MethodHandle _propSetterMh(Class<?> clazz, String setterName, Class<?> type) {");
        w.println("        try {");
        w.println("            java.lang.invoke.MethodHandles.Lookup lookup = java.lang.invoke.MethodHandles.privateLookupIn(clazz, java.lang.invoke.MethodHandles.lookup());");
        w.println("            return lookup.findVirtual(clazz, setterName, MethodType.methodType(void.class, type));");
        w.println("        } catch (NoSuchMethodException e) {");
        w.println("            return null; // read-only property: writes will be rejected by the accessor");
        w.println("        } catch (java.lang.IllegalAccessException e) { throw new java.lang.ExceptionInInitializerError(e); }");
        w.println("    }");
        w.println();
    }

    // ───────────────────── Instance fields ─────────────────────

    private void writeFields(PrintWriter w, EntityScanner.EntityMetadata entity) {
        w.println("    private final Class<?> entityClass;");
        w.println("    private final String entityName;");
        w.println("    private final String tableName;");
        w.println("    private final String schemaName;");
        w.println("    private final String idAttributeName;");
        w.println("    private final Class<?> idAttributeType;");
        w.println("    private final PrimaryKeyMetadata primaryKeyMetadata;");
        w.println("    private final boolean hasVersionAttribute;");
        w.println("    private final String versionAttributeName;");
        w.println("    private final Set<String> persistentAttributeNames;");
        w.println("    private final List<String> persistentAttributeOrder;");
        // Inheritance support
        w.println("    private final jakarta.persistence.InheritanceType inheritanceType;");
        w.println("    private final String discriminatorColumn;");
        w.println("    private final String discriminatorValue;");
        w.println("    private final Class<?> parentEntityClass;");
        w.println("    private final List<Class<?>> subclassEntityClasses;");
        w.println();
    }

    // ───────────────────── Constructor ─────────────────────

    private void writeConstructor(PrintWriter w, EntityScanner.EntityMetadata entity) {
        String simpleName = entity.type().getSimpleName().toString();

        w.println("    " + simpleName + "Metadata() {");
        w.println("        this.entityClass = " + simpleName + ".class;");
        w.println("        this.entityName = \"" + simpleName + "\";");
        w.println("        this.tableName = \"" + entity.tableName() + "\";");
        w.println("        this.schemaName = \"" + entity.schema() + "\";");
        w.println("        this.idAttributeName = \"" + entity.idAttribute().name() + "\";");
        w.println("        this.idAttributeType = " + entity.idAttribute().javaTypeFqn() + ".class;");
        w.println("        this.primaryKeyMetadata = new PrimaryKeyMetadata(");
        w.println("            \"" + entity.idAttribute().name() + "\",");
        w.println("            " + entity.idAttribute().javaTypeFqn() + ".class,");
        w.println("            " + getGenerationType(entity.idAttribute()) + ",");
        w.println("            " + getGeneratorName(entity.idAttribute()) + ");");
        w.println("        this.hasVersionAttribute = " + (entity.versionAttribute() != null) + ";");

        if (entity.versionAttribute() != null) {
            w.println("        this.versionAttributeName = \"" + entity.versionAttribute().name() + "\";");
        } else {
            w.println("        this.versionAttributeName = null;");
        }

        if (!entity.attributes().isEmpty()) {
            writeAttributesSetAndList(w, entity);
        } else {
            w.println("        this.persistentAttributeNames = Collections.emptySet();");
            w.println("        this.persistentAttributeOrder = Collections.emptyList();");
        }

        // Inheritance support
        io.vidocq.mansart.persistence.processor.parse.InheritanceParser.InheritanceInfo inheritanceInfo = entity.inheritanceInfo();
        if (inheritanceInfo != null) {
            // @Inheritance without an explicit strategy defaults to SINGLE_TABLE (JPA 11.1.24)
            String strategy = inheritanceInfo.strategy() != null
                    ? String.valueOf(inheritanceInfo.strategy()) : "SINGLE_TABLE";
            w.println("        this.inheritanceType = jakarta.persistence.InheritanceType." + strategy + ";");
            w.println("        this.discriminatorColumn = " + stringLiteralOrNull(inheritanceInfo.discriminatorColumn()) + ";");
            w.println("        this.discriminatorValue = " + stringLiteralOrNull(inheritanceInfo.discriminatorValue()) + ";");
            if (inheritanceInfo.parentEntity() != null) {
                w.println("        this.parentEntityClass = " + inheritanceInfo.parentEntity().getQualifiedName() + ".class;");
            } else {
                w.println("        this.parentEntityClass = null;");
            }
            w.println("        this.subclassEntityClasses = java.util.Collections.emptyList();");
        } else {
            w.println("        this.inheritanceType = null;");
            w.println("        this.discriminatorColumn = null;");
            w.println("        this.discriminatorValue = null;");
            w.println("        this.parentEntityClass = null;");
            w.println("        this.subclassEntityClasses = java.util.Collections.emptyList();");
        }

        w.println("    }");
        w.println();
    }

    private void writeAttributesSetAndList(PrintWriter w, EntityScanner.EntityMetadata entity) {
        w.println("        Set<String> _names = new java.util.HashSet<>();");
        w.println("        List<String> _order = new java.util.ArrayList<>();");

        for (EntityScanner.AttributeMetadata attr : entity.attributes()) {
            // Use column name if specified, otherwise use field/property name
            String columnName = attr.columnName() != null ? attr.columnName() : attr.name();
            w.println("        _names.add(\"" + columnName + "\");");
            w.println("        _order.add(\"" + columnName + "\");");
        }

        w.println("        this.persistentAttributeNames = Collections.unmodifiableSet(_names);");
        w.println("        this.persistentAttributeOrder = Collections.unmodifiableList(_order);");
        w.println();
    }

    // ───────────────────── Interface methods ─────────────────────

    private void writeInterfaceMethods(PrintWriter w, EntityScanner.EntityMetadata entity) {
        w.println("    @Override");
        w.println("    public Class<?> entityClass() {");
        w.println("        return entityClass;");
        w.println("    }");
        w.println();

        w.println("    @Override");
        w.println("    public String entityName() {");
        w.println("        return entityName;");
        w.println("    }");
        w.println();

        w.println("    @Override");
        w.println("    public String tableName() {");
        w.println("        return tableName;");
        w.println("    }");
        w.println();

        w.println("    @Override");
        w.println("    public String schemaName() {");
        w.println("        return schemaName;");
        w.println("    }");
        w.println();

        w.println("    @Override");
        w.println("    public String getIdAttributeName() {");
        w.println("        return idAttributeName;");
        w.println("    }");
        w.println();

        w.println("    @Override");
        w.println("    public Class<?> getIdAttributeType() {");
        w.println("        return idAttributeType;");
        w.println("    }");
        w.println();

        w.println("    @Override");
        w.println("    public PrimaryKeyMetadata getPrimaryKeyMetadata() {");
        w.println("        return primaryKeyMetadata;");
        w.println("    }");
        w.println();

        w.println("    @Override");
        w.println("    public boolean hasVersionAttribute() {");
        w.println("        return hasVersionAttribute;");
        w.println("    }");
        w.println();

        w.println("    @Override");
        w.println("    public String getVersionAttributeName() {");
        w.println("        return versionAttributeName;");
        w.println("    }");
        w.println();

        w.println("    @Override");
        w.println("    public Set<String> getPersistentAttributeNames() {");
        w.println("        return persistentAttributeNames;");
        w.println("    }");
        w.println();

        w.println("    @Override");
        w.println("    public List<String> getPersistentAttributeOrder() {");
        w.println("        return persistentAttributeOrder;");
        w.println("    }");
        w.println();

        w.println("    @Override");
        w.println("    public EntityState getState(Object entity) {");
        w.println("        // TODO: Implement state tracking");
        w.println("        return EntityState.NEW;");
        w.println("    }");
        w.println();

        w.println("    @Override");
        w.println("    public boolean isEntity(Class<?> clazz) {");
        w.println("        return entityClass.equals(clazz);");
        w.println("    }");
        w.println();

        // Inheritance methods
        w.println("    @Override");
        w.println("    public jakarta.persistence.InheritanceType getInheritanceType() {");
        w.println("        return inheritanceType;");
        w.println("    }");
        w.println();

        w.println("    @Override");
        w.println("    public String getDiscriminatorColumn() {");
        w.println("        return discriminatorColumn;");
        w.println("    }");
        w.println();

        w.println("    @Override");
        w.println("    public String getDiscriminatorValue() {");
        w.println("        return discriminatorValue;");
        w.println("    }");
        w.println();

        w.println("    @Override");
        w.println("    public Class<?> getParentEntityClass() {");
        w.println("        return parentEntityClass;");
        w.println("    }");
        w.println();

        w.println("    @Override");
        w.println("    public java.util.List<Class<?>> getSubclassEntityClasses() {");
        w.println("        return subclassEntityClasses;");
        w.println("    }");
        w.println();

        writeRelationshipMethods(w, entity);
    }

    // ───────────────────── Relationship metadata ─────────────────────

    private void writeRelationshipMethods(PrintWriter w, EntityScanner.EntityMetadata entity) {
        java.util.List<EntityScanner.AttributeMetadata> relAttrs = new java.util.ArrayList<>();
        for (EntityScanner.AttributeMetadata attr : entity.attributes()) {
            if (attr.relationshipInfo() != null) {
                relAttrs.add(attr);
            }
        }

        if (relAttrs.isEmpty()) {
            w.println("    @Override");
            w.println("    public io.vidocq.mansart.persistence.spi.RelationshipMetadata getRelationshipMetadata(String attributeName) {");
            w.println("        return null;");
            w.println("    }");
            w.println();

            w.println("    @Override");
            w.println("    public boolean isRelationship(String attributeName) {");
            w.println("        return false;");
            w.println("    }");
            w.println();

            w.println("    @Override");
            w.println("    public Set<String> getRelationshipAttributeNames() {");
            w.println("        return EMPTY_SET;");
            w.println("    }");
            w.println();
            return;
        }

        w.println("    private static final Map<String, io.vidocq.mansart.persistence.spi.RelationshipMetadata> RELATIONSHIPS;");
        w.println("    static {");
        w.println("        final Map<String, io.vidocq.mansart.persistence.spi.RelationshipMetadata> _rels = new HashMap<>();");
        for (EntityScanner.AttributeMetadata attr : relAttrs) {
            RelationshipInfo rel = attr.relationshipInfo();
            String key = attr.columnName() != null ? attr.columnName() : attr.name();
            String targetName = targetEntityClassName(rel.targetEntityName());
            String target = targetName == null ? "null" : targetName + ".class";
            String joinColumn = rel.joinColumnName() == null || rel.joinColumnName().isEmpty()
                    ? "null" : "\"" + rel.joinColumnName() + "\"";
            String referencedColumn = rel.referencedColumnName() == null || rel.referencedColumnName().isEmpty()
                    ? "null" : "\"" + rel.referencedColumnName() + "\"";
            w.println("        _rels.put(\"" + key + "\", new GeneratedRelationshipMetadata(");
            w.println("            io.vidocq.mansart.persistence.spi.RelationshipMetadata.RelationshipType." + rel.type().name() + ",");
            w.println("            " + target + ",");
            w.println("            " + joinColumn + ",");
            w.println("            " + rel.joinColumnNullable() + ",");
            w.println("            " + rel.optional() + ",");
            w.println("            io.vidocq.mansart.persistence.spi.RelationshipMetadata.FetchType." + fetchTypeName(rel) + ",");
            w.println("            " + cascadeTypesLiteral(rel) + ",");
            w.println("            " + rel.orphanRemoval() + ",");
            w.println("            " + referencedColumn + "));");
        }
        w.println("        RELATIONSHIPS = Collections.unmodifiableMap(_rels);");
        w.println("    }");
        w.println();

        w.println("    @Override");
        w.println("    public io.vidocq.mansart.persistence.spi.RelationshipMetadata getRelationshipMetadata(String attributeName) {");
        w.println("        return RELATIONSHIPS.get(attributeName);");
        w.println("    }");
        w.println();

        w.println("    @Override");
        w.println("    public boolean isRelationship(String attributeName) {");
        w.println("        return RELATIONSHIPS.containsKey(attributeName);");
        w.println("    }");
        w.println();

        w.println("    @Override");
        w.println("    public Set<String> getRelationshipAttributeNames() {");
        w.println("        return RELATIONSHIPS.keySet();");
        w.println("    }");
        w.println();
    }

    /**
     * Normalizes the scanner's target entity name into a class literal base.
     * To-many attributes carry the full collection type (e.g.
     * {@code java.util.List<com.acme.Book>}) — the element type is the target.
     */
    private String targetEntityClassName(String targetEntityName) {
        if (targetEntityName == null || targetEntityName.isEmpty()) {
            return null;
        }
        String name = targetEntityName;
        int lt = name.indexOf('<');
        if (lt >= 0) {
            int gt = name.lastIndexOf('>');
            name = name.substring(lt + 1, gt > lt ? gt : name.length());
            // Map<K, V> associations target the value type
            int comma = name.lastIndexOf(',');
            if (comma >= 0) {
                name = name.substring(comma + 1);
            }
        }
        name = name.trim();
        return name.isEmpty() ? null : name;
    }

    private String fetchTypeName(RelationshipInfo rel) {
        if (rel.fetchType() != null) {
            return rel.fetchType().name();
        }
        // JPA defaults: to-many associations are LAZY, to-one associations are EAGER
        return switch (rel.type()) {
            case ONE_TO_MANY, MANY_TO_MANY -> "LAZY";
            case MANY_TO_ONE, ONE_TO_ONE -> "EAGER";
        };
    }

    private String cascadeTypesLiteral(RelationshipInfo rel) {
        StringBuilder sb = new StringBuilder("new io.vidocq.mansart.persistence.spi.RelationshipMetadata.CascadeType[] {");
        if (rel.cascadeTypes() != null) {
            for (int i = 0; i < rel.cascadeTypes().length; i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                sb.append("io.vidocq.mansart.persistence.spi.RelationshipMetadata.CascadeType.")
                        .append(rel.cascadeTypes()[i].name());
            }
        }
        return sb.append("}").toString();
    }

    // ───────────────────── Accessor methods (DEBT-05) ─────────────────────

    private void writeAccessorMethods(PrintWriter w, EntityScanner.EntityMetadata entity) {
        // accessor(String)
        w.println("    @Override");
        w.println("    public io.vidocq.mansart.persistence.spi.AttributeAccessor accessor(String attributeName) {");
        w.println("        for (io.vidocq.mansart.persistence.spi.AttributeAccessor _acc : ACCESSORS) {");
        w.println("            if (_acc.name().equals(attributeName)) return _acc;");
        w.println("        }");
        w.println("        throw new IllegalArgumentException(\"Unknown attribute: \" + attributeName);");
        w.println("    }");
        w.println();

        // readAttribute
        w.println("    @Override");
        w.println("    public Object readAttribute(Object entity, String attributeName) {");
        w.println("        return accessor(attributeName).read(entity);");
        w.println("    }");
        w.println();

        // writeAttribute
        w.println("    @Override");
        w.println("    public void writeAttribute(Object entity, String attributeName, Object value) {");
        w.println("        accessor(attributeName).write(entity, value);");
        w.println("    }");
        w.println();

        // createInstance
        w.println("    @Override");
        w.println("    public Object createInstance() {");
        w.println("        try {");
        w.println("            return INSTANCE_CONSTRUCTOR.invoke();");
        w.println("        } catch (java.lang.Throwable ex) {");
        w.println("            throw new RuntimeException(\"Cannot instantiate entity\", ex);");
        w.println("        }");
        w.println("    }");
        w.println();

        // lifecycleCallbacks
        w.println("    @Override");
        w.println("    public LifecycleCallbackDispatcher lifecycleCallbacks() {");
        w.println("        return CALLBACKS;");
        w.println("    }");
        w.println();
    }

    // ───────────────────── Helper methods ─────────────────────

    private void writeHelperMethods(PrintWriter w, EntityScanner.EntityMetadata entity) {
        // No-op — the closing brace is written by generate() after this method
    }

    // ───────────────────── Nested SimpleAccessor ─────────────────────

    /**
     * Generates a nested {@code SimpleAccessor<T>} class that wraps a getter
     * and setter {@code MethodHandle}.  This mirrors the mansart-data pattern
     * where each attribute accessor holds pre-resolved handles.
     */
    private void writeSimpleAccessorNested(PrintWriter w) {
        w.println("    /**");
        w.println("     * Pre-compiled accessor for a single persistent attribute.");
        w.println("     */");
        w.println("    private static final class SimpleAccessor<T> implements io.vidocq.mansart.persistence.spi.AttributeAccessor {");
        w.println("        private final String name;");
        w.println("        private final Class<?> type;");
        w.println("        private final MethodHandle getter;");
        w.println("        private final MethodHandle setter;");
        w.println();
        w.println("        SimpleAccessor(String name, Class<?> type, MethodHandle getter, MethodHandle setter) {");
        w.println("            this.name = name;");
        w.println("            this.type = type;");
        w.println("            this.getter = getter;");
        w.println("            this.setter = setter;");
        w.println("        }");
        w.println();
        w.println("        @Override public String name() { return name; }");
        w.println("        @Override public Class<?> type() { return type; }");
        w.println();
        w.println("        @Override");
        w.println("        public Object read(Object entity) {");
        w.println("            try { return getter.invoke(entity); }");
        w.println("            catch (java.lang.Throwable ex) { throw new RuntimeException(ex); }");
        w.println("        }");
        w.println();
        w.println("        @Override");
        w.println("        public void write(Object entity, Object value) {");
        w.println("            if (setter == null) {");
        w.println("                throw new UnsupportedOperationException(\"Attribute '\" + name + \"' has no setter\");");
        w.println("            }");
        w.println("            try { setter.invoke(entity, value); }");
        w.println("            catch (java.lang.Throwable ex) { throw new RuntimeException(ex); }");
        w.println("        }");
        w.println("    }");
        w.println();
    }

    /**
     * Generates a nested {@code GeneratedRelationshipMetadata} class holding the
     * immutable relationship description captured by the scanner. Only emitted
     * when the entity actually declares relationship attributes.
     */
    private void writeRelationshipMetadataNested(PrintWriter w, EntityScanner.EntityMetadata entity) {
        boolean hasRelationship = false;
        for (EntityScanner.AttributeMetadata attr : entity.attributes()) {
            if (attr.relationshipInfo() != null) {
                hasRelationship = true;
                break;
            }
        }
        if (!hasRelationship) {
            return;
        }

        w.println("    /**");
        w.println("     * Immutable relationship description generated from annotations.");
        w.println("     */");
        w.println("    private static final class GeneratedRelationshipMetadata implements io.vidocq.mansart.persistence.spi.RelationshipMetadata {");
        w.println("        private final RelationshipType relationshipType;");
        w.println("        private final Class<?> targetEntity;");
        w.println("        private final String joinColumnName;");
        w.println("        private final boolean joinColumnNullable;");
        w.println("        private final boolean optional;");
        w.println("        private final FetchType fetchType;");
        w.println("        private final CascadeType[] cascadeTypes;");
        w.println("        private final boolean orphanRemoval;");
        w.println("        private final String referencedColumnName;");
        w.println();
        w.println("        GeneratedRelationshipMetadata(RelationshipType relationshipType, Class<?> targetEntity,");
        w.println("                String joinColumnName, boolean joinColumnNullable, boolean optional,");
        w.println("                FetchType fetchType, CascadeType[] cascadeTypes, boolean orphanRemoval,");
        w.println("                String referencedColumnName) {");
        w.println("            this.relationshipType = relationshipType;");
        w.println("            this.targetEntity = targetEntity;");
        w.println("            this.joinColumnName = joinColumnName;");
        w.println("            this.joinColumnNullable = joinColumnNullable;");
        w.println("            this.optional = optional;");
        w.println("            this.fetchType = fetchType;");
        w.println("            this.cascadeTypes = cascadeTypes;");
        w.println("            this.orphanRemoval = orphanRemoval;");
        w.println("            this.referencedColumnName = referencedColumnName;");
        w.println("        }");
        w.println();
        w.println("        @Override public RelationshipType getRelationshipType() { return relationshipType; }");
        w.println("        @Override public Class<?> getTargetEntity() { return targetEntity; }");
        w.println("        @Override public String getJoinColumnName() { return joinColumnName; }");
        w.println("        @Override public boolean isJoinColumnNullable() { return joinColumnNullable; }");
        w.println("        @Override public boolean isOptional() { return optional; }");
        w.println("        @Override public FetchType getFetchType() { return fetchType; }");
        w.println("        @Override public CascadeType[] getCascadeTypes() { return cascadeTypes.clone(); }");
        w.println("        @Override public boolean isOrphanRemoval() { return orphanRemoval; }");
        w.println("        @Override public String getReferencedColumnName() { return referencedColumnName; }");
        w.println("    }");
        w.println();
    }

    // ───────────────────── Utility ─────────────────────

    /** Emits a quoted string literal, or the {@code null} literal for a null/"null" value. */
    private static String stringLiteralOrNull(Object value) {
        if (value == null || "null".equals(String.valueOf(value))) {
            return "null";
        }
        return "\"" + value + "\"";
    }

    private String getGenerationType(EntityScanner.AttributeMetadata attribute) {
        if (attribute.genValueInfo() == null) {
            // No @GeneratedValue: the identifier is application-assigned
            return "null";
        }
        return "jakarta.persistence.GenerationType." + attribute.genValueInfo().generationType();
    }

    private String getGeneratorName(EntityScanner.AttributeMetadata attribute) {
        if (attribute.genValueInfo() == null || attribute.genValueInfo().generatorName() == null) {
            return "null";
        }
        return "\"" + attribute.genValueInfo().generatorName() + "\"";
    }

    private String packageOf(String fqn) {
        int i = fqn.lastIndexOf('.');
        return i < 0 ? "" : fqn.substring(0, i);
    }

    /**
     * Strips generic type parameters from a fully-qualified type name.
     * E.g. "java.util.List&lt;java.lang.String&gt;" → "java.util.List".
     * Required because MethodHandles.findGetter/findSetter accept only raw class types.
     */
    private static String stripGenerics(String fqn) {
        int angle = fqn.indexOf('<');
        return angle < 0 ? fqn : fqn.substring(0, angle);
    }

    /**
     * Returns the boxed Java type for MethodHandles.lookup().
     * MethodHandles.findGetter/findSetter require the raw declared type,
     * not the boxed wrapper (e.g. int.class, not Integer.class).
     */
    static String box(String javaType) {
        return switch (javaType) {
            case "boolean" -> "java.lang.Boolean";
            case "byte"    -> "java.lang.Byte";
            case "short"   -> "java.lang.Short";
            case "int"     -> "java.lang.Integer";
            case "long"    -> "java.lang.Long";
            case "float"   -> "java.lang.Float";
            case "double"  -> "java.lang.Double";
            case "char"    -> "java.lang.Character";
            default        -> javaType;
        };
    }
}

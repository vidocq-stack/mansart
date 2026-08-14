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

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.ArrayList;

/**
 * Generates the entity metadata implementation class.
 *
 * <p>This class creates a metadata class that implements the SPI's
 * {@code io.vidocq.mansart.persistence.spi.EntityMetadata} interface,
 * providing runtime-accessible metadata about the entity.
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
            w.println("import java.util.List;");
            w.println("import java.util.Set;");
            w.println("import java.util.HashMap;");
            w.println("import java.util.Map;");
            w.println("import java.util.ArrayList;");
            w.println("import java.util.Collections;");
            w.println("import " + GENERATED_ANNOTATION + ";");
            w.println();
            w.println("@Generated(\"io.vidocq.mansart.persistence.processor.MansartProcessor\")");
            w.println("final class " + className + " implements EntityMetadata {");
            w.println();
            w.println("    private static final Set<String> EMPTY_SET = Collections.emptySet();");
            w.println("    private static final List<String> EMPTY_LIST = Collections.emptyList();");
            w.println();

            writeFields(w, entity);
            writeConstructor(w, entity);
            writeInterfaceMethods(w, entity);
            writeHelperMethods(w, entity);
            
            w.println("}");
        }
    }

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
        w.println();
    }

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
            w.println("        this.persistentAttributeNames = EMPTY_SET;");
            w.println("        this.persistentAttributeOrder = EMPTY_LIST;");
        }

        w.println("    }");
        w.println();
    }

    private void writeAttributesSetAndList(PrintWriter w, EntityScanner.EntityMetadata entity) {
        w.println("        Set<String> _names = new ArrayList<>();");
        w.println("        List<String> _order = new ArrayList<>();");
        
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
    }

    private void writeHelperMethods(PrintWriter w, EntityScanner.EntityMetadata entity) {
        w.println("}");
        w.println();
    }

    private String getGenerationType(EntityScanner.AttributeMetadata attribute) {
        if (attribute.genValueInfo() == null) {
            return "jakarta.persistence.GenerationType.AUTO";
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
}

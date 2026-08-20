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

/**
 * Generates the static metamodel class for JPA entities.
 *
 * <p>This class creates a JPA-compliant static metamodel class (e.g., {@code Book_})
 * that provides type-safe access to entity attributes for use in type-safe queries.
 */
public final class StaticMetamodelGenerator {


    private final SourceSink sink;

    /**
     * Creates a new StaticMetamodelGenerator.
     *
     * @param sink the source sink for writing generated files
     */
    public StaticMetamodelGenerator(SourceSink sink) {
        this.sink = sink;
    }

    /**
     * Generates the static metamodel for the given scanned entity.
     *
     * @param entity the scanned entity metadata
     * @throws IOException if writing fails
     */
    public void generate(EntityScanner.EntityMetadata entity) throws IOException {
        String pkg = packageOf(entity.type().getQualifiedName().toString());
        String simpleName = entity.type().getSimpleName().toString();
        String className = simpleName + "_";
        String fqn = pkg.isEmpty() ? className : pkg + "." + className;

        try (PrintWriter w = new PrintWriter(sink.createSource(fqn))) {
            if (!pkg.isEmpty()) {
                w.println("package " + pkg + ";");
                w.println();
            }

            w.println("import jakarta.persistence.metamodel.StaticMetamodel;");
            w.println("import jakarta.persistence.metamodel.SingularAttribute;");
            w.println("import jakarta.persistence.metamodel.SetAttribute;");
            w.println("import jakarta.persistence.metamodel.ListAttribute;");
            w.println("import jakarta.persistence.metamodel.MapAttribute;");
            w.println();
            w.println("@StaticMetamodel(" + simpleName + ".class)");
            w.println("public abstract class " + className + " {");
            w.println();

            // Write static attributes for all fields
            for (EntityScanner.AttributeMetadata attr : entity.attributes()) {
                writeAttribute(w, attr, entity);
            }

            w.println();
            w.println("    // Static metamodel initialization");
            w.println("    static {");
            w.println("        // Attributes are initialized by the JPA provider");
            w.println("    }");
            w.println();
            w.println("    private " + className + "() {}");
            w.println("}");
        }
    }

    private void writeAttribute(PrintWriter w, EntityScanner.AttributeMetadata attr, EntityScanner.EntityMetadata entity) {
        String javaType = attr.javaTypeFqn();
        String attrName = attr.name();
        String entitySimpleName = entitySimpleTypeName(entity);

        // Determine attribute type based on kind
        switch (attr.kind()) {
            case ID:
                // For ID attributes, use the specific id type
                String idBoxed = idBoxedType(attr);
                String idType = "SingularAttribute<" + entitySimpleName + ", " + idBoxed + ">";
                w.println("    public static volatile " + idType + " " + attrName + ";");
                break;

            case VERSION:
                w.println("    public static volatile SingularAttribute<" + entitySimpleName + ", " + box(javaType) + "> " + attrName + ";");
                break;

            case REFERENCE:
                // For references, use the target entity type
                w.println("    public static volatile SingularAttribute<" + entitySimpleName + ", " + box(javaType) + "> " + attrName + ";");
                break;

            case ENUM:
                w.println("    @SuppressWarnings({\"rawtypes\", \"unchecked\"})");
                w.println("    public static volatile SingularAttribute " + attrName + ";");
                break;

            case TEXT:
            case NUMERIC:
            case BOOLEAN:
            case TEMPORAL:
            case OBJECT:
            default:
                w.println("    public static volatile SingularAttribute<" + entitySimpleName + ", " + box(javaType) + "> " + attrName + ";");
                break;
        }
    }

    private String box(String javaType) {
        return switch (javaType) {
            case "boolean" -> "java.lang.Boolean";
            case "byte" -> "java.lang.Byte";
            case "short" -> "java.lang.Short";
            case "int" -> "java.lang.Integer";
            case "long" -> "java.lang.Long";
            case "float" -> "java.lang.Float";
            case "double" -> "java.lang.Double";
            case "char" -> "java.lang.Character";
            default -> javaType;
        };
    }

    private String packageOf(String fqn) {
        int i = fqn.lastIndexOf('.');
        return i < 0 ? "" : fqn.substring(0, i);
    }

    private String entitySimpleTypeName(EntityScanner.EntityMetadata entity) {
        return entity.type().getSimpleName().toString();
    }

    private String idBoxedType(EntityScanner.AttributeMetadata attr) {
        String type = attr.javaTypeFqn();
        return box(type);
    }
}

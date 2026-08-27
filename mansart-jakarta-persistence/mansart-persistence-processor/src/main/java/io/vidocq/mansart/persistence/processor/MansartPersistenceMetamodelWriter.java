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

import javax.annotation.processing.Filer;
import javax.annotation.processing.FilerException;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Writes the JPA-standard static metamodel class ({@code ClassName_}) for an entity.
 *
 * <p>For an entity {@code Employee}, generates {@code Employee_} — an abstract class
 * with {@code public static volatile SingularAttribute<Employee, T>} fields
 * for every persistent attribute, plus {@code public static final String} constants.</p>
 *
 * @see <a href="https://jakarta.ee/specifications/persistence/3.2/">Jakarta Persistence 3.2 Spec, Section 6.2</a>
 */
final class MansartPersistenceMetamodelWriter {

    private final Filer filer;

    MansartPersistenceMetamodelWriter(Filer filer) {
        this.filer = filer;
    }

    /**
     * Generates the JPA static metamodel class for the given entity descriptor.
     *
     * @param e the entity descriptor (must have at least one @Id attribute)
     * @throws IOException if the file cannot be written
     */
    void write(EntityScanner.EntityDescriptor e) throws IOException {
        String pkg = packageOf(e.type().getQualifiedName().toString());
        String simple = e.type().getSimpleName().toString();
        String className = simple + "_";
        String fqn = pkg.isEmpty() ? className : pkg + "." + className;

        FileObject file = filer.createResource(
                StandardLocation.SOURCE_OUTPUT, "", fqn + ".java");

        try (PrintWriter w = new PrintWriter(file.openWriter())) {
            if (!pkg.isEmpty()) {
                w.println("package " + pkg + ";");
                w.println();
            }
            w.println("import jakarta.persistence.metamodel.SingularAttribute;");
            w.println("import jakarta.persistence.metamodel.StaticMetamodel;");
            w.println("import javax.annotation.processing.Generated;");
            w.println();
            w.println("@Generated(\"io.vidocq.mansart.persistence.processor.MansartPersistenceProcessor\")");
            w.println("@StaticMetamodel(" + simple + ".class)");
            w.println("public abstract class " + className + " {");
            w.println();

            for (EntityScanner.AttributeDescriptor a : e.attributes()) {
                EntityScanner.AttributeKind kind = a.kind();
                if (kind == EntityScanner.AttributeKind.COLLECTION
                        || kind == EntityScanner.AttributeKind.LIST
                        || kind == EntityScanner.AttributeKind.MAP
                        || kind == EntityScanner.AttributeKind.ONE_TO_MANY
                        || kind == EntityScanner.AttributeKind.MANY_TO_MANY) {
                    continue;
                }
                String boxed = box(a.javaTypeFqn());
                w.println("    public static volatile SingularAttribute<" + simple + ", " + boxed + "> " + a.name() + ";");
            }

            writePluralAttributes(w, simple, e.attributes());

            w.println();
            for (EntityScanner.AttributeDescriptor a : e.attributes()) {
                EntityScanner.AttributeKind kind = a.kind();
                if (kind == EntityScanner.AttributeKind.COLLECTION
                        || kind == EntityScanner.AttributeKind.LIST
                        || kind == EntityScanner.AttributeKind.MAP
                        || kind == EntityScanner.AttributeKind.ONE_TO_MANY
                        || kind == EntityScanner.AttributeKind.MANY_TO_MANY) {
                    continue;
                }
                w.println("    public static final String " + screamingSnake(a.name()) + " = \"" + a.name() + "\";");
            }

            w.println("}");
        }
    }

    /**
     * Generates the JPA static metamodel class for the given embeddable descriptor.
     * Embeddables do not have @Id or @Version fields.
     */
    void writeEmbeddable(EntityScanner.EmbeddableDescriptor e) throws IOException {
        String pkg = packageOf(e.type().getQualifiedName().toString());
        String simple = e.type().getSimpleName().toString();
        String className = simple + "_";
        String fqn = pkg.isEmpty() ? className : pkg + "." + className;

        FileObject file = filer.createResource(
                StandardLocation.SOURCE_OUTPUT, "", fqn + ".java");

        try (PrintWriter w = new PrintWriter(file.openWriter())) {
            if (!pkg.isEmpty()) {
                w.println("package " + pkg + ";");
                w.println();
            }
            w.println("import jakarta.persistence.metamodel.SingularAttribute;");
            w.println("import jakarta.persistence.metamodel.StaticMetamodel;");
            w.println("import javax.annotation.processing.Generated;");
            w.println();
            w.println("@Generated(\"io.vidocq.mansart.persistence.processor.MansartPersistenceProcessor\")");
            w.println("@StaticMetamodel(" + simple + ".class)");
            w.println("public abstract class " + className + " {");
            w.println();

            for (EntityScanner.AttributeDescriptor a : e.attributes()) {
                EntityScanner.AttributeKind kind = a.kind();
                if (kind == EntityScanner.AttributeKind.COLLECTION
                        || kind == EntityScanner.AttributeKind.LIST
                        || kind == EntityScanner.AttributeKind.MAP) {
                    continue;
                }
                String boxed = box(a.javaTypeFqn());
                w.println("    public static volatile SingularAttribute<" + simple + ", " + boxed + "> " + a.name() + ";");
            }

            writePluralAttributes(w, simple, e.attributes());

            w.println();
            for (EntityScanner.AttributeDescriptor a : e.attributes()) {
                w.println("    public static final String " + screamingSnake(a.name()) + " = \"" + a.name() + "\";");
            }

            w.println("}");
        }
    }

    /**
     * Generates the JPA static metamodel class for the given mapped-superclass descriptor.
     */
    void writeMappedSuperclass(EntityScanner.EntityDescriptor e) throws IOException {
        String pkg = packageOf(e.type().getQualifiedName().toString());
        String simple = e.type().getSimpleName().toString();
        String className = simple + "_";
        String fqn = pkg.isEmpty() ? className : pkg + "." + className;

        FileObject file = filer.createResource(
                StandardLocation.SOURCE_OUTPUT, "", fqn + ".java");

        try (PrintWriter w = new PrintWriter(file.openWriter())) {
            if (!pkg.isEmpty()) {
                w.println("package " + pkg + ";");
                w.println();
            }
            w.println("import jakarta.persistence.metamodel.SingularAttribute;");
            w.println("import jakarta.persistence.metamodel.StaticMetamodel;");
            w.println("import javax.annotation.processing.Generated;");
            w.println();
            w.println("@Generated(\"io.vidocq.mansart.persistence.processor.MansartPersistenceProcessor\")");
            w.println("@StaticMetamodel(" + simple + ".class)");
            w.println("public abstract class " + className + " {");
            w.println();

            for (EntityScanner.AttributeDescriptor a : e.attributes()) {
                if (a.kind() == EntityScanner.AttributeKind.ID
                        || a.kind() == EntityScanner.AttributeKind.VERSION) {
                    continue;
                }
                String boxed = box(a.javaTypeFqn());
                w.println("    public static volatile SingularAttribute<" + simple + ", " + boxed + "> " + a.name() + ";");
            }

            writePluralAttributes(w, simple, e.attributes());

            w.println();
            for (EntityScanner.AttributeDescriptor a : e.attributes()) {
                if (a.kind() == EntityScanner.AttributeKind.ID
                        || a.kind() == EntityScanner.AttributeKind.VERSION) {
                    continue;
                }
                w.println("    public static final String " + screamingSnake(a.name()) + " = \"" + a.name() + "\";");
            }

            w.println("}");
        }
    }

    /**
     * Writes plural attribute fields (SetAttribute, ListAttribute, MapAttribute)
     * for any @ElementCollection, @OneToMany, or @ManyToMany fields in the descriptor.
     */
    private void writePluralAttributes(PrintWriter w, String simple,
                                        java.util.List<EntityScanner.AttributeDescriptor> attributes) {
        for (EntityScanner.AttributeDescriptor a : attributes) {
            switch (a.kind()) {
                case COLLECTION ->
                    w.println("    public static volatile SetAttribute<" + simple + ", " + elementType(a.javaTypeFqn()) + "> " + a.name() + ";");
                case LIST ->
                    w.println("    public static volatile ListAttribute<" + simple + ", " + elementType(a.javaTypeFqn()) + "> " + a.name() + ";");
                case MAP ->
                    w.println("    public static volatile MapAttribute<" + simple + ", " + mapKeyType(a.javaTypeFqn()) + ", " + mapValueType(a.javaTypeFqn()) + "> " + a.name() + ";");
                case ONE_TO_MANY, MANY_TO_MANY ->
                    w.println("    public static volatile SetAttribute<" + simple + ", " + elementType(a.javaTypeFqn()) + "> " + a.name() + ";");
                default -> { /* SingularAttribute already written */ }
            }
        }
    }

    /** Extracts the element type from a collection type string (e.g. "java.util.Set<ZipCode>" → "ZipCode"). */
    private static String elementType(String fqn) {
        int start = fqn.indexOf('<');
        int end = fqn.lastIndexOf('>');
        if (start >= 0 && end > start) {
            return fqn.substring(start + 1, end).trim();
        }
        return fqn;
    }

    /** Extracts the key type from a Map type string (e.g. "java.util.Map<ZipCode, String>" → "ZipCode"). */
    private static String mapKeyType(String fqn) {
        int start = fqn.indexOf('<');
        int comma = fqn.indexOf(',', start);
        int end = fqn.lastIndexOf('>');
        if (start >= 0 && comma > start && end > comma) {
            return fqn.substring(start + 1, comma).trim();
        }
        return fqn;
    }

    /** Extracts the value type from a Map type string (e.g. "java.util.Map<ZipCode, String>" → "String"). */
    private static String mapValueType(String fqn) {
        int start = fqn.indexOf('<');
        int comma = fqn.indexOf(',', start);
        int end = fqn.lastIndexOf('>');
        if (start >= 0 && comma > start && end > comma) {
            return fqn.substring(comma + 1, end).trim();
        }
        return fqn;
    }

    /** Boxes a primitive type to its wrapper class for use in generic type parameters. */
    private static String box(String fqn) {
        return switch (fqn) {
            case "boolean"  -> "java.lang.Boolean";
            case "byte"     -> "java.lang.Byte";
            case "short"    -> "java.lang.Short";
            case "int"      -> "java.lang.Integer";
            case "long"     -> "java.lang.Long";
            case "float"    -> "java.lang.Float";
            case "double"   -> "java.lang.Double";
            default         -> fqn;
        };
    }

    /** Converts {@code camelCase} to {@code SCREAMING_SNAKE_CASE}. */
    private static String screamingSnake(String camel) {
        StringBuilder sb = new StringBuilder(camel.length() + 4);
        for (int i = 0; i < camel.length(); i++) {
            char c = camel.charAt(i);
            if (Character.isUpperCase(c) && i > 0) sb.append('_');
            sb.append(Character.toUpperCase(c));
        }
        return sb.toString();
    }

    private static String packageOf(String fqn) {
        int i = fqn.lastIndexOf('.');
        return i < 0 ? "" : fqn.substring(0, i);
    }
}

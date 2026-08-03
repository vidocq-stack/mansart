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
package io.vidocq.mansart.persistence.processor.metamodel;

import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.lang.model.element.*;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

/**
 * Generates static metamodel classes for JPA entities, embeddables, and mapped superclasses.
 * <p>
 * For each managed class (Entity, Embeddable, MappedSuperclass), this writer generates a
 * corresponding metamodel class with the "_" suffix. The generated class contains static
 * attribute models (SingularAttribute or PluralAttribute) for each persistent attribute.
 * <p>
 * The generated metamodel implements Jakarta Persistence 3.2 static metamodel specifications.
 */
public class StaticMetamodelWriter {

    private static final String METAMODEL_SUFFIX = "_";
    
    private final Filer filer;
    private final Messager messager;
    private final Elements elementUtils;
    private final Types typeUtils;
    private final MetamodelTypeResolver typeResolver;

    public StaticMetamodelWriter(Filer filer, Messager messager, Elements elementUtils, Types typeUtils) {
        this.filer = Objects.requireNonNull(filer);
        this.messager = Objects.requireNonNull(messager);
        this.elementUtils = Objects.requireNonNull(elementUtils);
        this.typeUtils = Objects.requireNonNull(typeUtils);
        this.typeResolver = new MetamodelTypeResolver(elementUtils, typeUtils);
    }

    /**
     * Checks if the given element has the specified annotation.
     */
    private boolean hasAnnotation(Element element, String annotationName) {
        for (AnnotationMirror annotation : element.getAnnotationMirrors()) {
            if (annotation.getAnnotationType().toString().equals(annotationName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Generates the static metamodel for the given class.
     *
     * @param typeElement the class to generate metamodel for
     * @return true if the metamodel was generated successfully
     */
    public boolean generate(TypeElement typeElement) {
        String className = typeElement.getQualifiedName().toString();
        String metamodelClassName = className + METAMODEL_SUFFIX;
        
        // Get package information
        PackageElement packageElement = elementUtils.getPackageOf(typeElement);
        String packageName = packageElement.getQualifiedName().toString();
        
        // Collect persistent attributes
        List<AttributeModel> attributes = collectPersistentAttributes(typeElement);
        
        // Determine the type of the original class for javadoc
        String managedType = determineManagedType(typeElement);
        
        // Generate the source code
        String sourceCode = generateMetamodelSource(packageName, metamodelClassName, 
                                                    className, attributes, managedType);
        
        // Write to file
        try {
            JavaFileObject sourceFile = filer.createSourceFile(metamodelClassName);
            try (PrintWriter writer = new PrintWriter(sourceFile.openWriter())) {
                writer.print(sourceCode);
            }
            return true;
        } catch (IOException e) {
            messager.printMessage(Diagnostic.Kind.ERROR,
                "Failed to write metamodel for " + className + ": " + e.getMessage());
            return false;
        }
    }

    /**
     * Collects all persistent attributes from the class and its superclasses.
     */
    private List<AttributeModel> collectPersistentAttributes(TypeElement typeElement) {
        List<AttributeModel> attributes = new ArrayList<>();
        
        // Process the class hierarchy
        Set<TypeElement> processedClasses = new HashSet<>();
        collectAttributesFromHierarchy(typeElement, attributes, processedClasses);
        
        return attributes;
    }

    /**
     * Recursively collects attributes from the class hierarchy.
     */
    private void collectAttributesFromHierarchy(TypeElement typeElement, 
                                                 List<AttributeModel> attributes,
                                                 Set<TypeElement> processedClasses) {
        // Avoid processing the same class multiple times (diamond inheritance)
        if (processedClasses.contains(typeElement)) {
            return;
        }
        processedClasses.add(typeElement);
        
        // Process superclass first (so attributes are ordered from top to bottom)
        TypeMirror superclass = typeElement.getSuperclass();
        if (superclass.getKind() != TypeKind.NONE) {
            TypeElement superclassElement = (TypeElement) typeUtils.asElement(superclass);
            if (superclassElement != null && isManagedClass(superclassElement)) {
                collectAttributesFromHierarchy(superclassElement, attributes, processedClasses);
            }
        }
        
        // Collect attributes from this class
        for (Element enclosedElement : typeElement.getEnclosedElements()) {
            if (enclosedElement.getKind() == ElementKind.FIELD) {
                VariableElement field = (VariableElement) enclosedElement;
                AttributeModel attribute = createAttributeModel(field, typeElement);
                if (attribute != null) {
                    attributes.add(attribute);
                }
            }
        }
    }

    /**
     * Checks if the given class is a managed class (Entity, Embeddable, or MappedSuperclass).
     */
    private boolean isManagedClass(TypeElement typeElement) {
        return hasAnnotation(typeElement, "jakarta.persistence.Entity") ||
               hasAnnotation(typeElement, "jakarta.persistence.Embeddable") ||
               hasAnnotation(typeElement, "jakarta.persistence.MappedSuperclass");
    }

    /**
     * Creates an AttributeModel for the given field.
     *
     * @return AttributeModel or null if the field should not be included in the metamodel
     */
    private AttributeModel createAttributeModel(VariableElement field, TypeElement declaringClass) {
        // Skip static fields
        if (field.getModifiers().contains(Modifier.STATIC)) {
            return null;
        }
        
        // Skip transient fields
        if (hasAnnotation(field, "java.lang.Transient") ||
            hasAnnotation(field, "jakarta.persistence.Transient")) {
            return null;
        }
        
        // Get the attribute name
        String attributeName = field.getSimpleName().toString();
        
        // Get the Java type
        TypeMirror javaType = field.asType();
        String javaTypeName = javaType.toString();
        
        // Get the persistent attribute type (mapped type)
        TypeMirror persistentAttributeType = typeResolver.resolvePersistentAttributeType(field, declaringClass);
        String persistentAttributeTypeName = persistentAttributeType.toString();
        
        // Determine if it's a singular or plural attribute
        boolean isPlural = typeResolver.isPluralAttribute(field);
        
        // Determine if it's an association
        boolean isAssociation = typeResolver.isAssociation(field);
        
        // Determine if it's a basic type
        boolean isBasic = !isAssociation;
        
        // Get the declaration type (field or method - for now, we only support field-based)
        String declarationType = "FIELD";
        
        return new AttributeModel(attributeName, javaTypeName, persistentAttributeTypeName, 
                                  isPlural, isAssociation, isBasic, declarationType);
    }

    /**
     * Determines the managed type for javadoc purposes.
     */
    private String determineManagedType(TypeElement typeElement) {
        if (hasAnnotation(typeElement, "jakarta.persistence.Entity")) {
            return "Entity";
        } else if (hasAnnotation(typeElement, "jakarta.persistence.Embeddable")) {
            return "Embeddable";
        } else if (hasAnnotation(typeElement, "jakarta.persistence.MappedSuperclass")) {
            return "MappedSuperclass";
        }
        return "Managed";
    }

    /**
     * Generates the complete metamodel source code.
     */
    private String generateMetamodelSource(String packageName, String metamodelClassName,
                                         String originalClassName, List<AttributeModel> attributes,
                                         String managedType) {
        StringBuilder builder = new StringBuilder();
        
        // License header
        appendLicenseHeader(builder);
        
        // Package declaration
        if (!packageName.isEmpty()) {
            builder.append("package ").append(packageName).append(";\n\n");
        }
        
        // Import statements
        appendImports(builder);
        builder.append("\n");
        
        // Generated annotation
        builder.append("@Generated(value = \"io.vidocq.mansart.persistence.processor.MansartPersistenceProcessor\",\n");
        builder.append("         date = \"").append(new Date()).append("\",\n");
        builder.append("         comments = \"Generated static metamodel for ")
                  .append(managedType)
                  .append(" ")
                  .append(originalClassName)
                  .append("\")\n");
        builder.append("\n");
        
        // Class javadoc
        builder.append("/**\n");
        builder.append(" * Static metamodel for ").append(originalClassName).append(".\n");
        builder.append(" * This class was generated by Mansart Persistence Processor.\n");
        builder.append(" * DO NOT EDIT MANUALLY.\n");
        builder.append(" */\n");
        
        // Class declaration
        builder.append("public abstract class ").append(getSimpleClassName(metamodelClassName)).append(" {\n\n");
        
        // Public no-arg constructor
        builder.append("    /**\n");
        builder.append("     * Protected no-arg constructor.\n");
        builder.append("     */\n");
        builder.append("    protected ").append(getSimpleClassName(metamodelClassName)).append("() {}\n\n");
        
        // Generate attribute models
        for (AttributeModel attribute : attributes) {
            appendAttributeModel(builder, attribute, originalClassName);
        }
        
        // Close class
        builder.append("}\n");
        
        return builder.toString();
    }

    /**
     * Appends a single attribute model to the source code.
     */
    private void appendAttributeModel(StringBuilder builder, AttributeModel attribute,
                                     String originalClassName) {
        String attributeName = attribute.getName();
        
        // Field javadoc
        builder.append("    /**\n");
        builder.append("     * Attribute model for ").append(attributeName).append(".\n");
        if (attribute.isAssociation()) {
            builder.append("     * This is an association attribute.\n");
        } else {
            builder.append("     * This is a basic attribute.\n");
        }
        if (attribute.isPlural()) {
            builder.append("     * This is a plural attribute.\n");
        } else {
            builder.append("     * This is a singular attribute.\n");
        }
        builder.append("     */\n");
        
        // Generate the attribute declaration
        if (attribute.isPlural()) {
            // PluralAttribute<X, C, E> where X is the declaring type, C is the collection type, E is the element type
            String collectionType = typeResolver.extractCollectionType(attribute.getJavaType());
            String elementType = typeResolver.extractElementType(attribute.getJavaType());
            
            builder.append("    public static volatile PluralAttribute<")
                      .append(originalClassName).append(", ")
                      .append(collectionType).append(", ")
                      .append(elementType).append("> ")
                      .append(attributeName).append(";\n");
        } else {
            // SingularAttribute<X, T> where X is the declaring type, T is the attribute type
            builder.append("    public static volatile SingularAttribute<")
                      .append(originalClassName).append(", ")
                      .append(attribute.getPersistentAttributeType()).append("> ")
                      .append(attributeName).append(";\n");
        }
        
        builder.append("\n");
    }

    /**
     * Appends the license header to the source code.
     */
    private void appendLicenseHeader(StringBuilder builder) {
        builder.append("/*\n");
        builder.append(" * Copyright (c) ${year} Yann Blazart, Antoine Sabot-Durand and the Vidocq contributors\n");
        builder.append(" *\n");
        builder.append(" * This program and the accompanying materials are made available under the\n");
        builder.append(" * terms of the Eclipse Public License 2.0 which is available at\n");
        builder.append(" * https://www.eclipse.org/legal/epl-2.0/\n");
        builder.append(" *\n");
        builder.append(" * This Source Code may also be made available under the following Secondary\n");
        builder.append(" * Licenses when the conditions for such availability set forth in the Eclipse\n");
        builder.append(" * Public License, v. 2.0 are satisfied: GNU General Public License, version 2\n");
        builder.append(" * or any later version, which is available at\n");
        builder.append(" * https://www.gnu.org/licenses/old-licenses/gpl-2.0.html\n");
        builder.append(" *\n");
        builder.append(" * It is also made available under the European Union Public Licence v. 1.2,\n");
        builder.append(" * which is available at\n");
        builder.append(" * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12\n");
        builder.append(" *\n");
        builder.append(" * SPDX-License-Identifier: EPL-2.0 OR EUPL-1.2 OR GPL-2.0-or-later\n");
        builder.append(" */\n");
    }

    /**
     * Appends the necessary import statements.
     */
    private void appendImports(StringBuilder builder) {
        builder.append("import jakarta.annotation.Generated;\n");
        builder.append("import jakarta.persistence.metamodel.Attribute;\n");
        builder.append("import jakarta.persistence.metamodel.BasicType;\n");
        builder.append("import jakarta.persistence.metamodel.Bindable;\n");
        builder.append("import jakarta.persistence.metamodel.PluralAttribute;\n");
        builder.append("import jakarta.persistence.metamodel.SingularAttribute;\n");
        builder.append("import jakarta.persistence.metamodel.StaticMetamodel;\n");
        builder.append("import jakarta.persistence.metamodel.Type;\n");
    }

    /**
     * Extracts the simple class name from a fully qualified name.
     */
    private String getSimpleClassName(String qualifiedName) {
        int lastDot = qualifiedName.lastIndexOf('.');
        return lastDot >= 0 ? qualifiedName.substring(lastDot + 1) : qualifiedName;
    }
}

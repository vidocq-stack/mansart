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
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-1.2
 *
 * SPDX-License-Identifier: EPL-2.0 OR EUPL-1.2 OR GPL-2.0-or-later
 */
package io.vidocq.mansart.persistence.processor.metamodel;

import javax.lang.model.element.*;
import javax.lang.model.type.*;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.util.*;

/**
 * Resolves types for static metamodel generation.
 * <p>
 * This class provides utility methods to determine:
 * <ul>
 *   <li>Whether a field is a plural attribute (collection-valued)</li>
 *   <li>Whether a field is an association (references another entity/embeddable)</li>
 *   <li>The persistent attribute type (mapped type) for a field</li>
 *   <li>Collection and element types for plural attributes</li>
 * </ul>
 */
public class MetamodelTypeResolver {

    private static final Set<String> COLLECTION_TYPES = new HashSet<>(Arrays.asList(
        "java.util.Collection",
        "java.util.List",
        "java.util.Set",
        "java.util.SortedSet",
        "java.util.NavigableSet",
        "java.util.SortedMap",
        "java.util.NavigableMap",
        "java.util.Map"
    ));

    private static final Set<String> BASIC_TYPES = new HashSet<>(Arrays.asList(
        "java.lang.String",
        "java.lang.Integer",
        "java.lang.Long",
        "java.lang.Float",
        "java.lang.Double",
        "java.lang.Boolean",
        "java.lang.Character",
        "java.lang.Byte",
        "java.lang.Short",
        "java.math.BigInteger",
        "java.math.BigDecimal",
        "java.util.Date",
        "java.util.Calendar",
        "java.sql.Date",
        "java.sql.Time",
        "java.sql.Timestamp",
        "java.time.LocalDate",
        "java.time.LocalTime",
        "java.time.LocalDateTime",
        "java.time.OffsetDateTime",
        "java.time.Instant",
        "java.time.ZonedDateTime",
        "java.time.Year",
        "java.time.YearMonth",
        "java.time.MonthDay",
        "byte[]",
        "Byte[]",
        "char[]",
        "Character[]"
    ));

    private final Elements elementUtils;
    private final Types typeUtils;

    public MetamodelTypeResolver(Elements elementUtils, Types typeUtils) {
        this.elementUtils = Objects.requireNonNull(elementUtils);
        this.typeUtils = Objects.requireNonNull(typeUtils);
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
     * Determines if the given field represents a plural attribute (collection-valued).
     */
    public boolean isPluralAttribute(VariableElement field) {
        TypeMirror type = field.asType();
        
        // Check if it's a collection type
        if (isCollectionType(type)) {
            return true;
        }
        
        // Check if it's an array type
        if (type.getKind() == TypeKind.ARRAY) {
            return true;
        }
        
        return false;
    }

    /**
     * Determines if the given field represents an association (references another entity or embeddable).
     */
    public boolean isAssociation(VariableElement field) {
        TypeMirror type = field.asType();
        
        // Remove generics for checking
        TypeMirror rawType = typeUtils.erasure(type);
        Element element = typeUtils.asElement(rawType);
        
        if (element instanceof TypeElement) {
            TypeElement typeElement = (TypeElement) element;
            
            // Check for Entity annotation
            if (hasAnnotation(typeElement, "jakarta.persistence.Entity")) {
                return true;
            }
            
            // Check for Embeddable annotation
            if (hasAnnotation(typeElement, "jakarta.persistence.Embeddable")) {
                return true;
            }
            
            // Check for MappedSuperclass annotation
            if (hasAnnotation(typeElement, "jakarta.persistence.MappedSuperclass")) {
                return true;
            }
            
            // If it's a collection, check the element type
            if (isCollectionType(type)) {
                TypeMirror elementType = getElementType(type);
                if (elementType != null) {
                    Element elementTypeElement = typeUtils.asElement(elementType);
                    if (elementTypeElement instanceof TypeElement) {
                        TypeElement elementTypeElementType = (TypeElement) elementTypeElement;
                        if (hasAnnotation(elementTypeElementType, "jakarta.persistence.Entity") ||
                            hasAnnotation(elementTypeElementType, "jakarta.persistence.Embeddable") ||
                            hasAnnotation(elementTypeElementType, "jakarta.persistence.MappedSuperclass")) {
                            return true;
                        }
                    }
                }
            }
        }
        
        return false;
    }

    /**
     * Resolves the persistent attribute type for the given field.
     * For basic types, this is the type itself.
     * For associations, this is the referenced entity type.
     * For collections, this is the collection type.
     */
    public TypeMirror resolvePersistentAttributeType(VariableElement field, TypeElement declaringClass) {
        TypeMirror type = field.asType();
        
        // Handle collection types
        if (isCollectionType(type)) {
            return type;
        }
        
        // Handle array types
        if (type.getKind() == TypeKind.ARRAY) {
            return type;
        }
        
        // For other types, return the type itself
        return type;
    }

    /**
     * Extracts the collection type (e.g., List, Set) from a parameterized collection type.
     */
    public static String extractCollectionType(String typeName) {
        if (typeName.contains("<")) {
            return typeName.substring(0, typeName.indexOf('<'));
        }
        return typeName;
    }

    /**
     * Extracts the element type from a collection or array type.
     */
    public static String extractElementType(String typeName) {
        if (typeName.contains("<")) {
            String generics = typeName.substring(typeName.indexOf('<') + 1, typeName.lastIndexOf('>'));
            // For simplicity, take the first type parameter (element type for Collection, value type for Map)
            if (generics.contains(",")) {
                return generics.split(",")[1].trim(); // For Map, second parameter is value type
            }
            return generics.trim();
        } else if (typeName.endsWith("[]")) {
            return typeName.substring(0, typeName.length() - 2);
        }
        return "java.lang.Object";
    }

    /**
     * Checks if the given type is a collection type.
     */
    private boolean isCollectionType(TypeMirror type) {
        if (type.getKind() != TypeKind.DECLARED) {
            return false;
        }
        
        TypeElement typeElement = (TypeElement) typeUtils.asElement(type);
        if (typeElement == null) {
            return false;
        }
        
        String qualifiedName = typeElement.getQualifiedName().toString();
        
        // Check if it's a direct collection type
        if (COLLECTION_TYPES.contains(qualifiedName)) {
            return true;
        }
        
        // Check if it's a subclass of Collection
        for (TypeMirror superInterface : typeElement.getInterfaces()) {
            if (isCollectionType(superInterface)) {
                return true;
            }
        }
        
        TypeMirror superclass = typeElement.getSuperclass();
        if (superclass.getKind() != TypeKind.NONE) {
            return isCollectionType(superclass);
        }
        
        return false;
    }

    /**
     * Gets the element type of a collection or map type.
     */
    private TypeMirror getElementType(TypeMirror type) {
        if (type.getKind() != TypeKind.DECLARED) {
            return null;
        }
        
        DeclaredType declaredType = (DeclaredType) type;
        List<? extends TypeMirror> typeArguments = declaredType.getTypeArguments();
        
        if (typeArguments.isEmpty()) {
            return null;
        }
        
        // For Collection, first type argument is the element type
        // For Map, second type argument is the value type (which is what we want for PluralAttribute)
        TypeElement typeElement = (TypeElement) declaredType.asElement();
        String qualifiedName = typeElement.getQualifiedName().toString();
        
        if (qualifiedName.equals("java.util.Map") || 
            qualifiedName.equals("java.util.SortedMap") ||
            qualifiedName.equals("java.util.NavigableMap")) {
            // For Map types, we want the value type (second type argument)
            if (typeArguments.size() >= 2) {
                return typeArguments.get(1);
            }
        }
        
        // For Collection types, we want the first type argument
        return typeArguments.get(0);
    }

    /**
     * Checks if the given type is a basic type.
     */
    public boolean isBasicType(TypeMirror type) {
        if (type.getKind() == TypeKind.DECLARED) {
            TypeElement typeElement = (TypeElement) typeUtils.asElement(type);
            if (typeElement != null) {
                String qualifiedName = typeElement.getQualifiedName().toString();
                return BASIC_TYPES.contains(qualifiedName) || 
                       hasAnnotation(typeElement, "jakarta.persistence.Embeddable");
            }
        }
        
        // Primitive types are basic
        return type.getKind().isPrimitive();
    }
}

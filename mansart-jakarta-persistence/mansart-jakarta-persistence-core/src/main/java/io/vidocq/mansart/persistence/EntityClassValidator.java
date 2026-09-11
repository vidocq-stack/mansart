package io.vidocq.mansart.persistence;

import jakarta.persistence.Entity;
import java.lang.reflect.Modifier;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Set;
import java.util.HashSet;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.SortedMap;
import java.util.NavigableSet;
import java.util.NavigableMap;

public class EntityClassValidator {

    public static void validate(Class<?> clazz) {
        if (!clazz.isAnnotationPresent(Entity.class)) {
            return; // Only validate classes annotated with @Entity
        }

        // 1. Check if the class is final
        if (Modifier.isFinal(clazz.getModifiers())) {
            throw new IllegalArgumentException("class is final");
        }

        // 2. Check if any field is final
        for (Field field : clazz.getDeclaredFields()) {
            if (Modifier.isFinal(field.getModifiers())) {
                throw new IllegalArgumentException("field is final");
            }
        }

        // 3. Check if the class has a public or protected no-arg constructor
        boolean hasNoArgConstructor = false;
        for (Constructor<?> ctor : clazz.getDeclaredConstructors()) {
            if (ctor.getParameterCount() == 0 &&
                (Modifier.isPublic(ctor.getModifiers()) || Modifier.isProtected(ctor.getModifiers()))) {
                hasNoArgConstructor = true;
                break;
            }
        }
        if (!hasNoArgConstructor) {
            throw new IllegalArgumentException("no public or protected no-arg constructor");
        }
        
        // 4. Validate field visibility
        validateFieldVisibility(clazz);
        
        // 5. Validate accessor methods
        validateAccessorMethods(clazz);
        
        // 6. Validate collection field types
        validateCollectionFieldTypes(clazz);
    }
    
    private static void validateFieldVisibility(Class<?> clazz) {
        for (Field field : clazz.getDeclaredFields()) {
            if (Modifier.isPublic(field.getModifiers())) {
                throw new IllegalArgumentException("Persistent fields must not be public: " + field.getName());
            }
        }
    }
    
    private static void validateAccessorMethods(Class<?> clazz) {
        Set<String> accessorPatterns = Set.of("get", "is", "set");
        
        for (Method method : clazz.getDeclaredMethods()) {
            String methodName = method.getName();
            
            // Check if method matches getter/setter pattern
            if (methodName.startsWith("get") && methodName.length() > 3) {
                // It's a getter
            } else if (methodName.startsWith("is") && methodName.length() > 2) {
                // It's a boolean getter
            } else if (methodName.startsWith("set") && methodName.length() > 3) {
                // It's a setter
            } else {
                continue; // Not an accessor method
            }
            
            // Check if accessor is public or protected
            if (!Modifier.isPublic(method.getModifiers()) && !Modifier.isProtected(method.getModifiers())) {
                throw new IllegalArgumentException("Accessor methods must be public or protected: " + methodName);
            }
        }
    }
    
    private static void validateCollectionFieldTypes(Class<?> clazz) {
        Set<Class<?>> allowedCollectionInterfaces = Set.of(
            Collection.class,
            List.class,
            Set.class,
            Map.class,
            SortedSet.class,
            SortedMap.class,
            NavigableSet.class,
            NavigableMap.class
        );
        
        Set<Class<?>> concreteCollectionImplementations = Set.of(
            java.util.ArrayList.class,
            java.util.HashMap.class,
            java.util.HashSet.class,
            java.util.LinkedList.class,
            java.util.TreeMap.class,
            java.util.TreeSet.class,
            java.util.LinkedHashMap.class,
            java.util.LinkedHashSet.class
        );
        
        for (Field field : clazz.getDeclaredFields()) {
            Class<?> fieldType = field.getType();
            
            // Skip if not a collection type
            if (!Collection.class.isAssignableFrom(fieldType) && 
                !Map.class.isAssignableFrom(fieldType)) {
                continue;
            }
            
            // Check if it's a concrete implementation
            if (concreteCollectionImplementations.contains(fieldType)) {
                throw new IllegalArgumentException("Collection-valued fields must use interface types, not concrete implementations: " + field.getName());
            }
            
            // Check if it's an allowed interface (skip if it is)
            if (allowedCollectionInterfaces.contains(fieldType)) {
                continue;
            }
            
            // Any other collection type (custom implementation) is allowed
            // since the constraint is only about the predefined concrete implementations
        }
    }
}

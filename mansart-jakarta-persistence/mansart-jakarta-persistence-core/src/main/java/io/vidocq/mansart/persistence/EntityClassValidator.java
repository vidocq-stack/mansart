package io.vidocq.mansart.persistence;

import jakarta.persistence.Entity;
import java.lang.reflect.Modifier;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;

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
    }
}

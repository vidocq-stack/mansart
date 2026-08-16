/*
 * Copyright (c) 2026 Yann Blazart, Antoine Sabot-Durand and the Vidocq contributors
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0 OR EUPL-1.2 OR GPL-2.0-or-later
 */
package io.vidocq.mansart.persistence.core.runtime;

import jakarta.persistence.EntityListeners;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PostRemove;
import jakarta.persistence.PostUpdate;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreRemove;
import jakarta.persistence.PreUpdate;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Manages lifecycle callbacks for JPA entities and entity listeners (M9-3, M9-4).
 * 
 * <p>This class detects and invokes lifecycle callback methods annotated with
 * JPA annotations ({@link PrePersist}, {@link PostPersist}, {@link PreRemove},
 * {@link PostRemove}, {@link PreUpdate}, {@link PostUpdate}, {@link PostLoad}).
 * 
 * <p>Supports both:
 * <ul>
 *   <li>Entity callbacks: methods on the entity class itself (no parameters)</li>
 *   <li>Entity listeners: methods on listener classes registered via {@link jakarta.persistence.EntityListeners} (one parameter: the entity)</li>
 * </ul>
 * 
 * <p>Callbacks are invoked using MethodHandles for better performance and
 * module system compatibility.
 */
public class LifecycleCallbackManager {

    /**
     * Lifecycle callback types.
     */
    public enum CallbackType {
        PRE_PERSIST,
        POST_PERSIST,
        PRE_REMOVE,
        POST_REMOVE,
        PRE_UPDATE,
        POST_UPDATE,
        POST_LOAD
    }

    /**
     * Caches callback methods for each entity/listener class and callback type.
     * Key: entity/listener class name + "#" + isListenerClass flag, Value: map of callback type to list of MethodHandles
     */
    private final Map<String, Map<CallbackType, List<MethodHandle>>> callbackCache = new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * Mapping from annotation class to CallbackType.
     */
    private static final Map<Class<?>, CallbackType> ANNOTATION_TO_CALLBACK = new java.util.HashMap<>();
    static {
        ANNOTATION_TO_CALLBACK.put(PrePersist.class, CallbackType.PRE_PERSIST);
        ANNOTATION_TO_CALLBACK.put(PostPersist.class, CallbackType.POST_PERSIST);
        ANNOTATION_TO_CALLBACK.put(PreRemove.class, CallbackType.PRE_REMOVE);
        ANNOTATION_TO_CALLBACK.put(PostRemove.class, CallbackType.POST_REMOVE);
        ANNOTATION_TO_CALLBACK.put(PreUpdate.class, CallbackType.PRE_UPDATE);
        ANNOTATION_TO_CALLBACK.put(PostUpdate.class, CallbackType.POST_UPDATE);
        ANNOTATION_TO_CALLBACK.put(PostLoad.class, CallbackType.POST_LOAD);
    }

    /**
     * Creates a new LifecycleCallbackManager.
     */
    public LifecycleCallbackManager() {
    }

    /**
     * Invokes lifecycle callbacks of the specified type for an entity.
     * This includes both entity callbacks (methods on the entity class) and
     * entity listener callbacks (methods on listener classes registered via @EntityListeners).
     * 
     * @param entity the entity instance
     * @param callbackType the type of callback to invoke
     */
    public void invokeCallback(Object entity, CallbackType callbackType) {
        if (entity == null) {
            return;
        }

        Class<?> entityClass = entity.getClass();
        
        // Invoke entity callbacks (methods on the entity class itself)
        invokeEntityCallbacks(entity, entityClass, callbackType);
        
        // Invoke entity listener callbacks (methods on listener classes)
        invokeEntityListenerCallbacks(entity, entityClass, callbackType);
    }
    
    /**
     * Invokes callbacks defined directly on the entity class.
     */
    private void invokeEntityCallbacks(Object entity, Class<?> entityClass, CallbackType callbackType) {
        Map<CallbackType, List<MethodHandle>> callbacks = getCallbacks(entityClass, false);
        List<MethodHandle> methodHandles = callbacks.get(callbackType);

        if (methodHandles != null) {
            for (MethodHandle handle : methodHandles) {
                try {
                    handle.invoke(entity);
                } catch (Throwable t) {
                    throw new RuntimeException(
                        "Failed to invoke " + callbackType + " entity callback on " + entityClass.getName(), t);
                }
            }
        }
    }
    
    /**
     * Invokes callbacks defined on entity listener classes.
     */
    private void invokeEntityListenerCallbacks(Object entity, Class<?> entityClass, CallbackType callbackType) {
        // Get listener classes from @EntityListeners annotation
        EntityListeners entityListenersAnnotation = 
            entityClass.getAnnotation(EntityListeners.class);
        
        if (entityListenersAnnotation == null) {
            return;
        }
        
        Class<?>[] listenerClasses = entityListenersAnnotation.value();
        for (Class<?> listenerClass : listenerClasses) {
            invokeListenerCallbacks(entity, listenerClass, callbackType);
        }
    }
    
    /**
     * Invokes callbacks on a specific listener class.
     * Creates a new instance of the listener class for each invocation.
     */
    private void invokeListenerCallbacks(Object entity, Class<?> listenerClass, CallbackType callbackType) {
        Map<CallbackType, List<MethodHandle>> callbacks = getCallbacks(listenerClass, true);
        List<MethodHandle> methodHandles = callbacks.get(callbackType);

        if (methodHandles != null) {
            // Create a new instance of the listener
            Object listenerInstance;
            try {
                listenerInstance = listenerClass.getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                throw new RuntimeException(
                    "Failed to instantiate listener class " + listenerClass.getName(), e);
            }
            
            for (MethodHandle handle : methodHandles) {
                try {
                    // For listener methods: signature is (Entity entity)
                    // MethodHandle expects (listenerInstance, entity) when method is non-static
                    handle.invoke(listenerInstance, entity);
                } catch (Throwable t) {
                    throw new RuntimeException(
                        "Failed to invoke " + callbackType + " listener callback on " + listenerClass.getName(), t);
                }
            }
        }
    }

    /**
     * Gets all callback methods for an entity class, caching the result.
     * 
     * @param entityClass the entity class
     * @param isListenerClass true if this is a listener class
     * @return map of callback type to list of MethodHandles
     */
    private Map<CallbackType, List<MethodHandle>> getCallbacks(Class<?> entityClass, boolean isListenerClass) {
        // Use a compound key for caching: entityClass + isListenerClass flag
        String cacheKey = entityClass.getName() + "#" + isListenerClass;
        return callbackCache.computeIfAbsent(cacheKey, k -> discoverCallbacks(entityClass, isListenerClass));
    }

    /**
     * Discovers all lifecycle callback methods for an entity or listener class.
     * 
     * <p>For entity classes: methods must have void return type and NO parameters.
     * <p>For listener classes: methods must have void return type and ONE parameter (the entity).
     * 
     * @param entityClass the entity or listener class to scan
     * @param isListenerClass true if this is a listener class (expects 1 parameter), false for entity class (expects 0 parameters)
     * @return map of callback type to list of MethodHandles
     */
    private Map<CallbackType, List<MethodHandle>> discoverCallbacks(Class<?> entityClass, boolean isListenerClass) {
        Map<CallbackType, List<MethodHandle>> callbacks = new EnumMap<>(CallbackType.class);

        // Initialize empty lists for each callback type
        for (CallbackType type : CallbackType.values()) {
            callbacks.put(type, new ArrayList<>());
        }

        // Scan all methods in the class hierarchy
        Class<?> current = entityClass;
        while (current != null && current != Object.class) {
            for (Method method : current.getDeclaredMethods()) {
                // Check for each lifecycle annotation
                for (Map.Entry<Class<?>, CallbackType> entry : ANNOTATION_TO_CALLBACK.entrySet()) {
                    @SuppressWarnings("unchecked")
                    Class<? extends java.lang.annotation.Annotation> annotationClass = 
                        (Class<? extends java.lang.annotation.Annotation>) entry.getKey();
                    if (method.isAnnotationPresent(annotationClass)) {
                        CallbackType callbackType = entry.getValue();

                        // Validate method signature
                        // Entity callbacks: void return type, 0 parameters
                        // Listener callbacks: void return type, 1 parameter (the entity)
                        int expectedParamCount = isListenerClass ? 1 : 0;
                        if (method.getReturnType() == void.class && method.getParameterCount() == expectedParamCount) {
                            try {
                                // Make accessible for private methods
                                method.setAccessible(true);
                                
                                // Create MethodHandle using private access
                                MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(
                                    current, MethodHandles.lookup());
                                MethodHandle handle = lookup.unreflect(method);
                                
                                callbacks.get(callbackType).add(handle);
                            } catch (IllegalAccessException e) {
                                throw new RuntimeException(
                                    "Failed to create MethodHandle for @" + annotationClass.getSimpleName() +
                                    " method " + method.getName() + " in class " + current.getName(), e);
                            }
                        } else {
                            String expectedParams = isListenerClass ? "1 parameter" : "no parameters";
                            throw new IllegalArgumentException(
                                "Lifecycle callback method " + method.getName() + " in class " + 
                                current.getName() + " annotated with @" + annotationClass.getSimpleName() +
                                " must have void return type and " + expectedParams);
                        }
                    }
                }
            }
            current = current.getSuperclass();
        }

        return callbacks;
    }

    /**
     * Clears the callback cache for a specific entity class.
     * Useful when entity classes are redefined dynamically.
     * 
     * @param entityClass the entity class
     */
    public void clearCache(Class<?> entityClass) {
        callbackCache.remove(entityClass);
    }

    /**
     * Clears the entire callback cache.
     */
    public void clearCache() {
        callbackCache.clear();
    }
}

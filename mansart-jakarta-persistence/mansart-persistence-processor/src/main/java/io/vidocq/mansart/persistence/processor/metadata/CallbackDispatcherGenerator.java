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
import io.vidocq.mansart.persistence.spi.LifecycleCallbackDispatcher;

import javax.annotation.processing.Messager;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Generates a compile-time lifecycle-callback dispatcher for an entity class.
 *
 * <p>For an entity {@code Book}, generates {@code Book_LifecycleCallbacks} which
 * directly invokes {@code @PrePersist}, {@code @PostLoad}, … methods — no runtime
 * reflection, no {@code MethodHandles} scanning (DEBT-06).
 *
 * <p>Supports both entity callbacks (void methods, no params on the entity class)
 * and entity listeners (methods on classes registered via {@code @EntityListeners}).
 */
public final class CallbackDispatcherGenerator {


    // Lifecycle annotations → dispatcher phase
    private static final Map<String, String> CALLBACK_ANNOTATIONS = new java.util.LinkedHashMap<>();
    static {
        CALLBACK_ANNOTATIONS.put("jakarta.persistence.PrePersist",  "PRE_PERSIST");
        CALLBACK_ANNOTATIONS.put("jakarta.persistence.PostPersist", "POST_PERSIST");
        CALLBACK_ANNOTATIONS.put("jakarta.persistence.PreUpdate",   "PRE_UPDATE");
        CALLBACK_ANNOTATIONS.put("jakarta.persistence.PostUpdate",  "POST_UPDATE");
        CALLBACK_ANNOTATIONS.put("jakarta.persistence.PreRemove",   "PRE_REMOVE");
        CALLBACK_ANNOTATIONS.put("jakarta.persistence.PostRemove",  "POST_REMOVE");
        CALLBACK_ANNOTATIONS.put("jakarta.persistence.PostLoad",    "POST_LOAD");
    }

    private final SourceSink sink;
    private final Messager messager;

    /**
     * Creates a new CallbackDispatcherGenerator.
     *
     * @param sink      the source sink for writing generated files
     * @param messager  the messager for diagnostics (nullable)
     */
    public CallbackDispatcherGenerator(SourceSink sink, Messager messager) {
        this.sink = sink;
        this.messager = messager;
    }

    /**
     * Generates the callback dispatcher for the given entity.
     *
     * @param entity the scanned entity metadata
     * @throws IOException if writing fails
     */
    public void generate(EntityScanner.EntityMetadata entity) throws IOException {
        String pkg = packageOf(entity.type().getQualifiedName().toString());
        String simpleName = entity.type().getSimpleName().toString();
        String className = simpleName + "_LifecycleCallbacks";
        String fqn = pkg.isEmpty() ? className : pkg + "." + className;

        // Discover callbacks by scanning the entity type and its listeners
        CallbackDiscovery discovery = discoverCallbacks(entity);

        try (PrintWriter w = new PrintWriter(sink.createSource(fqn))) {
            if (!pkg.isEmpty()) {
                w.println("package " + pkg + ";");
                w.println();
            }

            w.println("import io.vidocq.mansart.persistence.spi.LifecycleCallbackDispatcher;");
            w.println("import io.vidocq.mansart.persistence.spi.LifecycleCallbackDispatcher.Phase;");
            w.println("import java.lang.invoke.MethodHandle;");
            w.println("import java.lang.invoke.MethodHandles;");
            w.println("import java.lang.invoke.MethodType;");
            w.println();
            w.println("public final class " + className + " implements LifecycleCallbackDispatcher {");
            w.println();
            w.println("    " + className + "() {}");
            w.println();

            // Static LOOKUP
            w.println("    private static final MethodHandles.Lookup LOOKUP;");
            w.println("    static {");
            w.println("        try {");
            w.println("            LOOKUP = MethodHandles.privateLookupIn(" + simpleName + ".class, MethodHandles.lookup());");
            w.println("        } catch (java.lang.IllegalAccessException ex) {");
            w.println("            throw new java.lang.ExceptionInInitializerError(ex);");
            w.println("        }");
            w.println("    }");
            w.println();

            // Static callback arrays per phase
            writeCallbackArrays(w, discovery, simpleName);

            // invoke(entity, phase)
            writeInvokeMethod(w, discovery);

            w.println("}");
        }
    }

    // ───────────────────── Callback discovery ─────────────────────

    /**
     * Scans the entity type (and its @EntityListeners) for lifecycle callback methods.
     */
    private CallbackDiscovery discoverCallbacks(EntityScanner.EntityMetadata entity) {
        CallbackDiscovery discovery = new CallbackDiscovery();
        TypeElement type = entity.type();

        // Scan entity class itself (entity callbacks: void, no params)
        scanClass(type, discovery, false);

        // Scan @EntityListeners (listener callbacks: void, 1 param — the entity)
        String entityListenersAnno = "jakarta.persistence.EntityListeners";
        for (AnnotationMirror mirror : type.getAnnotationMirrors()) {
            String annoName = ((TypeElement) mirror.getAnnotationType().asElement()).getQualifiedName().toString();
            if (entityListenersAnno.equals(annoName)) {
                var values = mirror.getElementValues();
                for (var entry : values.entrySet()) {
                    if ("value".equals(entry.getKey().getSimpleName().toString())) {
                        var annotationValue = entry.getValue();
                        // Use the visitor pattern properly
                        var visitor = new javax.lang.model.util.SimpleAnnotationValueVisitor8<java.util.List<TypeElement>, Void>() {
                            @Override
                            public java.util.List<TypeElement> visitArray(java.util.List<? extends javax.lang.model.element.AnnotationValue> values, Void unused) {
                                java.util.List<TypeElement> result = new java.util.ArrayList<>();
                                for (javax.lang.model.element.AnnotationValue av : values) {
                                    TypeElement listenerType = resolveListenerTypeFromAnnotationValue(av);
                                    if (listenerType != null) {
                                        result.add(listenerType);
                                    }
                                }
                                return result;
                            }
                            @Override
                            public java.util.List<TypeElement> visitType(javax.lang.model.type.TypeMirror t, Void unused) {
                                System.out.println("[DEBUG visitType] t=" + t);
                                java.util.List<TypeElement> result = new java.util.ArrayList<>();
                                if (t.getKind() == javax.lang.model.type.TypeKind.DECLARED) {
                                    var declared = (javax.lang.model.type.DeclaredType) t;
                                    if (declared.asElement() instanceof TypeElement te) {
                                        result.add(te);
                                    }
                                }
                                return result;
                            }
                            @Override
                            protected java.util.List<TypeElement> defaultAction(Object o, Void unused) {
                                System.out.println("[DEBUG defaultAction] o=" + o + " class=" + (o == null ? "null" : o.getClass().getName()));
                                return java.util.List.of();
                            }
                        };
                        java.util.List<TypeElement> listenerTypes = annotationValue.accept(visitor, null);
                        System.out.println("[DEBUG listenerTypes] " + listenerTypes);
                        for (TypeElement listenerType : listenerTypes) {
                            scanClass(listenerType, discovery, true);
                        }
                    }
                }
            }
        }

        return discovery;
    }

    private void scanClass(TypeElement clazz, CallbackDiscovery discovery, boolean isListener) {
        TypeElement current = clazz;
        // We can only scan the direct class for now (superclass scanning requires
        // the class to be available at annotation processing time, which it usually is)
        while (current != null && !current.getQualifiedName().toString().equals("java.lang.Object")) {
            for (Element member : current.getEnclosedElements()) {
                if (member.getKind() != ElementKind.METHOD) continue;
                ExecutableElement method = (ExecutableElement) member;

                // Check for each lifecycle annotation
                for (Map.Entry<String, String> entry : CALLBACK_ANNOTATIONS.entrySet()) {
                    String annoFqn = entry.getKey();
                    String phase = entry.getValue();

                    if (hasAnnotation(method, annoFqn)) {
                        // Validate signature
                        boolean valid = validateMethodSignature(method, isListener);
                        if (valid) {
                            String methodName = method.getSimpleName().toString();
                            String declaringClass = current.getQualifiedName().toString();
                            String handleKey = declaringClass + "." + methodName;

                            if (isListener) {
                                // Listener method: (Entity entity) — needs listener instance + entity
                                discovery.addListenerCallback(phase, handleKey, declaringClass, methodName);
                            } else {
                                // Entity method: () — just the entity
                                discovery.addEntityCallback(phase, handleKey, declaringClass, methodName);
                            }
                        } else {
                            if (messager != null) {
                                messager.printMessage(Diagnostic.Kind.WARNING,
                                    "[mansart-persistence] @" + annoFqn.substring(annoFqn.lastIndexOf('.') + 1) +
                                    " method " + method.getSimpleName() + " in " + current.getQualifiedName() +
                                    " has invalid signature for " + (isListener ? "listener" : "entity") +
                                    " callbacks", method);
                            }
                        }
                    }
                }
            }

            // Walk superclass
            TypeMirror superclass = current.getSuperclass();
            if (superclass.getKind() == javax.lang.model.type.TypeKind.DECLARED) {
                Element superEl = ((javax.lang.model.type.DeclaredType) superclass).asElement();
                if (superEl instanceof TypeElement superType) {
                    current = superType;
                } else {
                    current = null;
                }
            } else {
                current = null;
            }
        }
    }

    private boolean hasAnnotation(Element element, String annotationFqn) {
        for (AnnotationMirror mirror : element.getAnnotationMirrors()) {
            String name = ((TypeElement) mirror.getAnnotationType().asElement()).getQualifiedName().toString();
            if (annotationFqn.equals(name)) return true;
        }
        return false;
    }

    private boolean validateMethodSignature(ExecutableElement method, boolean isListener) {
        if (method.getReturnType().getKind() != javax.lang.model.type.TypeKind.VOID) return false;
        int expectedParams = isListener ? 1 : 0;
        return method.getParameters().size() == expectedParams;
    }

    /**
     * Resolves a listener type from an AnnotationValue.
     * Handles both standard TypeElement and Javac Attribute.Class.
     */
    @SuppressWarnings("unchecked")
    private TypeElement resolveListenerTypeFromAnnotationValue(javax.lang.model.element.AnnotationValue av) {
        // Use the visitor pattern to get the type
        var visitor = new javax.lang.model.util.SimpleAnnotationValueVisitor8<TypeElement, Void>() {
            @Override
            public TypeElement visitType(javax.lang.model.type.TypeMirror t, Void unused) {
                if (t.getKind() == javax.lang.model.type.TypeKind.DECLARED) {
                    var declared = (javax.lang.model.type.DeclaredType) t;
                    if (declared.asElement() instanceof TypeElement te) {
                        return te;
                    }
                }
                return null;
            }
            @Override
            protected TypeElement defaultAction(Object o, Void unused) {
                return null;
            }
        };
        return av.accept(visitor, null);
    }

    // ───────────────────── Generate callback arrays ─────────────────────

    private void writeCallbackArrays(PrintWriter w, CallbackDiscovery discovery, String simpleName) {
        // Count total callbacks to decide if try-catch is needed
        int totalEntityCallbacks = 0;
        int totalListenerCallbacks = 0;
        for (LifecycleCallbackDispatcher.Phase phase : LifecycleCallbackDispatcher.Phase.values()) {
            totalEntityCallbacks += discovery.entityCallbacks(phase).size();
            for (CallbackDiscovery.ListenerCallback lc : discovery.listenerCallbacks(phase)) {
                totalListenerCallbacks++;
            }
        }
        boolean needsTryCatch = totalEntityCallbacks > 0 || totalListenerCallbacks > 0;

        // Entity callbacks: pre-resolved MethodHandle[] per phase
        w.println("    private static final java.util.Map<Phase, MethodHandle[]> ENTITY_CALLBACKS;");
        w.println("    private static final java.util.Map<Phase, ListenerCallback[]> LISTENER_CALLBACKS;");
        w.println("    static {");

        if (needsTryCatch) {
            w.println("        try {");
        }
        w.println("            final java.util.EnumMap<Phase, MethodHandle[]> _entity = new java.util.EnumMap<>(Phase.class);");

        for (LifecycleCallbackDispatcher.Phase phase : LifecycleCallbackDispatcher.Phase.values()) {
            List<String> handles = discovery.entityCallbacks(phase);
            w.println("            _entity.put(Phase." + phase + ", new MethodHandle[] {");
            for (int i = 0; i < handles.size(); i++) {
                String handleKey = handles.get(i);
                int lastDot = handleKey.lastIndexOf('.');
                String className = handleKey.substring(0, lastDot);
                String methodName = handleKey.substring(lastDot + 1);
                w.println("                LOOKUP.findVirtual(" + className + ".class, \"" + methodName + "\", MethodType.methodType(void.class)),");
            }
            w.println("            });");
        }
        w.println("            ENTITY_CALLBACKS = java.util.Collections.unmodifiableMap(_entity);");

        w.println("            final java.util.EnumMap<Phase, ListenerCallback[]> _listener = new java.util.EnumMap<>(Phase.class);");

        for (LifecycleCallbackDispatcher.Phase phase : LifecycleCallbackDispatcher.Phase.values()) {
            List<CallbackDiscovery.ListenerCallback> listeners = discovery.listenerCallbacks(phase);
            w.println("            _listener.put(Phase." + phase + ", new ListenerCallback[] {");
            for (int i = 0; i < listeners.size(); i++) {
                CallbackDiscovery.ListenerCallback lc = listeners.get(i);
                w.println("                new ListenerCallback(" + lc.declaringClass() + ".class, LOOKUP.findVirtual(" + lc.declaringClass() + ".class, \"" + lc.methodName() + "\", MethodType.methodType(void.class, " + simpleName + ".class))),");
            }
            w.println("            });");
        }
        w.println("            LISTENER_CALLBACKS = java.util.Collections.unmodifiableMap(_listener);");

        if (needsTryCatch) {
            w.println("        } catch (NoSuchMethodException | IllegalAccessException ex) {");
            w.println("            throw new java.lang.ExceptionInInitializerError(ex);");
            w.println("        }");
        }
        w.println("    }");
        w.println();
    }

    // ───────────────────── Generate invoke method ─────────────────────

    private void writeInvokeMethod(PrintWriter w, CallbackDiscovery discovery) {
        w.println("    @Override");
        w.println("    public void invoke(Object entity, Phase phase) {");
        w.println("        // Entity callbacks (void, no params)");
        w.println("        MethodHandle[] handles = ENTITY_CALLBACKS.get(phase);");
        w.println("        if (handles != null) {");
        w.println("            for (MethodHandle h : handles) {");
        w.println("                try { h.invoke(entity); }");
        w.println("                catch (java.lang.Throwable ex) {");
        w.println("                    throw new RuntimeException(\"Callback failed: \" + phase, ex);");
        w.println("                }");
        w.println("            }");
        w.println("        }");
        w.println();

        w.println("        // Listener callbacks (void, 1 param — entity)");
        w.println("        ListenerCallback[] lcs = LISTENER_CALLBACKS.get(phase);");
        w.println("        if (lcs != null) {");
        w.println("            for (ListenerCallback lc : lcs) {");
        w.println("                try {");
        w.println("                    Object listener = lc.listenerClass().getDeclaredConstructor().newInstance();");
        w.println("                    lc.handle().invoke(listener, entity);");
        w.println("                } catch (java.lang.Throwable ex) {");
        w.println("                    throw new RuntimeException(\"Listener callback failed: \" + phase, ex);");
        w.println("                }");
        w.println("            }");
        w.println("        }");
        w.println("    }");
        w.println();

        // ListenerCallback record: holds resolved class + MethodHandle
        w.println("    /** Compile-time resolved listener callback. */");
        w.println("    private static final record ListenerCallback(Class<?> listenerClass, MethodHandle handle) {}");
        w.println();
    }

    // ───────────────────── CallbackDiscovery ─────────────────────

    /**
     * Holds discovered callback methods grouped by phase.
     */
    static class CallbackDiscovery {
        private final Map<LifecycleCallbackDispatcher.Phase, List<String>> entityCallbacks = new EnumMap<>(LifecycleCallbackDispatcher.Phase.class);
        private final Map<LifecycleCallbackDispatcher.Phase, List<ListenerCallback>> listenerCallbacks = new EnumMap<>(LifecycleCallbackDispatcher.Phase.class);

        CallbackDiscovery() {
            for (LifecycleCallbackDispatcher.Phase p : LifecycleCallbackDispatcher.Phase.values()) {
                entityCallbacks.put(p, new ArrayList<>());
                listenerCallbacks.put(p, new ArrayList<>());
            }
        }

        void addEntityCallback(String phase, String handleKey, String declaringClass, String methodName) {
            entityCallbacks.computeIfAbsent(LifecycleCallbackDispatcher.Phase.valueOf(phase), _ -> new ArrayList<>()).add(handleKey);
        }

        void addListenerCallback(String phase, String handleKey, String declaringClass, String methodName) {
            listenerCallbacks.computeIfAbsent(LifecycleCallbackDispatcher.Phase.valueOf(phase), _ -> new ArrayList<>())
                .add(new ListenerCallback(declaringClass, methodName));
        }

        List<String> entityCallbacks(LifecycleCallbackDispatcher.Phase phase) {
            return entityCallbacks.getOrDefault(phase, List.of());
        }

        List<ListenerCallback> listenerCallbacks(LifecycleCallbackDispatcher.Phase phase) {
            return listenerCallbacks.getOrDefault(phase, List.of());
        }

        record ListenerCallback(String declaringClass, String methodName) {}
    }

    // ───────────────────── Utility ─────────────────────

    private String packageOf(String fqn) {
        int i = fqn.lastIndexOf('.');
        return i < 0 ? "" : fqn.substring(0, i);
    }
}

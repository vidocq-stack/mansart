/*
 * SPDX-License-Identifier: EPL-2.0 OR EUPL-1.2 OR GPL-2.0-or-later
 */
package io.vidocq.mansart.persistence.core.runtime;

import io.vidocq.mansart.persistence.spi.EntityAccessor;

import java.io.IOException;
import java.io.InputStream;
import java.lang.classfile.ClassFile;
import java.lang.classfile.ClassModel;
import java.lang.classfile.FieldModel;
import java.lang.classfile.attribute.RuntimeVisibleAnnotationsAttribute;
import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDescs;
import java.lang.constant.MethodTypeDesc;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Generates hidden classes at runtime via {@link MethodHandles.Lookup#defineHiddenClass}
 * using the Java 26 Class-File API. The generated class implements {@link EntityAccessor}
 * and calls the entity's public getters and setters via {@code invokevirtual}.
 * <p>
 * No {@code MethodHandles.lookup()} is performed on user types — the lookup is on this
 * generator class. The hidden class calls only public methods on the entity.
 */
public final class RuntimeEntityClassGenerator {

    private static final String JPA_TRANSIENT = "Ljakarta/persistence/Transient;";

    private static final ClassDesc CD_STRING = ClassDesc.of("java.lang.String");
    private static final ClassDesc CD_OBJECT = ClassDesc.of("java.lang.Object");
    private static final ClassDesc CD_ILLEGAL_ARG = ClassDesc.of("java.lang.IllegalArgumentException");
    private static final ClassDesc CD_INTEGER = ClassDesc.of("java.lang.Integer");
    private static final ClassDesc CD_LONG_OBJ = ClassDesc.of("java.lang.Long");
    private static final String INIT = "<init>";
    private static final String VALUE_OF = "valueOf";
    private static final ClassDesc CD_BOOLEAN = ClassDesc.of("java.lang.Boolean");
    private static final ClassDesc CD_DOUBLE = ClassDesc.of("java.lang.Double");
    private static final ClassDesc CD_FLOAT = ClassDesc.of("java.lang.Float");

    /**
     * Generates a hidden {@link EntityAccessor} for the given entity class.
     *
     * @param entityClass the entity class
     * @param <T>         the entity type
     * @return an accessor that reads and writes entity fields via public getters/setters
     */
    @SuppressWarnings("unchecked")
    public <T> EntityAccessor<T> generateAccessor(Class<T> entityClass) {
        List<FieldInfo> fields = extractFields(entityClass);
        byte[] bytecode = generateBytecode(entityClass, fields);

        try {
            MethodHandles.Lookup lookup = MethodHandles.lookup();
            Class<?> hiddenClass = lookup.defineHiddenClass(bytecode, true).lookupClass();
            return (EntityAccessor<T>) hiddenClass.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Failed to generate hidden accessor for " + entityClass.getName(), e);
        }
    }

    // --- Field extraction ---

    private <T> List<FieldInfo> extractFields(Class<T> entityClass) {
        String resourceName = "/" + entityClass.getName().replace('.', '/') + ".class";
        byte[] classBytes;
        try (InputStream is = entityClass.getResourceAsStream(resourceName)) {
            if (is == null) {
                throw new IllegalArgumentException("Cannot read class file for " + entityClass.getName());
            }
            classBytes = is.readAllBytes();
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to read class file for " + entityClass.getName(), e);
        }

        ClassModel classModel = ClassFile.of().parse(classBytes);
        List<FieldInfo> fields = new ArrayList<>();
        for (FieldModel fm : classModel.fields()) {
            if (hasAnnotation(fm, JPA_TRANSIENT)) continue;
            String fieldName = fm.fieldName().toString();
            String internalType = fm.fieldType().toString();
            Class<?> javaType = toJavaType(internalType, entityClass.getClassLoader());
            fields.add(new FieldInfo(fieldName, javaType));
        }
        return fields;
    }

    private static boolean hasAnnotation(FieldModel model, String internalName) {
        for (var attr : model.attributes()) {
            if (attr instanceof RuntimeVisibleAnnotationsAttribute rva) {
                for (var ann : rva.annotations()) {
                    if (ann.className().toString().equals(internalName)) return true;
                }
            }
        }
        return false;
    }

    // --- Bytecode generation ---

    private <T> byte[] generateBytecode(Class<T> entityClass, List<FieldInfo> fields) {
        ClassDesc entityDesc = entityClass.describeConstable()
                .orElseThrow(() -> new IllegalStateException("Cannot get ClassDesc for " + entityClass));
        ClassDesc accessorDesc = EntityAccessor.class.describeConstable()
                .orElseThrow(() -> new IllegalStateException("Cannot get ClassDesc for EntityAccessor"));
        String generatedName = "HiddenAccessor_" + entityClass.getSimpleName();

        MethodTypeDesc getDesc = MethodTypeDesc.of(CD_OBJECT, CD_OBJECT, CD_STRING);
        MethodTypeDesc setDesc = MethodTypeDesc.of(ConstantDescs.CD_void, CD_OBJECT, CD_STRING, CD_OBJECT);

        return ClassFile.of().build(
                ClassDesc.of("io.vidocq.mansart.persistence.core.runtime", generatedName),
                classBuilder -> classBuilder
                        .withSuperclass(ConstantDescs.CD_Object)
                        .withInterfaceSymbols(accessorDesc)
                        .withMethod(INIT, MethodTypeDesc.of(ConstantDescs.CD_void),
                                ClassFile.ACC_PUBLIC, mb -> mb.withCode(cb -> {
                                    cb.aload(0);
                                    cb.invokespecial(ConstantDescs.CD_Object, INIT,
                                            MethodTypeDesc.of(ConstantDescs.CD_void));
                                    cb.return_();
                                }))
                        .withMethod("get", getDesc, ClassFile.ACC_PUBLIC, mb ->
                                mb.withCode(cb -> generateGetBody(cb, entityDesc, fields)))
                        .withMethod("set", setDesc, ClassFile.ACC_PUBLIC, mb ->
                                mb.withCode(cb -> generateSetBody(cb, entityDesc, fields)))
        );
    }

    private void generateGetBody(java.lang.classfile.CodeBuilder cb, ClassDesc entityDesc,
                                 List<FieldInfo> fields) {
        for (FieldInfo field : fields) {
            cb.aload(2);
            cb.ldc(field.name());
            cb.invokevirtual(CD_STRING, "equals",
                    MethodTypeDesc.of(ConstantDescs.CD_boolean, CD_OBJECT));
            var nextLabel = cb.newLabel();
            cb.ifeq(nextLabel);

            cb.aload(1);
            cb.checkcast(entityDesc);
            String fieldName = field.name();
            String capitalized = capitalize(fieldName);
            String getterName = "get" + capitalized;
            // For boolean/Boolean fields, prefer "is" prefix per JavaBean convention
            if (field.type() == boolean.class || field.type() == Boolean.class) {
                getterName = "is" + capitalized;
            }
            MethodTypeDesc getterDesc = MethodTypeDesc.of(toClassDesc(field.type()));
            cb.invokevirtual(entityDesc, getterName, getterDesc);

            boxIfPrimitive(cb, field.type());
            cb.areturn();

            cb.labelBinding(nextLabel);
        }

        cb.new_(CD_ILLEGAL_ARG);
        cb.dup();
        cb.ldc("Unknown field");
        cb.invokespecial(CD_ILLEGAL_ARG, "<init>",
                MethodTypeDesc.of(ConstantDescs.CD_void, CD_STRING));
        cb.athrow();
    }

    private void generateSetBody(java.lang.classfile.CodeBuilder cb, ClassDesc entityDesc,
                                List<FieldInfo> fields) {
        for (FieldInfo field : fields) {
            cb.aload(2);
            cb.ldc(field.name());
            cb.invokevirtual(CD_STRING, "equals",
                    MethodTypeDesc.of(ConstantDescs.CD_boolean, CD_OBJECT));
            var nextLabel = cb.newLabel();
            cb.ifeq(nextLabel);

            cb.aload(1);
            cb.checkcast(entityDesc);
            cb.aload(3);
            unboxAndCast(cb, field.type());
            String setterName = "set" + capitalize(field.name());
            MethodTypeDesc setterDesc = MethodTypeDesc.of(ConstantDescs.CD_void, toClassDesc(field.type()));
            cb.invokevirtual(entityDesc, setterName, setterDesc);
            cb.return_();

            cb.labelBinding(nextLabel);
        }

        cb.new_(CD_ILLEGAL_ARG);
        cb.dup();
        cb.ldc("Unknown field");
        cb.invokespecial(CD_ILLEGAL_ARG, "<init>",
                MethodTypeDesc.of(ConstantDescs.CD_void, CD_STRING));
        cb.athrow();
    }

    private void boxIfPrimitive(java.lang.classfile.CodeBuilder cb, Class<?> type) {
        if (type == int.class) {
            cb.invokestatic(CD_INTEGER, VALUE_OF,
                    MethodTypeDesc.of(CD_INTEGER, ConstantDescs.CD_int));
        } else if (type == long.class) {
            cb.invokestatic(CD_LONG_OBJ, VALUE_OF,
                    MethodTypeDesc.of(CD_LONG_OBJ, ConstantDescs.CD_long));
        } else if (type == boolean.class) {
            cb.invokestatic(CD_BOOLEAN, VALUE_OF,
                    MethodTypeDesc.of(CD_BOOLEAN, ConstantDescs.CD_boolean));
        } else if (type == double.class) {
            cb.invokestatic(CD_DOUBLE, VALUE_OF,
                    MethodTypeDesc.of(CD_DOUBLE, ConstantDescs.CD_double));
        } else if (type == float.class) {
            cb.invokestatic(CD_FLOAT, VALUE_OF,
                    MethodTypeDesc.of(CD_FLOAT, ConstantDescs.CD_float));
        }
    }

    private void unboxAndCast(java.lang.classfile.CodeBuilder cb, Class<?> type) {
        if (type == int.class) {
            cb.checkcast(CD_INTEGER);
            cb.invokevirtual(CD_INTEGER, "intValue",
                    MethodTypeDesc.of(ConstantDescs.CD_int));
        } else if (type == long.class) {
            cb.checkcast(CD_LONG_OBJ);
            cb.invokevirtual(CD_LONG_OBJ, "longValue",
                    MethodTypeDesc.of(ConstantDescs.CD_long));
        } else if (type == boolean.class) {
            cb.checkcast(CD_BOOLEAN);
            cb.invokevirtual(CD_BOOLEAN, "booleanValue",
                    MethodTypeDesc.of(ConstantDescs.CD_boolean));
        } else if (type == double.class) {
            cb.checkcast(CD_DOUBLE);
            cb.invokevirtual(CD_DOUBLE, "doubleValue",
                    MethodTypeDesc.of(ConstantDescs.CD_double));
        } else if (type == float.class) {
            cb.checkcast(CD_FLOAT);
            cb.invokevirtual(CD_FLOAT, "floatValue",
                    MethodTypeDesc.of(ConstantDescs.CD_float));
        } else {
            cb.checkcast(toClassDesc(type));
        }
    }

    private static ClassDesc toClassDesc(Class<?> type) {
        if (type.isPrimitive()) {
            if (type == int.class) return ConstantDescs.CD_int;
            if (type == long.class) return ConstantDescs.CD_long;
            if (type == boolean.class) return ConstantDescs.CD_boolean;
            if (type == double.class) return ConstantDescs.CD_double;
            if (type == float.class) return ConstantDescs.CD_float;
            if (type == void.class) return ConstantDescs.CD_void;
            if (type == byte.class) return ConstantDescs.CD_byte;
            if (type == short.class) return ConstantDescs.CD_short;
            if (type == char.class) return ConstantDescs.CD_char;
        }
        return type.describeConstable()
                .orElseThrow(() -> new IllegalStateException("Cannot get ClassDesc for " + type));
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private static Class<?> toJavaType(String internalType, ClassLoader classLoader) {
        return switch (internalType) {
            case "I" -> int.class;
            case "J" -> long.class;
            case "Z" -> boolean.class;
            case "D" -> double.class;
            case "F" -> float.class;
            case "B" -> byte.class;
            case "S" -> short.class;
            case "C" -> char.class;
            case "V" -> void.class;
            default -> {
                if (internalType.startsWith("L") && internalType.endsWith(";")) {
                    String className = internalType.substring(1, internalType.length() - 1).replace('/', '.');
                    try {
                        yield Class.forName(className, false, classLoader);
                    } catch (ClassNotFoundException e) {
                        throw new IllegalArgumentException("Cannot load type: " + className, e);
                    }
                }
                throw new IllegalArgumentException("Unknown type: " + internalType);
            }
        };
    }

    private record FieldInfo(String name, Class<?> type) {}
}

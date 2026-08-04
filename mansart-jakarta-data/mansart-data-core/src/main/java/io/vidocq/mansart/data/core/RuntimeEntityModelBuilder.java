/*
 * Copyright (c) ${year} Yann Blazart, Antoine Sabot-Durand and the Vidocq contributors
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
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-1.2
 *
 * SPDX-License-Identifier: EPL-2.0 OR EUPL-1.2 OR GPL-2.0-or-later
 */

package io.vidocq.mansart.data.core;

import io.vidocq.mansart.data.dialect.Attribute;
import io.vidocq.mansart.data.dialect.EntityModel;
import io.vidocq.mansart.data.dialect.SqlNames;
import io.vidocq.mansart.data.dialect.attribute.BooleanAttribute;
import io.vidocq.mansart.data.dialect.attribute.EnumAttribute;
import io.vidocq.mansart.data.dialect.attribute.IdAttribute;
import io.vidocq.mansart.data.dialect.attribute.NumericAttribute;
import io.vidocq.mansart.data.dialect.attribute.ReferenceAttribute;
import io.vidocq.mansart.data.dialect.attribute.TemporalAttribute;
import io.vidocq.mansart.data.dialect.attribute.TextAttribute;
import io.vidocq.mansart.data.dialect.attribute.VersionAttribute;

import java.lang.annotation.Annotation;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Runtime equivalent of {@code mansart-data-processor.EntityScanner} +
 * {@code MansartMetamodelWriter}: builds an {@link EntityModel} for an entity class via
 * reflection, reading the standard JPA mapping annotations ({@code @jakarta.persistence.*}).
 * Used by the M7 runtime path when a {@code @Repository} interface is discovered for which
 * no compile-time {@code _Entity} metamodel exists (typically: TCK entities, ad-hoc usages
 * without APT).
 *
 * <p>M7-29: dropped the parallel {@code io.vidocq.mansart.data.*} mirror annotations —
 * {@code jakarta.persistence-api} is a spec API jar, not an implementation, so the
 * « zero external impl » philosophy holds with the standard names.
 */
public final class RuntimeEntityModelBuilder {

    private RuntimeEntityModelBuilder() {}

    public static <E> EntityModel<E> build(Class<E> entityClass) {
        return build(entityClass, null);
    }

    /**
     * M7-5 — Jakarta Data 1.0 supports entities with no explicit {@code @Id} annotation: the
     * field named {@code id} (or matching the K type from {@code BasicRepository<E, K>}) is the
     * primary key by convention. If {@code idTypeHint} is non-null and no annotated id is found,
     * a field named {@code id} (or the single field of that type) is used as the implicit id.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <E> EntityModel<E> build(Class<E> entityClass, Class<?> idTypeHint) {
        try {
            MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(
                    entityClass, MethodHandles.lookup());
            MethodHandle ctor = lookup.findConstructor(entityClass, MethodType.methodType(void.class));

            String tableName = readTableName(entityClass);
            String schema    = readSchema(entityClass);

            List<Field> persistedFields = new ArrayList<>();
            // Get all fields including inherited ones
            for (Class<?> clazz = entityClass; clazz != null && clazz != Object.class; clazz = clazz.getSuperclass()) {
                for (Field field : clazz.getDeclaredFields()) {
                    int mods = field.getModifiers();
                    if (Modifier.isStatic(mods) || Modifier.isTransient(mods)) continue;
                    if (hasAnnotation(field, "jakarta.persistence.Transient")) continue;
                    persistedFields.add(field);
                }
            }

            // M7-5 — pick the implicit id field BEFORE iterating attributes so describeField
            // sees a stable choice. Order: explicit annotation > field named "id" > single
            // field whose type matches the K hint > field named "<entitySimple>Id".
            Field implicitIdField = pickImplicitIdField(entityClass, persistedFields, idTypeHint);

            List<Attribute<E, ?>> attrs = new ArrayList<>();
            IdAttribute<E, ?> id = null;
            VersionAttribute<E, ?> version = null;

            for (Field field : persistedFields) {
                boolean implicitId = field == implicitIdField;
                Attribute<E, ?> a = describeField(entityClass, field, lookup, implicitId);
                attrs.add(a);
                if (a instanceof IdAttribute<?, ?>      ida) id      = (IdAttribute<E, ?>) ida;
                if (a instanceof VersionAttribute<?, ?> v)   version = (VersionAttribute<E, ?>) v;
            }

            if (id == null) {
                throw new MansartDataException("Entity " + entityClass.getName()
                        + " has no @Id field (and no implicit id field matched idTypeHint="
                        + (idTypeHint == null ? "<none>" : idTypeHint.getName()) + ")");
            }

            return new EntityModel<>(entityClass, tableName, schema, id,
                    Optional.ofNullable(version), attrs, ctor);
        } catch (NoSuchMethodException e) {
            throw new MansartDataException(entityClass.getName()
                    + " must declare a no-arg constructor (Mansart/JPA convention).", e);
        } catch (IllegalAccessException e) {
            throw new MansartDataException("Cannot privateLookupIn " + entityClass.getName()
                    + " — open the entity package to mansart-data-core via `opens` in your module-info.", e);
        }
    }

    private static boolean isExplicitId(Field field) {
        return hasAnnotation(field, "jakarta.persistence.Id") 
                || hasAnnotation(field, "jakarta.persistence.EmbeddedId");
    }

    /**
     * Jakarta Data 1.0 implicit-id resolution. Returns the field that should be treated as the
     * id when no field carries an explicit {@code @Id}. Otherwise null (an explicit one will be
     * picked up by {@code describeField}).
     */
    private static Field pickImplicitIdField(Class<?> entityClass, List<Field> persisted, Class<?> idTypeHint) {
        for (Field f : persisted) if (isExplicitId(f)) return null;

        for (Field f : persisted) {
            if ("id".equals(f.getName()) && (idTypeHint == null || matchesType(f, idTypeHint))) return f;
        }

        if (idTypeHint != null) {
            Field unique = null;
            int matchCount = 0;
            for (Field f : persisted) {
                if (matchesType(f, idTypeHint)) { unique = f; matchCount++; }
            }
            if (matchCount == 1) return unique;
        }

        String entityName = entityClass.getSimpleName();
        if (!entityName.isEmpty()) {
            String suffix = Character.toLowerCase(entityName.charAt(0)) + entityName.substring(1) + "Id";
            for (Field f : persisted) {
                if (suffix.equals(f.getName())
                        && (idTypeHint == null || matchesType(f, idTypeHint))) return f;
            }
            // Also accept "<entityName>Identifier" / "<entityName>Key" — the TCK uses
            // boxIdentifier on Box.
            for (String alt : new String[] {
                    Character.toLowerCase(entityName.charAt(0)) + entityName.substring(1) + "Identifier",
                    Character.toLowerCase(entityName.charAt(0)) + entityName.substring(1) + "Key" }) {
                for (Field f : persisted) {
                    if (alt.equals(f.getName())
                            && (idTypeHint == null || matchesType(f, idTypeHint))) return f;
                }
            }
        }
        return null;
    }

    private static boolean matchesType(Field f, Class<?> hint) {
        Class<?> ft = boxed(f.getType());
        Class<?> hb = boxed(hint);
        return hb.isAssignableFrom(ft) || ft.isAssignableFrom(hb);
    }

    /* ---- field → Attribute mapping ---- */

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <E> Attribute<E, ?> describeField(Class<E> entityClass, Field field,
                                                     MethodHandles.Lookup lookup,
                                                     boolean implicitId) {
        String name = field.getName();
        Class<?> type = boxed(field.getType());
        Class<?> declaringClass = field.getDeclaringClass();

        // Check for annotations on getter as well as field (JPA allows both)
        MethodHandle getter, setter;
        Method getterMethod = null;
        try {
            // Find getter method by scanning all methods
            // JPA allows annotations on getters, and getter names may not follow exact field name conventions
            Method foundGetter = null;
            Method foundSetter = null;
            
            for (Method m : declaringClass.getDeclaredMethods()) {
                String methodName = m.getName();
                Class<?> returnType = m.getReturnType();
                Class<?>[] paramTypes = m.getParameterTypes();
                
                // Check for getter: method name starts with "get" or "is", no params, return type matches
                if (paramTypes.length == 0 && returnType.equals(field.getType())) {
                    if (methodName.startsWith("get") && methodName.length() > 3) {
                        String fieldNameFromGetter = methodName.substring(3);
                        // Simple check: field name matches (case-insensitive first char) or exact match
                        if (methodName.substring(3).equalsIgnoreCase(name) || 
                            methodName.substring(3).equals(name) ||
                            name.equalsIgnoreCase(methodName.substring(3))) {
                            foundGetter = m;
                        }
                    } else if (methodName.startsWith("is") && methodName.length() > 2 &&
                               (field.getType() == boolean.class || field.getType() == Boolean.class)) {
                        String fieldNameFromGetter = methodName.substring(2);
                        if (methodName.substring(2).equalsIgnoreCase(name) || 
                            methodName.substring(2).equals(name) ||
                            name.equalsIgnoreCase(methodName.substring(2))) {
                            foundGetter = m;
                        }
                    }
                }
                
                // Check for setter: method name starts with "set", one param, return type void
                if (methodName.startsWith("set") && methodName.length() > 3 && 
                    paramTypes.length == 1 && m.getReturnType() == void.class &&
                    paramTypes[0].equals(field.getType())) {
                    String fieldNameFromSetter = methodName.substring(3);
                    if (methodName.substring(3).equalsIgnoreCase(name) || 
                        methodName.substring(3).equals(name) ||
                        name.equalsIgnoreCase(methodName.substring(3))) {
                        foundSetter = m;
                    }
                }
            }
            
            if (foundGetter != null) {
                getterMethod = foundGetter;
                // Try to create MethodHandles
                MethodHandles.Lookup fieldLookup;
                try {
                    fieldLookup = MethodHandles.privateLookupIn(declaringClass, MethodHandles.lookup());
                } catch (IllegalAccessException e) {
                    fieldLookup = MethodHandles.lookup();
                }
                try {
                    getter = fieldLookup.unreflect(foundGetter);
                } catch (IllegalAccessException e) {
                    // Try with accessible
                    foundGetter.setAccessible(true);
                    getter = fieldLookup.unreflect(foundGetter);
                }
                if (foundSetter != null) {
                    try {
                        setter = fieldLookup.unreflect(foundSetter);
                    } catch (IllegalAccessException e) {
                        foundSetter.setAccessible(true);
                        setter = fieldLookup.unreflect(foundSetter);
                    }
                } else {
                    setter = fieldLookup.findSetter(declaringClass, name, field.getType());
                }
            } else {
                // Fallback to findGetter/findSetter
                MethodHandles.Lookup fieldLookup;
                try {
                    fieldLookup = MethodHandles.privateLookupIn(declaringClass, MethodHandles.lookup());
                } catch (IllegalAccessException e) {
                    fieldLookup = MethodHandles.lookup();
                }
                getter = fieldLookup.findGetter(declaringClass, name, field.getType());
                setter = fieldLookup.findSetter(declaringClass, name, field.getType());
            }
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new MansartDataException("Cannot create MethodHandle for " + entityClass.getName()
                    + "#" + name, e);
        }

        String columnName = readColumnName(field, false);
        boolean nullable  = readBooleanColumnAttr(field, "nullable", true);
        boolean unique    = readBooleanColumnAttr(field, "unique",   false);
        int     length    = (int) readIntColumnAttr(field, "length", 255);
        
        boolean isId = implicitId || hasAnnotation(field, "jakarta.persistence.Id")
                || hasAnnotation(field, "jakarta.persistence.EmbeddedId")
                || (getterMethod != null && (hasAnnotation(getterMethod, "jakarta.persistence.Id") 
                    || hasAnnotation(getterMethod, "jakarta.persistence.EmbeddedId")));
        boolean isVersion = hasAnnotation(field, "jakarta.persistence.Version") 
                || (getterMethod != null && hasAnnotation(getterMethod, "jakarta.persistence.Version"));
        boolean isManyToOne = hasAnnotation(field, "jakarta.persistence.ManyToOne") 
                || (getterMethod != null && hasAnnotation(getterMethod, "jakarta.persistence.ManyToOne"));
        boolean isOneToOne = hasAnnotation(field, "jakarta.persistence.OneToOne")
                || (getterMethod != null && hasAnnotation(getterMethod, "jakarta.persistence.OneToOne"));
        boolean isReference = isManyToOne || isOneToOne;
        boolean generated = hasAnnotation(field, "jakarta.persistence.GeneratedValue")
                || (getterMethod != null && hasAnnotation(getterMethod, "jakarta.persistence.GeneratedValue"));
        
        // If column name annotation is on getter, use it
        if (getterMethod != null) {
            String getterColumnName = readAnnoMember(getterMethod, "jakarta.persistence.Column", "name", String.class, "");
            if (!getterColumnName.isEmpty()) {
                columnName = getterColumnName;
            } else if (isReference) {
                getterColumnName = readAnnoMember(getterMethod, "jakarta.persistence.JoinColumn", "name", String.class, "");
                if (!getterColumnName.isEmpty()) columnName = getterColumnName;
            }
        }

        if (isId) {
            return new IdAttribute<>(name, columnName, type, entityClass, generated, getter, setter);
        }
        if (isVersion) {
            return new VersionAttribute(name, columnName, type, entityClass, getter, setter);
        }
        if (isReference) {
            // M8-3 — referencedColumnName from @JoinColumn(referencedColumnName = ...) defaults to "id".
            String referencedCol = readAnnoMember(field, "jakarta.persistence.JoinColumn",
                    "referencedColumnName", String.class, "");
            if (referencedCol.isEmpty()) referencedCol = "id";
            return new ReferenceAttribute<>(name, columnName, type, entityClass,
                    nullable, unique, false, referencedCol, getter, setter);
        }
        if (type.isEnum()) {
            return new EnumAttribute(name, columnName, type, entityClass,
                    nullable, unique,
                    io.vidocq.mansart.data.dialect.attribute.EnumStorage.ORDINAL,
                    getter, setter);
        }
        if (type == String.class) {
            return new TextAttribute<>(name, columnName, entityClass, nullable, unique, length, getter, setter);
        }
        if (isTemporal(type)) {
            return new TemporalAttribute<>(name, columnName, type, entityClass, nullable, unique, getter, setter);
        }
        if (type == Boolean.class) {
            return new BooleanAttribute<>(name, columnName, entityClass, nullable, unique, getter, setter);
        }
        if (isNumeric(type)) {
            return new NumericAttribute(name, columnName, type, entityClass,
                    nullable, unique, 0, 0, getter, setter);
        }
        // Fallback — let the dialect figure it out via Types.OTHER
        return new NumericAttribute(name, columnName, type, entityClass,
                nullable, unique, 0, 0, getter, setter);
    }

    /* ---- annotation reading via reflection-by-name (no compile dep on JPA) ---- */

    private static boolean hasAnnotation(java.lang.reflect.AnnotatedElement el, String fqn) {
        for (Annotation a : el.getDeclaredAnnotations()) {
            if (a.annotationType().getName().equals(fqn)) return true;
        }
        return false;
    }

    private static <T> T readAnnoMember(java.lang.reflect.AnnotatedElement el, String annoFqn,
                                        String member, Class<T> type, T defaultValue) {
        for (Annotation a : el.getDeclaredAnnotations()) {
            if (!a.annotationType().getName().equals(annoFqn)) continue;
            try {
                Method m = a.annotationType().getMethod(member);
                Object v = m.invoke(a);
                if (v == null) return defaultValue;
                if (type.isInstance(v)) return type.cast(v);
                return defaultValue;
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ignored) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    private static String readTableName(Class<?> entityClass) {
        String name = readAnnoMember(entityClass, "jakarta.persistence.Table",
                "name", String.class, "");
        if (name.isEmpty()) name = SqlNames.tableName(entityClass.getSimpleName());
        return name;
    }

    private static String readSchema(Class<?> entityClass) {
        return readAnnoMember(entityClass, "jakarta.persistence.Table",
                "schema", String.class, "");
    }

    private static String readColumnName(Field field, boolean isReference) {
        String n = readAnnoMember(field, "jakarta.persistence.Column", "name", String.class, "");
        if (n.isEmpty()) n = readAnnoMember(field, "jakarta.persistence.JoinColumn", "name", String.class, "");
        if (n.isEmpty()) {
            n = isReference
                    ? SqlNames.foreignKeyColumn(field.getName())
                    : SqlNames.columnName(field.getName());
        }
        return n;
    }

    private static boolean readBooleanColumnAttr(Field field, String member, boolean def) {
        Boolean b = readAnnoMember(field, "jakarta.persistence.Column", member, Boolean.class, null);
        return b == null ? def : b;
    }

    private static long readIntColumnAttr(Field field, String member, long def) {
        Integer i = readAnnoMember(field, "jakarta.persistence.Column", member, Integer.class, null);
        return i == null ? def : i.longValue();
    }

    private static Class<?> boxed(Class<?> c) {
        if (c == boolean.class) return Boolean.class;
        if (c == byte.class)    return Byte.class;
        if (c == short.class)   return Short.class;
        if (c == int.class)     return Integer.class;
        if (c == long.class)    return Long.class;
        if (c == float.class)   return Float.class;
        if (c == double.class)  return Double.class;
        if (c == char.class)    return Character.class;
        return c;
    }

    private static boolean isTemporal(Class<?> c) {
        return c == LocalDate.class || c == LocalDateTime.class || c == LocalTime.class
                || c == OffsetDateTime.class || c == java.time.Instant.class
                || c == java.util.Date.class || c == java.sql.Date.class;
    }

    private static boolean isNumeric(Class<?> c) {
        return c == Byte.class || c == Short.class || c == Integer.class || c == Long.class
                || c == Float.class || c == Double.class
                || c == BigDecimal.class || c == BigInteger.class;
    }
}

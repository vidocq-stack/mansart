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
package io.vidocq.mansart.persistence.core;

import io.vidocq.mansart.data.dialect.EntityModel;
import io.vidocq.mansart.data.dialect.attribute.IdAttribute;
import io.vidocq.mansart.data.dialect.attribute.TextAttribute;
import io.vidocq.mansart.data.dialect.attribute.VersionAttribute;

import javax.annotation.processing.Generated;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.List;
import java.util.Optional;

/**
 * Static metamodel for TestEntity.
 * M6 — Manually created metamodel for testing.
 */
@Generated("io.vidocq.mansart.data.processor.MansartProcessor")
public final class _TestEntity {

    private _TestEntity() {}

    private static final MethodHandles.Lookup LOOKUP;
    static {
        try {
            LOOKUP = MethodHandles.privateLookupIn(TestEntity.class, MethodHandles.lookup());
        } catch (IllegalAccessException ex) {
            throw new ExceptionInInitializerError(ex);
        }
    }

    private static MethodHandle getterMh(String fieldName, Class<?> type) {
        try {
            return LOOKUP.findGetter(TestEntity.class, fieldName, type);
        } catch (NoSuchFieldException | IllegalAccessException ex) {
            throw new ExceptionInInitializerError(ex);
        }
    }

    private static MethodHandle setterMh(String fieldName, Class<?> type) {
        try {
            return LOOKUP.findSetter(TestEntity.class, fieldName, type);
        } catch (NoSuchFieldException | IllegalAccessException ex) {
            throw new ExceptionInInitializerError(ex);
        }
    }

    private static MethodHandle constructorMh() {
        try {
            return LOOKUP.findConstructor(TestEntity.class, MethodType.methodType(void.class));
        } catch (NoSuchMethodException | IllegalAccessException ex) {
            throw new ExceptionInInitializerError("TestEntity must have a no-arg constructor");
        }
    }

    public static final IdAttribute<TestEntity, Long> id = 
        new IdAttribute<>("id", "id", Long.class, TestEntity.class, true, 
            getterMh("id", Long.class), setterMh("id", Long.class));

    public static final TextAttribute<TestEntity> name = 
        new TextAttribute<>("name", "name", TestEntity.class, false, false, 100, 
            getterMh("name", String.class), setterMh("name", String.class));

    public static final TextAttribute<TestEntity> description = 
        new TextAttribute<>("description", "description", TestEntity.class, false, false, 500, 
            getterMh("description", String.class), setterMh("description", String.class));

    public static final VersionAttribute<TestEntity, Integer> version = 
        new VersionAttribute<>("version", "version", Integer.class, TestEntity.class, 
            getterMh("version", Integer.class), setterMh("version", Integer.class));

    public static final EntityModel<TestEntity> $MODEL = new EntityModel<>(
        TestEntity.class,
        "test_entity",
        "",
        id,
        Optional.of(version),
        List.of(id, name, description, version),
        constructorMh()
    );
}

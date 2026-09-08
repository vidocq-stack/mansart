/*
 * Copyright (c) 2025 Vidocq contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.vidocq.mansart.persistence.core.jpql;

import java.util.Objects;

import jakarta.persistence.Parameter;

/**
 * Parameter implementation for named and positional query parameters.
 */
public final class MansartParameter<T> implements Parameter<T> {

    private final String name;
    private final Integer position;
    private final Class<T> type;

    public MansartParameter(String name, Integer position, Class<T> type) {
        this.name = name;
        this.position = position;
        this.type = Objects.requireNonNull(type, "type must not be null");
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Integer getPosition() {
        return position;
    }

    @Override
    public Class<T> getParameterType() {
        return type;
    }

    @Override
    public String toString() {
        if (name != null) {
            return "Parameter<" + type.getSimpleName() + "> [name=" + name + "]";
        } else if (position != null) {
            return "Parameter<" + type.getSimpleName() + "> [position=" + position + "]";
        } else {
            return "Parameter<" + type.getSimpleName() + "> [unnamed/positionless]";
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MansartParameter<?> that = (MansartParameter<?>) o;
        return Objects.equals(name, that.name) && Objects.equals(position, that.position) && Objects.equals(type, that.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, position, type);
    }
}
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
package io.vidocq.mansart.persistence.processor.enhancer;

import java.util.Objects;

/**
 * Represents an enhanced attribute for bytecode enhancement.
 * <p>
 * This class holds all the information needed to generate enhanced
 * accessor methods (getters/setters) with dirty tracking and lazy loading.
 */
public final class EnhancedAttribute {

    private final String name;
    private final String type;
    private final int bitPosition;
    private final boolean needsLazyLoading;
    private final boolean needsDirtyTracking;

    /**
     * Creates a new EnhancedAttribute.
     *
     * @param name the name of the attribute
     * @param type the Java type of the attribute
     * @param bitPosition the bit position in the dirty mask (0 = first bit)
     * @param needsLazyLoading true if this attribute should support lazy loading
     * @param needsDirtyTracking true if this attribute should support dirty tracking
     */
    public EnhancedAttribute(String name, String type, int bitPosition,
                            boolean needsLazyLoading, boolean needsDirtyTracking) {
        this.name = Objects.requireNonNull(name, "Attribute name cannot be null");
        this.type = Objects.requireNonNull(type, "Attribute type cannot be null");
        this.bitPosition = bitPosition;
        this.needsLazyLoading = needsLazyLoading;
        this.needsDirtyTracking = needsDirtyTracking;
    }

    /**
     * Returns the name of the attribute.
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the Java type of the attribute.
     */
    public String getType() {
        return type;
    }

    /**
     * Returns the bit position in the dirty mask.
     * Each attribute gets one bit: 0 = first, 1 = second, etc.
     */
    public int getBitPosition() {
        return bitPosition;
    }

    /**
     * Returns true if this attribute needs lazy loading support.
     * This is typically true for association fields (ManyToOne, OneToMany, etc.).
     */
    public boolean needsLazyLoading() {
        return needsLazyLoading;
    }

    /**
     * Returns true if this attribute needs dirty tracking.
     * This is typically true for all persistent fields except version and read-only fields.
     */
    public boolean needsDirtyTracking() {
        return needsDirtyTracking;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EnhancedAttribute that = (EnhancedAttribute) o;
        return bitPosition == that.bitPosition &&
               needsLazyLoading == that.needsLazyLoading &&
               needsDirtyTracking == that.needsDirtyTracking &&
               name.equals(that.name) &&
               type.equals(that.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, type, bitPosition, needsLazyLoading, needsDirtyTracking);
    }

    @Override
    public String toString() {
        return "EnhancedAttribute{" +
                "name='" + name + '\'' +
                ", type='" + type + '\'' +
                ", bitPosition=" + bitPosition +
                ", needsLazyLoading=" + needsLazyLoading +
                ", needsDirtyTracking=" + needsDirtyTracking +
                '}';
    }
}

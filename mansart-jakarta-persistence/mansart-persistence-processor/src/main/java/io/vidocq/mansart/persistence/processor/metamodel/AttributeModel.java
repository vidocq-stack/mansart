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
package io.vidocq.mansart.persistence.processor.metamodel;

import java.util.Objects;

/**
 * Represents a persistent attribute model for static metamodel generation.
 * <p>
 * This class holds all the information needed to generate a single attribute
 * in the static metamodel class (Entity_, etc.).
 */
public final class AttributeModel {

    private final String name;
    private final String javaType;
    private final String persistentAttributeType;
    private final boolean plural;
    private final boolean association;
    private final boolean basic;
    private final String declarationType;

    /**
     * Creates a new AttributeModel.
     *
     * @param name the name of the attribute
     * @param javaType the Java type of the attribute (as a string)
     * @param persistentAttributeType the persistent attribute type (mapped type)
     * @param plural true if this is a plural attribute (collection-valued)
     * @param association true if this is an association attribute
     * @param basic true if this is a basic attribute
     * @param declarationType the declaration type (FIELD or METHOD)
     */
    public AttributeModel(String name, String javaType, String persistentAttributeType,
                          boolean plural, boolean association, boolean basic, String declarationType) {
        this.name = Objects.requireNonNull(name);
        this.javaType = Objects.requireNonNull(javaType);
        this.persistentAttributeType = Objects.requireNonNull(persistentAttributeType);
        this.plural = plural;
        this.association = association;
        this.basic = basic;
        this.declarationType = Objects.requireNonNull(declarationType);
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
    public String getJavaType() {
        return javaType;
    }

    /**
     * Returns the persistent attribute type (mapped type).
     */
    public String getPersistentAttributeType() {
        return persistentAttributeType;
    }

    /**
     * Returns true if this is a plural attribute (collection-valued).
     */
    public boolean isPlural() {
        return plural;
    }

    /**
     * Returns true if this is an association attribute.
     */
    public boolean isAssociation() {
        return association;
    }

    /**
     * Returns true if this is a basic attribute.
     */
    public boolean isBasic() {
        return basic;
    }

    /**
     * Returns the declaration type (FIELD or METHOD).
     */
    public String getDeclarationType() {
        return declarationType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AttributeModel that = (AttributeModel) o;
        return plural == that.plural && 
               association == that.association && 
               basic == that.basic && 
               name.equals(that.name) && 
               javaType.equals(that.javaType) && 
               persistentAttributeType.equals(that.persistentAttributeType) &&
               declarationType.equals(that.declarationType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, javaType, persistentAttributeType, plural, association, basic, declarationType);
    }

    @Override
    public String toString() {
        return "AttributeModel{" +
                "name='" + name + '\'' +
                ", javaType='" + javaType + '\'' +
                ", persistentAttributeType='" + persistentAttributeType + '\'' +
                ", plural=" + plural +
                ", association=" + association +
                ", basic=" + basic +
                ", declarationType='" + declarationType + '\'' +
                '}';
    }
}

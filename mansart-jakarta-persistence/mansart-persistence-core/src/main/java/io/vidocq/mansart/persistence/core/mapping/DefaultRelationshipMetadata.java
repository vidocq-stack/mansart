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
package io.vidocq.mansart.persistence.core.mapping;

import io.vidocq.mansart.persistence.spi.RelationshipMetadata;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Default implementation of {@link RelationshipMetadata}.
 */
public final class DefaultRelationshipMetadata implements RelationshipMetadata {

    private final RelationshipType type;
    private final Class<?> targetEntity;
    private final String joinColumnName;
    private final boolean joinColumnNullable;
    private final boolean optional;
    private final FetchType fetchType;
    private final CascadeType[] cascadeTypes;
    private final boolean orphanRemoval;
    private final String referencedColumnName;

    private DefaultRelationshipMetadata(Builder builder) {
        this.type = builder.type;
        this.targetEntity = builder.targetEntity;
        this.joinColumnName = builder.joinColumnName;
        this.joinColumnNullable = builder.joinColumnNullable;
        this.optional = builder.optional;
        this.fetchType = builder.fetchType;
        this.cascadeTypes = builder.cascadeTypes;
        this.orphanRemoval = builder.orphanRemoval;
        this.referencedColumnName = builder.referencedColumnName;
    }

    @Override
    public RelationshipType getRelationshipType() {
        return type;
    }

    @Override
    public Class<?> getTargetEntity() {
        return targetEntity;
    }

    @Override
    public String getJoinColumnName() {
        return joinColumnName;
    }

    @Override
    public boolean isJoinColumnNullable() {
        return joinColumnNullable;
    }

    @Override
    public boolean isOptional() {
        return optional;
    }

    @Override
    public FetchType getFetchType() {
        return fetchType;
    }

    @Override
    public CascadeType[] getCascadeTypes() {
        return cascadeTypes != null ? cascadeTypes : new CascadeType[0];
    }

    @Override
    public boolean isOrphanRemoval() {
        return orphanRemoval;
    }

    @Override
    public String getReferencedColumnName() {
        return referencedColumnName;
    }

    /**
     * Returns the set of cascade types as an unmodifiable set.
     *
     * @return unmodifiable set of cascade types
     */
    public Set<CascadeType> getCascadeTypeSet() {
        if (cascadeTypes == null || cascadeTypes.length == 0) {
            return Collections.emptySet();
        }
        return Arrays.stream(cascadeTypes).collect(Collectors.toUnmodifiableSet());
    }

    /**
     * Creates a new builder for DefaultRelationshipMetadata.
     *
     * @return a new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for DefaultRelationshipMetadata.
     */
    public static final class Builder {
        private RelationshipType type;
        private Class<?> targetEntity;
        private String joinColumnName;
        private boolean joinColumnNullable = true;
        private boolean optional = true;
        private FetchType fetchType = FetchType.EAGER;
        private CascadeType[] cascadeTypes = new CascadeType[0];
        private boolean orphanRemoval = false;
        private String referencedColumnName;

        private Builder() {
        }

        public Builder type(RelationshipType type) {
            this.type = type;
            return this;
        }

        public Builder targetEntity(Class<?> targetEntity) {
            this.targetEntity = targetEntity;
            return this;
        }

        public Builder joinColumnName(String joinColumnName) {
            this.joinColumnName = joinColumnName;
            return this;
        }

        public Builder joinColumnNullable(boolean joinColumnNullable) {
            this.joinColumnNullable = joinColumnNullable;
            return this;
        }

        public Builder optional(boolean optional) {
            this.optional = optional;
            return this;
        }

        public Builder fetchType(FetchType fetchType) {
            this.fetchType = fetchType;
            return this;
        }

        public Builder cascadeTypes(CascadeType... cascadeTypes) {
            this.cascadeTypes = cascadeTypes != null ? cascadeTypes : new CascadeType[0];
            return this;
        }

        public Builder orphanRemoval(boolean orphanRemoval) {
            this.orphanRemoval = orphanRemoval;
            return this;
        }

        public Builder referencedColumnName(String referencedColumnName) {
            this.referencedColumnName = referencedColumnName;
            return this;
        }

        public DefaultRelationshipMetadata build() {
            return new DefaultRelationshipMetadata(this);
        }
    }
}

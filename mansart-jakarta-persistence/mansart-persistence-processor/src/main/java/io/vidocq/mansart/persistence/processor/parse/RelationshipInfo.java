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
package io.vidocq.mansart.persistence.processor.parse;

import io.vidocq.mansart.persistence.spi.RelationshipMetadata;

/**
 * Holds relationship information extracted during annotation processing.
 *
 * <p>This class contains all the metadata needed to create a runtime
 * {@link RelationshipMetadata} instance.
 */
public record RelationshipInfo(
        RelationshipType type,
        String targetEntityName,
        RelationshipMetadata.FetchType fetchType,
        RelationshipMetadata.CascadeType[] cascadeTypes,
        boolean optional,
        String joinColumnName,
        String referencedColumnName,
        boolean joinColumnNullable,
        boolean orphanRemoval,
        String mappedBy
) {
    
    /**
     * Types of relationships supported.
     */
    public enum RelationshipType {
        MANY_TO_ONE,
        ONE_TO_ONE,
        ONE_TO_MANY,
        MANY_TO_MANY
    }

    /**
     * Creates a RelationshipInfo from ManyToOneParser.ManyToOneInfo.
     */
    public static RelationshipInfo fromManyToOne(ManyToOneParser.ManyToOneInfo info) {
        return new RelationshipInfo(
            RelationshipType.MANY_TO_ONE,
            info.targetEntityName(),
            info.fetchType(),
            info.cascadeTypes(),
            info.optional(),
            info.joinColumnInfo() != null ? info.joinColumnInfo().name() : null,
            info.joinColumnInfo() != null ? info.joinColumnInfo().referencedColumnName() : null,
            info.joinColumnInfo() != null ? info.joinColumnInfo().nullable() : true,
            info.orphanRemoval(),
            null
        );
    }

    /**
     * Creates a RelationshipInfo from OneToOneParser.OneToOneInfo.
     */
    public static RelationshipInfo fromOneToOne(OneToOneParser.OneToOneInfo info) {
        return new RelationshipInfo(
            RelationshipType.ONE_TO_ONE,
            info.targetEntityName(),
            info.fetchType(),
            info.cascadeTypes(),
            info.optional(),
            info.joinColumnInfo() != null ? info.joinColumnInfo().name() : null,
            info.joinColumnInfo() != null ? info.joinColumnInfo().referencedColumnName() : null,
            info.joinColumnInfo() != null ? info.joinColumnInfo().nullable() : true,
            info.orphanRemoval(),
            info.mappedBy()
        );
    }

    /**
     * Creates a RelationshipInfo from OneToManyParser.OneToManyInfo.
     */
    public static RelationshipInfo fromOneToMany(OneToManyParser.OneToManyInfo info) {
        return new RelationshipInfo(
            RelationshipType.ONE_TO_MANY,
            info.targetEntityName(),
            info.fetchType(),
            info.cascadeTypes(),
            true, // OneToMany is always optional (collection can be empty)
            info.joinColumnInfo() != null ? info.joinColumnInfo().name() : null,
            info.joinColumnInfo() != null ? info.joinColumnInfo().referencedColumnName() : null,
            info.joinColumnInfo() != null ? info.joinColumnInfo().nullable() : true,
            info.orphanRemoval(),
            info.mappedBy()
        );
    }

    /**
     * Creates a RelationshipInfo from ManyToManyParser.ManyToManyInfo.
     */
    public static RelationshipInfo fromManyToMany(ManyToManyParser.ManyToManyInfo info) {
        return new RelationshipInfo(
            RelationshipType.MANY_TO_MANY,
            info.targetEntityName(),
            info.fetchType(),
            info.cascadeTypes(),
            true, // ManyToMany is always optional (collection can be empty)
            null, // Join column name from join table
            null, // Referenced column name from join table
            true, // Join column nullable
            false, // ManyToMany doesn't support orphanRemoval
            info.mappedBy()
        );
    }
}

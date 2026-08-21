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
package io.vidocq.mansart.persistence.core.runtime;

import jakarta.persistence.PersistenceUnitUtil;
import jakarta.persistence.metamodel.Attribute;

/**
 * Mansart implementation of {@link PersistenceUnitUtil}.
 *
 * <p>Provides utility methods for obtaining the identifier of an entity.
 */
public class MansartPersistenceUnitUtil implements PersistenceUnitUtil {

    @Override
    public Object getIdentifier(Object entity) {
        if (entity == null) {
            return null;
        }
        try {
            var metadata = io.vidocq.mansart.persistence.spi.EntityMetadataRegistry.getMetadata(entity.getClass());
            if (metadata != null) {
                return metadata.readAttribute(entity, metadata.getIdAttributeName());
            }
        } catch (Exception e) {
            // No metadata available — cannot determine identifier
        }
        return null;
    }

    @Override
    public Object getVersion(Object entity) {
        // For now, return null - version tracking not yet implemented
        return null;
    }

    @Override
    public boolean isLoaded(Object entity) {
        // For now, assume all entities are loaded
        return true;
    }

    @Override
    public <E> boolean isLoaded(E entity, Attribute<? super E, ?> attribute) {
        // For now, assume all attributes are loaded
        return true;
    }

    @Override
    public boolean isLoaded(Object entity, String attributeName) {
        // For now, assume all attributes are loaded
        return true;
    }

    @Override
    public void load(Object entity) {
        // No-op for now
    }

    @Override
    public void load(Object entity, String attributeName) {
        // No-op for now
    }

    @Override
    public <E> void load(E entity, Attribute<? super E, ?> attribute) {
        // No-op for now
    }

    @Override
    public boolean isInstance(Object entity, Class<?> targetEntity) {
        if (entity == null) {
            return false;
        }
        return targetEntity.isInstance(entity);
    }

    @Override
    public <T> Class<? extends T> getClass(T entity) {
        if (entity == null) {
            return null;
        }
        @SuppressWarnings("unchecked")
        Class<? extends T> result = (Class<? extends T>) entity.getClass();
        return result;
    }
}

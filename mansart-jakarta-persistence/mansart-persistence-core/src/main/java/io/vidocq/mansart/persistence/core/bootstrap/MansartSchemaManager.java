/*
 * Copyright (c) ${year} Yann Blazart, Antoine Sabot-Durand and the Vidocq contributors
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * This Source Code may also be made available under the following Secondary
 * Licenses when the conditions for such availability set forth in the
 * Eclipse Public License v. 2.0 are satisfied: GNU General Public License, version 2
 * or later version, which is available at
 * https://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 *
 * It is also made available under the European Union Public Licence v. 1.2,
 * which is available at
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 *
 * SPDX-License-Identifier: EPL-2.0 OR EUPL-1.2 OR GPL-2.0-or-later
 */

package io.vidocq.mansart.persistence.core.bootstrap;

import jakarta.persistence.SchemaManager;
import jakarta.persistence.SchemaValidationException;

/**
 * Basic implementation of SchemaManager for Mansart.
 * For M7, this is a placeholder implementation that logs operations.
 * Full schema generation will be implemented in later milestones.
 */
public class MansartSchemaManager implements SchemaManager {

    private final DefaultMansartEntityManagerFactory entityManagerFactory;

    public MansartSchemaManager(DefaultMansartEntityManagerFactory entityManagerFactory) {
        this.entityManagerFactory = entityManagerFactory;
    }

    @Override
    public void create(boolean force) {
        // TODO: Implement schema creation
        // For now, this is a no-op as Mansart delegates schema operations
        // to the underlying data layer (Mansart Data)
        System.Logger logger = System.getLogger(getClass().getName());
        logger.log(System.Logger.Level.INFO, "SchemaManager.create() called with force=" + force);
    }

    @Override
    public void drop(boolean force) {
        // TODO: Implement schema dropping
        System.Logger logger = System.getLogger(getClass().getName());
        logger.log(System.Logger.Level.INFO, "SchemaManager.drop() called with force=" + force);
    }

    @Override
    public void validate() throws SchemaValidationException {
        // TODO: Implement schema validation
        System.Logger logger = System.getLogger(getClass().getName());
        logger.log(System.Logger.Level.INFO, "SchemaManager.validate() called");
        
        // For now, validation always passes
        // In a real implementation, this would validate that the database
        // schema matches the entity mappings
    }

    @Override
    public void truncate() {
        // TODO: Implement schema truncation
        System.Logger logger = System.getLogger(getClass().getName());
        logger.log(System.Logger.Level.INFO, "SchemaManager.truncate() called");
    }
}

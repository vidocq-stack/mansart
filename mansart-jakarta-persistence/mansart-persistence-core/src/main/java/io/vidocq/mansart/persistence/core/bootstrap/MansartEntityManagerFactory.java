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
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-1.2
 *
 * SPDX-License-Identifier: EPL-2.0 OR EUPL-1.2 OR GPL-2.0-or-later
 */

package io.vidocq.mansart.persistence.core.bootstrap;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceUnitUtil;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.metamodel.Metamodel;
import jakarta.persistence.SynchronizationType;
import jakarta.persistence.Cache;
import jakarta.persistence.PersistenceUnitTransactionType;
import jakarta.persistence.SchemaManager;
import jakarta.persistence.EntityGraph;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQueryReference;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Abstract base class for Mansart EntityManagerFactory implementations.
 * All methods throw UnsupportedOperationException by default.
 * Concrete implementations (like DefaultMansartEntityManagerFactory) should
 * override the methods they support.
 */
public abstract class MansartEntityManagerFactory implements EntityManagerFactory {

    private final String persistenceUnitName;
    private final Map properties;

    /**
     * Creates a new MansartEntityManagerFactory.
     *
     * @param persistenceUnitName the name of the persistence unit
     * @param properties the properties for this factory
     */
    public MansartEntityManagerFactory(String persistenceUnitName, Map properties) {
        this.persistenceUnitName = persistenceUnitName;
        this.properties = properties;
    }

    /**
     * Returns the properties for this factory.
     *
     * @return the properties map
     */
    @Override
    public Map getProperties() {
        return properties;
    }

    /**
     * Returns the name of the persistence unit.
     *
     * @return the persistence unit name
     */
    public String getPersistenceUnitName() {
        return persistenceUnitName;
    }

    /**
     * M6 — Returns the list of entity classes managed by this persistence unit.
     * Override in concrete implementations to return actual entity classes.
     *
     * @return unmodifiable list of entity classes
     */
    public List<Class<?>> getEntityClasses() {
        return Collections.emptyList();
    }
}

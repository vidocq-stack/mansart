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
package io.vidocq.mansart.persistence.tests;

import io.vidocq.mansart.persistence.core.bootstrap.MansartPersistenceProvider;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import java.util.Collections;

/**
 * Base class for persistence tests.
 * Sets up and tears down EntityManager for each test.
 */
public abstract class BasePersistenceTest {

    private static final String PERSISTENCE_UNIT_NAME = "mansart-test-pu";

    protected EntityManagerFactory emf;
    protected EntityManager em;

    @BeforeEach
    public void setUp() {
        // Use the provider directly to bypass ServiceLoader issues
        MansartPersistenceProvider provider = new MansartPersistenceProvider();
        emf = provider.createEntityManagerFactory(PERSISTENCE_UNIT_NAME, Collections.emptyMap());
        em = emf.createEntityManager();
    }

    @AfterEach
    public void tearDown() {
        if (em != null && em.isOpen()) {
            em.close();
        }
        if (emf != null && emf.isOpen()) {
            emf.close();
        }
    }

    protected EntityTransaction getTransaction() {
        return em.getTransaction();
    }
}

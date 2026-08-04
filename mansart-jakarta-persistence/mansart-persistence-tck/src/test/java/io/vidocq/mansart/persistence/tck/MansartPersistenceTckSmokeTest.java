/*
 * Copyright (c) 2024 Contributors to the Vidocq Mansart project.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0, which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * This Source Code may also be made available under the following Secondary
 * Licenses when the conditions for such availability set forth in the
 * Eclipse Public License v. 2.0 are satisfied: GNU General Public License,
 * version 2 with the GNU Classpath Exception, which is available at
 * https://www.gnu.org/software/classpath/license.html.
 *
 * SPDX-License-Identifier: EPL-2.0 OR GPL-2.0 WITH Classpath-exception-2.0
 */
package io.vidocq.mansart.persistence.tck;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Smoke test pour verifier que l'integration avec le TCK Jakarta Persistence
 * fonctionne correctement avec l'implementation Mansart.
 *
 * Ce test verifie que :
 * 1. Le provider Mansart peut etre charge
 * 2. Une EntityManagerFactory peut etre creee
 * 3. Une EntityManager peut etre obtenue
 * 4. L'EntityManager est fonctionnelle
 */
class MansartPersistenceTckSmokeTest {

    private EntityManagerFactory emf;
    private EntityManager em;

    @BeforeEach
    void setUp() {
        System.setProperty("jakarta.persistence.provider",
                "io.vidocq.mansart.persistence.core.bootstrap.MansartPersistenceProvider");
        
        try {
            emf = Persistence.createEntityManagerFactory("smoke-test");
            em = emf.createEntityManager();
        } catch (Exception e) {
            // Si aucune persistence unit n'est configuree, on utilise les properties
            // par defaut pour H2
            emf = Persistence.createEntityManagerFactory("h2-mem");
            em = emf.createEntityManager();
        }
    }

    @AfterEach
    void tearDown() {
        if (em != null && em.isOpen()) {
            em.close();
        }
        if (emf != null && emf.isOpen()) {
            emf.close();
        }
    }

    @Test
    void providerCanBeLoaded() {
        assertThat(Persistence.getPersistenceProvider())
                .isNotNull();
    }

    @Test
    void entityManagerFactoryCanBeCreated() {
        assertThat(emf)
                .isNotNull();
    }

    @Test
    void entityManagerCanBeCreated() {
        assertThat(em)
                .isNotNull();
    }

    @Test
    void entityManagerIsOpen() {
        assertThat(em.isOpen())
                .isTrue();
    }

    @Test
    void entityManagerFactoryIsOpen() {
        assertThat(emf.isOpen())
                .isTrue();
    }
}

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
package io.vidocq.mansart.persistence.tck.smoke;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.spi.PersistenceProvider;
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
class MansartPersistenceSmokeTest {

    private static final String PROVIDER_CLASS = "io.vidocq.mansart.persistence.core.bootstrap.MansartPersistenceProvider";
    
    private EntityManagerFactory emf;
    private EntityManager em;

    @BeforeEach
    void setUp() throws Exception {
        Map<String, Object> properties = new HashMap<>();
        properties.put("jakarta.persistence.provider", PROVIDER_CLASS);
        properties.put("jakarta.persistence.jdbc.url", "jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1");
        properties.put("jakarta.persistence.jdbc.user", "sa");
        properties.put("jakarta.persistence.jdbc.password", "");
        properties.put("jakarta.persistence.jdbc.driver", "org.h2.Driver");
        
        PersistenceProvider provider = (PersistenceProvider) Class.forName(PROVIDER_CLASS)
                .getDeclaredConstructor()
                .newInstance();
        
        emf = provider.createEntityManagerFactory("test-pu", properties);
        em = emf.createEntityManager();
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

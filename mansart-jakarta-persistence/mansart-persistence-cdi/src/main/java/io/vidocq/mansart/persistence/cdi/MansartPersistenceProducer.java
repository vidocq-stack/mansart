/*
 * Copyright (c) 2026 Yann Blazart, Antoine Sabot-Durand and the Vidocq contributors
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0 OR EUPL-1.2 OR GPL-2.0-or-later
 */
package io.vidocq.mansart.persistence.cdi;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Disposes;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Named;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import jakarta.persistence.PersistenceUnit;

/**
 * CDI Producer for JPA EntityManagerFactory and EntityManager (M9-7).
 * 
 * <p>This class provides CDI beans for:
 * <ul>
 *   <li>{@link EntityManagerFactory} - application scoped, created from persistence.xml</li>
 *   <li>{@link EntityManager} - request/proxy scoped, created from EntityManagerFactory</li>
 * </ul>
 * 
 * <p>Mirror of {@link io.vidocq.mansart.data.cdi.MansartRuntimeProducer} for Jakarta Persistence.
 */
public class MansartPersistenceProducer {

    /**
     * Default persistence unit name.
     * Can be overridden by specifying a different persistence unit in persistence.xml
     * or by using @PersistenceUnit annotation.
     */
    private static final String DEFAULT_PERSISTENCE_UNIT = "mansart-pu";

    /**
     * Produces an EntityManagerFactory for the default persistence unit.
     * The EntityManagerFactory is application scoped and will be automatically
     * closed when the application shuts down.
     * 
     * @return the EntityManagerFactory for the default persistence unit
     */
    @Produces
    @ApplicationScoped
    @Named("default")
    public EntityManagerFactory produceEntityManagerFactory() {
        try {
            return Persistence.createEntityManagerFactory(DEFAULT_PERSISTENCE_UNIT);
        } catch (Exception e) {
            throw new RuntimeException(
                "Failed to create EntityManagerFactory for persistence unit: " + DEFAULT_PERSISTENCE_UNIT, e);
        }
    }

    /**
     * Disposes the EntityManagerFactory when the application context ends.
     * 
     * @param emf the EntityManagerFactory to close
     */
    public void disposeEntityManagerFactory(@Disposes EntityManagerFactory emf) {
        if (emf != null && emf.isOpen()) {
            emf.close();
        }
    }

    /**
     * Produces a request-scoped EntityManager from the EntityManagerFactory.
     * Each EntityManager is tied to the current transaction and will be
     * automatically closed at the end of the request.
     * 
     * @param emf the EntityManagerFactory
     * @return a new EntityManager
     */
    @Produces
    // Note: In a real implementation, this would use @RequestScoped
    // For now, we return a new instance each time as we don't have
    // request scope in this basic implementation
    public EntityManager produceEntityManager(EntityManagerFactory emf) {
        if (emf == null || !emf.isOpen()) {
            throw new IllegalStateException("EntityManagerFactory is not available or closed");
        }
        return emf.createEntityManager();
    }

    /**
     * Disposes the EntityManager when the request context ends.
     * 
     * @param em the EntityManager to close
     */
    public void disposeEntityManager(@Disposes EntityManager em) {
        if (em != null && em.isOpen()) {
            em.close();
        }
    }

}

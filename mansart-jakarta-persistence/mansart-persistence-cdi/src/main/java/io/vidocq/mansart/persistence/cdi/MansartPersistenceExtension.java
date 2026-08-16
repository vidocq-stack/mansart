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

import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.build.compatible.spi.BuildCompatibleExtension;
import jakarta.enterprise.inject.build.compatible.spi.Enhancement;
import jakarta.enterprise.inject.build.compatible.spi.Messages;
import jakarta.enterprise.inject.build.compatible.spi.Validation;
import jakarta.enterprise.inject.spi.Extension;

/**
 * CDI Build Compatible Extension for Mansart Persistence (M9-7).
 * 
 * <p>This extension mirrors the structure of mansart-data-cdi, providing:
 * <ul>
 *   <li>Discovery and registration of JPA EntityManagerFactory producers</li>
 *   <li>Discovery and registration of JPA EntityManager producers</li>
 *   <li>Integration with Vauban CDI Lite container</li>
 * </ul>
 * 
 * <p>Mirror of {@link io.vidocq.mansart.data.cdi.MansartDataExtension} for Jakarta Persistence.
 */
public class MansartPersistenceExtension implements BuildCompatibleExtension {

    /**
     * Called during the validation phase of the build.
     * This implementation performs no validation as Mansart Persistence
     * relies on runtime configuration via persistence.xml.
     * 
     * @param validation the validation context
     */
    public void validate(@Observes Validation validation) {
        // No validation needed - Mansart Persistence uses runtime discovery
        // via ServiceLoader and persistence.xml configuration
    }

    /**
     * Called during the enhancement phase of the build.
     * This implementation performs no bytecode enhancement as Mansart Persistence
     * uses APT (Annotation Processing Tool) for compile-time generation.
     * 
     * @param enhancement the enhancement context
     */
    public void enhance(@Observes Enhancement enhancement) {
        // No bytecode enhancement needed - Mansart uses APT for:
        // - Static metamodel generation
        // - Entity support classes
        // - Repository implementations (future)
    }

    /**
     * Called to register messages during the build.
     * This implementation registers no messages.
     * 
     * @param messages the messages context
     */
    public void messages(@Observes Messages messages) {
        // No messages to register
    }
}

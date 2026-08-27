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
 * which is available at https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 *
 * SPDX-License-Identifier: EPL-2.0 OR EUPL-1.2 OR GPL-2.0-or-later
 */
package io.vidocq.mansart.persistence.tests;

import jakarta.persistence.spi.PersistenceProvider;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Verify that the Mansart persistence provider is discoverable via ServiceLoader
 * and that all its methods fail by design with {@code UnsupportedOperationException}.
 *
 * @see io.vidocq.mansart.persistence.core.MansartPersistenceProvider
 */
class ProviderDiscoveryTest {

    @Test
    void providerIsDiscoverableViaServiceLoader() {
        List<PersistenceProvider> providers = new ArrayList<>();
        Iterator<PersistenceProvider> it = ServiceLoader.load(PersistenceProvider.class).iterator();
        while (it.hasNext()) {
            providers.add(it.next());
        }
        assertThat(providers)
            .hasSize(1)
            .first()
            .isInstanceOf(io.vidocq.mansart.persistence.core.MansartPersistenceProvider.class);
    }

    @Test
    void createEntityManagerFactoryThrows() {
        PersistenceProvider provider = ServiceLoader.load(PersistenceProvider.class).iterator().next();

        // createContainerEntityManagerFactory() is now implemented — it parses persistence.xml
        // and throws IllegalArgumentException when the requested persistence unit is not found.
        assertThatExceptionOfType(IllegalArgumentException.class)
            .isThrownBy(() -> provider.createEntityManagerFactory("nonexistent", Map.of()))
            .withMessageContaining("not found in persistence.xml");
    }
}
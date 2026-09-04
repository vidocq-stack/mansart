/*
 * SPDX-License-Identifier: EPL-2.0 OR EUPL-1.2 OR GPL-2.0-or-later
 */
package io.vidocq.mansart.persistence.tck;

import jakarta.persistence.spi.PersistenceProvider;
import org.junit.jupiter.api.Test;

import java.util.ServiceLoader;
import java.util.stream.StreamSupport;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test that the Mansart PersistenceProvider is discoverable via ServiceLoader.
 * This is the baseline test for M0-JP-06: Harness wiring + provider registration.
 */
public class ProviderDiscoveryTest {

    @Test
    public void testProviderIsDiscoverable() {
        ServiceLoader<PersistenceProvider> loader = ServiceLoader.load(PersistenceProvider.class);
        
        var providers = StreamSupport.stream(loader.spliterator(), false)
                .toList();
        
        assertFalse(providers.isEmpty(), "No PersistenceProvider implementations found");
        
        var mansartProvider = providers.stream()
                .filter(p -> p.getClass().getName().equals("io.vidocq.mansart.persistence.core.MansartPersistenceProvider"))
                .findFirst();
        
        assertTrue(mansartProvider.isPresent(), 
                "MansartPersistenceProvider not found. Check META-INF/services/jakarta.persistence.spi.PersistenceProvider");
    }

    @Test
    public void testProviderThrowsNotImplemented() {
        ServiceLoader<PersistenceProvider> loader = ServiceLoader.load(PersistenceProvider.class);
        
        var mansartProvider = StreamSupport.stream(loader.spliterator(), false)
                .filter(p -> p.getClass().getName().equals("io.vidocq.mansart.persistence.core.MansartPersistenceProvider"))
                .findFirst();
        
        assertTrue(mansartProvider.isPresent(), "MansartPersistenceProvider not found");
        
        // All methods should throw UnsupportedOperationException for stub implementation
        var provider = mansartProvider.get();
        
        assertThrows(UnsupportedOperationException.class, 
            () -> provider.createEntityManagerFactory("test", java.util.Map.of()));
    }
}

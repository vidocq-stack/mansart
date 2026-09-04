/*
 * SPDX-License-Identifier: EPL-2.0 OR EUPL-1.2 OR GPL-2.0-or-later
 */
package io.vidocq.mansart.persistence.tests;

import io.vidocq.mansart.persistence.core.MansartEntityManagerFactory;
import io.vidocq.mansart.persistence.core.MansartPersistenceProvider;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import jakarta.persistence.PersistenceConfiguration;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for M4-JP-23: MansartPersistenceProvider parses persistence.xml and creates
 * a working EntityManagerFactory, via both the classic XML path and the JPA 3.2
 * programmatic {@link PersistenceConfiguration} path.
 */
public class MansartPersistenceProviderTest {

    @Test
    public void createEntityManagerFactoryFromXmlReturnsMansartFactory() {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("test-pu");

        assertThat(emf).isInstanceOf(MansartEntityManagerFactory.class);
        assertThat(emf.getName()).isEqualTo("test-pu");
        assertThat(emf.isOpen()).isTrue();
        emf.close();
        assertThat(emf.isOpen()).isFalse();
    }

    @Test
    public void xmlPropertiesAreExposedByFactory() {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("test-pu");

        assertThat(emf.getProperties())
                .containsEntry("jakarta.persistence.jdbc.url", "jdbc:h2:mem:testdb")
                .containsEntry("io.vidocq.mansart.test.marker", "from-xml");

        emf.close();
    }

    @Test
    public void runtimePropertiesOverrideXmlProperties() {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("test-pu",
                java.util.Map.of("io.vidocq.mansart.test.marker", "overridden"));

        assertThat(emf.getProperties())
                .containsEntry("io.vidocq.mansart.test.marker", "overridden");

        emf.close();
    }

    @Test
    public void unknownPersistenceUnitReturnsNullFromProvider() {
        MansartPersistenceProvider provider = new MansartPersistenceProvider();
        EntityManagerFactory emf = provider.createEntityManagerFactory("does-not-exist", java.util.Map.of());

        assertThat(emf).isNull();
    }

    @Test
    public void secondPersistenceUnitIsParsed() {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("second-pu");

        assertThat(emf.getName()).isEqualTo("second-pu");
        assertThat(emf.getProperties())
                .containsEntry("io.vidocq.mansart.test.marker", "second");

        emf.close();
    }

    @Test
    public void programmaticConfigurationCreatesFactory() {
        PersistenceConfiguration config = new PersistenceConfiguration("prog-pu")
                .transactionType(jakarta.persistence.PersistenceUnitTransactionType.RESOURCE_LOCAL)
                .property("io.vidocq.mansart.test.marker", "programmatic");

        EntityManagerFactory emf = config.createEntityManagerFactory();

        assertThat(emf).isInstanceOf(MansartEntityManagerFactory.class);
        assertThat(emf.getName()).isEqualTo("prog-pu");
        assertThat(emf.getProperties())
                .containsEntry("io.vidocq.mansart.test.marker", "programmatic");
        assertThat(emf.isOpen()).isTrue();
        emf.close();
        assertThat(emf.isOpen()).isFalse();
    }

    @Test
    public void closingAlreadyClosedFactoryThrows() {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("test-pu");
        emf.close();

        assertThatThrownBy(emf::close)
                .isInstanceOf(IllegalStateException.class);
    }
}

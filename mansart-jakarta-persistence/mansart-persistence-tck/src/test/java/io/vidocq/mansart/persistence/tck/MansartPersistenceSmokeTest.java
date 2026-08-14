package io.vidocq.mansart.persistence.tck;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Basic smoke test to verify Mansart Persistence provider is discoverable
 * and can create an EntityManagerFactory.
 * 
 * This is the M7-20 entry point: verify the TCK runner configuration is correct.
 */
@Test
public class MansartPersistenceSmokeTest {

    private EntityManagerFactory emf;
    private EntityManager em;

    @BeforeMethod
    public void setUp() {
        emf = Persistence.createEntityManagerFactory("mansart-tck-pu");
        em = emf.createEntityManager();
    }

    @AfterMethod
    public void tearDown() {
        if (em != null) {
            em.close();
        }
        if (emf != null) {
            emf.close();
        }
    }

    @Test
    public void testPersistenceProviderIsDiscoverable() {
        assertThat(Persistence.createEntityManagerFactory("mansart-tck-pu"))
            .isNotNull();
    }

    @Test
    public void testEntityManagerFactoryIsOpen() {
        assertThat(emf.isOpen()).isTrue();
    }

    @Test
    public void testEntityManagerIsOpen() {
        assertThat(em.isOpen()).isTrue();
    }
}

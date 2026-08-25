/**
 * Core runtime — EntityManagerFactory, EntityManager, persistence context,
 * JPQL/Criteria, flush.
 *
 * Exports nothing; consumers obtain instances via
 * {@link jakarta.persistence.Persistence#createEntityManagerFactory}.
 * ServiceLoader support is added when real implementation classes exist.
 */
module io.vidocq.mansart.persistence.core {

    requires transitive jakarta.persistence;

    requires java.xml;

    requires io.vidocq.mansart.persistence.spi;
    requires io.vidocq.mansart.data.dialect.spi;

    requires jakarta.inject;
    requires static jakarta.transaction;

    provides jakarta.persistence.spi.PersistenceProvider
        with io.vidocq.mansart.persistence.core.MansartPersistenceProvider;

}

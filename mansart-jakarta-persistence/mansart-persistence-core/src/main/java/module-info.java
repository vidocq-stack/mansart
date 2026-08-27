/**
 * Core runtime — EntityManagerFactory, EntityManager, persistence context,
 * JPQL/Criteria, flush.
 *
 * Exports the metamodel package; consumers obtain instances via
 * {@link jakarta.persistence.Persistence#createEntityManagerFactory}.
 */
module io.vidocq.mansart.persistence.core {

    requires transitive jakarta.persistence;

    requires java.xml;

    requires io.vidocq.mansart.persistence.spi;
    requires io.vidocq.mansart.data.dialect.spi;

    requires jakarta.inject;
    requires static jakarta.transaction;

    exports io.vidocq.mansart.persistence.core.metamodel;

    provides jakarta.persistence.spi.PersistenceProvider
        with io.vidocq.mansart.persistence.core.MansartPersistenceProvider;

}

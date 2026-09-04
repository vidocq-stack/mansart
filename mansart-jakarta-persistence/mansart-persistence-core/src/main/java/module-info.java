/**
 * Mansart Jakarta Persistence 3.2 - Core Runtime Module
 *
 * Core implementation: PersistenceProvider, EntityManagerFactory, EntityManager,
 * PersistenceContext, Query execution, JPQL parsing, Criteria API.
 */
module io.vidocq.mansart.persistence.core {
    requires static jakarta.persistence;
    requires transitive io.vidocq.mansart.persistence.spi;
    requires transitive io.vidocq.mansart.transactions.core;
    requires transitive io.vidocq.mansart.data.dialect.spi;

    exports io.vidocq.mansart.persistence.core;
    exports io.vidocq.mansart.persistence.core.runtime;

    provides jakarta.persistence.spi.PersistenceProvider
        with io.vidocq.mansart.persistence.core.MansartPersistenceProvider;
}

/**
 * Mansart Jakarta Persistence 3.2 - CDI Integration Module
 *
 * CDI 4.1 integration via Build Compatible Extension (BCE) for Vauban.
 */
module io.vidocq.mansart.persistence.cdi {
    requires static jakarta.persistence;
    requires transitive io.vidocq.mansart.persistence.core;
    requires jakarta.cdi;
    requires jakarta.inject;

    exports io.vidocq.mansart.persistence.cdi;

    provides jakarta.enterprise.inject.spi.Extension
        with io.vidocq.mansart.persistence.cdi.MansartPersistenceExtension;
}
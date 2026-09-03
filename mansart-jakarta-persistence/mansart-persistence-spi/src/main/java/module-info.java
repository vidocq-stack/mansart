/**
 * Mansart Jakarta Persistence 3.2 - SPI Module
 *
 * Defines the metadata model (EntityModel, Attribute hierarchy) and
 * re-exports the dialect SPI from mansart-data-dialect-spi.
 */
module io.vidocq.mansart.persistence.spi {
    requires static jakarta.persistence;
    requires transitive io.vidocq.mansart.data.dialect.spi;

    exports io.vidocq.mansart.persistence.spi;
}

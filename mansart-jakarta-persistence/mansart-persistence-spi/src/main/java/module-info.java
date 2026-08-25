/**
 * Persistence metadata model — SPI types (exception classes, metadata POJOs)
 * and re-export of the data-dialect SPI contract.
 *
 * No public types are exported; this module is a consumer of
 * {@code jakarta.persistence} and a producer for the processor and core modules.
 */
module io.vidocq.mansart.persistence.spi {

    requires jakarta.persistence;

}

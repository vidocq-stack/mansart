/**
 * Mansart Persistence SPI module.
 * 
 * Service Provider Interface for Mansart Persistence implementation.
 * 
 * <p>Classes in this package are used by the APT processor and the core
 * implementation to manage entity metadata and persistence bootstrap.
 */
module io.vidocq.mansart.persistence.spi {
    requires transitive io.vidocq.mansart.persistence.api;
    requires transitive jakarta.persistence;
    
    exports io.vidocq.mansart.persistence.spi;
}

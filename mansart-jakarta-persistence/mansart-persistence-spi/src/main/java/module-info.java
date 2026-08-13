/**
 * Mansart Persistence SPI module.
 * 
 * Service Provider Interface for Mansart Persistence implementation.
 */
module io.vidocq.mansart.persistence.spi {
    requires io.vidocq.mansart.persistence.api;
    requires jakarta.persistence;
    requires io.vidocq.mansart.data.dialect.spi;
    
    exports io.vidocq.mansart.persistence.spi;
    
    // Open for service loading
    opens io.vidocq.mansart.persistence.spi to java.base;
}

/**
 * Mansart Persistence API module.
 * 
 * Exposes public API extensions for Jakarta Persistence 3.2 implementation.
 */
module io.vidocq.mansart.persistence.api {
    requires jakarta.persistence;
    requires jakarta.inject;
    requires jakarta.cdi;
    
    exports io.vidocq.mansart.persistence;
}

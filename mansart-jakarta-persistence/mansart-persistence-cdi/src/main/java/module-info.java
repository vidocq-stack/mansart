/**
 * Mansart Persistence CDI module.
 * 
 * CDI Build Compatible Extension for Mansart Persistence.
 */
module io.vidocq.mansart.persistence.cdi {
    requires io.vidocq.mansart.persistence.core;
    requires io.vidocq.mansart.persistence.api;
    requires jakarta.persistence;
    requires jakarta.enterprise.cdi;
    requires jakarta.inject;
    requires io.vidocq.vauban.cdi;
    
    exports io.vidocq.mansart.persistence.cdi;
    
    // Required for CDI service loading
    opens io.vidocq.mansart.persistence.cdi to java.base, jakarta.cdi;
}

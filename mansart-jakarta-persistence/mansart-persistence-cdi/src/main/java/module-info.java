/**
 * Mansart Persistence CDI module.
 * 
 * CDI Build Compatible Extension for Mansart Persistence.
 */
module io.vidocq.mansart.persistence.cdi {
    requires io.vidocq.mansart.persistence.core;
    requires io.vidocq.mansart.persistence.api;
    requires jakarta.persistence;
    requires jakarta.cdi;
    requires jakarta.inject;
}

/**
 * Mansart Persistence Core module.
 * 
 * Core runtime implementation of Jakarta Persistence 3.2.
 * 
 * Note: This module is in early development (M7-1). Most packages and classes
 * will be added in subsequent milestones.
 */
module io.vidocq.mansart.persistence.core {
    requires io.vidocq.mansart.persistence.api;
    requires io.vidocq.mansart.persistence.spi;
    requires jakarta.persistence;
    requires jakarta.transaction;
    requires java.sql;
    requires io.vidocq.mansart.data.dialect.spi;
    requires io.vidocq.mansart.data.dialect.h2;
    requires io.vidocq.mansart.transactions.core;
    
    exports io.vidocq.mansart.persistence.core.bootstrap;
}

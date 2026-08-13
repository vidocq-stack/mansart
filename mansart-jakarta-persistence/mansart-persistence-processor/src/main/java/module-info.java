/**
 * Mansart Persistence Processor module.
 * 
 * Annotation Processing Tool for static metamodel generation.
 */
module io.vidocq.mansart.persistence.processor {
    requires io.vidocq.mansart.persistence.api;
    requires io.vidocq.mansart.persistence.spi;
    requires jakarta.persistence;
    requires jakarta.annotation;
    
    // APT processor - no exports needed, runs at compile time
}

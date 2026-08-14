/**
 * Mansart Persistence Processor module.
 * 
 * Annotation Processing Tool for static metamodel generation and entity metadata.
 */
module io.vidocq.mansart.persistence.processor {
    requires io.vidocq.mansart.persistence.api;
    requires io.vidocq.mansart.persistence.spi;
    requires jakarta.persistence;
    requires jakarta.annotation;
    requires java.compiler;
    
    exports io.vidocq.mansart.persistence.processor;
    exports io.vidocq.mansart.persistence.processor.metadata;
    exports io.vidocq.mansart.persistence.processor.parse;
    
    uses javax.annotation.processing.Processor;
    
    provides javax.annotation.processing.Processor with
        io.vidocq.mansart.persistence.processor.MansartProcessor;
}

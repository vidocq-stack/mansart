/**
 * Mansart Jakarta Persistence 3.2 - APT Processor Module
 *
 * Annotation processor that generates entity metamodel from JPA annotations.
 */
module io.vidocq.mansart.persistence.processor {
    requires static jakarta.persistence;
    requires io.vidocq.mansart.persistence.spi;
    requires io.vidocq.mansart.data.dialect.spi;
    requires java.compiler;

    exports io.vidocq.mansart.persistence.processor;
}

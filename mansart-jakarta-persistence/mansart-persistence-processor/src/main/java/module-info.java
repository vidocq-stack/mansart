/**
 * Mansart Persistence annotation processor.
 *
 * <p>Processes {@code @jakarta.persistence.Entity} annotations and generates
 * the JPA static metamodel ({@code ClassName_}) for each entity class.</p>
 *
 * <p>This module is <b>not</b> on the runtime class path — it is consumed
 * at compile time by the Maven compiler plugin.</p>
 */
module io.vidocq.mansart.persistence.processor {

    requires java.compiler;
    requires io.vidocq.mansart.data.dialect.spi;

    exports io.vidocq.mansart.persistence.processor;

    provides javax.annotation.processing.Processor
            with io.vidocq.mansart.persistence.processor.MansartPersistenceProcessor;

}

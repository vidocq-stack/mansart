/**
 * Annotation processor — reads entity source, emits {@code _Entity} metamodel,
 * descriptors and accessors via APT (JDK 25, {@code RELEASE_25}).
 *
 * The Jakarta Persistence API is only needed at compile time;
 * {@code requires static} avoids a runtime dependency.
 */
module io.vidocq.mansart.persistence.processor {

    requires io.vidocq.mansart.persistence.spi;
    requires io.vidocq.mansart.data.dialect.spi;

    requires static jakarta.persistence;

}

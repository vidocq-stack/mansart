/**
 * Mansart Persistence Maven plugin — build-time codegen for external JARs.
 *
 * <p>This module produces a Maven plugin JAR only. No types are exported;
 * it is consumed at build time, never at runtime.</p>
 */
module io.vidocq.mansart.persistence.mavenplugin {

    requires io.vidocq.mansart.persistence.processor;

}

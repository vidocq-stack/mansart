/**
 * Mansart Jakarta Persistence 3.2 - External Library Module
 *
 * Test fixture: plain JPA entities compiled in a JAR without any Mansart dependencies.
 * Used to test that Tier 2 (Maven plugin) generates the same code as Tier 1 (APT).
 */
module io.vidocq.mansart.persistence.external.lib {
    requires static jakarta.persistence;

    exports io.vidocq.mansart.persistence.external.lib;
}

/**
 * Mansart Jakarta Persistence 3.2 - TCK Module
 *
 * OUT OF REACTOR - standalone module for running the official TCK.
 */
module io.vidocq.mansart.persistence.tck {
    requires static jakarta.persistence;
    requires io.vidocq.mansart.persistence.core;
    requires com.h2database;
}

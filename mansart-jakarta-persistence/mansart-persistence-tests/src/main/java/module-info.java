/**
 * Mansart Persistence Tests module.
 * 
 * Unit and integration tests for Mansart Persistence implementation.
 */
module io.vidocq.mansart.persistence.tests {
    requires io.vidocq.mansart.persistence.core;
    requires io.vidocq.mansart.persistence.api;
    requires jakarta.persistence;
    requires com.h2database;
    requires org.junit.jupiter.api;
    requires org.assertj.core;
    
    // Test module - no exports needed
    opens io.vidocq.mansart.persistence to org.junit.platform.commons;
}

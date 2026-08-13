/**
 * Mansart Persistence API module.
 * 
 * Re-exports Jakarta Persistence 3.2 API under Mansart groupId for 
 * consistent dependency management across the Mansart ecosystem.
 */
module io.vidocq.mansart.persistence.api {
    requires transitive jakarta.persistence;
    
    exports io.vidocq.mansart.persistence;
}

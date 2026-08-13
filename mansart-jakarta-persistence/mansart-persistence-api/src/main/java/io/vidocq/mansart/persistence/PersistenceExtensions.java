/**
 * Public API extensions for Mansart Persistence.
 * 
 * <p>This class provides utility methods and constants for the Mansart
 * Jakarta Persistence implementation.
 */
package io.vidocq.mansart.persistence;

/**
 * Utility class for Mansart Persistence API extensions.
 * 
 * <p>This class will be expanded with useful extension methods
 * as the implementation matures.
 */
public final class PersistenceExtensions {
    
    /**
     * The current Mansart Persistence implementation version.
     */
    public static final String VERSION = "0.3.0-SNAPSHOT";
    
    /**
     * The Jakarta Persistence specification version implemented.
     */
    public static final String JPA_SPEC_VERSION = "3.2";
    
    /**
     * Private constructor to prevent instantiation.
     */
    private PersistenceExtensions() {
    }
    
    /**
     * Returns the implementation version.
     * 
     * @return the current implementation version
     */
    public static String getVersion() {
        return VERSION;
    }
    
    /**
     * Returns the JPA specification version.
     * 
     * @return the JPA specification version
     */
    public static String getJpaSpecVersion() {
        return JPA_SPEC_VERSION;
    }
}

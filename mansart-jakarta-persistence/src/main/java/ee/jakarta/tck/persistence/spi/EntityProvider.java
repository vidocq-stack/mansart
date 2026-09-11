package ee.jakarta.tck.persistence.spi;

import java.nio.file.Path;
import java.util.Set;

/**
 * Provider interface for entity metadata.
 * Implemented by the persistence provider to make entity information available to the persistence context.
 */
public interface EntityProvider {
    
    /**
     * Initializes the entity provider with the classpath to scan.
     * @param classPath the classpath to scan for entities
     */
    void initialize(Path classPath);
    
    /**
     * Returns metadata for the specified entity class.
     * @param entityClass the fully qualified entity class name
     * @return the entity metadata, or null if not found
     */
    EntityMetadata getEntityMetadata(String entityClass);
    
    /**
     * Returns all registered entity metadata.
     */
    Set<EntityMetadata> getAllEntities();
    
    /**
     * Checks if the specified class is a registered entity.
     */
    boolean isEntityClass(String entityClass);
    
    /**
     * Returns the number of registered entities.
     */
    int getEntityCount();
}
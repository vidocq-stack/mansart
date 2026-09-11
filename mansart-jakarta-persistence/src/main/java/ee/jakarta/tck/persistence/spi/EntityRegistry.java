package ee.jakarta.tck.persistence.spi;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Registry for discovered entity classes and their metadata.
 * This is the central place where entities are registered and managed.
 */
public class EntityRegistry {
    
    private final Map<String, EntityMetadata> entities = new HashMap<>();
    private final EntityScanner scanner;
    
    public EntityRegistry(EntityScanner scanner) {
        this.scanner = scanner;
    }
    
    /**
     * Scans the classpath and registers all discovered entities.
     * @param classPath the classpath to scan
     * @throws Exception if scanning fails
     */
    public void registerEntities(Path classPath) throws Exception {
        Set<String> entityClasses = scanner.scan(classPath);
        
        for (String entityClass : entityClasses) {
            // Create metadata for each entity
            EntityMetadata metadata = new EntityMetadata(entityClass);
            entities.put(entityClass, metadata);
        }
    }
    
    /**
     * Returns metadata for the specified entity class.
     * @param entityClass the fully qualified entity class name
     * @return the entity metadata, or null if not found
     */
    public EntityMetadata getEntityMetadata(String entityClass) {
        return entities.get(entityClass);
    }
    
    /**
     * Returns all registered entity metadata.
     */
    public Set<EntityMetadata> getAllEntities() {
        return Set.copyOf(entities.values());
    }
    
    /**
     * Returns the number of registered entities.
     */
    public int getEntityCount() {
        return entities.size();
    }
    
    /**
     * Checks if the specified class is a registered entity.
     */
    public boolean isEntityClass(String entityClass) {
        return entities.containsKey(entityClass);
    }
}
package ee.jakarta.tck.persistence.spi;

import java.nio.file.Path;
import java.util.Set;

/**
 * Default implementation of the EntityProvider interface.
 * Uses EntityScanner and EntityRegistry to discover and manage entities.
 */
public class DefaultEntityProvider implements EntityProvider {
    
    private final EntityScanner scanner;
    private final EntityRegistry registry;
    private boolean initialized = false;
    
    public DefaultEntityProvider() {
        this.scanner = new EntityScanner();
        this.registry = new EntityRegistry(scanner);
    }
    
    @Override
    public void initialize(Path classPath) {
        try {
            registry.registerEntities(classPath);
            initialized = true;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to initialize entity provider", e);
        }
    }
    
    @Override
    public EntityMetadata getEntityMetadata(String entityClass) {
        if (!initialized) {
            throw new IllegalStateException("EntityProvider not initialized");
        }
        return registry.getEntityMetadata(entityClass);
    }
    
    @Override
    public Set<EntityMetadata> getAllEntities() {
        if (!initialized) {
            throw new IllegalStateException("EntityProvider not initialized");
        }
        return registry.getAllEntities();
    }
    
    @Override
    public boolean isEntityClass(String entityClass) {
        if (!initialized) {
            throw new IllegalStateException("EntityProvider not initialized");
        }
        return registry.isEntityClass(entityClass);
    }
    
    @Override
    public int getEntityCount() {
        if (!initialized) {
            throw new IllegalStateException("EntityProvider not initialized");
        }
        return registry.getEntityCount();
    }
}
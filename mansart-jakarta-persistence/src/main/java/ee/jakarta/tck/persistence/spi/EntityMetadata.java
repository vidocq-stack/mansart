package ee.jakarta.tck.persistence.spi;

import java.util.Set;

/**
 * Metadata about an entity class.
 * Captures essential information for persistence operations.
 */
public class EntityMetadata {
    
    private final String entityClass;
    private final Set<String> persistentFields;
    private final Set<String> persistentProperties;
    private final boolean hasNoArgConstructor;
    private final boolean isNonFinal;
    private final Set<String> relationships;
    
    public EntityMetadata(String entityClass) {
        this.entityClass = entityClass;
        this.persistentFields = Set.of(); // Will be populated by scanning
        this.persistentProperties = Set.of(); // Will be populated by scanning
        this.hasNoArgConstructor = true; // Verified by scanner
        this.isNonFinal = true; // Verified by scanner
        this.relationships = Set.of(); // Will be populated by scanning relationships
    }
    
    public String getEntityClass() {
        return entityClass;
    }
    
    public Set<String> getPersistentFields() {
        return persistentFields;
    }
    
    public Set<String> getPersistentProperties() {
        return persistentProperties;
    }
    
    public boolean hasNoArgConstructor() {
        return hasNoArgConstructor;
    }
    
    public boolean isNonFinal() {
        return isNonFinal;
    }
    
    public Set<String> getRelationships() {
        return relationships;
    }
    
    @Override
    public String toString() {
        return "EntityMetadata{" +
                "entityClass='" + entityClass + '\'' +
                ", persistentFields=" + persistentFields +
                ", persistentProperties=" + persistentProperties +
                ", hasNoArgConstructor=" + hasNoArgConstructor +
                ", isNonFinal=" + isNonFinal +
                ", relationships=" + relationships +
                '}';
    }
}
package io.vidocq.mansart.persistence;

import jakarta.persistence.Entity;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * M1-T002: Persistent fields and properties visibility
 * Tests that:
 * 1. Persistent fields must be private, protected, or package-private — public fields forbidden.
 * 2. Property accessor methods (getters/setters) must be public or protected.
 * 3. Collection-valued fields/properties must use java.util Collection, Set, List, or Map interfaces.
 */
class EntityClassValidatorM1T002Test {

    // --- Test entities ---

    @Entity
    static class EntityWithPublicField {
        @SuppressWarnings("unused")
        public String publicField;
        @SuppressWarnings("unused")
        private String privateField;
    }

    @Entity
    static class EntityWithPackagePrivateAccessor {
        @SuppressWarnings("unused")
        private String name;
        // package-private setter — should be rejected
        void setName(String name) { this.name = name; }
        // package-private getter — should be rejected
        String getName() { return name; }
    }

    @Entity
    static class EntityWithConcreteCollection {
        @SuppressWarnings("unused")
        private ArrayList<String> items = new ArrayList<>();
    }

    // --- Tests ---

    /**
     * M1-T002: Public fields on entities are not allowed.
     */
    @Test
    void testPersistentFieldsMustNotBePublic() {
        assertThrows(IllegalArgumentException.class,
            () -> EntityClassValidator.validate(EntityWithPublicField.class),
            "EntityClassValidator should reject public fields on entities");
    }

    /**
     * M1-T002: Property accessor methods must be public or protected.
     */
    @Test
    void testAccessorMethodsMustBePublicOrProtected() {
        assertThrows(IllegalArgumentException.class,
            () -> EntityClassValidator.validate(EntityWithPackagePrivateAccessor.class),
            "EntityClassValidator should reject package-private accessors");
    }

    /**
     * M1-T002: Collection-valued fields must use interface types.
     */
    @Test
    void testCollectionValuedFieldsMustUseInterfaces() {
        assertThrows(IllegalArgumentException.class,
            () -> EntityClassValidator.validate(EntityWithConcreteCollection.class),
            "EntityClassValidator should reject concrete collection types");
    }
}

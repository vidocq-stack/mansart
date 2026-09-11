package io.vidocq.mansart.persistence;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Modifier;

import static org.junit.jupiter.api.Assertions.*;

/**
 * M1-T001 — Entity class requirements (spec 2.1).
 *
 * An entity class must:
 *   - be annotated with @Entity or declared in XML
 *   - be a top-level or static inner class
 *   - have a public or protected no-arg constructor
 *   - be non-final
 *   - have all methods and persistent fields be non-final
 */
class M1T001EntityClassRequirementsTest {

    /**
     * A final entity class must fail entity-class validation.
     */
    @Test
    void finalEntityClassMustFail() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> EntityClassValidator.validate(FinalEntity.class)
        );
        assertTrue(ex.getMessage().contains("final"),
                "Expected 'final' in: " + ex.getMessage());
    }

    /**
     * An entity without a no-arg constructor must fail validation.
     */
    @Test
    void noNoArgConstructorMustFail() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> EntityClassValidator.validate(NoArgEntity.class)
        );
        assertTrue(ex.getMessage().contains("no-arg") || ex.getMessage().contains("constructor"),
                "Expected 'no-arg' or 'constructor' in: " + ex.getMessage());
    }

    /**
     * An entity with a final field must fail validation.
     */
    @Test
    void entityWithFinalFieldMustFail() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> EntityClassValidator.validate(EntityWithFinalField.class)
        );
        assertTrue(ex.getMessage().contains("final"),
                "Expected 'final' in: " + ex.getMessage());
    }

    /**
     * A valid entity class (non-final, no-arg constructor, non-final fields)
     * must pass validation.
     */
    @Test
    void validEntityClassMustPass() {
        // Should not throw
        EntityClassValidator.validate(ValidEntity.class);
    }

    // --- test fixtures ---

    @jakarta.persistence.Entity
    private static final class FinalEntity {
        @jakarta.persistence.Id
        private int id;
    }

    @jakarta.persistence.Entity
    private static class NoArgEntity {
        @jakarta.persistence.Id
        private int id = 0;
        
        // Only a parameterized constructor
        public NoArgEntity(int id) { this.id = id; }
        // NO default constructor — intentional for test
    }

    @jakarta.persistence.Entity
    private static class EntityWithFinalField {
        @jakarta.persistence.Id
        private final int id = 0;
        // NO default constructor — intentional for test
    }

    @jakarta.persistence.Entity
    static class ValidEntity {
        @jakarta.persistence.Id
        private int id;
        private String name;
        
        // Public no-arg constructor required by JPA spec
        public ValidEntity() {}
    }
}

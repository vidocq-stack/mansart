/*
 * SPDX-License-Identifier: EPL-2.0 OR EUPL-1.2 OR GPL-2.0-or-later
 */
package io.vidocq.mansart.persistence.tests;

import io.vidocq.mansart.persistence.core.runtime.MansartCallback;
import io.vidocq.mansart.persistence.spi.EntityAccessor;
import io.vidocq.mansart.persistence.spi.EntityModel;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for M3-JP-21: MansartCallback dispatch mechanism for hidden classes.
 */
public class MansartCallbackTest {

    @Test
    void testGetAccessorForEntity() {
        MansartCallback callback = new MansartCallback();

        EntityAccessor<DispatchEntity> accessor = callback.getAccessor(DispatchEntity.class);

        DispatchEntity entity = new DispatchEntity();
        entity.setId(1L);
        entity.setName("test");

        assertThat(accessor.get(entity, "id")).isEqualTo(1L);
        assertThat(accessor.get(entity, "name")).isEqualTo("test");
    }

    @Test
    void testGetEntityModelForEntity() {
        MansartCallback callback = new MansartCallback();

        EntityModel<DispatchEntity> model = callback.getEntityModel(DispatchEntity.class);

        assertThat(model).isNotNull();
        assertThat(model.getEntityClass()).isEqualTo(DispatchEntity.class);
        assertThat(model.getTableName()).isEqualTo("dispatch_entities");
    }

    @Test
    void testAccessorIsCached() {
        MansartCallback callback = new MansartCallback();

        EntityAccessor<DispatchEntity> accessor1 = callback.getAccessor(DispatchEntity.class);
        EntityAccessor<DispatchEntity> accessor2 = callback.getAccessor(DispatchEntity.class);

        assertThat(accessor1).isSameAs(accessor2);
    }

    @Test
    void testEntityModelIsCached() {
        MansartCallback callback = new MansartCallback();

        EntityModel<DispatchEntity> model1 = callback.getEntityModel(DispatchEntity.class);
        EntityModel<DispatchEntity> model2 = callback.getEntityModel(DispatchEntity.class);

        assertThat(model1).isSameAs(model2);
    }

    @Test
    void testGetWarnings() {
        MansartCallback callback = new MansartCallback();
        callback.getEntityModel(DispatchEntity.class);

        assertThat(callback.getWarnings()).hasSize(1);
        assertThat(callback.getWarnings().get(0)).contains("DispatchEntity");
    }

    @jakarta.persistence.Entity
    @jakarta.persistence.Table(name = "dispatch_entities")
    public static class DispatchEntity {
        @jakarta.persistence.Id
        private Long id;
        private String name;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }
}

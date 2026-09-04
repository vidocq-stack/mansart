/*
 * SPDX-License-Identifier: EPL-2.0 OR EUPL-1.2 OR GPL-2.0-or-later
 */
package io.vidocq.mansart.persistence.tests;

import io.vidocq.mansart.persistence.core.runtime.RuntimeEntityModelBuilder;
import io.vidocq.mansart.persistence.core.runtime.Tier3WarningCollector;
import io.vidocq.mansart.persistence.spi.EntityModel;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for M3-JP-22: Warning mechanism pointing to Maven plugin for tier-3 entities.
 */
public class Tier3WarningTest {

    @Test
    public void testWarningCollectedWhenBuildingTier3Model() {
        Tier3WarningCollector collector = new Tier3WarningCollector();
        RuntimeEntityModelBuilder builder = new RuntimeEntityModelBuilder(collector);

        EntityModel<UnprocessedEntity> model = builder.build(UnprocessedEntity.class);

        assertThat(model).isNotNull();
        List<String> warnings = collector.getWarnings();
        assertThat(warnings).hasSize(1);
        assertThat(warnings.get(0)).contains("UnprocessedEntity");
        assertThat(warnings.get(0)).contains("Maven plugin");
        assertThat(warnings.get(0)).contains("mansart-persistence-maven-plugin");
    }

    @Test
    public void testWarningCollectorAccumulatesMultipleEntities() {
        Tier3WarningCollector collector = new Tier3WarningCollector();
        RuntimeEntityModelBuilder builder = new RuntimeEntityModelBuilder(collector);

        builder.build(UnprocessedEntity.class);
        builder.build(SecondEntity.class);

        List<String> warnings = collector.getWarnings();
        assertThat(warnings).hasSize(2);
        assertThat(warnings.get(0)).contains("UnprocessedEntity");
        assertThat(warnings.get(1)).contains("SecondEntity");
    }

    @Test
    public void testWarningCollectorEmptyByDefault() {
        Tier3WarningCollector collector = new Tier3WarningCollector();
        assertThat(collector.getWarnings()).isEmpty();
    }

    @jakarta.persistence.Entity
    @jakarta.persistence.Table(name = "unprocessed_entities")
    public static class UnprocessedEntity {
        @jakarta.persistence.Id
        private Long id;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
    }

    @jakarta.persistence.Entity
    public static class SecondEntity {
        @jakarta.persistence.Id
        private Long id;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
    }
}

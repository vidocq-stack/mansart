/*
 * SPDX-License-Identifier: EPL-2.0 OR EUPL-1.2 OR GPL-2.0-or-later
 */
package io.vidocq.mansart.persistence.core.runtime;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Collects warnings for Tier 3 entities — entities that were not processed by
 * APT (Tier 1) or the Maven plugin (Tier 2) and fell back to runtime parsing.
 * <p>
 * Each warning names the entity class and points to the Maven plugin as the
 * recommended fix for better performance.
 */
public final class Tier3WarningCollector {

    private static final Logger LOG = Logger.getLogger(Tier3WarningCollector.class.getName());

    private final List<String> warnings = new ArrayList<>();

    /**
     * Records a tier-3 warning for the given entity class name.
     *
     * @param entityClassName the fully qualified name of the entity that fell back to tier-3
     */
    void warnTier3(String entityClassName) {
        String message = "Entity " + entityClassName + " fell back to Tier 3 (runtime Class-File API). "
                + "Add the Maven plugin (mansart-persistence-maven-plugin) to the project's build "
                + "to generate metamodel at build time for better startup performance.";
        warnings.add(message);
        LOG.warning(message);
    }

    /**
     * Returns the list of accumulated warnings.
     *
     * @return unmodifiable list of warning messages
     */
    public List<String> getWarnings() {
        return List.copyOf(warnings);
    }
}

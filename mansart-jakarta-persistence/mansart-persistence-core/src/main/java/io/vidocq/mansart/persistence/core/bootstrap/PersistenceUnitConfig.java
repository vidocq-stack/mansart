/*
 * Copyright (c) 2026 Yann Blazart, Antoine Sabot-Durand and the Vidocq contributors
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0 OR GPL-2.0-or-later
 */

package io.vidocq.mansart.persistence.core.bootstrap;

import java.util.*;

/**
 * Simple configuration extracted from persistence.xml for a persistence unit.
 * This is a simplified representation that captures the essential configuration
 * needed by Mansart, without implementing the full PersistenceUnitInfo interface.
 */
public final class PersistenceUnitConfig {

    private final String name;
    private final Map<String, Object> properties;
    private final List<String> classNames;

    private PersistenceUnitConfig(Builder builder) {
        this.name = builder.name;
        this.properties = Collections.unmodifiableMap(new HashMap<>(builder.properties));
        this.classNames = Collections.unmodifiableList(new ArrayList<>(builder.classNames));
    }

    public String getName() {
        return name;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    public List<String> getClassNames() {
        return classNames;
    }

    /**
     * Loads and returns the entity classes from the class names.
     *
     * @return list of loaded entity classes
     */
    public List<Class<?>> getEntityClasses() {
        List<Class<?>> classes = new ArrayList<>();
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        
        for (String className : classNames) {
            try {
                Class<?> clazz = Class.forName(className, true, classLoader);
                classes.add(clazz);
            } catch (ClassNotFoundException e) {
                // Log warning and continue
                System.Logger logger = System.getLogger(PersistenceUnitConfig.class.getName());
                logger.log(System.Logger.Level.WARNING, "Could not load entity class: " + className);
            }
        }
        
        return classes;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String name;
        private final Map<String, Object> properties = new HashMap<>();
        private final List<String> classNames = new ArrayList<>();

        public Builder withName(String name) {
            this.name = name;
            return this;
        }

        public Builder addProperty(String key, Object value) {
            this.properties.put(key, value);
            return this;
        }

        public Builder addClassName(String className) {
            this.classNames.add(className);
            return this;
        }

        public PersistenceUnitConfig build() {
            return new PersistenceUnitConfig(this);
        }
    }
}

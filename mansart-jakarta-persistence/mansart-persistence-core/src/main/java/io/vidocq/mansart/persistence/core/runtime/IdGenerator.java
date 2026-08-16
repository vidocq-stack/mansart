/*
 * Copyright (c) 2026 Yann Blazart, Antoine Sabot-Durand and the Vidocq contributors
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0 OR EUPL-1.2 OR GPL-2.0-or-later
 */
package io.vidocq.mansart.persistence.core.runtime;

import jakarta.persistence.GenerationType;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * ID generator for JPA entities (M9-10 - TCK error reduction).
 * 
 * <p>This class provides ID generation strategies for different JPA
 * {@link GenerationType} values:
 * <ul>
 *   <li>AUTO: Uses a global counter</li>
 *   <li>IDENTITY: Delegates to the database (in-memory: uses global counter)</li>
 *   <li>SEQUENCE: Uses per-sequence counters</li>
 *   <li>TABLE: Uses per-table sequence simulation</li>
 * </ul>
 */
public class IdGenerator {

    /**
     * Global counter for AUTO and IDENTITY strategies.
     */
    private static final AtomicLong GLOBAL_COUNTER = new AtomicLong(1);

    /**
     * Sequence counters: sequence name -> counter.
     */
    private static final Map<String, AtomicLong> SEQUENCE_COUNTERS = new ConcurrentHashMap<>();

    /**
     * Table sequence counters: table name -> counter.
     */
    private static final Map<String, AtomicLong> TABLE_COUNTERS = new ConcurrentHashMap<>();

    /**
     * Private constructor - utility class.
     */
    private IdGenerator() {
    }

    /**
     * Generates an ID based on the specified generation type and generator name.
     * 
     * @param generationType the JPA generation type
     * @param generatorName the generator name (may be null for AUTO/IDENTITY)
     * @return the generated ID
     */
    public static Long generateId(GenerationType generationType, String generatorName) {
        switch (generationType) {
            case AUTO:
            case IDENTITY:
                return GLOBAL_COUNTER.getAndIncrement();

            case SEQUENCE:
                if (generatorName == null || generatorName.isEmpty()) {
                    return GLOBAL_COUNTER.getAndIncrement();
                }
                return SEQUENCE_COUNTERS.computeIfAbsent(generatorName, k -> new AtomicLong(1)).getAndIncrement();

            case TABLE:
                if (generatorName == null || generatorName.isEmpty()) {
                    return GLOBAL_COUNTER.getAndIncrement();
                }
                return TABLE_COUNTERS.computeIfAbsent(generatorName, k -> new AtomicLong(1)).getAndIncrement();

            default:
                // Fallback to AUTO
                return GLOBAL_COUNTER.getAndIncrement();
        }
    }

    /**
     * Resets all counters. Useful for testing.
     */
    public static void reset() {
        GLOBAL_COUNTER.set(1);
        SEQUENCE_COUNTERS.clear();
        TABLE_COUNTERS.clear();
    }

    /**
     * Gets the current value of the global counter (for testing).
     * 
     * @return the current global counter value
     */
    public static long getGlobalCounter() {
        return GLOBAL_COUNTER.get();
    }
}

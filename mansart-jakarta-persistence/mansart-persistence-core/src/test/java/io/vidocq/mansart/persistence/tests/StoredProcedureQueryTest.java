/*
 * Copyright (c) 2026 Vidocq contributors
 * SPDX-License-Identifier: EPL-2.0 OR EUPL-1.2 OR GPL-2.0-or-later
 */
package io.vidocq.mansart.persistence.tests;

import io.vidocq.mansart.data.dialect.Dialect;
import io.vidocq.mansart.data.dialect.EntityModel;
import io.vidocq.mansart.persistence.core.runtime.MansartParameter;
import io.vidocq.mansart.persistence.core.runtime.MansartStoredProcedureQuery;
import io.vidocq.mansart.persistence.core.jpql.JpqlExecutor;
import jakarta.persistence.Parameter;
import jakarta.persistence.ParameterMode;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test for StoredProcedureQuery parameter handling.
 * M9-10: Verify getParameterValue throws IllegalArgumentException for unregistered parameters.
 */
public class StoredProcedureQueryTest {

    private final Dialect dialect = null;
    private final JpqlExecutor.ConnectionProvider connectionProvider = null;
    private final Map<Class<?>, EntityModel<?>> entityModels = Collections.emptyMap();
    private final Map<String, Class<?>> entityClasses = Collections.emptyMap();

    @Test
    public void testGetParameterValueThrowsIAEForUnregisteredParameterByName() {
        MansartStoredProcedureQuery spq = new MansartStoredProcedureQuery(
            "testProc", dialect, connectionProvider, entityModels, entityClasses);
        
        // Register a parameter
        spq.registerStoredProcedureParameter("param1", String.class, ParameterMode.IN);
        
        // Create a Parameter object for an unregistered parameter
        Parameter<String> unregisteredParam = new MansartParameter<>("param2", String.class);
        
        // Should throw IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () -> {
            spq.getParameterValue(unregisteredParam);
        });
    }

    @Test
    public void testGetParameterValueThrowsIAEForUnregisteredParameterByPosition() {
        MansartStoredProcedureQuery spq = new MansartStoredProcedureQuery(
            "testProc", dialect, connectionProvider, entityModels, entityClasses);
        
        // Register a parameter at position 1
        spq.registerStoredProcedureParameter(1, String.class, ParameterMode.IN);
        
        // Create a Parameter object for position 2 (unregistered)
        Parameter<String> unregisteredParam = new MansartParameter<>(2, String.class);
        
        // Should throw IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () -> {
            spq.getParameterValue(unregisteredParam);
        });
    }

    @Test
    public void testGetParameterValueIntThrowsIAEForUnregisteredParameter() {
        MansartStoredProcedureQuery spq = new MansartStoredProcedureQuery(
            "testProc", dialect, connectionProvider, entityModels, entityClasses);
        
        // Register a parameter at position 1
        spq.registerStoredProcedureParameter(1, String.class, ParameterMode.IN);
        
        // Try to get value of unregistered position 2
        assertThrows(IllegalArgumentException.class, () -> {
            spq.getParameterValue(2);
        });
    }

    @Test
    public void testGetParameterValueStringThrowsIAEForUnregisteredParameter() {
        MansartStoredProcedureQuery spq = new MansartStoredProcedureQuery(
            "testProc", dialect, connectionProvider, entityModels, entityClasses);
        
        // Register a parameter
        spq.registerStoredProcedureParameter("param1", String.class, ParameterMode.IN);
        
        // Try to get value of unregistered parameter
        assertThrows(IllegalArgumentException.class, () -> {
            spq.getParameterValue("param2");
        });
    }

    @Test
    public void testGetParameterValueThrowsIAEForNullParameter() {
        MansartStoredProcedureQuery spq = new MansartStoredProcedureQuery(
            "testProc", dialect, connectionProvider, entityModels, entityClasses);
        
        assertThrows(IllegalArgumentException.class, () -> {
            spq.getParameterValue((Parameter<?>) null);
        });
    }

    @Test
    public void testGetParameterValueStringThrowsIAEForNullName() {
        MansartStoredProcedureQuery spq = new MansartStoredProcedureQuery(
            "testProc", dialect, connectionProvider, entityModels, entityClasses);
        
        assertThrows(IllegalArgumentException.class, () -> {
            spq.getParameterValue((String) null);
        });
    }
}

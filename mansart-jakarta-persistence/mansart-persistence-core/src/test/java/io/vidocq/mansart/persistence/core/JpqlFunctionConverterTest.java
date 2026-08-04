/*
 * Copyright (c) 2026 Yann Blazart, Antoine Sabot-Durand and the Vidocq contributors
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * This Source Code may also be made available under the following Secondary
 * Licenses when the conditions for such availability set forth in the Eclipse
 * Public License, v. 2.0 are satisfied: GNU General Public License, version 2
 * or any later version, which is available at
 * https://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 *
 * It is also made available under the European Union Public Licence v. 1.2,
 * which is available at
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-1.2
 *
 * SPDX-License-Identifier: EPL-2.0 OR EUPL-1.2 OR GPL-2.0-or-later
 */
package io.vidocq.mansart.persistence.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for JpqlFunctionConverter.
 * M6 — JPQL function support tests.
 */
class JpqlFunctionConverterTest {

    @Test
    void testIsKnownStringFunction() {
        assertTrue(JpqlFunctionConverter.isKnownFunction("UPPER"));
        assertTrue(JpqlFunctionConverter.isKnownFunction("upper"));
        assertTrue(JpqlFunctionConverter.isKnownFunction("LOWER"));
        assertTrue(JpqlFunctionConverter.isKnownFunction("TRIM"));
        assertTrue(JpqlFunctionConverter.isKnownFunction("CONCAT"));
        assertTrue(JpqlFunctionConverter.isKnownFunction("SUBSTRING"));
        assertTrue(JpqlFunctionConverter.isKnownFunction("LENGTH"));
    }

    @Test
    void testIsKnownNumericFunction() {
        assertTrue(JpqlFunctionConverter.isKnownFunction("ABS"));
        assertTrue(JpqlFunctionConverter.isKnownFunction("abs"));
        assertTrue(JpqlFunctionConverter.isKnownFunction("MOD"));
        assertTrue(JpqlFunctionConverter.isKnownFunction("SQRT"));
        assertTrue(JpqlFunctionConverter.isKnownFunction("SIZE"));
    }

    @Test
    void testIsKnownDateFunction() {
        assertTrue(JpqlFunctionConverter.isKnownFunction("CURRENT_DATE"));
        assertTrue(JpqlFunctionConverter.isKnownFunction("CURRENT_TIME"));
        assertTrue(JpqlFunctionConverter.isKnownFunction("CURRENT_TIMESTAMP"));
        assertTrue(JpqlFunctionConverter.isKnownFunction("LOCAL_DATE"));
        assertTrue(JpqlFunctionConverter.isKnownFunction("LOCAL_TIME"));
        assertTrue(JpqlFunctionConverter.isKnownFunction("LOCAL_DATETIME"));
    }

    @Test
    void testIsKnownAggregateFunction() {
        assertTrue(JpqlFunctionConverter.isAggregateFunction("AVG"));
        assertTrue(JpqlFunctionConverter.isAggregateFunction("COUNT"));
        assertTrue(JpqlFunctionConverter.isAggregateFunction("MAX"));
        assertTrue(JpqlFunctionConverter.isAggregateFunction("MIN"));
        assertTrue(JpqlFunctionConverter.isAggregateFunction("SUM"));
    }

    @Test
    void testIsUnknownFunction() {
        assertFalse(JpqlFunctionConverter.isKnownFunction("UNKNOWN_FUNCTION"));
        assertFalse(JpqlFunctionConverter.isKnownFunction("FOOBAR"));
        assertFalse(JpqlFunctionConverter.isKnownFunction(""));
        assertFalse(JpqlFunctionConverter.isKnownFunction(null));
    }

    @Test
    void testIsStringFunction() {
        assertTrue(JpqlFunctionConverter.isStringFunction("UPPER"));
        assertTrue(JpqlFunctionConverter.isStringFunction("LOWER"));
        assertTrue(JpqlFunctionConverter.isStringFunction("TRIM"));
        assertFalse(JpqlFunctionConverter.isStringFunction("ABS"));
        assertFalse(JpqlFunctionConverter.isStringFunction("COUNT"));
    }

    @Test
    void testIsNumericFunction() {
        assertTrue(JpqlFunctionConverter.isNumericFunction("ABS"));
        assertTrue(JpqlFunctionConverter.isNumericFunction("MOD"));
        assertTrue(JpqlFunctionConverter.isNumericFunction("SQRT"));
        assertFalse(JpqlFunctionConverter.isNumericFunction("UPPER"));
        assertFalse(JpqlFunctionConverter.isNumericFunction("COUNT"));
    }

    @Test
    void testIsDateFunction() {
        assertTrue(JpqlFunctionConverter.isDateFunction("CURRENT_DATE"));
        assertTrue(JpqlFunctionConverter.isDateFunction("CURRENT_TIME"));
        assertFalse(JpqlFunctionConverter.isDateFunction("UPPER"));
        assertFalse(JpqlFunctionConverter.isDateFunction("ABS"));
    }

    @Test
    void testFunctionCategoryMutualExclusion() {
        // A function should belong to only one category
        String[] functions = {"UPPER", "ABS", "CURRENT_DATE", "COUNT", "CONCAT", "LOCAL_DATE"};
        
        for (String func : functions) {
            int count = 0;
            if (JpqlFunctionConverter.isStringFunction(func)) count++;
            if (JpqlFunctionConverter.isNumericFunction(func)) count++;
            if (JpqlFunctionConverter.isDateFunction(func)) count++;
            if (JpqlFunctionConverter.isAggregateFunction(func)) count++;
            
            assertEquals(1, count, "Function " + func + " should belong to exactly one category");
        }
    }
}

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

import io.vidocq.mansart.data.query.ast.JpqlParser;
import io.vidocq.mansart.data.query.ast.JpqlStmt;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ParameterExtractor.
 * M6 — Parameter binding tests.
 */
class ParameterExtractorTest {

    @Test
    void testExtractNamedParameterFromWhere() {
        JpqlStmt stmt = JpqlParser.parse("SELECT b FROM Book b WHERE b.title = :title");
        
        Map<String, Object> namedParams = new HashMap<>();
        namedParams.put("title", "Test Book");
        
        Object[] args = ParameterExtractor.extractParameters(stmt, namedParams, Map.of());
        
        assertNotNull(args);
        assertEquals(1, args.length);
        assertEquals("Test Book", args[0]);
    }

    @Test
    void testExtractMultipleNamedParameters() {
        JpqlStmt stmt = JpqlParser.parse(
            "SELECT b FROM Book b WHERE b.title = :title AND b.price > :price");
        
        Map<String, Object> namedParams = new HashMap<>();
        namedParams.put("title", "Test Book");
        namedParams.put("price", 100);
        
        Object[] args = ParameterExtractor.extractParameters(stmt, namedParams, Map.of());
        
        assertNotNull(args);
        assertEquals(2, args.length);
        assertEquals("Test Book", args[0]);
        assertEquals(100, args[1]);
    }

    @Test
    void testExtractPositionalParameter() {
        JpqlStmt stmt = JpqlParser.parse("SELECT b FROM Book b WHERE b.title = ?1");
        
        Map<Integer, Object> positionalParams = new HashMap<>();
        positionalParams.put(1, "Test Book");
        
        Object[] args = ParameterExtractor.extractParameters(stmt, Map.of(), positionalParams);
        
        assertNotNull(args);
        assertEquals(1, args.length);
        assertEquals("Test Book", args[0]);
    }

    @Test
    void testExtractMultiplePositionalParameters() {
        JpqlStmt stmt = JpqlParser.parse(
            "SELECT b FROM Book b WHERE b.title = ?1 AND b.price > ?2");
        
        Map<Integer, Object> positionalParams = new HashMap<>();
        positionalParams.put(1, "Test Book");
        positionalParams.put(2, 200);
        
        Object[] args = ParameterExtractor.extractParameters(stmt, Map.of(), positionalParams);
        
        assertNotNull(args);
        assertEquals(2, args.length);
        assertEquals("Test Book", args[0]);
        assertEquals(200, args[1]);
    }

    @Test
    void testExtractMixedParameters() {
        JpqlStmt stmt = JpqlParser.parse(
            "SELECT b FROM Book b WHERE b.title = :title AND b.price > ?1");
        
        Map<String, Object> namedParams = new HashMap<>();
        namedParams.put("title", "Test Book");
        Map<Integer, Object> positionalParams = new HashMap<>();
        positionalParams.put(1, 150);
        
        Object[] args = ParameterExtractor.extractParameters(stmt, namedParams, positionalParams);
        
        assertNotNull(args);
        assertEquals(2, args.length);
        assertEquals("Test Book", args[0]);
        assertEquals(150, args[1]);
    }

    @Test
    void testExtractParametersFromInPredicate() {
        JpqlStmt stmt = JpqlParser.parse(
            "SELECT b FROM Book b WHERE b.category IN (:cat1, :cat2, :cat3)");
        
        Map<String, Object> namedParams = new HashMap<>();
        namedParams.put("cat1", "Fiction");
        namedParams.put("cat2", "Sci-Fi");
        namedParams.put("cat3", "Mystery");
        
        Object[] args = ParameterExtractor.extractParameters(stmt, namedParams, Map.of());
        
        assertNotNull(args);
        assertEquals(3, args.length);
        assertEquals("Fiction", args[0]);
        assertEquals("Sci-Fi", args[1]);
        assertEquals("Mystery", args[2]);
    }

    @Test
    void testExtractParametersFromBetween() {
        JpqlStmt stmt = JpqlParser.parse(
            "SELECT b FROM Book b WHERE b.price BETWEEN :min AND :max");
        
        Map<String, Object> namedParams = new HashMap<>();
        namedParams.put("min", 100);
        namedParams.put("max", 200);
        
        Object[] args = ParameterExtractor.extractParameters(stmt, namedParams, Map.of());
        
        assertNotNull(args);
        assertEquals(2, args.length);
        assertEquals(100, args[0]);
        assertEquals(200, args[1]);
    }

    @Test
    void testExtractParametersFromLike() {
        JpqlStmt stmt = JpqlParser.parse(
            "SELECT b FROM Book b WHERE b.title LIKE :pattern");
        
        Map<String, Object> namedParams = new HashMap<>();
        namedParams.put("pattern", "%Test%");
        
        Object[] args = ParameterExtractor.extractParameters(stmt, namedParams, Map.of());
        
        assertNotNull(args);
        assertEquals(1, args.length);
        assertEquals("%Test%", args[0]);
    }

    @Test
    void testExtractParametersFromOrderBy() {
        JpqlStmt stmt = JpqlParser.parse(
            "SELECT b FROM Book b WHERE b.category = :cat ORDER BY b.price DESC");
        
        Map<String, Object> namedParams = new HashMap<>();
        namedParams.put("cat", "Fiction");
        
        Object[] args = ParameterExtractor.extractParameters(stmt, namedParams, Map.of());
        
        assertNotNull(args);
        assertEquals(1, args.length);
        assertEquals("Fiction", args[0]);
    }

    @Test
    void testExtractParametersFromSelectClause() {
        JpqlStmt stmt = JpqlParser.parse(
            "SELECT b.title, b.price FROM Book b WHERE b.id = :id");
        
        Map<String, Object> namedParams = new HashMap<>();
        namedParams.put("id", 1L);
        
        Object[] args = ParameterExtractor.extractParameters(stmt, namedParams, Map.of());
        
        assertNotNull(args);
        assertEquals(1, args.length);
        assertEquals(1L, args[0]);
    }

    @Test
    void testConvertPositionalToNamed() {
        Map<Integer, Object> positionalParams = new HashMap<>();
        positionalParams.put(1, "value1");
        positionalParams.put(2, "value2");
        
        Map<String, Object> namedParams = ParameterExtractor.convertPositionalToNamed(positionalParams);
        
        assertNotNull(namedParams);
        assertEquals(2, namedParams.size());
        assertEquals("value1", namedParams.get("__pos_1"));
        assertEquals("value2", namedParams.get("__pos_2"));
    }

    @Test
    void testMergeParameters() {
        Map<String, Object> namedParams = Map.of("name", "Test");
        Map<Integer, Object> positionalParams = Map.of(1, 100);
        
        Map<String, Object> merged = ParameterExtractor.mergeParameters(namedParams, positionalParams);
        
        assertNotNull(merged);
        assertEquals(2, merged.size());
        assertEquals("Test", merged.get("name"));
        assertEquals(100, merged.get("__pos_1"));
    }

    @Test
    void testExtractParametersFromExistsSubquery() {
        JpqlStmt stmt = JpqlParser.parse(
            "SELECT b FROM Book b WHERE EXISTS (SELECT 1 FROM TestEntity t WHERE t.id = :id)");
        
        Map<String, Object> namedParams = new HashMap<>();
        namedParams.put("id", 1L);
        
        Object[] args = ParameterExtractor.extractParameters(stmt, namedParams, Map.of());
        
        assertNotNull(args);
        assertEquals(1, args.length);
        assertEquals(1L, args[0]);
    }

    @Test
    void testExtractParametersFromNotExistsSubquery() {
        JpqlStmt stmt = JpqlParser.parse(
            "SELECT b FROM Book b WHERE NOT EXISTS (SELECT 1 FROM TestEntity t WHERE t.id = :id)");
        
        Map<String, Object> namedParams = new HashMap<>();
        namedParams.put("id", 5L);
        
        Object[] args = ParameterExtractor.extractParameters(stmt, namedParams, Map.of());
        
        assertNotNull(args);
        assertEquals(1, args.length);
        assertEquals(5L, args[0]);
    }

    @Test
    void testMissingNamedParameterThrowsException() {
        JpqlStmt stmt = JpqlParser.parse("SELECT b FROM Book b WHERE b.title = :title");
        
        Map<String, Object> namedParams = new HashMap<>();
        // Don't set :title
        
        assertThrows(IllegalArgumentException.class, () -> {
            ParameterExtractor.extractParameters(stmt, namedParams, Map.of());
        });
    }

    @Test
    void testMissingPositionalParameterThrowsException() {
        JpqlStmt stmt = JpqlParser.parse("SELECT b FROM Book b WHERE b.title = ?1");
        
        // Don't set ?1
        assertThrows(IllegalArgumentException.class, () -> {
            ParameterExtractor.extractParameters(stmt, Map.of(), Map.of());
        });
    }

    @Test
    void testNoParametersReturnsEmptyArray() {
        JpqlStmt stmt = JpqlParser.parse("SELECT b FROM Book b");
        
        Object[] args = ParameterExtractor.extractParameters(stmt, Map.of(), Map.of());
        
        assertNotNull(args);
        assertEquals(0, args.length);
    }

    @Test
    void testParametersInFunction() {
        JpqlStmt stmt = JpqlParser.parse(
            "SELECT b FROM Book b WHERE UPPER(b.title) = :title");
        
        Map<String, Object> namedParams = new HashMap<>();
        namedParams.put("title", "TEST");
        
        Object[] args = ParameterExtractor.extractParameters(stmt, namedParams, Map.of());
        
        assertNotNull(args);
        assertEquals(1, args.length);
        assertEquals("TEST", args[0]);
    }

    @Test
    void testParametersInAndPredicate() {
        // Test simple AND without parentheses first
        JpqlStmt stmt = JpqlParser.parse(
            "SELECT b FROM Book b WHERE b.title = :title AND b.price > :price");
        
        Map<String, Object> namedParams = new HashMap<>();
        namedParams.put("title", "Book1");
        namedParams.put("price", 100);
        
        Object[] args = ParameterExtractor.extractParameters(stmt, namedParams, Map.of());
        
        assertNotNull(args);
        assertEquals(2, args.length);
        assertEquals("Book1", args[0]);
        assertEquals(100, args[1]);
    }

    @Test
    void testParametersInOrPredicate() {
        JpqlStmt stmt = JpqlParser.parse(
            "SELECT b FROM Book b WHERE b.title = :title OR b.price > :price");
        
        Map<String, Object> namedParams = new HashMap<>();
        namedParams.put("title", "Book1");
        namedParams.put("price", 50);
        
        Object[] args = ParameterExtractor.extractParameters(stmt, namedParams, Map.of());
        
        assertNotNull(args);
        assertEquals(2, args.length);
        assertEquals("Book1", args[0]);
        assertEquals(50, args[1]);
    }

    @Test
    void testParametersInNotPredicate() {
        JpqlStmt stmt = JpqlParser.parse(
            "SELECT b FROM Book b WHERE NOT (b.title = :title)");
        
        Map<String, Object> namedParams = new HashMap<>();
        namedParams.put("title", "Book1");
        
        Object[] args = ParameterExtractor.extractParameters(stmt, namedParams, Map.of());
        
        assertNotNull(args);
        assertEquals(1, args.length);
        assertEquals("Book1", args[0]);
    }

    @Test
    void testParametersInComplexAndOr() {
        // Test: b.title = :title AND b.author = :author OR b.price > :price
        JpqlStmt stmt = JpqlParser.parse(
            "SELECT b FROM Book b WHERE b.title = :title AND b.author = :author OR b.price > :price");
        
        Map<String, Object> namedParams = new HashMap<>();
        namedParams.put("title", "Book1");
        namedParams.put("author", "Author1");
        namedParams.put("price", 100);
        
        Object[] args = ParameterExtractor.extractParameters(stmt, namedParams, Map.of());
        
        assertNotNull(args);
        // The order depends on how the parser structures AND/OR
        // But we should have all 3 parameters
        assertEquals(3, args.length);
    }
}

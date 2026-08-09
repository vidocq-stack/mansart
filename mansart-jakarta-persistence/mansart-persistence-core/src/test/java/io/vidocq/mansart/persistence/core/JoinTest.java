/*
 * Copyright (c) ${year} Yann Blazart, Antoine Sabot-Durand and the Vidocq contributors
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0 OR GPL-2.0-or-later
 */

package io.vidocq.mansart.persistence.core;

import io.vidocq.mansart.data.query.ast.*;
import jakarta.persistence.*;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for JPQL JOIN syntax implementation.
 * M6 - Priority 2: Verify explicit JOIN support
 */
class JoinTest {

    @Test
    void testParseInnerJoin() {
        JpqlStmt stmt = JpqlParser.parse(
            "SELECT b FROM TestEntity b JOIN b.relatedEntity r");
        
        assertThat(stmt).isNotNull();
        assertThat(stmt).isInstanceOf(JpqlSelectStmt.class);
        
        JpqlSelectStmt selectStmt = (JpqlSelectStmt) stmt;
        assertThat(selectStmt.fromClause().hasJoins()).isTrue();
    }

    @Test
    void testParseLeftJoin() {
        JpqlStmt stmt = JpqlParser.parse(
            "SELECT b FROM TestEntity b LEFT JOIN b.relatedEntity r");
        
        assertThat(stmt).isInstanceOf(JpqlSelectStmt.class);
        JpqlSelectStmt selectStmt = (JpqlSelectStmt) stmt;
        assertThat(selectStmt.fromClause().hasJoins()).isTrue();
    }

    @Test
    void testParseRightJoin() {
        JpqlStmt stmt = JpqlParser.parse(
            "SELECT b FROM TestEntity b RIGHT JOIN b.relatedEntity r ON r.id = b.id");
        
        assertThat(stmt).isInstanceOf(JpqlSelectStmt.class);
    }

    @Test
    void testParseJoinWithOn() {
        JpqlStmt stmt = JpqlParser.parse(
            "SELECT b FROM TestEntity b JOIN b.relatedEntity r ON r.name = b.name");
        
        assertThat(stmt).isInstanceOf(JpqlSelectStmt.class);
        JpqlSelectStmt selectStmt = (JpqlSelectStmt) stmt;
        
        JpqlFromClause fromClause = selectStmt.fromClause();
        // At least one from item with joins
        assertThat(fromClause.items()).isNotEmpty();
        
        // Find the from item that has joins
        JpqlFromItem fromItemWithJoins = fromClause.items().stream()
            .filter(item -> !item.joins().isEmpty())
            .findFirst()
            .orElse(null);
        
        assertThat(fromItemWithJoins).isNotNull();
        assertThat(fromItemWithJoins.joins()).isNotEmpty();
        
        JpqlJoin join = fromItemWithJoins.joins().get(0);
        assertThat(join.onCondition()).isPresent();
    }

    @Test
    void testParseMultipleJoins() {
        JpqlStmt stmt = JpqlParser.parse(
            "SELECT b FROM TestEntity b JOIN b.relatedEntity r JOIN r.anotherEntity a");
        
        assertThat(stmt).isInstanceOf(JpqlSelectStmt.class);
        JpqlSelectStmt selectStmt = (JpqlSelectStmt) stmt;
        assertThat(selectStmt.fromClause().hasJoins()).isTrue();
    }

    @Test
    void testParseFetchJoin() {
        JpqlStmt stmt = JpqlParser.parse(
            "SELECT b FROM TestEntity b JOIN FETCH b.relatedEntity r");
        
        assertThat(stmt).isInstanceOf(JpqlSelectStmt.class);
    }

    @Test
    void testExecuteJoinQuery() {
        // This test requires a proper entity setup with relationships
        // For now, just verify parsing works
        JpqlStmt stmt = JpqlParser.parse(
            "SELECT b FROM TestEntity b JOIN b.relatedEntity r WHERE r.id = 1");
        
        assertThat(stmt).isNotNull();
    }
}

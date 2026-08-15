/*
 * Copyright (c) 2026 Vidocq contributors
 * SPDX-License-Identifier: EPL-2.0 OR EUPL-1.2 OR GPL-2.0-or-later
 */
package io.vidocq.mansart.persistence.core.jpql;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for GROUP BY and HAVING support (M8-1).
 */
class JPQLParserGroupByTest {

    @Test
    void testParseGroupByOnly() {
        JPQLParser parser = new JPQLParser(Map.of("Person", Person.class));
        JPQLQuery<?> query = parser.parse("SELECT p FROM Person p GROUP BY p.name");
        
        assertNotNull(query);
        JPQLQuerySpecification spec = query.specification();
        assertNotNull(spec.groupByClause());
        assertEquals(1, spec.groupByClause().expressions().size());
        assertNull(spec.havingClause());
    }

    @Test
    void testParseGroupByMultipleExpressions() {
        JPQLParser parser = new JPQLParser(Map.of("Person", Person.class));
        JPQLQuery<?> query = parser.parse("SELECT p FROM Person p GROUP BY p.name, p.age");
        
        assertNotNull(query);
        JPQLQuerySpecification spec = query.specification();
        assertNotNull(spec.groupByClause());
        assertEquals(2, spec.groupByClause().expressions().size());
        assertNull(spec.havingClause());
    }

    @Test
    void testParseHavingOnly() {
        JPQLParser parser = new JPQLParser(Map.of("Person", Person.class));
        JPQLQuery<?> query = parser.parse("SELECT p FROM Person p HAVING COUNT(p) > 1");
        
        assertNotNull(query);
        JPQLQuerySpecification spec = query.specification();
        assertNull(spec.groupByClause());
        assertNotNull(spec.havingClause());
    }

    @Test
    void testParseGroupByWithHaving() {
        JPQLParser parser = new JPQLParser(Map.of("Person", Person.class));
        JPQLQuery<?> query = parser.parse("SELECT p FROM Person p GROUP BY p.name HAVING COUNT(p) > 1");
        
        assertNotNull(query);
        JPQLQuerySpecification spec = query.specification();
        assertNotNull(spec.groupByClause());
        assertNotNull(spec.havingClause());
    }

    @Test
    void testParseGroupByWithWhereAndHaving() {
        JPQLParser parser = new JPQLParser(Map.of("Person", Person.class));
        JPQLQuery<?> query = parser.parse(
            "SELECT p FROM Person p WHERE p.age > 18 GROUP BY p.name HAVING COUNT(p) > 1");
        
        assertNotNull(query);
        JPQLQuerySpecification spec = query.specification();
        assertNotNull(spec.whereClause());
        assertNotNull(spec.groupByClause());
        assertNotNull(spec.havingClause());
    }

    // Simple test entity
    public static class Person {
        private String name;
        private int age;
        
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public int getAge() { return age; }
        public void setAge(int age) { this.age = age; }
    }
}

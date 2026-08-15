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
 * It is also made available under the European Union Public License v. 1.2,
 * which is available at
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-1-2
 *
 * SPDX-License-Identifier: EPL-2.0 OR EUPL-1.2 OR GPL-2.0-or-later
 */
package io.vidocq.mansart.persistence.tests;

import io.vidocq.mansart.persistence.core.jpql.JPQLParser;
import io.vidocq.mansart.persistence.core.jpql.JPQLQuery;
import io.vidocq.mansart.persistence.core.jpql.JPQLQuerySpecification;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for GROUP BY and HAVING support (M8-1).
 */
public class JPQLParserGroupByTest {

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

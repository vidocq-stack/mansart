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

import org.junit.jupiter.api.Test;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for PersistenceXmlParser.
 */
class PersistenceXmlParserTest {

    @Test
    void testParseBasicPersistenceXml() throws IOException {
        String xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <persistence xmlns="https://jakarta.ee/xml/ns/persistence"
                       version="3.2">
                <persistence-unit name="testPU">
                    <properties>
                        <property name="jakarta.persistence.jdbc.url" value="jdbc:h2:mem:test"/>
                        <property name="jakarta.persistence.jdbc.user" value="sa"/>
                    </properties>
                </persistence-unit>
            </persistence>
            """;
        
        List<PersistenceUnitConfig> configs = parseXml(xml);
        
        assertEquals(1, configs.size());
        PersistenceUnitConfig config = configs.get(0);
        
        assertEquals("testPU", config.getName());
        
        Map<String, Object> props = config.getProperties();
        assertEquals("jdbc:h2:mem:test", props.get("jakarta.persistence.jdbc.url"));
        assertEquals("sa", props.get("jakarta.persistence.jdbc.user"));
    }

    @Test
    void testParseMultiplePersistenceUnits() throws IOException {
        String xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <persistence xmlns="https://jakarta.ee/xml/ns/persistence"
                       version="3.2">
                <persistence-unit name="pu1">
                    <properties>
                        <property name="prop1" value="value1"/>
                    </properties>
                </persistence-unit>
                <persistence-unit name="pu2">
                    <properties>
                        <property name="prop2" value="value2"/>
                    </properties>
                </persistence-unit>
            </persistence>
            """;
        
        List<PersistenceUnitConfig> configs = parseXml(xml);
        
        assertEquals(2, configs.size());
        assertEquals("pu1", configs.get(0).getName());
        assertEquals("pu2", configs.get(1).getName());
        assertEquals("value1", configs.get(0).getProperties().get("prop1"));
        assertEquals("value2", configs.get(1).getProperties().get("prop2"));
    }

    @Test
    void testParseEmptyPersistenceXml() throws IOException {
        String xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <persistence xmlns="https://jakarta.ee/xml/ns/persistence"
                       version="3.2">
            </persistence>
            """;
        
        List<PersistenceUnitConfig> configs = parseXml(xml);
        
        assertTrue(configs.isEmpty());
    }

    @Test
    void testParseWithoutNamespace() throws IOException {
        String xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <persistence version="2.2">
                <persistence-unit name="testPU">
                    <properties>
                        <property name="test" value="value"/>
                    </properties>
                </persistence-unit>
            </persistence>
            """;
        
        List<PersistenceUnitConfig> configs = parseXml(xml);
        
        assertEquals(1, configs.size());
        assertEquals("testPU", configs.get(0).getName());
        assertEquals("value", configs.get(0).getProperties().get("test"));
    }

    // Helper method to parse XML string
    private List<PersistenceUnitConfig> parseXml(String xmlContent) throws IOException {
        byte[] bytes = xmlContent.getBytes(StandardCharsets.UTF_8);
        InputStream in = new ByteArrayInputStream(bytes);
        return PersistenceXmlParser.parse(in);
    }
}

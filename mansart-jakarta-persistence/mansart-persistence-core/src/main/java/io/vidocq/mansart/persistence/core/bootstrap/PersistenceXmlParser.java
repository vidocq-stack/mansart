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

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.*;

/**
 * Parser for Jakarta Persistence persistence.xml files.
 * Uses JDK's built-in JAXP API (no external dependencies).
 */
public final class PersistenceXmlParser {

    private static final String PERSISTENCE_NS = "https://jakarta.ee/xml/ns/persistence";

    private PersistenceXmlParser() {}

    /**
     * Finds and parses all persistence.xml files in the classpath.
     */
    public static List<PersistenceUnitConfig> parseAll() throws IOException {
        List<PersistenceUnitConfig> allConfigs = new ArrayList<>();
        Enumeration<URL> urls = ClassLoader.getSystemResources("META-INF/persistence.xml");
        
        while (urls.hasMoreElements()) {
            URL url = urls.nextElement();
            try (InputStream in = url.openStream()) {
                allConfigs.addAll(parse(in));
            } catch (Exception e) {
                throw new IOException("Failed to parse persistence.xml at " + url, e);
            }
        }
        
        return allConfigs;
    }

    /**
     * Parses a persistence.xml file from an InputStream.
     */
    public static List<PersistenceUnitConfig> parse(InputStream in) throws IOException {
        try {
            var factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setExpandEntityReferences(false);
            
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(in);
            doc.getDocumentElement().normalize();
            
            return parseDocument(doc);
        } catch (Exception e) {
            throw new IOException("Invalid persistence.xml", e);
        }
    }

    /**
     * Finds a persistence unit configuration by name.
     */
    public static PersistenceUnitConfig findByName(String persistenceUnitName) throws IOException {
        List<PersistenceUnitConfig> allConfigs = parseAll();
        for (PersistenceUnitConfig config : allConfigs) {
            if (persistenceUnitName.equals(config.getName())) {
                return config;
            }
        }
        return null;
    }

    private static List<PersistenceUnitConfig> parseDocument(Document doc) {
        List<PersistenceUnitConfig> configs = new ArrayList<>();
        Element root = doc.getDocumentElement();
        
        List<Element> puElements = childrenByTagNS(root, PERSISTENCE_NS, "persistence-unit");
        if (puElements.isEmpty()) {
            puElements = childrenByTag(root, "persistence-unit");
        }
        
        for (Element puElement : puElements) {
            PersistenceUnitConfig.Builder configBuilder = PersistenceUnitConfig.builder();
            parsePersistenceUnit(puElement, configBuilder);
            configs.add(configBuilder.build());
        }
        
        return configs;
    }

    private static void parsePersistenceUnit(Element puElement, PersistenceUnitConfig.Builder builder) {
        String name = puElement.getAttribute("name");
        if (name != null && !name.isEmpty()) {
            builder.withName(name);
        }
        
        List<Element> children = children(puElement);
        for (Element child : children) {
            String localName = getLocalName(child);
            
            switch (localName) {
                case "class":
                    String className = child.getAttribute("name");
                    if (className != null && !className.isEmpty()) {
                        builder.addClassName(className);
                    } else {
                        // class element can also contain the class name as text content
                        String textContent = text(child);
                        if (textContent != null && !textContent.isEmpty()) {
                            builder.addClassName(textContent);
                        }
                    }
                    break;
                case "properties":
                    parseProperties(child, builder);
                    break;
            }
        }
    }

    private static void parseProperties(Element propertiesElement, PersistenceUnitConfig.Builder builder) {
        for (Element prop : children(propertiesElement)) {
            String localName = getLocalName(prop);
            if ("property".equals(localName)) {
                String name = prop.getAttribute("name");
                String value = prop.getAttribute("value");
                if (name != null && !name.isEmpty() && value != null) {
                    builder.addProperty(name, value);
                }
            }
        }
    }

    // ---- DOM Helper Methods ----

    private static List<Element> children(Element parent) {
        NodeList kids = parent.getChildNodes();
        List<Element> out = new ArrayList<>();
        for (int i = 0; i < kids.getLength(); i++) {
            Node n = kids.item(i);
            if (n instanceof Element el) out.add(el);
        }
        return out;
    }

    private static List<Element> childrenByTag(Element parent, String tag) {
        List<Element> out = new ArrayList<>();
        for (Element e : children(parent)) {
            if (e.getTagName().equals(tag) || e.getLocalName().equals(tag)) {
                out.add(e);
            }
        }
        return out;
    }

    private static List<Element> childrenByTagNS(Element parent, String namespace, String localName) {
        List<Element> out = new ArrayList<>();
        for (Element e : children(parent)) {
            if (namespace.equals(e.getNamespaceURI()) && localName.equals(e.getLocalName())) {
                out.add(e);
            }
        }
        return out;
    }

    private static String getLocalName(Element element) {
        String localName = element.getLocalName();
        if (localName != null && !localName.isEmpty()) {
            return localName;
        }
        String tagName = element.getTagName();
        int colonIndex = tagName.indexOf(':');
        if (colonIndex > 0) {
            return tagName.substring(colonIndex + 1);
        }
        return tagName;
    }

    private static String text(Element e) {
        String t = e.getTextContent();
        return t == null ? null : t.trim();
    }
}

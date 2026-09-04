/*
 * SPDX-License-Identifier: EPL-2.0 OR EUPL-1.2 OR GPL-2.0-or-later
 */
package io.vidocq.mansart.persistence.core;

import jakarta.persistence.PersistenceException;
import jakarta.persistence.PersistenceUnitTransactionType;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Package-private XML parser for META-INF/persistence.xml files.
 * Parses persistence units using JDK DOM API with namespace awareness.
 */
final class PersistenceXmlParser {

    static final String PERSISTENCE_XML = "META-INF/persistence.xml";
    static final String NS = "https://jakarta.ee/xml/ns/persistence";

    /**
     * Record describing a parsed persistence unit.
     * All fields are package-private for use by the provider.
     */
    record PersistenceUnitDescriptor(
            String name,
            PersistenceUnitTransactionType transactionType,
            String providerClassName,
            String jtaDataSource,
            String nonJtaDataSource,
            Map<String, String> properties
    ) {}

    /**
     * Parses all persistence.xml files from the classpath and returns a map of persistence units.
     * Later entries with duplicate names overwrite earlier ones.
     *
     * @param loader the class loader to use for resource lookup (may be null)
     * @return a LinkedHashMap keyed by persistence unit name
     * @throws PersistenceException if parsing fails
     */
    static Map<String, PersistenceUnitDescriptor> parse(ClassLoader loader) {
        try {
            // Use context class loader, fallback to this class's class loader
            ClassLoader cl = loader != null ? loader : PersistenceXmlParser.class.getClassLoader();
            Map<String, PersistenceUnitDescriptor> result = new LinkedHashMap<>();

            // Get all persistence.xml resources
            Enumeration<URL> resources = cl.getResources(PERSISTENCE_XML);
            while (resources.hasMoreElements()) {
                URL url = resources.nextElement();
                parsePersistenceXml(url, result);
            }

            return result;
        } catch (Exception e) {
            throw new PersistenceException("Failed to parse persistence.xml", e);
        }
    }

    private static void secureFeature(DocumentBuilderFactory f, String feature, boolean value) {
        try {
            f.setFeature(feature, value);
        } catch (ParserConfigurationException _) {
            // Not supported by this parser; remaining features provide defense-in-depth
        }
    }

    private static DocumentBuilderFactory newSecureFactory() {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        secureFeature(factory, XMLConstants.FEATURE_SECURE_PROCESSING, true);
        secureFeature(factory, "http://apache.org/xml/features/disallow-doctype-decl", true);
        secureFeature(factory, "http://xml.org/sax/features/external-general-entities", false);
        secureFeature(factory, "http://xml.org/sax/features/external-parameter-entities", false);
        secureFeature(factory, "http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        try {
            factory.setExpandEntityReferences(false);
        } catch (IllegalArgumentException _) {
            // Not supported by this parser
        }
        try {
            factory.setXIncludeAware(false);
        } catch (UnsupportedOperationException _) {
            // Not supported by this parser
        }
        return factory;
    }

    private static void parsePersistenceXml(URL url, Map<String, PersistenceUnitDescriptor> result) {
        try (InputStream is = url.openStream()) {
            DocumentBuilderFactory factory = newSecureFactory();

            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(is);
            doc.getDocumentElement().normalize();

            // Parse all persistence-unit elements
            NodeList puNodes = doc.getElementsByTagNameNS(NS, "persistence-unit");
            for (int i = 0; i < puNodes.getLength(); i++) {
                Element puElement = (Element) puNodes.item(i);
                parsePersistenceUnit(puElement, result);
            }
        } catch (IOException | ParserConfigurationException | SAXException e) {
            throw new PersistenceException("Failed to parse persistence.xml at " + url, e);
        }
    }

    private static String childText(Element parent, String localName) {
        NodeList nodes = parent.getElementsByTagNameNS(NS, localName);
        if (nodes.getLength() == 0) {
            return null;
        }
        String text = nodes.item(0).getTextContent().trim();
        return text.isEmpty() ? null : text;
    }

    private static Map<String, String> parseProperties(Element puElement) {
        Map<String, String> properties = new LinkedHashMap<>();
        NodeList propertiesNodes = puElement.getElementsByTagNameNS(NS, "properties");
        if (propertiesNodes.getLength() > 0) {
            Element propertiesElement = (Element) propertiesNodes.item(0);
            NodeList propertyNodes = propertiesElement.getElementsByTagNameNS(NS, "property");
            for (int i = 0; i < propertyNodes.getLength(); i++) {
                Element propertyElement = (Element) propertyNodes.item(i);
                String propName = propertyElement.getAttribute("name");
                String propValue = propertyElement.getAttribute("value");
                if (propName != null && !propName.trim().isEmpty() && propValue != null) {
                    properties.put(propName.trim(), propValue.trim());
                }
            }
        }
        return properties;
    }

    private static void parsePersistenceUnit(Element puElement, Map<String, PersistenceUnitDescriptor> result) {
        // Required: name attribute
        String name = puElement.getAttribute("name");
        if (name == null || name.trim().isEmpty()) {
            throw new PersistenceException("persistence-unit must have a 'name' attribute");
        }

        // Optional: transaction-type attribute (default RESOURCE_LOCAL per spec for SE)
        String transactionTypeAttr = puElement.getAttribute("transaction-type");
        PersistenceUnitTransactionType transactionType = transactionTypeAttr != null && !transactionTypeAttr.trim().isEmpty()
                ? PersistenceUnitTransactionType.valueOf(transactionTypeAttr)
                : PersistenceUnitTransactionType.RESOURCE_LOCAL;

        Map<String, String> properties = parseProperties(puElement);

        // Create descriptor and add to result (later wins for duplicates)
        PersistenceUnitDescriptor descriptor = new PersistenceUnitDescriptor(
                name,
                transactionType,
                childText(puElement, "provider"),
                childText(puElement, "jta-data-source"),
                childText(puElement, "non-jta-data-source"),
                Collections.unmodifiableMap(properties)
        );
        result.put(name, descriptor);
    }

    // Prevent instantiation
    private PersistenceXmlParser() {
        throw new AssertionError("Utility class cannot be instantiated");
    }
}

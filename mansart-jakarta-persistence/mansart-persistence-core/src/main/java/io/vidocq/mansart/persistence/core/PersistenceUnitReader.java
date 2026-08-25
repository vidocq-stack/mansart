/*
 * Copyright (c) 2026 Yann Blazart, Antoine Sabot-Durand and the Vidocq contributors
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * This Source Code may also be made available under the following Secondary
 * Licenses when the conditions of such availability set forth in the Eclipse
 * Public License, v. 2.0 are satisfied: GNU General Public License, version 2
 * or any later version, which is available at
 * https://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 *
 * It is also made available under the European Union Public Licence v. 1.2,
 * which is available at https://joinup.ec.europa.eu/collection/eupl/eupl-1.2
 *
 * SPDX-License-Identifier: EPL-2.0 OR EUPL-1.2 OR GPL-2.0-or-later
 */
package io.vidocq.mansart.persistence.core;

import jakarta.persistence.SharedCacheMode;
import jakarta.persistence.ValidationMode;
import jakarta.persistence.spi.PersistenceUnitTransactionType;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Parses a standard {@code persistence.xml} document into a
 * {@link PersistenceUnitInfoImpl} using only the JDK StAX API
 * ({@code java.xml}). Zero external dependencies.
 *
 * <p>The parser handles a single {@code <persistence-unit>} element
 * with the following child elements:</p>
 * <ul>
 *   <li>{@code <provider>} — persistence provider class name</li>
 *   <li>{@code <class>} — managed class names</li>
 *   <li>{@code <jta-data-source>} — JTA data source resource name</li>
 *   <li>{@code <non-jta-data-source>} — non-JTA data source resource name</li>
 *   <li>{@code <mapping-file>} — mapping file names</li>
 *   <li>{@code <jar-file>} — JAR file paths (resolved to URLs)</li>
 *   <li>{@code <property>} — key-value properties</li>
 *   <li>{@code <shared-cache-mode>} — shared cache mode</li>
 *   <li>{@code <validation-mode>} — validation mode</li>
 *   <li>{@code <scope>} — scope annotation name (JPA 4.0)</li>
 *   <li>{@code <qualifier>} — qualifier annotation names (JPA 4.0)</li>
 *   <li>{@code <exclude-unlisted-classes>} — exclude-unlisted-classes flag</li>
 * </ul>
 *
 * @see PersistenceUnitInfoImpl
 */
public final class PersistenceUnitReader {

    private static final String PERSISTENCE_NS = "https://jakarta.ee/xml/ns/persistence";

    private PersistenceUnitReader() {
        // Utility class — no instances.
    }

    /**
     * Reads a single persistence unit from the given {@code persistence.xml} URL.
     *
     * @param url URL pointing to a {@code persistence.xml} file
     * @return a fully populated {@link PersistenceUnitInfoImpl}
     * @throws IOException if the URL cannot be opened
     * @throws IllegalArgumentException if no {@code <persistence-unit>}
     *         element is found, or the document is not a valid
     *         {@code persistence.xml}
     */
    public static PersistenceUnitInfoImpl read(URL url) throws IOException {
        try (InputStream is = url.openStream()) {
            XMLInputFactory factory = XMLInputFactory.newInstance();
            factory.setProperty(XMLInputFactory.IS_VALIDATING, false);
            factory.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, true);
            XMLEventReader reader = factory.createXMLEventReader(is, StandardCharsets.UTF_8.name());

            PersistenceUnitInfoImpl info = new PersistenceUnitInfoImpl();
            boolean foundPersistenceUnit = false;
            boolean foundPersistenceElement = false;
            List<String> qualifierNames = new ArrayList<>();

            while (reader.hasNext()) {
                XMLEvent event = reader.nextEvent();

                if (event.isStartElement()) {
                    StartElement start = event.asStartElement();
                    String localName = start.getName().getLocalPart();

                    // The root <persistence> element establishes the namespace
                    if ("persistence".equals(localName) && !foundPersistenceElement) {
                        foundPersistenceElement = true;
                        String version = getAttribute(start, "version");
                        if (version != null) {
                            info.setPersistenceXMLSchemaVersion(version);
                        }
                        continue;
                    }

                    if ("persistence-unit".equals(localName)) {
                        foundPersistenceUnit = true;
                        String unitName = getAttribute(start, "name");
                        if (unitName == null) {
                            throw new IllegalArgumentException(
                                    "persistence-unit element missing required 'name' attribute");
                        }
                        info.setPersistenceUnitName(unitName);
                        // The root URL is the URL from which persistence.xml was loaded
                        info.setPersistenceUnitRootUrl(url);

                        String txAttr = getAttribute(start, "transaction-type");
                        if (txAttr != null) {
                            info.setTransactionType(parseTransactionType(txAttr));
                        } else {
                            // Default per spec: JTA
                            info.setTransactionType(PersistenceUnitTransactionType.JTA);
                        }
                    } else if (foundPersistenceUnit) {
                        parseChildElement(start, reader, info, url, qualifierNames);
                    }
                }
            }

            if (!foundPersistenceElement) {
                throw new IllegalArgumentException(
                        "Not a persistence.xml: no <persistence> root element");
            }
            if (!foundPersistenceUnit) {
                throw new IllegalArgumentException(
                        "No <persistence-unit> element found in persistence.xml");
            }

            // Set collected qualifier names
            info.setQualifierAnnotationNames(qualifierNames);

            return info;
        } catch (XMLStreamException e) {
            throw new IOException("Failed to parse persistence.xml from " + url, e);
        }
    }

    /**
     * Get an attribute value by local name from a StartElement,
     * searching all namespaces.
     */
    private static String getAttribute(StartElement start, String attributeName) {
        java.util.Iterator<Attribute> it = start.getAttributes();
        while (it.hasNext()) {
            Attribute attr = it.next();
            if (attributeName.equals(attr.getName().getLocalPart())) {
                return attr.getValue();
            }
        }
        return null;
    }

    /**
     * Parse child elements of a <persistence-unit>, consuming
     * their content from the reader.
     */
    private static void parseChildElement(
            StartElement start,
            XMLEventReader reader,
            PersistenceUnitInfoImpl info,
            URL baseUrl,
            List<String> qualifierNames) {
        String localName = start.getName().getLocalPart();

        switch (localName) {
            case "provider" -> info.setPersistenceProviderClassName(getTextContent(reader));
            case "class" -> info.addManagedClassName(getTextContent(reader));
            case "jta-data-source" -> info.setJtaDataSourceName(getTextContent(reader));
            case "non-jta-data-source" -> info.setNonJtaDataSourceName(getTextContent(reader));
            case "mapping-file" -> info.addMappingFileName(getTextContent(reader));
            case "jar-file" -> {
                String path = getTextContent(reader);
                if (path != null && !path.isBlank()) {
                    try {
                        info.addJarFileUrl(new URL(baseUrl, path));
                    } catch (IOException e) {
                        // Ignore unresolvable jar-file paths.
                    }
                }
            }
            case "property" -> {
                String name = getAttribute(start, "name");
                String value = getAttribute(start, "value");
                if (value == null) {
                    value = getTextContent(reader);
                }
                if (name != null) {
                    info.addProperty(name, value);
                }
            }
            case "shared-cache-mode" ->
                    info.setSharedCacheMode(parseSharedCacheMode(getTextContent(reader)));
            case "validation-mode" ->
                    info.setValidationMode(parseValidationMode(getTextContent(reader)));
            case "scope" -> info.setScopeAnnotationName(getTextContent(reader));
            case "qualifier" -> {
                String qualifier = getTextContent(reader);
                if (qualifier != null && !qualifier.isBlank()) {
                    qualifierNames.add(qualifier);
                }
            }
            case "exclude-unlisted-classes" ->
                    info.setExcludeUnlistedClasses(
                            Boolean.parseBoolean(getTextContent(reader)));
            default -> {
                // Unknown element — ignore silently (forward compatibility).
                // Skip nested content if any.
                skipElementContent(reader, localName);
            }
        }
    }

    /** Skip nested content of an unknown element. */
    private static void skipElementContent(XMLEventReader reader, String elementName) {
        int depth = 1;
        try {
            while (reader.hasNext() && depth > 0) {
                XMLEvent event = reader.nextEvent();
                if (event.isStartElement()) {
                    depth++;
                } else if (event.isEndElement()) {
                    String endLocal = event.asEndElement().getName().getLocalPart();
                    if (elementName.equals(endLocal)) {
                        depth--;
                    }
                }
            }
        } catch (XMLStreamException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Read character content from the reader until the matching end element.
     */
    private static String getTextContent(XMLEventReader reader) {
        StringBuilder sb = new StringBuilder();
        try {
            while (reader.hasNext()) {
                XMLEvent event = reader.nextEvent();
                if (event.isCharacters()) {
                    sb.append(event.asCharacters().getData());
                } else if (event.isEndElement()) {
                    break;
                }
            }
        } catch (XMLStreamException e) {
            throw new RuntimeException(e);
        }
        String text = sb.toString().trim();
        return text.isEmpty() ? null : text;
    }

    /**
     * Parse transaction-type string into the SPI enum.
     */
    private static PersistenceUnitTransactionType parseTransactionType(String value) {
        String upper = value.toUpperCase(Locale.ROOT);
        if ("JTA".equals(upper)) {
            return PersistenceUnitTransactionType.JTA;
        } else if ("RESOURCE_LOCAL".equals(upper)) {
            return PersistenceUnitTransactionType.RESOURCE_LOCAL;
        }
        throw new IllegalArgumentException(
                "Invalid transaction-type: " + value + " (expected JTA or RESOURCE_LOCAL)");
    }

    private static SharedCacheMode parseSharedCacheMode(String value) {
        String upper = value.toUpperCase(Locale.ROOT);
        return switch (upper) {
            case "ALL" -> SharedCacheMode.ALL;
            case "ENABLE_SELECTIVE" -> SharedCacheMode.ENABLE_SELECTIVE;
            case "DISABLE_SELECTIVE" -> SharedCacheMode.DISABLE_SELECTIVE;
            case "NONE" -> SharedCacheMode.NONE;
            default -> throw new IllegalArgumentException(
                    "Invalid shared-cache-mode: " + value);
        };
    }

    private static ValidationMode parseValidationMode(String value) {
        String upper = value.toUpperCase(Locale.ROOT);
        return switch (upper) {
            case "CALLBACK" -> ValidationMode.CALLBACK;
            case "AUTO" -> ValidationMode.AUTO;
            case "NONE" -> ValidationMode.NONE;
            default -> throw new IllegalArgumentException(
                    "Invalid validation-mode: " + value);
        };
    }
}

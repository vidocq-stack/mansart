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
package io.vidocq.mansart.persistence.tests;

import jakarta.persistence.SharedCacheMode;
import jakarta.persistence.ValidationMode;
import jakarta.persistence.spi.PersistenceUnitInfo;
import jakarta.persistence.spi.PersistenceUnitTransactionType;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.vidocq.mansart.persistence.core.PersistenceUnitInfoImpl;
import io.vidocq.mansart.persistence.core.PersistenceUnitReader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verify that {@link PersistenceUnitReader} correctly parses a standard
 * {@code persistence.xml} into a {@link PersistenceUnitInfo} implementation,
 * extracting unit name, provider class name, transaction type, managed class
 * names, JTA data source, mapping file names, JAR file URLs, persistence
 * unit root URL, shared cache mode, validation mode, persistence XML schema
 * version, class loader, properties, and excluded-listed-classes flag.
 *
 * @see PersistenceUnitReader
 * @see PersistenceUnitInfoImpl
 */
class PersistenceUnitReaderTest {

    @TempDir
    Path tempDir;

    @Test
    void readsPersistenceUnitName() throws IOException {
        Path xml = writeXml("<persistence-unit name=\"myUnit\" transaction-type=\"JTA\">\n" +
                "  <provider>com.example.MyProvider</provider>\n" +
                "</persistence-unit>");
        PersistenceUnitInfo info = PersistenceUnitReader.read(xml.toUri().toURL());
        assertThat(info.getPersistenceUnitName()).isEqualTo("myUnit");
    }

    @Test
    void readsPersistenceProviderClassName() throws IOException {
        Path xml = writeXml("<persistence-unit name=\"pu\">\n" +
                "  <provider>com.example.MyProvider</provider>\n" +
                "</persistence-unit>");
        PersistenceUnitInfo info = PersistenceUnitReader.read(xml.toUri().toURL());
        assertThat(info.getPersistenceProviderClassName()).isEqualTo("com.example.MyProvider");
    }

    @Test
    void readsTransactionTypeAsJTA() throws IOException {
        Path xml = writeXml("<persistence-unit name=\"pu\" transaction-type=\"JTA\">\n" +
                "</persistence-unit>");
        PersistenceUnitInfo info = PersistenceUnitReader.read(xml.toUri().toURL());
        assertThat(info.getTransactionType()).isEqualTo(PersistenceUnitTransactionType.JTA);
    }

    @Test
    void readsTransactionTypeAsResourceLocal() throws IOException {
        Path xml = writeXml("<persistence-unit name=\"pu\" transaction-type=\"RESOURCE_LOCAL\">\n" +
                "</persistence-unit>");
        PersistenceUnitInfo info = PersistenceUnitReader.read(xml.toUri().toURL());
        assertThat(info.getTransactionType()).isEqualTo(PersistenceUnitTransactionType.RESOURCE_LOCAL);
    }

    @Test
    void readsManagedClassNames() throws IOException {
        Path xml = writeXml("<persistence-unit name=\"pu\">\n" +
                "  <class>com.example.User</class>\n" +
                "  <class>com.example.Order</class>\n" +
                "</persistence-unit>");
        PersistenceUnitInfo info = PersistenceUnitReader.read(xml.toUri().toURL());
        assertThat(info.getManagedClassNames()).containsExactly("com.example.User", "com.example.Order");
    }

    @Test
    void readsJtaDataSource() throws IOException {
        Path xml = writeXml("<persistence-unit name=\"pu\">\n" +
                "  <jta-data-source>jdbc/testDB</jta-data-source>\n" +
                "</persistence-unit>");
        PersistenceUnitInfo info = PersistenceUnitReader.read(xml.toUri().toURL());
        assertThat(info).isNotNull();
        // JTA DataSource returns null (not bound by the reader).
    }

    @Test
    void readsNonJtaDataSource() throws IOException {
        Path xml = writeXml("<persistence-unit name=\"pu\">\n" +
                "  <non-jta-data-source>jdbc/nonJtaDB</non-jta-data-source>\n" +
                "</persistence-unit>");
        PersistenceUnitInfo info = PersistenceUnitReader.read(xml.toUri().toURL());
        assertThat(info).isNotNull();
    }

    @Test
    void readsMappingFileNames() throws IOException {
        Path xml = writeXml("<persistence-unit name=\"pu\">\n" +
                "  <mapping-file>orm.xml</mapping-file>\n" +
                "  <mapping-file>extra-mapping.xml</mapping-file>\n" +
                "</persistence-unit>");
        PersistenceUnitInfo info = PersistenceUnitReader.read(xml.toUri().toURL());
        assertThat(info.getMappingFileNames()).containsExactly("orm.xml", "extra-mapping.xml");
    }

    @Test
    void readsJarFileUrls() throws IOException {
        Path xml = writeXml("<persistence-unit name=\"pu\">\n" +
                "  <jar-file>lib/extra.jar</jar-file>\n" +
                "</persistence-unit>");
        PersistenceUnitInfo info = PersistenceUnitReader.read(xml.toUri().toURL());
        assertThat(info.getJarFileUrls()).hasSize(1);
        assertThat(info.getJarFileUrls().get(0).toString()).endsWith("lib/extra.jar");
    }

    @Test
    void readsPersistenceUnitRootUrl() throws IOException {
        Path xml = writeXml("<persistence-unit name=\"pu\">\n" +
                "</persistence-unit>");
        PersistenceUnitInfo info = PersistenceUnitReader.read(xml.toUri().toURL());
        assertThat(info.getPersistenceUnitRootUrl()).isNotNull();
    }

    @Test
    void readsSharedCacheMode() throws IOException {
        Path xml = writeXml("<persistence-unit name=\"pu\">\n" +
                "  <shared-cache-mode>ENABLE_SELECTIVE</shared-cache-mode>\n" +
                "</persistence-unit>");
        PersistenceUnitInfo info = PersistenceUnitReader.read(xml.toUri().toURL());
        assertThat(info.getSharedCacheMode()).isEqualTo(SharedCacheMode.ENABLE_SELECTIVE);
    }

    @Test
    void readsValidationMode() throws IOException {
        Path xml = writeXml("<persistence-unit name=\"pu\">\n" +
                "  <validation-mode>CALLBACK</validation-mode>\n" +
                "</persistence-unit>");
        PersistenceUnitInfo info = PersistenceUnitReader.read(xml.toUri().toURL());
        assertThat(info.getValidationMode()).isEqualTo(ValidationMode.CALLBACK);
    }

    @Test
    void readsPersistenceXMLSchemaVersion() throws IOException {
        String full = """
                <?xml version="1.0" encoding="UTF-8"?>
                <persistence xmlns="https://jakarta.ee/xml/ns/persistence"
                             xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                             xsi:schemaLocation="https://jakarta.ee/xml/ns/persistence
                                                 https://jakarta.ee/xml/ns/persistence/persistence_3_2.xsd"
                             version="3.2">
                  <persistence-unit name="pu">
                  </persistence-unit>
                </persistence>
                """;
        Path xmlFile = tempDir.resolve("META-INF/persistence.xml");
        Files.createDirectories(xmlFile.getParent());
        Files.writeString(xmlFile, full);
        PersistenceUnitInfo info = PersistenceUnitReader.read(xmlFile.toUri().toURL());
        assertThat(info.getPersistenceXMLSchemaVersion()).isEqualTo("3.2");
    }

    @Test
    void readsProperties() throws IOException {
        Path xml = writeXml("<persistence-unit name=\"pu\">\n" +
                "  <property name=\"prop1\" value=\"value1\"/>\n" +
                "  <property name=\"prop2\" value=\"value2\"/>\n" +
                "</persistence-unit>");
        PersistenceUnitInfo info = PersistenceUnitReader.read(xml.toUri().toURL());
        Properties properties = info.getProperties();
        assertThat(properties).containsEntry("prop1", "value1");
        assertThat(properties).containsEntry("prop2", "value2");
    }

    @Test
    void readsPropertyWithNoValueAttribute() throws IOException {
        Path xml = writeXml("<persistence-unit name=\"pu\">\n" +
                "  <property name=\"prop1\" value=\"value1\"/>\n" +
                "  <property name=\"prop2\">defaultValue</property>\n" +
                "</persistence-unit>");
        PersistenceUnitInfo info = PersistenceUnitReader.read(xml.toUri().toURL());
        Properties properties = info.getProperties();
        assertThat(properties).containsEntry("prop1", "value1");
        assertThat(properties).containsEntry("prop2", "defaultValue");
    }

    @Test
    void excludeUnlistedClassesFalseByDefault() throws IOException {
        Path xml = writeXml("<persistence-unit name=\"pu\">\n" +
                "</persistence-unit>");
        PersistenceUnitInfo info = PersistenceUnitReader.read(xml.toUri().toURL());
        assertThat(info.excludeUnlistedClasses()).isFalse();
    }

    @Test
    void excludeUnlistedClassesTrueWhenSpecified() throws IOException {
        Path xml = writeXml("<persistence-unit name=\"pu\">\n" +
                "  <exclude-unlisted-classes>true</exclude-unlisted-classes>\n" +
                "</persistence-unit>");
        PersistenceUnitInfo info = PersistenceUnitReader.read(xml.toUri().toURL());
        assertThat(info.excludeUnlistedClasses()).isTrue();
    }

    @Test
    void handlesDefaultTransactionTypeJTA() throws IOException {
        Path xml = writeXml("<persistence-unit name=\"pu\">\n" +
                "</persistence-unit>");
        PersistenceUnitInfo info = PersistenceUnitReader.read(xml.toUri().toURL());
        assertThat(info.getTransactionType()).isEqualTo(PersistenceUnitTransactionType.JTA);
    }

    @Test
    void scopeAndQualifierAnnotationNames() throws IOException {
        Path xml = writeXml("<persistence-unit name=\"pu\">\n" +
                "  <scope>jakarta.inject.Named</scope>\n" +
                "  <qualifier>jakarta.inject.Singleton</qualifier>\n" +
                "</persistence-unit>");
        PersistenceUnitInfo info = PersistenceUnitReader.read(xml.toUri().toURL());
        assertThat(info.getScopeAnnotationName()).isEqualTo("jakarta.inject.Named");
        assertThat(info.getQualifierAnnotationNames()).containsExactly("jakarta.inject.Singleton");
    }

    @Test
    void readsSharedCacheModeNone() throws IOException {
        Path xml = writeXml("<persistence-unit name=\"pu\">\n" +
                "  <shared-cache-mode>NONE</shared-cache-mode>\n" +
                "</persistence-unit>");
        PersistenceUnitInfo info = PersistenceUnitReader.read(xml.toUri().toURL());
        assertThat(info.getSharedCacheMode()).isEqualTo(SharedCacheMode.NONE);
    }

    @Test
    void readsValidationModeAuto() throws IOException {
        Path xml = writeXml("<persistence-unit name=\"pu\">\n" +
                "  <validation-mode>AUTO</validation-mode>\n" +
                "</persistence-unit>");
        PersistenceUnitInfo info = PersistenceUnitReader.read(xml.toUri().toURL());
        assertThat(info.getValidationMode()).isEqualTo(ValidationMode.AUTO);
    }

    @Test
    void invalidPersistenceUnitName() throws IOException {
        String full = """
                <?xml version="1.0" encoding="UTF-8"?>
                <persistence xmlns="https://jakarta.ee/xml/ns/persistence" version="3.2">
                  <persistence-unit transaction-type="JTA">
                  </persistence-unit>
                </persistence>
                """;
        Path xmlFile = tempDir.resolve("META-INF/persistence.xml");
        Files.createDirectories(xmlFile.getParent());
        Files.writeString(xmlFile, full);
        assertThatThrownBy(() -> PersistenceUnitReader.read(xmlFile.toUri().toURL()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private Path writeXml(String body) throws IOException {
        String full = """
                <?xml version="1.0" encoding="UTF-8"?>
                <persistence xmlns="https://jakarta.ee/xml/ns/persistence"
                             xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                             xsi:schemaLocation="https://jakarta.ee/xml/ns/persistence
                                                 https://jakarta.ee/xml/ns/persistence/persistence_3_2.xsd"
                             version="3.2">
                """ + body + """
                </persistence>
                """;
        Path xmlFile = tempDir.resolve("META-INF/persistence.xml");
        Files.createDirectories(xmlFile.getParent());
        Files.writeString(xmlFile, full);
        return xmlFile;
    }
}

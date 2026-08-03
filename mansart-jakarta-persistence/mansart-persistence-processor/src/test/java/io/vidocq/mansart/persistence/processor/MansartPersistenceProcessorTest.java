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
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 *
 * SPDX-License-Identifier: EPL-2.0 OR EUPL-1.2 OR GPL-2.0-or-later
 */
package io.vidocq.mansart.persistence.processor;

import org.junit.jupiter.api.Test;

import javax.annotation.processing.Processor;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ServiceLoader;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for MansartPersistenceProcessor.
 */
class MansartPersistenceProcessorTest {

    @Test
    void shouldBeAnnotationProcessor() {
        MansartPersistenceProcessor processor = new MansartPersistenceProcessor();
        assertThat(processor).isInstanceOf(Processor.class);
    }

    @Test
    void shouldHaveCorrectSupportedAnnotationTypes() {
        MansartPersistenceProcessor processor = new MansartPersistenceProcessor();
        // The processor supports various JPA class-level annotations
        assertThat(processor.getSupportedAnnotationTypes()).contains("jakarta.persistence.Entity");
        assertThat(processor.getSupportedAnnotationTypes()).contains("jakarta.persistence.Embeddable");
        assertThat(processor.getSupportedAnnotationTypes()).contains("jakarta.persistence.MappedSuperclass");
        assertThat(processor.getSupportedAnnotationTypes()).contains("jakarta.persistence.EntityListeners");
        assertThat(processor.getSupportedAnnotationTypes()).contains("jakarta.persistence.NamedQuery");
        assertThat(processor.getSupportedAnnotationTypes()).contains("jakarta.persistence.Converter");
    }

    @Test
    void shouldHaveCorrectSourceVersion() {
        MansartPersistenceProcessor processor = new MansartPersistenceProcessor();
        assertThat(processor.getSupportedSourceVersion()).isEqualTo(javax.lang.model.SourceVersion.RELEASE_25);
    }

    @Test
    void shouldBeDiscoverableViaServiceLoader() {
        ServiceLoader<Processor> loader = ServiceLoader.load(Processor.class);
        boolean found = false;
        for (Processor processor : loader) {
            if (processor instanceof MansartPersistenceProcessor) {
                found = true;
                break;
            }
        }
        assertThat(found).isTrue();
    }

    @Test
    void shouldHaveServiceFile() throws IOException {
        URL serviceUrl = MansartPersistenceProcessorTest.class.getClassLoader()
                .getResource("META-INF/services/javax.annotation.processing.Processor");
        
        assertThat(serviceUrl).isNotNull();
        
        try (InputStream is = serviceUrl.openStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
            String content = reader.lines().collect(Collectors.joining("\n"));
            assertThat(content).contains("io.vidocq.mansart.persistence.processor.MansartPersistenceProcessor");
        }
    }
}

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
package io.vidocq.mansart.persistence.maven;

import io.vidocq.mansart.persistence.processor.ExternalEntityCodegen;
import io.vidocq.mansart.persistence.processor.SourceSink;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * A throwaway annotation processor that the Maven plugin attaches to a source-less
 * {@code javax.tools.JavaCompiler} task. The {@link javax.annotation.processing.ProcessingEnvironment}
 * resolves types from the project classpath and provides a {@code Filer} that writes generated
 * sources into the configured output directory — giving {@link ExternalEntityCodegen} exactly the
 * {@code javax.lang.model} world the APT path uses, with no hand-written adapters.
 */
@SupportedAnnotationTypes("*")
@SupportedSourceVersion(SourceVersion.RELEASE_25)
final class ExternalEntityCodegenProcessor extends AbstractProcessor {

    private final List<String> entityFqns;
    private final List<String> generated = new ArrayList<>();
    private boolean done;

    ExternalEntityCodegenProcessor(List<String> entityFqns) {
        this.entityFqns = entityFqns;
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment round) {
        if (done) {
            return false;
        }
        done = true;
        SourceSink sink = fqn -> processingEnv.getFiler().createSourceFile(fqn).openWriter();
        generated.addAll(ExternalEntityCodegen.generate(
                processingEnv.getElementUtils(),
                processingEnv.getTypeUtils(),
                processingEnv.getMessager(),
                sink,
                entityFqns));
        return false;
    }

    /** FQNs of the entities whose companion classes were generated, available after the task completes. */
    List<String> generated() {
        return generated;
    }
}

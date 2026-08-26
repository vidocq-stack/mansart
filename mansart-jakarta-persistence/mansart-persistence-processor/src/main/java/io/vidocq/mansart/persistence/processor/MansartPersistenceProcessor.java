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
 * SPDX-License-Identifier: EPL-2.0 OR EUPL-1.2 OR GPL-2.0-or-later
 */
package io.vidocq.mansart.persistence.processor;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.io.Writer;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Jakarta Persistence annotation processor.
 *
 * <p>Scans {@code @jakarta.persistence.Entity}-annotated types and generates
 * the JPA static metamodel class ({@code ClassName_}) for each entity,
 * containing {@code SingularAttribute} fields for every persistent attribute.</p>
 *
 * <p>This is the APT tier of the three-tier code-generation doctrine:
 * tier 1 (this processor) handles entities in the user's sources;
 * tier 2 (Maven plugin) handles entities from external jars;
 * tier 3 (runtime Class-File API) is an extreme fallback.</p>
 *
 * @see <a href="https://jakarta.ee/specifications/persistence/3.2/">Jakarta Persistence 3.2 Spec</a>
 */
@SupportedSourceVersion(SourceVersion.RELEASE_25)
@SupportedAnnotationTypes("jakarta.persistence.Entity")
public final class MansartPersistenceProcessor extends AbstractProcessor {

    private Elements     elements;
    private Types        types;
    private EntityScanner       scanner;
    private MansartPersistenceMetamodelWriter writer;

    @Override
    public synchronized void init(ProcessingEnvironment env) {
        super.init(env);
        this.elements = env.getElementUtils();
        this.types    = env.getTypeUtils();
        this.scanner  = new EntityScanner(elements, types, env.getMessager());
        this.writer   = new MansartPersistenceMetamodelWriter(env.getFiler());
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment round) {
        if (round.processingOver()) {
            return false;
        }

        Set<TypeElement> entities = new LinkedHashSet<>();
        for (TypeElement annotation : annotations) {
            String fqn = annotation.getQualifiedName().toString();
            if (!"jakarta.persistence.Entity".equals(fqn)) continue;
            for (Element e : round.getElementsAnnotatedWith(annotation)) {
                if (e instanceof TypeElement t) {
                    entities.add(t);
                }
            }
        }

        for (TypeElement type : entities) {
            EntityScanner.EntityDescriptor descriptor = scanner.scan(type);
            if (descriptor == null) continue;
            try {
                writer.write(descriptor);
            } catch (IOException ex) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                        "[mansart-persistence] Failed to write metamodel for "
                                + type.getQualifiedName() + ": " + ex.getMessage(), type);
            }
        }

        return true;
    }
}

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

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;

import io.vidocq.mansart.persistence.processor.parse.EntityScanner;
import io.vidocq.mansart.persistence.processor.metadata.EntityMetadataGenerator;
import io.vidocq.mansart.persistence.processor.metadata.StaticMetamodelGenerator;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Annotation Processor for Jakarta Persistence annotations.
 *
 * <p>This processor:
 * <ul>
 *   <li>Scans entities annotated with {@code @jakarta.persistence.Entity}</li>
 *   <li>Extracts primary key information from {@code @Id} and {@code @GeneratedValue}</li>
 *   <li>Generates entity metadata classes (e.g., {@code BookMetadata})</li>
 *   <li>Generates static metamodel classes (e.g., {@code Book_})</li>
 * </ul>
 *
 * <p>The processor generates classes in the same package as the entity being processed.
 */
@SupportedSourceVersion(SourceVersion.RELEASE_25)
@SupportedAnnotationTypes({
    "jakarta.persistence.Entity",
    "jakarta.persistence.Id",
    "jakarta.persistence.GeneratedValue",
    "jakarta.persistence.Table",
    "jakarta.persistence.SequenceGenerator",
    "jakarta.persistence.TableGenerator"
})
public final class MansartProcessor extends AbstractProcessor {

    private EntityScanner entityScanner;
    private EntityMetadataGenerator entityMetadataGenerator;
    private StaticMetamodelGenerator staticMetamodelGenerator;
    private Filer filer;

    @Override
    public synchronized void init(ProcessingEnvironment env) {
        super.init(env);
        this.filer = env.getFiler();
        
        SourceSink sourceSink = new FilerSourceSink(filer);
        this.entityScanner = new EntityScanner(env);
        this.entityMetadataGenerator = new EntityMetadataGenerator(sourceSink);
        this.staticMetamodelGenerator = new StaticMetamodelGenerator(sourceSink);
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment round) {
        if (round.processingOver()) {
            return false;
        }

        Set<TypeElement> entities = new LinkedHashSet<>();

        for (TypeElement annotation : annotations) {
            String annotationFqn = annotation.getQualifiedName().toString();
            for (Element element : round.getElementsAnnotatedWith(annotation)) {
                if (element instanceof TypeElement typeElement) {
                    if ("jakarta.persistence.Entity".equals(annotationFqn)) {
                        entities.add(typeElement);
                    }
                }
            }
        }

        for (TypeElement entity : entities) {
            try {
                EntityScanner.EntityMetadata metadata = entityScanner.scan(entity);
                if (metadata == null) {
                    continue;
                }

                // Generate entity metadata class
                entityMetadataGenerator.generate(metadata);

                // Generate static metamodel class
                staticMetamodelGenerator.generate(metadata);

            } catch (Exception ex) {
                processingEnv.getMessager().printMessage(
                    Diagnostic.Kind.ERROR,
                    "[mansart-persistence] Failed to process entity " + entity.getQualifiedName() + ": " + ex.getMessage(),
                    entity
                );
            }
        }

        return true;
    }
}

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

import io.vidocq.mansart.persistence.processor.metadata.CallbackDispatcherGenerator;
import io.vidocq.mansart.persistence.processor.metadata.EntityMetadataGenerator;
import io.vidocq.mansart.persistence.processor.metadata.StaticMetamodelGenerator;
import io.vidocq.mansart.persistence.processor.parse.EntityScanner;

import javax.annotation.processing.Messager;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import java.util.ArrayList;
import java.util.List;

/**
 * Ahead-of-time code generation for {@code @Entity} classes that live in pre-compiled
 * dependency jars (where the {@code mansart-persistence-processor} APT never ran) —
 * typically the official Jakarta Persistence TCK entities. Drives the very same
 * generators used by the in-compiler path ({@link EntityMetadataGenerator},
 * {@link CallbackDispatcherGenerator}, {@link StaticMetamodelGenerator}).
 *
 * <p>Generated classes land in the <em>same package</em> as their entity (the metadata
 * class is package-private and uses {@code MethodHandles.privateLookupIn} on the entity),
 * which is safe on the classpath — the unnamed module has no split-package restriction.
 *
 * <p>The {@code mansart-persistence-maven-plugin} supplies real {@link Elements}/{@link Types}
 * obtained from a source-less {@code javax.tools.JavaCompiler} task over the project
 * classpath, plus a {@code Filer}-backed {@link SourceSink}.
 */
public final class ExternalEntityCodegen {

    private ExternalEntityCodegen() {
    }

    /**
     * Generates metadata, callback dispatcher, and static metamodel classes for each
     * entity named in {@code entityFqns}. Entities that cannot be resolved or scanned
     * are skipped with a warning — one exotic entity must not abort the whole batch.
     *
     * @param elements   element utils from a {@code JavaCompiler} task seeing the dependency jars
     * @param types      type utils from the same task
     * @param messager   diagnostics sink (never null in a processing environment)
     * @param sink       destination of generated sources
     * @param entityFqns fully-qualified names of external {@code @Entity} classes to process
     * @return the FQNs of the entities whose classes were actually generated
     */
    public static List<String> generate(Elements elements, Types types, Messager messager,
                                        SourceSink sink, List<String> entityFqns) {
        // External batch mode: an entity the scanner cannot handle (property access,
        // composite keys, ...) must be skipped, not abort the javac task — downgrade
        // the scanner's Kind.ERROR diagnostics to warnings.
        Messager tolerant = new NonFatalMessager(messager);
        EntityScanner scanner = new EntityScanner(elements, types, tolerant);
        EntityMetadataGenerator metadataGenerator = new EntityMetadataGenerator(sink);
        CallbackDispatcherGenerator callbackGenerator = new CallbackDispatcherGenerator(sink, tolerant);
        StaticMetamodelGenerator metamodelGenerator = new StaticMetamodelGenerator(sink);

        List<String> generated = new ArrayList<>();
        for (String fqn : entityFqns) {
            TypeElement entity = elements.getTypeElement(fqn);
            if (entity == null) {
                messager.printMessage(Diagnostic.Kind.WARNING,
                        "[mansart-persistence] External entity not resolvable on the classpath: " + fqn);
                continue;
            }
            try {
                EntityScanner.EntityMetadata metadata = scanner.scan(entity);
                if (metadata == null) {
                    continue;
                }
                metadataGenerator.generate(metadata);
                callbackGenerator.generate(metadata);
                // The dependency jar may already ship a static metamodel (the TCK does,
                // for its Criteria tests) — never shadow it with a regenerated one
                String metamodelFqn = fqn + "_";
                if (elements.getTypeElement(metamodelFqn) == null) {
                    metamodelGenerator.generate(metadata);
                }
                generated.add(fqn);
            } catch (Exception ex) {
                messager.printMessage(Diagnostic.Kind.WARNING,
                        "[mansart-persistence] Failed to generate metadata for external entity "
                                + fqn + ": " + ex);
            }
        }
        return generated;
    }

    /** Delegating messager that turns {@code Kind.ERROR} into {@code Kind.WARNING}. */
    private static final class NonFatalMessager implements Messager {

        private final Messager delegate;

        NonFatalMessager(Messager delegate) {
            this.delegate = delegate;
        }

        private static Diagnostic.Kind soften(Diagnostic.Kind kind) {
            return kind == Diagnostic.Kind.ERROR ? Diagnostic.Kind.WARNING : kind;
        }

        @Override
        public void printMessage(Diagnostic.Kind kind, CharSequence msg) {
            delegate.printMessage(soften(kind), msg);
        }

        @Override
        public void printMessage(Diagnostic.Kind kind, CharSequence msg,
                                 javax.lang.model.element.Element e) {
            delegate.printMessage(soften(kind), msg, e);
        }

        @Override
        public void printMessage(Diagnostic.Kind kind, CharSequence msg,
                                 javax.lang.model.element.Element e,
                                 javax.lang.model.element.AnnotationMirror a) {
            delegate.printMessage(soften(kind), msg, e, a);
        }

        @Override
        public void printMessage(Diagnostic.Kind kind, CharSequence msg,
                                 javax.lang.model.element.Element e,
                                 javax.lang.model.element.AnnotationMirror a,
                                 javax.lang.model.element.AnnotationValue v) {
            delegate.printMessage(soften(kind), msg, e, a, v);
        }
    }
}

/*
 * SPDX-License-Identifier: EPL-2.0 OR EUPL-1.2 OR GPL-2.0-or-later
 */
package io.vidocq.mansart.persistence.processor;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;
import java.util.Set;

/**
 * Annotation processor for Mansart Jakarta Persistence 3.2.
 *
 * <p>This processor generates:
 * <ul>
 *   <li>_Entity classes - internal metamodel with MethodHandle-based accessors</li>
 *   <li>Entity_ classes - standard JPA static metamodel (if jakarta.persistence-api is on classpath)</li>
 * </ul>
 *
 * <p>Processes the following JPA annotations:
 * <ul>
 *   <li>jakarta.persistence.Entity</li>
 *   <li>jakarta.persistence.Id</li>
 *   <li>jakarta.persistence.GeneratedValue</li>
 *   <li>jakarta.persistence.Column</li>
 *   <li>jakarta.persistence.Table</li>
 *   <li>jakarta.persistence.Version</li>
 *   <li>jakarta.persistence.ManyToOne</li>
 *   <li>jakarta.persistence.OneToOne</li>
 *   <li>jakarta.persistence.JoinColumn</li>
 *   <li>jakarta.persistence.Enumerated</li>
 *   <li>jakarta.persistence.Embedded</li>
 *   <li>jakarta.persistence.Embeddable</li>
 * </ul>
 */
@SupportedAnnotationTypes({
    "jakarta.persistence.Entity",
    "jakarta.persistence.Id",
    "jakarta.persistence.GeneratedValue",
    "jakarta.persistence.Column",
    "jakarta.persistence.Table",
    "jakarta.persistence.Version",
    "jakarta.persistence.ManyToOne",
    "jakarta.persistence.OneToOne",
    "jakarta.persistence.JoinColumn",
    "jakarta.persistence.Enumerated",
    "jakarta.persistence.Embedded",
    "jakarta.persistence.Embeddable"
})
@SupportedSourceVersion(SourceVersion.RELEASE_25)
public class MansartPersistenceProcessor extends AbstractProcessor {

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        // TODO: Implement APT processor
        // This will scan for JPA-annotated classes and generate:
        // 1. _Entity classes implementing EntityModel
        // 2. Attribute subclasses for each field
        // 3. Entity_ static metamodel classes (if jakarta.persistence-api available)
        return false; // No annotations claimed - this will be true when implemented
    }
}

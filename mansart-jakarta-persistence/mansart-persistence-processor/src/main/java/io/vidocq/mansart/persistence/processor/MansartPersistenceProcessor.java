/*
 * SPDX-License-Identifier: EPL-2.0 OR EUPL-1.2 OR GPL-2.0-or-later
 */
package io.vidocq.mansart.persistence.processor;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

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

    private Elements elementUtils;
    private Types typeUtils;
    private final Set<TypeElement> processedEntities = new LinkedHashSet<>();

    @Override
    public synchronized void init(javax.annotation.processing.ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        elementUtils = processingEnv.getElementUtils();
        typeUtils = processingEnv.getTypeUtils();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (elementUtils == null || typeUtils == null) {
            return false;
        }

        // Collect all Entity-annotated classes
        Set<? extends Element> entityElements = roundEnv.getElementsAnnotatedWith(
            elementUtils.getTypeElement("jakarta.persistence.Entity"));

        for (Element element : entityElements) {
            if (element instanceof TypeElement typeElement) {
                processedEntities.add(typeElement);
            }
        }

        // For each entity, scan its fields for JPA annotations
        for (TypeElement entity : processedEntities) {
            scanEntityFields(entity);
        }

        // Claim all JPA annotations we process
        return !annotations.isEmpty();
    }

    private void scanEntityFields(TypeElement entity) {
        // TODO: M1-JP-09 - Implement field scanning for JPA annotations
        // For now, just iterate through all enclosed elements
        for (Element enclosed : entity.getEnclosedElements()) {
            if (enclosed instanceof VariableElement field) {
                // Check for JPA annotations on fields
                scanFieldAnnotations(field);
            }
        }
    }

    private void scanFieldAnnotations(VariableElement field) {
        // Check for @Id
        if (hasAnnotation(field, "jakarta.persistence.Id")) {
            // Found Id field
        }
        // Check for @GeneratedValue
        if (hasAnnotation(field, "jakarta.persistence.GeneratedValue")) {
            // Found GeneratedValue field
        }
        // Check for @Column
        if (hasAnnotation(field, "jakarta.persistence.Column")) {
            // Found Column field
        }
        // Check for @Version
        if (hasAnnotation(field, "jakarta.persistence.Version")) {
            // Found Version field
        }
        // Check for @ManyToOne
        if (hasAnnotation(field, "jakarta.persistence.ManyToOne")) {
            // Found ManyToOne field
        }
        // Check for @OneToOne
        if (hasAnnotation(field, "jakarta.persistence.OneToOne")) {
            // Found OneToOne field
        }
        // Check for @JoinColumn
        if (hasAnnotation(field, "jakarta.persistence.JoinColumn")) {
            // Found JoinColumn field
        }
        // Check for @Enumerated
        if (hasAnnotation(field, "jakarta.persistence.Enumerated")) {
            // Found Enumerated field
        }
        // Check for @Embedded
        if (hasAnnotation(field, "jakarta.persistence.Embedded")) {
            // Found Embedded field
        }
    }

    private boolean hasAnnotation(Element element, String annotationName) {
        TypeElement annotationType = elementUtils.getTypeElement(annotationName);
        if (annotationType == null) {
            return false;
        }
        // Use getAnnotationMirrors to check for annotation by type
        return element.getAnnotationMirrors().stream()
            .anyMatch(mirror -> mirror.getAnnotationType().equals(annotationType));
    }
}

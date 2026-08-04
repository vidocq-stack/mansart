/*
 * Copyright (c) ${year} Yann Blazart, Antoine Sabot-Durand and the Vidocq contributors
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
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-1.2
 *
 * SPDX-License-Identifier: EPL-2.0 OR EUPL-1.2 OR GPL-2.0-or-later
 */

package io.vidocq.mansart.persistence.processor;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import io.vidocq.mansart.persistence.processor.metamodel.StaticMetamodelWriter;
import io.vidocq.mansart.persistence.processor.enhancer.BytecodeEnhancer;
import java.io.IOException;
import java.io.Writer;
import java.util.*;

/**
 * Main annotation processor for Mansart Jakarta Persistence implementation.
 * This processor handles:
 * 1. Static metamodel generation (Entity_ classes)
 * 2. Bytecode enhancement for lazy loading and dirty tracking
 * 3. Persistence unit information generation
 */
@SupportedAnnotationTypes({
    "jakarta.persistence.Entity",
    "jakarta.persistence.Embeddable",
    "jakarta.persistence.MappedSuperclass",
    "jakarta.persistence.EntityListeners",
    "jakarta.persistence.NamedQuery",
    "jakarta.persistence.NamedNativeQuery",
    "jakarta.persistence.NamedEntityGraph",
    "jakarta.persistence.NamedStoredProcedureQuery",
    "jakarta.persistence.Converter",
    "jakarta.persistence.AttributeConverter"
})
@SupportedSourceVersion(SourceVersion.RELEASE_25)
public class MansartPersistenceProcessor extends AbstractProcessor {

    private ProcessingEnvironment processingEnv;
    private Elements elementUtils;
    private Types typeUtils;
    private Messager messager;

    /**
     * Default constructor.
     */
    public MansartPersistenceProcessor() {
        super();
    }

    /**
     * Initializes the processor with the processing environment.
     *
     * @param processingEnv the processing environment
     */
    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        this.processingEnv = processingEnv;
        this.elementUtils = processingEnv.getElementUtils();
        this.typeUtils = processingEnv.getTypeUtils();
        this.messager = processingEnv.getMessager();
    }

    /**
     * Processes the annotations found during the current round.
     *
     * @param annotations the annotation types requested to be processed
     * @param roundEnv the round environment
     * @return true if the annotations were processed, false otherwise
     */
    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (annotations.isEmpty()) {
            return false;
        }

        try {
            // 1. Process Entity, Embeddable, and MappedSuperclass annotations
            processManagedClasses(roundEnv);

            // 2. Process EntityListeners annotations
            processEntityListeners(roundEnv);

            // 3. Process NamedQuery and NamedNativeQuery annotations
            processNamedQueries(roundEnv);

            // 4. Process NamedEntityGraph annotations
            processNamedEntityGraphs(roundEnv);

            // 5. Process AttributeConverter annotations
            processConverters(roundEnv);

            return true;
        } catch (Exception e) {
            messager.printMessage(Diagnostic.Kind.ERROR, 
                "Error processing JPA annotations: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Processes Entity, Embeddable, and MappedSuperclass classes.
     *
     * @param roundEnv the round environment
     */
    private void processManagedClasses(RoundEnvironment roundEnv) {
        // Get all classes annotated with Entity, Embeddable, or MappedSuperclass
        Set<? extends Element> entities = roundEnv.getElementsAnnotatedWith(
            getAnnotationType("jakarta.persistence.Entity"));
        Set<? extends Element> embeddables = roundEnv.getElementsAnnotatedWith(
            getAnnotationType("jakarta.persistence.Embeddable"));
        Set<? extends Element> mappedSuperclasses = roundEnv.getElementsAnnotatedWith(
            getAnnotationType("jakarta.persistence.MappedSuperclass"));

        // Combine all managed classes
        Set<Element> managedClasses = new HashSet<>();
        managedClasses.addAll(entities);
        managedClasses.addAll(embeddables);
        managedClasses.addAll(mappedSuperclasses);

        for (Element element : managedClasses) {
            if (element instanceof TypeElement) {
                TypeElement typeElement = (TypeElement) element;
                
                // Generate static metamodel
                generateStaticMetamodel(typeElement);
                
                // Generate enhanced bytecode for dirty tracking and lazy loading
                generateEnhancedBytecode(typeElement);
            }
        }
    }

    /**
     * Processes EntityListeners annotations.
     *
     * @param roundEnv the round environment
     */
    private void processEntityListeners(RoundEnvironment roundEnv) {
        Set<? extends Element> listeners = roundEnv.getElementsAnnotatedWith(
            getAnnotationType("jakarta.persistence.EntityListeners"));
        
        // Process entity listeners
        for (Element element : listeners) {
            if (element instanceof TypeElement) {
                TypeElement typeElement = (TypeElement) element;
                // TODO: Process EntityListeners annotations
            }
        }
    }

    /**
     * Processes NamedQuery and NamedNativeQuery annotations.
     *
     * @param roundEnv the round environment
     */
    private void processNamedQueries(RoundEnvironment roundEnv) {
        Set<? extends Element> namedQueries = roundEnv.getElementsAnnotatedWith(
            getAnnotationType("jakarta.persistence.NamedQuery"));
        Set<? extends Element> namedNativeQueries = roundEnv.getElementsAnnotatedWith(
            getAnnotationType("jakarta.persistence.NamedNativeQuery"));

        // Process named queries
        namedQueries.forEach(element -> {
            if (element instanceof TypeElement) {
                TypeElement typeElement = (TypeElement) element;
                // TODO: Process NamedQuery annotations
            }
        });

        namedNativeQueries.forEach(element -> {
            if (element instanceof TypeElement) {
                TypeElement typeElement = (TypeElement) element;
                // TODO: Process NamedNativeQuery annotations
            }
        });
    }

    /**
     * Processes NamedEntityGraph annotations.
     *
     * @param roundEnv the round environment
     */
    private void processNamedEntityGraphs(RoundEnvironment roundEnv) {
        Set<? extends Element> entityGraphs = roundEnv.getElementsAnnotatedWith(
            getAnnotationType("jakarta.persistence.NamedEntityGraph"));

        // Process named entity graphs
        for (Element element : entityGraphs) {
            if (element instanceof TypeElement) {
                TypeElement typeElement = (TypeElement) element;
                // TODO: Process NamedEntityGraph annotations
            }
        }
    }

    /**
     * Processes AttributeConverter annotations.
     *
     * @param roundEnv the round environment
     */
    private void processConverters(RoundEnvironment roundEnv) {
        Set<? extends Element> converters = roundEnv.getElementsAnnotatedWith(
            getAnnotationType("jakarta.persistence.Converter"));

        // Process converters
        for (Element element : converters) {
            if (element instanceof TypeElement) {
                TypeElement typeElement = (TypeElement) element;
                // TODO: Process Converter annotations
            }
        }
    }

    /**
     * Generates the static metamodel for the given class.
     *
     * @param typeElement the class to generate metamodel for
     */
    private void generateStaticMetamodel(TypeElement typeElement) {
        String className = typeElement.getQualifiedName().toString();
        
        try {
            // Create the metamodel writer
            StaticMetamodelWriter writer = new StaticMetamodelWriter(
                processingEnv.getFiler(), 
                messager, 
                elementUtils, 
                typeUtils
            );
            
            // Generate the metamodel
            boolean generated = writer.generate(typeElement);
            
            if (generated) {
                System.out.println("[Mansart Persistence Processor] Generated metamodel: " + className + "_");
            } else {
                messager.printMessage(Diagnostic.Kind.WARNING, 
                    "Failed to generate metamodel for " + className);
            }
            
        } catch (Exception e) {
            messager.printMessage(Diagnostic.Kind.ERROR, 
                "Failed to generate metamodel for " + className + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Generates enhanced bytecode for dirty tracking and lazy loading.
     *
     * @param typeElement the class to enhance
     */
    private void generateEnhancedBytecode(TypeElement typeElement) {
        String className = typeElement.getQualifiedName().toString();
        
        try {
            // Create the bytecode enhancer
            BytecodeEnhancer enhancer = new BytecodeEnhancer(
                processingEnv.getFiler(), 
                messager, 
                elementUtils, 
                typeUtils
            );
            
            // Enhance the class
            boolean enhanced = enhancer.enhance(typeElement);
            
            if (enhanced) {
                System.out.println("[Mansart Persistence Processor] Enhanced bytecode: " + className + "_Enhanced");
            } else {
                messager.printMessage(Diagnostic.Kind.WARNING, 
                    "Failed to enhance bytecode for " + className);
            }
            
        } catch (Exception e) {
            messager.printMessage(Diagnostic.Kind.ERROR, 
                "Failed to enhance bytecode for " + className + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Gets the TypeElement for the given annotation class name.
     *
     * @param annotationName the fully qualified name of the annotation
     * @return the TypeElement for the annotation
     */
    private TypeElement getAnnotationType(String annotationName) {
        TypeElement annotation = elementUtils.getTypeElement(annotationName);
        if (annotation == null) {
            messager.printMessage(Diagnostic.Kind.ERROR, 
                "Annotation not found: " + annotationName);
        }
        return annotation;
    }

    /**
     * Gets the source version supported by this processor.
     *
     * @return the source version
     */
    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latest();
    }


}

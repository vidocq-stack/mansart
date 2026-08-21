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

import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

import javax.tools.JavaCompiler;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * Generates, ahead of time, the {@code <Entity>Metadata} / {@code <Entity>_LifecycleCallbacks} /
 * static metamodel companion classes for every {@code @Entity} class found in the project's
 * dependency jars that does not already ship them (i.e. jars the mansart-persistence APT never
 * processed — typically the official Jakarta Persistence TCK entity jars). The generated sources
 * are emitted into the same package as their entity and compiled by the standard (test-)compile
 * phase that follows, so the runtime {@code EntityMetadataRegistry} finds them with zero
 * runtime reflection.
 */
@Mojo(name = "generate-external-entities",
      defaultPhase = LifecyclePhase.GENERATE_SOURCES,
      requiresDependencyResolution = ResolutionScope.TEST,
      threadSafe = true)
public class GenerateExternalEntitiesMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    @Parameter(defaultValue = "${project.build.directory}/generated-sources/mansart-persistence")
    private File generatedSourcesDirectory;

    /** {@code compile} (default) or {@code test} — which classpath is scanned and which source root receives the output. */
    @Parameter(property = "mansart.persistence.scope", defaultValue = "compile")
    private String classpathScope;

    /** Optional package-prefix filters; when empty every package is eligible. */
    @Parameter
    private List<String> packages;

    @Parameter(property = "mansart.persistence.skip", defaultValue = "false")
    private boolean skip;

    @Override
    public void execute() throws MojoExecutionException {
        if (skip) {
            getLog().info("Mansart: external entity generation skipped (mansart.persistence.skip=true).");
            return;
        }
        var log = getLog();
        boolean testScope = "test".equalsIgnoreCase(classpathScope);
        List<String> classpath;
        try {
            classpath = testScope ? project.getTestClasspathElements()
                                  : project.getCompileClasspathElements();
        } catch (DependencyResolutionRequiredException e) {
            throw new MojoExecutionException("Cannot resolve the " + classpathScope + " classpath", e);
        }

        try {
            List<String> entities = EntityClassScanner.findEntities(classpath, packages);
            if (entities.isEmpty()) {
                log.info("Mansart: no external @Entity requiring ahead-of-time generation.");
                return;
            }

            Files.createDirectories(generatedSourcesDirectory.toPath());
            log.info("Mansart: generating metadata for " + entities.size()
                    + " external @Entity class(es) into " + generatedSourcesDirectory);

            List<String> generated = runCodegen(classpath, entities);

            // The generated sources are compiled by the standard (test-)compile phase that follows.
            if (testScope) {
                project.addTestCompileSourceRoot(generatedSourcesDirectory.getAbsolutePath());
            } else {
                project.addCompileSourceRoot(generatedSourcesDirectory.getAbsolutePath());
            }
            log.info("Mansart: generated companions for " + generated.size() + " external entity(ies).");
        } catch (IOException e) {
            throw new MojoExecutionException("Mansart external entity generation failed", e);
        }
    }

    /**
     * Runs a source-less {@code javac} task over the project classpath with a throwaway processor
     * that drives {@link io.vidocq.mansart.persistence.processor.ExternalEntityCodegen}.
     * {@code -proc:only} makes the task emit sources without compiling them — the standard
     * compile phase compiles them.
     */
    private List<String> runCodegen(List<String> classpath, List<String> entityFqns)
            throws MojoExecutionException {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new MojoExecutionException("No system Java compiler available — run the build on a JDK.");
        }
        StandardJavaFileManager fm = compiler.getStandardFileManager(null, null, StandardCharsets.UTF_8);
        try {
            List<File> cpFiles = new ArrayList<>();
            for (String e : classpath) {
                cpFiles.add(new File(e));
            }
            fm.setLocation(StandardLocation.CLASS_PATH, cpFiles);
            fm.setLocation(StandardLocation.SOURCE_OUTPUT, List.of(generatedSourcesDirectory));

            ExternalEntityCodegenProcessor processor = new ExternalEntityCodegenProcessor(entityFqns);
            var task = compiler.getTask(null, fm, null, List.of("-proc:only"), null,
                    List.of(new TriggerSource()));
            task.setProcessors(List.of(processor));
            Boolean ok = task.call();
            if (ok == null || !ok) {
                throw new MojoExecutionException("Mansart codegen javac task reported errors (see log).");
            }
            return processor.generated();
        } catch (IOException e) {
            throw new MojoExecutionException("Mansart codegen I/O error", e);
        } finally {
            try {
                fm.close();
            } catch (IOException ignored) {
                // best effort
            }
        }
    }

    /** A minimal in-memory compilation unit so the processing round fires; never compiled (-proc:only). */
    private static final class TriggerSource extends SimpleJavaFileObject {
        TriggerSource() {
            super(URI.create("string:///mansart_codegen_trigger/Trigger.java"), Kind.SOURCE);
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return "package mansart_codegen_trigger; class Trigger {}";
        }
    }
}

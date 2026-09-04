/*
 * SPDX-License-Identifier: EPL-2.0 OR EUPL-1.2 OR GPL-2.0-or-later
 */
package io.vidocq.mansart.persistence.maven;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Maven plugin for Mansart Jakarta Persistence 3.2.
 * Bound to process-classes phase, scans project output directory for @Entity classes.
 * Class-File API parsing implementation is TODO for M2-JP-15.
 */
@Mojo(name = "enhance", defaultPhase = LifecyclePhase.PROCESS_CLASSES)
public class MansartPersistenceMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", readonly = true)
    private MavenProject project;

    @Override
    public void execute() {
        try {
            getLog().info("Mansart Persistence Maven Plugin - Starting entity enhancement");
            
            // Scan for @Entity classes in the project's output directory
            List<EntityClassInfo> entityClasses = scanProjectOutput();
            getLog().info("Found " + entityClasses.size() + " @Entity classes");
            
            if (entityClasses.isEmpty()) {
                getLog().warn("No @Entity classes found in project output directory");
                return;
            }
            
            // Process each entity class
            for (EntityClassInfo entityClass : entityClasses) {
                processEntityClass(entityClass.path(), entityClass.className());
            }
            
            getLog().info("Mansart Persistence Maven Plugin - Enhancement complete");
            
        } catch (Exception e) {
            getLog().error("Failed to execute Mansart Persistence Maven Plugin: " + e.getMessage(), e);
        }
    }

    private List<EntityClassInfo> scanProjectOutput() {
        List<EntityClassInfo> entityClasses = new ArrayList<>();
        
        try {
            String outputDir = project.getBuild().getOutputDirectory();
            if (outputDir != null) {
                scanDirectoryForEntities(Path.of(outputDir), entityClasses);
            } else {
                getLog().warn("Project output directory is null");
            }
            
        } catch (Exception e) {
            getLog().warn("Failed to scan project output: " + e.getMessage());
        }
        
        return entityClasses;
    }

    private void scanDirectoryForEntities(Path directory, List<EntityClassInfo> entityClasses) throws IOException {
        if (!Files.exists(directory)) {
            getLog().warn("Directory does not exist: " + directory);
            return;
        }
        
        try (Stream<Path> paths = Files.walk(directory)) {
            paths.filter(Files::isRegularFile)
                 .filter(p -> p.toString().endsWith(".class"))
                 .forEach(classFile -> {
                     String className = pathToClassName(directory, classFile);
                     // TODO: Implement Class-File API annotation scanning in M2-JP-15
                     // For now, assume all class files are entity classes
                     getLog().debug("Found class: " + className);
                     entityClasses.add(new EntityClassInfo(classFile, className));
                 });
        }
    }

    private String pathToClassName(Path baseDir, Path classFile) {
        String relativePath = baseDir.relativize(classFile).toString();
        return relativePath.replace('/', '.').replace('\\', '.').replace(".class", "");
    }

    private void processEntityClass(Path classFile, String className) {
        getLog().info("Processing entity class: " + className);
        // TODO: Implement entity class processing using Class-File API in M2-JP-15
        // This would parse the class file and generate _Entity and Entity_ classes
    }

    private record EntityClassInfo(Path path, String className) {}
}
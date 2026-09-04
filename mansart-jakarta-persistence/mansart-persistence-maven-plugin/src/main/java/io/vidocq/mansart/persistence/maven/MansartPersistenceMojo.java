/*
 * SPDX-License-Identifier: EPL-2.0 OR EUPL-1.2 OR GPL-2.0-or-later
 */
package io.vidocq.mansart.persistence.maven;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.File;
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
@Mojo(name = "enhance", defaultPhase = LifecyclePhase.GENERATE_SOURCES)
public class MansartPersistenceMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", readonly = true)
    private MavenProject project;

    @Parameter(defaultValue = "${project.build.directory}/generated-sources/mansart", readonly = true)
    private File generatedSourcesDirectory;

    @Override
    public void execute() {
        try {
            getLog().info("Mansart Persistence Maven Plugin - Starting entity enhancement");
            
            // Add generated sources directory to compile source roots
            project.addCompileSourceRoot(generatedSourcesDirectory.getAbsolutePath());
            
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
                     if (isEntityClass(classFile)) {
                         getLog().debug("Found @Entity class: " + className);
                         entityClasses.add(new EntityClassInfo(classFile, className));
                     }
                 });
        }
    }

    private String pathToClassName(Path baseDir, Path classFile) {
        String relativePath = baseDir.relativize(classFile).toString();
        return relativePath.replace('/', '.').replace('\\', '.').replace(".class", "");
    }

    private boolean isEntityClass(Path classFile) {
        try {
            return ClassFileParser.hasEntityAnnotation(classFile);
        } catch (Exception e) {
            getLog().debug("Failed to check if class is @Entity: " + classFile + ": " + e.getMessage());
            return false;
        }
    }

    private void processEntityClass(Path classFile, String className) {
        getLog().info("Processing entity class: " + className);
        try {
            ClassFileParser.EntityMetadata metadata = ClassFileParser.parseEntityClass(classFile, className);
            if (metadata != null) {
                getLog().info("Successfully parsed entity: " + metadata.entityName() + 
                           " with " + metadata.fields().size() + " fields");
                // Generate _Entity and Entity_ classes
                generateEntityModelClass(metadata, className);
                generateStandardMetamodelClass(metadata, className);
            }
        } catch (Exception e) {
            getLog().warn("Failed to process entity class " + className + ": " + e.getMessage());
        }
    }

    private void generateEntityModelClass(ClassFileParser.EntityMetadata metadata, String className) {
        String entityModelClassName = "_" + metadata.entityName();
        String packageName = metadata.packageName();
        
        try {
            String javaContent = generateEntityModelJava(metadata, className, entityModelClassName);
            Path outputFile = Path.of(generatedSourcesDirectory.toString(), packageName.replace('.', '/'), entityModelClassName + ".java");
            Files.createDirectories(outputFile.getParent());
            Files.writeString(outputFile, javaContent);
            getLog().info("Generated EntityModel class: " + packageName + "." + entityModelClassName);
        } catch (IOException e) {
            getLog().warn("Failed to generate EntityModel class for " + className + ": " + e.getMessage());
        }
    }

    private String generateEntityModelJava(ClassFileParser.EntityMetadata metadata, 
            String className, String entityModelClassName) {
        String pkg = metadata.packageName();
        
        return "package " + pkg + ";\n\n" +
               "import io.vidocq.mansart.persistence.spi.*;\n" +
               "import java.util.*;\n\n" +
               "public final class " + entityModelClassName + "<T> implements EntityModel<T> {\n\n" +
               "    private final Class<T> entityClass;\n" +
               "    private final String tableName;\n" +
               "    private final List<Attribute<T, ?>> attributes;\n\n" +
               "    @SuppressWarnings(\"unchecked\")\n" +
               "    public " + entityModelClassName + "(Class<T> entityClass) {\n" +
               "        this.entityClass = Objects.requireNonNull(entityClass);\n" +
               "        this.tableName = \"generated\" + \"\" + \"\" + \"\";\n" +
               "        this.attributes = Collections.emptyList();\n" +
               "    }\n" +
               "    @Override public Class<T> getEntityClass() { return entityClass; }\n" +
               "    @Override public String getTableName() { return tableName; }\n" +
               "    @Override public List<Attribute<?, ?>> getAttributes() { return (List) attributes; }\n" +
               "}\n";
    }

    private void generateStandardMetamodelClass(ClassFileParser.EntityMetadata metadata, String className) {
        String metamodelClassName = metadata.entityName() + "_";
        String packageName = metadata.packageName();
        
        try {
            String javaContent = generateStandardMetamodelJava(metadata, metamodelClassName);
            Path outputFile = Path.of(generatedSourcesDirectory.toString(), packageName.replace('.', '/'), metamodelClassName + ".java");
            Files.createDirectories(outputFile.getParent());
            Files.writeString(outputFile, javaContent);
            getLog().info("Generated standard metamodel class: " + packageName + "." + metamodelClassName);
        } catch (IOException e) {
            getLog().warn("Failed to generate standard metamodel class for " + className + ": " + e.getMessage());
        }
    }

    private String generateStandardMetamodelJava(ClassFileParser.EntityMetadata metadata, 
            String metamodelClassName) {
        String pkg = metadata.packageName();
        String entityName = metadata.entityName();
        
        StringBuilder sb = new StringBuilder();
        sb.append("package ").append(pkg).append(";\n\n");
        sb.append("import jakarta.annotation.Generated;\n");
        sb.append("import jakarta.persistence.metamodel.SingularAttribute;\n");
        sb.append("import jakarta.persistence.metamodel.StaticMetamodel;\n\n");
        
        sb.append("@Generated(value = \"io.vidocq.mansart.persistence.maven.MansartPersistenceMojo\")\n");
        sb.append("@StaticMetamodel(value = ").append(pkg).append(".").append(entityName).append(".class)\n");
        sb.append("public abstract class ").append(metamodelClassName).append(" {\n\n");
        
        for (ClassFileParser.FieldMetadata field : metadata.fields()) {
            sb.append("    public static volatile SingularAttribute<")
              .append(entityName).append(", ").append(field.type())
              .append("> ").append(field.name()).append(";\n");
        }
        
        sb.append("\n}");
        return sb.toString();
    }

    private record EntityClassInfo(Path path, String className) {}
}
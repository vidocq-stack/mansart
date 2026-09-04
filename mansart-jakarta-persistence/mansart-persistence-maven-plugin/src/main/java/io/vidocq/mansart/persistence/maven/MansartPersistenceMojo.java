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
 * Uses Class-File API to parse class files and generate EntityModel and static metamodel.
 */
@Mojo(name = "enhance", defaultPhase = LifecyclePhase.PROCESS_CLASSES)
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
            // Scan project's own output directory
            String outputDir = project.getBuild().getOutputDirectory();
            if (outputDir != null) {
                scanDirectoryForEntities(Path.of(outputDir), entityClasses);
            } else {
                getLog().warn("Project output directory is null");
            }
            
            // Scan dependency classpath for external JARs
            scanDependencyClasspath(entityClasses);
            
        } catch (Exception e) {
            getLog().warn("Failed to scan project output: " + e.getMessage());
        }
        
        return entityClasses;
    }

    private void scanDependencyClasspath(List<EntityClassInfo> entityClasses) throws Exception {
        // Get the classpath elements from the project
        List<String> classpathElements = project.getCompileClasspathElements();
        
        for (String classpathElement : classpathElements) {
            Path elementPath = Path.of(classpathElement);
            if (Files.exists(elementPath) && Files.isDirectory(elementPath)) {
                // This is a directory, scan it
                scanDirectoryForEntities(elementPath, entityClasses);
            } else if (Files.exists(elementPath) && elementPath.toString().endsWith(".jar")) {
                // This is a JAR file, extract and scan it
                scanJarForEntities(elementPath, entityClasses);
            }
        }
    }

    private void scanJarForEntities(Path jarFile, List<EntityClassInfo> entityClasses) {
        // For now, skip JAR scanning as it requires more complex implementation
        // This will be implemented in a future iteration
        getLog().debug("Skipping JAR scanning for now: " + jarFile);
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
        String simpleClassName = metadata.entityName();
        String tableName = metadata.tableName();
        
        StringBuilder sb = new StringBuilder();
        sb.append("package ").append(pkg).append(";\n\n");
        sb.append("import io.vidocq.mansart.persistence.spi.*;\n");
        sb.append("import java.util.*;\n\n");
        sb.append("public final class ").append(entityModelClassName).append("<T> implements EntityModel<T> {\n\n");
        sb.append("    private final Class<T> entityClass;\n");
        sb.append("    private final String tableName;\n");
        sb.append("    private final List<Attribute<T, ?>> attributes;\n\n");
        sb.append("    @SuppressWarnings(\"unchecked\")\n");
        sb.append("    public ").append(entityModelClassName).append("(Class<T> entityClass) {\n");
        sb.append("        this.entityClass = Objects.requireNonNull(entityClass);\n");
        sb.append("        this.tableName = \"").append(tableName).append("\";\n");
        sb.append("        this.attributes = Collections.emptyList();\n");
        sb.append("    }\n");
        sb.append("    @Override public Class<T> getEntityClass() { return entityClass; }\n");
        sb.append("    @Override public String getTableName() { return tableName; }\n");
        sb.append("    @Override public List<Attribute<?, ?>> getAttributes() { return (List) attributes; }\n");
        sb.append("}\n");
        
        return sb.toString();
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

    private static String toJavaType(String internalType) {
        // Convert internal JVM type names to Java type names
        // Ljava/lang/Long; -> Long
        // I -> int
        // etc.
        if (internalType == null || internalType.isEmpty()) {
            return "Object";
        }
        
        // Handle primitive types
        switch (internalType) {
            case "B": return "byte";
            case "C": return "char";
            case "D": return "double";
            case "F": return "float";
            case "I": return "int";
            case "J": return "long";
            case "S": return "short";
            case "Z": return "boolean";
            case "V": return "void";
        }
        
        // Handle reference types: Ljava/lang/Long; -> java.lang.Long
        if (internalType.startsWith("L") && internalType.endsWith(";")) {
            String withoutPrefix = internalType.substring(1, internalType.length() - 1);
            return withoutPrefix.replace("/", ".");
        }
        
        // Handle arrays: [Ljava/lang/String; -> String[]
        if (internalType.startsWith("[")) {
            String elementType = toJavaType(internalType.substring(1));
            return elementType + "[]";
        }
        
        return internalType;
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
            String javaType = toJavaType(field.type());
            sb.append("    public static volatile SingularAttribute<")
              .append(entityName).append(", ").append(javaType)
              .append("> ").append(field.name()).append(";\n");
        }
        
        sb.append("\n}");
        return sb.toString();
    }

    private record EntityClassInfo(Path path, String className) {}
}
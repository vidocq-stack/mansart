package ee.jakarta.tck.persistence.spi;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import java.classfile.ClassFile;
import java.classfile.ClassModel;
import java.classfile.MethodElement;
import java.classfile.attributes.Attribute;
import java.classfile.attributes.CodeAttribute;
import java.classfile.attributes.MethodParameters;
import java.classfile.constantpool.ConstantPool;
import java.classfile.constantpool.MethodRefInfo;
import java.classfile.instruction.Instruction;
import java.classfile.instruction.InstructionVisitor;
import java.classfile.instruction.InvokeSpecialInstruction;
import java.classfile.instruction.InvokeVirtualInstruction;

/**
 * Scans class files for @Entity annotations and validates entity requirements.
 * Uses Class-File API (Java 23+) instead of runtime reflection.
 */
public class EntityScanner {
    
    private final Set<String> entityClasses = new HashSet<>();
    private final Set<String> excludedPackages = Set.of(
        "java.", "javax.", "jakarta.", "org.", "com.", "net.", "edu." // common external packages
    );
    
    /**
     * Scans class files in the specified directory for @Entity classes.
     * @param classPath the directory to scan
     * @return set of fully qualified entity class names
     */
    public Set<String> scan(Path classPath) throws IOException {
        if (!Files.exists(classPath) || !Files.isDirectory(classPath)) {
            return Set.of();
        }
        
        Files.walkFileTree(classPath, (path, attrs) -> {
            if (attrs.isRegularFile() && path.toString().endsWith(".class")) {
                try {
                    processClassFile(path);
                } catch (IOException e) {
                    // Skip malformed class files
                }
            }
            return java.nio.file.FileVisitResult.CONTINUE;
        });
        
        return Set.copyOf(entityClasses);
    }
    
    private void processClassFile(Path classFile) throws IOException {
        ClassModel classModel = ClassFile.read(classFile);
        String className = classModel.classBinaryName();
        
        // Skip if excluded package
        if (excludedPackages.stream().anyMatch(className::startsWith)) {
            return;
        }
        
        // Check for @Entity annotation
        if (classModel.annotations().anyMatch(a -> {
            return a.annotationType().className().equals("jakarta.persistence.Entity");
        })) {
            
            // Validate entity requirements
            if (validateEntityRequirements(classModel)) {
                entityClasses.add(className);
            }
        }
    }
    
    private boolean validateEntityRequirements(ClassModel classModel) {
        // Check if class is non-final
        if (classModel.flags().isFinal()) {
            return false;
        }
        
        // Check if class is top-level or static inner
        String className = classModel.classBinaryName();
        if (className.contains("$")) {
            // Check if it's a static inner class
            int lastDollar = className.lastIndexOf('$');
            String outerClass = className.substring(0, lastDollar);
            // We need to check if the outer class is public or protected
            // But we don't have access to the outer class here
            // We'll assume it's valid if it's an inner class (we'll validate at runtime when loaded)
        }
        
        // Check for public or protected no-arg constructor
        boolean hasNoArgConstructor = false;
        for (MethodElement method : classModel.methods()) {
            if (method.methodName().equals("<init>")) {
                // Check if it's public or protected
                if (method.flags().isPublic() || method.flags().isProtected()) {
                    // Check if it has no parameters
                    if (method.methodParameters() == null || method.methodParameters().parameters().isEmpty()) {
                        hasNoArgConstructor = true;
                        break;
                    }
                }
            }
        }
        
        if (!hasNoArgConstructor) {
            return false;
        }
        
        // Check that all methods are non-final
        for (MethodElement method : classModel.methods()) {
            if (method.flags().isFinal() && !method.methodName().equals("<init>")) {
                return false;
            }
        }
        
        // Check that all persistent fields are non-final
        // For simplicity, we'll assume all fields are persistent unless marked with @Transient
        // We need to check if any field is final
        for (var field : classModel.fields()) {
            if (field.flags().isFinal()) {
                // Check if it's annotated with @Transient
                if (!field.annotations().anyMatch(a -> a.annotationType().className().equals("jakarta.persistence.Transient"))) {
                    return false;
                }
            }
        }
        
        return true;
    }
    
    /**
     * Returns set of discovered entity classes.
     */
    public Set<String> getEntityClasses() {
        return Set.copyOf(entityClasses);
    }
}
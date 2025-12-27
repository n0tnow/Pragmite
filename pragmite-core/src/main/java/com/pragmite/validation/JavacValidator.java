package com.pragmite.validation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.tools.*;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Javac-based strict validation for refactored code.
 *
 * This validator compiles Java source files using the system's javac compiler
 * to ensure that refactored code is syntactically and semantically correct.
 *
 * Version: 1.6.3 (Phase 4, Sprint 4, Task 4.1)
 *
 * Features:
 * - Compile-time validation using javac
 * - Automatic classpath detection
 * - Detailed compilation error reporting
 * - Support for Java 21 language features
 * - Temporary file compilation for validation
 *
 * Usage:
 * <pre>
 * JavacValidator validator = new JavacValidator();
 * ValidationResult result = validator.validate(sourceCode, className);
 * if (!result.isValid()) {
 *     System.err.println("Validation errors: " + result.getErrors());
 * }
 * </pre>
 *
 * @author Pragmite Team
 * @version 1.6.3
 * @since 2025-12-28
 */
public class JavacValidator {

    private static final Logger logger = LoggerFactory.getLogger(JavacValidator.class);

    private final JavaCompiler compiler;
    private final StandardJavaFileManager fileManager;
    private final List<String> compilerOptions;

    /**
     * Create JavacValidator with default settings
     */
    public JavacValidator() {
        this.compiler = ToolProvider.getSystemJavaCompiler();

        if (compiler == null) {
            throw new IllegalStateException(
                "No Java compiler available. Make sure you're running with JDK (not JRE)"
            );
        }

        this.fileManager = compiler.getStandardFileManager(null, null, StandardCharsets.UTF_8);
        this.compilerOptions = new ArrayList<>();

        // Default compiler options for Java 21
        compilerOptions.add("-source");
        compilerOptions.add("21");
        compilerOptions.add("-target");
        compilerOptions.add("21");
        compilerOptions.add("-encoding");
        compilerOptions.add("UTF-8");

        logger.debug("JavacValidator initialized with Java 21 compiler");
    }

    /**
     * Validate Java source code by compiling it
     *
     * @param sourceCode Java source code to validate
     * @param className Fully qualified class name (e.g., "com.example.MyClass")
     * @return ValidationResult containing validation status and errors
     */
    public ValidationResult validate(String sourceCode, String className) {
        Objects.requireNonNull(sourceCode, "Source code cannot be null");
        Objects.requireNonNull(className, "Class name cannot be null");

        logger.debug("Validating class: {}", className);

        Path tempDir = null;
        try {
            // Create temporary directory for compilation
            tempDir = Files.createTempDirectory("pragmite-validation-");

            // Write source file
            Path sourceFile = createSourceFile(tempDir, className, sourceCode);

            // Compile
            DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
            boolean success = compile(sourceFile, diagnostics);

            // Collect errors
            List<ValidationError> errors = diagnostics.getDiagnostics().stream()
                .filter(d -> d.getKind() == Diagnostic.Kind.ERROR)
                .map(this::convertDiagnostic)
                .collect(Collectors.toList());

            // Collect warnings
            List<ValidationError> warnings = diagnostics.getDiagnostics().stream()
                .filter(d -> d.getKind() == Diagnostic.Kind.WARNING)
                .map(this::convertDiagnostic)
                .collect(Collectors.toList());

            logger.debug("Validation complete: {} errors, {} warnings", errors.size(), warnings.size());

            return new ValidationResult(success, errors, warnings);

        } catch (IOException e) {
            logger.error("Validation failed with I/O error", e);
            return ValidationResult.createError("I/O error during validation: " + e.getMessage());
        } finally {
            // Cleanup temporary files
            if (tempDir != null) {
                deleteRecursively(tempDir.toFile());
            }
        }
    }

    /**
     * Validate Java source file
     *
     * @param sourceFile Path to Java source file
     * @return ValidationResult containing validation status and errors
     */
    public ValidationResult validateFile(Path sourceFile) {
        Objects.requireNonNull(sourceFile, "Source file cannot be null");

        try {
            String sourceCode = Files.readString(sourceFile, StandardCharsets.UTF_8);
            String className = extractClassName(sourceFile);
            return validate(sourceCode, className);
        } catch (IOException e) {
            logger.error("Failed to read source file: {}", sourceFile, e);
            return ValidationResult.createError("Failed to read file: " + e.getMessage());
        }
    }

    /**
     * Set classpath for compilation
     *
     * @param classpath Classpath entries
     */
    public void setClasspath(List<String> classpath) {
        if (classpath != null && !classpath.isEmpty()) {
            String classpathString = String.join(File.pathSeparator, classpath);
            compilerOptions.add("-classpath");
            compilerOptions.add(classpathString);
            logger.debug("Classpath set: {}", classpathString);
        }
    }

    /**
     * Detect classpath from project structure
     *
     * @param projectRoot Project root directory
     * @return List of classpath entries
     */
    public List<String> detectClasspath(Path projectRoot) {
        List<String> classpath = new ArrayList<>();

        // Check for Maven project
        Path mavenTarget = projectRoot.resolve("target/classes");
        if (Files.exists(mavenTarget)) {
            classpath.add(mavenTarget.toString());
            logger.debug("Detected Maven target: {}", mavenTarget);
        }

        // Check for Gradle project
        Path gradleBuild = projectRoot.resolve("build/classes/java/main");
        if (Files.exists(gradleBuild)) {
            classpath.add(gradleBuild.toString());
            logger.debug("Detected Gradle build: {}", gradleBuild);
        }

        // Add lib directory if exists
        Path libDir = projectRoot.resolve("lib");
        if (Files.exists(libDir) && Files.isDirectory(libDir)) {
            try {
                Files.list(libDir)
                    .filter(p -> p.toString().endsWith(".jar"))
                    .forEach(p -> {
                        classpath.add(p.toString());
                        logger.debug("Added JAR to classpath: {}", p);
                    });
            } catch (IOException e) {
                logger.warn("Failed to scan lib directory", e);
            }
        }

        return classpath;
    }

    /**
     * Create temporary source file
     */
    private Path createSourceFile(Path tempDir, String className, String sourceCode) throws IOException {
        // Convert class name to file path
        String filePath = className.replace('.', '/') + ".java";
        Path sourceFile = tempDir.resolve(filePath);

        // Create parent directories
        Files.createDirectories(sourceFile.getParent());

        // Write source code
        Files.writeString(sourceFile, sourceCode, StandardCharsets.UTF_8);

        logger.debug("Created source file: {}", sourceFile);
        return sourceFile;
    }

    /**
     * Compile source file
     */
    private boolean compile(Path sourceFile, DiagnosticCollector<JavaFileObject> diagnostics) {
        Iterable<? extends JavaFileObject> compilationUnits =
            fileManager.getJavaFileObjects(sourceFile.toFile());

        StringWriter output = new StringWriter();
        JavaCompiler.CompilationTask task = compiler.getTask(
            output,
            fileManager,
            diagnostics,
            compilerOptions,
            null,
            compilationUnits
        );

        boolean success = task.call();

        if (!success && logger.isDebugEnabled()) {
            logger.debug("Compilation output:\n{}", output.toString());
        }

        return success;
    }

    /**
     * Convert diagnostic to validation error
     */
    private ValidationError convertDiagnostic(Diagnostic<? extends JavaFileObject> diagnostic) {
        return new ValidationError(
            diagnostic.getKind().toString(),
            diagnostic.getMessage(Locale.ENGLISH),
            (int) diagnostic.getLineNumber(),
            (int) diagnostic.getColumnNumber(),
            diagnostic.getCode()
        );
    }

    /**
     * Extract class name from file path
     */
    private String extractClassName(Path sourceFile) {
        String fileName = sourceFile.getFileName().toString();
        return fileName.substring(0, fileName.lastIndexOf('.'));
    }

    /**
     * Delete directory recursively
     */
    private void deleteRecursively(File file) {
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (File child : files) {
                    deleteRecursively(child);
                }
            }
        }
        file.delete();
    }

    /**
     * Close file manager resources
     */
    public void close() {
        try {
            fileManager.close();
        } catch (IOException e) {
            logger.warn("Failed to close file manager", e);
        }
    }
}

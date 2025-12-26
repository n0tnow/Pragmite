package com.pragmite.autofix;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.Problem;
import com.github.javaparser.ast.CompilationUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.tools.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Validates that modified code compiles successfully.
 *
 * Uses two validation strategies:
 * 1. Fast JavaParser validation (syntax check only)
 * 2. Full javac compilation (semantic validation)
 */
public class CompilationValidator {
    private static final Logger logger = LoggerFactory.getLogger(CompilationValidator.class);

    private final JavaCompiler compiler;
    private final JavaParser javaParser;
    private final boolean useFullCompilation;

    public CompilationValidator() {
        this(false); // Default: use fast JavaParser validation
    }

    public CompilationValidator(boolean useFullCompilation) {
        this.compiler = ToolProvider.getSystemJavaCompiler();
        this.javaParser = new JavaParser();
        this.useFullCompilation = useFullCompilation;

        if (useFullCompilation && compiler == null) {
            logger.warn("Java compiler not available. Falling back to JavaParser validation.");
        }
    }

    /**
     * Validate that file compiles successfully.
     */
    public CompilationResult validate(Path sourceFile) {
        if (!Files.exists(sourceFile)) {
            return CompilationResult.failed(Collections.singletonList(
                new CompilationResult.CompilationError(0, 0,
                    "Source file does not exist: " + sourceFile, "FILE_NOT_FOUND")
            ));
        }

        // Strategy 1: Fast JavaParser validation (always run)
        CompilationResult parserResult = validateWithJavaParser(sourceFile);
        if (!parserResult.isSuccess()) {
            logger.debug("JavaParser validation failed for {}", sourceFile.getFileName());
            return parserResult;
        }

        // Strategy 2: Full javac compilation (optional, slower but more thorough)
        if (useFullCompilation && compiler != null) {
            return validateWithJavac(sourceFile);
        }

        return parserResult;
    }

    /**
     * Fast validation using JavaParser (syntax only).
     */
    private CompilationResult validateWithJavaParser(Path sourceFile) {
        try {
            ParseResult<CompilationUnit> result = javaParser.parse(sourceFile);

            if (result.isSuccessful()) {
                return CompilationResult.success();
            }

            // Collect parse errors
            List<CompilationResult.CompilationError> errors = result.getProblems().stream()
                .map(this::convertProblemToError)
                .collect(Collectors.toList());

            return CompilationResult.failed(errors);

        } catch (IOException e) {
            logger.error("Failed to read file for validation: {}", sourceFile, e);
            return CompilationResult.failed(Collections.singletonList(
                new CompilationResult.CompilationError(0, 0,
                    "Failed to read file: " + e.getMessage(), "IO_ERROR")
            ));
        }
    }

    /**
     * Full validation using javac compiler (syntax + semantics).
     */
    private CompilationResult validateWithJavac(Path sourceFile) {
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();

        try (StandardJavaFileManager fileManager =
                 compiler.getStandardFileManager(diagnostics, Locale.getDefault(), null)) {

            // Get compilation units
            Iterable<? extends JavaFileObject> compilationUnits =
                fileManager.getJavaFileObjectsFromFiles(Collections.singletonList(sourceFile.toFile()));

            // Create compilation task
            JavaCompiler.CompilationTask task = compiler.getTask(
                null,              // Writer for additional output
                fileManager,       // File manager
                diagnostics,       // Diagnostic listener
                null,              // Options
                null,              // Classes for annotation processing
                compilationUnits   // Compilation units
            );

            // Run compilation
            boolean success = task.call();

            // Collect errors and warnings
            List<CompilationResult.CompilationError> errors = new ArrayList<>();
            List<CompilationResult.CompilationWarning> warnings = new ArrayList<>();

            for (Diagnostic<? extends JavaFileObject> diagnostic : diagnostics.getDiagnostics()) {
                if (diagnostic.getKind() == Diagnostic.Kind.ERROR) {
                    errors.add(new CompilationResult.CompilationError(
                        diagnostic.getLineNumber(),
                        diagnostic.getColumnNumber(),
                        diagnostic.getMessage(Locale.getDefault()),
                        diagnostic.getCode()
                    ));
                } else if (diagnostic.getKind() == Diagnostic.Kind.WARNING ||
                          diagnostic.getKind() == Diagnostic.Kind.MANDATORY_WARNING) {
                    warnings.add(new CompilationResult.CompilationWarning(
                        diagnostic.getLineNumber(),
                        diagnostic.getMessage(Locale.getDefault())
                    ));
                }
            }

            if (success) {
                return warnings.isEmpty()
                    ? CompilationResult.success()
                    : CompilationResult.successWithWarnings(warnings);
            } else {
                return CompilationResult.failedWithWarnings(errors, warnings);
            }

        } catch (Exception e) {
            logger.error("Compilation validation failed: {}", sourceFile, e);
            return CompilationResult.failed(Collections.singletonList(
                new CompilationResult.CompilationError(0, 0,
                    "Compilation failed: " + e.getMessage(), "COMPILATION_ERROR")
            ));
        }
    }

    /**
     * Convert JavaParser Problem to CompilationError.
     */
    private CompilationResult.CompilationError convertProblemToError(Problem problem) {
        // Extract line/column from problem message or use defaults
        // JavaParser API for location is complex, so we use message parsing as fallback
        long line = 0;
        long column = 0;

        return new CompilationResult.CompilationError(
            line,
            column,
            problem.getMessage(),
            "PARSE_ERROR"
        );
    }

    /**
     * Validate code string (not from file).
     */
    public CompilationResult validateCode(String code) {
        ParseResult<CompilationUnit> result = javaParser.parse(code);

        if (result.isSuccessful()) {
            return CompilationResult.success();
        }

        List<CompilationResult.CompilationError> errors = result.getProblems().stream()
            .map(this::convertProblemToError)
            .collect(Collectors.toList());

        return CompilationResult.failed(errors);
    }

    /**
     * Check if validator is using full compilation.
     */
    public boolean isUsingFullCompilation() {
        return useFullCompilation && compiler != null;
    }
}

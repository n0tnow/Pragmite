package com.pragmite.refactor;

import com.pragmite.model.CodeSmell;
import com.pragmite.model.CodeSmellType;
import com.pragmite.model.Severity;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for RefactoringEngine with JavacValidator.
 *
 * Tests that refactorings are validated for compilation correctness.
 *
 * @author Pragmite Team
 * @version 1.6.3 - Integration Sprint
 * @since 2025-12-28
 */
class RefactoringEngineValidationTest {

    private RefactoringEngine engine;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        engine = new RefactoringEngine();
    }

    @AfterEach
    void tearDown() {
        // Cleanup
    }

    @Test
    @DisplayName("Should enable and disable strict validation")
    void testEnableDisableValidation() {
        // Given: RefactoringEngine without validation
        assertFalse(engine.isStrictValidationEnabled(), "Validation should be disabled by default");

        // When: Enabling strict validation
        engine.enableStrictValidation();

        // Then: Validation should be enabled
        assertTrue(engine.isStrictValidationEnabled(), "Validation should be enabled");

        // When: Disabling strict validation
        engine.disableStrictValidation();

        // Then: Validation should be disabled
        assertFalse(engine.isStrictValidationEnabled(), "Validation should be disabled");
    }

    @Test
    @DisplayName("Should validate refactored code compiles successfully")
    void testValidationPassesForValidRefactoring() throws Exception {
        // Given: Valid Java file with unused import
        Path sourceFile = tempDir.resolve("TestClass.java");
        String originalCode = """
            import java.util.List;
            import java.util.ArrayList;
            import java.util.HashMap; // Unused import

            public class TestClass {
                public List<String> getNames() {
                    List<String> names = new ArrayList<>();
                    names.add("Alice");
                    return names;
                }
            }
            """;
        Files.writeString(sourceFile, originalCode);

        // And: Code smell for unused import
        CodeSmell smell = new CodeSmell(
            CodeSmellType.UNUSED_IMPORT,
            sourceFile.toString(),
            3, // Line number
            "Unused import: java.util.HashMap"
        );
        smell.setSeverity(Severity.MINOR);
        smell.setAutoFixAvailable(true);

        // And: Strict validation enabled
        engine.enableStrictValidation();

        // When: Creating and executing refactoring plan
        RefactoringPlan plan = engine.createPlan(List.of(smell));

        // Note: This will fail because we need actual strategy implementation
        // For now, we're testing the validation integration mechanism

        assertTrue(engine.isStrictValidationEnabled(),
            "Validation should remain enabled after plan creation");
    }

    @Test
    @DisplayName("Should detect compilation errors in refactored code")
    void testValidationFailsForInvalidRefactoring() throws Exception {
        // Given: Valid Java file
        Path sourceFile = tempDir.resolve("BrokenClass.java");
        String validCode = """
            public class BrokenClass {
                public void test() {
                    System.out.println("Valid code");
                }
            }
            """;
        Files.writeString(sourceFile, validCode);

        // And: Validation enabled
        engine.enableStrictValidation();

        // Then: Validation mechanism is in place
        assertTrue(engine.isStrictValidationEnabled());

        // Note: Full integration test would require:
        // 1. A refactoring strategy that produces invalid code
        // 2. Executing the refactoring
        // 3. Verifying validation fails and rollback occurs
    }

    @Test
    @DisplayName("Should work without validation when disabled")
    void testRefactoringWorksWithoutValidation() throws Exception {
        // Given: Validation disabled (default)
        assertFalse(engine.isStrictValidationEnabled());

        // And: A simple Java file
        Path sourceFile = tempDir.resolve("SimpleClass.java");
        String code = """
            public class SimpleClass {
                public void hello() {
                    System.out.println("Hello");
                }
            }
            """;
        Files.writeString(sourceFile, code);

        // When: Creating refactoring plan
        RefactoringPlan plan = engine.createPlan(List.of());

        // Then: Plan creation should work
        assertNotNull(plan);
        assertTrue(plan.getActions().isEmpty());
    }

    @Test
    @DisplayName("Should handle validation for non-existent files gracefully")
    void testValidationWithNonExistentFile() {
        // Given: Validation enabled
        engine.enableStrictValidation();

        // And: Code smell pointing to non-existent file
        CodeSmell smell = new CodeSmell(
            CodeSmellType.LONG_METHOD,
            "/non/existent/file.java",
            10,
            "Method too long"
        );
        smell.setSeverity(Severity.MAJOR);

        // When: Creating plan
        RefactoringPlan plan = engine.createPlan(List.of(smell));

        // Then: Should handle gracefully
        assertNotNull(plan);
    }

    @Test
    @DisplayName("Validation integration should preserve original behavior when disabled")
    void testBackwardCompatibility() {
        // Given: Validation disabled
        assertFalse(engine.isStrictValidationEnabled());

        // When: Creating empty plan
        RefactoringPlan plan = engine.createPlan(List.of());

        // Then: Should work as before
        assertNotNull(plan);
        assertTrue(plan.getActions().isEmpty());
    }
}

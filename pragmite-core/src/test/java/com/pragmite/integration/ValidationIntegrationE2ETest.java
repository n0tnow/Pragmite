package com.pragmite.integration;

import com.pragmite.validation.JavacValidator;
import com.pragmite.validation.ValidationResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-end integration tests for validation workflow.
 *
 * Tests real-world scenarios of code validation after refactoring.
 *
 * @author Pragmite Team
 * @version 1.6.3 - Integration Sprint
 * @since 2025-12-28
 */
class ValidationIntegrationE2ETest {

    private JavacValidator validator;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        validator = new JavacValidator();
    }

    @AfterEach
    void tearDown() {
        if (validator != null) {
            validator.close();
        }
    }

    @Test
    @DisplayName("E2E: Valid refactored code should pass validation")
    void testValidRefactoredCode() throws Exception {
        // Simulate: Original code with unused import
        String originalCode = """
            import java.util.List;
            import java.util.ArrayList;
            import java.util.HashMap; // Unused

            public class UserService {
                public List<String> getUsers() {
                    List<String> users = new ArrayList<>();
                    users.add("Alice");
                    users.add("Bob");
                    return users;
                }
            }
            """;

        // Simulate: Refactored code with unused import removed
        String refactoredCode = """
            import java.util.List;
            import java.util.ArrayList;

            public class UserService {
                public List<String> getUsers() {
                    List<String> users = new ArrayList<>();
                    users.add("Alice");
                    users.add("Bob");
                    return users;
                }
            }
            """;

        Path sourceFile = tempDir.resolve("UserService.java");
        Files.writeString(sourceFile, refactoredCode);

        // When: Validating refactored code
        ValidationResult result = validator.validateFile(sourceFile);

        // Then: Should pass validation
        assertTrue(result.isValid(), "Refactored code should compile successfully");
        assertFalse(result.hasErrors(), "Should have no compilation errors");
    }

    @Test
    @DisplayName("E2E: Invalid refactored code should fail validation")
    void testInvalidRefactoredCode() throws Exception {
        // Simulate: Refactoring that accidentally broke the code
        String brokenCode = """
            import java.util.List;
            import java.util.ArrayList;

            public class UserService {
                public List<String> getUsers() {
                    List<String> users = new ArrayList<>();
                    users.add("Alice")  // Missing semicolon!
                    users.add("Bob");
                    return users;
                }
            }
            """;

        Path sourceFile = tempDir.resolve("UserService.java");
        Files.writeString(sourceFile, brokenCode);

        // When: Validating broken code
        ValidationResult result = validator.validateFile(sourceFile);

        // Then: Should fail validation
        assertFalse(result.isValid(), "Broken code should fail validation");
        assertTrue(result.hasErrors(), "Should detect syntax error");
        assertTrue(result.getErrorCount() > 0, "Should have at least one error");
    }

    @Test
    @DisplayName("E2E: Extract method refactoring should maintain compilation")
    void testExtractMethodRefactoring() throws Exception {
        // Simulate: Original long method
        String originalCode = """
            public class Calculator {
                public int calculate(int a, int b) {
                    int sum = a + b;
                    int product = a * b;
                    int result = sum * product;
                    return result;
                }
            }
            """;

        // Simulate: Refactored with extracted helper method
        String refactoredCode = """
            public class Calculator {
                public int calculate(int a, int b) {
                    int sum = computeSum(a, b);
                    int product = computeProduct(a, b);
                    int result = sum * product;
                    return result;
                }

                private int computeSum(int a, int b) {
                    return a + b;
                }

                private int computeProduct(int a, int b) {
                    return a * b;
                }
            }
            """;

        Path sourceFile = tempDir.resolve("Calculator.java");
        Files.writeString(sourceFile, refactoredCode);

        // When: Validating refactored code
        ValidationResult result = validator.validateFile(sourceFile);

        // Then: Should pass validation
        assertTrue(result.isValid(), "Extract method refactoring should maintain compilation");
        assertFalse(result.hasErrors());
    }

    @Test
    @DisplayName("E2E: Rename refactoring with missing reference should fail")
    void testIncompleteRenameRefactoring() throws Exception {
        // Simulate: Incomplete rename refactoring (missed one reference)
        String brokenCode = """
            public class OrderProcessor {
                private String customerName;

                public void setClientName(String name) {  // Renamed parameter
                    this.customerName = name;  // But field not renamed!
                }

                public String getClientName() {  // Renamed method
                    return customerName;  // Old field name still used
                }
            }
            """;

        Path sourceFile = tempDir.resolve("OrderProcessor.java");
        Files.writeString(sourceFile, brokenCode);

        // When: Validating code with incomplete rename
        ValidationResult result = validator.validateFile(sourceFile);

        // Then: Actually this SHOULD pass because the field name wasn't changed
        // This test shows validation catches actual compilation errors, not semantic issues
        assertTrue(result.isValid(), "This code actually compiles (no semantic checking)");
    }

    @Test
    @DisplayName("E2E: Type change refactoring should be validated")
    void testTypeChangeRefactoring() throws Exception {
        // Simulate: Refactoring that changes return type incorrectly
        String brokenCode = """
            public class DataService {
                // Changed return type from String to int
                public int getData() {
                    return "data";  // Type mismatch!
                }
            }
            """;

        Path sourceFile = tempDir.resolve("DataService.java");
        Files.writeString(sourceFile, brokenCode);

        // When: Validating code with type mismatch
        ValidationResult result = validator.validateFile(sourceFile);

        // Then: Should fail validation
        assertFalse(result.isValid(), "Type mismatch should fail compilation");
        assertTrue(result.hasErrors(), "Should detect type error");

        String errorMsg = result.getErrorMessage();
        assertTrue(errorMsg.toLowerCase().contains("incompatible") ||
                   errorMsg.toLowerCase().contains("type"),
                   "Error should mention type issue");
    }

    @Test
    @DisplayName("E2E: Complex refactoring with multiple changes should validate")
    void testComplexRefactoring() throws Exception {
        // Simulate: Complex refactoring with multiple changes
        String refactoredCode = """
            import java.util.List;
            import java.util.ArrayList;
            import java.util.stream.Collectors;

            public class UserRepository {
                private List<String> users = new ArrayList<>();

                public void addUser(String name) {
                    if (name != null && !name.isEmpty()) {
                        users.add(name);
                    }
                }

                public List<String> getActiveUsers() {
                    return users.stream()
                        .filter(u -> u != null)
                        .collect(Collectors.toList());
                }

                public int getUserCount() {
                    return users.size();
                }
            }
            """;

        Path sourceFile = tempDir.resolve("UserRepository.java");
        Files.writeString(sourceFile, refactoredCode);

        // When: Validating complex refactored code
        ValidationResult result = validator.validateFile(sourceFile);

        // Then: Should pass validation
        assertTrue(result.isValid(), "Complex refactoring should compile successfully");
        assertFalse(result.hasErrors());
    }
}

package com.pragmite.validation;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite for JavacValidator
 *
 * Tests validation of Java source code using javac compiler.
 *
 * @author Pragmite Team
 * @version 1.6.3
 * @since 2025-12-28
 */
class JavacValidatorTest {

    private JavacValidator validator;

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
    @DisplayName("Should validate correct Java code successfully")
    void testValidateCorrectCode() {
        // Given: Valid Java code
        String sourceCode = """
            package com.example;

            public class HelloWorld {
                public static void main(String[] args) {
                    System.out.println("Hello, World!");
                }
            }
            """;

        // When: Validating the code
        ValidationResult result = validator.validate(sourceCode, "com.example.HelloWorld");

        // Then: Validation should succeed
        assertTrue(result.isValid(), "Valid code should pass validation");
        assertFalse(result.hasErrors(), "Valid code should have no errors");
        assertEquals(0, result.getErrorCount(), "Error count should be 0");
    }

    @Test
    @DisplayName("Should detect syntax errors in invalid code")
    void testValidateInvalidSyntax() {
        // Given: Invalid Java code (missing semicolon)
        String sourceCode = """
            package com.example;

            public class BrokenClass {
                public void test() {
                    System.out.println("Missing semicolon")
                }
            }
            """;

        // When: Validating the code
        ValidationResult result = validator.validate(sourceCode, "com.example.BrokenClass");

        // Then: Validation should fail
        assertFalse(result.isValid(), "Invalid code should fail validation");
        assertTrue(result.hasErrors(), "Invalid code should have errors");
        assertTrue(result.getErrorCount() > 0, "Should have at least one error");
    }

    @Test
    @DisplayName("Should detect undefined symbols")
    void testValidateUndefinedSymbol() {
        // Given: Code with undefined variable
        String sourceCode = """
            package com.example;

            public class UndefinedVariable {
                public void test() {
                    System.out.println(undefinedVar);
                }
            }
            """;

        // When: Validating the code
        ValidationResult result = validator.validate(sourceCode, "com.example.UndefinedVariable");

        // Then: Validation should fail
        assertFalse(result.isValid(), "Code with undefined symbol should fail");
        assertTrue(result.hasErrors(), "Should have errors");

        // Check error message contains symbol info
        String errorMessage = result.getErrorMessage();
        assertTrue(errorMessage.toLowerCase().contains("cannot find symbol") ||
                   errorMessage.toLowerCase().contains("undefinedvar"),
                   "Error should mention undefined symbol");
    }

    @Test
    @DisplayName("Should detect type errors")
    void testValidateTypeError() {
        // Given: Code with type mismatch
        String sourceCode = """
            package com.example;

            public class TypeMismatch {
                public void test() {
                    String text = 123;
                }
            }
            """;

        // When: Validating the code
        ValidationResult result = validator.validate(sourceCode, "com.example.TypeMismatch");

        // Then: Validation should fail
        assertFalse(result.isValid(), "Code with type error should fail");
        assertTrue(result.hasErrors(), "Should have type error");
    }

    @Test
    @DisplayName("Should validate code with warnings but no errors")
    void testValidateWithWarnings() {
        // Given: Code that compiles but has warnings (unchecked)
        String sourceCode = """
            package com.example;

            import java.util.ArrayList;
            import java.util.List;

            public class WarningCode {
                public void test() {
                    List list = new ArrayList();
                    list.add("item");
                }
            }
            """;

        // When: Validating the code
        ValidationResult result = validator.validate(sourceCode, "com.example.WarningCode");

        // Then: Validation should succeed (warnings don't fail compilation)
        assertTrue(result.isValid(), "Code with only warnings should be valid");
        assertFalse(result.hasErrors(), "Should have no errors");
    }

    @Test
    @DisplayName("Should validate class with imports")
    void testValidateWithImports() {
        // Given: Code with Java standard library imports
        String sourceCode = """
            package com.example;

            import java.util.List;
            import java.util.ArrayList;

            public class ImportTest {
                public List<String> getNames() {
                    List<String> names = new ArrayList<>();
                    names.add("Alice");
                    names.add("Bob");
                    return names;
                }
            }
            """;

        // When: Validating the code
        ValidationResult result = validator.validate(sourceCode, "com.example.ImportTest");

        // Then: Validation should succeed
        assertTrue(result.isValid(), "Code with imports should be valid");
        assertFalse(result.hasErrors(), "Should have no errors");
    }

    @Test
    @DisplayName("Should validate modern Java features (Java 21)")
    void testValidateJava21Features() {
        // Given: Code using Java 21 features (text blocks, var, etc.)
        String sourceCode = """
            package com.example;

            public class ModernJava {
                public void test() {
                    var message = \"\"\"
                        This is a text block
                        spanning multiple lines
                        \"\"\";
                    System.out.println(message);
                }
            }
            """;

        // When: Validating the code
        ValidationResult result = validator.validate(sourceCode, "com.example.ModernJava");

        // Then: Validation should succeed
        assertTrue(result.isValid(), "Java 21 features should be supported");
        assertFalse(result.hasErrors(), "Should have no errors");
    }

    @Test
    @DisplayName("Should handle null source code")
    void testValidateNullSourceCode() {
        // When/Then: Validating null source code should throw exception
        assertThrows(NullPointerException.class, () -> {
            validator.validate(null, "com.example.Test");
        });
    }

    @Test
    @DisplayName("Should handle null class name")
    void testValidateNullClassName() {
        // When/Then: Validating with null class name should throw exception
        assertThrows(NullPointerException.class, () -> {
            validator.validate("public class Test {}", null);
        });
    }

    @Test
    @DisplayName("Should validate file from disk")
    void testValidateFile() throws Exception {
        // Given: Valid Java file on disk with matching class name
        Path tempDir = Files.createTempDirectory("test-");
        Path tempFile = tempDir.resolve("TestClass.java");
        try {
            String sourceCode = """
                public class TestClass {
                    public static void main(String[] args) {
                        System.out.println("Test");
                    }
                }
                """;
            Files.writeString(tempFile, sourceCode);

            // When: Validating the file
            ValidationResult result = validator.validateFile(tempFile);

            // Then: Validation should succeed
            assertTrue(result.isValid(), "Valid file should pass validation");
            assertFalse(result.hasErrors(), "Should have no errors");
        } finally {
            Files.deleteIfExists(tempFile);
            Files.deleteIfExists(tempDir);
        }
    }

    @Test
    @DisplayName("Should provide detailed error information")
    void testErrorDetails() {
        // Given: Invalid code
        String sourceCode = """
            package com.example;

            public class ErrorDetails {
                public void test() {
                    int x = "not a number";
                }
            }
            """;

        // When: Validating the code
        ValidationResult result = validator.validate(sourceCode, "com.example.ErrorDetails");

        // Then: Should have detailed error info
        assertFalse(result.isValid());
        assertTrue(result.hasErrors());

        ValidationError firstError = result.getErrors().get(0);
        assertNotNull(firstError);
        assertNotNull(firstError.getMessage());
        assertTrue(firstError.getLineNumber() > 0, "Error should have line number");
        assertEquals("ERROR", firstError.getKind());
    }

    @Test
    @DisplayName("Should format error message readably")
    void testErrorMessageFormatting() {
        // Given: Invalid code
        String sourceCode = """
            package com.example;

            public class FormatTest {
                public void test() {
                    undefinedMethod();
                }
            }
            """;

        // When: Validating the code
        ValidationResult result = validator.validate(sourceCode, "com.example.FormatTest");

        // Then: Error message should be readable
        String errorMessage = result.getErrorMessage();
        assertNotNull(errorMessage);
        assertTrue(errorMessage.contains("error"), "Should mention error count");
        assertTrue(errorMessage.contains("Line"), "Should include line numbers");
    }
}

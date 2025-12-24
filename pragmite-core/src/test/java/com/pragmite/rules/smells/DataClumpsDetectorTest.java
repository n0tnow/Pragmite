package com.pragmite.rules.smells;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.pragmite.model.CodeSmell;
import com.pragmite.model.CodeSmellType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DataClumpsDetectorTest {

    private DataClumpsDetector detector;

    @BeforeEach
    void setUp() {
        detector = new DataClumpsDetector();
    }

    @Test
    void shouldDetectDataClumps() {
        String code = """
            public class Test {
                public void createAddress(String street, String city, String zipCode) {
                    // Implementation
                }

                public void updateAddress(String street, String city, String zipCode) {
                    // Implementation
                }
            }
            """;

        CompilationUnit cu = StaticJavaParser.parse(code);
        List<CodeSmell> smells = detector.detect(cu, "Test.java", code);

        // With exact same parameters in 2+ methods, should detect data clump
        assertTrue(smells.size() > 0, "Should detect parameter clumps");
        assertTrue(smells.stream().anyMatch(s ->
            s.getType() == CodeSmellType.DATA_CLUMPS));
    }

    @Test
    void shouldNotDetectWithFewParameters() {
        String code = """
            public class Test {
                public void method1(String name, int age) {
                    // Implementation
                }

                public void method2(String name, int age) {
                    // Implementation
                }
            }
            """;

        CompilationUnit cu = StaticJavaParser.parse(code);
        List<CodeSmell> smells = detector.detect(cu, "Test.java", code);

        assertEquals(0, smells.size(), "Should not detect clumps with < 3 parameters");
    }

    @Test
    void shouldDetectRepeatedParameterPattern() {
        String code = """
            public class Test {
                public void processPayment(String cardNumber, String cvv, String expiryDate) {
                    // Implementation
                }

                public void validatePayment(String cardNumber, String cvv, String expiryDate) {
                    // Implementation
                }
            }
            """;

        CompilationUnit cu = StaticJavaParser.parse(code);
        List<CodeSmell> smells = detector.detect(cu, "Test.java", code);

        assertTrue(smells.size() > 0, "Should detect repeated parameter pattern");
        assertTrue(smells.stream().anyMatch(s ->
            s.getType() == CodeSmellType.DATA_CLUMPS));
    }
}

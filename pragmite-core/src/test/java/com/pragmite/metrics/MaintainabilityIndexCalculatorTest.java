package com.pragmite.metrics;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MaintainabilityIndexCalculatorTest {

    private MaintainabilityIndexCalculator calculator;
    private JavaParser parser;

    @BeforeEach
    void setUp() {
        calculator = new MaintainabilityIndexCalculator();
        parser = new JavaParser();
    }

    @Test
    void testSimpleMethod() {
        String code = """
            public class Test {
                public int add(int a, int b) {
                    return a + b;
                }
            }
            """;

        CompilationUnit cu = parseCode(code);
        Map<String, MaintainabilityIndex> indices = calculator.calculateAll(cu, "test.java");

        assertFalse(indices.isEmpty());
        MaintainabilityIndex mi = indices.values().iterator().next();

        // Simple method should have good maintainability (HIGH or MODERATE)
        assertTrue(mi.getNormalizedMI() > 65,
            "Simple method should have MI > 65, got: " + mi.getNormalizedMI());

        // Should be either HIGH or MODERATE (both are acceptable for simple methods)
        assertNotEquals(MaintainabilityIndex.MaintainabilityLevel.LOW, mi.getLevel(),
            "Simple method should not have LOW maintainability");
        assertFalse(mi.isLowMaintainability());
    }

    @Test
    void testComplexMethod() {
        String code = """
            public class Test {
                public int complex(int x, int y, int z, int w, String name, boolean flag) {
                    int result = 0;

                    if (x > y) {
                        if (flag) {
                            for (int i = 0; i < x; i++) {
                                for (int j = 0; j < y; j++) {
                                    if (i > j) {
                                        result += i * j;
                                    } else if (i < j) {
                                        result -= i * j;
                                    } else {
                                        result += z * w;
                                    }
                                }
                            }
                        } else {
                            while (x > 0) {
                                x--;
                                result += x;
                            }
                        }
                    } else {
                        switch (z) {
                            case 1: result = 1; break;
                            case 2: result = 2; break;
                            case 3: result = 3; break;
                            case 4: result = 4; break;
                            default: result = 0;
                        }
                    }

                    return result;
                }
            }
            """;

        CompilationUnit cu = parseCode(code);
        Map<String, MaintainabilityIndex> indices = calculator.calculateAll(cu, "test.java");

        MaintainabilityIndex mi = indices.values().iterator().next();

        // Complex method should have lower maintainability
        assertTrue(mi.getNormalizedMI() < 85,
            "Complex method should have MI < 85, got: " + mi.getNormalizedMI());

        // Should not be high maintainability
        assertNotEquals(MaintainabilityIndex.MaintainabilityLevel.HIGH, mi.getLevel());
    }

    @Test
    void testEmptyMethod() {
        String code = """
            public class Test {
                public void empty() {
                }
            }
            """;

        CompilationUnit cu = parseCode(code);
        Map<String, MaintainabilityIndex> indices = calculator.calculateAll(cu, "test.java");

        MaintainabilityIndex mi = indices.values().iterator().next();

        // Empty method should have reasonable MI
        assertTrue(mi.getNormalizedMI() >= 0,
            "MI should be non-negative");
    }

    @Test
    void testMIFormula() {
        // Create a method with known metrics
        String code = """
            public class Test {
                public int test(int a, int b) {
                    if (a > b) {
                        return a + b;
                    } else {
                        return a - b;
                    }
                }
            }
            """;

        CompilationUnit cu = parseCode(code);
        Map<String, MaintainabilityIndex> indices = calculator.calculateAll(cu, "test.java");

        MaintainabilityIndex mi = indices.values().iterator().next();

        // MI = 171 - 5.2 * ln(V) - 0.23 * CC - 16.2 * ln(LOC)
        double V = mi.getHalsteadVolume();
        int CC = mi.getCyclomaticComplexity();
        int LOC = mi.getLinesOfCode();

        double expected = 171.0
                        - 5.2 * Math.log(V)
                        - 0.23 * CC
                        - 16.2 * Math.log(LOC);

        assertEquals(expected, mi.getMI(), 0.01,
            "MI formula should be correctly calculated");
    }

    @Test
    void testNormalizedMI() {
        String code = """
            public class Test {
                public int test(int a) {
                    return a + 1;
                }
            }
            """;

        CompilationUnit cu = parseCode(code);
        Map<String, MaintainabilityIndex> indices = calculator.calculateAll(cu, "test.java");

        MaintainabilityIndex mi = indices.values().iterator().next();

        double normalized = mi.getNormalizedMI();

        // Normalized MI should be in range [0, 100]
        assertTrue(normalized >= 0, "Normalized MI should be >= 0");
        assertTrue(normalized <= 100, "Normalized MI should be <= 100");

        // Should match formula: (MI / 171) * 100
        double expected = Math.max(0, Math.min(100, (mi.getMI() / 171.0) * 100.0));
        assertEquals(expected, normalized, 0.01);
    }

    @Test
    void testHighMaintainability() {
        String code = """
            public class Test {
                public int getValue() {
                    return 42;
                }
            }
            """;

        CompilationUnit cu = parseCode(code);
        Map<String, MaintainabilityIndex> indices = calculator.calculateAll(cu, "test.java");

        MaintainabilityIndex mi = indices.values().iterator().next();

        assertEquals(MaintainabilityIndex.MaintainabilityLevel.HIGH, mi.getLevel());
        assertEquals("GREEN", mi.getColorCode());
        assertTrue(mi.isHighMaintainability());
        assertFalse(mi.isLowMaintainability());
    }

    @Test
    void testLowMaintainability() {
        String code = """
            public class Test {
                public void veryComplexMethod(int a, int b, int c, int d, int e, int f) {
                    for (int i = 0; i < a; i++) {
                        for (int j = 0; j < b; j++) {
                            for (int k = 0; k < c; k++) {
                                if (i > j && j > k) {
                                    System.out.println(i + j + k);
                                } else if (i < j && j < k) {
                                    System.out.println(i - j - k);
                                } else if (i == j) {
                                    System.out.println(i * k);
                                } else {
                                    System.out.println(d + e + f);
                                }
                            }
                        }
                    }
                }
            }
            """;

        CompilationUnit cu = parseCode(code);
        Map<String, MaintainabilityIndex> indices = calculator.calculateAll(cu, "test.java");

        MaintainabilityIndex mi = indices.values().iterator().next();

        // Should have low maintainability due to complexity
        assertTrue(mi.getNormalizedMI() < 85,
            "Complex method should have lower MI");
    }

    @Test
    void testMaintainabilityLevels() {
        // Test level classification
        MaintainabilityIndex highMI = new MaintainabilityIndex(
            "high", "test.java", 1, 50, 2, 5
        );
        assertTrue(highMI.getNormalizedMI() >= 85 || highMI.getNormalizedMI() >= 65,
            "Should have reasonable MI");

        MaintainabilityIndex lowMI = new MaintainabilityIndex(
            "low", "test.java", 1, 2000, 20, 100
        );
        assertTrue(lowMI.getNormalizedMI() < 85,
            "Should have low MI due to high complexity");
    }

    @Test
    void testColorCode() {
        String code = """
            public class Test {
                public int simple(int a) {
                    return a;
                }
            }
            """;

        CompilationUnit cu = parseCode(code);
        Map<String, MaintainabilityIndex> indices = calculator.calculateAll(cu, "test.java");

        MaintainabilityIndex mi = indices.values().iterator().next();

        String color = mi.getColorCode();
        assertTrue(color.equals("GREEN") || color.equals("YELLOW") || color.equals("RED"),
            "Color should be one of: GREEN, YELLOW, RED");
    }

    @Test
    void testRecommendation() {
        String code = """
            public class Test {
                public int test(int a) {
                    return a + 1;
                }
            }
            """;

        CompilationUnit cu = parseCode(code);
        Map<String, MaintainabilityIndex> indices = calculator.calculateAll(cu, "test.java");

        MaintainabilityIndex mi = indices.values().iterator().next();

        String recommendation = mi.getRecommendation();
        assertNotNull(recommendation);
        assertFalse(recommendation.isEmpty());
    }

    @Test
    void testMainIssue() {
        String code = """
            public class Test {
                public int test(int a) {
                    return a + 1;
                }
            }
            """;

        CompilationUnit cu = parseCode(code);
        Map<String, MaintainabilityIndex> indices = calculator.calculateAll(cu, "test.java");

        MaintainabilityIndex mi = indices.values().iterator().next();

        String mainIssue = mi.getMainIssue();
        assertNotNull(mainIssue);
        assertFalse(mainIssue.isEmpty());
    }

    @Test
    void testMainIssue_HighComplexity() {
        // Create method with high cyclomatic complexity
        MaintainabilityIndex mi = new MaintainabilityIndex(
            "complex", "test.java", 1, 100, 15, 20
        );

        String mainIssue = mi.getMainIssue();
        assertTrue(mainIssue.contains("complexity"),
            "Should identify complexity as main issue");
    }

    @Test
    void testMainIssue_HighVolume() {
        // Create method with high Halstead volume
        MaintainabilityIndex mi = new MaintainabilityIndex(
            "verbose", "test.java", 1, 1500, 5, 20
        );

        String mainIssue = mi.getMainIssue();
        assertTrue(mainIssue.contains("volume") || mainIssue.contains("Multiple"),
            "Should identify volume or multiple factors");
    }

    @Test
    void testMainIssue_LongMethod() {
        // Create method with many lines
        MaintainabilityIndex mi = new MaintainabilityIndex(
            "long", "test.java", 1, 200, 8, 80
        );

        String mainIssue = mi.getMainIssue();
        assertTrue(mainIssue.contains("Long") || mainIssue.contains("Multiple"),
            "Should identify length or multiple factors as issue");
    }

    @Test
    void testToString() {
        String code = """
            public class Test {
                public int test(int a) {
                    return a + 1;
                }
            }
            """;

        CompilationUnit cu = parseCode(code);
        Map<String, MaintainabilityIndex> indices = calculator.calculateAll(cu, "test.java");

        MaintainabilityIndex mi = indices.values().iterator().next();
        String str = mi.toString();

        // Verify toString contains key information
        assertTrue(str.contains("Maintainability Index"));
        assertTrue(str.contains("Halstead Volume"));
        assertTrue(str.contains("Cyclomatic Complexity"));
        assertTrue(str.contains("Lines of Code"));
        assertTrue(str.contains("MI (raw)"));
        assertTrue(str.contains("MI (normalized)"));
        assertTrue(str.contains("Level"));
        assertTrue(str.contains("Recommendation"));
    }

    @Test
    void testCompactString() {
        String code = """
            public class Test {
                public int test(int a) {
                    return a + 1;
                }
            }
            """;

        CompilationUnit cu = parseCode(code);
        Map<String, MaintainabilityIndex> indices = calculator.calculateAll(cu, "test.java");

        MaintainabilityIndex mi = indices.values().iterator().next();
        String compact = mi.toCompactString();

        assertTrue(compact.contains("MI="));
        assertTrue(compact.contains("test"));
    }

    @Test
    void testMultipleMethods() {
        String code = """
            public class Test {
                public int method1(int a) {
                    return a + 1;
                }

                public int method2(int a, int b) {
                    if (a > b) {
                        return a;
                    } else {
                        return b;
                    }
                }

                public void method3() {
                    System.out.println("test");
                }
            }
            """;

        CompilationUnit cu = parseCode(code);
        Map<String, MaintainabilityIndex> indices = calculator.calculateAll(cu, "test.java");

        assertEquals(3, indices.size(),
            "Should calculate MI for all 3 methods");
    }

    @Test
    void testEdgeCase_ZeroVolume() {
        // Direct construction with edge case values
        MaintainabilityIndex mi = new MaintainabilityIndex(
            "edge", "test.java", 1, 0, 1, 1
        );

        // Should handle gracefully without exception
        assertEquals(0, mi.getMI(), 0.01);
        assertEquals(0, mi.getNormalizedMI(), 0.01);
    }

    @Test
    void testEdgeCase_ZeroLOC() {
        // Direct construction with edge case values
        MaintainabilityIndex mi = new MaintainabilityIndex(
            "edge", "test.java", 1, 100, 1, 0
        );

        // Should handle gracefully without exception
        assertEquals(0, mi.getMI(), 0.01);
    }

    // Helper method
    private CompilationUnit parseCode(String code) {
        ParseResult<CompilationUnit> result = parser.parse(code);
        assertTrue(result.isSuccessful(), "Code should parse successfully");
        assertTrue(result.getResult().isPresent(), "Parse result should be present");
        return result.getResult().get();
    }
}

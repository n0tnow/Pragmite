package com.pragmite.metrics;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class HalsteadMetricsCalculatorTest {

    private HalsteadMetricsCalculator calculator;
    private JavaParser parser;

    @BeforeEach
    void setUp() {
        calculator = new HalsteadMetricsCalculator();
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
        Map<String, HalsteadMetrics> metrics = calculator.calculateAll(cu, "test.java");

        assertFalse(metrics.isEmpty());
        HalsteadMetrics halstead = metrics.values().iterator().next();

        // Should have operators: return, +
        assertTrue(halstead.getDistinctOperators() >= 2);

        // Should have operands: a, b
        assertTrue(halstead.getDistinctOperands() >= 2);

        // Basic sanity checks
        assertTrue(halstead.getVolume() > 0);
        assertTrue(halstead.getDifficulty() > 0);
        assertTrue(halstead.getEffort() > 0);
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
        Map<String, HalsteadMetrics> metrics = calculator.calculateAll(cu, "test.java");

        assertFalse(metrics.isEmpty());
        HalsteadMetrics halstead = metrics.values().iterator().next();

        // Empty method should have minimal metrics
        assertEquals(0, halstead.getVolume(), 0.01);
        assertEquals(0, halstead.getDifficulty(), 0.01);
    }

    @Test
    void testComplexMethod() {
        String code = """
            public class Test {
                public int complex(int x, int y, String name) {
                    int result = 0;

                    if (x > y) {
                        result = x * 2;
                    } else if (x < y) {
                        result = y * 2;
                    } else {
                        result = x + y;
                    }

                    for (int i = 0; i < 10; i++) {
                        result += i;
                    }

                    while (result > 100) {
                        result = result / 2;
                    }

                    return result;
                }
            }
            """;

        CompilationUnit cu = parseCode(code);
        Map<String, HalsteadMetrics> metrics = calculator.calculateAll(cu, "test.java");

        HalsteadMetrics halstead = metrics.values().iterator().next();

        // Complex method should have many operators
        assertTrue(halstead.getDistinctOperators() >= 10,
            "Expected many operators, got: " + halstead.getDistinctOperators());

        // Should have high volume
        assertTrue(halstead.getVolume() > 100,
            "Expected high volume, got: " + halstead.getVolume());

        // Should have measurable difficulty
        assertTrue(halstead.getDifficulty() > 5,
            "Expected high difficulty, got: " + halstead.getDifficulty());
    }

    @Test
    void testOperatorCounting() {
        String code = """
            public class Test {
                public boolean check(int a, int b) {
                    return a > b && b < 10 || a == 0;
                }
            }
            """;

        CompilationUnit cu = parseCode(code);
        Map<String, HalsteadMetrics> metrics = calculator.calculateAll(cu, "test.java");

        HalsteadMetrics halstead = metrics.values().iterator().next();

        // Operators: return, >, &&, <, ||, ==
        assertTrue(halstead.getDistinctOperators() >= 5,
            "Expected at least 5 distinct operators");

        // Operands: a, b, 10, 0
        assertTrue(halstead.getDistinctOperands() >= 4,
            "Expected at least 4 distinct operands");
    }

    @Test
    void testMethodCallsCounting() {
        String code = """
            public class Test {
                public void process(List<String> items) {
                    items.add("hello");
                    items.remove(0);
                    System.out.println(items.size());
                }
            }
            """;

        CompilationUnit cu = parseCode(code);
        Map<String, HalsteadMetrics> metrics = calculator.calculateAll(cu, "test.java");

        HalsteadMetrics halstead = metrics.values().iterator().next();

        // Should count method calls as operators
        assertTrue(halstead.getTotalOperators() >= 3,
            "Expected method calls to be counted as operators");

        // Method names should be operands
        assertTrue(halstead.getDistinctOperands() >= 3,
            "Expected method names as operands");
    }

    @Test
    void testLoopCounting() {
        String code = """
            public class Test {
                public void loops() {
                    for (int i = 0; i < 10; i++) {
                        System.out.println(i);
                    }

                    while (true) {
                        break;
                    }

                    do {
                        System.out.println("test");
                    } while (false);
                }
            }
            """;

        CompilationUnit cu = parseCode(code);
        Map<String, HalsteadMetrics> metrics = calculator.calculateAll(cu, "test.java");

        HalsteadMetrics halstead = metrics.values().iterator().next();

        // Should count loop keywords as operators
        assertTrue(halstead.getDistinctOperators() >= 3,
            "Expected for, while, do as operators");
    }

    @Test
    void testVocabulary() {
        String code = """
            public class Test {
                public int test(int a, int b) {
                    return a + b;
                }
            }
            """;

        CompilationUnit cu = parseCode(code);
        Map<String, HalsteadMetrics> metrics = calculator.calculateAll(cu, "test.java");

        HalsteadMetrics halstead = metrics.values().iterator().next();

        int n1 = halstead.getDistinctOperators();
        int n2 = halstead.getDistinctOperands();
        int vocabulary = halstead.getVocabulary();

        assertEquals(n1 + n2, vocabulary,
            "Vocabulary should be sum of distinct operators and operands");
    }

    @Test
    void testLength() {
        String code = """
            public class Test {
                public int test(int a, int b) {
                    return a + b;
                }
            }
            """;

        CompilationUnit cu = parseCode(code);
        Map<String, HalsteadMetrics> metrics = calculator.calculateAll(cu, "test.java");

        HalsteadMetrics halstead = metrics.values().iterator().next();

        int N1 = halstead.getTotalOperators();
        int N2 = halstead.getTotalOperands();
        int length = halstead.getLength();

        assertEquals(N1 + N2, length,
            "Length should be sum of total operators and operands");
    }

    @Test
    void testVolume() {
        String code = """
            public class Test {
                public int test(int a, int b, int c) {
                    return a + b + c;
                }
            }
            """;

        CompilationUnit cu = parseCode(code);
        Map<String, HalsteadMetrics> metrics = calculator.calculateAll(cu, "test.java");

        HalsteadMetrics halstead = metrics.values().iterator().next();

        double volume = halstead.getVolume();
        int length = halstead.getLength();
        int vocabulary = halstead.getVocabulary();

        // V = N * log2(n)
        double expected = length * (Math.log(vocabulary) / Math.log(2));

        assertEquals(expected, volume, 0.01,
            "Volume should be N * log2(n)");
    }

    @Test
    void testDifficulty() {
        String code = """
            public class Test {
                public int test(int a, int b) {
                    return a + b;
                }
            }
            """;

        CompilationUnit cu = parseCode(code);
        Map<String, HalsteadMetrics> metrics = calculator.calculateAll(cu, "test.java");

        HalsteadMetrics halstead = metrics.values().iterator().next();

        double difficulty = halstead.getDifficulty();

        // D = (n1/2) * (N2/n2)
        int n1 = halstead.getDistinctOperators();
        int n2 = halstead.getDistinctOperands();
        int N2 = halstead.getTotalOperands();

        double expected = (n1 / 2.0) * (N2 / (double) n2);

        assertEquals(expected, difficulty, 0.01,
            "Difficulty should be (n1/2) * (N2/n2)");
    }

    @Test
    void testEffort() {
        String code = """
            public class Test {
                public int test(int a, int b) {
                    return a + b;
                }
            }
            """;

        CompilationUnit cu = parseCode(code);
        Map<String, HalsteadMetrics> metrics = calculator.calculateAll(cu, "test.java");

        HalsteadMetrics halstead = metrics.values().iterator().next();

        double effort = halstead.getEffort();
        double difficulty = halstead.getDifficulty();
        double volume = halstead.getVolume();

        assertEquals(difficulty * volume, effort, 0.01,
            "Effort should be D * V");
    }

    @Test
    void testTimeToProgram() {
        String code = """
            public class Test {
                public int test(int a, int b) {
                    return a + b;
                }
            }
            """;

        CompilationUnit cu = parseCode(code);
        Map<String, HalsteadMetrics> metrics = calculator.calculateAll(cu, "test.java");

        HalsteadMetrics halstead = metrics.values().iterator().next();

        double time = halstead.getTimeToProgram();
        double effort = halstead.getEffort();

        assertEquals(effort / 18.0, time, 0.01,
            "Time should be E / 18");
    }

    @Test
    void testDeliveredBugs() {
        String code = """
            public class Test {
                public int test(int a, int b) {
                    return a + b;
                }
            }
            """;

        CompilationUnit cu = parseCode(code);
        Map<String, HalsteadMetrics> metrics = calculator.calculateAll(cu, "test.java");

        HalsteadMetrics halstead = metrics.values().iterator().next();

        double bugs = halstead.getDeliveredBugs();
        double volume = halstead.getVolume();

        assertEquals(volume / 3000.0, bugs, 0.0001,
            "Bugs should be V / 3000");
    }

    @Test
    void testComplexityLevel_Simple() {
        String code = """
            public class Test {
                public int simple(int a) {
                    return a;
                }
            }
            """;

        CompilationUnit cu = parseCode(code);
        Map<String, HalsteadMetrics> metrics = calculator.calculateAll(cu, "test.java");

        HalsteadMetrics halstead = metrics.values().iterator().next();

        assertEquals("Simple", halstead.getComplexityLevel());
        assertFalse(halstead.isComplex());
    }

    @Test
    void testComplexityLevel_Complex() {
        String code = """
            public class Test {
                public int veryComplex(int a, int b, int c, int d) {
                    int result = 0;
                    for (int i = 0; i < a; i++) {
                        for (int j = 0; j < b; j++) {
                            if (i > j) {
                                result += i * j;
                            } else if (i < j) {
                                result -= i * j;
                            } else {
                                result += c * d;
                            }
                        }
                    }
                    return result;
                }
            }
            """;

        CompilationUnit cu = parseCode(code);
        Map<String, HalsteadMetrics> metrics = calculator.calculateAll(cu, "test.java");

        HalsteadMetrics halstead = metrics.values().iterator().next();

        // Complex method should have high volume
        assertTrue(halstead.getVolume() > 100,
            "Complex method should have volume > 100");
    }

    @Test
    void testToString() {
        String code = """
            public class Test {
                public int test(int a, int b) {
                    return a + b;
                }
            }
            """;

        CompilationUnit cu = parseCode(code);
        Map<String, HalsteadMetrics> metrics = calculator.calculateAll(cu, "test.java");

        HalsteadMetrics halstead = metrics.values().iterator().next();
        String str = halstead.toString();

        // Verify toString contains key information
        assertTrue(str.contains("Halstead Metrics"));
        assertTrue(str.contains("n1"));
        assertTrue(str.contains("n2"));
        assertTrue(str.contains("Volume"));
        assertTrue(str.contains("Difficulty"));
        assertTrue(str.contains("Effort"));
    }

    @Test
    void testCompactString() {
        String code = """
            public class Test {
                public int test(int a, int b) {
                    return a + b;
                }
            }
            """;

        CompilationUnit cu = parseCode(code);
        Map<String, HalsteadMetrics> metrics = calculator.calculateAll(cu, "test.java");

        HalsteadMetrics halstead = metrics.values().iterator().next();
        String compact = halstead.toCompactString();

        assertTrue(compact.contains("V="));
        assertTrue(compact.contains("D="));
        assertTrue(compact.contains("E="));
        assertTrue(compact.contains("B="));
    }

    @Test
    void testMultipleMethods() {
        String code = """
            public class Test {
                public int method1(int a) {
                    return a + 1;
                }

                public int method2(int a, int b) {
                    return a * b;
                }

                public void method3() {
                    System.out.println("test");
                }
            }
            """;

        CompilationUnit cu = parseCode(code);
        Map<String, HalsteadMetrics> metrics = calculator.calculateAll(cu, "test.java");

        assertEquals(3, metrics.size(),
            "Should calculate metrics for all 3 methods");
    }

    // Helper method
    private CompilationUnit parseCode(String code) {
        ParseResult<CompilationUnit> result = parser.parse(code);
        assertTrue(result.isSuccessful(), "Code should parse successfully");
        assertTrue(result.getResult().isPresent(), "Parse result should be present");
        return result.getResult().get();
    }
}

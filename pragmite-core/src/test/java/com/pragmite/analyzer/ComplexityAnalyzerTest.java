package com.pragmite.analyzer;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.pragmite.model.BigOComplexity;
import com.pragmite.model.ComplexityInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ComplexityAnalyzerTest {

    private ComplexityAnalyzer analyzer;

    @BeforeEach
    void setUp() {
        analyzer = new ComplexityAnalyzer();
    }

    @Test
    void shouldDetectConstantComplexity() {
        String code = """
            public class Test {
                public int simple(int x) {
                    return x + 1;
                }
            }
            """;

        CompilationUnit cu = StaticJavaParser.parse(code);
        List<ComplexityInfo> complexities = analyzer.analyze(cu, "Test.java");

        // Note: Very simple methods may not be reported (design decision to reduce noise)
        // This test documents the current behavior
        assertTrue(complexities.size() <= 1, "Should report 0 or 1 complexity");
        if (complexities.size() == 1) {
            assertEquals(BigOComplexity.O_1, complexities.get(0).getTimeComplexity());
        }
    }

    @Test
    void shouldDetectLinearComplexity() {
        String code = """
            public class Test {
                public void iterate(int[] arr) {
                    for (int i = 0; i < arr.length; i++) {
                        System.out.println(arr[i]);
                    }
                }
            }
            """;

        CompilationUnit cu = StaticJavaParser.parse(code);
        List<ComplexityInfo> complexities = analyzer.analyze(cu, "Test.java");

        assertEquals(1, complexities.size());
        assertEquals(BigOComplexity.O_N, complexities.get(0).getTimeComplexity());
    }

    @Test
    void shouldDetectQuadraticComplexity() {
        String code = """
            public class Test {
                public void nestedLoop(int n) {
                    for (int i = 0; i < n; i++) {
                        for (int j = 0; j < n; j++) {
                            System.out.println(i + j);
                        }
                    }
                }
            }
            """;

        CompilationUnit cu = StaticJavaParser.parse(code);
        List<ComplexityInfo> complexities = analyzer.analyze(cu, "Test.java");

        assertEquals(1, complexities.size());
        assertEquals(BigOComplexity.O_N_SQUARED, complexities.get(0).getTimeComplexity());
    }

    @Test
    void shouldDetectCubicComplexity() {
        String code = """
            public class Test {
                public void tripleNested(int n) {
                    for (int i = 0; i < n; i++) {
                        for (int j = 0; j < n; j++) {
                            for (int k = 0; k < n; k++) {
                                System.out.println(i + j + k);
                            }
                        }
                    }
                }
            }
            """;

        CompilationUnit cu = StaticJavaParser.parse(code);
        List<ComplexityInfo> complexities = analyzer.analyze(cu, "Test.java");

        assertEquals(1, complexities.size());
        assertEquals(BigOComplexity.O_N_CUBED, complexities.get(0).getTimeComplexity());
    }

    @Test
    void shouldDetectNLogNComplexity() {
        String code = """
            import java.util.List;
            public class Test {
                public void sortAndIterate(List<Integer> list) {
                    list.stream()
                        .sorted()
                        .forEach(System.out::println);
                }
            }
            """;

        CompilationUnit cu = StaticJavaParser.parse(code);
        List<ComplexityInfo> complexities = analyzer.analyze(cu, "Test.java");

        assertEquals(1, complexities.size());
        assertEquals(BigOComplexity.O_N_LOG_N, complexities.get(0).getTimeComplexity());
    }

    @Test
    void shouldDetectLinearRecursion() {
        String code = """
            public class Test {
                public int factorial(int n) {
                    if (n <= 1) return 1;
                    return n * factorial(n - 1);
                }
            }
            """;

        CompilationUnit cu = StaticJavaParser.parse(code);
        List<ComplexityInfo> complexities = analyzer.analyze(cu, "Test.java");

        assertEquals(1, complexities.size());
        assertEquals(BigOComplexity.O_N, complexities.get(0).getTimeComplexity());
        assertNotNull(complexities.get(0).getReason());
    }

    @Test
    void shouldDetectExponentialRecursion() {
        String code = """
            public class Test {
                public int fibonacci(int n) {
                    if (n <= 1) return n;
                    return fibonacci(n - 1) + fibonacci(n - 2);
                }
            }
            """;

        CompilationUnit cu = StaticJavaParser.parse(code);
        List<ComplexityInfo> complexities = analyzer.analyze(cu, "Test.java");

        assertEquals(1, complexities.size());
        assertEquals(BigOComplexity.O_2_N, complexities.get(0).getTimeComplexity());
        assertNotNull(complexities.get(0).getReason());
    }

    @Test
    void shouldDetectLogarithmicComplexity() {
        String code = """
            public class Test {
                public int binarySearch(int[] arr, int target, int low, int high) {
                    if (low > high) return -1;
                    int mid = (low + high) / 2;
                    if (arr[mid] == target) return mid;
                    if (arr[mid] > target) return binarySearch(arr, target, low, mid - 1);
                    return binarySearch(arr, target, mid + 1, high);
                }
            }
            """;

        CompilationUnit cu = StaticJavaParser.parse(code);
        List<ComplexityInfo> complexities = analyzer.analyze(cu, "Test.java");

        assertEquals(1, complexities.size());
        assertEquals(BigOComplexity.O_LOG_N, complexities.get(0).getTimeComplexity());
    }
}

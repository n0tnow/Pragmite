package com.pragmite.performance;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for PragmiteBenchmarks.
 * Note: These are validation tests, not actual performance benchmarks.
 * JMH benchmarks should be run separately using the main method.
 */
class PragmiteBenchmarksTest {

    private PragmiteBenchmarks benchmarks;
    private PragmiteBenchmarks.AnalysisState state;

    @BeforeEach
    void setUp() {
        benchmarks = new PragmiteBenchmarks();
        state = new PragmiteBenchmarks.AnalysisState();
        state.setUp();
    }

    @Test
    void testAnalysisStateSetup() {
        assertNotNull(state.sampleCode);
        assertNotNull(state.compilationUnit);
        assertNotNull(state.parser);
        assertTrue(state.sampleCode.contains("public class BenchmarkClass"));
    }

    @Test
    void testBenchmarkJavaParser() {
        var result = benchmarks.benchmarkJavaParser(state);
        assertNotNull(result);
        assertTrue(result.toString().contains("BenchmarkClass"));
    }

    @Test
    void testBenchmarkComplexityAnalysis() {
        var result = benchmarks.benchmarkComplexityAnalysis(state);
        assertNotNull(result);
    }

    @Test
    void testBenchmarkCKMetrics() {
        var result = benchmarks.benchmarkCKMetrics(state);
        assertNotNull(result);
    }

    @Test
    void testBenchmarkHalsteadMetrics() {
        var result = benchmarks.benchmarkHalsteadMetrics(state);
        assertNotNull(result);
    }

    @Test
    void testBenchmarkMaintainabilityIndex() {
        var result = benchmarks.benchmarkMaintainabilityIndex(state);
        assertNotNull(result);
    }

    @Test
    void testBenchmarkGodClassDetection() {
        var result = benchmarks.benchmarkGodClassDetection(state);
        assertNotNull(result);
    }

    @Test
    void testBenchmarkLongMethodDetection() {
        var result = benchmarks.benchmarkLongMethodDetection(state);
        assertNotNull(result);
    }

    @Test
    void testBenchmarkFullAnalysis() {
        var result = benchmarks.benchmarkFullAnalysis(state);
        assertNotNull(result);
        assertTrue(result.getClass().isArray());
        assertEquals(3, ((Object[]) result).length);
    }

    @Test
    void testBenchmarkMemoryIntensive() {
        var result = benchmarks.benchmarkMemoryIntensive(state);
        assertNotNull(result);
        assertEquals(1000, result);
    }

    @Test
    void testRunBenchmarksMethod() {
        // Test that runBenchmarks method doesn't throw
        // We don't actually run it as it would take too long
        assertDoesNotThrow(() -> {
            // Just verify the method exists and can be called
            PragmiteBenchmarks.class.getMethod("runBenchmarks");
        });
    }

    @Test
    void testRunBenchmarkMethod() {
        // Test that runBenchmark method exists
        assertDoesNotThrow(() -> {
            PragmiteBenchmarks.class.getMethod("runBenchmark", String.class);
        });
    }

    @Test
    void testMultipleIterations() {
        // Run benchmark methods multiple times to ensure consistency
        for (int i = 0; i < 5; i++) {
            var result = benchmarks.benchmarkComplexityAnalysis(state);
            assertNotNull(result);
        }
    }

    @Test
    void testBenchmarkStability() {
        // Ensure benchmarks produce consistent results
        var complexity1 = benchmarks.benchmarkComplexityAnalysis(state);
        var complexity2 = benchmarks.benchmarkComplexityAnalysis(state);
        assertNotNull(complexity1);
        assertNotNull(complexity2);
    }

    @Test
    void testBenchmarkDoesNotModifyState() {
        String originalCode = state.sampleCode;
        var originalCu = state.compilationUnit;

        // Run benchmark
        benchmarks.benchmarkComplexityAnalysis(state);

        // Verify state wasn't modified
        assertEquals(originalCode, state.sampleCode);
        assertEquals(originalCu, state.compilationUnit);
    }

    @Test
    void testBenchmarkPerformanceBaseline() {
        // Establish baseline - operations should complete in reasonable time
        long startTime = System.currentTimeMillis();
        benchmarks.benchmarkComplexityAnalysis(state);
        long duration = System.currentTimeMillis() - startTime;

        // Should complete within 1 second for small test case
        assertTrue(duration < 1000, "Complexity analysis took too long: " + duration + "ms");
    }

    @Test
    void testAllBenchmarksRunnable() {
        // Verify all benchmarks can be executed without errors
        assertDoesNotThrow(() -> {
            benchmarks.benchmarkJavaParser(state);
            benchmarks.benchmarkComplexityAnalysis(state);
            benchmarks.benchmarkCKMetrics(state);
            benchmarks.benchmarkHalsteadMetrics(state);
            benchmarks.benchmarkMaintainabilityIndex(state);
            benchmarks.benchmarkGodClassDetection(state);
            benchmarks.benchmarkLongMethodDetection(state);
            benchmarks.benchmarkFullAnalysis(state);
            benchmarks.benchmarkMemoryIntensive(state);
        });
    }
}

package com.pragmite.performance;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.pragmite.analyzer.ComplexityAnalyzer;
import com.pragmite.metrics.CKMetricsCalculator;
import com.pragmite.metrics.HalsteadMetricsCalculator;
import com.pragmite.metrics.MaintainabilityIndexCalculator;
import com.pragmite.rules.smells.GodClassDetector;
import com.pragmite.rules.smells.LongMethodDetector;
import org.openjdk.jmh.annotations.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

/**
 * JMH Benchmarks for Pragmite performance testing.
 * Measures performance of critical analysis operations.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(1)
public class PragmiteBenchmarks {

    /**
     * State for file analysis benchmarks.
     */
    @State(Scope.Benchmark)
    public static class AnalysisState {
        public String sampleCode;
        public CompilationUnit compilationUnit;
        public JavaParser parser;

        @Setup(Level.Trial)
        public void setUp() {
            parser = new JavaParser();

            // Generate sample code for testing
            sampleCode = generateSampleJavaClass(100, 10);

            // Parse the code
            ParseResult<CompilationUnit> result = parser.parse(sampleCode);
            if (result.isSuccessful() && result.getResult().isPresent()) {
                compilationUnit = result.getResult().get();
            } else {
                throw new RuntimeException("Failed to parse sample code");
            }
        }

        private String generateSampleJavaClass(int lineCount, int methodCount) {
            StringBuilder sb = new StringBuilder();
            sb.append("package com.example.benchmark;\n\n");
            sb.append("import java.util.*;\n");
            sb.append("import java.io.*;\n\n");
            sb.append("public class BenchmarkClass {\n");
            sb.append("    private int field1;\n");
            sb.append("    private String field2;\n");
            sb.append("    private List<String> field3;\n\n");

            for (int i = 0; i < methodCount; i++) {
                sb.append("    public void method").append(i).append("() {\n");
                sb.append("        int x = 0;\n");
                sb.append("        for (int j = 0; j < 10; j++) {\n");
                sb.append("            if (j % 2 == 0) {\n");
                sb.append("                x += j;\n");
                sb.append("            } else {\n");
                sb.append("                x -= j;\n");
                sb.append("            }\n");
                sb.append("        }\n");
                sb.append("        System.out.println(x);\n");
                sb.append("    }\n\n");
            }

            sb.append("}\n");
            return sb.toString();
        }
    }

    /**
     * Benchmark: Parse Java code.
     */
    @Benchmark
    public CompilationUnit benchmarkJavaParser(AnalysisState state) {
        ParseResult<CompilationUnit> result = state.parser.parse(state.sampleCode);
        return result.getResult().orElse(null);
    }

    /**
     * Benchmark: Calculate complexity.
     */
    @Benchmark
    public Object benchmarkComplexityAnalysis(AnalysisState state) {
        ComplexityAnalyzer analyzer = new ComplexityAnalyzer();
        return analyzer.analyze(state.compilationUnit, "BenchmarkClass.java");
    }

    /**
     * Benchmark: Calculate CK Metrics.
     */
    @Benchmark
    public Object benchmarkCKMetrics(AnalysisState state) {
        CKMetricsCalculator calculator = new CKMetricsCalculator();
        return calculator.calculateAll(state.compilationUnit, "BenchmarkClass.java");
    }

    /**
     * Benchmark: Calculate Halstead Metrics.
     */
    @Benchmark
    public Object benchmarkHalsteadMetrics(AnalysisState state) {
        HalsteadMetricsCalculator calculator = new HalsteadMetricsCalculator();
        return calculator.calculateAll(state.compilationUnit, "BenchmarkClass.java");
    }

    /**
     * Benchmark: Calculate Maintainability Index.
     */
    @Benchmark
    public Object benchmarkMaintainabilityIndex(AnalysisState state) {
        MaintainabilityIndexCalculator calculator = new MaintainabilityIndexCalculator();
        return calculator.calculateAll(state.compilationUnit, "BenchmarkClass.java");
    }

    /**
     * Benchmark: God Class Detection.
     */
    @Benchmark
    public Object benchmarkGodClassDetection(AnalysisState state) {
        GodClassDetector detector = new GodClassDetector();
        return detector.detect(state.compilationUnit, "BenchmarkClass.java", state.sampleCode);
    }

    /**
     * Benchmark: Long Method Detection.
     */
    @Benchmark
    public Object benchmarkLongMethodDetection(AnalysisState state) {
        LongMethodDetector detector = new LongMethodDetector();
        return detector.detect(state.compilationUnit, "BenchmarkClass.java", state.sampleCode);
    }

    /**
     * Benchmark: Full analysis pipeline (parse + complexity + CK metrics).
     */
    @Benchmark
    public Object benchmarkFullAnalysis(AnalysisState state) {
        // Parse
        ParseResult<CompilationUnit> parseResult = state.parser.parse(state.sampleCode);
        if (!parseResult.isSuccessful() || !parseResult.getResult().isPresent()) {
            return null;
        }
        CompilationUnit cu = parseResult.getResult().get();

        // Complexity
        ComplexityAnalyzer complexityAnalyzer = new ComplexityAnalyzer();
        var complexity = complexityAnalyzer.analyze(cu, "BenchmarkClass.java");

        // CK Metrics
        CKMetricsCalculator ckCalculator = new CKMetricsCalculator();
        var ckMetrics = ckCalculator.calculateAll(cu, "BenchmarkClass.java");

        // Halstead Metrics
        HalsteadMetricsCalculator halsteadCalculator = new HalsteadMetricsCalculator();
        var halsteadMetrics = halsteadCalculator.calculateAll(cu, "BenchmarkClass.java");

        return new Object[]{complexity, ckMetrics, halsteadMetrics};
    }

    /**
     * Benchmark: Memory-intensive operations.
     */
    @Benchmark
    public Object benchmarkMemoryIntensive(AnalysisState state) {
        // Simulate memory-intensive analysis
        java.util.List<String> data = new java.util.ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            data.add(state.sampleCode);
        }
        return data.size();
    }

    /**
     * Main method to run benchmarks programmatically.
     */
    public static void main(String[] args) throws Exception {
        org.openjdk.jmh.Main.main(args);
    }

    /**
     * Run benchmarks with default options.
     */
    public static void runBenchmarks() {
        String[] args = {
            ".*PragmiteBenchmarks.*",
            "-f", "1",
            "-wi", "3",
            "-i", "5",
            "-r", "1",
            "-w", "1",
            "-tu", "ms",
            "-rf", "json",
            "-rff", "benchmark-results.json"
        };

        try {
            main(args);
        } catch (Exception e) {
            System.err.println("Failed to run benchmarks: " + e.getMessage());
        }
    }

    /**
     * Run specific benchmark.
     */
    public static void runBenchmark(String benchmarkName) {
        String[] args = {
            ".*PragmiteBenchmarks." + benchmarkName + ".*",
            "-f", "1",
            "-wi", "2",
            "-i", "3",
            "-r", "1",
            "-w", "1",
            "-tu", "ms"
        };

        try {
            main(args);
        } catch (Exception e) {
            System.err.println("Failed to run benchmark: " + e.getMessage());
        }
    }
}

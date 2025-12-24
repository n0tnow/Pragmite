package com.pragmite.benchmark;

import java.util.ArrayList;
import java.util.List;

/**
 * Contains JMH benchmark execution results.
 * Stores performance metrics for each benchmarked method.
 */
public class BenchmarkResult {
    private List<MethodResult> methodResults = new ArrayList<>();
    private long executionTimeMs;

    public BenchmarkResult() {}

    public void addMethodResult(MethodResult result) {
        methodResults.add(result);
    }

    public List<MethodResult> getMethodResults() {
        return methodResults;
    }

    public void setExecutionTimeMs(long time) {
        this.executionTimeMs = time;
    }

    public long getExecutionTimeMs() {
        return executionTimeMs;
    }

    /**
     * Gets the slowest benchmark method.
     */
    public MethodResult getSlowestMethod() {
        return methodResults.stream()
            .max((a, b) -> Double.compare(a.getScore(), b.getScore()))
            .orElse(null);
    }

    /**
     * Gets the fastest benchmark method.
     */
    public MethodResult getFastestMethod() {
        return methodResults.stream()
            .min((a, b) -> Double.compare(a.getScore(), b.getScore()))
            .orElse(null);
    }

    /**
     * Formats results as a human-readable string.
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n=== JMH Benchmark Results ===\n\n");

        for (int i = 0; i < methodResults.size(); i++) {
            MethodResult mr = methodResults.get(i);
            sb.append(String.format("%2d. %-60s %10.3f ± %8.3f %s\n",
                i + 1, mr.getMethodName(), mr.getScore(), mr.getScoreError(), mr.getUnit()));
        }

        if (!methodResults.isEmpty()) {
            sb.append("\nPerformance Summary:\n");
            MethodResult fastest = getFastestMethod();
            MethodResult slowest = getSlowestMethod();
            if (fastest != null && slowest != null) {
                sb.append(String.format("  Fastest: %s (%.3f %s)\n",
                    fastest.getMethodName(), fastest.getScore(), fastest.getUnit()));
                sb.append(String.format("  Slowest: %s (%.3f %s)\n",
                    slowest.getMethodName(), slowest.getScore(), slowest.getUnit()));

                if (fastest.getScore() > 0) {
                    double speedup = slowest.getScore() / fastest.getScore();
                    sb.append(String.format("  Speedup ratio: %.2fx\n", speedup));
                }
            }
        }

        sb.append("\n============================\n");
        return sb.toString();
    }

    /**
     * Represents performance metrics for a single benchmarked method.
     */
    public static class MethodResult {
        private final String methodName;
        private final double score;        // Average execution time
        private final double scoreError;   // Error margin
        private final String unit;         // Time unit (e.g., "us/op" for microseconds per operation)

        public MethodResult(String methodName, double score, double scoreError, String unit) {
            this.methodName = methodName;
            this.score = score;
            this.scoreError = scoreError;
            this.unit = unit;
        }

        public String getMethodName() {
            return methodName;
        }

        public double getScore() {
            return score;
        }

        public double getScoreError() {
            return scoreError;
        }

        public String getUnit() {
            return unit;
        }

        @Override
        public String toString() {
            return String.format("%s: %.3f ± %.3f %s", methodName, score, scoreError, unit);
        }
    }
}

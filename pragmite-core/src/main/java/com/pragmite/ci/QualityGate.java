package com.pragmite.ci;

import com.pragmite.model.AnalysisResult;
import com.pragmite.model.CodeSmell;
import com.pragmite.model.QualityScore;
import com.pragmite.model.Severity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * CI/CD Quality Gate - enforces quality thresholds in build pipelines.
 *
 * Usage in CI/CD:
 * <pre>
 * QualityGate gate = QualityGate.builder()
 *     .minQualityScore(70.0)
 *     .maxBlockerSmells(0)
 *     .maxCriticalSmells(5)
 *     .build();
 *
 * if (!gate.passes(analysisResult)) {
 *     System.exit(1); // Fail the build
 * }
 * </pre>
 */
public class QualityGate {

    private final double minQualityScore;
    private final int maxBlockerSmells;
    private final int maxCriticalSmells;
    private final int maxMajorSmells;
    private final int maxTotalSmells;
    private final double minCoverage; // For future integration with JaCoCo
    private final boolean failOnNewSmells;

    private QualityGate(Builder builder) {
        this.minQualityScore = builder.minQualityScore;
        this.maxBlockerSmells = builder.maxBlockerSmells;
        this.maxCriticalSmells = builder.maxCriticalSmells;
        this.maxMajorSmells = builder.maxMajorSmells;
        this.maxTotalSmells = builder.maxTotalSmells;
        this.minCoverage = builder.minCoverage;
        this.failOnNewSmells = builder.failOnNewSmells;
    }

    /**
     * Checks if the analysis result passes all quality gates.
     * @return true if all gates pass, false otherwise
     */
    public boolean passes(AnalysisResult result) {
        List<String> failures = getFailures(result);
        return failures.isEmpty();
    }

    /**
     * Gets list of quality gate failures.
     * @return list of failure messages, empty if all gates pass
     */
    public List<String> getFailures(AnalysisResult result) {
        List<String> failures = new ArrayList<>();

        QualityScore score = result.getQualityScore();

        // Check overall quality score
        if (score.getOverallScore() < minQualityScore) {
            failures.add(String.format(
                "Quality score %.1f is below minimum %.1f",
                score.getOverallScore(), minQualityScore));
        }

        // Check code smells by severity
        Map<Severity, Long> smellsBySeverity = result.getCodeSmells().stream()
            .collect(Collectors.groupingBy(
                s -> s.getType().getDefaultSeverity(),
                Collectors.counting()));

        long blockerCount = smellsBySeverity.getOrDefault(Severity.BLOCKER, 0L);
        long criticalCount = smellsBySeverity.getOrDefault(Severity.CRITICAL, 0L);
        long majorCount = smellsBySeverity.getOrDefault(Severity.MAJOR, 0L);

        if (blockerCount > maxBlockerSmells) {
            failures.add(String.format(
                "Found %d BLOCKER issues (max allowed: %d)",
                blockerCount, maxBlockerSmells));
        }

        if (criticalCount > maxCriticalSmells) {
            failures.add(String.format(
                "Found %d CRITICAL issues (max allowed: %d)",
                criticalCount, maxCriticalSmells));
        }

        if (majorCount > maxMajorSmells) {
            failures.add(String.format(
                "Found %d MAJOR issues (max allowed: %d)",
                majorCount, maxMajorSmells));
        }

        // Check total smells
        if (result.getCodeSmells().size() > maxTotalSmells) {
            failures.add(String.format(
                "Found %d total issues (max allowed: %d)",
                result.getCodeSmells().size(), maxTotalSmells));
        }

        return failures;
    }

    /**
     * Prints quality gate result to console.
     */
    public void printResult(AnalysisResult result) {
        List<String> failures = getFailures(result);

        if (failures.isEmpty()) {
            System.out.println("\n✅ Quality Gate: PASSED");
            System.out.println("All quality thresholds met.");
        } else {
            System.out.println("\n❌ Quality Gate: FAILED");
            System.out.println("Quality gate violations:");
            for (String failure : failures) {
                System.out.println("  • " + failure);
            }
        }
    }

    /**
     * Creates exit code for CI/CD (0 = pass, 1 = fail).
     */
    public int getExitCode(AnalysisResult result) {
        return passes(result) ? 0 : 1;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for QualityGate with sensible defaults.
     */
    public static class Builder {
        private double minQualityScore = 60.0; // D grade minimum
        private int maxBlockerSmells = 0;      // No blockers allowed
        private int maxCriticalSmells = 5;
        private int maxMajorSmells = 20;
        private int maxTotalSmells = 100;
        private double minCoverage = 0.0;      // Not enforced by default
        private boolean failOnNewSmells = false;

        public Builder minQualityScore(double score) {
            this.minQualityScore = score;
            return this;
        }

        public Builder maxBlockerSmells(int max) {
            this.maxBlockerSmells = max;
            return this;
        }

        public Builder maxCriticalSmells(int max) {
            this.maxCriticalSmells = max;
            return this;
        }

        public Builder maxMajorSmells(int max) {
            this.maxMajorSmells = max;
            return this;
        }

        public Builder maxTotalSmells(int max) {
            this.maxTotalSmells = max;
            return this;
        }

        public Builder minCoverage(double coverage) {
            this.minCoverage = coverage;
            return this;
        }

        public Builder failOnNewSmells(boolean fail) {
            this.failOnNewSmells = fail;
            return this;
        }

        /**
         * Preset: Strict quality gate (for critical projects).
         */
        public Builder strict() {
            this.minQualityScore = 80.0; // B grade
            this.maxBlockerSmells = 0;
            this.maxCriticalSmells = 0;
            this.maxMajorSmells = 5;
            this.maxTotalSmells = 20;
            return this;
        }

        /**
         * Preset: Balanced quality gate (recommended for most projects).
         */
        public Builder balanced() {
            this.minQualityScore = 70.0; // C grade
            this.maxBlockerSmells = 0;
            this.maxCriticalSmells = 3;
            this.maxMajorSmells = 15;
            this.maxTotalSmells = 50;
            return this;
        }

        /**
         * Preset: Lenient quality gate (for legacy codebases).
         */
        public Builder lenient() {
            this.minQualityScore = 50.0; // F grade
            this.maxBlockerSmells = 1;
            this.maxCriticalSmells = 10;
            this.maxMajorSmells = 30;
            this.maxTotalSmells = 150;
            return this;
        }

        public QualityGate build() {
            return new QualityGate(this);
        }
    }
}

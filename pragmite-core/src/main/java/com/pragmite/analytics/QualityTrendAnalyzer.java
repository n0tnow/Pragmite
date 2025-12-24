package com.pragmite.analytics;

import com.pragmite.model.AnalysisResult;
import com.pragmite.model.CodeSmell;
import com.pragmite.model.Severity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Analyzes quality trends over time.
 * Identifies improvements, regressions, and patterns in code quality.
 *
 * Features:
 * - Trend detection (improving/degrading/stable)
 * - Quality score calculation
 * - Regression identification
 * - Improvement tracking
 */
public class QualityTrendAnalyzer {
    private static final Logger logger = LoggerFactory.getLogger(QualityTrendAnalyzer.class);

    /**
     * Compares two analysis results and generates a trend report.
     */
    public TrendReport analyzeTrend(AnalysisResult baseline, AnalysisResult current) {
        TrendReport report = new TrendReport(baseline, current);

        // Calculate quality scores
        double baselineScore = calculateQualityScore(baseline);
        double currentScore = calculateQualityScore(current);
        report.setBaselineScore(baselineScore);
        report.setCurrentScore(currentScore);

        // Determine trend direction
        double scoreDelta = currentScore - baselineScore;
        if (Math.abs(scoreDelta) < 5) {
            report.setTrend(Trend.STABLE);
        } else if (scoreDelta > 0) {
            report.setTrend(Trend.IMPROVING);
        } else {
            report.setTrend(Trend.DEGRADING);
        }

        // Identify new issues
        Set<String> baselineIssues = getIssueSignatures(baseline);
        Set<String> currentIssues = getIssueSignatures(current);

        Set<String> newIssues = new HashSet<>(currentIssues);
        newIssues.removeAll(baselineIssues);
        report.setNewIssuesCount(newIssues.size());

        Set<String> fixedIssues = new HashSet<>(baselineIssues);
        fixedIssues.removeAll(currentIssues);
        report.setFixedIssuesCount(fixedIssues.size());

        // Calculate time elapsed
        long daysBetween = ChronoUnit.DAYS.between(
            baseline.getAnalyzedAt(),
            current.getAnalyzedAt()
        );
        report.setDaysBetween(daysBetween);

        logger.info("Trend analysis: {} ({} new, {} fixed, {} days)",
                   report.getTrend(), newIssues.size(), fixedIssues.size(), daysBetween);

        return report;
    }

    /**
     * Calculates an overall quality score (0-100).
     * Higher score = better quality.
     */
    public double calculateQualityScore(AnalysisResult result) {
        if (result.getCodeSmells().isEmpty()) {
            return 100.0;
        }

        // Weight smells by severity
        Map<Severity, Integer> severityCounts = result.getCodeSmells().stream()
            .collect(Collectors.groupingBy(
                smell -> smell.getSeverity(),
                Collectors.summingInt(smell -> 1)
            ));

        int criticalCount = severityCounts.getOrDefault(Severity.CRITICAL, 0);
        int majorCount = severityCounts.getOrDefault(Severity.MAJOR, 0);
        int minorCount = severityCounts.getOrDefault(Severity.MINOR, 0);
        int infoCount = severityCounts.getOrDefault(Severity.INFO, 0);

        // Weighted deduction
        double deduction = (criticalCount * 10) +
                          (majorCount * 5) +
                          (minorCount * 2) +
                          (infoCount * 0.5);

        // Normalize based on project size (lines of code)
        int totalLines = result.getTotalLines();
        if (totalLines == 0) totalLines = 1000; // Default if not set
        double normalizedDeduction = (deduction / Math.max(totalLines / 100.0, 1)) * 10;

        double score = Math.max(0, 100 - normalizedDeduction);

        logger.debug("Quality score: {}/100 (C:{}, M:{}, m:{}, I:{})",
                    score, criticalCount, majorCount, minorCount, infoCount);

        return score;
    }

    /**
     * Identifies quality hotspots (files/classes with most issues).
     */
    public List<Hotspot> identifyHotspots(AnalysisResult result, int topN) {
        Map<String, List<CodeSmell>> smellsByFile = result.getCodeSmells().stream()
            .collect(Collectors.groupingBy(CodeSmell::getFilePath));

        List<Hotspot> hotspots = new ArrayList<>();

        for (Map.Entry<String, List<CodeSmell>> entry : smellsByFile.entrySet()) {
            String filePath = entry.getKey();
            List<CodeSmell> smells = entry.getValue();

            int severityScore = smells.stream()
                .mapToInt(smell -> smell.getSeverity() == Severity.CRITICAL ? 10 :
                                   smell.getSeverity() == Severity.MAJOR ? 5 :
                                   smell.getSeverity() == Severity.MINOR ? 2 : 1)
                .sum();

            hotspots.add(new Hotspot(filePath, smells.size(), severityScore));
        }

        return hotspots.stream()
            .sorted(Comparator.comparingInt(Hotspot::getSeverityScore).reversed())
            .limit(topN)
            .collect(Collectors.toList());
    }

    private Set<String> getIssueSignatures(AnalysisResult result) {
        return result.getCodeSmells().stream()
            .map(smell -> smell.getFilePath() + ":" + smell.getStartLine() + ":" + smell.getType())
            .collect(Collectors.toSet());
    }

    public enum Trend {
        IMPROVING,
        STABLE,
        DEGRADING
    }

    public static class TrendReport {
        private final AnalysisResult baseline;
        private final AnalysisResult current;
        private Trend trend;
        private double baselineScore;
        private double currentScore;
        private int newIssuesCount;
        private int fixedIssuesCount;
        private long daysBetween;

        public TrendReport(AnalysisResult baseline, AnalysisResult current) {
            this.baseline = baseline;
            this.current = current;
        }

        // Getters and setters
        public Trend getTrend() { return trend; }
        public void setTrend(Trend trend) { this.trend = trend; }
        public double getBaselineScore() { return baselineScore; }
        public void setBaselineScore(double baselineScore) { this.baselineScore = baselineScore; }
        public double getCurrentScore() { return currentScore; }
        public void setCurrentScore(double currentScore) { this.currentScore = currentScore; }
        public int getNewIssuesCount() { return newIssuesCount; }
        public void setNewIssuesCount(int count) { this.newIssuesCount = count; }
        public int getFixedIssuesCount() { return fixedIssuesCount; }
        public void setFixedIssuesCount(int count) { this.fixedIssuesCount = count; }
        public long getDaysBetween() { return daysBetween; }
        public void setDaysBetween(long days) { this.daysBetween = days; }
        public AnalysisResult getBaseline() { return baseline; }
        public AnalysisResult getCurrent() { return current; }

        @Override
        public String toString() {
            return String.format("Quality Trend: %s (%.1f → %.1f) | New: %d, Fixed: %d, Days: %d",
                               trend, baselineScore, currentScore,
                               newIssuesCount, fixedIssuesCount, daysBetween);
        }
    }

    public static class Hotspot {
        private final String filePath;
        private final int issueCount;
        private final int severityScore;

        public Hotspot(String filePath, int issueCount, int severityScore) {
            this.filePath = filePath;
            this.issueCount = issueCount;
            this.severityScore = severityScore;
        }

        public String getFilePath() { return filePath; }
        public int getIssueCount() { return issueCount; }
        public int getSeverityScore() { return severityScore; }

        @Override
        public String toString() {
            return String.format("%s: %d issues (severity score: %d)",
                               filePath, issueCount, severityScore);
        }
    }
}

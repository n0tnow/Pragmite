package com.pragmite.persistence;

import com.pragmite.model.AnalysisResult;

/**
 * Represents a comparison between two analysis results.
 * Shows quality trends and regressions.
 */
public class AnalysisComparison {
    private AnalysisResult baseline;
    private AnalysisResult current;

    private int smellDelta;          // Change in code smell count
    private double qualityScoreDelta; // Change in quality score
    private int lineDelta;            // Change in total lines

    public AnalysisComparison() {}

    public AnalysisResult getBaseline() {
        return baseline;
    }

    public void setBaseline(AnalysisResult baseline) {
        this.baseline = baseline;
    }

    public AnalysisResult getCurrent() {
        return current;
    }

    public void setCurrent(AnalysisResult current) {
        this.current = current;
    }

    public int getSmellDelta() {
        return smellDelta;
    }

    public void setSmellDelta(int smellDelta) {
        this.smellDelta = smellDelta;
    }

    public double getQualityScoreDelta() {
        return qualityScoreDelta;
    }

    public void setQualityScoreDelta(double qualityScoreDelta) {
        this.qualityScoreDelta = qualityScoreDelta;
    }

    public int getLineDelta() {
        return lineDelta;
    }

    public void setLineDelta(int lineDelta) {
        this.lineDelta = lineDelta;
    }

    /**
     * Checks if quality improved compared to baseline.
     */
    public boolean isImprovement() {
        return qualityScoreDelta > 0 || smellDelta < 0;
    }

    /**
     * Checks if quality regressed compared to baseline.
     */
    public boolean isRegression() {
        return qualityScoreDelta < -5.0 || smellDelta > 10;
    }

    /**
     * Formats the comparison as a human-readable report.
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n=== Analysis Comparison ===\n\n");

        sb.append("Baseline: ").append(baseline.getAnalyzedAt()).append("\n");
        sb.append("Current:  ").append(current.getAnalyzedAt()).append("\n\n");

        // Quality score change
        sb.append("Quality Score:\n");
        if (baseline.getQualityScore() != null && current.getQualityScore() != null) {
            sb.append(String.format("  Baseline: %.1f\n", baseline.getQualityScore().getOverallScore()));
            sb.append(String.format("  Current:  %.1f\n", current.getQualityScore().getOverallScore()));
            sb.append(String.format("  Change:   %+.1f %s\n\n",
                qualityScoreDelta,
                qualityScoreDelta > 0 ? "✅ (Improved)" : qualityScoreDelta < 0 ? "❌ (Regressed)" : "(No change)"));
        }

        // Code smell change
        sb.append("Code Smells:\n");
        sb.append(String.format("  Baseline: %d\n", baseline.getCodeSmells().size()));
        sb.append(String.format("  Current:  %d\n", current.getCodeSmells().size()));
        sb.append(String.format("  Change:   %+d %s\n\n",
            smellDelta,
            smellDelta < 0 ? "✅ (Reduced)" : smellDelta > 0 ? "❌ (Increased)" : "(No change)"));

        // Line count change
        sb.append("Total Lines:\n");
        sb.append(String.format("  Baseline: %,d\n", baseline.getTotalLines()));
        sb.append(String.format("  Current:  %,d\n", current.getTotalLines()));
        sb.append(String.format("  Change:   %+,d\n\n", lineDelta));

        // Overall verdict
        sb.append("Overall: ");
        if (isImprovement()) {
            sb.append("✅ Quality IMPROVED\n");
        } else if (isRegression()) {
            sb.append("❌ Quality REGRESSED\n");
        } else {
            sb.append("➖ No significant change\n");
        }

        sb.append("===========================\n");
        return sb.toString();
    }
}

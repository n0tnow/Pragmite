package com.pragmite.metrics;

/**
 * Container for Maintainability Index metrics.
 *
 * Provides MI calculation and interpretation for code maintainability assessment.
 */
public class MaintainabilityIndex {

    private final String methodName;
    private final String filePath;
    private final int lineNumber;

    private final double halsteadVolume;
    private final int cyclomaticComplexity;
    private final int linesOfCode;

    public MaintainabilityIndex(String methodName, String filePath, int lineNumber,
                               double halsteadVolume, int cyclomaticComplexity, int linesOfCode) {
        this.methodName = methodName;
        this.filePath = filePath;
        this.lineNumber = lineNumber;
        this.halsteadVolume = halsteadVolume;
        this.cyclomaticComplexity = cyclomaticComplexity;
        this.linesOfCode = linesOfCode;
    }

    // Getters

    public String getMethodName() {
        return methodName;
    }

    public String getFilePath() {
        return filePath;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public double getHalsteadVolume() {
        return halsteadVolume;
    }

    public int getCyclomaticComplexity() {
        return cyclomaticComplexity;
    }

    public int getLinesOfCode() {
        return linesOfCode;
    }

    /**
     * Calculates Maintainability Index (original formula).
     * MI = 171 - 5.2 * ln(V) - 0.23 * CC - 16.2 * ln(LOC)
     *
     * Range: negative infinity to 171
     */
    public double getMI() {
        // Handle edge cases
        if (halsteadVolume <= 0 || linesOfCode <= 0) {
            return 0;
        }

        double lnVolume = Math.log(halsteadVolume);
        double lnLOC = Math.log(linesOfCode);

        double mi = 171.0
                  - 5.2 * lnVolume
                  - 0.23 * cyclomaticComplexity
                  - 16.2 * lnLOC;

        return mi;
    }

    /**
     * Calculates normalized Maintainability Index (0-100 scale).
     * MI_norm = max(0, (MI / 171) * 100)
     *
     * Range: 0 to 100
     */
    public double getNormalizedMI() {
        double mi = getMI();
        double normalized = (mi / 171.0) * 100.0;
        return Math.max(0, Math.min(100, normalized));
    }

    /**
     * Returns maintainability level based on MI score.
     * - 85-100: High (GREEN)
     * - 65-84: Moderate (YELLOW)
     * - 0-64: Low (RED)
     */
    public MaintainabilityLevel getLevel() {
        double mi = getNormalizedMI();

        if (mi >= 85) {
            return MaintainabilityLevel.HIGH;
        } else if (mi >= 65) {
            return MaintainabilityLevel.MODERATE;
        } else {
            return MaintainabilityLevel.LOW;
        }
    }

    /**
     * Returns color code for maintainability level.
     */
    public String getColorCode() {
        return switch (getLevel()) {
            case HIGH -> "GREEN";
            case MODERATE -> "YELLOW";
            case LOW -> "RED";
        };
    }

    /**
     * Returns true if the method has low maintainability.
     */
    public boolean isLowMaintainability() {
        return getLevel() == MaintainabilityLevel.LOW;
    }

    /**
     * Returns true if the method has high maintainability.
     */
    public boolean isHighMaintainability() {
        return getLevel() == MaintainabilityLevel.HIGH;
    }

    /**
     * Returns a recommendation based on MI score.
     */
    public String getRecommendation() {
        return switch (getLevel()) {
            case HIGH -> "Good maintainability. Code is easy to understand and modify.";
            case MODERATE -> "Moderate maintainability. Consider simplifying complex parts.";
            case LOW -> "Low maintainability. Refactoring recommended to improve code quality.";
        };
    }

    /**
     * Identifies the main contributing factor to low MI.
     */
    public String getMainIssue() {
        if (getLevel() == MaintainabilityLevel.HIGH) {
            return "None - code is maintainable";
        }

        // Identify which factor contributes most
        if (cyclomaticComplexity > 10) {
            return "High cyclomatic complexity (" + cyclomaticComplexity + ")";
        }

        if (halsteadVolume > 1000) {
            return "High Halstead volume (" + String.format("%.0f", halsteadVolume) + ")";
        }

        if (linesOfCode > 50) {
            return "Long method (" + linesOfCode + " LOC)";
        }

        return "Multiple factors affecting maintainability";
    }

    @Override
    public String toString() {
        return String.format(
            "Maintainability Index for %s (line %d):\n" +
            "  Inputs:\n" +
            "    Halstead Volume: %.2f\n" +
            "    Cyclomatic Complexity: %d\n" +
            "    Lines of Code: %d\n" +
            "  Results:\n" +
            "    MI (raw): %.2f\n" +
            "    MI (normalized): %.2f / 100\n" +
            "    Level: %s (%s)\n" +
            "    Recommendation: %s\n" +
            "    Main Issue: %s",
            methodName, lineNumber,
            halsteadVolume, cyclomaticComplexity, linesOfCode,
            getMI(), getNormalizedMI(),
            getLevel(), getColorCode(),
            getRecommendation(),
            getMainIssue()
        );
    }

    /**
     * Returns a compact one-line representation.
     */
    public String toCompactString() {
        return String.format(
            "%s: MI=%.1f [%s] - %s",
            methodName,
            getNormalizedMI(),
            getLevel(),
            getMainIssue()
        );
    }

    /**
     * Enum for maintainability levels.
     */
    public enum MaintainabilityLevel {
        HIGH,      // 85-100
        MODERATE,  // 65-84
        LOW        // 0-64
    }
}

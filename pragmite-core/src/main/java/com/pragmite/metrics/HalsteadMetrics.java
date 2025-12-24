package com.pragmite.metrics;

/**
 * Container for Halstead complexity metrics.
 *
 * Provides both basic counts and derived metrics for code complexity analysis.
 */
public class HalsteadMetrics {

    private final String methodName;
    private final String filePath;
    private final int lineNumber;

    // Basic counts
    private final int n1;  // Distinct operators
    private final int n2;  // Distinct operands
    private final int N1;  // Total operators
    private final int N2;  // Total operands

    public HalsteadMetrics(String methodName, String filePath, int lineNumber,
                          int n1, int n2, int N1, int N2) {
        this.methodName = methodName;
        this.filePath = filePath;
        this.lineNumber = lineNumber;
        this.n1 = n1;
        this.n2 = n2;
        this.N1 = N1;
        this.N2 = N2;
    }

    // Getters for basic counts

    public String getMethodName() {
        return methodName;
    }

    public String getFilePath() {
        return filePath;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public int getDistinctOperators() {
        return n1;
    }

    public int getDistinctOperands() {
        return n2;
    }

    public int getTotalOperators() {
        return N1;
    }

    public int getTotalOperands() {
        return N2;
    }

    // Derived metrics

    /**
     * Program Vocabulary: n = n1 + n2
     * Total number of unique symbols used.
     */
    public int getVocabulary() {
        return n1 + n2;
    }

    /**
     * Program Length: N = N1 + N2
     * Total number of operator and operand occurrences.
     */
    public int getLength() {
        return N1 + N2;
    }

    /**
     * Calculated Program Length: N^ = n1 * log2(n1) + n2 * log2(n2)
     * Theoretical minimum length for the algorithm.
     */
    public double getCalculatedLength() {
        if (n1 == 0 && n2 == 0) {
            return 0;
        }

        double part1 = n1 > 0 ? n1 * log2(n1) : 0;
        double part2 = n2 > 0 ? n2 * log2(n2) : 0;

        return part1 + part2;
    }

    /**
     * Program Volume: V = N * log2(n)
     * Size of the implementation in bits.
     * Higher values indicate larger, more complex code.
     */
    public double getVolume() {
        int n = getVocabulary();
        int N = getLength();

        if (n == 0) {
            return 0;
        }

        return N * log2(n);
    }

    /**
     * Program Difficulty: D = (n1/2) * (N2/n2)
     * How difficult the program is to write or understand.
     * Higher values indicate more difficult code.
     */
    public double getDifficulty() {
        if (n2 == 0) {
            return 0;
        }

        return (n1 / 2.0) * (N2 / (double) n2);
    }

    /**
     * Program Effort: E = D * V
     * Mental effort required to write the code.
     * Higher values indicate more effort needed.
     */
    public double getEffort() {
        return getDifficulty() * getVolume();
    }

    /**
     * Time to Program: T = E / 18 (in seconds)
     * Estimated time to write the code.
     * Based on Stroud number (18 elementary discriminations per second).
     */
    public double getTimeToProgram() {
        return getEffort() / 18.0;
    }

    /**
     * Number of Delivered Bugs: B = V / 3000
     * Estimated number of errors in the implementation.
     * Empirically derived constant.
     */
    public double getDeliveredBugs() {
        return getVolume() / 3000.0;
    }

    /**
     * Returns complexity level based on Volume.
     * - < 100: Simple
     * - 100-1000: Moderate
     * - > 1000: Complex
     */
    public String getComplexityLevel() {
        double volume = getVolume();

        if (volume < 100) {
            return "Simple";
        } else if (volume < 1000) {
            return "Moderate";
        } else {
            return "Complex";
        }
    }

    /**
     * Returns true if the method is overly complex (V > 1000).
     */
    public boolean isComplex() {
        return getVolume() > 1000;
    }

    /**
     * Returns true if the method is difficult to understand (D > 30).
     */
    public boolean isDifficult() {
        return getDifficulty() > 30;
    }

    /**
     * Returns true if the method likely has bugs (B > 0.1).
     */
    public boolean isErrorProne() {
        return getDeliveredBugs() > 0.1;
    }

    private double log2(double value) {
        if (value <= 0) {
            return 0;
        }
        return Math.log(value) / Math.log(2);
    }

    @Override
    public String toString() {
        return String.format(
            "Halstead Metrics for %s (line %d):\n" +
            "  Basic Counts:\n" +
            "    n1 (distinct operators): %d\n" +
            "    n2 (distinct operands): %d\n" +
            "    N1 (total operators): %d\n" +
            "    N2 (total operands): %d\n" +
            "  Derived Metrics:\n" +
            "    Vocabulary (n): %d\n" +
            "    Length (N): %d\n" +
            "    Calculated Length (N^): %.2f\n" +
            "    Volume (V): %.2f\n" +
            "    Difficulty (D): %.2f\n" +
            "    Effort (E): %.2f\n" +
            "    Time (T): %.2f seconds\n" +
            "    Bugs (B): %.4f\n" +
            "  Complexity: %s",
            methodName, lineNumber,
            n1, n2, N1, N2,
            getVocabulary(), getLength(), getCalculatedLength(),
            getVolume(), getDifficulty(), getEffort(),
            getTimeToProgram(), getDeliveredBugs(),
            getComplexityLevel()
        );
    }

    /**
     * Returns a compact one-line representation.
     */
    public String toCompactString() {
        return String.format(
            "%s: V=%.0f D=%.1f E=%.0f B=%.3f [%s]",
            methodName,
            getVolume(),
            getDifficulty(),
            getEffort(),
            getDeliveredBugs(),
            getComplexityLevel()
        );
    }
}

package com.pragmite.model;

/**
 * Big-O karmaşıklık notasyonu.
 */
public enum BigOComplexity {
    O_1("O(1)", "Constant", 1),
    O_LOG_N("O(log n)", "Logarithmic", 2),
    O_N("O(n)", "Linear", 3),
    O_N_LOG_N("O(n log n)", "Linearithmic", 4),
    O_N_SQUARED("O(n²)", "Quadratic", 5),
    O_N_CUBED("O(n³)", "Cubic", 6),
    O_2_N("O(2ⁿ)", "Exponential", 7),
    O_N_FACTORIAL("O(n!)", "Factorial", 8),
    UNKNOWN("Unknown", "Unknown", 0);

    private final String notation;
    private final String name;
    private final int severity;

    BigOComplexity(String notation, String name, int severity) {
        this.notation = notation;
        this.name = name;
        this.severity = severity;
    }

    public String getNotation() { return notation; }
    public String getName() { return name; }
    public int getSeverity() { return severity; }

    /**
     * İki karmaşıklığın baskın olanını döndürür.
     */
    public static BigOComplexity dominant(BigOComplexity a, BigOComplexity b) {
        if (a == null) return b;
        if (b == null) return a;
        return a.severity >= b.severity ? a : b;
    }

    /**
     * İç içe döngüler için karmaşıklıkları çarpar.
     */
    public static BigOComplexity multiply(BigOComplexity a, BigOComplexity b) {
        if (a == null || b == null) return UNKNOWN;

        // O(n) * O(n) = O(n²)
        if (a == O_N && b == O_N) return O_N_SQUARED;
        // O(n²) * O(n) = O(n³)
        if ((a == O_N_SQUARED && b == O_N) || (a == O_N && b == O_N_SQUARED)) return O_N_CUBED;
        // O(n) * O(log n) = O(n log n)
        if ((a == O_N && b == O_LOG_N) || (a == O_LOG_N && b == O_N)) return O_N_LOG_N;
        // O(1) ile çarpım değiştirmez
        if (a == O_1) return b;
        if (b == O_1) return a;

        // Diğer durumlar için baskın olanı döndür
        return dominant(a, b);
    }

    @Override
    public String toString() {
        return notation;
    }
}

package com.pragmite.model;

/**
 * Pragmatic Programmer ilkelerine göre kalite skorları.
 */
public class QualityScore {
    private double dryScore;          // Don't Repeat Yourself (0-100)
    private double orthogonalityScore; // Bağımsızlık/düşük bağlılık (0-100)
    private double correctnessScore;   // Doğruluk (0-100)
    private double perfScore;          // Performans (0-100)
    private double pragmaticScore;     // Birleşik skor (0-100)

    // Ağırlıklar (yapılandırılabilir)
    private static final double DRY_WEIGHT = 0.30;
    private static final double ORTHO_WEIGHT = 0.30;
    private static final double CORRECT_WEIGHT = 0.25;
    private static final double PERF_WEIGHT = 0.15;

    public QualityScore() {
        this.dryScore = 100;
        this.orthogonalityScore = 100;
        this.correctnessScore = 100;
        this.perfScore = 100;
        calculatePragmaticScore();
    }

    public QualityScore(double dryScore, double orthogonalityScore, double correctnessScore, double perfScore) {
        this.dryScore = Math.max(0, Math.min(100, dryScore));
        this.orthogonalityScore = Math.max(0, Math.min(100, orthogonalityScore));
        this.correctnessScore = Math.max(0, Math.min(100, correctnessScore));
        this.perfScore = Math.max(0, Math.min(100, perfScore));
        calculatePragmaticScore();
    }

    public double getOverallScore() {
        return pragmaticScore;
    }

    public void calculatePragmaticScore() {
        this.pragmaticScore = (dryScore * DRY_WEIGHT) +
                              (orthogonalityScore * ORTHO_WEIGHT) +
                              (correctnessScore * CORRECT_WEIGHT) +
                              (perfScore * PERF_WEIGHT);
    }

    public String getGrade() {
        if (pragmaticScore >= 90) return "A";
        if (pragmaticScore >= 80) return "B";
        if (pragmaticScore >= 70) return "C";
        if (pragmaticScore >= 60) return "D";
        return "F";
    }

    // Getters and Setters
    public double getDryScore() { return dryScore; }
    public void setDryScore(double dryScore) {
        this.dryScore = Math.max(0, Math.min(100, dryScore));
        calculatePragmaticScore();
    }

    public double getOrthogonalityScore() { return orthogonalityScore; }
    public void setOrthogonalityScore(double orthogonalityScore) {
        this.orthogonalityScore = Math.max(0, Math.min(100, orthogonalityScore));
        calculatePragmaticScore();
    }

    public double getCorrectnessScore() { return correctnessScore; }
    public void setCorrectnessScore(double correctnessScore) {
        this.correctnessScore = Math.max(0, Math.min(100, correctnessScore));
        calculatePragmaticScore();
    }

    public double getPerfScore() { return perfScore; }
    public void setPerfScore(double perfScore) {
        this.perfScore = Math.max(0, Math.min(100, perfScore));
        calculatePragmaticScore();
    }

    public double getPragmaticScore() { return pragmaticScore; }

    @Override
    public String toString() {
        return String.format(
            "Quality Score: %.1f (%s)\n" +
            "  DRY: %.1f | Orthogonality: %.1f | Correctness: %.1f | Performance: %.1f",
            pragmaticScore, getGrade(),
            dryScore, orthogonalityScore, correctnessScore, perfScore);
    }
}

package com.pragmite.scoring;

import com.pragmite.model.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Pragmatic Programmer prensipleri bazında kalite skoru hesaplayıcı.
 * DRY, Orthogonality, Correctness ve Performance skorlarını hesaplar.
 */
public class ScoreCalculator {

    private static final double MAX_SCORE = 100.0;

    /**
     * Analiz sonuçlarından genel kalite skorunu hesaplar.
     */
    public QualityScore calculate(List<FileAnalysis> fileAnalyses, List<CodeSmell> allSmells) {
        double dryScore = calculateDryScore(allSmells);
        double orthogonalityScore = calculateOrthogonalityScore(allSmells);
        double correctnessScore = calculateCorrectnessScore(allSmells);
        double performanceScore = calculatePerformanceScore(allSmells, fileAnalyses);

        return new QualityScore(dryScore, orthogonalityScore, correctnessScore, performanceScore);
    }

    /**
     * DRY (Don't Repeat Yourself) skoru.
     * Duplicate code, copy-paste gibi tekrar eden kod kokularını değerlendirir.
     */
    private double calculateDryScore(List<CodeSmell> smells) {
        long dryViolations = smells.stream()
            .filter(s -> s.getType().getPragmaticPrinciple() == PragmaticPrinciple.DRY)
            .count();

        return calculateScoreFromViolations(dryViolations, 10);
    }

    /**
     * Orthogonality skoru.
     * God Class, Feature Envy, High Coupling gibi bağımlılık kokularını değerlendirir.
     */
    private double calculateOrthogonalityScore(List<CodeSmell> smells) {
        long orthogonalityViolations = smells.stream()
            .filter(s -> s.getType().getPragmaticPrinciple() == PragmaticPrinciple.ORTHOGONALITY)
            .count();

        return calculateScoreFromViolations(orthogonalityViolations, 8);
    }

    /**
     * Correctness skoru.
     * Empty Catch, Null Check eksikliği, Magic Numbers gibi doğruluk kokularını değerlendirir.
     */
    private double calculateCorrectnessScore(List<CodeSmell> smells) {
        long correctnessViolations = smells.stream()
            .filter(s -> s.getType().getPragmaticPrinciple() == PragmaticPrinciple.CORRECTNESS)
            .count();

        return calculateScoreFromViolations(correctnessViolations, 15);
    }

    /**
     * Performance skoru.
     * Big-O karmaşıklığı, String concatenation in loop gibi performans kokularını değerlendirir.
     */
    private double calculatePerformanceScore(List<CodeSmell> smells, List<FileAnalysis> fileAnalyses) {
        // Performans ihlalleri
        long performanceViolations = smells.stream()
            .filter(s -> s.getType().getPragmaticPrinciple() == PragmaticPrinciple.PERFORMANCE)
            .count();

        // Yüksek karmaşıklıklı metotları da hesaba kat
        long highComplexityMethods = fileAnalyses.stream()
            .flatMap(f -> f.getMethods().stream())
            .filter(m -> m.getBigOComplexity().ordinal() >= BigOComplexity.O_N_SQUARED.ordinal())
            .count();

        long totalViolations = performanceViolations + (highComplexityMethods / 2);

        return calculateScoreFromViolations(totalViolations, 12);
    }

    /**
     * İhlal sayısından skor hesaplar.
     * Her ihlal skoru düşürür, ancak minimum 0'a iner.
     */
    private double calculateScoreFromViolations(long violations, double penaltyPerViolation) {
        double score = MAX_SCORE - (violations * penaltyPerViolation);
        return Math.max(0, Math.min(MAX_SCORE, score));
    }

    /**
     * Dosya bazlı kalite raporu oluşturur.
     */
    public Map<String, Double> calculateFileScores(List<FileAnalysis> fileAnalyses, List<CodeSmell> allSmells) {
        Map<String, List<CodeSmell>> smellsByFile = allSmells.stream()
            .collect(Collectors.groupingBy(CodeSmell::getFilePath));

        return fileAnalyses.stream()
            .collect(Collectors.toMap(
                FileAnalysis::getFilePath,
                fa -> calculateFileScore(fa, smellsByFile.getOrDefault(fa.getFilePath(), List.of()))
            ));
    }

    /**
     * Tek bir dosya için skor hesaplar.
     */
    private double calculateFileScore(FileAnalysis fileAnalysis, List<CodeSmell> smells) {
        if (smells.isEmpty()) {
            return MAX_SCORE;
        }

        // Severity'ye göre ağırlıklı penalty
        double totalPenalty = smells.stream()
            .mapToDouble(s -> getSeverityPenalty(s.getType().getDefaultSeverity()))
            .sum();

        return Math.max(0, MAX_SCORE - totalPenalty);
    }

    /**
     * Severity'ye göre penalty değeri döner.
     */
    private double getSeverityPenalty(Severity severity) {
        return switch (severity) {
            case BLOCKER -> 25.0;
            case CRITICAL -> 15.0;
            case MAJOR -> 10.0;
            case MINOR -> 5.0;
            case INFO -> 2.0;
        };
    }

    /**
     * Genel proje sağlık durumunu belirler.
     */
    public String getHealthStatus(QualityScore score) {
        double overall = score.getOverallScore();

        if (overall >= 90) return "EXCELLENT";
        if (overall >= 75) return "GOOD";
        if (overall >= 60) return "MODERATE";
        if (overall >= 40) return "POOR";
        return "CRITICAL";
    }

    /**
     * Genel proje sağlık durumu için emoji döner.
     */
    public String getHealthEmoji(QualityScore score) {
        double overall = score.getOverallScore();

        if (overall >= 90) return "🟢";
        if (overall >= 75) return "🟡";
        if (overall >= 60) return "🟠";
        if (overall >= 40) return "🔴";
        return "⛔";
    }
}

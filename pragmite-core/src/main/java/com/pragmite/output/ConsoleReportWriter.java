package com.pragmite.output;

import com.pragmite.model.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Console output report writer.
 */
public class ConsoleReportWriter {

    private final boolean verbose;

    public ConsoleReportWriter(boolean verbose) {
        this.verbose = verbose;
    }

    public void write(AnalysisResult result) {
        printSummary(result);
        printQualityScore(result.getQualityScore());
        printSmellsSummary(result.getCodeSmells());

        if (verbose) {
            printDetailedSmells(result.getCodeSmells());
            printComplexityDetails(result.getFileAnalyses());
        }

        printConclusion(result);
    }

    private void printSummary(AnalysisResult result) {
        System.out.println("\n┌─────────────────────────────────────────────────────────────┐");
        System.out.println("│                      ANALYSIS SUMMARY                        │");
        System.out.println("├─────────────────────────────────────────────────────────────┤");
        System.out.printf("│  Analyzed files: %-42d │%n", result.getFileAnalyses().size());
        System.out.printf("│  Total methods: %-43d │%n", countMethods(result));
        System.out.printf("│  Detected code smells: %-36d │%n", result.getCodeSmells().size());
        System.out.printf("│  Analysis time: %-43s │%n", formatDuration(result.getAnalysisDurationMs()));
        System.out.println("└─────────────────────────────────────────────────────────────┘");
    }

    private void printQualityScore(QualityScore score) {
        System.out.println("\n┌─────────────────────────────────────────────────────────────┐");
        System.out.println("│                    QUALITY SCORE                             │");
        System.out.println("├─────────────────────────────────────────────────────────────┤");
        System.out.printf("│  %s Overall Score: %.1f/100 (%s)%n",
            getScoreEmoji(score.getOverallScore()),
            score.getOverallScore(),
            score.getGrade());
        System.out.println("├─────────────────────────────────────────────────────────────┤");
        System.out.printf("│  %s DRY (Don't Repeat):      %s %.1f/100%n",
            getScoreEmoji(score.getDryScore()), getProgressBar(score.getDryScore()), score.getDryScore());
        System.out.printf("│  %s Orthogonality:           %s %.1f/100%n",
            getScoreEmoji(score.getOrthogonalityScore()), getProgressBar(score.getOrthogonalityScore()), score.getOrthogonalityScore());
        System.out.printf("│  %s Correctness:             %s %.1f/100%n",
            getScoreEmoji(score.getCorrectnessScore()), getProgressBar(score.getCorrectnessScore()), score.getCorrectnessScore());
        System.out.printf("│  %s Performance:             %s %.1f/100%n",
            getScoreEmoji(score.getPerfScore()), getProgressBar(score.getPerfScore()), score.getPerfScore());
        System.out.println("└─────────────────────────────────────────────────────────────┘");
    }

    private void printSmellsSummary(List<CodeSmell> smells) {
        if (smells.isEmpty()) {
            System.out.println("\n✓ No code smells detected!");
            return;
        }

        System.out.println("\n┌─────────────────────────────────────────────────────────────┐");
        System.out.println("│                   CODE SMELLS SUMMARY                        │");
        System.out.println("├─────────────────────────────────────────────────────────────┤");

        // Group by severity
        Map<Severity, Long> bySeverity = smells.stream()
            .collect(Collectors.groupingBy(s -> s.getType().getDefaultSeverity(), Collectors.counting()));

        for (Severity severity : Severity.values()) {
            long count = bySeverity.getOrDefault(severity, 0L);
            if (count > 0) {
                System.out.printf("│  %s %-12s: %d%n", getSeverityIcon(severity), severity.name(), count);
            }
        }

        System.out.println("├─────────────────────────────────────────────────────────────┤");

        // Group by type
        Map<CodeSmellType, Long> byType = smells.stream()
            .collect(Collectors.groupingBy(CodeSmell::getType, Collectors.counting()));

        byType.entrySet().stream()
            .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
            .limit(5)
            .forEach(e -> System.out.printf("│  • %-40s: %d%n", e.getKey().getName(), e.getValue()));

        System.out.println("└─────────────────────────────────────────────────────────────┘");
    }

    private void printDetailedSmells(List<CodeSmell> smells) {
        if (smells.isEmpty()) return;

        System.out.println("\n┌─────────────────────────────────────────────────────────────┐");
        System.out.println("│                  DETAILED CODE SMELLS                        │");
        System.out.println("└─────────────────────────────────────────────────────────────┘");

        // Group by file
        Map<String, List<CodeSmell>> byFile = smells.stream()
            .collect(Collectors.groupingBy(CodeSmell::getFilePath));

        byFile.forEach((file, fileSmells) -> {
            System.out.println("\n📁 " + shortenPath(file));
            fileSmells.forEach(smell -> {
                System.out.printf("   %s [Line %d] %s%n",
                    getSeverityIcon(smell.getType().getDefaultSeverity()),
                    smell.getLine(),
                    smell.getMessage());
                if (smell.getSuggestion() != null) {
                    System.out.printf("      💡 Suggestion: %s%n", smell.getSuggestion());
                }
            });
        });
    }

    private void printComplexityDetails(List<FileAnalysis> fileAnalyses) {
        System.out.println("\n┌─────────────────────────────────────────────────────────────┐");
        System.out.println("│                  COMPLEXITY ANALYSIS                         │");
        System.out.println("└─────────────────────────────────────────────────────────────┘");

        fileAnalyses.stream()
            .flatMap(f -> f.getMethods().stream()
                .map(m -> new Object[]{f.getFilePath(), m}))
            .filter(arr -> {
                MethodInfo m = (MethodInfo) arr[1];
                return m.getCyclomaticComplexity() > 5 ||
                       (m.getBigOComplexity() != null && m.getBigOComplexity().ordinal() >= BigOComplexity.O_N_SQUARED.ordinal());
            })
            .limit(10)
            .forEach(arr -> {
                String file = shortenPath((String) arr[0]);
                MethodInfo m = (MethodInfo) arr[1];
                System.out.printf("  • %s::%s%n", file, m.getName());
                System.out.printf("      CC: %d | Big-O: %s | Lines: %d%n",
                    m.getCyclomaticComplexity(),
                    m.getBigOComplexity() != null ? m.getBigOComplexity().getNotation() : "O(1)",
                    m.getLineCount());
            });
    }

    private void printConclusion(AnalysisResult result) {
        System.out.println("\n════════════════════════════════════════════════════════════════");

        QualityScore score = result.getQualityScore();
        double overall = score.getOverallScore();

        if (overall >= 90) {
            System.out.println("🎉 Congratulations! Your code quality is excellent.");
        } else if (overall >= 75) {
            System.out.println("👍 Your code quality is good, minor improvements possible.");
        } else if (overall >= 60) {
            System.out.println("⚠️  Your code quality is average. Some refactoring recommended.");
        } else if (overall >= 40) {
            System.out.println("🔶 Your code quality is low. Serious refactoring needed.");
        } else {
            System.out.println("🚨 Your code quality is critical! Urgent attention required.");
        }

        System.out.println("════════════════════════════════════════════════════════════════\n");
    }

    // Helper methods
    private int countMethods(AnalysisResult result) {
        return result.getFileAnalyses().stream()
            .mapToInt(f -> f.getMethods().size())
            .sum();
    }

    private String formatDuration(long ms) {
        if (ms < 1000) return ms + " ms";
        return String.format("%.2f seconds", ms / 1000.0);
    }

    private String getScoreEmoji(double score) {
        if (score >= 90) return "🟢";
        if (score >= 75) return "🟡";
        if (score >= 60) return "🟠";
        if (score >= 40) return "🔴";
        return "⛔";
    }

    private String getSeverityIcon(Severity severity) {
        return switch (severity) {
            case BLOCKER -> "⛔";
            case CRITICAL -> "🔴";
            case MAJOR -> "🟠";
            case MINOR -> "🟡";
            case INFO -> "🔵";
        };
    }

    private String getProgressBar(double score) {
        int filled = (int) (score / 10);
        int empty = 10 - filled;
        return "█".repeat(filled) + "░".repeat(empty);
    }

    private String shortenPath(String path) {
        int idx = path.lastIndexOf(java.io.File.separator);
        if (idx > 0) {
            int prevIdx = path.lastIndexOf(java.io.File.separator, idx - 1);
            if (prevIdx >= 0) {
                return "..." + path.substring(prevIdx);
            }
        }
        return path;
    }
}

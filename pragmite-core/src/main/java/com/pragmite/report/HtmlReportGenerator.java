package com.pragmite.report;

import com.pragmite.model.AnalysisResult;
import com.pragmite.model.CodeSmell;
import com.pragmite.model.QualityScore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Generates HTML reports from analysis results.
 * Uses template-based rendering with Chart.js for visualizations.
 */
public class HtmlReportGenerator {

    private static final Logger logger = LoggerFactory.getLogger(HtmlReportGenerator.class);
    private static final String TEMPLATE_PATH = "/templates/report-template.html";

    /**
     * Generates an HTML report and saves it to the specified path.
     */
    public void generate(AnalysisResult result, Path outputPath) throws IOException {
        logger.info("Generating HTML report: {}", outputPath);

        // Load template
        String template = loadTemplate();

        // Replace placeholders
        String html = replacePlaceholders(template, result);

        // Write to file
        Files.writeString(outputPath, html, StandardCharsets.UTF_8);

        logger.info("HTML report generated successfully: {}", outputPath);
    }

    /**
     * Loads the HTML template from resources.
     */
    private String loadTemplate() throws IOException {
        try (InputStream is = getClass().getResourceAsStream(TEMPLATE_PATH)) {
            if (is == null) {
                throw new IOException("Template not found: " + TEMPLATE_PATH);
            }
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    /**
     * Replaces placeholders in the template with actual data.
     */
    private String replacePlaceholders(String template, AnalysisResult result) {
        Map<String, String> placeholders = buildPlaceholders(result);

        String html = template;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            html = html.replace("{{" + entry.getKey() + "}}", entry.getValue());
        }

        return html;
    }

    /**
     * Builds all placeholder values from analysis result.
     */
    private Map<String, String> buildPlaceholders(AnalysisResult result) {
        Map<String, String> placeholders = new HashMap<>();

        // Basic info
        placeholders.put("PROJECT_NAME", result.getProjectName() != null ? result.getProjectName() : "Unknown Project");
        placeholders.put("TIMESTAMP", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

        // Quality score
        QualityScore qualityScore = result.getQualityScore();
        if (qualityScore != null) {
            int overallScore = (int) qualityScore.getOverallScore();
            placeholders.put("QUALITY_SCORE", String.valueOf(overallScore));
            placeholders.put("QUALITY_SCORE_CLASS", getScoreClass(overallScore));
            placeholders.put("QUALITY_RATING", getScoreRating(overallScore));

            placeholders.put("DRY_SCORE", String.valueOf((int) qualityScore.getDryScore()));
            placeholders.put("ORTHOGONALITY_SCORE", String.valueOf((int) qualityScore.getOrthogonalityScore()));
            placeholders.put("CORRECTNESS_SCORE", String.valueOf((int) qualityScore.getCorrectnessScore()));
            placeholders.put("PERFORMANCE_SCORE", String.valueOf((int) qualityScore.getPerfScore()));
        } else {
            placeholders.put("QUALITY_SCORE", "N/A");
            placeholders.put("QUALITY_SCORE_CLASS", "");
            placeholders.put("QUALITY_RATING", "Not calculated");
            placeholders.put("DRY_SCORE", "0");
            placeholders.put("ORTHOGONALITY_SCORE", "0");
            placeholders.put("CORRECTNESS_SCORE", "0");
            placeholders.put("PERFORMANCE_SCORE", "0");
        }

        // Issue counts
        List<CodeSmell> smells = result.getCodeSmells();
        placeholders.put("TOTAL_ISSUES", String.valueOf(smells.size()));

        Map<String, Long> severityCounts = countBySeverity(smells);
        placeholders.put("CRITICAL_COUNT", String.valueOf(severityCounts.getOrDefault("CRITICAL", 0L)));
        placeholders.put("HIGH_COUNT", String.valueOf(severityCounts.getOrDefault("HIGH", 0L)));
        placeholders.put("MEDIUM_COUNT", String.valueOf(severityCounts.getOrDefault("MEDIUM", 0L)));
        placeholders.put("LOW_COUNT", String.valueOf(severityCounts.getOrDefault("LOW", 0L)));
        placeholders.put("INFO_COUNT", String.valueOf(severityCounts.getOrDefault("INFO", 0L)));

        // File statistics
        int filesAnalyzed = result.getFilesAnalyzed() != null ? result.getFilesAnalyzed() : 0;
        int linesOfCode = result.getTotalLines();
        Long analysisTimeMs = result.getAnalysisTimeMs();
        long analysisTime = analysisTimeMs != null ? analysisTimeMs / 1000 : 0;

        placeholders.put("FILES_ANALYZED", String.valueOf(filesAnalyzed));
        placeholders.put("LINES_OF_CODE", String.format("%,d", linesOfCode));
        placeholders.put("ANALYSIS_TIME", String.valueOf(analysisTime));
        placeholders.put("FILES_PER_SECOND", analysisTime > 0 ? String.format("%.1f", (double) filesAnalyzed / analysisTime) : "N/A");

        // Code metrics
        placeholders.put("AVG_COMPLEXITY", result.getAverageComplexity() != null ?
            String.format("%.1f", result.getAverageComplexity()) : "N/A");
        placeholders.put("MAX_COMPLEXITY", result.getMaxComplexity() != null ?
            String.valueOf(result.getMaxComplexity()) : "N/A");
        placeholders.put("AVG_METHOD_LENGTH", result.getAverageMethodLength() != null ?
            String.format("%.1f", result.getAverageMethodLength()) : "N/A");
        placeholders.put("DUPLICATION_PERCENTAGE", result.getDuplicationPercentage() != null ?
            String.format("%.1f", result.getDuplicationPercentage()) : "0.0");

        // Issues table
        placeholders.put("ISSUES_TABLE", buildIssuesTable(smells));

        return placeholders;
    }

    /**
     * Counts code smells by severity.
     */
    private Map<String, Long> countBySeverity(List<CodeSmell> smells) {
        return smells.stream()
            .collect(Collectors.groupingBy(
                smell -> smell.getSeverity() != null ? smell.getSeverity().toString() : "INFO",
                Collectors.counting()
            ));
    }

    /**
     * Returns CSS class for quality score.
     */
    private String getScoreClass(int score) {
        if (score >= 90) return "score-excellent";
        if (score >= 70) return "score-good";
        if (score >= 50) return "score-warning";
        return "score-poor";
    }

    /**
     * Returns rating text for quality score.
     */
    private String getScoreRating(int score) {
        if (score >= 90) return "Excellent";
        if (score >= 70) return "Good";
        if (score >= 50) return "Fair";
        if (score >= 30) return "Poor";
        return "Critical";
    }

    /**
     * Builds HTML table rows for top issues (max 50).
     */
    private String buildIssuesTable(List<CodeSmell> smells) {
        StringBuilder html = new StringBuilder();

        // Sort by severity and take top 50
        List<CodeSmell> topIssues = smells.stream()
            .sorted((a, b) -> compareSeverity(a, b))
            .limit(50)
            .collect(Collectors.toList());

        for (CodeSmell smell : topIssues) {
            String severity = smell.getSeverity() != null ? smell.getSeverity().toString() : "INFO";
            String severityClass = "severity-" + severity.toLowerCase();
            String badgeClass = "badge-" + severity.toLowerCase();

            String fileName = extractFileName(smell.getFilePath());
            String type = smell.getType() != null ? smell.getType().toString().replace("_", " ") : "Unknown";
            String description = escapeHtml(smell.getMessage());

            html.append("<tr>");
            html.append("<td><span class=\"badge ").append(badgeClass).append("\">").append(severity).append("</span></td>");
            html.append("<td>").append(type).append("</td>");
            html.append("<td>").append(fileName).append("</td>");
            html.append("<td>").append(smell.getLine()).append("</td>");
            html.append("<td>").append(description).append("</td>");
            html.append("</tr>");
        }

        if (topIssues.isEmpty()) {
            html.append("<tr><td colspan=\"5\" style=\"text-align:center; color:#6b7280;\">No issues found! 🎉</td></tr>");
        }

        return html.toString();
    }

    /**
     * Compares severity for sorting (Critical > High > Medium > Low > Info).
     */
    private int compareSeverity(CodeSmell a, CodeSmell b) {
        return getSeverityOrder(b) - getSeverityOrder(a); // Descending
    }

    private int getSeverityOrder(CodeSmell smell) {
        String severity = smell.getSeverity() != null ? smell.getSeverity().toString() : "INFO";
        switch (severity) {
            case "CRITICAL": return 5;
            case "HIGH": return 4;
            case "MEDIUM": return 3;
            case "LOW": return 2;
            default: return 1;
        }
    }

    /**
     * Extracts file name from full path.
     */
    private String extractFileName(String filePath) {
        if (filePath == null) return "Unknown";
        int lastSlash = Math.max(filePath.lastIndexOf('/'), filePath.lastIndexOf('\\'));
        return lastSlash >= 0 ? filePath.substring(lastSlash + 1) : filePath;
    }

    /**
     * Escapes HTML special characters.
     */
    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&#39;");
    }
}

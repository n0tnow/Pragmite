package com.pragmite.report;

import com.pragmite.ai.AIAnalysisResult;
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
        generate(result, null, outputPath);
    }

    /**
     * Generates an HTML report with AI analysis and saves it to the specified path.
     */
    public void generate(AnalysisResult result, List<AIAnalysisResult> aiResults, Path outputPath) throws IOException {
        logger.info("Generating HTML report: {}", outputPath);

        // Load template
        String template = loadTemplate();

        // Replace placeholders
        String html = replacePlaceholders(template, result, aiResults);

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
    private String replacePlaceholders(String template, AnalysisResult result, List<AIAnalysisResult> aiResults) {
        Map<String, String> placeholders = buildPlaceholders(result);

        String html = template;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            html = html.replace("{{" + entry.getKey() + "}}", entry.getValue());
        }

        // Add AI analysis section if available
        if (aiResults != null && !aiResults.isEmpty()) {
            String aiSection = buildAiAnalysisSection(aiResults);
            html = html.replace("{{AI_ANALYSIS_SECTION}}", aiSection);
        } else {
            html = html.replace("{{AI_ANALYSIS_SECTION}}", "");
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

    /**
     * Builds the AI analysis section HTML.
     */
    private String buildAiAnalysisSection(List<AIAnalysisResult> aiResults) {
        StringBuilder html = new StringBuilder();

        html.append("<div class=\"section\" id=\"ai-analysis\">\n");
        html.append("  <h2>🤖 AI-Powered Analysis & Refactoring Suggestions</h2>\n");
        html.append("  <p style=\"color: #6b7280; margin-bottom: 30px;\">")
            .append("AI-generated root cause analysis, impact assessment, and ready-to-use prompts for automated refactoring.")
            .append("</p>\n");

        for (AIAnalysisResult result : aiResults) {
            html.append(buildAiAnalysisCard(result));
        }

        html.append("</div>\n");

        return html.toString();
    }

    /**
     * Builds a single AI analysis card.
     */
    private String buildAiAnalysisCard(AIAnalysisResult result) {
        StringBuilder html = new StringBuilder();

        String severity = result.getMetadata().getOrDefault("severity", "INFO");
        String category = result.getMetadata().getOrDefault("category", "Code Quality");
        String badgeClass = "badge-" + severity.toLowerCase();

        html.append("<div class=\"ai-card\" style=\"")
            .append("background: white; ")
            .append("border-radius: 8px; ")
            .append("padding: 24px; ")
            .append("margin-bottom: 24px; ")
            .append("box-shadow: 0 2px 4px rgba(0,0,0,0.1); ")
            .append("border-left: 4px solid #667eea;")
            .append("\">\n");

        // Header with location and severity
        html.append("  <div style=\"display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px;\">\n");
        html.append("    <div>\n");
        html.append("      <h3 style=\"margin: 0; font-size: 1.1em; color: #1f2937;\">")
            .append("📍 ").append(escapeHtml(extractFileName(result.getFilePath())))
            .append(":").append(result.getLineNumber())
            .append("</h3>\n");
        html.append("      <p style=\"margin: 4px 0 0 0; color: #6b7280; font-size: 0.9em;\">")
            .append(escapeHtml(result.getSmellType().toString().replace("_", " ")))
            .append("</p>\n");
        html.append("    </div>\n");
        html.append("    <div>\n");
        html.append("      <span class=\"badge ").append(badgeClass).append("\">")
            .append(severity).append("</span>\n");
        html.append("      <span class=\"badge\" style=\"background: #e0e7ff; color: #4338ca; margin-left: 8px;\">")
            .append(category).append("</span>\n");
        html.append("    </div>\n");
        html.append("  </div>\n");

        // Root Cause
        html.append("  <div style=\"margin-bottom: 16px;\">\n");
        html.append("    <h4 style=\"color: #667eea; margin-bottom: 8px; font-size: 0.95em;\">🎯 Root Cause</h4>\n");
        html.append("    <p style=\"color: #374151; line-height: 1.6; margin: 0;\">")
            .append(escapeHtml(result.getRootCause()))
            .append("</p>\n");
        html.append("  </div>\n");

        // Impact
        html.append("  <div style=\"margin-bottom: 16px;\">\n");
        html.append("    <h4 style=\"color: #f59e0b; margin-bottom: 8px; font-size: 0.95em;\">⚠️ Impact</h4>\n");
        html.append("    <p style=\"color: #374151; line-height: 1.6; margin: 0;\">")
            .append(escapeHtml(result.getImpact()))
            .append("</p>\n");
        html.append("  </div>\n");

        // Recommendation
        html.append("  <div style=\"margin-bottom: 16px;\">\n");
        html.append("    <h4 style=\"color: #10b981; margin-bottom: 8px; font-size: 0.95em;\">✅ Recommendation</h4>\n");
        html.append("    <p style=\"color: #374151; line-height: 1.6; margin: 0;\">")
            .append(escapeHtml(result.getRecommendation()))
            .append("</p>\n");
        html.append("  </div>\n");

        // Code Snippets
        if (!result.getCodeSnippets().isEmpty()) {
            html.append("  <div style=\"margin-bottom: 16px;\">\n");
            html.append("    <h4 style=\"color: #6b7280; margin-bottom: 8px; font-size: 0.95em;\">📝 Code Context</h4>\n");
            for (int i = 0; i < result.getCodeSnippets().size(); i++) {
                String snippet = result.getCodeSnippets().get(i);
                html.append("    <pre style=\"")
                    .append("background: #f9fafb; ")
                    .append("padding: 16px; ")
                    .append("border-radius: 6px; ")
                    .append("overflow-x: auto; ")
                    .append("font-size: 0.85em; ")
                    .append("line-height: 1.5; ")
                    .append("margin: 8px 0;")
                    .append("\"><code>").append(escapeHtml(snippet)).append("</code></pre>\n");
            }
            html.append("  </div>\n");
        }

        // Refactored Code Section (if available)
        if (result.hasRefactoredCode()) {
            com.pragmite.ai.RefactoredCode refactored = result.getRefactoredCode();
            html.append("  <div style=\"margin-top: 20px; padding-top: 20px; border-top: 2px dashed #e5e7eb;\">\n");
            html.append("    <h4 style=\"color: #10b981; margin-bottom: 12px; font-size: 0.95em;\">✨ AI-Generated Refactored Code</h4>\n");

            // Explanation
            if (refactored.getExplanation() != null && !refactored.getExplanation().isEmpty()) {
                html.append("    <p style=\"color: #374151; margin-bottom: 12px;\">")
                    .append(escapeHtml(refactored.getExplanation()))
                    .append("</p>\n");
            }

            // Before/After Code
            html.append("    <div style=\"display: grid; grid-template-columns: 1fr 1fr; gap: 16px; margin: 16px 0;\">\n");

            // Before (Original)
            html.append("      <div>\n");
            html.append("        <h5 style=\"color: #6b7280; margin-bottom: 8px; font-size: 0.85em;\">📄 Before</h5>\n");
            html.append("        <pre style=\"")
                .append("background: #fee2e2; ")
                .append("padding: 12px; ")
                .append("border-radius: 6px; ")
                .append("overflow-x: auto; ")
                .append("font-size: 0.8em; ")
                .append("line-height: 1.4; ")
                .append("margin: 0;")
                .append("\"><code>").append(escapeHtml(refactored.getOriginalCode())).append("</code></pre>\n");
            html.append("      </div>\n");

            // After (Refactored)
            html.append("      <div>\n");
            html.append("        <h5 style=\"color: #6b7280; margin-bottom: 8px; font-size: 0.85em;\">✅ After</h5>\n");
            html.append("        <pre style=\"")
                .append("background: #d1fae5; ")
                .append("padding: 12px; ")
                .append("border-radius: 6px; ")
                .append("overflow-x: auto; ")
                .append("font-size: 0.8em; ")
                .append("line-height: 1.4; ")
                .append("margin: 0;")
                .append("\"><code>").append(escapeHtml(refactored.getRefactoredCode())).append("</code></pre>\n");
            html.append("      </div>\n");
            html.append("    </div>\n");

            // Why Better
            if (refactored.getWhyBetter() != null && !refactored.getWhyBetter().isEmpty()) {
                html.append("    <div style=\"background: #f0fdf4; padding: 12px; border-radius: 6px; margin: 12px 0; border-left: 4px solid #10b981;\">\n");
                html.append("      <h5 style=\"color: #065f46; margin: 0 0 8px 0; font-size: 0.85em;\">💡 Why This is Better</h5>\n");
                html.append("      <p style=\"color: #064e3b; margin: 0; font-size: 0.85em;\">")
                    .append(escapeHtml(refactored.getWhyBetter()))
                    .append("</p>\n");
                html.append("    </div>\n");
            }

            // Changes Made
            if (!refactored.getChanges().isEmpty()) {
                html.append("    <div style=\"margin-top: 12px;\">\n");
                html.append("      <h5 style=\"color: #6b7280; margin-bottom: 8px; font-size: 0.85em;\">📝 Changes Made</h5>\n");
                html.append("      <ul style=\"margin: 0; padding-left: 20px; color: #374151; font-size: 0.85em;\">\n");
                for (String change : refactored.getChanges()) {
                    html.append("        <li>").append(escapeHtml(change)).append("</li>\n");
                }
                html.append("      </ul>\n");
                html.append("    </div>\n");
            }

            html.append("  </div>\n");
        }

        // AI Prompt with Copy Button
        html.append("  <div style=\"margin-top: 20px; padding-top: 20px; border-top: 2px dashed #e5e7eb;\">\n");
        html.append("    <div style=\"display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px;\">\n");
        html.append("      <h4 style=\"color: #667eea; margin: 0; font-size: 0.95em;\">🤖 AI Prompt (Copy & Paste to Claude/GPT-4/Gemini)</h4>\n");
        html.append("      <button onclick=\"copyPrompt").append(result.hashCode()).append("()\" ")
            .append("style=\"")
            .append("background: #667eea; ")
            .append("color: white; ")
            .append("border: none; ")
            .append("padding: 8px 16px; ")
            .append("border-radius: 6px; ")
            .append("cursor: pointer; ")
            .append("font-size: 0.85em; ")
            .append("font-weight: 600;")
            .append("\">📋 Copy Prompt</button>\n");
        html.append("    </div>\n");
        html.append("    <pre id=\"prompt").append(result.hashCode()).append("\" style=\"")
            .append("background: #1f2937; ")
            .append("color: #f9fafb; ")
            .append("padding: 16px; ")
            .append("border-radius: 6px; ")
            .append("overflow-x: auto; ")
            .append("font-size: 0.85em; ")
            .append("line-height: 1.6; ")
            .append("margin: 0; ")
            .append("white-space: pre-wrap;")
            .append("\">").append(escapeHtml(result.getAiPrompt())).append("</pre>\n");
        html.append("  </div>\n");

        html.append("</div>\n");

        // Add JavaScript for copy functionality
        html.append("<script>\n");
        html.append("function copyPrompt").append(result.hashCode()).append("() {\n");
        html.append("  const prompt = document.getElementById('prompt").append(result.hashCode()).append("').textContent;\n");
        html.append("  navigator.clipboard.writeText(prompt).then(() => {\n");
        html.append("    alert('✅ AI prompt copied to clipboard!');\n");
        html.append("  }).catch(err => {\n");
        html.append("    console.error('Failed to copy:', err);\n");
        html.append("  });\n");
        html.append("}\n");
        html.append("</script>\n");

        return html.toString();
    }
}

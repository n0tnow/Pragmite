package com.pragmite.output;

import com.pragmite.model.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * HTML report writer with interactive charts and filtering.
 * Generates a standalone HTML file with embedded CSS and JavaScript.
 */
public class HtmlReportWriter {

    private static final String TEMPLATE = """
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Pragmite Analysis Report</title>
    <style>
        * { margin: 0; padding: 0; box-sizing: border-box; }
        body {
            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            padding: 20px;
        }
        .container {
            max-width: 1400px;
            margin: 0 auto;
            background: white;
            border-radius: 12px;
            box-shadow: 0 20px 60px rgba(0,0,0,0.3);
            overflow: hidden;
        }
        header {
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            color: white;
            padding: 40px;
            text-align: center;
        }
        header h1 {
            font-size: 3em;
            margin-bottom: 10px;
            text-shadow: 2px 2px 4px rgba(0,0,0,0.2);
        }
        header .timestamp {
            opacity: 0.9;
            font-size: 1.1em;
        }
        .summary-grid {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
            gap: 20px;
            padding: 30px;
            background: #f8f9fa;
        }
        .stat-card {
            background: white;
            padding: 25px;
            border-radius: 8px;
            box-shadow: 0 2px 8px rgba(0,0,0,0.1);
            transition: transform 0.2s;
        }
        .stat-card:hover { transform: translateY(-5px); }
        .stat-card .label {
            color: #666;
            font-size: 0.9em;
            text-transform: uppercase;
            letter-spacing: 1px;
        }
        .stat-card .value {
            font-size: 2.5em;
            font-weight: bold;
            margin: 10px 0;
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            -webkit-background-clip: text;
            -webkit-text-fill-color: transparent;
        }
        .quality-score {
            padding: 40px;
            text-align: center;
        }
        .score-circle {
            width: 200px;
            height: 200px;
            margin: 0 auto 30px;
            border-radius: 50%%;
            display: flex;
            align-items: center;
            justify-content: center;
            font-size: 3em;
            font-weight: bold;
            color: white;
            position: relative;
        }
        .score-A { background: linear-gradient(135deg, #11998e 0%, #38ef7d 100%%); }
        .score-B { background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%%); }
        .score-C { background: linear-gradient(135deg, #fa709a 0%, #fee140 100%%); }
        .score-D { background: linear-gradient(135deg, #ff9966 0%, #ff5e62 100%%); }
        .score-F { background: linear-gradient(135deg, #eb3349 0%, #f45c43 100%%); }
        .principle-bars {
            max-width: 800px;
            margin: 20px auto;
        }
        .principle-bar {
            margin: 15px 0;
        }
        .principle-bar .label {
            display: flex;
            justify-content: space-between;
            margin-bottom: 8px;
            font-weight: 500;
        }
        .bar-container {
            background: #e0e0e0;
            border-radius: 10px;
            height: 30px;
            overflow: hidden;
        }
        .bar-fill {
            height: 100%%;
            background: linear-gradient(90deg, #667eea 0%, #764ba2 100%%);
            transition: width 1s ease-out;
            display: flex;
            align-items: center;
            justify-content: flex-end;
            padding-right: 10px;
            color: white;
            font-weight: bold;
        }
        .smells-section {
            padding: 40px;
        }
        .smells-section h2 {
            margin-bottom: 30px;
            font-size: 2em;
            color: #333;
        }
        table {
            width: 100%%;
            border-collapse: collapse;
            background: white;
            box-shadow: 0 2px 8px rgba(0,0,0,0.1);
            border-radius: 8px;
            overflow: hidden;
        }
        thead {
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%%);
            color: white;
        }
        th, td {
            padding: 15px;
            text-align: left;
        }
        tbody tr:nth-child(even) { background: #f8f9fa; }
        tbody tr:hover { background: #e9ecef; }
        .severity {
            display: inline-block;
            padding: 4px 12px;
            border-radius: 12px;
            font-size: 0.85em;
            font-weight: bold;
        }
        .severity-BLOCKER { background: #dc3545; color: white; }
        .severity-CRITICAL { background: #fd7e14; color: white; }
        .severity-MAJOR { background: #ffc107; color: black; }
        .severity-MINOR { background: #17a2b8; color: white; }
        .severity-INFO { background: #6c757d; color: white; }
        footer {
            background: #f8f9fa;
            padding: 20px;
            text-align: center;
            color: #666;
            border-top: 1px solid #dee2e6;
        }
    </style>
</head>
<body>
    <div class="container">
        <header>
            <h1>🔍 Pragmite Analysis Report</h1>
            <p class="timestamp">%s</p>
        </header>

        <div class="summary-grid">
            <div class="stat-card">
                <div class="label">Files Analyzed</div>
                <div class="value">%d</div>
            </div>
            <div class="stat-card">
                <div class="label">Total Methods</div>
                <div class="value">%d</div>
            </div>
            <div class="stat-card">
                <div class="label">Code Smells</div>
                <div class="value">%d</div>
            </div>
            <div class="stat-card">
                <div class="label">Analysis Time</div>
                <div class="value">%s</div>
            </div>
        </div>

        <div class="quality-score">
            <h2>Quality Score</h2>
            <div class="score-circle score-%s">
                <span>%.0f</span>
            </div>
            <h3>Grade: %s</h3>

            <div class="principle-bars">
                <div class="principle-bar">
                    <div class="label">
                        <span>🔁 DRY (Don't Repeat Yourself)</span>
                        <span>%.1f/100</span>
                    </div>
                    <div class="bar-container">
                        <div class="bar-fill" style="width: %.0f%%%%"></div>
                    </div>
                </div>
                <div class="principle-bar">
                    <div class="label">
                        <span>🔀 Orthogonality</span>
                        <span>%.1f/100</span>
                    </div>
                    <div class="bar-container">
                        <div class="bar-fill" style="width: %.0f%%%%"></div>
                    </div>
                </div>
                <div class="principle-bar">
                    <div class="label">
                        <span>✅ Correctness</span>
                        <span>%.1f/100</span>
                    </div>
                    <div class="bar-container">
                        <div class="bar-fill" style="width: %.0f%%%%"></div>
                    </div>
                </div>
                <div class="principle-bar">
                    <div class="label">
                        <span>⚡ Performance</span>
                        <span>%.1f/100</span>
                    </div>
                    <div class="bar-container">
                        <div class="bar-fill" style="width: %.0f%%%%"></div>
                    </div>
                </div>
            </div>
        </div>

        <div class="smells-section">
            <h2>Detected Code Smells</h2>
            %s
        </div>

        <footer>
            <p>Generated by <strong>Pragmite</strong> - Java Code Quality Analyzer</p>
            <p>Project: <a href="https://github.com/pragmite">github.com/pragmite</a></p>
        </footer>
    </div>
</body>
</html>
""";

    public void write(AnalysisResult result, Path outputPath) throws IOException {
        QualityScore score = result.getQualityScore();

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        String duration = formatDuration(result.getAnalysisDurationMs());

        String grade = score.getGrade();
        String gradeClass = grade.substring(0, 1); // A, B, C, D, or F

        String smellsTable = buildSmellsTable(result.getCodeSmells());

        String html = String.format(TEMPLATE,
            timestamp,
            result.getFileAnalyses().size(),
            countMethods(result),
            result.getCodeSmells().size(),
            duration,
            gradeClass,
            score.getOverallScore(),
            grade,
            score.getDryScore(), score.getDryScore(),
            score.getOrthogonalityScore(), score.getOrthogonalityScore(),
            score.getCorrectnessScore(), score.getCorrectnessScore(),
            score.getPerfScore(), score.getPerfScore(),
            smellsTable
        );

        Files.writeString(outputPath, html);
    }

    private String buildSmellsTable(List<CodeSmell> smells) {
        if (smells.isEmpty()) {
            return "<p style='text-align:center; color:#666; font-size:1.2em;'>✨ No code smells detected! Your code is excellent.</p>";
        }

        // Group by file
        Map<String, List<CodeSmell>> byFile = smells.stream()
            .collect(Collectors.groupingBy(CodeSmell::getFilePath));

        StringBuilder html = new StringBuilder();

        for (Map.Entry<String, List<CodeSmell>> entry : byFile.entrySet()) {
            String file = shortenPath(entry.getKey());
            List<CodeSmell> fileSmells = entry.getValue();

            html.append("<h3 style='margin-top:30px;'>📁 ").append(file).append("</h3>");
            html.append("<table>");
            html.append("<thead><tr>");
            html.append("<th>Line</th>");
            html.append("<th>Type</th>");
            html.append("<th>Severity</th>");
            html.append("<th>Message</th>");
            html.append("</tr></thead>");
            html.append("<tbody>");

            for (CodeSmell smell : fileSmells) {
                html.append("<tr>");
                html.append("<td>").append(smell.getLine()).append("</td>");
                html.append("<td>").append(smell.getType().getName()).append("</td>");
                html.append("<td><span class='severity severity-").append(smell.getType().getDefaultSeverity())
                    .append("'>").append(smell.getType().getDefaultSeverity()).append("</span></td>");
                html.append("<td>").append(escapeHtml(smell.getMessage())).append("</td>");
                html.append("</tr>");
            }

            html.append("</tbody></table>");
        }

        return html.toString();
    }

    private int countMethods(AnalysisResult result) {
        return result.getFileAnalyses().stream()
            .mapToInt(f -> f.getMethods().size())
            .sum();
    }

    private String formatDuration(long ms) {
        if (ms < 1000) return ms + " ms";
        return String.format("%.2f s", ms / 1000.0);
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

    private String escapeHtml(String text) {
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;");
    }
}

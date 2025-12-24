package com.pragmite.analytics;

import com.pragmite.model.AnalysisResult;
import com.pragmite.model.CodeSmell;
import com.pragmite.model.CodeSmellType;
import com.pragmite.model.Severity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class QualityTrendAnalyzerTest {

    private QualityTrendAnalyzer analyzer;

    @BeforeEach
    void setUp() {
        analyzer = new QualityTrendAnalyzer();
    }

    @Test
    void testCalculateQualityScore_NoSmells() {
        AnalysisResult result = new AnalysisResult();
        result.setTotalLines(1000);

        double score = analyzer.calculateQualityScore(result);

        assertEquals(100.0, score, 0.01, "Perfect code should score 100");
    }

    @Test
    void testCalculateQualityScore_WithSmells() {
        AnalysisResult result = new AnalysisResult();
        result.setTotalLines(1000);

        // Add some code smells
        result.addCodeSmell(new CodeSmell(CodeSmellType.LONG_METHOD, "Test.java", 10, "Long method"));
        result.addCodeSmell(new CodeSmell(CodeSmellType.HIGH_CYCLOMATIC_COMPLEXITY, "Test.java", 20, "Complex"));

        double score = analyzer.calculateQualityScore(result);

        assertTrue(score < 100, "Code with smells should score less than 100");
        assertTrue(score > 0, "Score should be positive");
    }

    @Test
    void testCalculateQualityScore_CriticalSmellsLowerScore() {
        AnalysisResult result1 = new AnalysisResult();
        result1.setTotalLines(1000);
        CodeSmell minorSmell = new CodeSmell(CodeSmellType.UNUSED_IMPORT, "Test.java", 1, "Unused");
        minorSmell.setSeverity(Severity.MINOR);
        result1.addCodeSmell(minorSmell);

        AnalysisResult result2 = new AnalysisResult();
        result2.setTotalLines(1000);
        CodeSmell criticalSmell = new CodeSmell(CodeSmellType.GOD_CLASS, "Test.java", 1, "God class");
        criticalSmell.setSeverity(Severity.CRITICAL);
        result2.addCodeSmell(criticalSmell);

        double score1 = analyzer.calculateQualityScore(result1);
        double score2 = analyzer.calculateQualityScore(result2);

        assertTrue(score1 > score2, "Critical smells should lower score more than minor smells");
    }

    @Test
    void testAnalyzeTrend_Improving() {
        AnalysisResult baseline = createResultWithSmells(10, LocalDateTime.now().minusDays(7));
        AnalysisResult current = createResultWithSmells(5, LocalDateTime.now());

        QualityTrendAnalyzer.TrendReport report = analyzer.analyzeTrend(baseline, current);

        assertNotNull(report);
        assertEquals(QualityTrendAnalyzer.Trend.IMPROVING, report.getTrend());
        assertTrue(report.getFixedIssuesCount() > 0);
    }

    @Test
    void testAnalyzeTrend_Degrading() {
        AnalysisResult baseline = createResultWithSmells(5, LocalDateTime.now().minusDays(7));
        AnalysisResult current = createResultWithSmells(15, LocalDateTime.now());

        QualityTrendAnalyzer.TrendReport report = analyzer.analyzeTrend(baseline, current);

        assertEquals(QualityTrendAnalyzer.Trend.DEGRADING, report.getTrend());
        assertTrue(report.getCurrentScore() < report.getBaselineScore());
    }

    @Test
    void testAnalyzeTrend_Stable() {
        AnalysisResult baseline = createResultWithSmells(10, LocalDateTime.now().minusDays(7));
        AnalysisResult current = createResultWithSmells(10, LocalDateTime.now());

        QualityTrendAnalyzer.TrendReport report = analyzer.analyzeTrend(baseline, current);

        assertEquals(QualityTrendAnalyzer.Trend.STABLE, report.getTrend());
    }

    @Test
    void testIdentifyHotspots() {
        AnalysisResult result = new AnalysisResult();

        // Add multiple smells to different files
        for (int i = 0; i < 5; i++) {
            result.addCodeSmell(new CodeSmell(CodeSmellType.LONG_METHOD, "File1.java", i, "Smell " + i));
        }
        for (int i = 0; i < 3; i++) {
            result.addCodeSmell(new CodeSmell(CodeSmellType.MAGIC_NUMBER, "File2.java", i, "Smell " + i));
        }
        result.addCodeSmell(new CodeSmell(CodeSmellType.UNUSED_IMPORT, "File3.java", 1, "Smell"));

        List<QualityTrendAnalyzer.Hotspot> hotspots = analyzer.identifyHotspots(result, 3);

        assertNotNull(hotspots);
        assertTrue(hotspots.size() <= 3);
        assertFalse(hotspots.isEmpty());

        // First hotspot should be File1.java (5 issues)
        QualityTrendAnalyzer.Hotspot topHotspot = hotspots.get(0);
        assertTrue(topHotspot.getFilePath().contains("File1.java"));
        assertEquals(5, topHotspot.getIssueCount());
    }

    @Test
    void testTrendReportToString() {
        AnalysisResult baseline = createResultWithSmells(10, LocalDateTime.now().minusDays(7));
        AnalysisResult current = createResultWithSmells(5, LocalDateTime.now());

        QualityTrendAnalyzer.TrendReport report = analyzer.analyzeTrend(baseline, current);
        String reportStr = report.toString();

        assertNotNull(reportStr);
        assertTrue(reportStr.contains("Quality Trend"));
        assertTrue(reportStr.contains("New:"));
        assertTrue(reportStr.contains("Fixed:"));
    }

    @Test
    void testHotspotToString() {
        List<QualityTrendAnalyzer.Hotspot> hotspots = analyzer.identifyHotspots(
            createResultWithSmells(5, LocalDateTime.now()), 1);

        if (!hotspots.isEmpty()) {
            String hotspotStr = hotspots.get(0).toString();
            assertNotNull(hotspotStr);
            assertTrue(hotspotStr.contains("issues"));
        }
    }

    private AnalysisResult createResultWithSmells(int smellCount, LocalDateTime analyzedAt) {
        AnalysisResult result = new AnalysisResult();
        result.setTotalLines(1000);
        result.setAnalyzedAt(analyzedAt);

        for (int i = 0; i < smellCount; i++) {
            result.addCodeSmell(new CodeSmell(
                CodeSmellType.LONG_METHOD,
                "Test.java",
                i * 10,
                "Smell " + i
            ));
        }

        return result;
    }
}

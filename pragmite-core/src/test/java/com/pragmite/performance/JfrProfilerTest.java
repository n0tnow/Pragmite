package com.pragmite.performance;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class JfrProfilerTest {

    private JfrProfiler profiler;

    @BeforeEach
    void setUp() {
        profiler = new JfrProfiler();
    }

    @AfterEach
    void tearDown() {
        if (profiler.isEnabled()) {
            Path recordingPath = profiler.stopRecording();
            if (recordingPath != null && Files.exists(recordingPath)) {
                try {
                    Files.delete(recordingPath);
                } catch (Exception e) {
                    // Ignore cleanup errors
                }
            }
        }
    }

    @Test
    void testStartAndStopRecording() {
        profiler.startRecording();
        assertTrue(profiler.isEnabled());

        Path recordingPath = profiler.stopRecording();
        assertNotNull(recordingPath);
        assertFalse(profiler.isEnabled());
    }

    @Test
    void testStartRecordingWithCustomSettings() {
        profiler.startRecording("test-recording", Duration.ofMinutes(5));
        assertTrue(profiler.isEnabled());

        profiler.stopRecording();
        assertFalse(profiler.isEnabled());
    }

    @Test
    void testRecordFileAnalysis() {
        profiler.startRecording();

        profiler.recordFileAnalysis(
            "TestFile.java",
            150L,
            1024L,
            50,
            10,
            "success"
        );

        // Verify stats were updated
        var stats = profiler.getOperationStats();
        assertTrue(stats.containsKey("file_analysis"));
        assertEquals(1, stats.get("file_analysis").count);

        profiler.stopRecording();
    }

    @Test
    void testRecordCodeSmell() {
        profiler.startRecording();

        profiler.recordCodeSmell(
            "TestFile.java",
            "GodClass",
            "HIGH",
            50L,
            3
        );

        // Verify stats were updated
        var stats = profiler.getOperationStats();
        assertTrue(stats.containsKey("smell_detection_GodClass"));

        profiler.stopRecording();
    }

    @Test
    void testRecordMetricsCalculation() {
        profiler.startRecording();

        profiler.recordMetricsCalculation(
            "TestFile.java",
            "Halstead",
            75L,
            42.5
        );

        // Verify stats were updated
        var stats = profiler.getOperationStats();
        assertTrue(stats.containsKey("metrics_Halstead"));

        profiler.stopRecording();
    }

    @Test
    void testMultipleEvents() {
        profiler.startRecording();

        // Record multiple events
        for (int i = 0; i < 10; i++) {
            profiler.recordFileAnalysis(
                "File" + i + ".java",
                100L + i,
                1024L,
                50,
                5,
                "success"
            );
        }

        var stats = profiler.getOperationStats();
        assertTrue(stats.containsKey("file_analysis"));
        assertEquals(10, stats.get("file_analysis").count);

        profiler.stopRecording();
    }

    @Test
    void testOperationStats() {
        profiler.startRecording();

        // Record events with different durations
        profiler.recordFileAnalysis("File1.java", 100L, 1024L, 50, 5, "success");
        profiler.recordFileAnalysis("File2.java", 200L, 2048L, 100, 10, "success");
        profiler.recordFileAnalysis("File3.java", 150L, 1536L, 75, 7, "success");

        var stats = profiler.getOperationStats().get("file_analysis");
        assertNotNull(stats);
        assertEquals(3, stats.count);
        assertEquals(450L, stats.totalDuration);
        assertEquals(150.0, stats.getAverageDuration(), 0.01);
        assertEquals(100L, stats.minDuration);
        assertEquals(200L, stats.maxDuration);

        profiler.stopRecording();
    }

    @Test
    void testRecordingNotEnabled() {
        // Don't start recording
        assertFalse(profiler.isEnabled());

        // Events should be ignored
        profiler.recordFileAnalysis("Test.java", 100L, 1024L, 50, 5, "success");

        var stats = profiler.getOperationStats();
        assertTrue(stats.isEmpty());
    }

    @Test
    void testStopRecordingWithoutStart() {
        Path recordingPath = profiler.stopRecording();
        assertNull(recordingPath);
    }

    @Test
    void testAnalyzeRecording() throws Exception {
        profiler.startRecording();

        // Record some events
        profiler.recordFileAnalysis("File1.java", 100L, 1024L, 50, 5, "success");
        profiler.recordFileAnalysis("File2.java", 150L, 2048L, 75, 8, "success");
        profiler.recordCodeSmell("File1.java", "LongMethod", "MEDIUM", 50L, 2);
        profiler.recordMetricsCalculation("File1.java", "CK", 75L, 25.0);

        Path recordingPath = profiler.stopRecording();
        assertNotNull(recordingPath);
        assertTrue(Files.exists(recordingPath));

        // Analyze the recording
        JfrProfiler.JfrAnalysisReport report = profiler.analyzeRecording(recordingPath);
        assertNotNull(report);

        // Verify report contains expected data
        assertTrue(report.totalFileAnalyses >= 0);
        assertTrue(report.totalSmellsDetected >= 0);
        assertTrue(report.totalMetricsCalculated >= 0);

        // Clean up
        Files.delete(recordingPath);
    }

    @Test
    void testAnalyzeNonExistentRecording() {
        Path nonExistent = Path.of("non-existent.jfr");
        JfrProfiler.JfrAnalysisReport report = profiler.analyzeRecording(nonExistent);

        // Should return empty report without throwing
        assertNotNull(report);
        assertEquals(0, report.totalFileAnalyses);
        assertEquals(0, report.totalSmellsDetected);
        assertEquals(0, report.totalMetricsCalculated);
    }

    @Test
    void testRestartRecording() {
        // Start first recording
        profiler.startRecording();
        assertTrue(profiler.isEnabled());

        // Start second recording (should stop first one)
        profiler.startRecording();
        assertTrue(profiler.isEnabled());

        profiler.stopRecording();
        assertFalse(profiler.isEnabled());
    }

    @Test
    void testOperationStatsToString() {
        JfrProfiler.OperationStats stats = new JfrProfiler.OperationStats();
        stats.count = 5;
        stats.totalDuration = 500;
        stats.minDuration = 80;
        stats.maxDuration = 120;

        String statsString = stats.toString();
        assertNotNull(statsString);
        assertTrue(statsString.contains("count=5"));
        assertTrue(statsString.contains("avg=100"));
        assertTrue(statsString.contains("min=80"));
        assertTrue(statsString.contains("max=120"));
    }

    @Test
    void testJfrAnalysisReportToString() {
        JfrProfiler.JfrAnalysisReport report = new JfrProfiler.JfrAnalysisReport();
        report.totalFileAnalyses = 10;
        report.averageAnalysisDuration = 125.5;
        report.totalSmellsDetected = 25;
        report.totalMetricsCalculated = 30;

        String reportString = report.toString();
        assertNotNull(reportString);
        assertTrue(reportString.contains("10 files"));
        assertTrue(reportString.contains("25 smells"));
        assertTrue(reportString.contains("30 metrics"));
    }

    @Test
    void testCustomEventCreation() {
        // Test that custom events can be created
        assertDoesNotThrow(() -> {
            JfrProfiler.FileAnalysisEvent event = new JfrProfiler.FileAnalysisEvent();
            event.filePath = "Test.java";
            event.durationMs = 100;
            event.fileSize = 1024;
            event.linesOfCode = 50;
            event.complexity = 5;
            event.status = "success";
        });

        assertDoesNotThrow(() -> {
            JfrProfiler.CodeSmellEvent event = new JfrProfiler.CodeSmellEvent();
            event.filePath = "Test.java";
            event.smellType = "GodClass";
            event.severity = "HIGH";
            event.detectionTimeMs = 50;
            event.smellCount = 3;
        });

        assertDoesNotThrow(() -> {
            JfrProfiler.MetricsCalculationEvent event = new JfrProfiler.MetricsCalculationEvent();
            event.filePath = "Test.java";
            event.metricType = "Halstead";
            event.durationMs = 75;
            event.metricValue = 42.5;
        });
    }
}

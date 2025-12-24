package com.pragmite.performance;

import jdk.jfr.Event;
import jdk.jfr.Label;
import jdk.jfr.Description;
import jdk.jfr.Category;
import jdk.jfr.StackTrace;
import jdk.jfr.Recording;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordingFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * JFR (Java Flight Recorder) integration for performance profiling.
 * Records custom events for analysis operations.
 */
public class JfrProfiler {
    private static final Logger logger = LoggerFactory.getLogger(JfrProfiler.class);

    private Recording recording;
    private final Map<String, OperationStats> operationStats = new ConcurrentHashMap<>();
    private boolean enabled = false;

    /**
     * Custom JFR event for file analysis operations.
     */
    @Label("File Analysis")
    @Description("Analysis of a single file")
    @Category({"Pragmite", "Analysis"})
    @StackTrace(false)
    public static class FileAnalysisEvent extends Event {
        @Label("File Path")
        public String filePath;

        @Label("Analysis Duration")
        public long durationMs;

        @Label("File Size")
        public long fileSize;

        @Label("Lines of Code")
        public int linesOfCode;

        @Label("Complexity")
        public int complexity;

        @Label("Status")
        public String status;
    }

    /**
     * Custom JFR event for code smell detection.
     */
    @Label("Code Smell Detection")
    @Description("Detection of code smells in a file")
    @Category({"Pragmite", "Quality"})
    @StackTrace(false)
    public static class CodeSmellEvent extends Event {
        @Label("File Path")
        public String filePath;

        @Label("Smell Type")
        public String smellType;

        @Label("Severity")
        public String severity;

        @Label("Detection Time")
        public long detectionTimeMs;

        @Label("Smell Count")
        public int smellCount;
    }

    /**
     * Custom JFR event for metrics calculation.
     */
    @Label("Metrics Calculation")
    @Description("Calculation of code metrics")
    @Category({"Pragmite", "Metrics"})
    @StackTrace(false)
    public static class MetricsCalculationEvent extends Event {
        @Label("File Path")
        public String filePath;

        @Label("Metric Type")
        public String metricType;

        @Label("Calculation Duration")
        public long durationMs;

        @Label("Metric Value")
        public double metricValue;
    }

    /**
     * Starts JFR recording with default settings.
     */
    public void startRecording() {
        startRecording("pragmite-recording", Duration.ofMinutes(10));
    }

    /**
     * Starts JFR recording with custom settings.
     */
    public void startRecording(String name, Duration maxAge) {
        try {
            if (recording != null && recording.getState() == jdk.jfr.RecordingState.RUNNING) {
                logger.warn("Recording already running, stopping previous recording");
                stopRecording();
            }

            recording = new Recording();
            recording.setName(name);
            recording.setMaxAge(maxAge);
            recording.setDumpOnExit(true);

            // Enable custom events
            recording.enable(FileAnalysisEvent.class)
                     .withThreshold(Duration.ofMillis(10));
            recording.enable(CodeSmellEvent.class)
                     .withThreshold(Duration.ofMillis(5));
            recording.enable(MetricsCalculationEvent.class)
                     .withThreshold(Duration.ofMillis(5));

            recording.start();
            enabled = true;

            logger.info("JFR recording started: {}", name);
        } catch (Exception e) {
            logger.error("Failed to start JFR recording", e);
            enabled = false;
        }
    }

    /**
     * Stops JFR recording and saves to file.
     */
    public Path stopRecording() {
        return stopRecording("pragmite-recording-" + System.currentTimeMillis() + ".jfr");
    }

    /**
     * Stops JFR recording and saves to specified file.
     */
    public Path stopRecording(String filename) {
        if (recording == null) {
            logger.warn("No active recording to stop");
            return null;
        }

        try {
            Path outputPath = Paths.get(filename);
            recording.dump(outputPath);
            recording.stop();
            recording.close();

            enabled = false;
            logger.info("JFR recording stopped and saved to: {}", outputPath.toAbsolutePath());

            return outputPath;
        } catch (IOException e) {
            logger.error("Failed to stop JFR recording", e);
            return null;
        } finally {
            recording = null;
        }
    }

    /**
     * Records a file analysis event.
     */
    public void recordFileAnalysis(String filePath, long durationMs, long fileSize,
                                   int linesOfCode, int complexity, String status) {
        if (!enabled) return;

        FileAnalysisEvent event = new FileAnalysisEvent();
        event.filePath = filePath;
        event.durationMs = durationMs;
        event.fileSize = fileSize;
        event.linesOfCode = linesOfCode;
        event.complexity = complexity;
        event.status = status;

        event.commit();

        // Update statistics
        updateStats("file_analysis", durationMs);
    }

    /**
     * Records a code smell detection event.
     */
    public void recordCodeSmell(String filePath, String smellType, String severity,
                               long detectionTimeMs, int smellCount) {
        if (!enabled) return;

        CodeSmellEvent event = new CodeSmellEvent();
        event.filePath = filePath;
        event.smellType = smellType;
        event.severity = severity;
        event.detectionTimeMs = detectionTimeMs;
        event.smellCount = smellCount;

        event.commit();

        // Update statistics
        updateStats("smell_detection_" + smellType, detectionTimeMs);
    }

    /**
     * Records a metrics calculation event.
     */
    public void recordMetricsCalculation(String filePath, String metricType,
                                        long durationMs, double metricValue) {
        if (!enabled) return;

        MetricsCalculationEvent event = new MetricsCalculationEvent();
        event.filePath = filePath;
        event.metricType = metricType;
        event.durationMs = durationMs;
        event.metricValue = metricValue;

        event.commit();

        // Update statistics
        updateStats("metrics_" + metricType, durationMs);
    }

    /**
     * Analyzes recorded events from a JFR file.
     */
    public JfrAnalysisReport analyzeRecording(Path jfrFile) {
        JfrAnalysisReport report = new JfrAnalysisReport();

        try (RecordingFile recordingFile = new RecordingFile(jfrFile)) {
            while (recordingFile.hasMoreEvents()) {
                RecordedEvent event = recordingFile.readEvent();

                if (event == null) continue;

                String eventName = event.getEventType().getName();

                if (eventName.contains("FileAnalysis")) {
                    report.totalFileAnalyses++;
                    report.totalAnalysisDuration += event.getLong("durationMs");
                } else if (eventName.contains("CodeSmell")) {
                    report.totalSmellsDetected += event.getInt("smellCount");
                    report.totalSmellDetectionTime += event.getLong("detectionTimeMs");
                } else if (eventName.contains("MetricsCalculation")) {
                    report.totalMetricsCalculated++;
                    report.totalMetricsCalculationTime += event.getLong("durationMs");
                }
            }

            if (report.totalFileAnalyses > 0) {
                report.averageAnalysisDuration =
                    (double) report.totalAnalysisDuration / report.totalFileAnalyses;
            }

            logger.info("JFR analysis complete: {}", report);
            return report;

        } catch (IOException e) {
            logger.error("Failed to analyze JFR recording", e);
            return report;
        }
    }

    /**
     * Gets current operation statistics.
     */
    public Map<String, OperationStats> getOperationStats() {
        return new HashMap<>(operationStats);
    }

    /**
     * Checks if JFR recording is enabled.
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Updates statistics for an operation.
     */
    private void updateStats(String operation, long durationMs) {
        operationStats.compute(operation, (k, v) -> {
            if (v == null) {
                v = new OperationStats();
            }
            v.count++;
            v.totalDuration += durationMs;
            v.minDuration = Math.min(v.minDuration, durationMs);
            v.maxDuration = Math.max(v.maxDuration, durationMs);
            return v;
        });
    }

    /**
     * Statistics for a specific operation type.
     */
    public static class OperationStats {
        public long count = 0;
        public long totalDuration = 0;
        public long minDuration = Long.MAX_VALUE;
        public long maxDuration = 0;

        public double getAverageDuration() {
            return count > 0 ? (double) totalDuration / count : 0;
        }

        @Override
        public String toString() {
            return String.format("count=%d, avg=%.2fms, min=%dms, max=%dms",
                               count, getAverageDuration(), minDuration, maxDuration);
        }
    }

    /**
     * Report from JFR analysis.
     */
    public static class JfrAnalysisReport {
        public long totalFileAnalyses = 0;
        public long totalAnalysisDuration = 0;
        public double averageAnalysisDuration = 0;
        public long totalSmellsDetected = 0;
        public long totalSmellDetectionTime = 0;
        public long totalMetricsCalculated = 0;
        public long totalMetricsCalculationTime = 0;

        @Override
        public String toString() {
            return String.format(
                "JFR Analysis Report: %d files analyzed (avg %.2fms), %d smells detected, %d metrics calculated",
                totalFileAnalyses, averageAnalysisDuration, totalSmellsDetected, totalMetricsCalculated
            );
        }
    }
}

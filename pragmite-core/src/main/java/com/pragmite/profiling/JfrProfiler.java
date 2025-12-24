package com.pragmite.profiling;

import jdk.jfr.Recording;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordingFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.*;

/**
 * Java Flight Recorder (JFR) integration for runtime performance profiling.
 * Captures CPU usage, memory allocation, method calls, and hotspots.
 *
 * Usage:
 * <pre>
 * JfrProfiler profiler = new JfrProfiler();
 * profiler.start();
 * // ... run analysis ...
 * ProfileReport report = profiler.stop();
 * </pre>
 */
public class JfrProfiler {
    private static final Logger logger = LoggerFactory.getLogger(JfrProfiler.class);

    private Recording recording;
    private Path recordingFile;

    /**
     * Starts JFR recording with default settings.
     */
    public void start() throws IOException {
        start(Duration.ofMinutes(10));
    }

    /**
     * Starts JFR recording with specified max duration.
     */
    public void start(Duration maxDuration) throws IOException {
        logger.info("Starting JFR recording...");

        recording = new Recording();

        // Enable CPU profiling
        recording.enable("jdk.CPULoad").withPeriod(Duration.ofSeconds(1));
        recording.enable("jdk.ExecutionSample").withPeriod(Duration.ofMillis(10));

        // Enable memory profiling
        recording.enable("jdk.ObjectAllocationInNewTLAB");
        recording.enable("jdk.ObjectAllocationOutsideTLAB");
        recording.enable("jdk.GCHeapSummary");

        // Enable method profiling
        recording.enable("jdk.JavaMethodStatistics");
        recording.enable("jdk.CompilerStatistics");

        // Set max duration to prevent runaway recordings
        recording.setMaxAge(maxDuration);
        recording.setMaxSize(100 * 1024 * 1024); // 100MB max

        // Start recording
        recording.start();

        logger.info("JFR recording started");
    }

    /**
     * Stops recording and analyzes results.
     */
    public ProfileReport stop() throws IOException {
        if (recording == null) {
            throw new IllegalStateException("Recording not started");
        }

        logger.info("Stopping JFR recording...");

        // Stop and dump to file
        recordingFile = Path.of("pragmite-profile-" + System.currentTimeMillis() + ".jfr");
        recording.dump(recordingFile);
        recording.close();

        logger.info("JFR recording saved to: {}", recordingFile);

        // Analyze the recording
        return analyzeRecording(recordingFile);
    }

    /**
     * Analyzes a JFR recording file and extracts performance insights.
     */
    private ProfileReport analyzeRecording(Path jfrFile) throws IOException {
        ProfileReport report = new ProfileReport();

        Map<String, Long> methodCalls = new HashMap<>();
        Map<String, Long> allocationSites = new HashMap<>();
        long totalCpuTime = 0;
        long totalAllocations = 0;

        try (RecordingFile recordingFile = new RecordingFile(jfrFile)) {
            while (recordingFile.hasMoreEvents()) {
                RecordedEvent event = recordingFile.readEvent();

                switch (event.getEventType().getName()) {
                    case "jdk.ExecutionSample":
                        // CPU hotspot
                        if (event.hasField("sampledThread")) {
                            String stackTrace = event.getStackTrace() != null
                                ? event.getStackTrace().toString()
                                : "unknown";

                            // Extract method name from stack trace
                            String method = extractMethodFromStackTrace(stackTrace);
                            if (method != null) {
                                methodCalls.merge(method, 1L, Long::sum);
                            }
                        }
                        totalCpuTime++;
                        break;

                    case "jdk.ObjectAllocationInNewTLAB":
                    case "jdk.ObjectAllocationOutsideTLAB":
                        // Memory allocation hotspot
                        if (event.hasField("allocationSize")) {
                            long size = event.getLong("allocationSize");
                            String stackTrace = event.getStackTrace() != null
                                ? event.getStackTrace().toString()
                                : "unknown";

                            String method = extractMethodFromStackTrace(stackTrace);
                            if (method != null) {
                                allocationSites.merge(method, size, Long::sum);
                            }
                            totalAllocations += size;
                        }
                        break;

                    case "jdk.CPULoad":
                        if (event.hasField("machineTotal")) {
                            double cpuLoad = event.getDouble("machineTotal");
                            report.addCpuSample(cpuLoad);
                        }
                        break;
                }
            }
        }

        // Find top hotspots
        report.setTopCpuMethods(getTopN(methodCalls, 10));
        report.setTopAllocationSites(getTopN(allocationSites, 10));
        report.setTotalCpuSamples(totalCpuTime);
        report.setTotalAllocations(totalAllocations);

        logger.info("Profile analysis complete: {} CPU samples, {} bytes allocated",
            totalCpuTime, totalAllocations);

        return report;
    }

    /**
     * Extracts method name from stack trace string.
     */
    private String extractMethodFromStackTrace(String stackTrace) {
        if (stackTrace == null || stackTrace.isEmpty()) {
            return null;
        }

        // Look for Pragmite methods only
        String[] lines = stackTrace.split("\n");
        for (String line : lines) {
            if (line.contains("com.pragmite")) {
                // Extract method name: "  at com.pragmite.package.Class.method(File.java:123)"
                int lastDot = line.lastIndexOf('.');
                int openParen = line.indexOf('(', lastDot);
                if (lastDot > 0 && openParen > lastDot) {
                    return line.substring(lastDot + 1, openParen).trim();
                }
            }
        }

        return null;
    }

    /**
     * Gets top N entries from a map sorted by value descending.
     */
    private <K> List<Map.Entry<K, Long>> getTopN(Map<K, Long> map, int n) {
        return map.entrySet().stream()
            .sorted(Map.Entry.<K, Long>comparingByValue().reversed())
            .limit(n)
            .toList();
    }

    /**
     * Gets the path to the recording file.
     */
    public Path getRecordingFile() {
        return recordingFile;
    }

    /**
     * Cleans up the recording file.
     */
    public void cleanup() throws IOException {
        if (recordingFile != null && recordingFile.toFile().exists()) {
            recordingFile.toFile().delete();
            logger.info("Deleted JFR recording file: {}", recordingFile);
        }
    }
}

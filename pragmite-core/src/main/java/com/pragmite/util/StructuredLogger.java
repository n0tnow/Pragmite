package com.pragmite.util;

import org.slf4j.Logger;
import org.slf4j.MDC;

import java.util.Map;

/**
 * Utility for structured logging with contextual information.
 * Supports adding metadata to log entries for better analysis.
 */
public class StructuredLogger {

    /**
     * Logs with structured context information.
     */
    public static void logWithContext(Logger logger, String level, String message, Map<String, String> context) {
        try {
            // Add context to MDC (Mapped Diagnostic Context)
            if (context != null) {
                context.forEach(MDC::put);
            }

            // Log the message
            switch (level.toUpperCase()) {
                case "DEBUG":
                    logger.debug(message);
                    break;
                case "INFO":
                    logger.info(message);
                    break;
                case "WARN":
                    logger.warn(message);
                    break;
                case "ERROR":
                    logger.error(message);
                    break;
                default:
                    logger.info(message);
            }
        } finally {
            // Clean up MDC
            if (context != null) {
                context.keySet().forEach(key -> MDC.remove(key));
            }
        }
    }

    /**
     * Logs an operation with timing and metadata.
     */
    public static void logOperation(Logger logger, String operation, long durationMs, boolean success, Map<String, String> metadata) {
        Map<String, String> context = new java.util.HashMap<>();
        context.put("operation", operation);
        context.put("duration_ms", String.valueOf(durationMs));
        context.put("success", String.valueOf(success));

        if (metadata != null) {
            context.putAll(metadata);
        }

        String message = String.format("Operation '%s' %s in %dms",
            operation,
            success ? "succeeded" : "failed",
            durationMs);

        logWithContext(logger, success ? "INFO" : "ERROR", message, context);
    }

    /**
     * Logs performance metrics.
     */
    public static void logMetric(Logger logger, String metricName, double value, String unit, Map<String, String> tags) {
        Map<String, String> context = new java.util.HashMap<>();
        context.put("metric", metricName);
        context.put("value", String.valueOf(value));
        context.put("unit", unit);

        if (tags != null) {
            tags.forEach((k, v) -> context.put("tag_" + k, v));
        }

        String message = String.format("METRIC: %s = %.2f %s", metricName, value, unit);
        logWithContext(logger, "INFO", message, context);
    }

    /**
     * Logs an error with full context.
     */
    public static void logError(Logger logger, String message, Throwable throwable, Map<String, String> context) {
        try {
            if (context != null) {
                context.forEach(MDC::put);
            }

            if (throwable != null) {
                logger.error(message, throwable);
            } else {
                logger.error(message);
            }
        } finally {
            if (context != null) {
                context.keySet().forEach(key -> MDC.remove(key));
            }
        }
    }

    /**
     * Creates a new operation context for tracking a workflow.
     */
    public static OperationContext startOperation(String operationName) {
        return new OperationContext(operationName);
    }

    /**
     * Context for tracking an operation's lifecycle.
     */
    public static class OperationContext implements AutoCloseable {
        private final String operationName;
        private final long startTime;
        private final Map<String, String> context;

        public OperationContext(String operationName) {
            this.operationName = operationName;
            this.startTime = System.currentTimeMillis();
            this.context = new java.util.HashMap<>();

            // Set operation context in MDC
            MDC.put("operation", operationName);
        }

        public OperationContext addContext(String key, String value) {
            context.put(key, value);
            MDC.put(key, value);
            return this;
        }

        public long getElapsedMs() {
            return System.currentTimeMillis() - startTime;
        }

        @Override
        public void close() {
            // Clean up MDC
            MDC.remove("operation");
            context.keySet().forEach(MDC::remove);
        }
    }
}

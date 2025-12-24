package com.pragmite.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tracks progress of long-running operations with ETA calculation.
 * Thread-safe implementation supporting nested operations.
 */
public class ProgressTracker {
    private static final Logger logger = LoggerFactory.getLogger(ProgressTracker.class);

    private final String operationName;
    private final long total;
    private final ProgressListener listener;

    private long current = 0;
    private long startTimeMs;
    private long lastUpdateTimeMs;
    private boolean started = false;
    private boolean completed = false;

    // For nested operations
    private ProgressTracker parentTracker;
    private double parentProgressContribution = 0.0;

    public ProgressTracker(String operationName, long total, ProgressListener listener) {
        this.operationName = operationName;
        this.total = total;
        this.listener = listener != null ? listener : ProgressListener.NOOP;
    }

    /**
     * Starts tracking progress.
     */
    public synchronized void start() {
        if (started) {
            logger.warn("Progress tracker for '{}' already started", operationName);
            return;
        }

        this.started = true;
        this.startTimeMs = System.currentTimeMillis();
        this.lastUpdateTimeMs = startTimeMs;

        listener.onStart(operationName, total);
        logger.info("Started tracking progress for '{}' (total: {})", operationName, total);
    }

    /**
     * Updates progress with a specific value.
     *
     * @param current Current progress value
     * @param message Descriptive message
     */
    public synchronized void update(long current, String message) {
        if (!started) {
            logger.warn("Progress tracker for '{}' not started", operationName);
            return;
        }

        if (completed) {
            logger.warn("Progress tracker for '{}' already completed", operationName);
            return;
        }

        this.current = Math.min(current, total);
        this.lastUpdateTimeMs = System.currentTimeMillis();

        listener.onProgress(this.current, total, message);

        // Update parent tracker if exists
        if (parentTracker != null) {
            double progressPercentage = getProgressPercentage();
            double parentProgress = progressPercentage * parentProgressContribution;
            parentTracker.updateFromChild(parentProgress);
        }

        // Log progress at 10% intervals
        double percentage = getProgressPercentage();
        if (percentage % 10 < 0.1 || this.current == total) {
            logger.info("Progress for '{}': {}% ({}/{}) - {} - ETA: {}",
                       operationName,
                       String.format("%.1f", percentage),
                       this.current,
                       total,
                       message,
                       getFormattedETA());
        }
    }

    /**
     * Increments progress by 1.
     *
     * @param message Descriptive message
     */
    public synchronized void increment(String message) {
        update(current + 1, message);
    }

    /**
     * Increments progress by a specific amount.
     *
     * @param amount Amount to increment
     * @param message Descriptive message
     */
    public synchronized void incrementBy(long amount, String message) {
        update(current + amount, message);
    }

    /**
     * Marks the operation as complete.
     *
     * @param success Whether the operation succeeded
     */
    public synchronized void complete(boolean success) {
        if (completed) {
            return;
        }

        this.completed = true;
        this.current = total;

        listener.onComplete(operationName, success);

        long elapsedMs = System.currentTimeMillis() - startTimeMs;
        logger.info("Completed '{}' in {} (success: {})",
                   operationName,
                   formatDuration(elapsedMs),
                   success);
    }

    /**
     * Gets current progress percentage (0-100).
     */
    public synchronized double getProgressPercentage() {
        if (total == 0) {
            return 0.0;
        }
        return (double) current / total * 100.0;
    }

    /**
     * Gets estimated time remaining in milliseconds.
     * Returns -1 if ETA cannot be calculated.
     */
    public synchronized long getETAMillis() {
        if (current == 0 || total == 0) {
            return -1;
        }

        long elapsedMs = System.currentTimeMillis() - startTimeMs;
        long remainingWork = total - current;

        // Calculate ETA based on average speed
        double workPerMs = (double) current / elapsedMs;
        if (workPerMs <= 0) {
            return -1;
        }

        return (long) (remainingWork / workPerMs);
    }

    /**
     * Gets formatted ETA string.
     */
    public synchronized String getFormattedETA() {
        long etaMs = getETAMillis();
        if (etaMs < 0) {
            return "calculating...";
        }
        return formatDuration(etaMs);
    }

    /**
     * Gets elapsed time in milliseconds.
     */
    public synchronized long getElapsedMillis() {
        if (!started) {
            return 0;
        }
        return System.currentTimeMillis() - startTimeMs;
    }

    /**
     * Creates a child tracker for nested operations.
     *
     * @param childOperationName Name of child operation
     * @param childTotal Total units of work for child
     * @param progressContribution How much of parent's progress this child represents (0.0-1.0)
     */
    public ProgressTracker createChildTracker(String childOperationName, long childTotal, double progressContribution) {
        ProgressTracker childTracker = new ProgressTracker(childOperationName, childTotal, listener);
        childTracker.parentTracker = this;
        childTracker.parentProgressContribution = progressContribution;

        listener.onSubtaskStart(childOperationName, getProgressPercentage());

        return childTracker;
    }

    /**
     * Updates progress from a child tracker.
     */
    private synchronized void updateFromChild(double childProgress) {
        // Child progress is already weighted by parentProgressContribution
        long childContributionAbsolute = (long) (childProgress / 100.0 * total);

        // Don't let child updates decrease overall progress
        if (current + childContributionAbsolute > current) {
            update(current + childContributionAbsolute, "Processing subtask");
        }
    }

    /**
     * Formats duration in human-readable format.
     */
    private static String formatDuration(long ms) {
        if (ms < 1000) {
            return ms + "ms";
        } else if (ms < 60000) {
            return String.format("%.1fs", ms / 1000.0);
        } else if (ms < 3600000) {
            long minutes = ms / 60000;
            long seconds = (ms % 60000) / 1000;
            return String.format("%dm %ds", minutes, seconds);
        } else {
            long hours = ms / 3600000;
            long minutes = (ms % 3600000) / 60000;
            return String.format("%dh %dm", hours, minutes);
        }
    }

    /**
     * Gets current progress value.
     */
    public synchronized long getCurrent() {
        return current;
    }

    /**
     * Gets total progress value.
     */
    public synchronized long getTotal() {
        return total;
    }

    /**
     * Gets operation name.
     */
    public String getOperationName() {
        return operationName;
    }

    /**
     * Checks if tracker has been started.
     */
    public synchronized boolean isStarted() {
        return started;
    }

    /**
     * Checks if tracker has been completed.
     */
    public synchronized boolean isCompleted() {
        return completed;
    }
}

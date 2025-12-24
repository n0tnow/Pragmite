package com.pragmite.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Console-based progress listener that prints progress updates to stdout.
 * Designed for CLI applications.
 */
public class ConsoleProgressListener implements ProgressListener {
    private static final Logger logger = LoggerFactory.getLogger(ConsoleProgressListener.class);

    private final boolean verbose;
    private long lastUpdateTimeMs = 0;
    private static final long MIN_UPDATE_INTERVAL_MS = 500; // Update at most every 500ms

    public ConsoleProgressListener() {
        this(false);
    }

    public ConsoleProgressListener(boolean verbose) {
        this.verbose = verbose;
    }

    @Override
    public void onStart(String operationName, long estimatedTotal) {
        System.out.println();
        System.out.println("╔═══════════════════════════════════════════════════════════════════╗");
        System.out.printf("║ Starting: %-55s ║%n", truncate(operationName, 55));
        System.out.printf("║ Total units: %-51d ║%n", estimatedTotal);
        System.out.println("╚═══════════════════════════════════════════════════════════════════╝");
        System.out.println();
    }

    @Override
    public void onProgress(long current, long total, String message) {
        // Rate limit updates to avoid flooding console
        long now = System.currentTimeMillis();
        if (now - lastUpdateTimeMs < MIN_UPDATE_INTERVAL_MS && current < total) {
            return;
        }
        lastUpdateTimeMs = now;

        double percentage = total > 0 ? (double) current / total * 100.0 : 0.0;
        String progressBar = createProgressBar(percentage, 40);

        // Clear line and print progress
        System.out.print("\r");
        System.out.printf("[%s] %5.1f%% (%d/%d) - %s",
                         progressBar,
                         percentage,
                         current,
                         total,
                         truncate(message, 40));
        System.out.flush();

        // Print newline if complete or in verbose mode
        if (current >= total || verbose) {
            System.out.println();
        }
    }

    @Override
    public void onComplete(String operationName, boolean success) {
        System.out.println(); // Ensure newline after progress bar
        System.out.println();

        if (success) {
            System.out.println("╔═══════════════════════════════════════════════════════════════════╗");
            System.out.printf("║ ✓ Completed: %-53s ║%n", truncate(operationName, 53));
            System.out.println("╚═══════════════════════════════════════════════════════════════════╝");
        } else {
            System.out.println("╔═══════════════════════════════════════════════════════════════════╗");
            System.out.printf("║ ✗ Failed: %-56s ║%n", truncate(operationName, 56));
            System.out.println("╚═══════════════════════════════════════════════════════════════════╝");
        }
        System.out.println();
    }

    @Override
    public void onSubtaskStart(String taskName, double parentProgress) {
        if (verbose) {
            System.out.println();
            System.out.printf("  ↳ Starting subtask: %s (parent at %.1f%%)%n", taskName, parentProgress);
        }
    }

    @Override
    public void onSubtaskComplete(String taskName, boolean success) {
        if (verbose) {
            System.out.printf("  ↳ Subtask %s: %s%n", taskName, success ? "✓" : "✗");
        }
    }

    /**
     * Creates a text-based progress bar.
     */
    private String createProgressBar(double percentage, int width) {
        int filled = (int) (percentage / 100.0 * width);
        int empty = width - filled;

        StringBuilder bar = new StringBuilder();

        // Filled portion
        for (int i = 0; i < filled; i++) {
            bar.append("█");
        }

        // Empty portion
        for (int i = 0; i < empty; i++) {
            bar.append("░");
        }

        return bar.toString();
    }

    /**
     * Truncates a string to a maximum length with ellipsis.
     */
    private String truncate(String str, int maxLength) {
        if (str == null) {
            return "";
        }
        if (str.length() <= maxLength) {
            return str;
        }
        return str.substring(0, maxLength - 3) + "...";
    }
}

package com.pragmite.util;

import com.pragmite.exception.ErrorCode;
import com.pragmite.exception.PragmiteException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Monitors memory usage and prevents out-of-memory errors.
 * Provides warnings and enforces limits.
 */
public class MemoryMonitor {
    private static final Logger logger = LoggerFactory.getLogger(MemoryMonitor.class);

    private static final double DEFAULT_WARNING_THRESHOLD = 0.80; // 80%
    private static final double DEFAULT_CRITICAL_THRESHOLD = 0.90; // 90%
    private static final long DEFAULT_CHECK_INTERVAL_MS = 1000; // 1 second

    private final Runtime runtime;
    private final double warningThreshold;
    private final double criticalThreshold;
    private long lastCheckTime = 0;
    private final long checkIntervalMs;

    public MemoryMonitor() {
        this(DEFAULT_WARNING_THRESHOLD, DEFAULT_CRITICAL_THRESHOLD);
    }

    public MemoryMonitor(double warningThreshold, double criticalThreshold) {
        this.runtime = Runtime.getRuntime();
        this.warningThreshold = warningThreshold;
        this.criticalThreshold = criticalThreshold;
        this.checkIntervalMs = DEFAULT_CHECK_INTERVAL_MS;

        logger.info("Memory monitor initialized (warning: {}%, critical: {}%)",
                   (int)(warningThreshold * 100), (int)(criticalThreshold * 100));
    }

    /**
     * Checks current memory usage and throws if critical.
     * @throws PragmiteException if memory usage exceeds critical threshold
     */
    public void checkMemory() throws PragmiteException {
        long currentTime = System.currentTimeMillis();

        // Rate limit checks
        if (currentTime - lastCheckTime < checkIntervalMs) {
            return;
        }

        lastCheckTime = currentTime;

        MemoryStats stats = getMemoryStats();
        double usageRatio = stats.getUsageRatio();

        if (usageRatio >= criticalThreshold) {
            // Critical - attempt GC and check again
            logger.warn("Memory usage critical ({}%), attempting garbage collection", (int)(usageRatio * 100));
            System.gc();

            // Re-check after GC
            stats = getMemoryStats();
            usageRatio = stats.getUsageRatio();

            if (usageRatio >= criticalThreshold) {
                throw new PragmiteException(
                    ErrorCode.OUT_OF_MEMORY,
                    String.format("Memory usage too high: %d%% (limit: %d%%)",
                                (int)(usageRatio * 100), (int)(criticalThreshold * 100))
                );
            }
        } else if (usageRatio >= warningThreshold) {
            logger.warn("Memory usage high: {}% (warning threshold: {}%)",
                       (int)(usageRatio * 100), (int)(warningThreshold * 100));
        }
    }

    /**
     * Gets current memory statistics.
     */
    public MemoryStats getMemoryStats() {
        long maxMemory = runtime.maxMemory();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;

        return new MemoryStats(maxMemory, totalMemory, usedMemory, freeMemory);
    }

    /**
     * Estimates memory required for an operation.
     * @param estimatedBytes Estimated bytes needed
     * @return true if there's likely enough memory
     */
    public boolean hasEnoughMemory(long estimatedBytes) {
        MemoryStats stats = getMemoryStats();
        long available = stats.getFreeMemory();

        // Add 20% buffer
        long required = (long) (estimatedBytes * 1.2);

        return available >= required;
    }

    /**
     * Suggests garbage collection if memory usage is high.
     */
    public void suggestGC() {
        MemoryStats stats = getMemoryStats();
        if (stats.getUsageRatio() > 0.75) {
            logger.debug("Suggesting garbage collection (usage: {}%)", (int)(stats.getUsageRatio() * 100));
            System.gc();
        }
    }

    /**
     * Memory statistics.
     */
    public static class MemoryStats {
        private final long maxMemory;
        private final long totalMemory;
        private final long usedMemory;
        private final long freeMemory;

        public MemoryStats(long maxMemory, long totalMemory, long usedMemory, long freeMemory) {
            this.maxMemory = maxMemory;
            this.totalMemory = totalMemory;
            this.usedMemory = usedMemory;
            this.freeMemory = freeMemory;
        }

        public long getMaxMemory() { return maxMemory; }
        public long getTotalMemory() { return totalMemory; }
        public long getUsedMemory() { return usedMemory; }
        public long getFreeMemory() { return freeMemory; }

        public double getUsageRatio() {
            return (double) usedMemory / maxMemory;
        }

        public String getUsedMemoryMB() {
            return String.format("%d MB", usedMemory / 1024 / 1024);
        }

        public String getMaxMemoryMB() {
            return String.format("%d MB", maxMemory / 1024 / 1024);
        }

        public String getFreeMemoryMB() {
            return String.format("%d MB", freeMemory / 1024 / 1024);
        }

        @Override
        public String toString() {
            return String.format("Memory: %s / %s (%.1f%% used, %s free)",
                               getUsedMemoryMB(), getMaxMemoryMB(),
                               getUsageRatio() * 100, getFreeMemoryMB());
        }
    }
}

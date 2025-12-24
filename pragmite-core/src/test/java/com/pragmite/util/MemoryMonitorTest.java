package com.pragmite.util;

import com.pragmite.exception.PragmiteException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assumptions;

import static org.junit.jupiter.api.Assertions.*;

class MemoryMonitorTest {

    private MemoryMonitor monitor;

    @BeforeEach
    void setUp() {
        monitor = new MemoryMonitor();
    }

    @Test
    void testDefaultConstructor() {
        assertNotNull(monitor);
    }

    @Test
    void testCustomThresholds() {
        MemoryMonitor customMonitor = new MemoryMonitor(0.70, 0.85);
        assertNotNull(customMonitor);
    }

    @Test
    void testGetMemoryStats() {
        MemoryMonitor.MemoryStats stats = monitor.getMemoryStats();

        assertNotNull(stats);
        assertTrue(stats.getMaxMemory() > 0);
        assertTrue(stats.getTotalMemory() > 0);
        assertTrue(stats.getUsedMemory() >= 0);
        assertTrue(stats.getFreeMemory() >= 0);
    }

    @Test
    void testMemoryStatsUsageRatio() {
        MemoryMonitor.MemoryStats stats = monitor.getMemoryStats();

        double usageRatio = stats.getUsageRatio();

        assertTrue(usageRatio >= 0.0);
        assertTrue(usageRatio <= 1.0);
    }

    @Test
    void testMemoryStatsFormatting() {
        MemoryMonitor.MemoryStats stats = monitor.getMemoryStats();

        String usedMB = stats.getUsedMemoryMB();
        String maxMB = stats.getMaxMemoryMB();
        String freeMB = stats.getFreeMemoryMB();

        assertNotNull(usedMB);
        assertNotNull(maxMB);
        assertNotNull(freeMB);

        assertTrue(usedMB.endsWith(" MB"));
        assertTrue(maxMB.endsWith(" MB"));
        assertTrue(freeMB.endsWith(" MB"));
    }

    @Test
    void testMemoryStatsToString() {
        MemoryMonitor.MemoryStats stats = monitor.getMemoryStats();

        String statsString = stats.toString();

        assertNotNull(statsString);
        assertTrue(statsString.contains("Memory:"));
        assertTrue(statsString.contains("MB"));
        assertTrue(statsString.contains("%"));
        assertTrue(statsString.contains("used"));
        assertTrue(statsString.contains("free"));
    }

    @Test
    void testMemoryStatsGetters() {
        long maxMemory = 1024 * 1024 * 100; // 100 MB
        long totalMemory = 1024 * 1024 * 80;  // 80 MB
        long usedMemory = 1024 * 1024 * 50;   // 50 MB
        long freeMemory = 1024 * 1024 * 30;   // 30 MB

        MemoryMonitor.MemoryStats stats = new MemoryMonitor.MemoryStats(
            maxMemory, totalMemory, usedMemory, freeMemory
        );

        assertEquals(maxMemory, stats.getMaxMemory());
        assertEquals(totalMemory, stats.getTotalMemory());
        assertEquals(usedMemory, stats.getUsedMemory());
        assertEquals(freeMemory, stats.getFreeMemory());
    }

    @Test
    void testMemoryStatsUsageRatioCalculation() {
        long maxMemory = 1000;
        long usedMemory = 500;

        MemoryMonitor.MemoryStats stats = new MemoryMonitor.MemoryStats(
            maxMemory, 800, usedMemory, 300
        );

        assertEquals(0.5, stats.getUsageRatio(), 0.01);
    }

    @Test
    void testCheckMemory_Normal() {
        // Under normal conditions, should not throw
        assertDoesNotThrow(() -> monitor.checkMemory());
    }

    @Test
    void testCheckMemory_RateLimit() throws Exception {
        // First check
        monitor.checkMemory();

        // Immediate second check should be rate-limited (no actual check)
        assertDoesNotThrow(() -> monitor.checkMemory());

        // Wait for rate limit to expire
        Thread.sleep(1100);

        // Should check again now
        assertDoesNotThrow(() -> monitor.checkMemory());
    }

    @Test
    void testCheckMemory_Critical() {
        // Get current memory usage to determine if test is viable
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory();
        long usedMemory = runtime.totalMemory() - runtime.freeMemory();
        double currentUsage = (double) usedMemory / maxMemory;

        // Skip test if already near memory limit (can't reliably test critical threshold)
        Assumptions.assumeTrue(currentUsage < 0.50,
            "Test skipped: Current memory usage too high (" + (int)(currentUsage * 100) + "%)");

        // Create monitor with threshold slightly above current usage
        double warningThreshold = Math.max(0.40, currentUsage + 0.05);
        double criticalThreshold = Math.max(0.50, currentUsage + 0.10);
        MemoryMonitor testMonitor = new MemoryMonitor(warningThreshold, criticalThreshold);

        // Under normal conditions, check should pass
        assertDoesNotThrow(() -> testMonitor.checkMemory(),
            "Memory check should not throw under normal conditions");
    }

    @Test
    void testHasEnoughMemory() {
        // Small request should succeed
        assertTrue(monitor.hasEnoughMemory(1024));

        // Very large request should fail
        assertFalse(monitor.hasEnoughMemory(Long.MAX_VALUE / 2));
    }

    @Test
    void testHasEnoughMemory_WithBuffer() {
        MemoryMonitor.MemoryStats stats = monitor.getMemoryStats();
        long available = stats.getFreeMemory();

        // Request exactly available memory (with 20% buffer, should fail)
        assertFalse(monitor.hasEnoughMemory(available));

        // Request 50% of available (with buffer, should succeed)
        assertTrue(monitor.hasEnoughMemory(available / 2));
    }

    @Test
    void testSuggestGC() {
        // Should not throw
        assertDoesNotThrow(() -> monitor.suggestGC());

        // Get stats before and after
        MemoryMonitor.MemoryStats statsBefore = monitor.getMemoryStats();

        monitor.suggestGC();

        MemoryMonitor.MemoryStats statsAfter = monitor.getMemoryStats();

        // Both stats should be valid
        assertNotNull(statsBefore);
        assertNotNull(statsAfter);
    }

    @Test
    void testMultipleMemoryChecks() throws Exception {
        for (int i = 0; i < 5; i++) {
            assertDoesNotThrow(() -> monitor.checkMemory());
            Thread.sleep(300);
        }
    }

    @Test
    void testMemoryStatsConsistency() {
        MemoryMonitor.MemoryStats stats = monitor.getMemoryStats();

        // Used + Free should be approximately Total
        // (may not be exact due to concurrent GC)
        long calculatedTotal = stats.getUsedMemory() + stats.getFreeMemory();
        long actualTotal = stats.getTotalMemory();

        // Allow some variance due to GC
        assertTrue(Math.abs(calculatedTotal - actualTotal) < stats.getMaxMemory() * 0.1,
            "Used + Free should approximately equal Total");
    }

    @Test
    void testMemoryStatsRelationships() {
        MemoryMonitor.MemoryStats stats = monitor.getMemoryStats();

        // Max >= Total
        assertTrue(stats.getMaxMemory() >= stats.getTotalMemory(),
            "Max memory should be >= Total memory");

        // Used >= 0
        assertTrue(stats.getUsedMemory() >= 0,
            "Used memory should be non-negative");

        // Free >= 0
        assertTrue(stats.getFreeMemory() >= 0,
            "Free memory should be non-negative");
    }

    @Test
    void testHighMemoryUsageScenario() {
        // Allocate some memory to increase usage
        byte[][] arrays = new byte[100][];
        try {
            for (int i = 0; i < arrays.length; i++) {
                arrays[i] = new byte[1024 * 1024]; // 1 MB each
            }

            MemoryMonitor.MemoryStats stats = monitor.getMemoryStats();

            // Usage should have increased
            assertTrue(stats.getUsedMemory() > 0);

            // Check memory should still work
            assertDoesNotThrow(() -> monitor.checkMemory());

        } finally {
            // Clear references
            for (int i = 0; i < arrays.length; i++) {
                arrays[i] = null;
            }
        }
    }

    @Test
    void testMemoryMonitorWithDifferentThresholds() {
        MemoryMonitor lowThreshold = new MemoryMonitor(0.50, 0.60);
        MemoryMonitor mediumThreshold = new MemoryMonitor(0.75, 0.85);
        MemoryMonitor highThreshold = new MemoryMonitor(0.90, 0.95);

        // All should get stats successfully
        assertNotNull(lowThreshold.getMemoryStats());
        assertNotNull(mediumThreshold.getMemoryStats());
        assertNotNull(highThreshold.getMemoryStats());
    }

    @Test
    void testMemoryStatsFormatConsistency() {
        MemoryMonitor.MemoryStats stats = monitor.getMemoryStats();

        String used = stats.getUsedMemoryMB();
        String max = stats.getMaxMemoryMB();
        String free = stats.getFreeMemoryMB();

        // All should have same format pattern
        assertTrue(used.matches("\\d+ MB"));
        assertTrue(max.matches("\\d+ MB"));
        assertTrue(free.matches("\\d+ MB"));
    }

    @Test
    void testConcurrentMemoryChecks() throws Exception {
        int threadCount = 10;
        Thread[] threads = new Thread[threadCount];

        for (int i = 0; i < threadCount; i++) {
            threads[i] = new Thread(() -> {
                for (int j = 0; j < 5; j++) {
                    assertDoesNotThrow(() -> monitor.checkMemory());
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            });
            threads[i].start();
        }

        // Wait for all threads
        for (Thread thread : threads) {
            thread.join(5000);
        }
    }

    @Test
    void testMemoryStatsZeroUsage() {
        // Edge case: stats with zero usage
        MemoryMonitor.MemoryStats stats = new MemoryMonitor.MemoryStats(
            1000, 1000, 0, 1000
        );

        assertEquals(0.0, stats.getUsageRatio(), 0.001);
        assertEquals("0 MB", stats.getUsedMemoryMB());
    }

    @Test
    void testMemoryStatsFullUsage() {
        // Edge case: stats with full usage
        MemoryMonitor.MemoryStats stats = new MemoryMonitor.MemoryStats(
            1000, 1000, 1000, 0
        );

        assertEquals(1.0, stats.getUsageRatio(), 0.001);
        assertEquals("0 MB", stats.getFreeMemoryMB());
    }
}

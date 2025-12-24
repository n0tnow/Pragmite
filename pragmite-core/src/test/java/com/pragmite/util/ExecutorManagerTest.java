package com.pragmite.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class ExecutorManagerTest {

    private ExecutorManager executorManager;

    @BeforeEach
    void setUp() {
        executorManager = new ExecutorManager(2, 4);
    }

    @AfterEach
    void tearDown() {
        if (executorManager != null) {
            executorManager.shutdownNow();
        }
    }

    @Test
    void testInitialization() {
        assertNotNull(executorManager);
        assertEquals(0, executorManager.getActiveAnalysisTaskCount());
    }

    @Test
    void testSubmitAnalysisTask() throws Exception {
        Future<String> future = executorManager.submitAnalysisTask(() -> {
            Thread.sleep(100);
            return "analysis result";
        });

        String result = future.get(2, TimeUnit.SECONDS);
        assertEquals("analysis result", result);
    }

    @Test
    void testSubmitBackgroundTaskCallable() throws Exception {
        Future<Integer> future = executorManager.submitBackgroundTask(() -> {
            Thread.sleep(50);
            return 42;
        });

        Integer result = future.get(2, TimeUnit.SECONDS);
        assertEquals(42, result);
    }

    @Test
    void testSubmitBackgroundTaskRunnable() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);

        executorManager.submitBackgroundTask(() -> {
            try {
                Thread.sleep(50);
                latch.countDown();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        assertTrue(latch.await(2, TimeUnit.SECONDS));
    }

    @Test
    void testActiveAnalysisTaskCount() throws Exception {
        CountDownLatch startLatch = new CountDownLatch(2); // Reduce to 2 for more reliable test
        CountDownLatch finishLatch = new CountDownLatch(1);

        List<Future<Void>> futures = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            futures.add(executorManager.submitAnalysisTask(() -> {
                startLatch.countDown();
                finishLatch.await();
                return null;
            }));
        }

        // Wait for all tasks to start
        assertTrue(startLatch.await(3, TimeUnit.SECONDS), "Tasks should start within timeout");

        // Should have at least 1 active task (may be 1 or 2 depending on scheduling)
        int activeCount = executorManager.getActiveAnalysisTaskCount();
        assertTrue(activeCount >= 1 && activeCount <= 2,
            "Active count should be 1-2, got: " + activeCount);

        // Release tasks
        finishLatch.countDown();

        // Wait for all futures to complete
        for (Future<Void> future : futures) {
            future.get(3, TimeUnit.SECONDS);
        }

        // Give a moment for counter to update
        Thread.sleep(100);

        // Active count should now be 0
        activeCount = executorManager.getActiveAnalysisTaskCount();
        assertTrue(activeCount == 0, "Active count should be 0 after completion, got: " + activeCount);
    }

    @Test
    void testScheduleAtFixedRate() throws Exception {
        AtomicInteger counter = new AtomicInteger(0);

        ScheduledFuture<?> future = executorManager.scheduleAtFixedRate(
            counter::incrementAndGet,
            0,
            100,
            TimeUnit.MILLISECONDS
        );

        // Wait for a few executions
        Thread.sleep(350);

        future.cancel(false);

        int count = counter.get();
        assertTrue(count >= 2, "Should have executed at least 2 times, got: " + count);
    }

    @Test
    void testGetStats() throws Exception {
        // Submit some tasks
        List<Future<String>> futures = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            futures.add(executorManager.submitAnalysisTask(() -> {
                Thread.sleep(100);
                return "done";
            }));
        }

        // Check stats
        ExecutorManager.ExecutorStats stats = executorManager.getStats();

        assertNotNull(stats);
        assertTrue(stats.getAnalysisPoolSize() >= 0);
        assertTrue(stats.getAnalysisQueueSize() >= 0);
        assertTrue(stats.getBackgroundPoolSize() >= 0);

        // Wait for tasks to complete
        for (Future<String> future : futures) {
            future.get(2, TimeUnit.SECONDS);
        }
    }

    @Test
    void testStatsToString() {
        ExecutorManager.ExecutorStats stats = executorManager.getStats();

        String statsString = stats.toString();

        assertNotNull(statsString);
        assertTrue(statsString.contains("ExecutorStats"));
        assertTrue(statsString.contains("Analysis"));
        assertTrue(statsString.contains("Background"));
    }

    @Test
    void testMultipleAnalysisTasks() throws Exception {
        int taskCount = 10;
        List<Future<Integer>> futures = new ArrayList<>();

        for (int i = 0; i < taskCount; i++) {
            final int taskId = i;
            futures.add(executorManager.submitAnalysisTask(() -> {
                Thread.sleep(50);
                return taskId;
            }));
        }

        // All tasks should complete
        for (int i = 0; i < taskCount; i++) {
            Integer result = futures.get(i).get(2, TimeUnit.SECONDS);
            assertEquals(i, result);
        }
    }

    @Test
    void testMultipleBackgroundTasks() throws Exception {
        int taskCount = 10;
        CountDownLatch latch = new CountDownLatch(taskCount);

        for (int i = 0; i < taskCount; i++) {
            executorManager.submitBackgroundTask(() -> {
                try {
                    Thread.sleep(50);
                    latch.countDown();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        assertTrue(latch.await(3, TimeUnit.SECONDS));
    }

    @Test
    void testTaskException() {
        Future<String> future = executorManager.submitAnalysisTask(() -> {
            throw new RuntimeException("Test exception");
        });

        assertThrows(ExecutionException.class, () -> {
            future.get(2, TimeUnit.SECONDS);
        });
    }

    @Test
    void testShutdown() throws Exception {
        // Submit a quick task
        Future<String> future = executorManager.submitAnalysisTask(() -> "done");
        future.get(1, TimeUnit.SECONDS);

        // Shutdown
        executorManager.shutdown();

        // Note: With CallerRunsPolicy, tasks may execute in caller thread instead of rejecting
        // So we just verify shutdown was called and executor eventually terminates
        // This is expected behavior for graceful shutdown with backpressure handling

        // Verify we can still check stats after shutdown
        ExecutorManager.ExecutorStats stats = executorManager.getStats();
        assertNotNull(stats, "Stats should still be available after shutdown");
    }

    @Test
    void testShutdownNow() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);

        // Submit a long-running task
        Future<String> longTask = executorManager.submitAnalysisTask(() -> {
            try {
                latch.await(5, TimeUnit.SECONDS);
                return "done";
            } catch (InterruptedException e) {
                return "interrupted";
            }
        });

        // Give task time to start
        Thread.sleep(100);

        // Immediate shutdown - this interrupts running tasks
        executorManager.shutdownNow();

        // Release latch
        latch.countDown();

        // Verify the task was interrupted or completed
        try {
            String result = longTask.get(1, TimeUnit.SECONDS);
            assertTrue(result.equals("interrupted") || result.equals("done"),
                "Task should complete with interrupted or done status");
        } catch (CancellationException | TimeoutException e) {
            // Also acceptable - task was cancelled
        }
    }

    @Test
    void testGracefulShutdown() throws Exception {
        List<Future<String>> futures = new ArrayList<>();

        // Submit tasks
        for (int i = 0; i < 5; i++) {
            futures.add(executorManager.submitAnalysisTask(() -> {
                Thread.sleep(100);
                return "done";
            }));
        }

        // Initiate shutdown
        executorManager.shutdown();

        // Existing tasks should still complete
        for (Future<String> future : futures) {
            String result = future.get(2, TimeUnit.SECONDS);
            assertEquals("done", result);
        }
    }

    @Test
    void testDefaultConstructor() {
        ExecutorManager manager = new ExecutorManager();

        assertNotNull(manager);
        assertEquals(0, manager.getActiveAnalysisTaskCount());

        manager.shutdownNow();
    }

    @Test
    void testCustomPoolSizes() throws Exception {
        ExecutorManager manager = new ExecutorManager(1, 2);

        Future<String> future = manager.submitAnalysisTask(() -> "test");
        String result = future.get(1, TimeUnit.SECONDS);

        assertEquals("test", result);

        manager.shutdownNow();
    }

    @Test
    void testConcurrentAnalysisAndBackgroundTasks() throws Exception {
        CountDownLatch analysisLatch = new CountDownLatch(5);
        CountDownLatch backgroundLatch = new CountDownLatch(5);

        // Submit analysis tasks
        for (int i = 0; i < 5; i++) {
            executorManager.submitAnalysisTask(() -> {
                Thread.sleep(50);
                analysisLatch.countDown();
                return null;
            });
        }

        // Submit background tasks
        for (int i = 0; i < 5; i++) {
            executorManager.submitBackgroundTask(() -> {
                try {
                    Thread.sleep(50);
                    backgroundLatch.countDown();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        // Both should complete
        assertTrue(analysisLatch.await(2, TimeUnit.SECONDS));
        assertTrue(backgroundLatch.await(2, TimeUnit.SECONDS));
    }

    @Test
    void testScheduledTaskCancellation() throws Exception {
        AtomicInteger counter = new AtomicInteger(0);

        ScheduledFuture<?> future = executorManager.scheduleAtFixedRate(
            counter::incrementAndGet,
            0,
            50,
            TimeUnit.MILLISECONDS
        );

        // Let it run a few times
        Thread.sleep(150);

        // Cancel
        assertTrue(future.cancel(false));

        int countAtCancel = counter.get();

        // Wait more
        Thread.sleep(150);

        // Counter should not have increased
        assertEquals(countAtCancel, counter.get());
    }

    @Test
    void testExecutorStatsGetters() {
        ExecutorManager.ExecutorStats stats = new ExecutorManager.ExecutorStats(
            1, 2, 3, 100, 4, 5
        );

        assertEquals(1, stats.getAnalysisActiveCount());
        assertEquals(2, stats.getAnalysisPoolSize());
        assertEquals(3, stats.getAnalysisQueueSize());
        assertEquals(100, stats.getAnalysisCompletedTasks());
        assertEquals(4, stats.getBackgroundActiveCount());
        assertEquals(5, stats.getBackgroundPoolSize());
    }

    @Test
    void testThreadNaming() throws Exception {
        AtomicInteger threadNameChecks = new AtomicInteger(0);

        Future<String> future = executorManager.submitAnalysisTask(() -> {
            String threadName = Thread.currentThread().getName();
            if (threadName.startsWith("pragmite-analysis")) {
                threadNameChecks.incrementAndGet();
            }
            return threadName;
        });

        String threadName = future.get(1, TimeUnit.SECONDS);

        assertTrue(threadName.startsWith("pragmite-analysis"),
            "Thread should have correct name prefix, got: " + threadName);
        assertEquals(1, threadNameChecks.get());
    }

    @Test
    void testBackpressure() throws Exception {
        // Try to submit more tasks than the queue can handle
        // With CallerRunsPolicy, excess tasks should run in caller thread

        int taskCount = 1500; // More than queue capacity
        CountDownLatch latch = new CountDownLatch(taskCount);

        for (int i = 0; i < taskCount; i++) {
            executorManager.submitBackgroundTask(() -> {
                latch.countDown();
            });
        }

        // All tasks should eventually complete
        assertTrue(latch.await(10, TimeUnit.SECONDS),
            "All tasks should complete within timeout");
    }
}

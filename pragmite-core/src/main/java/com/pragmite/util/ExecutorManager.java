package com.pragmite.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Manages thread pools for parallel analysis operations.
 * Provides configurable thread pools with proper shutdown handling.
 */
public class ExecutorManager {
    private static final Logger logger = LoggerFactory.getLogger(ExecutorManager.class);

    private static final int DEFAULT_CORE_POOL_SIZE = Runtime.getRuntime().availableProcessors();
    private static final int DEFAULT_MAX_POOL_SIZE = Runtime.getRuntime().availableProcessors() * 2;
    private static final long DEFAULT_KEEP_ALIVE_TIME = 60L;
    private static final int DEFAULT_QUEUE_CAPACITY = 1000;

    private final ExecutorService analysisExecutor;
    private final ExecutorService backgroundExecutor;
    private final ScheduledExecutorService scheduledExecutor;
    private final AtomicInteger activeAnalysisTasks = new AtomicInteger(0);

    public ExecutorManager() {
        this(DEFAULT_CORE_POOL_SIZE, DEFAULT_MAX_POOL_SIZE);
    }

    public ExecutorManager(int corePoolSize, int maxPoolSize) {
        logger.info("Initializing ExecutorManager (core: {}, max: {})", corePoolSize, maxPoolSize);

        // Thread factory with meaningful names
        ThreadFactory analysisThreadFactory = new NamedThreadFactory("pragmite-analysis");
        ThreadFactory backgroundThreadFactory = new NamedThreadFactory("pragmite-background");
        ThreadFactory scheduledThreadFactory = new NamedThreadFactory("pragmite-scheduled");

        // Analysis executor for CPU-intensive tasks (file analysis, parsing)
        this.analysisExecutor = new ThreadPoolExecutor(
            corePoolSize,
            maxPoolSize,
            DEFAULT_KEEP_ALIVE_TIME,
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(DEFAULT_QUEUE_CAPACITY),
            analysisThreadFactory,
            new ThreadPoolExecutor.CallerRunsPolicy() // Backpressure handling
        );

        // Background executor for I/O tasks (file reading, backup operations)
        this.backgroundExecutor = new ThreadPoolExecutor(
            Math.max(2, corePoolSize / 2),
            Math.max(4, maxPoolSize / 2),
            DEFAULT_KEEP_ALIVE_TIME,
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(DEFAULT_QUEUE_CAPACITY),
            backgroundThreadFactory,
            new ThreadPoolExecutor.CallerRunsPolicy()
        );

        // Scheduled executor for periodic tasks (cache cleanup, metrics)
        this.scheduledExecutor = Executors.newScheduledThreadPool(
            2,
            scheduledThreadFactory
        );

        logger.info("ExecutorManager initialized successfully");
    }

    /**
     * Submits an analysis task.
     */
    public <T> Future<T> submitAnalysisTask(Callable<T> task) {
        activeAnalysisTasks.incrementAndGet();
        return analysisExecutor.submit(() -> {
            try {
                return task.call();
            } finally {
                activeAnalysisTasks.decrementAndGet();
            }
        });
    }

    /**
     * Submits a background task.
     */
    public <T> Future<T> submitBackgroundTask(Callable<T> task) {
        return backgroundExecutor.submit(task);
    }

    /**
     * Submits a background task without return value.
     */
    public void submitBackgroundTask(Runnable task) {
        backgroundExecutor.submit(task);
    }

    /**
     * Schedules a task to run periodically.
     */
    public ScheduledFuture<?> scheduleAtFixedRate(Runnable task, long initialDelay, long period, TimeUnit unit) {
        return scheduledExecutor.scheduleAtFixedRate(task, initialDelay, period, unit);
    }

    /**
     * Gets the number of active analysis tasks.
     */
    public int getActiveAnalysisTaskCount() {
        return activeAnalysisTasks.get();
    }

    /**
     * Gets executor statistics.
     */
    public ExecutorStats getStats() {
        ThreadPoolExecutor analysisPool = (ThreadPoolExecutor) analysisExecutor;
        ThreadPoolExecutor backgroundPool = (ThreadPoolExecutor) backgroundExecutor;

        return new ExecutorStats(
            analysisPool.getActiveCount(),
            analysisPool.getPoolSize(),
            analysisPool.getQueue().size(),
            analysisPool.getCompletedTaskCount(),
            backgroundPool.getActiveCount(),
            backgroundPool.getPoolSize()
        );
    }

    /**
     * Initiates graceful shutdown.
     */
    public void shutdown() {
        logger.info("Shutting down ExecutorManager...");

        analysisExecutor.shutdown();
        backgroundExecutor.shutdown();
        scheduledExecutor.shutdown();

        try {
            if (!analysisExecutor.awaitTermination(30, TimeUnit.SECONDS)) {
                logger.warn("Analysis executor did not terminate in time, forcing shutdown");
                analysisExecutor.shutdownNow();
            }

            if (!backgroundExecutor.awaitTermination(30, TimeUnit.SECONDS)) {
                logger.warn("Background executor did not terminate in time, forcing shutdown");
                backgroundExecutor.shutdownNow();
            }

            if (!scheduledExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                logger.warn("Scheduled executor did not terminate in time, forcing shutdown");
                scheduledExecutor.shutdownNow();
            }

            logger.info("ExecutorManager shut down successfully");

        } catch (InterruptedException e) {
            logger.error("Interrupted during shutdown", e);
            Thread.currentThread().interrupt();

            analysisExecutor.shutdownNow();
            backgroundExecutor.shutdownNow();
            scheduledExecutor.shutdownNow();
        }
    }

    /**
     * Forces immediate shutdown.
     */
    public void shutdownNow() {
        logger.warn("Forcing immediate shutdown of ExecutorManager");

        analysisExecutor.shutdownNow();
        backgroundExecutor.shutdownNow();
        scheduledExecutor.shutdownNow();
    }

    /**
     * Named thread factory for better debugging.
     */
    private static class NamedThreadFactory implements ThreadFactory {
        private final AtomicInteger threadNumber = new AtomicInteger(1);
        private final String namePrefix;

        NamedThreadFactory(String namePrefix) {
            this.namePrefix = namePrefix;
        }

        @Override
        public Thread newThread(Runnable r) {
            Thread thread = new Thread(r, namePrefix + "-" + threadNumber.getAndIncrement());
            thread.setDaemon(false);
            thread.setPriority(Thread.NORM_PRIORITY);
            return thread;
        }
    }

    /**
     * Executor statistics.
     */
    public static class ExecutorStats {
        private final int analysisActiveCount;
        private final int analysisPoolSize;
        private final int analysisQueueSize;
        private final long analysisCompletedTasks;
        private final int backgroundActiveCount;
        private final int backgroundPoolSize;

        public ExecutorStats(int analysisActiveCount, int analysisPoolSize, int analysisQueueSize,
                           long analysisCompletedTasks, int backgroundActiveCount, int backgroundPoolSize) {
            this.analysisActiveCount = analysisActiveCount;
            this.analysisPoolSize = analysisPoolSize;
            this.analysisQueueSize = analysisQueueSize;
            this.analysisCompletedTasks = analysisCompletedTasks;
            this.backgroundActiveCount = backgroundActiveCount;
            this.backgroundPoolSize = backgroundPoolSize;
        }

        public int getAnalysisActiveCount() { return analysisActiveCount; }
        public int getAnalysisPoolSize() { return analysisPoolSize; }
        public int getAnalysisQueueSize() { return analysisQueueSize; }
        public long getAnalysisCompletedTasks() { return analysisCompletedTasks; }
        public int getBackgroundActiveCount() { return backgroundActiveCount; }
        public int getBackgroundPoolSize() { return backgroundPoolSize; }

        @Override
        public String toString() {
            return String.format("ExecutorStats[Analysis: %d/%d active, %d queued, %d completed | Background: %d/%d active]",
                               analysisActiveCount, analysisPoolSize, analysisQueueSize, analysisCompletedTasks,
                               backgroundActiveCount, backgroundPoolSize);
        }
    }
}

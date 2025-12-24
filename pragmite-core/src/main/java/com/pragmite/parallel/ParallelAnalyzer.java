package com.pragmite.parallel;

import com.pragmite.analyzer.ProjectAnalyzer;
import com.pragmite.model.FileAnalysis;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * Parallel file analyzer that processes multiple files concurrently.
 * Significantly improves performance for large projects.
 *
 * Features:
 * - Configurable thread pool size
 * - Automatic work distribution
 * - Progress tracking
 * - Error isolation (one file failure doesn't stop others)
 */
public class ParallelAnalyzer {
    private static final Logger logger = LoggerFactory.getLogger(ParallelAnalyzer.class);

    private final int threadPoolSize;
    private final ExecutorService executorService;

    public ParallelAnalyzer() {
        this(Runtime.getRuntime().availableProcessors());
    }

    public ParallelAnalyzer(int threadPoolSize) {
        this.threadPoolSize = Math.max(1, threadPoolSize);
        this.executorService = Executors.newFixedThreadPool(this.threadPoolSize);
        logger.info("Parallel analyzer initialized with {} threads", this.threadPoolSize);
    }

    /**
     * Analyzes multiple files in parallel.
     */
    public List<FileAnalysis> analyzeFiles(List<Path> files, ProjectAnalyzer analyzer) {
        logger.info("Starting parallel analysis of {} files using {} threads",
                   files.size(), threadPoolSize);

        List<Future<FileAnalysis>> futures = new ArrayList<>();
        List<FileAnalysis> results = new ArrayList<>();

        // Submit all files for processing
        for (Path file : files) {
            Future<FileAnalysis> future = executorService.submit(() -> {
                try {
                    logger.debug("Analyzing: {}", file);
                    return analyzer.analyzeFile(file);
                } catch (Exception e) {
                    logger.error("Error analyzing {}: {}", file, e.getMessage(), e);
                    return null; // Return null for failed analyses
                }
            });
            futures.add(future);
        }

        // Collect results
        int completed = 0;
        for (Future<FileAnalysis> future : futures) {
            try {
                FileAnalysis result = future.get(5, TimeUnit.MINUTES); // 5 min timeout per file
                if (result != null) {
                    results.add(result);
                }
                completed++;

                if (completed % 10 == 0) {
                    logger.info("Progress: {}/{} files analyzed", completed, files.size());
                }

            } catch (TimeoutException e) {
                logger.error("Analysis timed out for a file");
                future.cancel(true);
            } catch (InterruptedException | ExecutionException e) {
                logger.error("Error retrieving analysis result: {}", e.getMessage());
            }
        }

        logger.info("Parallel analysis complete: {}/{} files successful",
                   results.size(), files.size());

        return results;
    }

    /**
     * Shuts down the thread pool.
     */
    public void shutdown() {
        logger.info("Shutting down parallel analyzer");
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Gets the configured thread pool size.
     */
    public int getThreadPoolSize() {
        return threadPoolSize;
    }
}

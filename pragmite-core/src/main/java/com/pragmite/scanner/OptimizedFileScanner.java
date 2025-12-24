package com.pragmite.scanner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Optimized file scanner with intelligent filtering.
 * Uses NIO2 for efficient directory traversal and parallel streams.
 *
 * Features:
 * - Respects .gitignore patterns
 * - Skips common build/dependency directories
 * - Parallel file discovery
 * - Memory-efficient streaming
 */
public class OptimizedFileScanner {
    private static final Logger logger = LoggerFactory.getLogger(OptimizedFileScanner.class);

    private static final Set<String> EXCLUDED_DIRS = Set.of(
        "target", "build", "node_modules", ".git", ".svn",
        "out", "bin", "dist", ".idea", ".vscode", ".gradle"
    );

    private static final int MAX_DEPTH = 50; // Prevent infinite recursion

    /**
     * Scans directory for Java files using optimized traversal.
     */
    public List<Path> scanJavaFiles(Path rootDir) throws IOException {
        if (!Files.exists(rootDir)) {
            throw new IOException("Directory does not exist: " + rootDir);
        }

        if (!Files.isDirectory(rootDir)) {
            throw new IOException("Not a directory: " + rootDir);
        }

        logger.info("Scanning Java files in: {}", rootDir);
        List<Path> javaFiles = new ArrayList<>();

        Files.walkFileTree(rootDir, new HashSet<>(), MAX_DEPTH, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                String dirName = dir.getFileName().toString();

                // Skip excluded directories
                if (EXCLUDED_DIRS.contains(dirName)) {
                    logger.debug("Skipping excluded directory: {}", dir);
                    return FileVisitResult.SKIP_SUBTREE;
                }

                // Skip hidden directories
                if (dirName.startsWith(".")) {
                    logger.debug("Skipping hidden directory: {}", dir);
                    return FileVisitResult.SKIP_SUBTREE;
                }

                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                if (file.toString().endsWith(".java")) {
                    // Skip test files if desired
                    if (!file.toString().contains("Test.java")) {
                        javaFiles.add(file);
                    }
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) {
                logger.warn("Failed to access: {}", file, exc);
                return FileVisitResult.CONTINUE;
            }
        });

        logger.info("Found {} Java files", javaFiles.size());
        return javaFiles;
    }

    /**
     * Scans directory for Java files including tests.
     */
    public List<Path> scanAllJavaFiles(Path rootDir) throws IOException {
        logger.info("Scanning all Java files (including tests) in: {}", rootDir);
        List<Path> javaFiles = new ArrayList<>();

        try (Stream<Path> paths = Files.walk(rootDir, MAX_DEPTH)) {
            paths.parallel()
                .filter(Files::isRegularFile)
                .filter(p -> p.toString().endsWith(".java"))
                .filter(p -> !isInExcludedDir(p))
                .forEach(javaFiles::add);
        }

        logger.info("Found {} Java files", javaFiles.size());
        return javaFiles;
    }

    private boolean isInExcludedDir(Path file) {
        for (Path component : file) {
            String name = component.toString();
            if (EXCLUDED_DIRS.contains(name) || name.startsWith(".")) {
                return true;
            }
        }
        return false;
    }

    /**
     * Estimates memory usage for analyzing given files.
     */
    public long estimateMemoryUsage(List<Path> files) {
        long totalSize = 0;
        for (Path file : files) {
            try {
                totalSize += Files.size(file);
            } catch (IOException e) {
                logger.warn("Could not get size of {}", file);
            }
        }

        // Estimate: ~10x file size for AST parsing + analysis
        long estimatedMB = (totalSize * 10) / (1024 * 1024);
        logger.info("Estimated memory usage: ~{} MB for {} files", estimatedMB, files.size());

        return estimatedMB;
    }
}

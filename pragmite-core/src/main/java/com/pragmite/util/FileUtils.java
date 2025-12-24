package com.pragmite.util;

import com.pragmite.exception.ErrorCode;
import com.pragmite.exception.FileOperationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.function.Supplier;

/**
 * Utility class for file operations with automatic retry logic.
 * Handles transient failures like file locks, network issues, etc.
 */
public class FileUtils {
    private static final Logger logger = LoggerFactory.getLogger(FileUtils.class);

    private static final int DEFAULT_MAX_RETRIES = 3;
    private static final long DEFAULT_RETRY_DELAY_MS = 100;
    private static final double BACKOFF_MULTIPLIER = 2.0;

    /**
     * Reads file content with retry logic.
     */
    public static String readFileWithRetry(Path filePath) throws FileOperationException {
        return readFileWithRetry(filePath, DEFAULT_MAX_RETRIES);
    }

    /**
     * Reads file content with retry logic and custom retry count.
     */
    public static String readFileWithRetry(Path filePath, int maxRetries) throws FileOperationException {
        return retryOperation(
            () -> {
                try {
                    return Files.readString(filePath);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            },
            maxRetries,
            "read",
            filePath.toString()
        );
    }

    /**
     * Writes file content with retry logic.
     */
    public static void writeFileWithRetry(Path filePath, String content) throws FileOperationException {
        writeFileWithRetry(filePath, content, DEFAULT_MAX_RETRIES);
    }

    /**
     * Writes file content with retry logic and custom retry count.
     */
    public static void writeFileWithRetry(Path filePath, String content, int maxRetries) throws FileOperationException {
        retryOperation(
            () -> {
                try {
                    Files.writeString(filePath, content);
                    return null;
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            },
            maxRetries,
            "write",
            filePath.toString()
        );
    }

    /**
     * Copies file with retry logic.
     */
    public static void copyFileWithRetry(Path source, Path target) throws FileOperationException {
        copyFileWithRetry(source, target, DEFAULT_MAX_RETRIES);
    }

    /**
     * Copies file with retry logic and custom retry count.
     */
    public static void copyFileWithRetry(Path source, Path target, int maxRetries) throws FileOperationException {
        retryOperation(
            () -> {
                try {
                    Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
                    return null;
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            },
            maxRetries,
            "copy",
            source.toString() + " to " + target.toString()
        );
    }

    /**
     * Deletes file with retry logic.
     */
    public static void deleteFileWithRetry(Path filePath) throws FileOperationException {
        deleteFileWithRetry(filePath, DEFAULT_MAX_RETRIES);
    }

    /**
     * Deletes file with retry logic and custom retry count.
     */
    public static void deleteFileWithRetry(Path filePath, int maxRetries) throws FileOperationException {
        retryOperation(
            () -> {
                try {
                    Files.deleteIfExists(filePath);
                    return null;
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            },
            maxRetries,
            "delete",
            filePath.toString()
        );
    }

    /**
     * Generic retry operation with exponential backoff.
     */
    private static <T> T retryOperation(
            Supplier<T> operation,
            int maxRetries,
            String operationName,
            String targetPath) throws FileOperationException {

        int attempt = 0;
        long delayMs = DEFAULT_RETRY_DELAY_MS;

        while (true) {
            attempt++;
            try {
                return operation.get();
            } catch (RuntimeException e) {
                Throwable cause = e.getCause();

                // Check if we should retry
                boolean shouldRetry = attempt < maxRetries && isTransientError(cause);

                if (!shouldRetry) {
                    // Final failure - throw exception
                    logger.error("Failed to {} file '{}' after {} attempt(s)",
                               operationName, targetPath, attempt);

                    if (cause instanceof IOException) {
                        throw FileOperationException.from(targetPath, (IOException) cause);
                    } else {
                        throw new FileOperationException(
                            ErrorCode.FILE_IO_ERROR,
                            targetPath,
                            "Unexpected error: " + e.getMessage());
                    }
                }

                // Transient error - retry with backoff
                logger.warn("Attempt {}/{} to {} file '{}' failed: {}. Retrying in {}ms...",
                          attempt, maxRetries, operationName, targetPath,
                          cause.getMessage(), delayMs);

                try {
                    Thread.sleep(delayMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new FileOperationException(
                        ErrorCode.THREAD_INTERRUPTED,
                        targetPath,
                        "Operation interrupted during retry");
                }

                // Exponential backoff
                delayMs = (long) (delayMs * BACKOFF_MULTIPLIER);
            }
        }
    }

    /**
     * Determines if an error is transient and worth retrying.
     */
    private static boolean isTransientError(Throwable cause) {
        if (!(cause instanceof IOException)) {
            return false;
        }

        String message = cause.getMessage();
        if (message == null) {
            return false;
        }

        message = message.toLowerCase();

        // Common transient error patterns
        return message.contains("being used by another process") ||
               message.contains("locked") ||
               message.contains("temporary") ||
               message.contains("timeout") ||
               message.contains("network") ||
               message.contains("connection");
    }

    /**
     * Checks if a file exists with retry logic.
     */
    public static boolean existsWithRetry(Path filePath) {
        return existsWithRetry(filePath, DEFAULT_MAX_RETRIES);
    }

    /**
     * Checks if a file exists with retry logic and custom retry count.
     */
    public static boolean existsWithRetry(Path filePath, int maxRetries) {
        int attempt = 0;
        long delayMs = DEFAULT_RETRY_DELAY_MS;

        while (attempt < maxRetries) {
            attempt++;
            try {
                return Files.exists(filePath);
            } catch (Exception e) {
                logger.warn("Attempt {}/{} to check existence of '{}' failed: {}",
                          attempt, maxRetries, filePath, e.getMessage());

                if (attempt >= maxRetries) {
                    logger.error("Failed to check existence of '{}' after {} attempts",
                               filePath, maxRetries);
                    return false;
                }

                try {
                    Thread.sleep(delayMs);
                    delayMs = (long) (delayMs * BACKOFF_MULTIPLIER);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }
        }

        return false;
    }
}

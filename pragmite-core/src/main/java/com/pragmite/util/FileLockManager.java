package com.pragmite.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages file locks to prevent concurrent edits during refactoring.
 * Thread-safe implementation using file system locks.
 */
public class FileLockManager {
    private static final Logger logger = LoggerFactory.getLogger(FileLockManager.class);

    private final Map<String, LockInfo> locks = new ConcurrentHashMap<>();

    /**
     * Acquires an exclusive lock on a file.
     * Blocks until lock is available.
     *
     * @param filePath Path to the file to lock
     * @return Lock token that must be used to release the lock
     * @throws IOException if lock cannot be acquired
     */
    public String acquireLock(Path filePath) throws IOException {
        String absolutePath = filePath.toAbsolutePath().toString();

        // Check if already locked by this manager
        if (locks.containsKey(absolutePath)) {
            logger.warn("File already locked: {}", absolutePath);
            throw new IOException("File is already locked: " + absolutePath);
        }

        try {
            // Open file for reading and writing to get a FileChannel
            FileChannel channel = FileChannel.open(
                filePath,
                StandardOpenOption.READ,
                StandardOpenOption.WRITE
            );

            // Try to acquire exclusive lock
            FileLock lock = channel.lock();

            String lockToken = generateLockToken(absolutePath);
            locks.put(absolutePath, new LockInfo(lockToken, lock, channel));

            logger.debug("Acquired lock on file: {}", absolutePath);
            return lockToken;

        } catch (OverlappingFileLockException e) {
            logger.error("File is locked by another process: {}", absolutePath);
            throw new IOException("File is locked by another process: " + absolutePath, e);
        }
    }

    /**
     * Tries to acquire a lock without blocking.
     *
     * @param filePath Path to the file to lock
     * @return Lock token if successful, null if file is already locked
     * @throws IOException if an I/O error occurs
     */
    public String tryAcquireLock(Path filePath) throws IOException {
        String absolutePath = filePath.toAbsolutePath().toString();

        if (locks.containsKey(absolutePath)) {
            return null; // Already locked
        }

        try {
            FileChannel channel = FileChannel.open(
                filePath,
                StandardOpenOption.READ,
                StandardOpenOption.WRITE
            );

            // Try to acquire lock without blocking
            FileLock lock = channel.tryLock();

            if (lock == null) {
                channel.close();
                return null; // Could not acquire lock
            }

            String lockToken = generateLockToken(absolutePath);
            locks.put(absolutePath, new LockInfo(lockToken, lock, channel));

            logger.debug("Acquired lock on file: {}", absolutePath);
            return lockToken;

        } catch (OverlappingFileLockException e) {
            return null; // File is locked
        }
    }

    /**
     * Releases a previously acquired lock.
     *
     * @param filePath Path to the file
     * @param lockToken Token returned by acquireLock
     * @throws IOException if lock cannot be released
     */
    public void releaseLock(Path filePath, String lockToken) throws IOException {
        String absolutePath = filePath.toAbsolutePath().toString();

        LockInfo lockInfo = locks.get(absolutePath);

        if (lockInfo == null) {
            logger.warn("No lock found for file: {}", absolutePath);
            return;
        }

        if (!lockInfo.token.equals(lockToken)) {
            throw new IOException("Invalid lock token for file: " + absolutePath);
        }

        try {
            lockInfo.lock.release();
            lockInfo.channel.close();
            locks.remove(absolutePath);

            logger.debug("Released lock on file: {}", absolutePath);

        } catch (IOException e) {
            logger.error("Error releasing lock on file: {}", absolutePath, e);
            throw e;
        }
    }

    /**
     * Checks if a file is currently locked by this manager.
     */
    public boolean isLocked(Path filePath) {
        String absolutePath = filePath.toAbsolutePath().toString();
        return locks.containsKey(absolutePath);
    }

    /**
     * Releases all locks held by this manager.
     * Should be called on shutdown.
     */
    public void releaseAllLocks() {
        logger.info("Releasing all file locks ({} locks)", locks.size());

        for (Map.Entry<String, LockInfo> entry : locks.entrySet()) {
            try {
                entry.getValue().lock.release();
                entry.getValue().channel.close();
                logger.debug("Released lock on: {}", entry.getKey());
            } catch (IOException e) {
                logger.error("Error releasing lock on: {}", entry.getKey(), e);
            }
        }

        locks.clear();
    }

    /**
     * Gets the number of currently held locks.
     */
    public int getLockCount() {
        return locks.size();
    }

    /**
     * Generates a unique lock token.
     */
    private String generateLockToken(String filePath) {
        // Use UUID to ensure uniqueness even for rapid consecutive locks
        return "lock-" + UUID.randomUUID().toString();
    }

    /**
     * Information about a held lock.
     */
    private static class LockInfo {
        final String token;
        final FileLock lock;
        final FileChannel channel;

        LockInfo(String token, FileLock lock, FileChannel channel) {
            this.token = token;
            this.lock = lock;
            this.channel = channel;
        }
    }
}

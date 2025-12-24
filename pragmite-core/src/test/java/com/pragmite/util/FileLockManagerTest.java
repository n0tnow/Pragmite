package com.pragmite.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class FileLockManagerTest {

    @TempDir
    Path tempDir;

    private FileLockManager lockManager;
    private Path testFile;

    @BeforeEach
    void setUp() throws IOException {
        lockManager = new FileLockManager();
        testFile = tempDir.resolve("test.txt");
        Files.writeString(testFile, "test content");
    }

    @Test
    void testAcquireLock() throws IOException {
        String token = lockManager.acquireLock(testFile);

        assertNotNull(token);
        assertTrue(token.startsWith("lock-"));
        assertTrue(lockManager.isLocked(testFile));
        assertEquals(1, lockManager.getLockCount());

        lockManager.releaseLock(testFile, token);
    }

    @Test
    void testReleaseLock() throws IOException {
        String token = lockManager.acquireLock(testFile);
        assertTrue(lockManager.isLocked(testFile));

        lockManager.releaseLock(testFile, token);

        assertFalse(lockManager.isLocked(testFile));
        assertEquals(0, lockManager.getLockCount());
    }

    @Test
    void testCannotAcquireLockTwice() throws IOException {
        String token1 = lockManager.acquireLock(testFile);

        assertThrows(IOException.class, () -> {
            lockManager.acquireLock(testFile);
        });

        lockManager.releaseLock(testFile, token1);
    }

    @Test
    void testTryAcquireLock_Success() throws IOException {
        String token = lockManager.tryAcquireLock(testFile);

        assertNotNull(token);
        assertTrue(lockManager.isLocked(testFile));

        lockManager.releaseLock(testFile, token);
    }

    @Test
    void testTryAcquireLock_AlreadyLocked() throws IOException {
        String token1 = lockManager.acquireLock(testFile);

        String token2 = lockManager.tryAcquireLock(testFile);

        assertNull(token2);

        lockManager.releaseLock(testFile, token1);
    }

    @Test
    void testReleaseLock_InvalidToken() throws IOException {
        String token = lockManager.acquireLock(testFile);

        assertThrows(IOException.class, () -> {
            lockManager.releaseLock(testFile, "invalid-token");
        });

        lockManager.releaseLock(testFile, token);
    }

    @Test
    void testReleaseLock_NotLocked() throws IOException {
        // Should not throw, just warn
        lockManager.releaseLock(testFile, "any-token");

        assertFalse(lockManager.isLocked(testFile));
    }

    @Test
    void testMultipleFiles() throws IOException {
        Path file1 = tempDir.resolve("file1.txt");
        Path file2 = tempDir.resolve("file2.txt");
        Files.writeString(file1, "content1");
        Files.writeString(file2, "content2");

        String token1 = lockManager.acquireLock(file1);
        String token2 = lockManager.acquireLock(file2);

        assertTrue(lockManager.isLocked(file1));
        assertTrue(lockManager.isLocked(file2));
        assertEquals(2, lockManager.getLockCount());

        lockManager.releaseLock(file1, token1);
        lockManager.releaseLock(file2, token2);

        assertEquals(0, lockManager.getLockCount());
    }

    @Test
    void testReleaseAllLocks() throws IOException {
        Path file1 = tempDir.resolve("file1.txt");
        Path file2 = tempDir.resolve("file2.txt");
        Path file3 = tempDir.resolve("file3.txt");
        Files.writeString(file1, "content1");
        Files.writeString(file2, "content2");
        Files.writeString(file3, "content3");

        lockManager.acquireLock(file1);
        lockManager.acquireLock(file2);
        lockManager.acquireLock(file3);

        assertEquals(3, lockManager.getLockCount());

        lockManager.releaseAllLocks();

        assertEquals(0, lockManager.getLockCount());
        assertFalse(lockManager.isLocked(file1));
        assertFalse(lockManager.isLocked(file2));
        assertFalse(lockManager.isLocked(file3));
    }

    @Test
    void testIsLocked() throws IOException {
        assertFalse(lockManager.isLocked(testFile));

        String token = lockManager.acquireLock(testFile);
        assertTrue(lockManager.isLocked(testFile));

        lockManager.releaseLock(testFile, token);
        assertFalse(lockManager.isLocked(testFile));
    }

    @Test
    void testGetLockCount() throws IOException {
        assertEquals(0, lockManager.getLockCount());

        Path file1 = tempDir.resolve("file1.txt");
        Path file2 = tempDir.resolve("file2.txt");
        Files.writeString(file1, "content1");
        Files.writeString(file2, "content2");

        String token1 = lockManager.acquireLock(file1);
        assertEquals(1, lockManager.getLockCount());

        String token2 = lockManager.acquireLock(file2);
        assertEquals(2, lockManager.getLockCount());

        lockManager.releaseLock(file1, token1);
        assertEquals(1, lockManager.getLockCount());

        lockManager.releaseLock(file2, token2);
        assertEquals(0, lockManager.getLockCount());
    }

    @Test
    void testConcurrentLockAttempts() throws Exception {
        CountDownLatch latch = new CountDownLatch(2);
        AtomicBoolean thread1Success = new AtomicBoolean(false);
        AtomicBoolean thread2Success = new AtomicBoolean(false);

        Thread thread1 = new Thread(() -> {
            try {
                String token = lockManager.tryAcquireLock(testFile);
                if (token != null) {
                    thread1Success.set(true);
                    Thread.sleep(100);
                    lockManager.releaseLock(testFile, token);
                }
            } catch (Exception e) {
                // Expected in concurrent scenario
            } finally {
                latch.countDown();
            }
        });

        Thread thread2 = new Thread(() -> {
            try {
                String token = lockManager.tryAcquireLock(testFile);
                if (token != null) {
                    thread2Success.set(true);
                    Thread.sleep(100);
                    lockManager.releaseLock(testFile, token);
                }
            } catch (Exception e) {
                // Expected in concurrent scenario
            } finally {
                latch.countDown();
            }
        });

        thread1.start();
        thread2.start();

        assertTrue(latch.await(5, TimeUnit.SECONDS));

        // Only one thread should have acquired the lock
        assertTrue(thread1Success.get() ^ thread2Success.get(),
            "Exactly one thread should acquire the lock");
    }

    @Test
    void testLockTokenUniqueness() throws IOException {
        Path file1 = tempDir.resolve("file1.txt");
        Path file2 = tempDir.resolve("file2.txt");
        Files.writeString(file1, "content1");
        Files.writeString(file2, "content2");

        String token1 = lockManager.acquireLock(file1);
        String token2 = lockManager.acquireLock(file2);

        assertNotEquals(token1, token2);

        lockManager.releaseLock(file1, token1);
        lockManager.releaseLock(file2, token2);
    }

    @Test
    void testLockAfterRelease() throws IOException {
        String token1 = lockManager.acquireLock(testFile);
        lockManager.releaseLock(testFile, token1);

        // Should be able to lock again with UUID-based tokens
        String token2 = lockManager.acquireLock(testFile);
        assertNotNull(token2);
        assertNotEquals(token1, token2, "Tokens should be unique with UUID generation");

        lockManager.releaseLock(testFile, token2);
    }

    @Test
    void testReleaseAllLocksWithErrors() throws IOException {
        Path file1 = tempDir.resolve("file1.txt");
        Files.writeString(file1, "content1");

        lockManager.acquireLock(file1);

        // Delete the file to cause error during release
        Files.delete(file1);

        // Should not throw, just log error
        assertDoesNotThrow(() -> lockManager.releaseAllLocks());

        assertEquals(0, lockManager.getLockCount());
    }

    @Test
    void testAbsolutePathNormalization() throws IOException {
        Path relativeFile = tempDir.resolve("relative.txt");
        Files.writeString(relativeFile, "content");

        // Acquire lock with relative path
        String token1 = lockManager.acquireLock(relativeFile);
        assertNotNull(token1, "Should acquire lock successfully");

        // Try to lock the same file again (should fail - already locked)
        assertThrows(IOException.class, () -> {
            lockManager.acquireLock(relativeFile);
        }, "Should not be able to lock already locked file");

        // Try with absolute path (should also fail - same file)
        assertThrows(IOException.class, () -> {
            lockManager.acquireLock(relativeFile.toAbsolutePath());
        }, "Should not be able to lock same file via absolute path");

        // Release and verify we can lock again
        lockManager.releaseLock(relativeFile, token1);

        String token2 = lockManager.acquireLock(relativeFile.toAbsolutePath());
        assertNotNull(token2, "Should be able to lock after release");
        lockManager.releaseLock(relativeFile, token2);
    }
}

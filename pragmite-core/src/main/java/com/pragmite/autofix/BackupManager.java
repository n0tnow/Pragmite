package com.pragmite.autofix;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * Manages backups of source files before auto-apply modifications.
 *
 * Features:
 * - Creates timestamped backups in temp directory
 * - Calculates checksums for verification
 * - Supports restore from backup
 * - Automatic cleanup of old backups
 */
public class BackupManager {
    private static final Logger logger = LoggerFactory.getLogger(BackupManager.class);
    private static final int MAX_BACKUPS_PER_FILE = 10;

    private final Path backupDir;
    private final boolean enabled;

    public BackupManager() {
        this(true);
    }

    public BackupManager(boolean enabled) {
        this.enabled = enabled;
        this.backupDir = Paths.get(System.getProperty("java.io.tmpdir"), "pragmite-backups");

        if (enabled) {
            try {
                Files.createDirectories(backupDir);
                logger.info("Backup directory: {}", backupDir);
            } catch (IOException e) {
                logger.error("Failed to create backup directory", e);
            }
        }
    }

    /**
     * Create backup of file before modification.
     */
    public Backup createBackup(Path sourceFile) throws IOException {
        if (!enabled) {
            return createDummyBackup(sourceFile);
        }

        if (!Files.exists(sourceFile)) {
            throw new IOException("Source file does not exist: " + sourceFile);
        }

        // Generate backup filename with timestamp
        String timestamp = Instant.now().toString()
            .replace(":", "-")
            .replace(".", "-");

        String originalFileName = sourceFile.getFileName().toString();
        String backupFileName = originalFileName + ".backup." + timestamp;
        Path backupPath = backupDir.resolve(backupFileName);

        // Copy file to backup location
        Files.copy(sourceFile, backupPath, StandardCopyOption.REPLACE_EXISTING);

        // Calculate checksum for verification
        String checksum = calculateChecksum(sourceFile);
        long size = Files.size(sourceFile);

        Backup backup = new Backup(sourceFile, backupPath, Instant.now(), size, checksum);

        logger.info("Created backup: {} → {}", sourceFile.getFileName(), backupPath.getFileName());

        // Cleanup old backups
        cleanupOldBackups(originalFileName);

        return backup;
    }

    /**
     * Restore file from backup.
     */
    public void restore(Backup backup) throws IOException {
        if (!enabled) {
            logger.warn("Backup disabled, skipping restore");
            return;
        }

        if (!Files.exists(backup.getBackupPath())) {
            throw new IOException("Backup file does not exist: " + backup.getBackupPath());
        }

        // Restore from backup
        Files.copy(backup.getBackupPath(), backup.getOriginalPath(),
                   StandardCopyOption.REPLACE_EXISTING);

        logger.info("Restored {} from backup {}",
                   backup.getOriginalPath().getFileName(),
                   backup.getBackupPath().getFileName());

        // Verify checksum
        String currentChecksum = calculateChecksum(backup.getOriginalPath());
        if (!currentChecksum.equals(backup.getChecksum())) {
            logger.warn("Checksum mismatch after restore! Expected: {}, Got: {}",
                       backup.getChecksum(), currentChecksum);
        }
    }

    /**
     * Restore multiple files from backups.
     */
    public void restoreAll(List<Backup> backups) {
        int restored = 0;
        int failed = 0;

        for (Backup backup : backups) {
            try {
                restore(backup);
                restored++;
            } catch (IOException e) {
                logger.error("Failed to restore backup: {}", backup.getBackupPath(), e);
                failed++;
            }
        }

        logger.info("Restore complete: {} succeeded, {} failed", restored, failed);
    }

    /**
     * Clean up old backups, keeping only the last MAX_BACKUPS_PER_FILE.
     */
    public void cleanupOldBackups(String originalFileName) {
        if (!enabled) {
            return;
        }

        try {
            // Find all backups for this file
            String backupPrefix = originalFileName + ".backup.";
            List<Path> backups = new ArrayList<>();

            try (Stream<Path> paths = Files.list(backupDir)) {
                paths.filter(p -> p.getFileName().toString().startsWith(backupPrefix))
                     .forEach(backups::add);
            }

            // Sort by creation time (newest first)
            backups.sort(Comparator.<Path, FileTime>comparing(p -> {
                try {
                    return Files.getLastModifiedTime(p);
                } catch (IOException e) {
                    return FileTime.fromMillis(0);
                }
            }).reversed());

            // Delete old backups beyond MAX_BACKUPS_PER_FILE
            if (backups.size() > MAX_BACKUPS_PER_FILE) {
                for (int i = MAX_BACKUPS_PER_FILE; i < backups.size(); i++) {
                    Files.deleteIfExists(backups.get(i));
                    logger.debug("Deleted old backup: {}", backups.get(i).getFileName());
                }
            }

        } catch (IOException e) {
            logger.error("Failed to cleanup old backups", e);
        }
    }

    /**
     * Clean up all backups older than specified days.
     */
    public void cleanupOldBackups(int olderThanDays) {
        if (!enabled) {
            return;
        }

        try {
            Instant cutoff = Instant.now().minusSeconds(olderThanDays * 24L * 60 * 60);
            int deleted = 0;

            try (Stream<Path> paths = Files.list(backupDir)) {
                List<Path> oldBackups = paths
                    .filter(p -> {
                        try {
                            BasicFileAttributes attrs = Files.readAttributes(p, BasicFileAttributes.class);
                            return attrs.creationTime().toInstant().isBefore(cutoff);
                        } catch (IOException e) {
                            return false;
                        }
                    })
                    .toList();

                for (Path backup : oldBackups) {
                    Files.deleteIfExists(backup);
                    deleted++;
                }
            }

            logger.info("Cleaned up {} old backups (older than {} days)", deleted, olderThanDays);

        } catch (IOException e) {
            logger.error("Failed to cleanup old backups", e);
        }
    }

    /**
     * Calculate MD5 checksum of file for verification.
     */
    private String calculateChecksum(Path file) throws IOException {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] bytes = Files.readAllBytes(file);
            byte[] digest = md.digest(bytes);

            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();

        } catch (NoSuchAlgorithmException e) {
            // MD5 should always be available
            throw new IOException("MD5 algorithm not available", e);
        }
    }

    /**
     * Create a dummy backup when backup is disabled.
     */
    private Backup createDummyBackup(Path sourceFile) {
        return new Backup(sourceFile, sourceFile, Instant.now(), 0, "");
    }

    /**
     * Get backup directory path.
     */
    public Path getBackupDir() {
        return backupDir;
    }

    /**
     * Check if backups are enabled.
     */
    public boolean isEnabled() {
        return enabled;
    }
}

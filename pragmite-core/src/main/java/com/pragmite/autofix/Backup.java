package com.pragmite.autofix;

import java.nio.file.Path;
import java.time.Instant;

/**
 * Represents a backup of a source file before modification.
 * Used for rollback if auto-apply fails.
 */
public class Backup {
    private final Path originalPath;
    private final Path backupPath;
    private final Instant createdAt;
    private final long originalSize;
    private final String checksum;

    public Backup(Path originalPath, Path backupPath, Instant createdAt,
                  long originalSize, String checksum) {
        this.originalPath = originalPath;
        this.backupPath = backupPath;
        this.createdAt = createdAt;
        this.originalSize = originalSize;
        this.checksum = checksum;
    }

    public Path getOriginalPath() {
        return originalPath;
    }

    public Path getBackupPath() {
        return backupPath;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public long getOriginalSize() {
        return originalSize;
    }

    public String getChecksum() {
        return checksum;
    }

    @Override
    public String toString() {
        return "Backup{" +
               "original=" + originalPath +
               ", backup=" + backupPath +
               ", created=" + createdAt +
               '}';
    }
}

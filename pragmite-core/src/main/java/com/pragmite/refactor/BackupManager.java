package com.pragmite.refactor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages file backups for safe refactoring rollback.
 * Creates timestamped backups before applying refactorings.
 */
public class BackupManager {
    private static final Logger logger = LoggerFactory.getLogger(BackupManager.class);

    private static final String BACKUP_DIR = ".pragmite/backups";
    private static final DateTimeFormatter TIMESTAMP_FORMAT =
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH-mm-ss-SSS");

    private final Path backupRoot;

    public BackupManager() {
        this.backupRoot = Paths.get(BACKUP_DIR);
    }

    public BackupManager(Path backupRoot) {
        this.backupRoot = backupRoot;
    }

    /**
     * Creates a backup for a single file.
     * @param filePath Path to the file to backup
     * @return Backup ID that can be used for restore
     */
    public String createBackup(String filePath) throws IOException {
        String backupId = generateBackupId();
        Path backupDir = backupRoot.resolve(backupId);
        Files.createDirectories(backupDir);

        Path sourceFile = Paths.get(filePath);
        if (!Files.exists(sourceFile)) {
            throw new IOException("File not found: " + filePath);
        }

        Path relativePath = getRelativePath(sourceFile);
        Path backupFile = backupDir.resolve(relativePath);
        Files.createDirectories(backupFile.getParent());
        Files.copy(sourceFile, backupFile, StandardCopyOption.REPLACE_EXISTING);

        // Save path mapping for restore
        savePathMapping(backupDir, backupFile, sourceFile);

        logger.info("Backup created: {} for file: {}", backupId, filePath);
        return backupId;
    }

    /**
     * Creates a backup for all files affected by the refactoring plan.
     * @return Backup ID that can be used for restore
     */
    public String createBackup(RefactoringPlan plan) throws IOException {
        String backupId = generateBackupId();
        Path backupDir = backupRoot.resolve(backupId);
        Files.createDirectories(backupDir);

        List<String> affectedFiles = plan.getAffectedFiles();
        logger.info("Creating backup for {} files", affectedFiles.size());

        for (String filePath : affectedFiles) {
            Path sourceFile = Paths.get(filePath);
            if (!Files.exists(sourceFile)) {
                logger.warn("File not found for backup: {}", filePath);
                continue;
            }

            // Preserve directory structure in backup
            Path relativePath = getRelativePath(sourceFile);
            Path backupFile = backupDir.resolve(relativePath);

            // Create parent directories
            Files.createDirectories(backupFile.getParent());

            // Copy file
            Files.copy(sourceFile, backupFile, StandardCopyOption.REPLACE_EXISTING);

            // Save path mapping for restore
            savePathMapping(backupDir, backupFile, sourceFile);

            logger.debug("Backed up: {} -> {}", sourceFile, backupFile);
        }

        // Save backup metadata
        saveBackupMetadata(backupDir, plan);

        logger.info("Backup created: {}", backupId);
        return backupId;
    }

    /**
     * Restores files from a backup.
     */
    public void restore(String backupId) throws IOException {
        Path backupDir = backupRoot.resolve(backupId);

        if (!Files.exists(backupDir)) {
            throw new IOException("Backup not found: " + backupId);
        }

        logger.info("Restoring from backup: {}", backupId);

        // Read path mappings from metadata
        Path metadataFile = backupDir.resolve("backup-paths.txt");
        if (!Files.exists(metadataFile)) {
            // Fallback to old behavior if metadata doesn't exist
            restoreLegacy(backupDir);
            return;
        }

        // Read path mappings
        List<String> pathMappings = Files.readAllLines(metadataFile);
        for (String mapping : pathMappings) {
            if (mapping.trim().isEmpty()) continue;

            String[] parts = mapping.split(" -> ", 2);
            if (parts.length != 2) continue;

            Path backupFile = Paths.get(parts[0]);
            Path targetFile = Paths.get(parts[1]);

            if (Files.exists(backupFile)) {
                Files.createDirectories(targetFile.getParent());
                Files.copy(backupFile, targetFile, StandardCopyOption.REPLACE_EXISTING);
                logger.debug("Restored: {} -> {}", backupFile, targetFile);
            }
        }

        logger.info("Restore complete: {}", backupId);
    }

    /**
     * Legacy restore without path metadata (for backwards compatibility).
     */
    private void restoreLegacy(Path backupDir) throws IOException {
        Files.walkFileTree(backupDir, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (file.getFileName().toString().equals("backup-metadata.txt") ||
                    file.getFileName().toString().equals("backup-paths.txt")) {
                    return FileVisitResult.CONTINUE;
                }

                Path relativePath = backupDir.relativize(file);
                Path targetFile = getAbsolutePath(relativePath);

                Files.createDirectories(targetFile.getParent());
                Files.copy(file, targetFile, StandardCopyOption.REPLACE_EXISTING);
                logger.debug("Restored: {} -> {}", file, targetFile);

                return FileVisitResult.CONTINUE;
            }
        });
    }

    /**
     * Lists all available backups.
     */
    public List<String> listBackups() throws IOException {
        List<String> backups = new ArrayList<>();

        if (!Files.exists(backupRoot)) {
            return backups;
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(backupRoot)) {
            for (Path entry : stream) {
                if (Files.isDirectory(entry)) {
                    backups.add(entry.getFileName().toString());
                }
            }
        }

        backups.sort((a, b) -> b.compareTo(a)); // Sort descending (newest first)
        return backups;
    }

    /**
     * Deletes a backup.
     */
    public void deleteBackup(String backupId) throws IOException {
        Path backupDir = backupRoot.resolve(backupId);

        if (!Files.exists(backupDir)) {
            throw new IOException("Backup not found: " + backupId);
        }

        // Delete directory recursively
        Files.walkFileTree(backupDir, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });

        logger.info("Deleted backup: {}", backupId);
    }

    /**
     * Deletes backups older than the specified count.
     */
    public void cleanupOldBackups(int keepCount) throws IOException {
        List<String> backups = listBackups();

        if (backups.size() <= keepCount) {
            return;
        }

        int deleteCount = backups.size() - keepCount;
        for (int i = backups.size() - 1; i >= backups.size() - deleteCount; i--) {
            deleteBackup(backups.get(i));
        }

        logger.info("Cleaned up {} old backups", deleteCount);
    }

    /**
     * Generates a unique backup ID based on timestamp.
     */
    private String generateBackupId() {
        return "backup-" + LocalDateTime.now().format(TIMESTAMP_FORMAT);
    }

    /**
     * Gets relative path from absolute path (removes drive/root).
     */
    private Path getRelativePath(Path absolutePath) {
        // For simplicity, use the last 3 path components
        int nameCount = absolutePath.getNameCount();
        if (nameCount > 3) {
            return absolutePath.subpath(nameCount - 3, nameCount);
        }
        return absolutePath.getFileName();
    }

    /**
     * Converts relative path back to absolute (best effort).
     */
    private Path getAbsolutePath(Path relativePath) {
        // This is a simplified implementation
        // In production, you'd want to store the original absolute paths in metadata
        return Paths.get(System.getProperty("user.dir")).resolve(relativePath);
    }

    /**
     * Saves backup metadata for auditing.
     */
    private void saveBackupMetadata(Path backupDir, RefactoringPlan plan) throws IOException {
        Path metadataFile = backupDir.resolve("backup-metadata.txt");
        StringBuilder metadata = new StringBuilder();

        metadata.append("Backup created: ").append(LocalDateTime.now()).append("\n");
        metadata.append("Refactoring plan: ").append(plan).append("\n");
        metadata.append("Affected files:\n");

        for (String file : plan.getAffectedFiles()) {
            metadata.append("  - ").append(file).append("\n");
        }

        Files.writeString(metadataFile, metadata.toString());
    }

    /**
     * Saves path mapping for restore operations.
     */
    private void savePathMapping(Path backupDir, Path backupFile, Path sourceFile) throws IOException {
        Path pathsFile = backupDir.resolve("backup-paths.txt");

        String mapping = backupFile.toString() + " -> " + sourceFile.toString() + "\n";

        // Append to file
        if (Files.exists(pathsFile)) {
            Files.writeString(pathsFile, Files.readString(pathsFile) + mapping);
        } else {
            Files.writeString(pathsFile, mapping);
        }
    }
}

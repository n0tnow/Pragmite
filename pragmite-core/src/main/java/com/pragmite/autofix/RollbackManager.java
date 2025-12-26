package com.pragmite.autofix;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Rollback Manager for Pragmite v1.3.0 + v1.5.0
 * Manages rollback of auto-fix operations using:
 * - Database-tracked backups (v1.3.0 - existing auto-fix)
 * - File-based backups (v1.5.0 - new auto-apply)
 */
public class RollbackManager {

    private static final Logger logger = LoggerFactory.getLogger(RollbackManager.class);

    private final Connection connection;
    private final BackupManager backupManager;

    public RollbackManager(Connection connection) {
        this(connection, null);
    }

    public RollbackManager(Connection connection, BackupManager backupManager) {
        this.connection = connection;
        this.backupManager = backupManager;
    }

    // ============================================================================
    // v1.5.0 File-Based Rollback Methods (for auto-apply feature)
    // ============================================================================

    /**
     * List all file-based backups for a specific file (v1.5.0).
     */
    public List<FileBackupInfo> listFileBackups(String fileName) throws IOException {
        if (backupManager == null || !backupManager.isEnabled()) {
            throw new IllegalStateException("BackupManager not available. File-based rollback requires backup manager.");
        }

        List<FileBackupInfo> backups = new ArrayList<>();
        Path backupDir = backupManager.getBackupDir();

        if (!Files.exists(backupDir)) {
            return backups;
        }

        String backupPrefix = fileName + ".backup.";

        try (java.util.stream.Stream<Path> paths = Files.list(backupDir)) {
            paths.filter(p -> p.getFileName().toString().startsWith(backupPrefix))
                 .forEach(backupPath -> {
                     try {
                         FileBackupInfo info = createFileBackupInfo(backupPath, fileName);
                         backups.add(info);
                     } catch (IOException e) {
                         logger.warn("Failed to read backup info: {}", backupPath, e);
                     }
                 });
        }

        // Sort by creation time (newest first)
        backups.sort(java.util.Comparator.comparing(FileBackupInfo::getCreatedAt).reversed());

        return backups;
    }

    /**
     * Rollback a file to a specific backup (v1.5.0).
     */
    public FileRollbackResult rollbackToFileBackup(Path targetFile, Path backupPath) {
        if (backupManager == null) {
            return FileRollbackResult.failed("BackupManager not available");
        }

        try {
            if (!Files.exists(backupPath)) {
                return FileRollbackResult.failed("Backup file does not exist: " + backupPath);
            }

            if (!Files.exists(targetFile)) {
                return FileRollbackResult.failed("Target file does not exist: " + targetFile);
            }

            // Create a safety backup of current state
            Backup safetyBackup = backupManager.createBackup(targetFile);

            // Restore from backup
            Files.copy(backupPath, targetFile, StandardCopyOption.REPLACE_EXISTING);

            logger.info("Rolled back {} from backup {}",
                       targetFile.getFileName(), backupPath.getFileName());

            return FileRollbackResult.success(targetFile, backupPath, safetyBackup);

        } catch (IOException e) {
            logger.error("Rollback failed: {}", targetFile, e);
            return FileRollbackResult.failed("Rollback failed: " + e.getMessage());
        }
    }

    /**
     * Rollback to the most recent file backup (v1.5.0).
     */
    public FileRollbackResult rollbackToLatestFileBackup(Path targetFile) throws IOException {
        String fileName = targetFile.getFileName().toString();
        List<FileBackupInfo> backups = listFileBackups(fileName);

        if (backups.isEmpty()) {
            return FileRollbackResult.failed("No backups found for: " + fileName);
        }

        FileBackupInfo latest = backups.get(0);
        return rollbackToFileBackup(targetFile, latest.getBackupPath());
    }

    private FileBackupInfo createFileBackupInfo(Path backupPath, String originalFileName) throws IOException {
        java.nio.file.attribute.BasicFileAttributes attrs =
            Files.readAttributes(backupPath, java.nio.file.attribute.BasicFileAttributes.class);
        long size = Files.size(backupPath);

        return new FileBackupInfo(
            originalFileName,
            backupPath,
            attrs.creationTime().toInstant(),
            size
        );
    }

    // ============================================================================
    // v1.3.0 Database-Based Rollback Methods (existing auto-fix)
    // ============================================================================

    /**
     * Rollback the last fix operation.
     */
    public RollbackResult rollbackLast() throws SQLException, IOException {
        logger.info("Rolling back last fix operation...");

        // Get the last successful fix operation
        String sql = """
            SELECT id FROM fix_operations
            WHERE status IN ('SUCCESS', 'PARTIAL')
              AND id NOT IN (SELECT DISTINCT operation_id FROM individual_fixes WHERE status = 'ROLLED_BACK')
            ORDER BY started_at DESC
            LIMIT 1
            """;

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (!rs.next()) {
                logger.warn("No fix operations available to rollback");
                return RollbackResult.noOperationsFound();
            }

            long operationId = rs.getLong("id");
            return rollback(operationId);
        }
    }

    /**
     * Rollback a specific fix operation by ID.
     */
    public RollbackResult rollback(long fixOperationId) throws SQLException, IOException {
        logger.info("Rolling back fix operation {}", fixOperationId);

        RollbackResult result = new RollbackResult();
        result.setFixOperationId(fixOperationId);

        // Get all file backups for this operation
        String sql = """
            SELECT id, file_path, original_content, is_restored
            FROM file_backups
            WHERE fix_operation_id = ?
            ORDER BY created_at
            """;

        List<Long> backupIds = new ArrayList<>();

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setLong(1, fixOperationId);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    long backupId = rs.getLong("id");
                    String filePath = rs.getString("file_path");
                    byte[] originalContent = rs.getBytes("original_content");
                    boolean isRestored = rs.getBoolean("is_restored");

                    result.incrementTotal();
                    backupIds.add(backupId);

                    if (isRestored) {
                        logger.info("Backup {} already restored, skipping", backupId);
                        result.incrementSkipped();
                        continue;
                    }

                    try {
                        // Restore original content
                        Path file = Paths.get(filePath);
                        Files.write(file, originalContent);

                        result.incrementRestored();
                        result.addRestoredFile(filePath);

                        logger.info("Restored file: {}", filePath);

                        // Mark backup as restored
                        markBackupRestored(backupId);

                    } catch (IOException e) {
                        result.incrementFailed();
                        result.addError("Failed to restore " + filePath + ": " + e.getMessage());
                        logger.error("Failed to restore {}: {}", filePath, e.getMessage());
                    }
                }
            }
        }

        if (result.getTotalBackups() == 0) {
            logger.warn("No backups found for fix operation {}", fixOperationId);
            result.setSuccess(false);
            return result;
        }

        // Mark individual fixes as rolled back
        markFixesRolledBack(fixOperationId);

        // Update fix operation status
        updateFixOperationStatus(fixOperationId);

        result.setSuccess(result.getFailedCount() == 0);
        logger.info("Rollback completed: {}", result);

        return result;
    }

    /**
     * Rollback all fixes for a specific file.
     */
    public RollbackResult rollbackFile(String filePath) throws SQLException, IOException {
        logger.info("Rolling back all fixes for file: {}", filePath);

        RollbackResult result = new RollbackResult();

        // Get all fix operations that modified this file
        String sql = """
            SELECT DISTINCT fb.fix_operation_id, fb.id as backup_id,
                   fb.original_content, fb.is_restored
            FROM file_backups fb
            WHERE fb.file_path = ?
              AND fb.is_restored = 0
            ORDER BY fb.created_at DESC
            """;

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, filePath);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (!rs.next()) {
                    logger.warn("No backups found for file: {}", filePath);
                    return RollbackResult.noOperationsFound();
                }

                // Get the most recent backup (first row after DESC sort)
                long backupId = rs.getLong("backup_id");
                byte[] originalContent = rs.getBytes("original_content");

                result.incrementTotal();

                try {
                    // Restore original content
                    Path file = Paths.get(filePath);
                    Files.write(file, originalContent);

                    result.incrementRestored();
                    result.addRestoredFile(filePath);

                    logger.info("Restored file: {}", filePath);

                    // Mark all backups for this file as restored
                    markFileBackupsRestored(filePath);
                    result.setSuccess(true);

                } catch (IOException e) {
                    result.incrementFailed();
                    result.addError("Failed to restore " + filePath + ": " + e.getMessage());
                    result.setSuccess(false);
                    logger.error("Failed to restore {}: {}", filePath, e.getMessage());
                }
            }
        }

        return result;
    }

    /**
     * Get list of rollbackable fix operations.
     */
    public List<RollbackableOperation> getRollbackableOperations() throws SQLException {
        String sql = """
            SELECT
                fo.id,
                fo.started_at,
                fo.fix_type,
                fo.total_fixes_succeeded,
                fo.status,
                COUNT(fb.id) as backup_count,
                SUM(CASE WHEN fb.is_restored = 0 THEN 1 ELSE 0 END) as unrestored_count
            FROM fix_operations fo
            LEFT JOIN file_backups fb ON fo.id = fb.fix_operation_id
            WHERE fo.status IN ('SUCCESS', 'PARTIAL')
            GROUP BY fo.id
            HAVING unrestored_count > 0
            ORDER BY fo.started_at DESC
            """;

        List<RollbackableOperation> operations = new ArrayList<>();

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                RollbackableOperation op = new RollbackableOperation();
                op.setId(rs.getLong("id"));
                op.setStartedAt(rs.getTimestamp("started_at").toLocalDateTime());
                op.setFixType(rs.getString("fix_type"));
                op.setSuccessCount(rs.getInt("total_fixes_succeeded"));
                op.setStatus(rs.getString("status"));
                op.setBackupCount(rs.getInt("backup_count"));
                op.setUnrestoredCount(rs.getInt("unrestored_count"));
                operations.add(op);
            }
        }

        return operations;
    }

    /**
     * Mark a backup as restored.
     */
    private void markBackupRestored(long backupId) throws SQLException {
        String sql = "UPDATE file_backups SET is_restored = 1, restored_at = CURRENT_TIMESTAMP WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setLong(1, backupId);
            pstmt.executeUpdate();
        }
    }

    /**
     * Mark all backups for a file as restored.
     */
    private void markFileBackupsRestored(String filePath) throws SQLException {
        String sql = "UPDATE file_backups SET is_restored = 1, restored_at = CURRENT_TIMESTAMP WHERE file_path = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, filePath);
            pstmt.executeUpdate();
        }
    }

    /**
     * Mark individual fixes as rolled back.
     */
    private void markFixesRolledBack(long fixOperationId) throws SQLException {
        String sql = """
            UPDATE individual_fixes
            SET status = 'ROLLED_BACK', rolled_back_at = CURRENT_TIMESTAMP
            WHERE operation_id = ? AND status != 'ROLLED_BACK'
            """;

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setLong(1, fixOperationId);
            int updated = pstmt.executeUpdate();
            logger.info("Marked {} individual fixes as rolled back", updated);
        }
    }

    /**
     * Update fix operation status after rollback.
     */
    private void updateFixOperationStatus(long fixOperationId) throws SQLException {
        String sql = "UPDATE fix_operations SET status = 'ROLLED_BACK' WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setLong(1, fixOperationId);
            pstmt.executeUpdate();
        }
    }

    /**
     * Rollback result model.
     */
    public static class RollbackResult {
        private long fixOperationId = -1;
        private int totalBackups = 0;
        private int restoredCount = 0;
        private int failedCount = 0;
        private int skippedCount = 0;
        private boolean success = false;
        private List<String> restoredFiles = new ArrayList<>();
        private List<String> errors = new ArrayList<>();

        public void incrementTotal() { totalBackups++; }
        public void incrementRestored() { restoredCount++; }
        public void incrementFailed() { failedCount++; }
        public void incrementSkipped() { skippedCount++; }

        public void addRestoredFile(String file) { restoredFiles.add(file); }
        public void addError(String error) { errors.add(error); }

        public static RollbackResult noOperationsFound() {
            RollbackResult result = new RollbackResult();
            result.setSuccess(false);
            result.addError("No fix operations available to rollback");
            return result;
        }

        // Getters and setters
        public long getFixOperationId() { return fixOperationId; }
        public void setFixOperationId(long id) { this.fixOperationId = id; }

        public int getTotalBackups() { return totalBackups; }
        public int getRestoredCount() { return restoredCount; }
        public int getFailedCount() { return failedCount; }
        public int getSkippedCount() { return skippedCount; }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public List<String> getRestoredFiles() { return restoredFiles; }
        public List<String> getErrors() { return errors; }

        @Override
        public String toString() {
            return String.format("RollbackResult{operation=%d, total=%d, restored=%d, failed=%d, skipped=%d}",
                    fixOperationId, totalBackups, restoredCount, failedCount, skippedCount);
        }
    }

    /**
     * Rollbackable operation model.
     */
    public static class RollbackableOperation {
        private long id;
        private java.time.LocalDateTime startedAt;
        private String fixType;
        private int successCount;
        private String status;
        private int backupCount;
        private int unrestoredCount;

        // Getters and setters
        public long getId() { return id; }
        public void setId(long id) { this.id = id; }

        public java.time.LocalDateTime getStartedAt() { return startedAt; }
        public void setStartedAt(java.time.LocalDateTime startedAt) { this.startedAt = startedAt; }

        public String getFixType() { return fixType; }
        public void setFixType(String fixType) { this.fixType = fixType; }

        public int getSuccessCount() { return successCount; }
        public void setSuccessCount(int successCount) { this.successCount = successCount; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public int getBackupCount() { return backupCount; }
        public void setBackupCount(int backupCount) { this.backupCount = backupCount; }

        public int getUnrestoredCount() { return unrestoredCount; }
        public void setUnrestoredCount(int unrestoredCount) { this.unrestoredCount = unrestoredCount; }
    }

    // ============================================================================
    // v1.5.0 File-Based Rollback Models
    // ============================================================================

    /**
     * Information about a file-based backup (v1.5.0).
     */
    public static class FileBackupInfo {
        private final String originalFileName;
        private final Path backupPath;
        private final java.time.Instant createdAt;
        private final long size;

        public FileBackupInfo(String originalFileName, Path backupPath, java.time.Instant createdAt, long size) {
            this.originalFileName = originalFileName;
            this.backupPath = backupPath;
            this.createdAt = createdAt;
            this.size = size;
        }

        public String getOriginalFileName() {
            return originalFileName;
        }

        public Path getBackupPath() {
            return backupPath;
        }

        public java.time.Instant getCreatedAt() {
            return createdAt;
        }

        public long getSize() {
            return size;
        }

        public String getFormattedTimestamp() {
            return java.time.format.DateTimeFormatter
                .ofPattern("yyyy-MM-dd HH:mm:ss")
                .withZone(java.time.ZoneId.systemDefault())
                .format(createdAt);
        }

        public String getFormattedSize() {
            if (size < 1024) {
                return size + " B";
            } else if (size < 1024 * 1024) {
                return String.format("%.1f KB", size / 1024.0);
            } else {
                return String.format("%.1f MB", size / (1024.0 * 1024.0));
            }
        }

        @Override
        public String toString() {
            return String.format("%s | %s | %s | %s",
                originalFileName,
                getFormattedTimestamp(),
                getFormattedSize(),
                backupPath.getFileName());
        }
    }

    /**
     * Result of a file-based rollback operation (v1.5.0).
     */
    public static class FileRollbackResult {
        private final boolean success;
        private final Path targetFile;
        private final Path backupPath;
        private final Backup safetyBackup;
        private final String errorMessage;

        private FileRollbackResult(boolean success, Path targetFile, Path backupPath,
                                  Backup safetyBackup, String errorMessage) {
            this.success = success;
            this.targetFile = targetFile;
            this.backupPath = backupPath;
            this.safetyBackup = safetyBackup;
            this.errorMessage = errorMessage;
        }

        public static FileRollbackResult success(Path targetFile, Path backupPath, Backup safetyBackup) {
            return new FileRollbackResult(true, targetFile, backupPath, safetyBackup, null);
        }

        public static FileRollbackResult failed(String errorMessage) {
            return new FileRollbackResult(false, null, null, null, errorMessage);
        }

        public boolean isSuccess() {
            return success;
        }

        public Path getTargetFile() {
            return targetFile;
        }

        public Path getBackupPath() {
            return backupPath;
        }

        public Backup getSafetyBackup() {
            return safetyBackup;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        @Override
        public String toString() {
            if (success) {
                return String.format("FileRollbackResult{success=true, file=%s, backup=%s}",
                    targetFile.getFileName(), backupPath.getFileName());
            } else {
                return String.format("FileRollbackResult{success=false, error=%s}", errorMessage);
            }
        }
    }
}

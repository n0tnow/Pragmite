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
 * Rollback Manager for Pragmite v1.3.0
 * Manages rollback of auto-fix operations using database-tracked backups.
 */
public class RollbackManager {

    private static final Logger logger = LoggerFactory.getLogger(RollbackManager.class);

    private final Connection connection;

    public RollbackManager(Connection connection) {
        this.connection = connection;
    }

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
}

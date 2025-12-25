package com.pragmite.autofix;

import java.util.ArrayList;
import java.util.List;

/**
 * Result of an auto-fix operation.
 */
public class FixResult {

    private int totalAttempted = 0;
    private int successCount = 0;
    private int failureCount = 0;
    private int skippedCount = 0;

    private List<FixOperation> operations = new ArrayList<>();
    private List<String> errors = new ArrayList<>();
    private long fixOperationId = -1;

    public void incrementAttempted() {
        totalAttempted++;
    }

    public void incrementSuccess() {
        successCount++;
    }

    public void incrementFailure() {
        failureCount++;
    }

    public void incrementSkipped() {
        skippedCount++;
    }

    public void addOperation(FixOperation operation) {
        operations.add(operation);
    }

    public void addError(String error) {
        errors.add(error);
    }

    public boolean isSuccess() {
        return failureCount == 0 && successCount > 0;
    }

    public boolean isPartialSuccess() {
        return successCount > 0 && failureCount > 0;
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    // Getters
    public int getTotalAttempted() {
        return totalAttempted;
    }

    public int getSuccessCount() {
        return successCount;
    }

    public int getFailureCount() {
        return failureCount;
    }

    public int getSkippedCount() {
        return skippedCount;
    }

    public List<FixOperation> getOperations() {
        return operations;
    }

    public List<String> getErrors() {
        return errors;
    }

    public long getFixOperationId() {
        return fixOperationId;
    }

    public void setFixOperationId(long fixOperationId) {
        this.fixOperationId = fixOperationId;
    }

    @Override
    public String toString() {
        return String.format("FixResult{attempted=%d, success=%d, failed=%d, skipped=%d}",
                totalAttempted, successCount, failureCount, skippedCount);
    }

    /**
     * Individual fix operation record.
     */
    public static class FixOperation {
        private String filePath;
        private String smellType;
        private String fixAction;
        private boolean success;
        private String errorMessage;

        public FixOperation(String filePath, String smellType, String fixAction) {
            this.filePath = filePath;
            this.smellType = smellType;
            this.fixAction = fixAction;
        }

        // Getters and setters
        public String getFilePath() {
            return filePath;
        }

        public String getSmellType() {
            return smellType;
        }

        public String getFixAction() {
            return fixAction;
        }

        public boolean isSuccess() {
            return success;
        }

        public void setSuccess(boolean success) {
            this.success = success;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public void setErrorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
        }

        @Override
        public String toString() {
            return String.format("%s: %s in %s - %s",
                    success ? "SUCCESS" : "FAILED",
                    fixAction, filePath, smellType);
        }
    }
}

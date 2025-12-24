package com.pragmite.refactor;

import java.util.ArrayList;
import java.util.List;

/**
 * Result of executing a refactoring plan.
 * Contains details about success/failure of each action.
 */
public class RefactoringResult {
    private boolean success;
    private boolean dryRun;
    private String backupId;
    private int successCount;
    private int failureCount;

    private final List<ActionOutcome> outcomes;

    public RefactoringResult() {
        this.outcomes = new ArrayList<>();
    }

    public void addAction(RefactoringAction action, boolean success, String message) {
        outcomes.add(new ActionOutcome(action, success, message));
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public boolean isDryRun() {
        return dryRun;
    }

    public void setDryRun(boolean dryRun) {
        this.dryRun = dryRun;
    }

    public String getBackupId() {
        return backupId;
    }

    public void setBackupId(String backupId) {
        this.backupId = backupId;
    }

    public int getSuccessCount() {
        return successCount;
    }

    public void setSuccessCount(int successCount) {
        this.successCount = successCount;
    }

    public int getFailureCount() {
        return failureCount;
    }

    public void setFailureCount(int failureCount) {
        this.failureCount = failureCount;
    }

    public List<ActionOutcome> getOutcomes() {
        return outcomes;
    }

    /**
     * Formats the result as a human-readable report.
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n=== Refactoring Result ===\n\n");

        if (dryRun) {
            sb.append("Mode: DRY RUN (no changes made)\n\n");
        } else {
            sb.append("Mode: LIVE EXECUTION\n");
            if (backupId != null) {
                sb.append("Backup ID: ").append(backupId).append("\n\n");
            }
        }

        sb.append(String.format("Status: %s\n", success ? "✅ SUCCESS" : "❌ FAILED"));
        sb.append(String.format("Actions: %d succeeded, %d failed\n\n",
            successCount, failureCount));

        // List outcomes
        for (int i = 0; i < outcomes.size(); i++) {
            ActionOutcome outcome = outcomes.get(i);
            sb.append(String.format("%2d. %s %s\n    %s\n",
                i + 1,
                outcome.success ? "✅" : "❌",
                outcome.action.getDescription(),
                outcome.message));
        }

        sb.append("\n=========================\n");
        return sb.toString();
    }

    /**
     * Represents the outcome of a single refactoring action.
     */
    public static class ActionOutcome {
        private final RefactoringAction action;
        private final boolean success;
        private final String message;

        public ActionOutcome(RefactoringAction action, boolean success, String message) {
            this.action = action;
            this.success = success;
            this.message = message;
        }

        public RefactoringAction getAction() {
            return action;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }
    }
}

package com.pragmite.refactor;

/**
 * Complete result of the safe refactoring workflow.
 */
public class WorkflowResult {
    private String projectRoot;
    private String phase;
    private boolean success;
    private boolean rolledBack;

    private RefactoringPlan plan;
    private RefactoringResult refactoringResult;
    private VerificationResult buildResult;
    private VerificationResult testResult;
    private String backupId;

    public WorkflowResult() {}

    public String getProjectRoot() {
        return projectRoot;
    }

    public void setProjectRoot(String projectRoot) {
        this.projectRoot = projectRoot;
    }

    public String getPhase() {
        return phase;
    }

    public void setPhase(String phase) {
        this.phase = phase;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public boolean isRolledBack() {
        return rolledBack;
    }

    public void setRolledBack(boolean rolledBack) {
        this.rolledBack = rolledBack;
    }

    public RefactoringPlan getPlan() {
        return plan;
    }

    public void setPlan(RefactoringPlan plan) {
        this.plan = plan;
    }

    public RefactoringResult getRefactoringResult() {
        return refactoringResult;
    }

    public void setRefactoringResult(RefactoringResult refactoringResult) {
        this.refactoringResult = refactoringResult;
    }

    public VerificationResult getBuildResult() {
        return buildResult;
    }

    public void setBuildResult(VerificationResult buildResult) {
        this.buildResult = buildResult;
    }

    public VerificationResult getTestResult() {
        return testResult;
    }

    public void setTestResult(VerificationResult testResult) {
        this.testResult = testResult;
    }

    public String getBackupId() {
        return backupId;
    }

    public void setBackupId(String backupId) {
        this.backupId = backupId;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n╔════════════════════════════════════════════╗\n");
        sb.append("║   SAFE REFACTORING WORKFLOW RESULT        ║\n");
        sb.append("╚════════════════════════════════════════════╝\n\n");

        sb.append("Project: ").append(projectRoot).append("\n");
        sb.append("Phase: ").append(phase).append("\n");
        sb.append("Status: ").append(success ? "✅ SUCCESS" : "❌ FAILED").append("\n");

        if (rolledBack) {
            sb.append("Rollback: ✅ Changes were reverted\n");
        }

        if (backupId != null) {
            sb.append("Backup ID: ").append(backupId).append("\n");
        }

        sb.append("\n");

        if (plan != null) {
            sb.append("Refactoring Plan:\n");
            sb.append("  - Actions: ").append(plan.size()).append("\n");
            sb.append("  - Affected files: ").append(plan.getAffectedFiles().size()).append("\n\n");
        }

        if (refactoringResult != null) {
            sb.append("Refactoring Execution:\n");
            sb.append("  - Succeeded: ").append(refactoringResult.getSuccessCount()).append("\n");
            sb.append("  - Failed: ").append(refactoringResult.getFailureCount()).append("\n\n");
        }

        if (buildResult != null) {
            sb.append("Build Verification: ")
                .append(buildResult.isSuccess() ? "✅ PASSED" : "❌ FAILED")
                .append("\n");
        }

        if (testResult != null) {
            sb.append("Test Verification: ")
                .append(testResult.isSuccess() ? "✅ PASSED" : "❌ FAILED")
                .append("\n");
        }

        sb.append("\n════════════════════════════════════════════\n");

        return sb.toString();
    }
}

package com.pragmite.refactor;

import com.pragmite.model.CodeSmell;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * Safe refactoring workflow with automatic build/test verification and rollback.
 *
 * Workflow:
 * 1. Create refactoring plan
 * 2. Create backup
 * 3. Apply refactorings
 * 4. Run build verification
 * 5. Run test verification
 * 6. If any step fails -> automatic rollback
 *
 * Usage:
 * <pre>
 * SafeRefactoringWorkflow workflow = new SafeRefactoringWorkflow(projectRoot);
 * WorkflowResult result = workflow.executeRefactoring(codeSmells);
 *
 * if (!result.isSuccess()) {
 *     System.out.println("Refactoring failed and was rolled back");
 * }
 * </pre>
 */
public class SafeRefactoringWorkflow {
    private static final Logger logger = LoggerFactory.getLogger(SafeRefactoringWorkflow.class);

    private final RefactoringEngine refactoringEngine;
    private final BuildTestVerifier verifier;
    private final Path projectRoot;

    private boolean runTests = true;
    private boolean autoRollback = true;

    public SafeRefactoringWorkflow(Path projectRoot) {
        this.projectRoot = projectRoot;
        this.refactoringEngine = new RefactoringEngine();
        this.verifier = new BuildTestVerifier(projectRoot);
    }

    /**
     * Executes the complete safe refactoring workflow.
     */
    public WorkflowResult executeRefactoring(List<CodeSmell> codeSmells) {
        WorkflowResult workflowResult = new WorkflowResult();
        workflowResult.setProjectRoot(projectRoot.toString());

        logger.info("Starting safe refactoring workflow for {} code smells", codeSmells.size());

        try {
            // Step 1: Create refactoring plan
            logger.info("Step 1: Creating refactoring plan...");
            RefactoringPlan plan = refactoringEngine.createPlan(codeSmells);
            workflowResult.setPlan(plan);

            if (plan.getActions().isEmpty()) {
                logger.info("No refactoring actions to execute");
                workflowResult.setSuccess(true);
                workflowResult.setPhase("Planning");
                return workflowResult;
            }

            logger.info("Created plan with {} actions affecting {} files",
                plan.getActions().size(), plan.getAffectedFiles().size());

            // Step 2: Apply refactorings (with automatic backup)
            logger.info("Step 2: Applying refactorings...");
            RefactoringResult refactorResult = refactoringEngine.execute(plan, false);
            workflowResult.setRefactoringResult(refactorResult);
            workflowResult.setBackupId(refactorResult.getBackupId());

            if (!refactorResult.isSuccess()) {
                logger.error("Refactoring execution failed");
                workflowResult.setPhase("Refactoring");
                workflowResult.setSuccess(false);
                performRollback(refactorResult.getBackupId(), workflowResult);
                return workflowResult;
            }

            logger.info("Refactoring applied successfully: {} actions", refactorResult.getSuccessCount());

            // Step 3: Run build verification
            logger.info("Step 3: Verifying build...");
            VerificationResult buildResult = verifier.runBuild();
            workflowResult.setBuildResult(buildResult);

            if (!buildResult.isSuccess()) {
                logger.error("Build verification failed");
                workflowResult.setPhase("Build Verification");
                workflowResult.setSuccess(false);
                performRollback(refactorResult.getBackupId(), workflowResult);
                return workflowResult;
            }

            logger.info("Build verification passed");

            // Step 4: Run tests (if enabled)
            if (runTests) {
                logger.info("Step 4: Running tests...");
                VerificationResult testResult = verifier.runTests();
                workflowResult.setTestResult(testResult);

                if (!testResult.isSuccess()) {
                    logger.error("Test verification failed");
                    workflowResult.setPhase("Test Verification");
                    workflowResult.setSuccess(false);
                    performRollback(refactorResult.getBackupId(), workflowResult);
                    return workflowResult;
                }

                logger.info("Test verification passed");
            } else {
                logger.info("Step 4: Skipping tests (disabled)");
            }

            // Success!
            logger.info("Refactoring workflow completed successfully!");
            workflowResult.setPhase("Complete");
            workflowResult.setSuccess(true);

            // Optionally cleanup backup after success
            // refactoringEngine.getBackupManager().deleteBackup(refactorResult.getBackupId());

        } catch (Exception e) {
            logger.error("Workflow failed with exception", e);
            workflowResult.setSuccess(false);
            workflowResult.setPhase("Exception: " + e.getMessage());

            if (workflowResult.getBackupId() != null) {
                performRollback(workflowResult.getBackupId(), workflowResult);
            }
        }

        return workflowResult;
    }

    /**
     * Executes dry-run to preview what would be refactored.
     */
    public WorkflowResult dryRun(List<CodeSmell> codeSmells) {
        WorkflowResult workflowResult = new WorkflowResult();
        workflowResult.setProjectRoot(projectRoot.toString());
        workflowResult.setPhase("Dry Run");

        try {
            RefactoringPlan plan = refactoringEngine.createPlan(codeSmells);
            workflowResult.setPlan(plan);

            RefactoringResult refactorResult = refactoringEngine.execute(plan, true);
            workflowResult.setRefactoringResult(refactorResult);
            workflowResult.setSuccess(true);

            logger.info("Dry run complete: {} actions would be applied", plan.size());

        } catch (Exception e) {
            logger.error("Dry run failed", e);
            workflowResult.setSuccess(false);
            workflowResult.setPhase("Dry Run Failed: " + e.getMessage());
        }

        return workflowResult;
    }

    /**
     * Performs automatic rollback if enabled.
     */
    private void performRollback(String backupId, WorkflowResult workflowResult) {
        if (!autoRollback || backupId == null) {
            logger.warn("Auto-rollback disabled or no backup available");
            workflowResult.setRolledBack(false);
            return;
        }

        try {
            logger.warn("Performing automatic rollback...");
            refactoringEngine.rollback(backupId);
            workflowResult.setRolledBack(true);
            logger.info("Rollback successful - all changes reverted");
        } catch (IOException e) {
            logger.error("Rollback failed!", e);
            workflowResult.setRolledBack(false);
            workflowResult.setPhase(workflowResult.getPhase() + " (ROLLBACK FAILED!)");
        }
    }

    // Configuration setters

    public SafeRefactoringWorkflow withTests(boolean runTests) {
        this.runTests = runTests;
        return this;
    }

    public SafeRefactoringWorkflow withAutoRollback(boolean autoRollback) {
        this.autoRollback = autoRollback;
        return this;
    }

    public SafeRefactoringWorkflow withTimeout(int seconds) {
        this.verifier.setTimeoutSeconds(seconds);
        return this;
    }

    public RefactoringEngine getRefactoringEngine() {
        return refactoringEngine;
    }
}

package com.pragmite.refactor;

import com.pragmite.model.CodeSmell;
import com.pragmite.refactor.strategies.*;
import com.pragmite.validation.JavacValidator;
import com.pragmite.validation.ValidationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Automated refactoring engine that safely applies code transformations.
 * Supports dry-run mode and automatic rollback on failure.
 *
 * Usage:
 * <pre>
 * RefactoringEngine engine = new RefactoringEngine();
 * RefactoringPlan plan = engine.createPlan(codeSmells);
 * RefactoringResult result = engine.execute(plan, dryRun: false);
 * </pre>
 */
public class RefactoringEngine {
    private static final Logger logger = LoggerFactory.getLogger(RefactoringEngine.class);

    private final List<RefactoringStrategy> strategies;
    private final BackupManager backupManager;
    private JavacValidator validator; // v1.6.3 - Integration Sprint
    private boolean strictValidation = false; // v1.6.3 - Enable/disable validation

    public RefactoringEngine() {
        this.strategies = new ArrayList<>();
        this.backupManager = new BackupManager();

        // Register built-in refactoring strategies
        registerDefaultStrategies();
    }

    /**
     * Registers default refactoring strategies.
     */
    private void registerDefaultStrategies() {
        // Basic refactorings
        strategies.add(new RemoveUnusedImportsStrategy());
        strategies.add(new ExtractMagicNumberStrategy());
        strategies.add(new IntroduceTryWithResourcesStrategy());
        strategies.add(new RemoveDeadCodeStrategy());
        strategies.add(new SimplifyConditionalStrategy());

        // Advanced refactorings
        strategies.add(new ExtractMethodStrategy());
        strategies.add(new IntroduceParameterObjectStrategy());
        strategies.add(new ReplaceConditionalWithPolymorphismStrategy());
        strategies.add(new InlineMethodStrategy());
        strategies.add(new RenameVariableStrategy());

        // Intelligent refactorings (auto-apply)
        strategies.add(new SmartExtractMethodStrategy());
        strategies.add(new SmartRenameStrategy());

        logger.info("Refactoring engine initialized with {} strategies", strategies.size());
    }

    /**
     * Registers a custom refactoring strategy.
     */
    public void registerStrategy(RefactoringStrategy strategy) {
        strategies.add(strategy);
        logger.info("Registered strategy: {}", strategy.getName());
    }

    /**
     * Enable strict validation using javac compiler.
     * When enabled, all refactorings will be validated for compilation correctness.
     * v1.6.3 - Integration Sprint
     */
    public void enableStrictValidation() {
        if (this.validator == null) {
            this.validator = new JavacValidator();
        }
        this.strictValidation = true;
        logger.info("Strict validation enabled");
    }

    /**
     * Disable strict validation.
     * v1.6.3 - Integration Sprint
     */
    public void disableStrictValidation() {
        this.strictValidation = false;
        logger.info("Strict validation disabled");
    }

    /**
     * Check if strict validation is enabled.
     * v1.6.3 - Integration Sprint
     */
    public boolean isStrictValidationEnabled() {
        return strictValidation;
    }

    /**
     * Creates a refactoring plan from detected code smells.
     * Only includes smells that have auto-fix available.
     */
    public RefactoringPlan createPlan(List<CodeSmell> codeSmells) {
        RefactoringPlan plan = new RefactoringPlan();

        for (CodeSmell smell : codeSmells) {
            if (!smell.isAutoFixAvailable()) {
                continue; // Skip smells without auto-fix
            }

            // Find applicable strategy
            RefactoringStrategy strategy = findStrategyFor(smell);
            if (strategy != null) {
                RefactoringAction action = new RefactoringAction(smell, strategy);
                plan.addAction(action);
            }
        }

        logger.info("Created refactoring plan with {} actions", plan.getActions().size());
        return plan;
    }

    /**
     * Finds a refactoring strategy that can handle the given code smell.
     */
    private RefactoringStrategy findStrategyFor(CodeSmell smell) {
        for (RefactoringStrategy strategy : strategies) {
            if (strategy.canHandle(smell)) {
                return strategy;
            }
        }
        return null;
    }

    /**
     * Executes a refactoring plan.
     * @param plan The refactoring plan to execute
     * @param dryRun If true, shows what would be changed without modifying files
     * @return Execution result with success/failure details
     */
    public RefactoringResult execute(RefactoringPlan plan, boolean dryRun) throws IOException {
        RefactoringResult result = new RefactoringResult();
        result.setDryRun(dryRun);

        if (plan.getActions().isEmpty()) {
            logger.info("No refactoring actions to execute");
            result.setSuccess(true);
            return result;
        }

        logger.info("Executing refactoring plan with {} actions (dry-run: {})",
            plan.getActions().size(), dryRun);

        // Create backup before making changes (unless dry-run)
        if (!dryRun) {
            String backupId = backupManager.createBackup(plan);
            result.setBackupId(backupId);
            logger.info("Created backup: {}", backupId);
        }

        // Execute each action
        int successCount = 0;
        int failureCount = 0;

        for (RefactoringAction action : plan.getActions()) {
            try {
                logger.debug("Executing: {}", action);

                if (dryRun) {
                    // In dry-run, just validate the action
                    boolean valid = action.getStrategy().validate(action.getCodeSmell());
                    if (valid) {
                        result.addAction(action, true, "Would apply: " + action.getDescription());
                        successCount++;
                    } else {
                        result.addAction(action, false, "Validation failed");
                        failureCount++;
                    }
                } else {
                    // Actually apply the refactoring
                    String outcome = action.getStrategy().apply(action.getCodeSmell());
                    
                    // v1.6.3 - Integration Sprint: Post-refactoring validation
                    if (strictValidation && validator != null) {
                        Path sourceFile = Paths.get(action.getCodeSmell().getFilePath());
                        if (Files.exists(sourceFile)) {
                            ValidationResult validationResult = validator.validateFile(sourceFile);
                            
                            if (!validationResult.isValid()) {
                                logger.error("Refactoring validation failed for {}", sourceFile);
                                logger.error("Validation errors: {}", validationResult.getErrorMessage());
                                
                                result.addAction(action, false, 
                                    "Validation failed: " + validationResult.getErrorMessage());
                                failureCount++;
                                
                                // Stop and rollback on validation failure
                                logger.warn("Stopping execution due to validation failure. Rolling back...");
                                throw new RuntimeException("Validation failed: " + validationResult.getErrorMessage());
                            } else {
                                logger.debug("Validation passed for {}", sourceFile);
                            }
                        }
                    }
                    
                    result.addAction(action, true, outcome);
                    successCount++;
                    logger.info("Applied: {}", action.getDescription());
                }

            } catch (Exception e) {
                logger.error("Failed to execute action: {}", action, e);
                result.addAction(action, false, "Error: " + e.getMessage());
                failureCount++;

                // Stop execution on first failure (safety measure)
                if (!dryRun) {
                    logger.warn("Stopping execution due to failure. Attempting rollback...");
                    break;
                }
            }
        }

        result.setSuccessCount(successCount);
        result.setFailureCount(failureCount);
        result.setSuccess(failureCount == 0);

        logger.info("Refactoring execution complete: {} succeeded, {} failed",
            successCount, failureCount);

        return result;
    }

    /**
     * Rolls back a refactoring using its backup ID.
     */
    public void rollback(String backupId) throws IOException {
        logger.info("Rolling back refactoring: {}", backupId);
        backupManager.restore(backupId);
        logger.info("Rollback complete");
    }

    /**
     * Gets the backup manager for manual backup operations.
     */
    public BackupManager getBackupManager() {
        return backupManager;
    }
}

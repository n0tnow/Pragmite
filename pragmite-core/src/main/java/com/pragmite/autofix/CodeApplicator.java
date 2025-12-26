package com.pragmite.autofix;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.pragmite.ai.RefactoredCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

/**
 * Main application engine for auto-apply functionality.
 *
 * Orchestrates the entire process:
 * 1. Create backup
 * 2. Parse original and refactored code to AST
 * 3. Apply refactoring using ASTReplacer
 * 4. Write modified code back to file
 * 5. Validate compilation
 * 6. Rollback on failure
 *
 * Version: v0.6
 */
public class CodeApplicator {
    private static final Logger logger = LoggerFactory.getLogger(CodeApplicator.class);

    private final BackupManager backupManager;
    private final CompilationValidator validator;
    private final ASTReplacer astReplacer;
    private final JavaParser javaParser;
    private final boolean dryRun;

    /**
     * Create CodeApplicator with default settings.
     */
    public CodeApplicator() {
        this(false, true);
    }

    /**
     * Create CodeApplicator with custom settings.
     *
     * @param dryRun If true, simulate changes without applying
     * @param enableBackup If true, create backups before modifications
     */
    public CodeApplicator(boolean dryRun, boolean enableBackup) {
        this.backupManager = new BackupManager(enableBackup);
        this.validator = new CompilationValidator(false); // Use fast JavaParser validation
        this.astReplacer = new ASTReplacer();
        this.javaParser = new JavaParser();
        this.dryRun = dryRun;

        logger.info("CodeApplicator initialized (dryRun={}, backup={})", dryRun, enableBackup);
    }

    /**
     * Apply refactored code to source file.
     *
     * @param refactored The refactored code to apply
     * @param sourceFile The source file to modify
     * @return ApplicationResult with success/failure status
     */
    public ApplicationResult apply(RefactoredCode refactored, Path sourceFile) {
        Instant startTime = Instant.now();
        logger.info("Applying refactoring to: {}", sourceFile);

        // Validate inputs
        if (refactored == null || !refactored.isSuccessful()) {
            return ApplicationResult.failed("Refactored code is null or unsuccessful");
        }

        if (!Files.exists(sourceFile)) {
            return ApplicationResult.failed("Source file does not exist: " + sourceFile);
        }

        // DRY RUN: Show what would be changed without applying
        if (dryRun) {
            return performDryRun(refactored, sourceFile);
        }

        Backup backup = null;

        try {
            // Step 1: Create backup
            backup = backupManager.createBackup(sourceFile);
            logger.debug("Backup created: {}", backup.getBackupPath());

            // Step 2: Read original file
            String originalContent = Files.readString(sourceFile);
            long originalLines = originalContent.lines().count();

            // Step 3: Parse original code to AST
            ParseResult<CompilationUnit> originalParseResult = javaParser.parse(sourceFile);
            if (!originalParseResult.isSuccessful()) {
                return ApplicationResult.failed("Failed to parse original file",
                    originalParseResult.getProblems().stream()
                        .map(p -> p.getMessage())
                        .toList());
            }

            CompilationUnit originalAST = originalParseResult.getResult().get();

            // Step 4: Parse refactored code to AST
            String refactoredCodeStr = refactored.getRefactoredCode();
            ParseResult<CompilationUnit> refactoredParseResult = javaParser.parse(refactoredCodeStr);

            if (!refactoredParseResult.isSuccessful()) {
                return ApplicationResult.failed("Failed to parse refactored code",
                    refactoredParseResult.getProblems().stream()
                        .map(p -> p.getMessage())
                        .toList());
            }

            CompilationUnit refactoredAST = refactoredParseResult.getResult().get();

            // Step 5: Replace code using AST
            boolean replaced = astReplacer.replace(originalAST, refactoredAST, refactored);

            if (!replaced) {
                logger.warn("AST replacement failed, attempting fallback");
                // Fallback: Replace entire file content (dangerous but better than nothing)
                return applyFallback(refactored, sourceFile, backup, startTime);
            }

            // Step 6: Write modified code back to file
            String modifiedContent = originalAST.toString();
            Files.writeString(sourceFile, modifiedContent);
            logger.debug("Modified code written to file");

            // Step 7: Validate compilation
            CompilationResult compilationResult = validator.validate(sourceFile);

            if (!compilationResult.isSuccess()) {
                logger.error("Compilation failed after applying refactoring");

                // Rollback on compilation failure
                if (backup != null) {
                    backupManager.restore(backup);
                    logger.info("Rolled back changes due to compilation failure");
                }

                return ApplicationResult.compilationFailed(sourceFile, backup, compilationResult);
            }

            // Step 8: Calculate metrics
            long modifiedLines = modifiedContent.lines().count();
            ApplicationMetrics metrics = ApplicationMetrics.builder()
                .startTime(startTime)
                .endTime(Instant.now())
                .linesChanged(Math.abs(modifiedLines - originalLines))
                .linesAdded(Math.max(0, modifiedLines - originalLines))
                .linesRemoved(Math.max(0, originalLines - modifiedLines))
                .compilationSucceeded(true)
                .build();

            logger.info("Successfully applied refactoring: {}", metrics);

            // Return success with warnings if any
            if (compilationResult.hasWarnings()) {
                return ApplicationResult.successWithWarnings(sourceFile, backup, metrics, compilationResult);
            }

            return ApplicationResult.success(sourceFile, backup, metrics);

        } catch (IOException e) {
            logger.error("IO error during code application", e);

            // Attempt rollback on IO error
            if (backup != null) {
                try {
                    backupManager.restore(backup);
                    return ApplicationResult.rolledBack(sourceFile, backup,
                        "IO error: " + e.getMessage());
                } catch (IOException restoreError) {
                    logger.error("Failed to restore backup after IO error", restoreError);
                }
            }

            return ApplicationResult.failed("IO error: " + e.getMessage());

        } catch (Exception e) {
            logger.error("Unexpected error during code application", e);

            // Attempt rollback on unexpected error
            if (backup != null) {
                try {
                    backupManager.restore(backup);
                    return ApplicationResult.rolledBack(sourceFile, backup,
                        "Unexpected error: " + e.getMessage());
                } catch (IOException restoreError) {
                    logger.error("Failed to restore backup after unexpected error", restoreError);
                }
            }

            return ApplicationResult.failed("Unexpected error: " + e.getMessage());
        }
    }

    /**
     * Perform dry run - show what would be changed without applying.
     */
    private ApplicationResult performDryRun(RefactoredCode refactored, Path sourceFile) {
        logger.info("DRY RUN: Would apply refactoring to: {}", sourceFile);

        try {
            String originalContent = Files.readString(sourceFile);
            String refactoredContent = refactored.getRefactoredCode();

            long originalLines = originalContent.lines().count();
            long refactoredLines = refactoredContent.lines().count();

            ApplicationMetrics metrics = ApplicationMetrics.builder()
                .startTime(Instant.now())
                .endTime(Instant.now())
                .linesChanged(Math.abs(refactoredLines - originalLines))
                .linesAdded(Math.max(0, refactoredLines - originalLines))
                .linesRemoved(Math.max(0, originalLines - refactoredLines))
                .compilationSucceeded(false) // Not validated in dry run
                .build();

            logger.info("DRY RUN: Would change {} lines", metrics.getLinesChanged());

            return ApplicationResult.success(sourceFile, null, metrics);

        } catch (IOException e) {
            logger.error("Failed to perform dry run", e);
            return ApplicationResult.failed("Dry run failed: " + e.getMessage());
        }
    }

    /**
     * Fallback strategy: Replace entire file content.
     * Used when AST replacement fails.
     */
    private ApplicationResult applyFallback(RefactoredCode refactored, Path sourceFile,
                                           Backup backup, Instant startTime) {
        logger.warn("Using fallback strategy: replacing entire file content");

        try {
            String originalContent = Files.readString(sourceFile);
            long originalLines = originalContent.lines().count();

            // Replace entire file with refactored code
            String refactoredContent = refactored.getRefactoredCode();
            Files.writeString(sourceFile, refactoredContent);

            // Validate compilation
            CompilationResult compilationResult = validator.validate(sourceFile);

            if (!compilationResult.isSuccess()) {
                logger.error("Fallback: Compilation failed");

                // Rollback
                if (backup != null) {
                    backupManager.restore(backup);
                }

                return ApplicationResult.compilationFailed(sourceFile, backup, compilationResult);
            }

            // Calculate metrics
            long modifiedLines = refactoredContent.lines().count();
            ApplicationMetrics metrics = ApplicationMetrics.builder()
                .startTime(startTime)
                .endTime(Instant.now())
                .linesChanged(Math.abs(modifiedLines - originalLines))
                .linesAdded(Math.max(0, modifiedLines - originalLines))
                .linesRemoved(Math.max(0, originalLines - modifiedLines))
                .compilationSucceeded(true)
                .build();

            logger.info("Fallback successful: {}", metrics);

            return ApplicationResult.success(sourceFile, backup, metrics);

        } catch (IOException e) {
            logger.error("Fallback strategy failed", e);

            // Attempt rollback
            if (backup != null) {
                try {
                    backupManager.restore(backup);
                    return ApplicationResult.rolledBack(sourceFile, backup,
                        "Fallback failed: " + e.getMessage());
                } catch (IOException restoreError) {
                    logger.error("Failed to restore backup after fallback failure", restoreError);
                }
            }

            return ApplicationResult.failed("Fallback failed: " + e.getMessage());
        }
    }

    /**
     * Apply multiple refactorings in batch.
     *
     * @param refactorings Map of source files to refactored code
     * @return List of application results
     */
    public java.util.List<ApplicationResult> applyBatch(java.util.Map<Path, RefactoredCode> refactorings) {
        logger.info("Applying batch refactoring to {} files", refactorings.size());

        java.util.List<ApplicationResult> results = new java.util.ArrayList<>();

        for (java.util.Map.Entry<Path, RefactoredCode> entry : refactorings.entrySet()) {
            Path sourceFile = entry.getKey();
            RefactoredCode refactored = entry.getValue();

            ApplicationResult result = apply(refactored, sourceFile);
            results.add(result);

            // Stop on first failure if rollback is enabled
            if (!result.isSuccess() && !dryRun) {
                logger.warn("Batch operation stopped due to failure: {}", sourceFile);
                // Could implement "rollback all" here if needed
                break;
            }
        }

        return results;
    }

    /**
     * Check if dry run mode is enabled.
     */
    public boolean isDryRun() {
        return dryRun;
    }

    /**
     * Get backup directory path.
     */
    public Path getBackupDir() {
        return backupManager.getBackupDir();
    }
}

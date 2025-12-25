package com.pragmite.autofix;

import com.pragmite.model.CodeSmell;
import com.pragmite.model.CodeSmellType;
import com.pragmite.refactor.RefactoringStrategy;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Auto-Fix Engine for Pragmite v1.3.0
 * Automatically applies fixes to detected code smells with backup and rollback support.
 *
 * Note: This is the framework for auto-fix. Actual fixer implementations will be added in future versions.
 */
public class AutoFixEngine {

    private final Map<CodeSmellType, RefactoringStrategy> fixers = new HashMap<>();
    private final BackupManager backupManager;

    public AutoFixEngine() {
        this.backupManager = new BackupManager();
        initializeFixers();
    }

    /**
     * Initialize available fixers.
     * TODO: Add actual fixer implementations in future versions.
     */
    private void initializeFixers() {
        // Fixers will be registered here as they are implemented
        // For v1.3.0, we're setting up the infrastructure
    }

    /**
     * Apply fixes to a list of code smells.
     */
    public FixResult applyFixes(List<CodeSmell> smells, FixOptions options) {
        FixResult result = new FixResult();

        // Filter smells based on options
        List<CodeSmell> fixableSmells = smells.stream()
                .filter(CodeSmell::isAutoFixAvailable)
                .filter(smell -> options.isTypeAllowed(smell.getType()))
                .filter(smell -> matchesFilePattern(smell.getFilePath(), options.getFilePatternFilter()))
                .collect(Collectors.toList());

        if (fixableSmells.isEmpty()) {
            return result;
        }

        // Group smells by file for batch processing
        Map<String, List<CodeSmell>> smellsByFile = fixableSmells.stream()
                .collect(Collectors.groupingBy(CodeSmell::getFilePath));

        // Process each file
        for (Map.Entry<String, List<CodeSmell>> entry : smellsByFile.entrySet()) {
            String filePath = entry.getKey();
            List<CodeSmell> fileSmells = entry.getValue();

            try {
                processFile(filePath, fileSmells, options, result);
            } catch (Exception e) {
                result.addError("Failed to process " + filePath + ": " + e.getMessage());

                if (options.isStopOnError()) {
                    break;
                }
            }
        }

        return result;
    }

    /**
     * Process all smells in a single file.
     */
    private void processFile(String filePath, List<CodeSmell> smells, FixOptions options, FixResult result)
            throws Exception {
        Path file = Paths.get(filePath);

        if (!Files.exists(file)) {
            result.addError("File not found: " + filePath);
            return;
        }

        // Create backup if needed
        Path backupPath = null;
        if (options.isCreateBackup() && !options.isDryRun()) {
            backupPath = backupManager.createBackup(file);
        }

        // Apply fixes for each smell in this file
        boolean fileModified = false;

        for (CodeSmell smell : smells) {
            result.incrementAttempted();

            try {
                if (applyFix(smell, options)) {
                    result.incrementSuccess();
                    fileModified = true;

                    FixResult.FixOperation op = new FixResult.FixOperation(
                            filePath,
                            smell.getType().toString(),
                            "Applied fix for " + smell.getType()
                    );
                    op.setSuccess(true);
                    result.addOperation(op);
                } else {
                    result.incrementSkipped();
                }
            } catch (Exception e) {
                result.incrementFailure();
                result.addError(String.format("Failed to fix %s in %s: %s",
                        smell.getType(), filePath, e.getMessage()));

                FixResult.FixOperation op = new FixResult.FixOperation(
                        filePath,
                        smell.getType().toString(),
                        "Failed: " + e.getMessage()
                );
                op.setSuccess(false);
                op.setErrorMessage(e.getMessage());
                result.addOperation(op);
            }
        }

        // If dry-run, restore backup
        if (options.isDryRun() && backupPath != null && fileModified) {
            Files.copy(backupPath, file, StandardCopyOption.REPLACE_EXISTING);
            Files.delete(backupPath);
        }
    }

    /**
     * Apply fix for a single code smell.
     */
    public boolean applyFix(CodeSmell smell, FixOptions options) throws Exception {
        if (!canFix(smell)) {
            return false;
        }

        RefactoringStrategy fixer = fixers.get(smell.getType());
        if (fixer == null) {
            return false;
        }

        // For dry-run, just check if fix would be applied
        if (options.isDryRun()) {
            return true;
        }

        // Apply the fix
        fixer.apply(smell);

        return true;
    }

    /**
     * Check if a code smell can be fixed.
     */
    public boolean canFix(CodeSmell smell) {
        return smell.isAutoFixAvailable() && fixers.containsKey(smell.getType());
    }

    /**
     * Get list of fixable smell types.
     */
    public Set<CodeSmellType> getFixableTypes() {
        return new HashSet<>(fixers.keySet());
    }

    /**
     * Check if file matches pattern filter.
     */
    private boolean matchesFilePattern(String filePath, String pattern) {
        if (pattern == null || pattern.isEmpty()) {
            return true;
        }

        // Simple glob-style pattern matching
        String regex = pattern
                .replace(".", "\\.")
                .replace("*", ".*")
                .replace("?", ".");

        return filePath.matches(regex);
    }

    /**
     * Backup manager for file backups.
     */
    public static class BackupManager {
        private static final String BACKUP_SUFFIX = ".pragmite-backup";

        public Path createBackup(Path file) throws Exception {
            Path backupPath = Paths.get(file.toString() + BACKUP_SUFFIX);
            Files.copy(file, backupPath, StandardCopyOption.REPLACE_EXISTING);
            return backupPath;
        }

        public void restoreBackup(Path backupPath) throws Exception {
            if (!Files.exists(backupPath)) {
                throw new IllegalArgumentException("Backup file not found: " + backupPath);
            }

            Path originalPath = Paths.get(backupPath.toString().replace(BACKUP_SUFFIX, ""));
            Files.copy(backupPath, originalPath, StandardCopyOption.REPLACE_EXISTING);
            Files.delete(backupPath);
        }

        public void deleteBackup(Path backupPath) throws Exception {
            if (Files.exists(backupPath)) {
                Files.delete(backupPath);
            }
        }
    }
}

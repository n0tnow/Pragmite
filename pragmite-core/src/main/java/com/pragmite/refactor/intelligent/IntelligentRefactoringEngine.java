package com.pragmite.refactor.intelligent;

import com.pragmite.model.CodeSmell;
import com.pragmite.refactor.BackupManager;
import com.pragmite.refactor.RefactoringStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Intelligent refactoring engine that applies refactorings automatically with backup/rollback support.
 */
public class IntelligentRefactoringEngine {
    private static final Logger logger = LoggerFactory.getLogger(IntelligentRefactoringEngine.class);

    private final BackupManager backupManager;

    public IntelligentRefactoringEngine() {
        this.backupManager = new BackupManager();
    }

    public RefactoringOutcome applyIntelligently(CodeSmell smell, RefactoringStrategy strategy) {
        logger.info("Starting intelligent refactoring: {}", strategy.getName());
        RefactoringOutcome outcome = new RefactoringOutcome(smell, strategy);

        try {
            if (!strategy.canHandle(smell) || !strategy.validate(smell)) {
                outcome.setStatus(RefactoringStatus.VALIDATION_FAILED);
                return outcome;
            }

            String backupId = backupManager.createBackup(smell.getFilePath());
            outcome.setBackupId(backupId);

            Path filePath = Paths.get(smell.getFilePath());
            String originalCode = Files.readString(filePath);
            String refactoringMessage = strategy.apply(smell);
            outcome.setRefactoringMessage(refactoringMessage);

            String refactoredCode = Files.readString(filePath);
            outcome.setCodeChanged(!originalCode.equals(refactoredCode));

            if (!outcome.isCodeChanged()) {
                outcome.setStatus(RefactoringStatus.NO_CHANGE);
            } else {
                outcome.setStatus(RefactoringStatus.SUCCESS);
            }

            logger.info("Refactoring successful");
            return outcome;

        } catch (Exception e) {
            logger.error("Refactoring failed", e);
            outcome.setStatus(RefactoringStatus.ERROR);
            outcome.setErrorMessage(e.getMessage());

            if (outcome.getBackupId() != null) {
                try {
                    backupManager.restore(outcome.getBackupId());
                    outcome.setRolledBack(true);
                } catch (Exception rollbackError) {
                    logger.error("Rollback failed!", rollbackError);
                }
            }
            return outcome;
        }
    }

    public enum RefactoringStatus {
        SUCCESS, NO_CHANGE, VALIDATION_FAILED, ERROR
    }

    public static class RefactoringOutcome {
        private final CodeSmell smell;
        private final RefactoringStrategy strategy;
        private RefactoringStatus status;
        private String backupId;
        private boolean codeChanged;
        private boolean rolledBack;
        private String refactoringMessage;
        private String errorMessage;

        public RefactoringOutcome(CodeSmell smell, RefactoringStrategy strategy) {
            this.smell = smell;
            this.strategy = strategy;
        }

        public RefactoringStatus getStatus() { return status; }
        public void setStatus(RefactoringStatus status) { this.status = status; }
        public String getBackupId() { return backupId; }
        public void setBackupId(String backupId) { this.backupId = backupId; }
        public boolean isCodeChanged() { return codeChanged; }
        public void setCodeChanged(boolean changed) { this.codeChanged = changed; }
        public boolean isRolledBack() { return rolledBack; }
        public void setRolledBack(boolean rolled) { this.rolledBack = rolled; }
        public String getRefactoringMessage() { return refactoringMessage; }
        public void setRefactoringMessage(String msg) { this.refactoringMessage = msg; }
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String msg) { this.errorMessage = msg; }
        public boolean isSuccess() { return status == RefactoringStatus.SUCCESS; }

        @Override
        public String toString() {
            return String.format("Refactoring %s: %s (Changed: %s)",
                status, strategy.getName(), codeChanged);
        }
    }
}

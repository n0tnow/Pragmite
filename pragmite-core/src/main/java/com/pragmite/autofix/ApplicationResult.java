package com.pragmite.autofix;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Result of applying refactored code to a source file.
 *
 * Tracks success/failure, backup info, errors, and metrics.
 *
 * Version: v0.5
 */
public class ApplicationResult {
    private final boolean success;
    private final ResultType type;
    private final Path appliedFile;
    private final Backup backup;
    private final List<String> errors;
    private final ApplicationMetrics metrics;
    private final CompilationResult compilationResult;

    private ApplicationResult(Builder builder) {
        this.success = builder.success;
        this.type = builder.type;
        this.appliedFile = builder.appliedFile;
        this.backup = builder.backup;
        this.errors = builder.errors != null ? new ArrayList<>(builder.errors) : new ArrayList<>();
        this.metrics = builder.metrics;
        this.compilationResult = builder.compilationResult;
    }

    public static ApplicationResult success(Path file, Backup backup, ApplicationMetrics metrics) {
        return new Builder()
            .success(true)
            .type(ResultType.SUCCESS)
            .appliedFile(file)
            .backup(backup)
            .metrics(metrics)
            .build();
    }

    public static ApplicationResult successWithWarnings(Path file, Backup backup,
                                                       ApplicationMetrics metrics,
                                                       CompilationResult compilation) {
        return new Builder()
            .success(true)
            .type(ResultType.SUCCESS_WITH_WARNINGS)
            .appliedFile(file)
            .backup(backup)
            .metrics(metrics)
            .compilationResult(compilation)
            .build();
    }

    public static ApplicationResult failed(String reason) {
        return new Builder()
            .success(false)
            .type(ResultType.FAILED)
            .errors(Collections.singletonList(reason))
            .build();
    }

    public static ApplicationResult failed(String reason, List<String> errors) {
        List<String> allErrors = new ArrayList<>();
        allErrors.add(reason);
        allErrors.addAll(errors);

        return new Builder()
            .success(false)
            .type(ResultType.FAILED)
            .errors(allErrors)
            .build();
    }

    public static ApplicationResult compilationFailed(Path file, Backup backup,
                                                     CompilationResult compilation) {
        return new Builder()
            .success(false)
            .type(ResultType.COMPILATION_FAILED)
            .appliedFile(file)
            .backup(backup)
            .compilationResult(compilation)
            .errors(compilation.getErrors().stream()
                .map(CompilationResult.CompilationError::getMessage)
                .toList())
            .build();
    }

    public static ApplicationResult skipped(String reason) {
        return new Builder()
            .success(false)
            .type(ResultType.SKIPPED)
            .errors(Collections.singletonList(reason))
            .build();
    }

    public static ApplicationResult rolledBack(Path file, Backup backup, String reason) {
        return new Builder()
            .success(false)
            .type(ResultType.ROLLED_BACK)
            .appliedFile(file)
            .backup(backup)
            .errors(Collections.singletonList(reason))
            .build();
    }

    // Getters

    public boolean isSuccess() {
        return success;
    }

    public ResultType getType() {
        return type;
    }

    public Path getAppliedFile() {
        return appliedFile;
    }

    public Backup getBackup() {
        return backup;
    }

    public boolean hasBackup() {
        return backup != null;
    }

    public List<String> getErrors() {
        return Collections.unmodifiableList(errors);
    }

    public ApplicationMetrics getMetrics() {
        return metrics;
    }

    public CompilationResult getCompilationResult() {
        return compilationResult;
    }

    public boolean wasRolledBack() {
        return type == ResultType.ROLLED_BACK;
    }

    public boolean wasSkipped() {
        return type == ResultType.SKIPPED;
    }

    // Builder

    public static class Builder {
        private boolean success = false;
        private ResultType type = ResultType.FAILED;
        private Path appliedFile = null;
        private Backup backup = null;
        private List<String> errors = new ArrayList<>();
        private ApplicationMetrics metrics = null;
        private CompilationResult compilationResult = null;

        public Builder success(boolean success) {
            this.success = success;
            return this;
        }

        public Builder type(ResultType type) {
            this.type = type;
            return this;
        }

        public Builder appliedFile(Path appliedFile) {
            this.appliedFile = appliedFile;
            return this;
        }

        public Builder backup(Backup backup) {
            this.backup = backup;
            return this;
        }

        public Builder errors(List<String> errors) {
            this.errors = errors != null ? new ArrayList<>(errors) : new ArrayList<>();
            return this;
        }

        public Builder addError(String error) {
            if (this.errors == null) {
                this.errors = new ArrayList<>();
            }
            this.errors.add(error);
            return this;
        }

        public Builder metrics(ApplicationMetrics metrics) {
            this.metrics = metrics;
            return this;
        }

        public Builder compilationResult(CompilationResult compilationResult) {
            this.compilationResult = compilationResult;
            return this;
        }

        public ApplicationResult build() {
            return new ApplicationResult(this);
        }
    }

    // Result Types

    public enum ResultType {
        SUCCESS,
        SUCCESS_WITH_WARNINGS,
        FAILED,
        COMPILATION_FAILED,
        SKIPPED,
        ROLLED_BACK
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("ApplicationResult{");
        sb.append("type=").append(type);

        if (appliedFile != null) {
            sb.append(", file=").append(appliedFile.getFileName());
        }

        if (success && metrics != null) {
            sb.append(", ").append(metrics);
        }

        if (!errors.isEmpty()) {
            sb.append(", errors=").append(errors.size());
        }

        sb.append("}");
        return sb.toString();
    }
}

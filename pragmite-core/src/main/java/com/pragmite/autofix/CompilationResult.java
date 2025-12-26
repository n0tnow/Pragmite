package com.pragmite.autofix;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Result of compilation validation.
 */
public class CompilationResult {
    private final boolean success;
    private final List<CompilationError> errors;
    private final List<CompilationWarning> warnings;

    private CompilationResult(boolean success, List<CompilationError> errors, List<CompilationWarning> warnings) {
        this.success = success;
        this.errors = errors != null ? new ArrayList<>(errors) : new ArrayList<>();
        this.warnings = warnings != null ? new ArrayList<>(warnings) : new ArrayList<>();
    }

    public static CompilationResult success() {
        return new CompilationResult(true, Collections.emptyList(), Collections.emptyList());
    }

    public static CompilationResult successWithWarnings(List<CompilationWarning> warnings) {
        return new CompilationResult(true, Collections.emptyList(), warnings);
    }

    public static CompilationResult failed(List<CompilationError> errors) {
        return new CompilationResult(false, errors, Collections.emptyList());
    }

    public static CompilationResult failedWithWarnings(List<CompilationError> errors, List<CompilationWarning> warnings) {
        return new CompilationResult(false, errors, warnings);
    }

    public boolean isSuccess() {
        return success;
    }

    public List<CompilationError> getErrors() {
        return Collections.unmodifiableList(errors);
    }

    public List<CompilationWarning> getWarnings() {
        return Collections.unmodifiableList(warnings);
    }

    public boolean hasWarnings() {
        return !warnings.isEmpty();
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    @Override
    public String toString() {
        if (success) {
            return "Compilation succeeded" +
                   (hasWarnings() ? " with " + warnings.size() + " warnings" : "");
        } else {
            return "Compilation failed with " + errors.size() + " errors" +
                   (hasWarnings() ? " and " + warnings.size() + " warnings" : "");
        }
    }

    /**
     * Compilation error details.
     */
    public static class CompilationError {
        private final long lineNumber;
        private final long columnNumber;
        private final String message;
        private final String code;

        public CompilationError(long lineNumber, long columnNumber, String message, String code) {
            this.lineNumber = lineNumber;
            this.columnNumber = columnNumber;
            this.message = message;
            this.code = code;
        }

        public long getLineNumber() {
            return lineNumber;
        }

        public long getColumnNumber() {
            return columnNumber;
        }

        public String getMessage() {
            return message;
        }

        public String getCode() {
            return code;
        }

        @Override
        public String toString() {
            return String.format("Line %d:%d - %s", lineNumber, columnNumber, message);
        }
    }

    /**
     * Compilation warning details.
     */
    public static class CompilationWarning {
        private final long lineNumber;
        private final String message;

        public CompilationWarning(long lineNumber, String message) {
            this.lineNumber = lineNumber;
            this.message = message;
        }

        public long getLineNumber() {
            return lineNumber;
        }

        public String getMessage() {
            return message;
        }

        @Override
        public String toString() {
            return String.format("Line %d - %s", lineNumber, message);
        }
    }
}

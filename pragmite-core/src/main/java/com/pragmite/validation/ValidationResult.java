package com.pragmite.validation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Result of validation operation containing status, errors, and warnings.
 *
 * Version: 1.6.3 (Phase 4, Sprint 4, Task 4.1)
 *
 * @author Pragmite Team
 * @version 1.6.3
 * @since 2025-12-28
 */
public class ValidationResult {

    private final boolean valid;
    private final List<ValidationError> errors;
    private final List<ValidationError> warnings;

    /**
     * Create validation result
     *
     * @param valid Whether validation passed
     * @param errors List of validation errors
     * @param warnings List of validation warnings
     */
    public ValidationResult(boolean valid, List<ValidationError> errors, List<ValidationError> warnings) {
        this.valid = valid;
        this.errors = errors != null ? new ArrayList<>(errors) : new ArrayList<>();
        this.warnings = warnings != null ? new ArrayList<>(warnings) : new ArrayList<>();
    }

    /**
     * Create successful validation result
     */
    public static ValidationResult success() {
        return new ValidationResult(true, Collections.emptyList(), Collections.emptyList());
    }

    /**
     * Create failed validation result with single error
     */
    public static ValidationResult createError(String errorMessage) {
        ValidationError error = new ValidationError("ERROR", errorMessage, -1, -1, null);
        return new ValidationResult(false, List.of(error), Collections.emptyList());
    }

    /**
     * Check if validation passed
     */
    public boolean isValid() {
        return valid;
    }

    /**
     * Get validation errors
     */
    public List<ValidationError> getErrors() {
        return Collections.unmodifiableList(errors);
    }

    /**
     * Get validation warnings
     */
    public List<ValidationError> getWarnings() {
        return Collections.unmodifiableList(warnings);
    }

    /**
     * Check if there are any errors
     */
    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    /**
     * Check if there are any warnings
     */
    public boolean hasWarnings() {
        return !warnings.isEmpty();
    }

    /**
     * Get total error count
     */
    public int getErrorCount() {
        return errors.size();
    }

    /**
     * Get total warning count
     */
    public int getWarningCount() {
        return warnings.size();
    }

    /**
     * Get formatted error message
     */
    public String getErrorMessage() {
        if (errors.isEmpty()) {
            return "No errors";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Validation failed with ").append(errors.size()).append(" error(s):\n");

        for (int i = 0; i < errors.size(); i++) {
            ValidationError error = errors.get(i);
            sb.append(i + 1).append(". ");
            if (error.getLineNumber() > 0) {
                sb.append("Line ").append(error.getLineNumber());
                if (error.getColumnNumber() > 0) {
                    sb.append(", Column ").append(error.getColumnNumber());
                }
                sb.append(": ");
            }
            sb.append(error.getMessage()).append("\n");
        }

        return sb.toString();
    }

    @Override
    public String toString() {
        return "ValidationResult{" +
            "valid=" + valid +
            ", errors=" + errors.size() +
            ", warnings=" + warnings.size() +
            '}';
    }
}

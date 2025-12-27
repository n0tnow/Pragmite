package com.pragmite.validation;

/**
 * Represents a validation error or warning from javac compilation.
 *
 * Version: 1.6.3 (Phase 4, Sprint 4, Task 4.1)
 *
 * @author Pragmite Team
 * @version 1.6.3
 * @since 2025-12-28
 */
public class ValidationError {

    private final String kind;
    private final String message;
    private final int lineNumber;
    private final int columnNumber;
    private final String code;

    /**
     * Create validation error
     *
     * @param kind Error kind (ERROR, WARNING, NOTE, etc.)
     * @param message Error message
     * @param lineNumber Line number where error occurred
     * @param columnNumber Column number where error occurred
     * @param code Error code
     */
    public ValidationError(String kind, String message, int lineNumber, int columnNumber, String code) {
        this.kind = kind;
        this.message = message;
        this.lineNumber = lineNumber;
        this.columnNumber = columnNumber;
        this.code = code;
    }

    public String getKind() {
        return kind;
    }

    public String getMessage() {
        return message;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public int getColumnNumber() {
        return columnNumber;
    }

    public String getCode() {
        return code;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[").append(kind).append("]");
        if (lineNumber > 0) {
            sb.append(" Line ").append(lineNumber);
            if (columnNumber > 0) {
                sb.append(", Column ").append(columnNumber);
            }
        }
        sb.append(": ").append(message);
        if (code != null) {
            sb.append(" (").append(code).append(")");
        }
        return sb.toString();
    }
}

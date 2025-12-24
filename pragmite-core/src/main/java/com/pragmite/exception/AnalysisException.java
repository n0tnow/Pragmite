package com.pragmite.exception;

/**
 * Exception for code analysis operations.
 */
public class AnalysisException extends PragmiteException {

    private final String filePath;
    private final int lineNumber;

    public AnalysisException(ErrorCode errorCode, String message) {
        this(errorCode, message, null, -1);
    }

    public AnalysisException(ErrorCode errorCode, String message, String filePath) {
        this(errorCode, message, filePath, -1);
    }

    public AnalysisException(ErrorCode errorCode, String message, String filePath, int lineNumber) {
        super(errorCode, message, formatLocation(filePath, lineNumber), null);
        this.filePath = filePath;
        this.lineNumber = lineNumber;
    }

    public AnalysisException(ErrorCode errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
        this.filePath = null;
        this.lineNumber = -1;
    }

    private static String formatLocation(String filePath, int lineNumber) {
        if (filePath == null) return null;
        if (lineNumber <= 0) return "in " + filePath;
        return String.format("at %s:%d", filePath, lineNumber);
    }

    public String getFilePath() {
        return filePath;
    }

    public int getLineNumber() {
        return lineNumber;
    }
}

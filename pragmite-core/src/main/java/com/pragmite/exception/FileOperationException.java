package com.pragmite.exception;

import java.io.IOException;

/**
 * Exception for file I/O operations.
 */
public class FileOperationException extends PragmiteException {

    private final String filePath;

    public FileOperationException(ErrorCode errorCode, String filePath, Throwable cause) {
        super(errorCode,
              String.format("Failed to %s file: %s", getOperation(errorCode), filePath),
              cause != null ? cause.getMessage() : null,
              cause);
        this.filePath = filePath;
    }

    public FileOperationException(ErrorCode errorCode, String filePath, String details) {
        super(errorCode,
              String.format("Failed to %s file: %s", getOperation(errorCode), filePath),
              details,
              null);
        this.filePath = filePath;
    }

    public String getFilePath() {
        return filePath;
    }

    private static String getOperation(ErrorCode errorCode) {
        return switch (errorCode) {
            case FILE_NOT_FOUND -> "find";
            case FILE_NOT_READABLE -> "read";
            case FILE_NOT_WRITABLE -> "write to";
            default -> "access";
        };
    }

    /**
     * Creates FileOperationException from IOException.
     */
    public static FileOperationException from(String filePath, IOException e) {
        ErrorCode errorCode = determineErrorCode(e);
        return new FileOperationException(errorCode, filePath, e);
    }

    private static ErrorCode determineErrorCode(IOException e) {
        String message = e.getMessage() != null ? e.getMessage().toLowerCase() : "";

        if (message.contains("no such file") || message.contains("not found")) {
            return ErrorCode.FILE_NOT_FOUND;
        } else if (message.contains("access denied") || message.contains("permission")) {
            return ErrorCode.FILE_NOT_READABLE;
        } else {
            return ErrorCode.FILE_IO_ERROR;
        }
    }
}

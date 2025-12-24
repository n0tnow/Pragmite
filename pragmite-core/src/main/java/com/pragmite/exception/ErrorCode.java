package com.pragmite.exception;

/**
 * Error codes for Pragmite exceptions.
 * Each code has a unique identifier, category, and suggested action.
 */
public enum ErrorCode {
    // File I/O Errors (1000-1099)
    FILE_NOT_FOUND("E1001", "File or directory not found",
                   "Check that the file path is correct and the file exists"),
    FILE_NOT_READABLE("E1002", "File cannot be read",
                      "Check file permissions and ensure the file is not corrupted"),
    FILE_NOT_WRITABLE("E1003", "File cannot be written",
                      "Check file permissions and ensure the file is not locked by another process"),
    FILE_IO_ERROR("E1004", "File I/O operation failed",
                  "Check disk space and file system health"),

    // Parsing Errors (1100-1199)
    PARSE_ERROR("E1101", "Failed to parse Java source file",
                "Ensure the file contains valid Java syntax"),
    INVALID_SYNTAX("E1102", "Invalid Java syntax detected",
                   "Fix syntax errors before running analysis"),
    UNSUPPORTED_JAVA_VERSION("E1103", "Unsupported Java version",
                             "Pragmite supports Java 11 and above"),

    // Analysis Errors (1200-1299)
    ANALYSIS_FAILED("E1201", "Code analysis failed",
                    "Check the error details and ensure the code is valid"),
    CACHE_ERROR("E1202", "Cache operation failed",
                "Try clearing the cache or disabling caching"),
    DETECTOR_ERROR("E1203", "Code smell detector failed",
                   "Check detector configuration"),

    // Refactoring Errors (1300-1399)
    REFACTORING_FAILED("E1301", "Refactoring operation failed",
                       "Review the suggested refactoring and try again"),
    BACKUP_FAILED("E1302", "Failed to create backup",
                  "Ensure sufficient disk space and write permissions"),
    RESTORE_FAILED("E1303", "Failed to restore from backup",
                   "Backup may be corrupted - manual intervention required"),
    BUILD_VERIFICATION_FAILED("E1304", "Build verification failed",
                              "Fix compilation errors before proceeding"),
    TEST_VERIFICATION_FAILED("E1305", "Test verification failed",
                             "Review test failures and fix issues"),

    // Configuration Errors (1400-1499)
    CONFIG_NOT_FOUND("E1401", "Configuration file not found",
                     "Create .pragmite.yaml in project root or home directory"),
    CONFIG_INVALID("E1402", "Invalid configuration",
                   "Check YAML syntax and configuration values"),
    CONFIG_LOAD_ERROR("E1403", "Failed to load configuration",
                      "Ensure configuration file is readable"),

    // Validation Errors (1500-1599)
    INVALID_PARAMETER("E1501", "Invalid parameter value",
                      "Check parameter constraints and try again"),
    VALIDATION_FAILED("E1502", "Validation failed",
                      "Review validation errors and correct input"),

    // System Errors (1600-1699)
    TIMEOUT("E1601", "Operation timed out",
            "Try increasing timeout or check for hanging processes"),
    OUT_OF_MEMORY("E1602", "Out of memory",
                  "Increase heap size or reduce analysis scope"),
    THREAD_INTERRUPTED("E1603", "Operation was interrupted",
                       "Retry the operation"),

    // Unknown/Generic Errors (1900-1999)
    UNKNOWN_ERROR("E1999", "An unexpected error occurred",
                  "Check logs for details and report if issue persists");

    private final String code;
    private final String description;
    private final String suggestedAction;

    ErrorCode(String code, String description, String suggestedAction) {
        this.code = code;
        this.description = description;
        this.suggestedAction = suggestedAction;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public String getSuggestedAction() {
        return suggestedAction;
    }

    @Override
    public String toString() {
        return code + ": " + description;
    }
}

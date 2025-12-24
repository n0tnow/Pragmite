package com.pragmite.exception;

/**
 * Base exception for all Pragmite-specific exceptions.
 * Provides error codes and user-friendly messages.
 */
public class PragmiteException extends Exception {
    private final ErrorCode errorCode;
    private final String userMessage;
    private final String technicalDetails;

    public PragmiteException(ErrorCode errorCode, String userMessage) {
        this(errorCode, userMessage, null, null);
    }

    public PragmiteException(ErrorCode errorCode, String userMessage, Throwable cause) {
        this(errorCode, userMessage, null, cause);
    }

    public PragmiteException(ErrorCode errorCode, String userMessage, String technicalDetails, Throwable cause) {
        super(formatMessage(errorCode, userMessage, technicalDetails), cause);
        this.errorCode = errorCode;
        this.userMessage = userMessage;
        this.technicalDetails = technicalDetails;
    }

    private static String formatMessage(ErrorCode errorCode, String userMessage, String technicalDetails) {
        StringBuilder sb = new StringBuilder();
        sb.append("[").append(errorCode.getCode()).append("] ");
        sb.append(userMessage);
        if (technicalDetails != null && !technicalDetails.isEmpty()) {
            sb.append(" (").append(technicalDetails).append(")");
        }
        return sb.toString();
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public String getUserMessage() {
        return userMessage;
    }

    public String getTechnicalDetails() {
        return technicalDetails;
    }

    /**
     * Returns a user-friendly error message with suggested actions.
     */
    public String getUserFriendlyMessage() {
        StringBuilder sb = new StringBuilder();
        sb.append("Error: ").append(userMessage).append("\n");

        if (errorCode.getSuggestedAction() != null) {
            sb.append("Suggestion: ").append(errorCode.getSuggestedAction()).append("\n");
        }

        sb.append("Error Code: ").append(errorCode.getCode());

        return sb.toString();
    }
}

package com.pragmite.exception;

/**
 * Exception for refactoring operations.
 */
public class RefactoringException extends PragmiteException {

    private final String refactoringType;
    private final String targetElement;

    public RefactoringException(ErrorCode errorCode, String refactoringType, String targetElement, String message) {
        super(errorCode,
              String.format("%s refactoring failed for '%s': %s", refactoringType, targetElement, message));
        this.refactoringType = refactoringType;
        this.targetElement = targetElement;
    }

    public RefactoringException(ErrorCode errorCode, String refactoringType, String targetElement, String message, Throwable cause) {
        super(errorCode,
              String.format("%s refactoring failed for '%s': %s", refactoringType, targetElement, message),
              cause);
        this.refactoringType = refactoringType;
        this.targetElement = targetElement;
    }

    public String getRefactoringType() {
        return refactoringType;
    }

    public String getTargetElement() {
        return targetElement;
    }
}

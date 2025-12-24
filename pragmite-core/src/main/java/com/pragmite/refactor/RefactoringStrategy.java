package com.pragmite.refactor;

import com.pragmite.model.CodeSmell;

/**
 * Interface for refactoring strategies.
 * Each strategy handles a specific type of code smell transformation.
 */
public interface RefactoringStrategy {

    /**
     * Gets the name of this refactoring strategy.
     */
    String getName();

    /**
     * Checks if this strategy can handle the given code smell.
     */
    boolean canHandle(CodeSmell smell);

    /**
     * Validates that the refactoring can be safely applied.
     * @return true if refactoring is safe to apply
     */
    boolean validate(CodeSmell smell) throws Exception;

    /**
     * Applies the refactoring transformation.
     * @return Description of what was changed
     */
    String apply(CodeSmell smell) throws Exception;

    /**
     * Generates a preview of the refactoring without applying it (dry-run mode).
     * Default implementation returns null (not supported).
     * @return Preview of changes, or null if preview not supported
     */
    default RefactoringPreview preview(CodeSmell smell) throws Exception {
        return null; // Default: preview not supported
    }

    /**
     * Gets a description of what this refactoring does.
     */
    String getDescription();
}

package com.pragmite.refactor;

import com.pragmite.model.CodeSmell;

/**
 * Represents a single refactoring action to be executed.
 */
public class RefactoringAction {
    private final CodeSmell codeSmell;
    private final RefactoringStrategy strategy;

    public RefactoringAction(CodeSmell codeSmell, RefactoringStrategy strategy) {
        this.codeSmell = codeSmell;
        this.strategy = strategy;
    }

    public CodeSmell getCodeSmell() {
        return codeSmell;
    }

    public RefactoringStrategy getStrategy() {
        return strategy;
    }

    public String getDescription() {
        return String.format("%s at %s:%d",
            strategy.getName(),
            codeSmell.getFilePath(),
            codeSmell.getStartLine());
    }

    @Override
    public String toString() {
        return getDescription();
    }
}

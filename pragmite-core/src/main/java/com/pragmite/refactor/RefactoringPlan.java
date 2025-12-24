package com.pragmite.refactor;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A plan containing multiple refactoring actions to be executed.
 */
public class RefactoringPlan {
    private final List<RefactoringAction> actions;

    public RefactoringPlan() {
        this.actions = new ArrayList<>();
    }

    public void addAction(RefactoringAction action) {
        actions.add(action);
    }

    public List<RefactoringAction> getActions() {
        return actions;
    }

    /**
     * Gets actions grouped by file path.
     */
    public List<String> getAffectedFiles() {
        return actions.stream()
            .map(a -> a.getCodeSmell().getFilePath())
            .distinct()
            .collect(Collectors.toList());
    }

    /**
     * Gets the number of actions in this plan.
     */
    public int size() {
        return actions.size();
    }

    @Override
    public String toString() {
        return String.format("RefactoringPlan[%d actions, %d files]",
            actions.size(), getAffectedFiles().size());
    }
}

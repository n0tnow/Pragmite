package com.pragmite.refactor;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a preview of refactoring changes without actually applying them.
 * Used for dry-run mode.
 */
public class RefactoringPreview {
    private final String refactoringType;
    private final String targetElement;
    private final String filePath;
    private final List<Change> changes;
    private final String summary;
    private final boolean safe;

    public RefactoringPreview(String refactoringType, String targetElement, String filePath) {
        this.refactoringType = refactoringType;
        this.targetElement = targetElement;
        this.filePath = filePath;
        this.changes = new ArrayList<>();
        this.summary = "";
        this.safe = true;
    }

    public RefactoringPreview(String refactoringType, String targetElement, String filePath,
                             List<Change> changes, String summary, boolean safe) {
        this.refactoringType = refactoringType;
        this.targetElement = targetElement;
        this.filePath = filePath;
        this.changes = new ArrayList<>(changes);
        this.summary = summary;
        this.safe = safe;
    }

    public String getRefactoringType() {
        return refactoringType;
    }

    public String getTargetElement() {
        return targetElement;
    }

    public String getFilePath() {
        return filePath;
    }

    public List<Change> getChanges() {
        return new ArrayList<>(changes);
    }

    public String getSummary() {
        return summary;
    }

    public boolean isSafe() {
        return safe;
    }

    /**
     * Generates a human-readable diff of the changes.
     */
    public String generateDiff() {
        StringBuilder diff = new StringBuilder();
        diff.append("=== Refactoring Preview ===\n");
        diff.append("Type: ").append(refactoringType).append("\n");
        diff.append("Target: ").append(targetElement).append("\n");
        diff.append("File: ").append(filePath).append("\n");
        diff.append("Safe: ").append(safe ? "YES" : "NO").append("\n");
        diff.append("\n");

        if (!summary.isEmpty()) {
            diff.append("Summary:\n").append(summary).append("\n\n");
        }

        if (changes.isEmpty()) {
            diff.append("No specific changes to preview.\n");
        } else {
            diff.append("Changes (").append(changes.size()).append("):\n");
            for (int i = 0; i < changes.size(); i++) {
                Change change = changes.get(i);
                diff.append(String.format("%d. %s\n", i + 1, change.getDescription()));

                if (change.getOldCode() != null && change.getNewCode() != null) {
                    diff.append("   - Old:\n");
                    for (String line : change.getOldCode().split("\n")) {
                        diff.append("     - ").append(line).append("\n");
                    }
                    diff.append("   + New:\n");
                    for (String line : change.getNewCode().split("\n")) {
                        diff.append("     + ").append(line).append("\n");
                    }
                }
                diff.append("\n");
            }
        }

        return diff.toString();
    }

    @Override
    public String toString() {
        return String.format("RefactoringPreview[%s on %s in %s, %d changes, safe=%s]",
                           refactoringType, targetElement, filePath, changes.size(), safe);
    }

    /**
     * Represents a single change in the refactoring.
     */
    public static class Change {
        private final ChangeType type;
        private final String description;
        private final int lineNumber;
        private final String oldCode;
        private final String newCode;

        public Change(ChangeType type, String description, int lineNumber) {
            this(type, description, lineNumber, null, null);
        }

        public Change(ChangeType type, String description, int lineNumber, String oldCode, String newCode) {
            this.type = type;
            this.description = description;
            this.lineNumber = lineNumber;
            this.oldCode = oldCode;
            this.newCode = newCode;
        }

        public ChangeType getType() {
            return type;
        }

        public String getDescription() {
            return description;
        }

        public int getLineNumber() {
            return lineNumber;
        }

        public String getOldCode() {
            return oldCode;
        }

        public String getNewCode() {
            return newCode;
        }

        @Override
        public String toString() {
            return String.format("[%s] Line %d: %s", type, lineNumber, description);
        }
    }

    public enum ChangeType {
        ADD,           // Adding new code
        REMOVE,        // Removing code
        MODIFY,        // Modifying existing code
        RENAME,        // Renaming identifier
        EXTRACT,       // Extracting to new method/class
        INLINE         // Inlining code
    }
}

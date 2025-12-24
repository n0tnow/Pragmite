package com.pragmite.refactoring;

import com.pragmite.model.CodeSmell;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a concrete refactoring suggestion for a detected code smell.
 * Provides step-by-step instructions, code examples, and auto-fix capabilities.
 */
public class RefactoringSuggestion {
    private final String title;
    private final String description;
    private final Difficulty difficulty;
    private final List<String> steps;
    private final String beforeCode;
    private final String afterCode;
    private final boolean autoFixAvailable;
    private final CodeSmell relatedSmell;

    public enum Difficulty {
        EASY("Easy - Simple automated refactoring"),
        MEDIUM("Medium - Requires careful review"),
        HARD("Hard - Manual intervention recommended");

        private final String description;

        Difficulty(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    private RefactoringSuggestion(Builder builder) {
        this.title = builder.title;
        this.description = builder.description;
        this.difficulty = builder.difficulty;
        this.steps = builder.steps;
        this.beforeCode = builder.beforeCode;
        this.afterCode = builder.afterCode;
        this.autoFixAvailable = builder.autoFixAvailable;
        this.relatedSmell = builder.relatedSmell;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public Difficulty getDifficulty() {
        return difficulty;
    }

    public List<String> getSteps() {
        return new ArrayList<>(steps);
    }

    public String getBeforeCode() {
        return beforeCode;
    }

    public String getAfterCode() {
        return afterCode;
    }

    public boolean isAutoFixAvailable() {
        return autoFixAvailable;
    }

    public CodeSmell getRelatedSmell() {
        return relatedSmell;
    }

    /**
     * Formats the suggestion as a readable text report.
     */
    public String formatAsText() {
        StringBuilder sb = new StringBuilder();
        sb.append("═══════════════════════════════════════════════════════════════\n");
        sb.append("  REFACTORING SUGGESTION: ").append(title).append("\n");
        sb.append("═══════════════════════════════════════════════════════════════\n\n");

        sb.append("Description: ").append(description).append("\n");
        sb.append("Difficulty:  ").append(difficulty).append(" - ").append(difficulty.getDescription()).append("\n");
        sb.append("Auto-Fix:    ").append(autoFixAvailable ? "✓ Available" : "✗ Manual only").append("\n\n");

        if (!steps.isEmpty()) {
            sb.append("Steps to Fix:\n");
            for (int i = 0; i < steps.size(); i++) {
                sb.append("  ").append(i + 1).append(". ").append(steps.get(i)).append("\n");
            }
            sb.append("\n");
        }

        if (beforeCode != null && !beforeCode.isEmpty()) {
            sb.append("Before:\n");
            sb.append("─────────────────────────────────────────────────────────────\n");
            sb.append(beforeCode).append("\n");
            sb.append("─────────────────────────────────────────────────────────────\n\n");
        }

        if (afterCode != null && !afterCode.isEmpty()) {
            sb.append("After:\n");
            sb.append("─────────────────────────────────────────────────────────────\n");
            sb.append(afterCode).append("\n");
            sb.append("─────────────────────────────────────────────────────────────\n\n");
        }

        return sb.toString();
    }

    /**
     * Builder for RefactoringSuggestion.
     */
    public static class Builder {
        private String title;
        private String description;
        private Difficulty difficulty = Difficulty.MEDIUM;
        private List<String> steps = new ArrayList<>();
        private String beforeCode = "";
        private String afterCode = "";
        private boolean autoFixAvailable = false;
        private CodeSmell relatedSmell;

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder difficulty(Difficulty difficulty) {
            this.difficulty = difficulty;
            return this;
        }

        public Builder addStep(String step) {
            this.steps.add(step);
            return this;
        }

        public Builder steps(List<String> steps) {
            this.steps = new ArrayList<>(steps);
            return this;
        }

        public Builder beforeCode(String beforeCode) {
            this.beforeCode = beforeCode;
            return this;
        }

        public Builder afterCode(String afterCode) {
            this.afterCode = afterCode;
            return this;
        }

        public Builder autoFixAvailable(boolean autoFixAvailable) {
            this.autoFixAvailable = autoFixAvailable;
            return this;
        }

        public Builder relatedSmell(CodeSmell relatedSmell) {
            this.relatedSmell = relatedSmell;
            return this;
        }

        public RefactoringSuggestion build() {
            if (title == null || title.isEmpty()) {
                throw new IllegalStateException("Title is required");
            }
            if (description == null || description.isEmpty()) {
                throw new IllegalStateException("Description is required");
            }
            return new RefactoringSuggestion(this);
        }
    }
}

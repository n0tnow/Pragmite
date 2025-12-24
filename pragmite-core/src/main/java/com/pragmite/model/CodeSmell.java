package com.pragmite.model;

/**
 * Tespit edilen kod kokusunu temsil eder.
 */
public class CodeSmell {
    private CodeSmellType type;
    private Severity severity;
    private String filePath;
    private int startLine;
    private int endLine;
    private String description;
    private String suggestion;
    private String affectedElement; // Sınıf veya metot adı
    private boolean autoFixAvailable;

    public CodeSmell() {}

    public CodeSmell(CodeSmellType type, String filePath, int startLine, String description) {
        this.type = type;
        this.filePath = filePath;
        this.startLine = startLine;
        this.description = description;
        this.severity = type.getDefaultSeverity();
    }

    // Builder pattern for fluent API
    public CodeSmell withEndLine(int endLine) {
        this.endLine = endLine;
        return this;
    }

    public CodeSmell withSuggestion(String suggestion) {
        this.suggestion = suggestion;
        return this;
    }

    public CodeSmell withAffectedElement(String element) {
        this.affectedElement = element;
        return this;
    }

    public CodeSmell withAutoFix(boolean available) {
        this.autoFixAvailable = available;
        return this;
    }

    // Getters and Setters
    public CodeSmellType getType() { return type; }
    public void setType(CodeSmellType type) { this.type = type; }

    public Severity getSeverity() { return severity; }
    public void setSeverity(Severity severity) { this.severity = severity; }

    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }

    public int getStartLine() { return startLine; }
    public void setStartLine(int startLine) { this.startLine = startLine; }

    // Alias for getStartLine for convenience
    public int getLine() { return startLine; }

    // Alias for getDescription for convenience
    public String getMessage() { return description; }

    public int getEndLine() { return endLine; }
    public void setEndLine(int endLine) { this.endLine = endLine; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getSuggestion() { return suggestion; }
    public void setSuggestion(String suggestion) { this.suggestion = suggestion; }

    public String getAffectedElement() { return affectedElement; }
    public void setAffectedElement(String affectedElement) { this.affectedElement = affectedElement; }

    public boolean isAutoFixAvailable() { return autoFixAvailable; }
    public void setAutoFixAvailable(boolean autoFixAvailable) { this.autoFixAvailable = autoFixAvailable; }

    @Override
    public String toString() {
        return String.format("[%s] %s at %s:%d - %s",
            severity, type.getName(), filePath, startLine, description);
    }
}

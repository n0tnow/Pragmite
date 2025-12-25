package com.pragmite.ai;

import com.pragmite.model.CodeSmell;
import com.pragmite.model.CodeSmellType;
import java.util.ArrayList;
import java.util.List;

/**
 * Extracts relevant code context for AI analysis.
 * Provides intelligent snippet selection based on code smell type.
 *
 * @since 1.4.0
 */
public class ContextExtractor {

    private static final int DEFAULT_CONTEXT_LINES = 5;
    private static final int MAX_SNIPPET_LINES = 50;

    /**
     * Extracts relevant code snippets for a code smell.
     *
     * @param smell The code smell
     * @param sourceCode Complete source code
     * @return List of code snippets with context
     */
    public List<String> extractContext(CodeSmell smell, String sourceCode) {
        List<String> snippets = new ArrayList<>();
        String[] lines = sourceCode.split("\n");

        // Extract primary snippet (the problematic code)
        String primarySnippet = extractPrimarySnippet(smell, lines);
        if (!primarySnippet.isEmpty()) {
            snippets.add(primarySnippet);
        }

        // For certain types, extract additional context
        if (needsAdditionalContext(smell.getType())) {
            String additionalContext = extractAdditionalContext(smell, lines);
            if (!additionalContext.isEmpty()) {
                snippets.add(additionalContext);
            }
        }

        return snippets;
    }

    /**
     * Extracts the primary code snippet containing the smell.
     */
    private String extractPrimarySnippet(CodeSmell smell, String[] lines) {
        int lineNumber = smell.getLine() - 1; // Convert to 0-indexed

        if (lineNumber < 0 || lineNumber >= lines.length) {
            return "";
        }

        int contextLines = getContextLinesForType(smell.getType());
        int startLine = Math.max(0, lineNumber - contextLines);
        int endLine = Math.min(lines.length - 1, lineNumber + contextLines);

        // For methods, try to extract the entire method
        if (isMethodRelatedSmell(smell.getType())) {
            int methodStart = findMethodStart(lines, lineNumber);
            int methodEnd = findMethodEnd(lines, lineNumber);

            if (methodStart >= 0 && methodEnd >= 0) {
                int methodLines = methodEnd - methodStart + 1;
                if (methodLines <= MAX_SNIPPET_LINES) {
                    startLine = methodStart;
                    endLine = methodEnd;
                }
            }
        }

        return extractLines(lines, startLine, endLine, smell.getLine());
    }

    /**
     * Extracts additional context if needed (e.g., related classes for coupling issues).
     */
    private String extractAdditionalContext(CodeSmell smell, String[] lines) {
        // For duplicate code, try to find the duplicated section
        if (smell.getType() == CodeSmellType.DUPLICATED_CODE) {
            return extractDuplicateContext(smell, lines);
        }

        // For feature envy, could extract the envied class (future enhancement)
        if (smell.getType() == CodeSmellType.FEATURE_ENVY) {
            return ""; // Placeholder for cross-file analysis
        }

        return "";
    }

    /**
     * Extracts context for duplicate code.
     */
    private String extractDuplicateContext(CodeSmell smell, String[] lines) {
        // This is a simplified version - could be enhanced with actual duplicate detection
        String message = smell.getMessage();
        if (message != null && message.contains("line")) {
            // Try to extract line numbers from message
            // Format: "Duplicate code block starting at line X"
            return ""; // Placeholder - would need enhanced duplicate detection
        }
        return "";
    }

    /**
     * Finds the start of a method declaration.
     */
    private int findMethodStart(String[] lines, int currentLine) {
        // Look backwards for method signature
        for (int i = currentLine; i >= 0 && i >= currentLine - 20; i--) {
            String trimmed = lines[i].trim();
            // Look for method patterns: visibility modifier + return type + methodName
            if (trimmed.matches(".*\\s+\\w+\\s*\\([^)]*\\)\\s*\\{?.*") &&
                (trimmed.contains("public") || trimmed.contains("private") ||
                 trimmed.contains("protected") || trimmed.startsWith("static"))) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Finds the end of a method (matching closing brace).
     */
    private int findMethodEnd(String[] lines, int currentLine) {
        int braceCount = 0;
        boolean foundOpenBrace = false;

        for (int i = currentLine; i < lines.length && i < currentLine + 100; i++) {
            String line = lines[i];
            for (char c : line.toCharArray()) {
                if (c == '{') {
                    braceCount++;
                    foundOpenBrace = true;
                } else if (c == '}') {
                    braceCount--;
                    if (foundOpenBrace && braceCount == 0) {
                        return i;
                    }
                }
            }
        }
        return -1;
    }

    /**
     * Extracts lines from source code with line numbers.
     */
    private String extractLines(String[] lines, int start, int end, int highlightLine) {
        StringBuilder snippet = new StringBuilder();

        for (int i = start; i <= end; i++) {
            if (i >= lines.length) break;

            int lineNum = i + 1; // Convert to 1-indexed for display
            String prefix = (lineNum == highlightLine) ? ">>> " : "    ";
            snippet.append(String.format("%s%4d | %s", prefix, lineNum, lines[i]));

            if (i < end) {
                snippet.append("\n");
            }
        }

        return snippet.toString();
    }

    /**
     * Determines if a smell type is method-related.
     */
    private boolean isMethodRelatedSmell(CodeSmellType type) {
        return type == CodeSmellType.LONG_METHOD ||
               type == CodeSmellType.HIGH_CYCLOMATIC_COMPLEXITY ||
               type == CodeSmellType.LONG_PARAMETER_LIST ||
               type == CodeSmellType.DEEPLY_NESTED_CODE ||
               type == CodeSmellType.FEATURE_ENVY ||
               type == CodeSmellType.STRING_CONCAT_IN_LOOP ||
               type == CodeSmellType.EMPTY_CATCH_BLOCK ||
               type == CodeSmellType.MESSAGE_CHAIN;
    }

    /**
     * Determines if additional context is needed for a smell type.
     */
    private boolean needsAdditionalContext(CodeSmellType type) {
        return type == CodeSmellType.DUPLICATED_CODE ||
               type == CodeSmellType.FEATURE_ENVY;
    }

    /**
     * Gets the number of context lines to show based on smell type.
     */
    private int getContextLinesForType(CodeSmellType type) {
        return switch (type) {
            case MAGIC_NUMBER, MAGIC_STRING, UNUSED_IMPORT, UNUSED_VARIABLE -> 2;
            case DEEPLY_NESTED_CODE, HIGH_CYCLOMATIC_COMPLEXITY -> 10;
            case LONG_METHOD, LARGE_CLASS, GOD_CLASS -> 15;
            default -> DEFAULT_CONTEXT_LINES;
        };
    }

    /**
     * Extracts class-level context for class-related smells.
     */
    public String extractClassContext(CodeSmell smell, String sourceCode) {
        String[] lines = sourceCode.split("\n");

        // Find class declaration
        for (int i = 0; i < Math.min(50, lines.length); i++) {
            String trimmed = lines[i].trim();
            if (trimmed.startsWith("public class") ||
                trimmed.startsWith("class") ||
                trimmed.startsWith("public interface")) {

                // Extract class signature and first few methods
                int endLine = Math.min(i + 30, lines.length - 1);
                return extractLines(lines, i, endLine, smell.getLine());
            }
        }

        return "";
    }
}

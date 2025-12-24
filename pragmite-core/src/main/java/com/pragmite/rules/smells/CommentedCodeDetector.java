package com.pragmite.rules.smells;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.comments.Comment;
import com.pragmite.model.CodeSmell;
import com.pragmite.model.CodeSmellType;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Detects commented-out code in the codebase.
 * Commented code should be removed and managed through version control instead.
 *
 * Heuristics:
 * - Contains Java keywords (if, for, while, class, public, etc.)
 * - Contains method calls with parentheses
 * - Contains variable assignments
 */
public class CommentedCodeDetector implements SmellDetector {

    private static final Pattern CODE_PATTERNS = Pattern.compile(
        "\\b(if|for|while|do|switch|class|public|private|protected|static|void|return|new|this|super)\\b|" +
        "\\w+\\s*\\(|" +
        "\\w+\\s*=\\s*[^=]"
    );

    private static final int MIN_CODE_LINES = 2; // Minimum lines to consider as commented code

    @Override
    public List<CodeSmell> detect(CompilationUnit cu, String filePath, String content) {
        List<CodeSmell> smells = new ArrayList<>();

        for (Comment comment : cu.getAllComments()) {
            String commentText = comment.getContent();
            if (looksLikeCode(commentText)) {
                CodeSmell smell = new CodeSmell(
                    CodeSmellType.DEAD_CODE,
                    filePath,
                    comment.getBegin().get().line,
                    "Commented-out code detected. Remove and use version control instead."
                );
                smell.withSuggestion("Delete commented code and rely on version control history")
                    .withAutoFix(true);
                smells.add(smell);
            }
        }

        return smells;
    }

    private boolean looksLikeCode(String commentText) {
        // Skip JavaDoc comments
        if (commentText.trim().startsWith("*")) {
            return false;
        }

        String[] lines = commentText.split("\n");
        int codeLineCount = 0;

        for (String line : lines) {
            String trimmed = line.trim().replaceAll("^[/*\\s]+", "");
            if (trimmed.isEmpty()) continue;

            // Skip typical comment text
            if (trimmed.startsWith("TODO") || trimmed.startsWith("FIXME") ||
                trimmed.startsWith("NOTE") || trimmed.startsWith("@")) {
                continue;
            }

            if (CODE_PATTERNS.matcher(trimmed).find()) {
                codeLineCount++;
            }
        }

        return codeLineCount >= MIN_CODE_LINES;
    }
}

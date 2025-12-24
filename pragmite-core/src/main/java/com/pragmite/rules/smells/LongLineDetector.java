package com.pragmite.rules.smells;

import com.github.javaparser.ast.CompilationUnit;
import com.pragmite.model.CodeSmell;
import com.pragmite.model.CodeSmellType;

import java.util.ArrayList;
import java.util.List;

/**
 * Detects excessively long lines of code.
 * Long lines reduce readability and make code harder to review.
 *
 * Threshold: 120 characters (industry standard)
 */
public class LongLineDetector implements SmellDetector {

    private static final int MAX_LINE_LENGTH = 120;

    @Override
    public List<CodeSmell> detect(CompilationUnit cu, String filePath, String content) {
        List<CodeSmell> smells = new ArrayList<>();

        String[] lines = content.split("\n");
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            int lineNumber = i + 1;

            // Skip lines that are just comments or strings
            String trimmed = line.trim();
            if (trimmed.startsWith("//") || trimmed.startsWith("*") || trimmed.startsWith("/*")) {
                continue;
            }

            if (line.length() > MAX_LINE_LENGTH) {
                // Count actual code length (excluding leading whitespace)
                int codeLength = line.length() - (line.length() - line.trim().length());

                CodeSmell smell = new CodeSmell(
                    CodeSmellType.DEEPLY_NESTED_CODE, // Reusing closest related type
                    filePath,
                    lineNumber,
                    String.format("Line is too long (%d characters, max %d). Consider breaking into multiple lines.",
                                line.length(), MAX_LINE_LENGTH)
                );
                smell.withSuggestion("Break long lines at logical points (operators, parameters, method chains)")
                    .withAutoFix(false);
                smells.add(smell);
            }
        }

        return smells;
    }
}

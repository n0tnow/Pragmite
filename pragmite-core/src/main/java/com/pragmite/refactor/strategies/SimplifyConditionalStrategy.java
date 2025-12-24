package com.pragmite.refactor.strategies;

import com.pragmite.model.CodeSmell;
import com.pragmite.model.CodeSmellType;
import com.pragmite.refactor.RefactoringStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Simplifies complex boolean expressions by extracting them into well-named variables.
 * Makes conditional logic more readable and maintainable.
 *
 * Example:
 * Before: if (user.getAge() > 18 && user.hasLicense() && !user.isSuspended())
 * After:  boolean canDrive = user.getAge() > 18 && user.hasLicense() && !user.isSuspended();
 *         if (canDrive)
 */
public class SimplifyConditionalStrategy implements RefactoringStrategy {
    private static final Logger logger = LoggerFactory.getLogger(SimplifyConditionalStrategy.class);

    private static final Pattern IF_PATTERN = Pattern.compile(
        "if\\s*\\((.+)\\)\\s*\\{?");

    @Override
    public String getName() {
        return "Simplify Conditional Expression";
    }

    @Override
    public boolean canHandle(CodeSmell smell) {
        // Can handle complex boolean expressions or deeply nested code
        return smell.getType() == CodeSmellType.DEEPLY_NESTED_CODE ||
               smell.getDescription().toLowerCase().contains("complex boolean");
    }

    @Override
    public boolean validate(CodeSmell smell) throws Exception {
        Path filePath = Paths.get(smell.getFilePath());

        if (!Files.exists(filePath)) {
            return false;
        }

        if (!Files.isWritable(filePath)) {
            return false;
        }

        return true;
    }

    @Override
    public String apply(CodeSmell smell) throws Exception {
        Path filePath = Paths.get(smell.getFilePath());
        List<String> lines = Files.readAllLines(filePath);

        int lineIndex = smell.getStartLine() - 1;

        if (lineIndex < 0 || lineIndex >= lines.size()) {
            throw new IllegalArgumentException("Invalid line number: " + smell.getStartLine());
        }

        String line = lines.get(lineIndex);
        Matcher matcher = IF_PATTERN.matcher(line.trim());

        if (!matcher.find()) {
            throw new IllegalStateException("Could not find if statement at line " + smell.getStartLine());
        }

        String condition = matcher.group(1);

        // Generate a descriptive variable name based on context
        String variableName = generateVariableName(condition, smell.getAffectedElement());

        // Get indentation
        String indent = getIndentation(line);

        // Create boolean variable declaration
        String booleanDeclaration = indent + "boolean " + variableName + " = " + condition + ";";

        // Simplify the if statement
        String simplifiedIf = line.replaceFirst(
            Pattern.quote(condition),
            variableName
        );

        // Insert boolean variable before if statement
        lines.add(lineIndex, booleanDeclaration);
        lines.set(lineIndex + 1, simplifiedIf);

        // Write back
        Files.write(filePath, lines);

        logger.info("Simplified conditional at {}:{} with variable '{}'",
            filePath.getFileName(), smell.getStartLine(), variableName);

        return String.format("Extracted complex condition into variable '%s'", variableName);
    }

    @Override
    public String getDescription() {
        return "Simplifies complex boolean expressions by extracting them into well-named variables";
    }

    /**
     * Generates a descriptive boolean variable name from the condition.
     */
    private String generateVariableName(String condition, String context) {
        // Remove common noise words
        String cleaned = condition.toLowerCase()
            .replaceAll("\\s+", "")
            .replaceAll("[()!]", "");

        // Try to infer meaning
        if (cleaned.contains("valid")) return "isValid";
        if (cleaned.contains("empty") || cleaned.contains("null")) return "isEmpty";
        if (cleaned.contains("ready")) return "isReady";
        if (cleaned.contains("active")) return "isActive";
        if (cleaned.contains("enabled")) return "isEnabled";
        if (cleaned.contains("authorized") || cleaned.contains("permission")) return "isAuthorized";
        if (cleaned.contains("age") && cleaned.contains(">")) return "isAdult";
        if (cleaned.contains("length") || cleaned.contains("size")) return "hasValidSize";

        // Generic fallback
        if (context != null && !context.isEmpty()) {
            return "should" + capitalize(context);
        }

        return "condition";
    }

    /**
     * Capitalizes first letter of a string.
     */
    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    /**
     * Gets the indentation of a line.
     */
    private String getIndentation(String line) {
        int count = 0;
        for (char c : line.toCharArray()) {
            if (c == ' ' || c == '\t') {
                count++;
            } else {
                break;
            }
        }
        return line.substring(0, count);
    }
}

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
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Introduces try-with-resources for AutoCloseable resources.
 * Converts manual resource management to automatic resource management.
 */
public class IntroduceTryWithResourcesStrategy implements RefactoringStrategy {
    private static final Logger logger = LoggerFactory.getLogger(IntroduceTryWithResourcesStrategy.class);

    private static final Pattern RESOURCE_DECLARATION = Pattern.compile(
        "(\\w+(?:<[^>]+>)?)\\s+(\\w+)\\s*=\\s*new\\s+\\w+.*?;");

    @Override
    public String getName() {
        return "Introduce Try-With-Resources";
    }

    @Override
    public boolean canHandle(CodeSmell smell) {
        return smell.getType() == CodeSmellType.MISSING_TRY_WITH_RESOURCES;
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

        int startLine = smell.getStartLine() - 1; // 0-based index
        int endLine = smell.getEndLine() > 0 ? smell.getEndLine() - 1 : startLine;

        if (startLine < 0 || endLine >= lines.size()) {
            throw new IllegalArgumentException("Invalid line range: " + smell.getStartLine() + "-" + smell.getEndLine());
        }

        // Extract resource declaration
        String resourceLine = lines.get(startLine).trim();
        Matcher matcher = RESOURCE_DECLARATION.matcher(resourceLine);

        if (!matcher.find()) {
            throw new IllegalStateException("Could not parse resource declaration: " + resourceLine);
        }

        String resourceType = matcher.group(1);
        String resourceName = matcher.group(2);

        // Find the try block or code block that uses the resource
        int tryBlockStart = findTryBlockOrUsage(lines, startLine, resourceName);
        int tryBlockEnd = findBlockEnd(lines, tryBlockStart);

        if (tryBlockStart == -1 || tryBlockEnd == -1) {
            throw new IllegalStateException("Could not find try block or usage for resource: " + resourceName);
        }

        // Build the new try-with-resources block
        List<String> newLines = new ArrayList<>();

        // Lines before the resource declaration
        for (int i = 0; i < startLine; i++) {
            newLines.add(lines.get(i));
        }

        // Add try-with-resources statement
        String indent = getIndentation(lines.get(startLine));
        newLines.add(indent + "try (" + resourceLine + ") {");

        // Add the code inside try block (skip old try keyword if exists)
        for (int i = tryBlockStart + 1; i < tryBlockEnd; i++) {
            String line = lines.get(i);
            // Skip old close() calls
            if (!line.contains(resourceName + ".close()")) {
                newLines.add("    " + line); // Add extra indentation
            }
        }

        newLines.add(indent + "}");

        // Lines after the try block
        for (int i = tryBlockEnd + 1; i < lines.size(); i++) {
            newLines.add(lines.get(i));
        }

        // Write back
        Files.write(filePath, newLines);

        logger.info("Introduced try-with-resources for {} in {}",
            resourceName, filePath.getFileName());

        return String.format("Introduced try-with-resources for resource: %s", resourceName);
    }

    @Override
    public String getDescription() {
        return "Converts manual resource management to try-with-resources for AutoCloseable types";
    }

    /**
     * Finds the try block or first usage of the resource.
     */
    private int findTryBlockOrUsage(List<String> lines, int startLine, String resourceName) {
        for (int i = startLine + 1; i < lines.size(); i++) {
            String line = lines.get(i).trim();

            if (line.startsWith("try {") || line.startsWith("try{")) {
                return i;
            }

            if (line.contains(resourceName + ".")) {
                // Found usage, we'll create a try block here
                return i - 1;
            }
        }
        return -1;
    }

    /**
     * Finds the end of a code block (matching closing brace).
     */
    private int findBlockEnd(List<String> lines, int startLine) {
        int braceCount = 0;
        boolean started = false;

        for (int i = startLine; i < lines.size(); i++) {
            String line = lines.get(i);

            for (char c : line.toCharArray()) {
                if (c == '{') {
                    braceCount++;
                    started = true;
                } else if (c == '}') {
                    braceCount--;
                    if (started && braceCount == 0) {
                        return i;
                    }
                }
            }
        }

        return -1;
    }

    /**
     * Gets the indentation (leading whitespace) of a line.
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

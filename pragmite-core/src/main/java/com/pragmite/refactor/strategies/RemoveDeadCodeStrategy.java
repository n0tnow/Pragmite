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

/**
 * Removes unreachable or unused code blocks.
 * Safely identifies and removes dead code that cannot be executed.
 */
public class RemoveDeadCodeStrategy implements RefactoringStrategy {
    private static final Logger logger = LoggerFactory.getLogger(RemoveDeadCodeStrategy.class);

    @Override
    public String getName() {
        return "Remove Dead Code";
    }

    @Override
    public boolean canHandle(CodeSmell smell) {
        return smell.getType() == CodeSmellType.DEAD_CODE;
    }

    @Override
    public boolean validate(CodeSmell smell) throws Exception {
        Path filePath = Paths.get(smell.getFilePath());

        if (!Files.exists(filePath)) {
            logger.warn("File does not exist: {}", filePath);
            return false;
        }

        if (!Files.isWritable(filePath)) {
            logger.warn("File is not writable: {}", filePath);
            return false;
        }

        return true;
    }

    @Override
    public String apply(CodeSmell smell) throws Exception {
        Path filePath = Paths.get(smell.getFilePath());
        List<String> lines = Files.readAllLines(filePath);

        int startLine = smell.getStartLine() - 1; // 0-based
        int endLine = smell.getEndLine() > 0 ? smell.getEndLine() - 1 : startLine;

        if (startLine < 0 || endLine >= lines.size()) {
            throw new IllegalArgumentException("Invalid line range: " + smell.getStartLine() + "-" + smell.getEndLine());
        }

        // Create new list without dead code lines
        List<String> newLines = new ArrayList<>();

        for (int i = 0; i < lines.size(); i++) {
            // Skip lines in the dead code range
            if (i < startLine || i > endLine) {
                newLines.add(lines.get(i));
            }
        }

        // Write back to file
        Files.write(filePath, newLines);

        int removedLines = endLine - startLine + 1;
        logger.info("Removed {} lines of dead code from {}:{}",
            removedLines, filePath.getFileName(), smell.getStartLine());

        return String.format("Removed %d lines of dead code", removedLines);
    }

    @Override
    public String getDescription() {
        return "Removes unreachable or unused code blocks";
    }
}

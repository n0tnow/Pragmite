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
 * Removes unused import statements from Java files.
 * Safely identifies and removes import lines that are not referenced in the code.
 */
public class RemoveUnusedImportsStrategy implements RefactoringStrategy {
    private static final Logger logger = LoggerFactory.getLogger(RemoveUnusedImportsStrategy.class);

    @Override
    public String getName() {
        return "Remove Unused Imports";
    }

    @Override
    public boolean canHandle(CodeSmell smell) {
        return smell.getType() == CodeSmellType.UNUSED_IMPORT;
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

        int lineIndex = smell.getStartLine() - 1; // Convert to 0-based index

        if (lineIndex < 0 || lineIndex >= lines.size()) {
            throw new IllegalArgumentException("Invalid line number: " + smell.getStartLine());
        }

        String lineToRemove = lines.get(lineIndex);

        // Verify this is actually an import line
        if (!lineToRemove.trim().startsWith("import ")) {
            throw new IllegalStateException(
                "Line " + smell.getStartLine() + " is not an import statement: " + lineToRemove);
        }

        // Remove the import line
        List<String> newLines = new ArrayList<>();
        for (int i = 0; i < lines.size(); i++) {
            if (i != lineIndex) {
                newLines.add(lines.get(i));
            }
        }

        // Write back to file
        Files.write(filePath, newLines);

        logger.info("Removed unused import from {}:{}", filePath.getFileName(), smell.getStartLine());

        return String.format("Removed unused import: %s", lineToRemove.trim());
    }

    @Override
    public String getDescription() {
        return "Removes import statements that are not used in the code";
    }
}

package com.pragmite.refactor.strategies;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.NameExpr;
import com.pragmite.model.CodeSmell;
import com.pragmite.model.CodeSmellType;
import com.pragmite.naming.SmartNamingEngine;
import com.pragmite.refactor.RefactoringStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * Smart Rename Strategy - Automatically renames poorly named variables.
 *
 * Features:
 * - Uses SmartNamingEngine to generate meaningful names
 * - Finds all references and renames them consistently
 * - Preserves scope boundaries
 * - Generates report of changes
 * - Applies changes directly to the file
 */
@SuppressWarnings("unchecked")
public class SmartRenameStrategy implements RefactoringStrategy {
    private static final Logger logger = LoggerFactory.getLogger(SmartRenameStrategy.class);
    private final SmartNamingEngine namingEngine;

    public SmartRenameStrategy() {
        this.namingEngine = new SmartNamingEngine();
    }

    @Override
    public String getName() {
        return "Smart Rename (Auto-Apply)";
    }

    @Override
    public boolean canHandle(CodeSmell smell) {
        return smell.getType() == CodeSmellType.UNUSED_VARIABLE ||
               smell.getMessage().toLowerCase().contains("naming") ||
               smell.getMessage().toLowerCase().contains("hungarian");
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
        String sourceCode = Files.readString(filePath);
        CompilationUnit cu = StaticJavaParser.parse(sourceCode);

        int renameCount = 0;
        StringBuilder report = new StringBuilder();

        // Find all poorly named variables
        for (VariableDeclarator var : cu.findAll(VariableDeclarator.class)) {
            String currentName = var.getNameAsString();
            String type = var.getTypeAsString();

            // Get naming suggestions
            List<SmartNamingEngine.NamingSuggestion> suggestions =
                namingEngine.suggestVariableName(currentName, type, getContext(var), cu);

            if (!suggestions.isEmpty()) {
                // Take the best suggestion
                SmartNamingEngine.NamingSuggestion bestSuggestion = suggestions.get(0);
                String newName = bestSuggestion.getSuggestedName();

                // Skip if name is same
                if (currentName.equals(newName)) {
                    continue;
                }

                // Rename all references
                boolean renamed = renameVariable(cu, var, currentName, newName);

                if (renamed) {
                    renameCount++;
                    report.append(String.format("Renamed '%s' → '%s' (%s)%n",
                                               currentName,
                                               newName,
                                               bestSuggestion.getReason()));
                    logger.info("Renamed variable: {} → {}", currentName, newName);
                }
            }
        }

        if (renameCount > 0) {
            // Write refactored code back to file
            Files.writeString(filePath, cu.toString());
            logger.info("Applied {} renames to {}", renameCount, filePath.getFileName());
            return String.format("Successfully renamed %d variable(s):%n%s", renameCount, report.toString());
        } else {
            return "No variables needed renaming";
        }
    }

    private String getContext(VariableDeclarator var) {
        // Get surrounding code as context
        return var.getParentNode()
            .map(Object::toString)
            .orElse("");
    }

    private boolean renameVariable(CompilationUnit cu, VariableDeclarator var,
                                   String oldName, String newName) {
        try {
            // Rename the declaration
            var.setName(newName);

            // Find and rename all usages within the same scope
            var.findAncestor(com.github.javaparser.ast.body.MethodDeclaration.class).ifPresent(method -> {
                method.findAll(NameExpr.class).forEach(nameExpr -> {
                    if (nameExpr.getNameAsString().equals(oldName)) {
                        nameExpr.setName(newName);
                    }
                });
            });

            return true;
        } catch (Exception e) {
            logger.error("Failed to rename {} to {}: {}", oldName, newName, e.getMessage());
            return false;
        }
    }

    @Override
    public String getDescription() {
        return "Smart Rename: Automatically renames poorly named variables with meaningful alternatives";
    }
}

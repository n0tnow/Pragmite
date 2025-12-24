package com.pragmite.refactor.strategies;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.pragmite.model.CodeSmell;
import com.pragmite.model.CodeSmellType;
import com.pragmite.refactor.RefactoringStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * Extracts code blocks from long methods into separate methods.
 * Reduces method length and improves readability.
 */
public class ExtractMethodStrategy implements RefactoringStrategy {

    private static final Logger logger = LoggerFactory.getLogger(ExtractMethodStrategy.class);
    private static final int MIN_EXTRACT_LINES = 5;
    private static final int LONG_METHOD_THRESHOLD = 20;

    @Override
    public String getName() {
        return "Extract Method";
    }

    @Override
    public boolean canHandle(CodeSmell smell) {
        return smell.getType() == CodeSmellType.LONG_METHOD ||
               smell.getType() == CodeSmellType.DEEPLY_NESTED_CODE;
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
        CompilationUnit cu = com.github.javaparser.StaticJavaParser.parse(sourceCode);

        int extractableBlocks = 0;
        StringBuilder report = new StringBuilder();

        for (MethodDeclaration method : cu.findAll(MethodDeclaration.class)) {
            if (shouldExtractFromMethod(method)) {
                int blocks = countExtractableBlocks(method);
                if (blocks > 0) {
                    extractableBlocks += blocks;
                    report.append(String.format("Found %d extractable block(s) in method '%s'%n",
                                               blocks, method.getNameAsString()));
                }
            }
        }

        if (extractableBlocks > 0) {
            logger.info("Identified {} extractable code blocks in {}", extractableBlocks, filePath.getFileName());
            return String.format("Identified %d code block(s) that could be extracted into separate methods. " +
                               "Manual extraction recommended.%n%s", extractableBlocks, report.toString());
        } else {
            return "No suitable code blocks found for extraction";
        }
    }

    private boolean shouldExtractFromMethod(MethodDeclaration method) {
        if (!method.getBody().isPresent()) return false;
        BlockStmt body = method.getBody().get();
        return body.getStatements().size() >= LONG_METHOD_THRESHOLD;
    }

    private int countExtractableBlocks(MethodDeclaration method) {
        if (!method.getBody().isPresent()) return 0;

        BlockStmt body = method.getBody().get();
        List<Statement> statements = body.getStatements();

        int extractableBlocks = 0;
        int consecutiveCount = 0;

        for (Statement stmt : statements) {
            if (isSimpleStatement(stmt)) {
                if (consecutiveCount >= MIN_EXTRACT_LINES) {
                    extractableBlocks++;
                }
                consecutiveCount = 0;
            } else {
                consecutiveCount++;
            }
        }

        // Check last block
        if (consecutiveCount >= MIN_EXTRACT_LINES) {
            extractableBlocks++;
        }

        return extractableBlocks;
    }

    private boolean isSimpleStatement(Statement stmt) {
        String stmtStr = stmt.toString().trim();
        return stmtStr.split("\n").length <= 2;
    }

    @Override
    public String getDescription() {
        return "Extract Method: Identifies code blocks in long methods that could be extracted";
    }
}

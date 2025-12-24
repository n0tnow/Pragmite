package com.pragmite.refactor.strategies;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.pragmite.model.CodeSmell;
import com.pragmite.model.CodeSmellType;
import com.pragmite.refactor.RefactoringStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Identifies trivial methods that only delegate to another method or return a simple value.
 * These methods add unnecessary indirection and could be inlined.
 */
public class InlineMethodStrategy implements RefactoringStrategy {

    private static final Logger logger = LoggerFactory.getLogger(InlineMethodStrategy.class);

    @Override
    public String getName() {
        return "Inline Method";
    }

    @Override
    public boolean canHandle(CodeSmell smell) {
        return smell.getType() == CodeSmellType.LAZY_CLASS ||
               smell.getType() == CodeSmellType.MIDDLE_MAN ||
               smell.getType() == CodeSmellType.SPECULATIVE_GENERALITY;
    }

    @Override
    public boolean validate(CodeSmell smell) throws Exception {
        Path filePath = Paths.get(smell.getFilePath());

        if (!Files.exists(filePath)) {
            logger.warn("File does not exist: {}", filePath);
            return false;
        }

        return true;
    }

    @Override
    public String apply(CodeSmell smell) throws Exception {
        Path filePath = Paths.get(smell.getFilePath());
        String sourceCode = Files.readString(filePath);
        CompilationUnit cu = com.github.javaparser.StaticJavaParser.parse(sourceCode);

        StringBuilder report = new StringBuilder();
        int inlineCandidates = 0;

        for (MethodDeclaration method : cu.findAll(MethodDeclaration.class)) {
            if (shouldInlineMethod(method)) {
                inlineCandidates++;
                report.append(String.format("Method '%s' at line %d is a trivial delegation candidate for inlining%n",
                                          method.getNameAsString(),
                                          method.getBegin().get().line));
            }
        }

        if (inlineCandidates > 0) {
            logger.info("Found {} method(s) that could be inlined in {}", inlineCandidates, filePath.getFileName());
            return String.format("Found %d trivial method(s) that could be inlined:%n%s" +
                               "Note: Inline carefully - ensure method is only called from a few places",
                               inlineCandidates, report.toString());
        } else {
            return "No trivial methods found for inlining";
        }
    }

    private boolean shouldInlineMethod(MethodDeclaration method) {
        if (!method.getBody().isPresent()) return false;

        BlockStmt body = method.getBody().get();

        // Only inline very simple methods (1-2 statements)
        if (body.getStatements().size() > 2) return false;
        if (body.getStatements().isEmpty()) return false;

        // Check if it's a simple return statement
        if (body.getStatements().size() == 1 &&
            body.getStatements().get(0) instanceof ReturnStmt) {
            ReturnStmt returnStmt = (ReturnStmt) body.getStatements().get(0);
            return returnStmt.getExpression().isPresent();
        }

        // Check if it's a simple getter/setter pattern
        String methodName = method.getNameAsString();
        if (methodName.startsWith("get") || methodName.startsWith("is") || methodName.startsWith("set")) {
            return body.getStatements().size() <= 2;
        }

        return false;
    }

    @Override
    public String getDescription() {
        return "Inline Method: Identifies trivial methods that only delegate or return simple values";
    }
}

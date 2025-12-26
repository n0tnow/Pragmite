package com.pragmite.autofix;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.pragmite.ai.RefactoredCode;
import com.pragmite.model.CodeSmell;
import com.pragmite.model.CodeSmellType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

/**
 * AST-based code replacement engine.
 *
 * Intelligently replaces code using Abstract Syntax Tree (AST) matching
 * instead of simple text replacement. This ensures:
 * - Accurate location matching
 * - Preserved formatting and structure
 * - Safe refactoring
 *
 * Version: v0.4
 */
public class ASTReplacer {
    private static final Logger logger = LoggerFactory.getLogger(ASTReplacer.class);

    /**
     * Replace matching AST nodes in original with refactored nodes.
     *
     * @param original Original compilation unit
     * @param refactored Refactored compilation unit
     * @param refactoredCode Context about the refactoring
     * @return true if replacement succeeded
     */
    public boolean replace(CompilationUnit original, CompilationUnit refactored, RefactoredCode refactoredCode) {
        CodeSmell smell = getSmellFromContext(refactoredCode);

        if (smell == null) {
            logger.warn("No code smell context available for replacement");
            return replaceByStructure(original, refactored);
        }

        // Strategy 1: Replace by method signature (for method-level smells)
        if (isMethodLevelSmell(smell.getType()) && smell.getMethodName() != null) {
            logger.debug("Using method replacement strategy for {}", smell.getMethodName());
            return replaceMethod(original, refactored, smell);
        }

        // Strategy 2: Replace by line number location
        if (smell.getLineNumber() > 0) {
            logger.debug("Using location-based replacement at line {}", smell.getLineNumber());
            return replaceByLocation(original, refactored, smell);
        }

        // Strategy 3: Replace by AST structure similarity
        logger.debug("Using structure-based replacement");
        return replaceByStructure(original, refactored);
    }

    /**
     * Strategy 1: Replace entire method by signature.
     * Best for: LONG_METHOD, HIGH_COMPLEXITY, FEATURE_ENVY
     */
    private boolean replaceMethod(CompilationUnit original, CompilationUnit refactored, CodeSmell smell) {
        String methodName = smell.getMethodName();
        String className = smell.getClassName();

        // Find class in original AST
        Optional<ClassOrInterfaceDeclaration> originalClass = findClass(original, className);
        if (originalClass.isEmpty()) {
            logger.warn("Could not find class {} in original AST", className);
            return false;
        }

        // Find method in original class
        Optional<MethodDeclaration> originalMethod = originalClass.get().getMethodsByName(methodName).stream()
            .findFirst();

        if (originalMethod.isEmpty()) {
            logger.warn("Could not find method {} in class {}", methodName, className);
            return false;
        }

        // Find class in refactored AST
        Optional<ClassOrInterfaceDeclaration> refactoredClass = findClass(refactored, className);
        if (refactoredClass.isEmpty()) {
            logger.warn("Could not find class {} in refactored AST", className);
            return false;
        }

        // Find method in refactored class
        Optional<MethodDeclaration> refactoredMethod = refactoredClass.get().getMethodsByName(methodName).stream()
            .findFirst();

        if (refactoredMethod.isEmpty()) {
            logger.warn("Could not find method {} in refactored class {}", methodName, className);
            return false;
        }

        // Replace method body
        if (originalMethod.get().getBody().isPresent() && refactoredMethod.get().getBody().isPresent()) {
            originalMethod.get().setBody(refactoredMethod.get().getBody().get());
            logger.info("Replaced method {} in class {}", methodName, className);
            return true;
        }

        logger.warn("Method body missing in original or refactored code");
        return false;
    }

    /**
     * Strategy 2: Replace code at specific line number.
     * Best for: MAGIC_NUMBER, DEEP_NESTING, STRING_CONCAT_IN_LOOP
     */
    private boolean replaceByLocation(CompilationUnit original, CompilationUnit refactored, CodeSmell smell) {
        int lineNumber = smell.getLineNumber();

        // Find statement at line number in original
        Optional<Statement> originalStmt = findStatementAtLine(original, lineNumber);
        if (originalStmt.isEmpty()) {
            logger.warn("Could not find statement at line {} in original AST", lineNumber);
            return replaceByStructure(original, refactored);
        }

        // Find corresponding statement in refactored code
        // For magic numbers, we need to find the constant declaration and the usage
        if (smell.getType() == CodeSmellType.MAGIC_NUMBER) {
            return replaceMagicNumber(original, refactored, smell, originalStmt.get());
        }

        // For other types, try to find matching statement
        Optional<Statement> refactoredStmt = findFirstStatement(refactored);
        if (refactoredStmt.isEmpty()) {
            logger.warn("Could not find replacement statement in refactored AST");
            return false;
        }

        // Replace the statement
        return replaceStatement(originalStmt.get(), refactoredStmt.get());
    }

    /**
     * Strategy 3: Replace by AST structure similarity.
     * Fallback strategy when we can't identify exact location.
     */
    private boolean replaceByStructure(CompilationUnit original, CompilationUnit refactored) {
        // Find the first class in both ASTs
        Optional<ClassOrInterfaceDeclaration> originalClass = original.findFirst(ClassOrInterfaceDeclaration.class);
        Optional<ClassOrInterfaceDeclaration> refactoredClass = refactored.findFirst(ClassOrInterfaceDeclaration.class);

        if (originalClass.isEmpty() || refactoredClass.isEmpty()) {
            logger.warn("Could not find class declarations for structure-based replacement");
            return false;
        }

        // Replace entire class body (dangerous, but better than nothing)
        try {
            // Get all members from refactored class
            List<?> refactoredMembers = refactoredClass.get().getMembers();

            // Clear original class members
            originalClass.get().getMembers().clear();

            // Add refactored members
            for (Object member : refactoredMembers) {
                if (member instanceof Node) {
                    originalClass.get().addMember((com.github.javaparser.ast.body.BodyDeclaration<?>) member);
                }
            }

            logger.info("Replaced class structure (fallback strategy)");
            return true;

        } catch (Exception e) {
            logger.error("Structure-based replacement failed", e);
            return false;
        }
    }

    /**
     * Special handling for magic number replacement.
     */
    private boolean replaceMagicNumber(CompilationUnit original, CompilationUnit refactored,
                                      CodeSmell smell, Statement originalStmt) {
        // Extract constant declaration from refactored code
        Optional<ClassOrInterfaceDeclaration> refactoredClass =
            refactored.findFirst(ClassOrInterfaceDeclaration.class);

        if (refactoredClass.isEmpty()) {
            return false;
        }

        // Add constant declaration to original class
        Optional<ClassOrInterfaceDeclaration> originalClass =
            findClass(original, smell.getClassName());

        if (originalClass.isEmpty()) {
            return false;
        }

        // Find field declarations in refactored class (the constant)
        refactoredClass.get().getFields().forEach(field -> {
            // Add to original class if not already present
            String fieldName = field.getVariables().get(0).getNameAsString();
            boolean exists = originalClass.get().getFields().stream()
                .anyMatch(f -> f.getVariables().get(0).getNameAsString().equals(fieldName));

            if (!exists) {
                originalClass.get().addMember(field);
                logger.info("Added constant field: {}", fieldName);
            }
        });

        // Replace the usage (statement with magic number)
        Optional<Statement> refactoredStmt = findFirstStatement(refactored);
        if (refactoredStmt.isPresent()) {
            return replaceStatement(originalStmt, refactoredStmt.get());
        }

        return true; // At least we added the constant
    }

    /**
     * Replace one statement with another in the AST.
     */
    private boolean replaceStatement(Statement original, Statement replacement) {
        try {
            original.replace(replacement.clone());
            logger.debug("Replaced statement successfully");
            return true;
        } catch (Exception e) {
            logger.error("Failed to replace statement", e);
            return false;
        }
    }

    /**
     * Find class by name in compilation unit.
     */
    private Optional<ClassOrInterfaceDeclaration> findClass(CompilationUnit cu, String className) {
        if (className == null) {
            return cu.findFirst(ClassOrInterfaceDeclaration.class);
        }

        return cu.findFirst(ClassOrInterfaceDeclaration.class,
            c -> c.getNameAsString().equals(className));
    }

    /**
     * Find statement at specific line number.
     */
    private Optional<Statement> findStatementAtLine(CompilationUnit cu, int lineNumber) {
        return cu.findFirst(Statement.class, stmt ->
            stmt.getBegin().isPresent() &&
            stmt.getBegin().get().line == lineNumber
        );
    }

    /**
     * Find first statement in compilation unit.
     */
    private Optional<Statement> findFirstStatement(CompilationUnit cu) {
        return cu.findFirst(Statement.class);
    }

    /**
     * Check if smell type is method-level.
     */
    private boolean isMethodLevelSmell(CodeSmellType type) {
        return type == CodeSmellType.LONG_METHOD ||
               type == CodeSmellType.HIGH_COMPLEXITY ||
               type == CodeSmellType.FEATURE_ENVY ||
               type == CodeSmellType.LONG_PARAMETER_LIST;
    }

    /**
     * Extract CodeSmell from RefactoredCode context.
     * This is a workaround until we update RefactoredCode to include CodeSmell.
     */
    private CodeSmell getSmellFromContext(RefactoredCode refactoredCode) {
        // TODO: RefactoredCode should include CodeSmell reference
        // For now, return null and use fallback strategy
        return null;
    }
}

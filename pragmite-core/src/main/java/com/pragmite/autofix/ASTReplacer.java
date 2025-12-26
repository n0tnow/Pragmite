package com.pragmite.autofix;

import com.github.javaparser.ast.CompilationUnit;
import com.pragmite.ai.RefactoredCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * AST-based code replacement engine (v0.4 - Simplified).
 *
 * Currently uses fallback strategy (full file replacement) due to CodeSmell
 * model limitations. Future versions will implement intelligent AST matching.
 *
 * Version: v0.4-simplified
 */
public class ASTReplacer {
    private static final Logger logger = LoggerFactory.getLogger(ASTReplacer.class);

    /**
     * Replace matching AST nodes in original with refactored nodes.
     * Currently uses fallback strategy - replaces entire file.
     *
     * TODO: Implement intelligent AST matching when CodeSmell model is enhanced
     * with method names, class names, and detailed location information.
     *
     * @param original Original compilation unit
     * @param refactored Refactored compilation unit
     * @param refactoredCode Context about the refactoring
     * @return true if replacement succeeded
     */
    public boolean replace(CompilationUnit original, CompilationUnit refactored, RefactoredCode refactoredCode) {
        logger.debug("Using fallback strategy: structure-based replacement");
        return replaceByStructure(original, refactored);
    }

    /**
     * Fallback strategy: Replace by AST structure similarity.
     * Replaces the entire class structure.
     */
    private boolean replaceByStructure(CompilationUnit original, CompilationUnit refactored) {
        try {
            // Clear all types from original
            original.getTypes().clear();

            // Copy all types from refactored to original
            refactored.getTypes().forEach(type -> {
                original.addType(type);
            });

            logger.info("Replaced compilation unit using structure-based strategy");
            return true;

        } catch (Exception e) {
            logger.error("Structure-based replacement failed", e);
            return false;
        }
    }
}

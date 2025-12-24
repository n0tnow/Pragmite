package com.pragmite.rules.smells;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.pragmite.model.CodeSmell;
import com.pragmite.model.CodeSmellType;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Duplicate Code (Clone) detector.
 * Detects duplicated code blocks using token-based comparison.
 * Minimum threshold: 6 consecutive similar statements.
 */
public class DuplicateCodeDetector implements SmellDetector {

    private static final int MIN_CLONE_STATEMENTS = 6;
    private static final double SIMILARITY_THRESHOLD = 0.85; // 85% similarity

    @Override
    public List<CodeSmell> detect(CompilationUnit cu, String filePath, String content) {
        List<CodeSmell> smells = new ArrayList<>();
        List<CodeBlock> codeBlocks = new ArrayList<>();

        // Extract all code blocks from methods
        cu.accept(new VoidVisitorAdapter<Void>() {
            @Override
            public void visit(MethodDeclaration md, Void arg) {
                super.visit(md, arg);

                md.getBody().ifPresent(body -> {
                    extractCodeBlocks(body, md.getNameAsString(), codeBlocks);
                });
            }
        }, null);

        // Compare all blocks pairwise
        for (int i = 0; i < codeBlocks.size(); i++) {
            for (int j = i + 1; j < codeBlocks.size(); j++) {
                CodeBlock block1 = codeBlocks.get(i);
                CodeBlock block2 = codeBlocks.get(j);

                double similarity = calculateSimilarity(block1, block2);

                if (similarity >= SIMILARITY_THRESHOLD) {
                    CodeSmell smell = new CodeSmell(
                        CodeSmellType.DUPLICATED_CODE,
                        filePath,
                        block1.startLine,
                        String.format("Duplicated code block found (%.0f%% similar to %s at line %d)",
                            similarity * 100, block2.methodName, block2.startLine)
                    );
                    smell.withSuggestion("Extract this duplicated code into a separate method")
                         .withAutoFix(false);
                    smells.add(smell);
                }
            }
        }

        return smells;
    }

    /**
     * Extracts code blocks from a method body using sliding window approach.
     */
    private void extractCodeBlocks(BlockStmt body, String methodName, List<CodeBlock> blocks) {
        List<Statement> statements = body.getStatements();

        if (statements.size() < MIN_CLONE_STATEMENTS) {
            return;
        }

        // Sliding window to extract blocks
        for (int i = 0; i <= statements.size() - MIN_CLONE_STATEMENTS; i++) {
            List<Statement> blockStatements = statements.subList(i, i + MIN_CLONE_STATEMENTS);

            Statement firstStmt = blockStatements.get(0);
            int startLine = firstStmt.getBegin().map(pos -> pos.line).orElse(0);

            String normalized = normalizeBlock(blockStatements);

            blocks.add(new CodeBlock(methodName, startLine, normalized, blockStatements));
        }
    }

    /**
     * Normalizes a code block by removing variable names and literals.
     * This allows detection of Type-2 clones (syntactically similar with renamed identifiers).
     */
    private String normalizeBlock(List<Statement> statements) {
        StringBuilder normalized = new StringBuilder();

        for (Statement stmt : statements) {
            String stmtStr = stmt.toString();

            // Replace identifiers with placeholders
            stmtStr = stmtStr.replaceAll("\\b[a-zA-Z_][a-zA-Z0-9_]*\\b", "ID");

            // Replace string literals
            stmtStr = stmtStr.replaceAll("\"[^\"]*\"", "STR");

            // Replace numeric literals
            stmtStr = stmtStr.replaceAll("\\b\\d+\\b", "NUM");

            // Remove whitespace
            stmtStr = stmtStr.replaceAll("\\s+", " ").trim();

            normalized.append(stmtStr).append(";");
        }

        return normalized.toString();
    }

    /**
     * Calculates similarity between two code blocks using Jaccard similarity.
     */
    private double calculateSimilarity(CodeBlock block1, CodeBlock block2) {
        if (block1.methodName.equals(block2.methodName) && block1.startLine == block2.startLine) {
            return 0.0; // Same block
        }

        Set<String> tokens1 = tokenize(block1.normalized);
        Set<String> tokens2 = tokenize(block2.normalized);

        if (tokens1.isEmpty() || tokens2.isEmpty()) {
            return 0.0;
        }

        // Jaccard similarity: |A ∩ B| / |A ∪ B|
        Set<String> intersection = new HashSet<>(tokens1);
        intersection.retainAll(tokens2);

        Set<String> union = new HashSet<>(tokens1);
        union.addAll(tokens2);

        return (double) intersection.size() / union.size();
    }

    /**
     * Tokenizes a normalized code block.
     */
    private Set<String> tokenize(String normalized) {
        return Arrays.stream(normalized.split("[;\\s]+"))
            .filter(s -> !s.isEmpty())
            .collect(Collectors.toSet());
    }

    /**
     * Represents a code block for comparison.
     */
    private static class CodeBlock {
        final String methodName;
        final int startLine;
        final String normalized;
        final List<Statement> statements;

        CodeBlock(String methodName, int startLine, String normalized, List<Statement> statements) {
            this.methodName = methodName;
            this.startLine = startLine;
            this.normalized = normalized;
            this.statements = statements;
        }
    }
}

package com.pragmite.metrics;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.pragmite.analyzer.CyclomaticComplexityCalculator;

import java.util.HashMap;
import java.util.Map;

/**
 * Maintainability Index Calculator.
 *
 * The Maintainability Index (MI) is a software metric that measures how maintainable
 * (easy to understand, modify, and extend) source code is.
 *
 * Original Formula (Microsoft):
 * MI = 171 - 5.2 * ln(V) - 0.23 * CC - 16.2 * ln(LOC)
 *
 * Where:
 * - V = Halstead Volume
 * - CC = Cyclomatic Complexity
 * - LOC = Lines of Code
 *
 * MI ranges from negative infinity to 171:
 * - 85-100: High maintainability (GREEN)
 * - 65-84: Moderate maintainability (YELLOW)
 * - 0-64: Low maintainability (RED)
 * - < 0: Very low maintainability (CRITICAL)
 *
 * Normalized MI (0-100):
 * MI_norm = max(0, (MI / 171) * 100)
 *
 * Reference: "A Complexity Measure" by Coleman et al., 1994
 */
public class MaintainabilityIndexCalculator {

    private final HalsteadMetricsCalculator halsteadCalculator;
    private final CyclomaticComplexityCalculator complexityCalculator;

    public MaintainabilityIndexCalculator() {
        this.halsteadCalculator = new HalsteadMetricsCalculator();
        this.complexityCalculator = new CyclomaticComplexityCalculator();
    }

    /**
     * Calculates Maintainability Index for all methods in a compilation unit.
     */
    public Map<String, MaintainabilityIndex> calculateAll(CompilationUnit cu, String filePath) {
        Map<String, MaintainabilityIndex> indexMap = new HashMap<>();

        // First, get Halstead metrics for all methods
        Map<String, HalsteadMetrics> halsteadMetrics = halsteadCalculator.calculateAll(cu, filePath);

        cu.accept(new VoidVisitorAdapter<Void>() {
            @Override
            public void visit(MethodDeclaration md, Void arg) {
                super.visit(md, arg);

                String methodName = md.getDeclarationAsString(false, false, false);
                int line = md.getBegin().map(pos -> pos.line).orElse(0);

                // Get metrics
                HalsteadMetrics halstead = halsteadMetrics.get(methodName);
                int cyclomaticComplexity = complexityCalculator.calculate(md);
                int loc = calculateLOC(md);

                if (halstead != null) {
                    MaintainabilityIndex mi = new MaintainabilityIndex(
                        md.getNameAsString(),
                        filePath,
                        line,
                        halstead.getVolume(),
                        cyclomaticComplexity,
                        loc
                    );

                    indexMap.put(methodName, mi);
                }
            }
        }, null);

        return indexMap;
    }

    /**
     * Calculates MI for a single method.
     */
    public MaintainabilityIndex calculateForMethod(MethodDeclaration md,
                                                   HalsteadMetrics halstead,
                                                   String filePath,
                                                   int line) {
        int cyclomaticComplexity = complexityCalculator.calculate(md);
        int loc = calculateLOC(md);

        return new MaintainabilityIndex(
            md.getNameAsString(),
            filePath,
            line,
            halstead.getVolume(),
            cyclomaticComplexity,
            loc
        );
    }

    /**
     * Calculates Lines of Code for a method (physical lines).
     */
    private int calculateLOC(MethodDeclaration md) {
        int startLine = md.getBegin().map(pos -> pos.line).orElse(0);
        int endLine = md.getEnd().map(pos -> pos.line).orElse(0);

        return Math.max(1, endLine - startLine + 1);
    }
}

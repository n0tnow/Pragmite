package com.pragmite.rules.smells;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.pragmite.metrics.CKMetrics;
import com.pragmite.metrics.CKMetricsCalculator;
import com.pragmite.model.CodeSmell;
import com.pragmite.model.CodeSmellType;

import java.util.*;

/**
 * Feature Envy detector.
 * Detects methods that use more data from another class than from their own class.
 * This suggests the method should be moved to the other class (Law of Demeter violation).
 * Enhanced with CK Metrics (CBO) for better coupling analysis.
 */
public class FeatureEnvyDetector implements SmellDetector {

    private static final double ENVY_THRESHOLD = 0.60; // 60% of accesses to external class
    private final CKMetricsCalculator ckCalculator = new CKMetricsCalculator();

    @Override
    public List<CodeSmell> detect(CompilationUnit cu, String filePath, String content) {
        List<CodeSmell> smells = new ArrayList<>();

        // Calculate CK metrics for enhanced coupling detection
        Map<String, CKMetrics> ckMetrics = ckCalculator.calculateAll(cu, filePath);

        cu.accept(new VoidVisitorAdapter<Void>() {
            private String currentClass = "";
            private Set<String> currentClassFields = new HashSet<>();
            private CKMetrics currentMetrics = null;

            @Override
            public void visit(ClassOrInterfaceDeclaration cid, Void arg) {
                currentClass = cid.getNameAsString();
                currentClassFields.clear();
                currentMetrics = ckMetrics.get(currentClass);

                // Collect field names
                cid.getFields().forEach(field -> {
                    field.getVariables().forEach(var -> {
                        currentClassFields.add(var.getNameAsString());
                    });
                });

                super.visit(cid, arg);
            }

            @Override
            public void visit(MethodDeclaration md, Void arg) {
                super.visit(md, arg);

                // Count accesses to own fields vs external objects
                Map<String, Integer> accessCounts = new HashMap<>();
                int ownFieldAccesses = 0;

                // Count field accesses
                for (FieldAccessExpr fae : md.findAll(FieldAccessExpr.class)) {
                    String scope = fae.getScope().toString();

                    if (scope.equals("this")) {
                        ownFieldAccesses++;
                    } else {
                        // External object access
                        accessCounts.put(scope, accessCounts.getOrDefault(scope, 0) + 1);
                    }
                }

                // Count method calls
                for (MethodCallExpr mce : md.findAll(MethodCallExpr.class)) {
                    mce.getScope().ifPresent(scope -> {
                        String scopeStr = scope.toString();

                        if (!scopeStr.equals("this") && !scopeStr.equals("super")) {
                            // Check if it's a call on a field (external object)
                            if (currentClassFields.contains(scopeStr) ||
                                scopeStr.matches("^[a-z][a-zA-Z0-9]*$")) { // local variable or field
                                accessCounts.put(scopeStr, accessCounts.getOrDefault(scopeStr, 0) + 1);
                            }
                        }
                    });
                }

                // Analyze envy
                int totalAccesses = ownFieldAccesses + accessCounts.values().stream().mapToInt(Integer::intValue).sum();

                if (totalAccesses >= 5) { // Minimum threshold to reduce noise
                    for (Map.Entry<String, Integer> entry : accessCounts.entrySet()) {
                        String enviedObject = entry.getKey();
                        int enviedAccesses = entry.getValue();

                        double envyRatio = (double) enviedAccesses / totalAccesses;

                        if (envyRatio >= ENVY_THRESHOLD) {
                            int line = md.getBegin().map(pos -> pos.line).orElse(0);

                            // Enhance with CK metrics context
                            String cboInfo = "";

                            if (currentMetrics != null) {
                                int cbo = currentMetrics.getCbo();
                                cboInfo = String.format(" [Class CBO: %d]", cbo);

                                // Add warning if class already has high coupling
                                if (currentMetrics.isHighlyCoupled()) {
                                    cboInfo += " - WARNING: Class already has high coupling!";
                                }
                            }

                            CodeSmell smell = new CodeSmell(
                                CodeSmellType.FEATURE_ENVY,
                                filePath,
                                line,
                                String.format("Method '%s' uses '%s' more than its own class (%.0f%% of accesses)%s",
                                    md.getNameAsString(), enviedObject, envyRatio * 100, cboInfo)
                            );
                            smell.withSuggestion(
                                String.format("Consider moving this method to the class that owns '%s' or refactoring to reduce coupling",
                                    enviedObject)
                            ).withAutoFix(false);

                            smells.add(smell);
                        }
                    }
                }
            }
        }, null);

        return smells;
    }
}

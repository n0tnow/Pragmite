package com.pragmite.rules.smells;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.pragmite.metrics.CKMetrics;
import com.pragmite.metrics.CKMetricsCalculator;
import com.pragmite.model.CodeSmell;
import com.pragmite.model.CodeSmellType;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * God Class (Tanrı Sınıf) kokusu dedektörü.
 * Çok fazla alan, metot ve dış bağımlılığı olan sınıfları tespit eder.
 * Enhanced with CK Metrics (WMC, CBO, LCOM) for accurate detection.
 */
public class GodClassDetector implements SmellDetector {

    private static final int FIELD_THRESHOLD = 15;
    private static final int METHOD_THRESHOLD = 20;
    private static final int COUPLING_THRESHOLD = 10;

    private final CKMetricsCalculator ckCalculator = new CKMetricsCalculator();

    @Override
    public List<CodeSmell> detect(CompilationUnit cu, String filePath, String content) {
        List<CodeSmell> smells = new ArrayList<>();

        // Calculate CK metrics for enhanced detection
        Map<String, CKMetrics> ckMetrics = ckCalculator.calculateAll(cu, filePath);

        cu.accept(new VoidVisitorAdapter<Void>() {
            @Override
            public void visit(ClassOrInterfaceDeclaration cid, Void arg) {
                super.visit(cid, arg);

                if (cid.isInterface()) return;

                String className = cid.getNameAsString();
                int fieldCount = cid.getFields().stream()
                    .mapToInt(f -> f.getVariables().size())
                    .sum();
                int methodCount = cid.getMethods().size();

                // Get CK metrics for this class
                CKMetrics metrics = ckMetrics.get(className);

                // Bağımlılık sayısını hesapla (CBO - Coupling Between Objects)
                Set<String> dependencies = new HashSet<>();
                cid.accept(new VoidVisitorAdapter<Void>() {
                    @Override
                    public void visit(MethodCallExpr mce, Void arg) {
                        super.visit(mce, arg);
                        mce.getScope().ifPresent(scope -> {
                            String scopeStr = scope.toString();
                            if (!scopeStr.equals("this") && !scopeStr.equals("super")) {
                                dependencies.add(scopeStr);
                            }
                        });
                    }

                    @Override
                    public void visit(FieldAccessExpr fae, Void arg) {
                        super.visit(fae, arg);
                        String scopeStr = fae.getScope().toString();
                        if (!scopeStr.equals("this") && !scopeStr.equals("super")) {
                            dependencies.add(scopeStr);
                        }
                    }
                }, null);

                int couplingScore = dependencies.size();

                // Enhanced detection using CK metrics
                boolean isGodClass = false;
                String detectionReason = "";

                if (metrics != null && metrics.isGodClass()) {
                    // CK Metrics indicate God Class (high WMC, LCOM, CBO)
                    isGodClass = true;
                    detectionReason = String.format(
                        "CK Metrics: WMC=%d, CBO=%d, LCOM=%d (Quality Score: %d/100)",
                        metrics.getWmc(), metrics.getCbo(), metrics.getLcom(), metrics.getQualityScore()
                    );
                } else {
                    // Fallback to original heuristic
                    int violationCount = 0;
                    if (fieldCount > FIELD_THRESHOLD) violationCount++;
                    if (methodCount > METHOD_THRESHOLD) violationCount++;
                    if (couplingScore > COUPLING_THRESHOLD) violationCount++;

                    if (violationCount >= 2) {
                        isGodClass = true;
                        detectionReason = String.format(
                            "%d alan, %d metot, %d bağımlılık",
                            fieldCount, methodCount, couplingScore
                        );
                    }
                }

                if (isGodClass) {
                    int line = cid.getBegin().map(pos -> pos.line).orElse(0);

                    CodeSmell smell = new CodeSmell(
                        CodeSmellType.GOD_CLASS,
                        filePath,
                        line,
                        String.format("Sınıf '%s' God Class belirtileri gösteriyor: %s",
                            className, detectionReason)
                    );
                    smell.withAffectedElement(className)
                         .withSuggestion("Single Responsibility Principle uygulayın. Sınıfı mantıksal parçalara ayırın (Extract Class)")
                         .withAutoFix(false);

                    smells.add(smell);
                }
            }
        }, null);

        return smells;
    }
}

package com.pragmite.rules.smells;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.pragmite.analyzer.CyclomaticComplexityCalculator;
import com.pragmite.model.CodeSmell;
import com.pragmite.model.CodeSmellType;

import java.util.ArrayList;
import java.util.List;

/**
 * Yüksek cyclomatic complexity kokusu dedektörü.
 * Varsayılan eşik: 10
 */
public class HighComplexityDetector implements SmellDetector {

    private static final int DEFAULT_THRESHOLD = 15;  // IMPROVED: 11-15 kabul edilebilir karmaşıklık
    private final int threshold;

    public HighComplexityDetector() {
        this(DEFAULT_THRESHOLD);
    }

    public HighComplexityDetector(int threshold) {
        this.threshold = threshold;
    }

    @Override
    public List<CodeSmell> detect(CompilationUnit cu, String filePath, String content) {
        List<CodeSmell> smells = new ArrayList<>();

        cu.accept(new VoidVisitorAdapter<Void>() {
            @Override
            public void visit(MethodDeclaration md, Void arg) {
                super.visit(md, arg);

                int complexity = CyclomaticComplexityCalculator.calculate(md);

                if (complexity > threshold) {
                    int line = md.getBegin().map(pos -> pos.line).orElse(0);

                    CodeSmell smell = new CodeSmell(
                        CodeSmellType.HIGH_CYCLOMATIC_COMPLEXITY,
                        filePath,
                        line,
                        String.format("Metot '%s' yüksek karmaşıklığa sahip: CC=%d (eşik: %d)",
                            md.getNameAsString(), complexity, threshold)
                    );
                    smell.withAffectedElement(md.getNameAsString())
                         .withSuggestion("Koşulları basitleştirin, metodu parçalara ayırın veya strateji pattern kullanın")
                         .withAutoFix(false);

                    smells.add(smell);
                }
            }
        }, null);

        return smells;
    }
}

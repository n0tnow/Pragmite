package com.pragmite.rules.smells;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.pragmite.model.CodeSmell;
import com.pragmite.model.CodeSmellType;

import java.util.ArrayList;
import java.util.List;

/**
 * Uzun parametre listesi kokusu dedektörü.
 * Varsayılan eşik: 4 parametre
 */
public class LongParameterListDetector implements SmellDetector {

    private static final int DEFAULT_THRESHOLD = 4;
    private final int threshold;

    public LongParameterListDetector() {
        this(DEFAULT_THRESHOLD);
    }

    public LongParameterListDetector(int threshold) {
        this.threshold = threshold;
    }

    @Override
    public List<CodeSmell> detect(CompilationUnit cu, String filePath, String content) {
        List<CodeSmell> smells = new ArrayList<>();

        cu.accept(new VoidVisitorAdapter<Void>() {
            @Override
            public void visit(MethodDeclaration md, Void arg) {
                super.visit(md, arg);

                int paramCount = md.getParameters().size();

                if (paramCount > threshold) {
                    int line = md.getBegin().map(pos -> pos.line).orElse(0);

                    CodeSmell smell = new CodeSmell(
                        CodeSmellType.LONG_PARAMETER_LIST,
                        filePath,
                        line,
                        String.format("Metot '%s' çok fazla parametreye sahip: %d (eşik: %d)",
                            md.getNameAsString(), paramCount, threshold)
                    );
                    smell.withAffectedElement(md.getNameAsString())
                         .withSuggestion("Parameter Object veya Builder pattern kullanın")
                         .withAutoFix(false);

                    smells.add(smell);
                }
            }
        }, null);

        return smells;
    }
}

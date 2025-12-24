package com.pragmite.rules.smells;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.pragmite.model.CodeSmell;
import com.pragmite.model.CodeSmellType;

import java.util.ArrayList;
import java.util.List;

/**
 * Uzun metot kokusu dedektörü.
 * Varsayılan eşik: 30 satır
 */
public class LongMethodDetector implements SmellDetector {

    private static final int DEFAULT_THRESHOLD = 50;  // IMPROVED: Modern kod için 30-50 satır kabul edilebilir
    private final int threshold;

    public LongMethodDetector() {
        this(DEFAULT_THRESHOLD);
    }

    public LongMethodDetector(int threshold) {
        this.threshold = threshold;
    }

    @Override
    public List<CodeSmell> detect(CompilationUnit cu, String filePath, String content) {
        List<CodeSmell> smells = new ArrayList<>();

        cu.accept(new VoidVisitorAdapter<Void>() {
            @Override
            public void visit(MethodDeclaration md, Void arg) {
                super.visit(md, arg);

                int startLine = md.getBegin().map(pos -> pos.line).orElse(0);
                int endLine = md.getEnd().map(pos -> pos.line).orElse(0);
                int lineCount = endLine - startLine + 1;

                if (lineCount > threshold) {
                    CodeSmell smell = new CodeSmell(
                        CodeSmellType.LONG_METHOD,
                        filePath,
                        startLine,
                        String.format("Metot '%s' çok uzun: %d satır (eşik: %d)",
                            md.getNameAsString(), lineCount, threshold)
                    );
                    smell.withEndLine(endLine)
                         .withAffectedElement(md.getNameAsString())
                         .withSuggestion("Metodu daha küçük, tek sorumluluğa sahip metotlara bölün (Extract Method)")
                         .withAutoFix(false);

                    smells.add(smell);
                }
            }
        }, null);

        return smells;
    }
}

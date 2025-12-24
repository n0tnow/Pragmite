package com.pragmite.rules.smells;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.pragmite.model.CodeSmell;
import com.pragmite.model.CodeSmellType;

import java.util.ArrayList;
import java.util.List;

/**
 * Büyük sınıf kokusu dedektörü.
 * Varsayılan eşik: 300 satır
 */
public class LargeClassDetector implements SmellDetector {

    private static final int DEFAULT_LINE_THRESHOLD = 300;
    private static final int DEFAULT_METHOD_THRESHOLD = 20;

    private final int lineThreshold;
    private final int methodThreshold;

    public LargeClassDetector() {
        this(DEFAULT_LINE_THRESHOLD, DEFAULT_METHOD_THRESHOLD);
    }

    public LargeClassDetector(int lineThreshold, int methodThreshold) {
        this.lineThreshold = lineThreshold;
        this.methodThreshold = methodThreshold;
    }

    @Override
    public List<CodeSmell> detect(CompilationUnit cu, String filePath, String content) {
        List<CodeSmell> smells = new ArrayList<>();

        cu.accept(new VoidVisitorAdapter<Void>() {
            @Override
            public void visit(ClassOrInterfaceDeclaration cid, Void arg) {
                super.visit(cid, arg);

                if (cid.isInterface()) return; // Interface'leri atla

                int startLine = cid.getBegin().map(pos -> pos.line).orElse(0);
                int endLine = cid.getEnd().map(pos -> pos.line).orElse(0);
                int lineCount = endLine - startLine + 1;
                int methodCount = cid.getMethods().size();

                if (lineCount > lineThreshold) {
                    CodeSmell smell = new CodeSmell(
                        CodeSmellType.LARGE_CLASS,
                        filePath,
                        startLine,
                        String.format("Sınıf '%s' çok büyük: %d satır (eşik: %d)",
                            cid.getNameAsString(), lineCount, lineThreshold)
                    );
                    smell.withEndLine(endLine)
                         .withAffectedElement(cid.getNameAsString())
                         .withSuggestion("Sınıfı daha küçük, tek sorumluluğa sahip sınıflara bölün (Extract Class)")
                         .withAutoFix(false);

                    smells.add(smell);
                }

                if (methodCount > methodThreshold) {
                    CodeSmell smell = new CodeSmell(
                        CodeSmellType.LARGE_CLASS,
                        filePath,
                        startLine,
                        String.format("Sınıf '%s' çok fazla metoda sahip: %d metot (eşik: %d)",
                            cid.getNameAsString(), methodCount, methodThreshold)
                    );
                    smell.withAffectedElement(cid.getNameAsString())
                         .withSuggestion("Sınıfı daha küçük sınıflara bölün veya bazı metodları helper sınıflara taşıyın")
                         .withAutoFix(false);

                    smells.add(smell);
                }
            }
        }, null);

        return smells;
    }
}

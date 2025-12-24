package com.pragmite.rules.smells;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.stmt.*;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.pragmite.model.CodeSmell;
import com.pragmite.model.CodeSmellType;

import java.util.ArrayList;
import java.util.List;

/**
 * Derin iç içe kod bloğu dedektörü.
 * Varsayılan eşik: 4 seviye
 */
public class DeepNestingDetector implements SmellDetector {

    private static final int DEFAULT_THRESHOLD = 4;
    private final int threshold;

    public DeepNestingDetector() {
        this(DEFAULT_THRESHOLD);
    }

    public DeepNestingDetector(int threshold) {
        this.threshold = threshold;
    }

    @Override
    public List<CodeSmell> detect(CompilationUnit cu, String filePath, String content) {
        List<CodeSmell> smells = new ArrayList<>();

        cu.accept(new VoidVisitorAdapter<Void>() {
            @Override
            public void visit(MethodDeclaration md, Void arg) {
                super.visit(md, arg);

                // Metot içindeki maksimum nesting derinliğini bul
                int[] maxDepth = {0};
                int[] currentDepth = {0};
                int[] deepestLine = {0};

                md.accept(new VoidVisitorAdapter<Void>() {
                    private void enterBlock(com.github.javaparser.ast.Node node) {
                        currentDepth[0]++;
                        if (currentDepth[0] > maxDepth[0]) {
                            maxDepth[0] = currentDepth[0];
                            deepestLine[0] = node.getBegin().map(p -> p.line).orElse(0);
                        }
                    }

                    private void exitBlock() {
                        currentDepth[0]--;
                    }

                    @Override
                    public void visit(IfStmt n, Void arg) {
                        enterBlock(n);
                        super.visit(n, arg);
                        exitBlock();
                    }

                    @Override
                    public void visit(ForStmt n, Void arg) {
                        enterBlock(n);
                        super.visit(n, arg);
                        exitBlock();
                    }

                    @Override
                    public void visit(ForEachStmt n, Void arg) {
                        enterBlock(n);
                        super.visit(n, arg);
                        exitBlock();
                    }

                    @Override
                    public void visit(WhileStmt n, Void arg) {
                        enterBlock(n);
                        super.visit(n, arg);
                        exitBlock();
                    }

                    @Override
                    public void visit(DoStmt n, Void arg) {
                        enterBlock(n);
                        super.visit(n, arg);
                        exitBlock();
                    }

                    @Override
                    public void visit(TryStmt n, Void arg) {
                        enterBlock(n);
                        super.visit(n, arg);
                        exitBlock();
                    }

                    @Override
                    public void visit(SwitchStmt n, Void arg) {
                        enterBlock(n);
                        super.visit(n, arg);
                        exitBlock();
                    }

                    @Override
                    public void visit(SynchronizedStmt n, Void arg) {
                        enterBlock(n);
                        super.visit(n, arg);
                        exitBlock();
                    }
                }, null);

                if (maxDepth[0] > threshold) {
                    int line = md.getBegin().map(pos -> pos.line).orElse(0);

                    CodeSmell smell = new CodeSmell(
                        CodeSmellType.DEEPLY_NESTED_CODE,
                        filePath,
                        line,
                        String.format("Metot '%s' çok derin iç içe yapıya sahip: %d seviye (eşik: %d)",
                            md.getNameAsString(), maxDepth[0], threshold)
                    );
                    smell.withAffectedElement(md.getNameAsString())
                         .withSuggestion("Guard clauses kullanın, early return yapın veya metodu parçalara ayırın")
                         .withAutoFix(false);

                    smells.add(smell);
                }
            }
        }, null);

        return smells;
    }
}

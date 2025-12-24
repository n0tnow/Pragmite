package com.pragmite.rules.smells;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.stmt.DoStmt;
import com.github.javaparser.ast.stmt.ForEachStmt;
import com.github.javaparser.ast.stmt.ForStmt;
import com.github.javaparser.ast.stmt.WhileStmt;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.pragmite.model.CodeSmell;
import com.pragmite.model.CodeSmellType;

import java.util.ArrayList;
import java.util.List;

/**
 * Döngü içinde String birleştirme kokusu dedektörü.
 * String += veya String + String döngü içinde yapılıyorsa uyarı verir.
 */
public class StringConcatInLoopDetector implements SmellDetector {

    @Override
    public List<CodeSmell> detect(CompilationUnit cu, String filePath, String content) {
        List<CodeSmell> smells = new ArrayList<>();

        cu.accept(new VoidVisitorAdapter<Void>() {
            private int loopDepth = 0;

            @Override
            public void visit(ForStmt n, Void arg) {
                loopDepth++;
                super.visit(n, arg);
                loopDepth--;
            }

            @Override
            public void visit(ForEachStmt n, Void arg) {
                loopDepth++;
                super.visit(n, arg);
                loopDepth--;
            }

            @Override
            public void visit(WhileStmt n, Void arg) {
                loopDepth++;
                super.visit(n, arg);
                loopDepth--;
            }

            @Override
            public void visit(DoStmt n, Void arg) {
                loopDepth++;
                super.visit(n, arg);
                loopDepth--;
            }

            @Override
            public void visit(AssignExpr ae, Void arg) {
                super.visit(ae, arg);

                if (loopDepth > 0 && ae.getOperator() == AssignExpr.Operator.PLUS) {
                    // += operatörü ile String birleştirme kontrolü
                    String target = ae.getTarget().toString();

                    // StringBuilder/StringBuffer zaten kullanılıyorsa uyarma
                    if (target.contains("StringBuilder") || target.contains("StringBuffer")) {
                        return;
                    }

                    // Değişken ismi String tipini belirtiyorsa uyar
                    // Int, long, double gibi primitive tiplerde += normal kullanımdır
                    if (target.toLowerCase().contains("str") ||
                        target.toLowerCase().contains("text") ||
                        target.toLowerCase().contains("message") ||
                        target.toLowerCase().contains("result") ||
                        target.toLowerCase().contains("output") ||
                        target.toLowerCase().contains("buffer") ||
                        target.toLowerCase().contains("content")) {

                        int line = ae.getBegin().map(pos -> pos.line).orElse(0);

                        CodeSmell smell = new CodeSmell(
                            CodeSmellType.STRING_CONCAT_IN_LOOP,
                            filePath,
                            line,
                            "Döngü içinde String birleştirme (+=) performans sorunu yaratabilir"
                        );
                        smell.withSuggestion("StringBuilder kullanın")
                             .withAutoFix(true);

                        smells.add(smell);
                    }
                }
            }

            @Override
            public void visit(BinaryExpr be, Void arg) {
                super.visit(be, arg);

                if (loopDepth > 0 && be.getOperator() == BinaryExpr.Operator.PLUS) {
                    // String + String kontrolü - basit heuristic
                    String left = be.getLeft().toString();
                    String right = be.getRight().toString();

                    // En az bir taraf string literal ise
                    if (left.startsWith("\"") || right.startsWith("\"")) {
                        int line = be.getBegin().map(pos -> pos.line).orElse(0);

                        CodeSmell smell = new CodeSmell(
                            CodeSmellType.STRING_CONCAT_IN_LOOP,
                            filePath,
                            line,
                            "Döngü içinde String birleştirme (+) performans sorunu yaratabilir"
                        );
                        smell.withSuggestion("StringBuilder kullanın")
                             .withAutoFix(true);

                        smells.add(smell);
                    }
                }
            }
        }, null);

        return smells;
    }
}

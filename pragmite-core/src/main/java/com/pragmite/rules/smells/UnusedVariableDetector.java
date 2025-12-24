package com.pragmite.rules.smells;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.pragmite.model.CodeSmell;
import com.pragmite.model.CodeSmellType;

import java.util.*;

/**
 * Kullanılmayan yerel değişken dedektörü.
 */
public class UnusedVariableDetector implements SmellDetector {

    @Override
    public List<CodeSmell> detect(CompilationUnit cu, String filePath, String content) {
        List<CodeSmell> smells = new ArrayList<>();

        cu.accept(new VoidVisitorAdapter<Void>() {
            @Override
            public void visit(MethodDeclaration md, Void arg) {
                super.visit(md, arg);

                // Tüm yerel değişkenleri topla
                Map<String, VariableInfo> declaredVariables = new HashMap<>();

                md.accept(new VoidVisitorAdapter<Void>() {
                    @Override
                    public void visit(VariableDeclarationExpr vde, Void arg) {
                        super.visit(vde, arg);

                        for (VariableDeclarator vd : vde.getVariables()) {
                            String varName = vd.getNameAsString();
                            int line = vd.getBegin().map(p -> p.line).orElse(0);

                            // _ ile başlayan değişkenler kasıtlı olarak ignore edilebilir
                            if (!varName.startsWith("_")) {
                                declaredVariables.put(varName, new VariableInfo(varName, line, false));
                            }
                        }
                    }
                }, null);

                // Değişken kullanımlarını kontrol et
                md.accept(new VoidVisitorAdapter<Void>() {
                    @Override
                    public void visit(NameExpr ne, Void arg) {
                        super.visit(ne, arg);

                        String name = ne.getNameAsString();
                        VariableInfo info = declaredVariables.get(name);
                        if (info != null) {
                            // Sadece tanımlandığı satırdan farklı bir satırda kullanılıyorsa "used" say
                            int usageLine = ne.getBegin().map(p -> p.line).orElse(0);
                            if (usageLine != info.line) {
                                info.used = true;
                            }
                        }
                    }
                }, null);

                // Kullanılmayan değişkenleri raporla
                for (VariableInfo info : declaredVariables.values()) {
                    if (!info.used) {
                        CodeSmell smell = new CodeSmell(
                            CodeSmellType.UNUSED_VARIABLE,
                            filePath,
                            info.line,
                            String.format("Kullanılmayan değişken: '%s'", info.name)
                        );
                        smell.withSuggestion("Bu değişkeni kaldırın veya kullanın")
                             .withAutoFix(true);

                        smells.add(smell);
                    }
                }
            }
        }, null);

        return smells;
    }

    private static class VariableInfo {
        final String name;
        final int line;
        boolean used;

        VariableInfo(String name, int line, boolean used) {
            this.name = name;
            this.line = line;
            this.used = used;
        }
    }
}

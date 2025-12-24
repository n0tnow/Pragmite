package com.pragmite.analyzer;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.pragmite.model.MethodInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * CompilationUnit'ten metot bilgilerini çıkarır.
 */
public class MethodExtractor {

    public static List<MethodInfo> extractMethods(CompilationUnit cu) {
        List<MethodInfo> methods = new ArrayList<>();

        cu.accept(new VoidVisitorAdapter<Void>() {
            @Override
            public void visit(MethodDeclaration md, Void arg) {
                super.visit(md, arg);

                MethodInfo info = new MethodInfo();
                info.setName(md.getNameAsString());
                info.setSignature(md.getDeclarationAsString(false, false, false));
                info.setParameterCount(md.getParameters().size());

                // Satır bilgileri
                md.getBegin().ifPresent(pos -> info.setStartLine(pos.line));
                md.getEnd().ifPresent(pos -> info.setEndLine(pos.line));

                if (info.getStartLine() > 0 && info.getEndLine() > 0) {
                    info.setLineCount(info.getEndLine() - info.getStartLine() + 1);
                }

                // Cyclomatic complexity hesapla
                int complexity = CyclomaticComplexityCalculator.calculate(md);
                info.setCyclomaticComplexity(complexity);

                methods.add(info);
            }
        }, null);

        return methods;
    }
}

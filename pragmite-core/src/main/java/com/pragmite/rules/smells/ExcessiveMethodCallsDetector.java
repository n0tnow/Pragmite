package com.pragmite.rules.smells;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.pragmite.model.CodeSmell;
import com.pragmite.model.CodeSmellType;

import java.util.ArrayList;
import java.util.List;

/**
 * Detects methods that make excessive external method calls.
 * Too many calls indicate the method is doing too much or has feature envy.
 *
 * Threshold: 15+ method calls in a single method
 */
public class ExcessiveMethodCallsDetector implements SmellDetector {

    private static final int MAX_METHOD_CALLS = 15;

    @Override
    public List<CodeSmell> detect(CompilationUnit cu, String filePath, String content) {
        List<CodeSmell> smells = new ArrayList<>();

        cu.findAll(MethodDeclaration.class).forEach(method -> {
            List<MethodCallExpr> methodCalls = method.findAll(MethodCallExpr.class);
            int callCount = methodCalls.size();

            if (callCount > MAX_METHOD_CALLS) {
                CodeSmell smell = new CodeSmell(
                    CodeSmellType.LONG_METHOD,
                    filePath,
                    method.getBegin().get().line,
                    String.format("Method '%s' makes %d method calls. Consider breaking into smaller methods.",
                                method.getNameAsString(), callCount)
                );
                smell.withAffectedElement(method.getNameAsString())
                    .withSuggestion("Extract groups of related calls into helper methods")
                    .withAutoFix(false);
                smells.add(smell);
            }
        });

        return smells;
    }
}

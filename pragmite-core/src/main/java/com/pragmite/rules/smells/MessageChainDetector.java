package com.pragmite.rules.smells;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.pragmite.model.CodeSmell;
import com.pragmite.model.CodeSmellType;

import java.util.ArrayList;
import java.util.List;

/**
 * Detects Message Chains (Law of Demeter violations).
 * Long chains of method calls indicate tight coupling and violate encapsulation.
 *
 * Example: object.getA().getB().getC().doSomething()
 */
public class MessageChainDetector implements SmellDetector {

    private static final int MAX_CHAIN_LENGTH = 3;

    @Override
    public List<CodeSmell> detect(CompilationUnit cu, String filePath, String content) {
        List<CodeSmell> smells = new ArrayList<>();

        cu.findAll(MethodCallExpr.class).forEach(call -> {
            int chainLength = calculateChainLength(call);

            if (chainLength > MAX_CHAIN_LENGTH) {
                CodeSmell smell = new CodeSmell(
                    CodeSmellType.MESSAGE_CHAIN,
                    filePath,
                    call.getBegin().get().line,
                    "Message chain of length " + chainLength + " detected. " +
                        "Violates Law of Demeter."
                );
                smell.withSuggestion("Hide delegate: add a method in the intermediate object that performs the entire operation")
                    .withAutoFix(false);
                smells.add(smell);
            }
        });

        return smells;
    }

    /**
     * Calculates the length of a method call chain.
     */
    private int calculateChainLength(MethodCallExpr call) {
        int length = 1;
        MethodCallExpr current = call;

        while (current.getScope().isPresent() &&
               current.getScope().get() instanceof MethodCallExpr) {
            length++;
            current = (MethodCallExpr) current.getScope().get();
        }

        return length;
    }
}

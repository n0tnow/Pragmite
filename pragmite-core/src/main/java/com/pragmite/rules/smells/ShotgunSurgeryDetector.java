package com.pragmite.rules.smells;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.pragmite.model.CodeSmell;
import com.pragmite.model.CodeSmellType;

import java.util.*;

/**
 * Detects Shotgun Surgery code smell.
 * Making any kind of change requires many small changes to many different classes.
 *
 * This detector identifies methods that are called from many different places,
 * suggesting that changes to this method would require updates in many locations.
 */
public class ShotgunSurgeryDetector implements SmellDetector {

    private static final int HIGH_COUPLING_THRESHOLD = 7; // Called from 7+ different methods

    @Override
    public List<CodeSmell> detect(CompilationUnit cu, String filePath, String content) {
        List<CodeSmell> smells = new ArrayList<>();

        // Map to count how many different methods call each method
        Map<String, Set<String>> methodCallers = new HashMap<>();

        // First pass: collect all method definitions
        Set<String> definedMethods = new HashSet<>();
        cu.findAll(MethodDeclaration.class).forEach(method -> {
            definedMethods.add(method.getNameAsString());
        });

        // Second pass: track method calls
        cu.findAll(MethodDeclaration.class).forEach(caller -> {
            String callerName = caller.getNameAsString();

            caller.findAll(MethodCallExpr.class).forEach(call -> {
                String calledMethod = call.getNameAsString();

                // Only track calls to methods defined in this file
                if (definedMethods.contains(calledMethod)) {
                    methodCallers.computeIfAbsent(calledMethod, k -> new HashSet<>())
                        .add(callerName);
                }
            });
        });

        // Find methods with too many callers
        methodCallers.forEach((methodName, callers) -> {
            if (callers.size() >= HIGH_COUPLING_THRESHOLD) {
                // Find the method declaration to get line number
                cu.findAll(MethodDeclaration.class).stream()
                    .filter(m -> m.getNameAsString().equals(methodName))
                    .findFirst()
                    .ifPresent(method -> {
                        CodeSmell smell = new CodeSmell(
                            CodeSmellType.INAPPROPRIATE_INTIMACY, // Reusing existing type
                            filePath,
                            method.getBegin().get().line,
                            "Method '" + methodName + "' is called from " + callers.size() +
                                " different places. Changes here will require shotgun surgery."
                        );
                        smell.withAffectedElement(methodName)
                            .withSuggestion("Consider consolidating the callers or using Observer/Event pattern")
                            .withAutoFix(false);
                        smells.add(smell);
                    });
            }
        });

        return smells;
    }
}

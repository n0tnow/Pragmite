package com.pragmite.rules.smells;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.pragmite.model.CodeSmell;
import com.pragmite.model.CodeSmellType;

import java.util.*;

/**
 * Inappropriate Intimacy detector.
 * Detects classes that are too tightly coupled - accessing each other's private parts frequently.
 * This violates encapsulation and makes both classes harder to maintain and test.
 */
public class InappropriateIntimacyDetector implements SmellDetector {

    private static final int INTIMACY_THRESHOLD = 8; // 8+ intimate interactions

    @Override
    public List<CodeSmell> detect(CompilationUnit cu, String filePath, String content) {
        List<CodeSmell> smells = new ArrayList<>();
        Map<String, ClassIntimacy> classIntimacies = new HashMap<>();

        // Analyze each class
        cu.accept(new VoidVisitorAdapter<Void>() {
            private String currentClass = "";
            private ClassIntimacy currentIntimacy = null;

            @Override
            public void visit(ClassOrInterfaceDeclaration cid, Void arg) {
                currentClass = cid.getNameAsString();
                currentIntimacy = new ClassIntimacy(currentClass);
                classIntimacies.put(currentClass, currentIntimacy);

                // Analyze fields (dependencies)
                for (FieldDeclaration fd : cid.getFields()) {
                    fd.getVariables().forEach(var -> {
                        String fieldType = var.getType().asString();
                        if (isUserDefinedType(fieldType)) {
                            currentIntimacy.addFieldDependency(fieldType);
                        }
                    });
                }

                super.visit(cid, arg);

                currentIntimacy.lineNumber = cid.getBegin().map(pos -> pos.line).orElse(0);
            }

            @Override
            public void visit(MethodDeclaration md, Void arg) {
                super.visit(md, arg);

                if (currentIntimacy == null) return;

                // Count method calls to other classes
                for (MethodCallExpr mce : md.findAll(MethodCallExpr.class)) {
                    mce.getScope().ifPresent(scope -> {
                        String scopeType = inferType(scope.toString());
                        if (scopeType != null && !scopeType.equals(currentClass)) {
                            currentIntimacy.addMethodCallDependency(scopeType);
                        }
                    });
                }

                // Count field accesses to other classes
                for (FieldAccessExpr fae : md.findAll(FieldAccessExpr.class)) {
                    String scope = fae.getScope().toString();
                    String scopeType = inferType(scope);
                    if (scopeType != null && !scopeType.equals(currentClass)) {
                        currentIntimacy.addFieldAccessDependency(scopeType);
                    }
                }
            }
        }, null);

        // Analyze intimacy levels
        for (ClassIntimacy intimacy : classIntimacies.values()) {
            for (Map.Entry<String, Integer> entry : intimacy.dependencies.entrySet()) {
                String otherClass = entry.getKey();
                int intimacyLevel = entry.getValue();

                if (intimacyLevel >= INTIMACY_THRESHOLD) {
                    CodeSmell smell = new CodeSmell(
                        CodeSmellType.INAPPROPRIATE_INTIMACY,
                        filePath,
                        intimacy.lineNumber,
                        String.format("Class '%s' has inappropriate intimacy with '%s' (%d intimate interactions)",
                            intimacy.className, otherClass, intimacyLevel)
                    );
                    smell.withSuggestion(
                        "Consider refactoring: extract common behavior into a separate class, " +
                        "use delegation instead of accessing internals, or merge the classes if they're truly inseparable"
                    ).withAutoFix(false);

                    smells.add(smell);
                }
            }
        }

        return smells;
    }

    /**
     * Checks if a type is user-defined (not a primitive or JDK class).
     */
    private boolean isUserDefinedType(String type) {
        // Remove generics
        type = type.replaceAll("<.*>", "").trim();

        return !type.matches("^(int|long|double|float|boolean|char|byte|short|void)$") &&
               !type.startsWith("java.") &&
               !type.startsWith("javax.") &&
               Character.isUpperCase(type.charAt(0));
    }

    /**
     * Infers the type from a variable name (simplified - assumes PascalCase for types).
     */
    private String inferType(String varName) {
        // If it's already a type name (starts with uppercase), return it
        if (varName.length() > 0 && Character.isUpperCase(varName.charAt(0))) {
            return varName;
        }

        // For now, we can't reliably infer type from variable name without full type resolution
        // Return null to skip
        return null;
    }

    /**
     * Tracks intimacy metrics for a class.
     */
    private static class ClassIntimacy {
        final String className;
        int lineNumber;
        final Map<String, Integer> dependencies = new HashMap<>();

        ClassIntimacy(String className) {
            this.className = className;
        }

        void addFieldDependency(String type) {
            dependencies.put(type, dependencies.getOrDefault(type, 0) + 2); // Field = stronger dependency
        }

        void addMethodCallDependency(String type) {
            dependencies.put(type, dependencies.getOrDefault(type, 0) + 1);
        }

        void addFieldAccessDependency(String type) {
            dependencies.put(type, dependencies.getOrDefault(type, 0) + 2); // Field access = intimate
        }
    }
}

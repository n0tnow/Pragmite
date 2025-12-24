package com.pragmite.rules.smells;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.pragmite.model.CodeSmell;
import com.pragmite.model.CodeSmellType;

import java.util.ArrayList;
import java.util.List;

/**
 * Detects Middle Man code smell.
 * A class that delegates most of its work to another class may be unnecessary.
 */
public class MiddleManDetector implements SmellDetector {

    private static final double DELEGATION_THRESHOLD = 0.8; // 80% of methods just delegate

    @Override
    public List<CodeSmell> detect(CompilationUnit cu, String filePath, String content) {
        List<CodeSmell> smells = new ArrayList<>();

        cu.findAll(ClassOrInterfaceDeclaration.class).forEach(classDecl -> {
            if (classDecl.isInterface()) return;

            List<MethodDeclaration> methods = classDecl.getMethods();
            if (methods.size() < 3) return; // Need at least 3 methods to be significant

            int delegatingMethods = 0;
            for (MethodDeclaration method : methods) {
                if (isSimpleDelegation(method)) {
                    delegatingMethods++;
                }
            }

            double delegationRatio = (double) delegatingMethods / methods.size();

            if (delegationRatio >= DELEGATION_THRESHOLD) {
                CodeSmell smell = new CodeSmell(
                    CodeSmellType.MIDDLE_MAN,
                    filePath,
                    classDecl.getBegin().get().line,
                    "Class '" + classDecl.getNameAsString() + "' acts as a middle man. " +
                        delegatingMethods + " out of " + methods.size() + " methods just delegate."
                );
                smell.withAffectedElement(classDecl.getNameAsString())
                    .withSuggestion("Remove middle man: let clients call the delegate directly")
                    .withAutoFix(false);
                smells.add(smell);
            }
        });

        return smells;
    }

    /**
     * Checks if a method is a simple delegation (one-liner that calls another object's method).
     */
    private boolean isSimpleDelegation(MethodDeclaration method) {
        if (method.getBody().isEmpty()) return false;

        var statements = method.getBody().get().getStatements();
        if (statements.size() != 1) return false;

        var stmt = statements.get(0);

        // Check if it's a return statement with a method call
        if (stmt instanceof ReturnStmt) {
            ReturnStmt returnStmt = (ReturnStmt) stmt;
            if (returnStmt.getExpression().isPresent()) {
                String expr = returnStmt.getExpression().get().toString();
                // Simple heuristic: contains a dot (field.method())
                return expr.contains(".") && expr.contains("(");
            }
        }

        // Or just a method call statement
        String stmtStr = stmt.toString();
        return stmtStr.contains(".") && stmtStr.contains("(") && stmtStr.split("\\.").length == 2;
    }
}

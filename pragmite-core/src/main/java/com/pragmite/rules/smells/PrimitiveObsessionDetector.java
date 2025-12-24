package com.pragmite.rules.smells;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.pragmite.model.CodeSmell;
import com.pragmite.model.CodeSmellType;

import java.util.ArrayList;
import java.util.List;

/**
 * Detects Primitive Obsession code smell.
 * Identifies overuse of primitives instead of small objects for simple tasks.
 *
 * Examples:
 * - Multiple primitive parameters that represent a concept (e.g., x, y coordinates)
 * - Primitive type codes instead of enums
 * - String abuse for representing structured data
 */
public class PrimitiveObsessionDetector implements SmellDetector {

    @Override
    public List<CodeSmell> detect(CompilationUnit cu, String filePath, String content) {
        List<CodeSmell> smells = new ArrayList<>();

        cu.findAll(MethodDeclaration.class).forEach(method -> {
            // Check for methods with many primitive parameters of the same type
            List<Parameter> primitiveParams = new ArrayList<>();
            for (Parameter param : method.getParameters()) {
                String type = param.getTypeAsString();
                if (isPrimitive(type)) {
                    primitiveParams.add(param);
                }
            }

            // If we have 3+ primitives of the same type, it might be primitive obsession
            if (countSameTypePrimitives(primitiveParams) >= 3) {
                CodeSmell smell = new CodeSmell(
                    CodeSmellType.PRIMITIVE_OBSESSION,
                    filePath,
                    method.getBegin().get().line,
                    "Method has " + primitiveParams.size() + " primitive parameters. " +
                        "Consider introducing a parameter object."
                );
                smell.withAffectedElement(method.getNameAsString())
                    .withSuggestion("Create a parameter object class to encapsulate related primitives")
                    .withAutoFix(false);
                smells.add(smell);
            }

            // Check for primitive type codes (int status, int type, etc.)
            method.getParameters().forEach(param -> {
                String name = param.getNameAsString().toLowerCase();
                String type = param.getTypeAsString();

                if (type.equals("int") || type.equals("String")) {
                    if (name.contains("type") || name.contains("status") || name.contains("code") ||
                        name.contains("mode") || name.contains("flag")) {

                        CodeSmell smell = new CodeSmell(
                            CodeSmellType.PRIMITIVE_OBSESSION,
                            filePath,
                            param.getBegin().get().line,
                            "Parameter '" + param.getNameAsString() + "' uses primitive type " + type +
                                " instead of an enum or type-safe class"
                        );
                        smell.withAffectedElement(method.getNameAsString())
                            .withSuggestion("Replace with an enum or create a type-safe class")
                            .withAutoFix(false);
                        smells.add(smell);
                    }
                }
            });
        });

        return smells;
    }

    private boolean isPrimitive(String type) {
        return type.equals("int") || type.equals("long") || type.equals("double") ||
               type.equals("float") || type.equals("boolean") || type.equals("char") ||
               type.equals("byte") || type.equals("short") || type.equals("String");
    }

    private int countSameTypePrimitives(List<Parameter> params) {
        if (params.isEmpty()) return 0;

        int maxCount = 0;
        for (Parameter p1 : params) {
            int count = 0;
            for (Parameter p2 : params) {
                if (p1.getTypeAsString().equals(p2.getTypeAsString())) {
                    count++;
                }
            }
            maxCount = Math.max(maxCount, count);
        }
        return maxCount;
    }
}

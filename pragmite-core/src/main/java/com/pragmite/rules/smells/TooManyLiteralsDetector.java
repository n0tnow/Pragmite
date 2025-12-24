package com.pragmite.rules.smells;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.expr.DoubleLiteralExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.pragmite.model.CodeSmell;
import com.pragmite.model.CodeSmellType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Detects methods with too many literal values.
 * Excessive literals indicate missing constants or configuration.
 *
 * Thresholds:
 * - 5+ numeric literals (excluding 0, 1, -1)
 * - 3+ string literals (excluding empty string)
 */
public class TooManyLiteralsDetector implements SmellDetector {

    private static final int MAX_NUMERIC_LITERALS = 5;
    private static final int MAX_STRING_LITERALS = 3;

    @Override
    public List<CodeSmell> detect(CompilationUnit cu, String filePath, String content) {
        List<CodeSmell> smells = new ArrayList<>();

        cu.findAll(MethodDeclaration.class).forEach(method -> {
            int numericCount = 0;
            int stringCount = 0;

            // Count integer literals (excluding common values)
            for (IntegerLiteralExpr literal : method.findAll(IntegerLiteralExpr.class)) {
                String value = literal.getValue();
                if (!value.equals("0") && !value.equals("1") && !value.equals("-1")) {
                    numericCount++;
                }
            }

            // Count double literals
            numericCount += method.findAll(DoubleLiteralExpr.class).size();

            // Count string literals (excluding empty)
            for (StringLiteralExpr literal : method.findAll(StringLiteralExpr.class)) {
                if (!literal.getValue().isEmpty()) {
                    stringCount++;
                }
            }

            if (numericCount >= MAX_NUMERIC_LITERALS) {
                CodeSmell smell = new CodeSmell(
                    CodeSmellType.MAGIC_NUMBER,
                    filePath,
                    method.getBegin().get().line,
                    String.format("Method '%s' contains %d numeric literals. Extract to named constants.",
                                method.getNameAsString(), numericCount)
                );
                smell.withAffectedElement(method.getNameAsString())
                    .withSuggestion("Extract repeated literals to named constants or enum values")
                    .withAutoFix(true);
                smells.add(smell);
            }

            if (stringCount >= MAX_STRING_LITERALS) {
                CodeSmell smell = new CodeSmell(
                    CodeSmellType.MAGIC_STRING,
                    filePath,
                    method.getBegin().get().line,
                    String.format("Method '%s' contains %d string literals. Extract to named constants.",
                                method.getNameAsString(), stringCount)
                );
                smell.withAffectedElement(method.getNameAsString())
                    .withSuggestion("Extract string literals to named constants or configuration")
                    .withAutoFix(true);
                smells.add(smell);
            }
        });

        return smells;
    }
}

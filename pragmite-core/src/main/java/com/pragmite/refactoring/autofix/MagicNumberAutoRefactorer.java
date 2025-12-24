package com.pragmite.refactoring.autofix;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.expr.DoubleLiteralExpr;
import com.github.javaparser.ast.expr.LongLiteralExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.pragmite.model.CodeSmell;
import com.pragmite.refactoring.AutoRefactorer;
import com.pragmite.refactoring.RefactoringSuggestion;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Auto-refactorer for magic number code smell.
 * Extracts magic numbers to named constants.
 */
@SuppressWarnings("unchecked")
public class MagicNumberAutoRefactorer implements AutoRefactorer {

    private static final Set<String> IGNORED_VALUES = Set.of("0", "1", "-1", "0.0", "1.0");

    @Override
    public boolean canAutoFix(CodeSmell smell) {
        return smell.getType() != null &&
               "Magic Number".equalsIgnoreCase(smell.getType().getName());
    }

    @Override
    public Optional<CompilationUnit> generateFixedCode(CodeSmell smell, CompilationUnit originalCu) {
        CompilationUnit fixedCu = originalCu.clone();

        Map<String, String> constantsToAdd = new HashMap<>();

        // Find all integer/double literals
        fixedCu.findAll(IntegerLiteralExpr.class).forEach(literal -> {
            String value = literal.getValue();
            if (!IGNORED_VALUES.contains(value) && !isAlreadyConstant(literal)) {
                String constantName = generateConstantName(value, "INT");
                constantsToAdd.put(constantName, value);
                literal.replace(new NameExpr(constantName));
            }
        });

        fixedCu.findAll(DoubleLiteralExpr.class).forEach(literal -> {
            String value = literal.getValue();
            if (!IGNORED_VALUES.contains(value) && !isAlreadyConstant(literal)) {
                String constantName = generateConstantName(value, "DOUBLE");
                constantsToAdd.put(constantName, value);
                literal.replace(new NameExpr(constantName));
            }
        });

        fixedCu.findAll(LongLiteralExpr.class).forEach(literal -> {
            String value = literal.getValue();
            if (!IGNORED_VALUES.contains(value) && !isAlreadyConstant(literal)) {
                String constantName = generateConstantName(value, "LONG");
                constantsToAdd.put(constantName, value);
                literal.replace(new NameExpr(constantName));
            }
        });

        // Add constants to the class
        if (!constantsToAdd.isEmpty()) {
            fixedCu.findFirst(ClassOrInterfaceDeclaration.class).ifPresent(classDecl -> {
                constantsToAdd.forEach((name, value) -> {
                    if (!hasConstant(classDecl, name)) {
                        addConstant(classDecl, name, value);
                    }
                });
            });
        }

        return Optional.of(fixedCu);
    }

    private boolean isAlreadyConstant(com.github.javaparser.ast.expr.Expression literal) {
        // Check if this literal is already part of a constant declaration
        return literal.findAncestor(FieldDeclaration.class)
                .map(field -> field.isStatic() && field.isFinal())
                .orElse(false);
    }

    private String generateConstantName(String value, String type) {
        // Remove decimal points and negative signs for name generation
        String cleanValue = value.replace(".", "_").replace("-", "NEG_");
        return type + "_" + cleanValue;
    }

    private boolean hasConstant(ClassOrInterfaceDeclaration classDecl, String constantName) {
        return classDecl.getFields().stream()
                .anyMatch(field -> field.getVariables().stream()
                        .anyMatch(var -> var.getNameAsString().equals(constantName)));
    }

    private void addConstant(ClassOrInterfaceDeclaration classDecl, String name, String value) {
        FieldDeclaration constant = classDecl.addFieldWithInitializer(
                determineType(value),
                name,
                parseValue(value),
                Modifier.Keyword.PRIVATE, Modifier.Keyword.STATIC, Modifier.Keyword.FINAL
        );

        // Move constant to top of class
        classDecl.getMembers().remove(constant);
        classDecl.getMembers().addFirst(constant);
    }

    private String determineType(String value) {
        if (value.contains(".")) {
            return "double";
        } else if (value.endsWith("L") || value.endsWith("l")) {
            return "long";
        } else {
            return "int";
        }
    }

    private com.github.javaparser.ast.expr.Expression parseValue(String value) {
        if (value.contains(".")) {
            return new DoubleLiteralExpr(value);
        } else if (value.endsWith("L") || value.endsWith("l")) {
            return new LongLiteralExpr(value);
        } else {
            return new IntegerLiteralExpr(value);
        }
    }

    @Override
    public RefactoringResult applyFix(CodeSmell smell, Path filePath, CompilationUnit originalCu) {
        try {
            Optional<CompilationUnit> fixedCuOpt = generateFixedCode(smell, originalCu);
            if (fixedCuOpt.isEmpty()) {
                return new RefactoringResult(false, "Could not generate fixed code",
                        originalCu.toString(), "", List.of());
            }

            CompilationUnit fixedCu = fixedCuOpt.get();
            String refactoredCode = fixedCu.toString();

            Files.writeString(filePath, refactoredCode);

            List<String> changes = Arrays.asList(
                    "Extracted magic numbers to named constants",
                    "Added private static final constants at class level",
                    "Replaced literal values with constant references"
            );

            return new RefactoringResult(true, "Successfully extracted magic numbers to constants",
                    originalCu.toString(), refactoredCode, changes);

        } catch (Exception e) {
            return new RefactoringResult(false, "Error applying fix: " + e.getMessage(),
                    originalCu.toString(), "", List.of());
        }
    }

    @Override
    public RefactoringSuggestion getSuggestion(CodeSmell smell, CompilationUnit originalCu) {
        String beforeCode = "public void calculateDiscount() {\n" +
                "    double discount = price * 0.15;\n" +
                "    if (quantity > 100) {\n" +
                "        discount = price * 0.25;\n" +
                "    }\n" +
                "}";

        String afterCode = "private static final double STANDARD_DISCOUNT_RATE = 0.15;\n" +
                "private static final double BULK_DISCOUNT_RATE = 0.25;\n" +
                "private static final int BULK_QUANTITY_THRESHOLD = 100;\n\n" +
                "public void calculateDiscount() {\n" +
                "    double discount = price * STANDARD_DISCOUNT_RATE;\n" +
                "    if (quantity > BULK_QUANTITY_THRESHOLD) {\n" +
                "        discount = price * BULK_DISCOUNT_RATE;\n" +
                "    }\n" +
                "}";

        return new RefactoringSuggestion.Builder()
                .title("Extract Magic Numbers to Named Constants")
                .description("Magic numbers make code harder to understand and maintain. Extracting them to named constants improves readability and makes the business logic explicit.")
                .difficulty(RefactoringSuggestion.Difficulty.EASY)
                .addStep("Identify all magic numbers in the code (excluding 0, 1, -1)")
                .addStep("Create descriptive constant names that explain the purpose")
                .addStep("Declare constants as private static final at class level")
                .addStep("Replace all occurrences of the magic number with the constant")
                .addStep("Group related constants together for better organization")
                .beforeCode(beforeCode)
                .afterCode(afterCode)
                .autoFixAvailable(true)
                .relatedSmell(smell)
                .build();
    }

    @Override
    public List<String> getSupportedSmellTypes() {
        return Arrays.asList("Magic Number");
    }
}

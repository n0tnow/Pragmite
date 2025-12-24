package com.pragmite.refactoring.autofix;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.ThisExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.pragmite.model.CodeSmell;
import com.pragmite.refactoring.AutoRefactorer;
import com.pragmite.refactoring.RefactoringSuggestion;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Auto-refactorer for field injection code smell.
 * Converts @Autowired field injection to constructor injection.
 */
public class FieldInjectionAutoRefactorer implements AutoRefactorer {

    @Override
    public boolean canAutoFix(CodeSmell smell) {
        return smell.getType() != null &&
               "Field Injection".equalsIgnoreCase(smell.getType().getName());
    }

    @Override
    public Optional<CompilationUnit> generateFixedCode(CodeSmell smell, CompilationUnit originalCu) {
        CompilationUnit fixedCu = originalCu.clone();

        fixedCu.findAll(ClassOrInterfaceDeclaration.class).forEach(classDecl -> {
            // Find all @Autowired fields
            List<FieldDeclaration> autowiredFields = new ArrayList<>();
            classDecl.getFields().forEach(field -> {
                if (field.getAnnotationByName("Autowired").isPresent()) {
                    autowiredFields.add(field);
                }
            });

            if (autowiredFields.isEmpty()) {
                return;
            }

            // Make fields final and remove @Autowired
            autowiredFields.forEach(field -> {
                field.getAnnotationByName("Autowired").ifPresent(ann -> ann.remove());
                field.setModifiers(Modifier.createModifierList(Modifier.Keyword.PRIVATE, Modifier.Keyword.FINAL));
            });

            // Create or update constructor
            Optional<ConstructorDeclaration> existingConstructor = classDecl.getConstructors().stream()
                    .findFirst();

            if (existingConstructor.isPresent()) {
                updateExistingConstructor(existingConstructor.get(), autowiredFields);
            } else {
                createNewConstructor(classDecl, autowiredFields);
            }
        });

        return Optional.of(fixedCu);
    }

    private void createNewConstructor(ClassOrInterfaceDeclaration classDecl, List<FieldDeclaration> fields) {
        ConstructorDeclaration constructor = classDecl.addConstructor(Modifier.Keyword.PUBLIC);
        constructor.addAnnotation("Autowired");

        BlockStmt body = new BlockStmt();

        for (FieldDeclaration field : fields) {
            String fieldName = field.getVariable(0).getNameAsString();
            String fieldType = field.getVariable(0).getTypeAsString();

            // Add parameter
            Parameter param = new Parameter();
            param.setType(fieldType);
            param.setName(fieldName);
            constructor.addParameter(param);

            // Add assignment in constructor body
            FieldAccessExpr thisField = new FieldAccessExpr(new ThisExpr(), fieldName);
            NameExpr paramExpr = new NameExpr(fieldName);
            AssignExpr assignment = new AssignExpr(thisField, paramExpr, AssignExpr.Operator.ASSIGN);
            body.addStatement(new ExpressionStmt(assignment));
        }

        constructor.setBody(body);
    }

    private void updateExistingConstructor(ConstructorDeclaration constructor, List<FieldDeclaration> fields) {
        if (constructor.getAnnotationByName("Autowired").isEmpty()) {
            constructor.addAnnotation("Autowired");
        }

        for (FieldDeclaration field : fields) {
            String fieldName = field.getVariable(0).getNameAsString();
            String fieldType = field.getVariable(0).getTypeAsString();

            // Add parameter if not exists
            boolean paramExists = constructor.getParameters().stream()
                    .anyMatch(p -> p.getNameAsString().equals(fieldName));

            if (!paramExists) {
                Parameter param = new Parameter();
                param.setType(fieldType);
                param.setName(fieldName);
                constructor.addParameter(param);

                // Add assignment
                FieldAccessExpr thisField = new FieldAccessExpr(new ThisExpr(), fieldName);
                NameExpr paramExpr = new NameExpr(fieldName);
                AssignExpr assignment = new AssignExpr(thisField, paramExpr, AssignExpr.Operator.ASSIGN);
                constructor.getBody().addStatement(new ExpressionStmt(assignment));
            }
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

            // Write the fixed code to file
            Files.writeString(filePath, refactoredCode);

            List<String> changes = Arrays.asList(
                    "Removed @Autowired from fields",
                    "Made fields final",
                    "Created/updated constructor with @Autowired",
                    "Added constructor parameters for all dependencies"
            );

            return new RefactoringResult(true, "Successfully converted to constructor injection",
                    originalCu.toString(), refactoredCode, changes);

        } catch (Exception e) {
            return new RefactoringResult(false, "Error applying fix: " + e.getMessage(),
                    originalCu.toString(), "", List.of());
        }
    }

    @Override
    public RefactoringSuggestion getSuggestion(CodeSmell smell, CompilationUnit originalCu) {
        StringBuilder beforeCode = new StringBuilder();
        StringBuilder afterCode = new StringBuilder();

        // Extract example from the code
        originalCu.findAll(FieldDeclaration.class).stream()
                .filter(field -> field.getAnnotationByName("Autowired").isPresent())
                .findFirst()
                .ifPresent(field -> {
                    beforeCode.append("@Autowired\n");
                    beforeCode.append("private ").append(field.getVariable(0).getTypeAsString())
                            .append(" ").append(field.getVariable(0).getNameAsString()).append(";");

                    afterCode.append("private final ").append(field.getVariable(0).getTypeAsString())
                            .append(" ").append(field.getVariable(0).getNameAsString()).append(";\n\n");
                    afterCode.append("@Autowired\n");
                    afterCode.append("public ").append(getClassName(originalCu))
                            .append("(").append(field.getVariable(0).getTypeAsString())
                            .append(" ").append(field.getVariable(0).getNameAsString()).append(") {\n");
                    afterCode.append("    this.").append(field.getVariable(0).getNameAsString())
                            .append(" = ").append(field.getVariable(0).getNameAsString()).append(";\n");
                    afterCode.append("}");
                });

        return new RefactoringSuggestion.Builder()
                .title("Convert Field Injection to Constructor Injection")
                .description("Field injection is discouraged in Spring applications. Constructor injection makes dependencies explicit, enables immutability, and facilitates testing.")
                .difficulty(RefactoringSuggestion.Difficulty.EASY)
                .addStep("Remove @Autowired annotation from fields")
                .addStep("Make fields final")
                .addStep("Create constructor with @Autowired annotation")
                .addStep("Add parameters to constructor for each dependency")
                .addStep("Assign parameters to fields in constructor body")
                .beforeCode(beforeCode.toString())
                .afterCode(afterCode.toString())
                .autoFixAvailable(true)
                .relatedSmell(smell)
                .build();
    }

    @Override
    public List<String> getSupportedSmellTypes() {
        return Arrays.asList("Field Injection");
    }

    private String getClassName(CompilationUnit cu) {
        return cu.findFirst(ClassOrInterfaceDeclaration.class)
                .map(c -> c.getNameAsString())
                .orElse("ClassName");
    }
}

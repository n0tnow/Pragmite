package com.pragmite.refactoring.autofix;

import com.github.javaparser.ast.CompilationUnit;
import com.pragmite.model.CodeSmell;
import com.pragmite.refactoring.AutoRefactorer;
import com.pragmite.refactoring.RefactoringSuggestion;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Suggestion provider for God Class code smell.
 * Provides guidance on decomposing large classes with too many responsibilities.
 */
public class GodClassSuggestionProvider implements AutoRefactorer {

    @Override
    public boolean canAutoFix(CodeSmell smell) {
        // God class refactoring requires architectural decisions
        return false;
    }

    @Override
    public Optional<CompilationUnit> generateFixedCode(CodeSmell smell, CompilationUnit originalCu) {
        return Optional.empty();
    }

    @Override
    public RefactoringResult applyFix(CodeSmell smell, Path filePath, CompilationUnit originalCu) {
        return new RefactoringResult(false,
                "God class requires manual decomposition based on business domains. See suggestion for guidance.",
                originalCu.toString(), "", List.of());
    }

    @Override
    public RefactoringSuggestion getSuggestion(CodeSmell smell, CompilationUnit originalCu) {
        String beforeCode = "public class UserService {\n" +
                "    // User management (100+ lines)\n" +
                "    public User createUser(...) { }\n" +
                "    public User updateUser(...) { }\n" +
                "    public void deleteUser(...) { }\n\n" +
                "    // Email operations (80+ lines)\n" +
                "    public void sendWelcomeEmail(...) { }\n" +
                "    public void sendPasswordResetEmail(...) { }\n\n" +
                "    // Permission checks (60+ lines)\n" +
                "    public boolean hasPermission(...) { }\n" +
                "    public void grantPermission(...) { }\n\n" +
                "    // Notification handling (50+ lines)\n" +
                "    public void notifyAdmins(...) { }\n" +
                "    public void sendNotification(...) { }\n\n" +
                "    // Logging and audit (40+ lines)\n" +
                "    public void logUserAction(...) { }\n" +
                "    public void auditChange(...) { }\n" +
                "}";

        String afterCode = "// Separate classes with single responsibilities\n\n" +
                "public class UserService {\n" +
                "    private final UserRepository userRepository;\n" +
                "    private final UserEmailService emailService;\n" +
                "    private final UserPermissionService permissionService;\n\n" +
                "    public User createUser(...) {\n" +
                "        User user = userRepository.save(...);\n" +
                "        emailService.sendWelcomeEmail(user);\n" +
                "        return user;\n" +
                "    }\n" +
                "}\n\n" +
                "public class UserEmailService {\n" +
                "    public void sendWelcomeEmail(User user) { }\n" +
                "    public void sendPasswordResetEmail(User user) { }\n" +
                "}\n\n" +
                "public class UserPermissionService {\n" +
                "    public boolean hasPermission(User user, Permission p) { }\n" +
                "    public void grantPermission(User user, Permission p) { }\n" +
                "}";

        return new RefactoringSuggestion.Builder()
                .title("Decompose God Class into Smaller, Focused Classes")
                .description("God classes violate the Single Responsibility Principle by doing too much. Break them down into smaller classes, each with a single, well-defined responsibility.")
                .difficulty(RefactoringSuggestion.Difficulty.HARD)
                .addStep("Analyze the class to identify distinct responsibilities/concerns")
                .addStep("Group related methods and fields by their purpose")
                .addStep("Create new service/helper classes for each responsibility")
                .addStep("Extract groups of methods to their appropriate new classes")
                .addStep("Update the original class to delegate to the new classes")
                .addStep("Ensure each new class has a clear, single purpose")
                .addStep("Use dependency injection to wire the classes together")
                .addStep("Update tests to cover the new class structure")
                .beforeCode(beforeCode)
                .afterCode(afterCode)
                .autoFixAvailable(false)
                .relatedSmell(smell)
                .build();
    }

    @Override
    public List<String> getSupportedSmellTypes() {
        return Arrays.asList("God Class");
    }
}

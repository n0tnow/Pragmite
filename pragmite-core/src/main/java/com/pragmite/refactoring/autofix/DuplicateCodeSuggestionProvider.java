package com.pragmite.refactoring.autofix;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.pragmite.model.CodeSmell;
import com.pragmite.refactoring.AutoRefactorer;
import com.pragmite.refactoring.RefactoringSuggestion;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Suggestion provider for duplicate code smell.
 * Due to complexity of code duplication patterns, this provides manual suggestions
 * rather than automatic fixes.
 */
public class DuplicateCodeSuggestionProvider implements AutoRefactorer {

    @Override
    public boolean canAutoFix(CodeSmell smell) {
        // Duplicate code is complex and requires manual review
        return false;
    }

    @Override
    public Optional<CompilationUnit> generateFixedCode(CodeSmell smell, CompilationUnit originalCu) {
        // Cannot automatically fix duplicate code due to context requirements
        return Optional.empty();
    }

    @Override
    public RefactoringResult applyFix(CodeSmell smell, Path filePath, CompilationUnit originalCu) {
        return new RefactoringResult(false,
                "Duplicate code requires manual refactoring. See suggestion for guidance.",
                originalCu.toString(), "", List.of());
    }

    @Override
    public RefactoringSuggestion getSuggestion(CodeSmell smell, CompilationUnit originalCu) {
        String beforeCode = "public class UserService {\n" +
                "    public UserDto getUser(Long id) {\n" +
                "        User user = userRepo.findById(id).orElseThrow();\n" +
                "        UserDto dto = new UserDto();\n" +
                "        dto.setId(user.getId());\n" +
                "        dto.setName(user.getName());\n" +
                "        dto.setEmail(user.getEmail());\n" +
                "        return dto;\n" +
                "    }\n\n" +
                "    public List<UserDto> getAllUsers() {\n" +
                "        return userRepo.findAll().stream()\n" +
                "            .map(user -> {\n" +
                "                UserDto dto = new UserDto();\n" +
                "                dto.setId(user.getId());\n" +
                "                dto.setName(user.getName());\n" +
                "                dto.setEmail(user.getEmail());\n" +
                "                return dto;\n" +
                "            })\n" +
                "            .collect(Collectors.toList());\n" +
                "    }\n" +
                "}";

        String afterCode = "public class UserService {\n" +
                "    // Extract common mapping logic to a separate method\n" +
                "    private UserDto convertToDto(User user) {\n" +
                "        UserDto dto = new UserDto();\n" +
                "        dto.setId(user.getId());\n" +
                "        dto.setName(user.getName());\n" +
                "        dto.setEmail(user.getEmail());\n" +
                "        return dto;\n" +
                "    }\n\n" +
                "    public UserDto getUser(Long id) {\n" +
                "        User user = userRepo.findById(id).orElseThrow();\n" +
                "        return convertToDto(user);\n" +
                "    }\n\n" +
                "    public List<UserDto> getAllUsers() {\n" +
                "        return userRepo.findAll().stream()\n" +
                "            .map(this::convertToDto)\n" +
                "            .collect(Collectors.toList());\n" +
                "    }\n" +
                "}";

        return new RefactoringSuggestion.Builder()
                .title("Extract Duplicate Code to Reusable Method")
                .description("Duplicate code increases maintenance burden and the risk of bugs. Extract common logic into a separate method to follow the DRY (Don't Repeat Yourself) principle.")
                .difficulty(RefactoringSuggestion.Difficulty.MEDIUM)
                .addStep("Identify the exact duplicated code blocks")
                .addStep("Determine if the duplicated code has the same intent")
                .addStep("Extract the common logic into a new private method")
                .addStep("Choose a descriptive name that explains the method's purpose")
                .addStep("Parameterize any values that differ between duplicates")
                .addStep("Replace all duplicate occurrences with calls to the new method")
                .addStep("Run tests to ensure behavior hasn't changed")
                .beforeCode(beforeCode)
                .afterCode(afterCode)
                .autoFixAvailable(false)
                .relatedSmell(smell)
                .build();
    }

    @Override
    public List<String> getSupportedSmellTypes() {
        return Arrays.asList("Duplicated Code");
    }
}

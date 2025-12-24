package com.pragmite.refactoring;

import com.github.javaparser.ast.CompilationUnit;
import com.pragmite.model.CodeSmell;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/**
 * Interface for automatic code refactoring capabilities.
 * Implementations can analyze code smells and automatically generate fixed code.
 */
public interface AutoRefactorer {

    /**
     * Checks if this refactorer can automatically fix the given code smell.
     *
     * @param smell The code smell to check
     * @return true if auto-fix is supported for this smell
     */
    boolean canAutoFix(CodeSmell smell);

    /**
     * Generates refactored code for the given code smell.
     *
     * @param smell The code smell to fix
     * @param originalCu The original compilation unit
     * @return Refactored compilation unit, or empty if fix cannot be applied
     */
    Optional<CompilationUnit> generateFixedCode(CodeSmell smell, CompilationUnit originalCu);

    /**
     * Applies the fix to the actual file.
     *
     * @param smell The code smell to fix
     * @param filePath The path to the file to fix
     * @param originalCu The original compilation unit
     * @return Result of the refactoring operation
     */
    RefactoringResult applyFix(CodeSmell smell, Path filePath, CompilationUnit originalCu);

    /**
     * Gets a refactoring suggestion for the given code smell.
     *
     * @param smell The code smell to analyze
     * @param originalCu The original compilation unit
     * @return Refactoring suggestion with fix instructions
     */
    RefactoringSuggestion getSuggestion(CodeSmell smell, CompilationUnit originalCu);

    /**
     * Gets all supported smell types that this refactorer can handle.
     *
     * @return List of supported smell type names
     */
    List<String> getSupportedSmellTypes();

    /**
     * Result of a refactoring operation.
     */
    class RefactoringResult {
        private final boolean success;
        private final String message;
        private final String originalCode;
        private final String refactoredCode;
        private final List<String> changes;

        public RefactoringResult(boolean success, String message, String originalCode,
                                String refactoredCode, List<String> changes) {
            this.success = success;
            this.message = message;
            this.originalCode = originalCode;
            this.refactoredCode = refactoredCode;
            this.changes = changes;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }

        public String getOriginalCode() {
            return originalCode;
        }

        public String getRefactoredCode() {
            return refactoredCode;
        }

        public List<String> getChanges() {
            return changes;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("RefactoringResult{");
            sb.append("success=").append(success);
            sb.append(", message='").append(message).append('\'');
            if (!changes.isEmpty()) {
                sb.append(", changes=").append(changes.size());
            }
            sb.append('}');
            return sb.toString();
        }
    }
}

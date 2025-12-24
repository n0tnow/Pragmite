package com.pragmite.refactoring;

import com.github.javaparser.ast.CompilationUnit;
import com.pragmite.model.CodeSmell;
import com.pragmite.refactoring.autofix.*;

import java.nio.file.Path;
import java.util.*;

/**
 * Central manager for refactoring suggestions and auto-fixes.
 * Coordinates different refactorers and provides unified access.
 */
public class RefactoringManager {

    private final Map<String, AutoRefactorer> refactorers = new HashMap<>();

    public RefactoringManager() {
        registerDefaultRefactorers();
    }

    private void registerDefaultRefactorers() {
        // Register all available refactorers
        registerRefactorer(new FieldInjectionAutoRefactorer());
        registerRefactorer(new MagicNumberAutoRefactorer());
        registerRefactorer(new DuplicateCodeSuggestionProvider());
        registerRefactorer(new GodClassSuggestionProvider());
        registerRefactorer(new LongMethodSuggestionProvider());
    }

    /**
     * Registers a new refactorer.
     */
    public void registerRefactorer(AutoRefactorer refactorer) {
        for (String smellType : refactorer.getSupportedSmellTypes()) {
            refactorers.put(smellType.toLowerCase(), refactorer);
        }
    }

    /**
     * Gets a refactoring suggestion for a code smell.
     *
     * @param smell The code smell to analyze
     * @param cu The compilation unit containing the smell
     * @return Refactoring suggestion, or empty if no refactorer available
     */
    public Optional<RefactoringSuggestion> getSuggestion(CodeSmell smell, CompilationUnit cu) {
        if (smell.getType() == null) {
            return Optional.empty();
        }
        AutoRefactorer refactorer = refactorers.get(smell.getType().getName().toLowerCase());
        if (refactorer == null) {
            return Optional.empty();
        }

        try {
            return Optional.of(refactorer.getSuggestion(smell, cu));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    /**
     * Gets refactoring suggestions for all code smells.
     *
     * @param smells List of code smells
     * @param cu The compilation unit
     * @return List of refactoring suggestions
     */
    public List<RefactoringSuggestion> getSuggestions(List<CodeSmell> smells, CompilationUnit cu) {
        List<RefactoringSuggestion> suggestions = new ArrayList<>();

        for (CodeSmell smell : smells) {
            getSuggestion(smell, cu).ifPresent(suggestions::add);
        }

        return suggestions;
    }

    /**
     * Checks if auto-fix is available for a code smell.
     *
     * @param smell The code smell to check
     * @return true if auto-fix is available
     */
    public boolean canAutoFix(CodeSmell smell) {
        if (smell.getType() == null) {
            return false;
        }
        AutoRefactorer refactorer = refactorers.get(smell.getType().getName().toLowerCase());
        return refactorer != null && refactorer.canAutoFix(smell);
    }

    /**
     * Applies automatic fix to a code smell.
     *
     * @param smell The code smell to fix
     * @param filePath Path to the file
     * @param cu The compilation unit
     * @return Result of the refactoring operation
     */
    public AutoRefactorer.RefactoringResult applyAutoFix(CodeSmell smell, Path filePath, CompilationUnit cu) {
        if (smell.getType() == null) {
            return new AutoRefactorer.RefactoringResult(
                    false,
                    "Code smell type is null",
                    cu.toString(),
                    "",
                    List.of()
            );
        }
        AutoRefactorer refactorer = refactorers.get(smell.getType().getName().toLowerCase());

        if (refactorer == null) {
            return new AutoRefactorer.RefactoringResult(
                    false,
                    "No refactorer available for smell type: " + smell.getType(),
                    cu.toString(),
                    "",
                    List.of()
            );
        }

        if (!refactorer.canAutoFix(smell)) {
            return new AutoRefactorer.RefactoringResult(
                    false,
                    "Auto-fix not available for smell type: " + smell.getType(),
                    cu.toString(),
                    "",
                    List.of()
            );
        }

        try {
            return refactorer.applyFix(smell, filePath, cu);
        } catch (Exception e) {
            return new AutoRefactorer.RefactoringResult(
                    false,
                    "Error applying auto-fix: " + e.getMessage(),
                    cu.toString(),
                    "",
                    List.of()
            );
        }
    }

    /**
     * Gets statistics about available refactorers.
     *
     * @return Statistics summary
     */
    public RefactoringStats getStats() {
        int totalRefactorers = (int) refactorers.values().stream().distinct().count();
        int autoFixCapable = (int) refactorers.values().stream()
                .distinct()
                .filter(r -> {
                    // Check if at least one supported smell type can be auto-fixed
                    for (String typeName : r.getSupportedSmellTypes()) {
                        try {
                            // Find matching CodeSmellType by name
                            for (com.pragmite.model.CodeSmellType smellType : com.pragmite.model.CodeSmellType.values()) {
                                if (smellType.getName().equalsIgnoreCase(typeName)) {
                                    CodeSmell testSmell = new CodeSmell(smellType, "test", 1, "test");
                                    if (r.canAutoFix(testSmell)) {
                                        return true;
                                    }
                                }
                            }
                        } catch (Exception e) {
                            // Ignore
                        }
                    }
                    return false;
                })
                .count();
        int supportedSmellTypes = refactorers.size();

        return new RefactoringStats(totalRefactorers, autoFixCapable, supportedSmellTypes);
    }

    /**
     * Gets all registered smell types that have refactoring support.
     *
     * @return Set of supported smell type names
     */
    public Set<String> getSupportedSmellTypes() {
        return new HashSet<>(refactorers.keySet());
    }

    /**
     * Statistics about refactoring capabilities.
     */
    public static class RefactoringStats {
        private final int totalRefactorers;
        private final int autoFixCapable;
        private final int supportedSmellTypes;

        public RefactoringStats(int totalRefactorers, int autoFixCapable, int supportedSmellTypes) {
            this.totalRefactorers = totalRefactorers;
            this.autoFixCapable = autoFixCapable;
            this.supportedSmellTypes = supportedSmellTypes;
        }

        public int getTotalRefactorers() {
            return totalRefactorers;
        }

        public int getAutoFixCapable() {
            return autoFixCapable;
        }

        public int getSupportedSmellTypes() {
            return supportedSmellTypes;
        }

        @Override
        public String toString() {
            return String.format(
                    "Refactoring Stats: %d refactorers, %d with auto-fix, %d smell types supported",
                    totalRefactorers, autoFixCapable, supportedSmellTypes
            );
        }
    }
}

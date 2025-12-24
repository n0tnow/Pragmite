package com.pragmite.refactoring;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.pragmite.model.CodeSmell;
import com.pragmite.model.CodeSmellType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class RefactoringManagerTest {

    private RefactoringManager manager;
    private CompilationUnit sampleCu;

    @BeforeEach
    void setUp() {
        manager = new RefactoringManager();

        String sampleCode = """
                package com.example;
                import org.springframework.beans.factory.annotation.Autowired;

                public class TestService {
                    @Autowired
                    private UserRepository userRepo;

                    public void processData() {
                        int threshold = 100;
                        if (threshold > 50) {
                            System.out.println("High threshold");
                        }
                    }
                }
                """;

        sampleCu = new JavaParser().parse(sampleCode).getResult().orElseThrow();
    }

    @Test
    void testGetSuggestionForFieldInjection() {
        CodeSmell smell = new CodeSmell(
                CodeSmellType.FIELD_INJECTION,
                "TestService.java",
                5,
                "Field injection detected"
        );

        Optional<RefactoringSuggestion> suggestion = manager.getSuggestion(smell, sampleCu);
        assertTrue(suggestion.isPresent());
        assertEquals("Convert Field Injection to Constructor Injection", suggestion.get().getTitle());
        assertTrue(suggestion.get().isAutoFixAvailable());
    }

    @Test
    void testGetSuggestionForMagicNumber() {
        CodeSmell smell = new CodeSmell(
                CodeSmellType.MAGIC_NUMBER,
                "TestService.java",
                9,
                "Magic number detected"
        );

        Optional<RefactoringSuggestion> suggestion = manager.getSuggestion(smell, sampleCu);
        assertTrue(suggestion.isPresent());
        assertEquals("Extract Magic Numbers to Named Constants", suggestion.get().getTitle());
        assertTrue(suggestion.get().isAutoFixAvailable());
    }

    @Test
    void testGetSuggestionForGodClass() {
        CodeSmell smell = new CodeSmell(
                CodeSmellType.GOD_CLASS,
                "TestService.java",
                1,
                "God class detected"
        );

        Optional<RefactoringSuggestion> suggestion = manager.getSuggestion(smell, sampleCu);
        assertTrue(suggestion.isPresent());
        assertEquals("Decompose God Class into Smaller, Focused Classes", suggestion.get().getTitle());
        assertFalse(suggestion.get().isAutoFixAvailable());
        assertEquals(RefactoringSuggestion.Difficulty.HARD, suggestion.get().getDifficulty());
    }

    @Test
    void testGetSuggestionForLongMethod() {
        CodeSmell smell = new CodeSmell(
                CodeSmellType.LONG_METHOD,
                "TestService.java",
                8,
                "Long method detected"
        );

        Optional<RefactoringSuggestion> suggestion = manager.getSuggestion(smell, sampleCu);
        assertTrue(suggestion.isPresent());
        assertEquals("Extract Long Method into Smaller Methods", suggestion.get().getTitle());
        assertFalse(suggestion.get().isAutoFixAvailable());
    }

    @Test
    void testGetSuggestionForDuplicateCode() {
        CodeSmell smell = new CodeSmell(
                CodeSmellType.DUPLICATED_CODE,
                "TestService.java",
                10,
                "Duplicate code detected"
        );

        Optional<RefactoringSuggestion> suggestion = manager.getSuggestion(smell, sampleCu);
        assertTrue(suggestion.isPresent());
        assertEquals("Extract Duplicate Code to Reusable Method", suggestion.get().getTitle());
        assertFalse(suggestion.get().isAutoFixAvailable());
    }

    @Test
    void testGetSuggestionForUnsupportedSmell() {
        CodeSmell smell = new CodeSmell(
                CodeSmellType.UNUSED_IMPORT,
                "TestService.java",
                1,
                "Unused import"
        );

        Optional<RefactoringSuggestion> suggestion = manager.getSuggestion(smell, sampleCu);
        assertFalse(suggestion.isPresent());
    }

    @Test
    void testGetSuggestionsForMultipleSmells() {
        List<CodeSmell> smells = List.of(
                new CodeSmell(CodeSmellType.FIELD_INJECTION, "Test.java", 1, "Field injection"),
                new CodeSmell(CodeSmellType.MAGIC_NUMBER, "Test.java", 5, "Magic number"),
                new CodeSmell(CodeSmellType.GOD_CLASS, "Test.java", 1, "God class")
        );

        List<RefactoringSuggestion> suggestions = manager.getSuggestions(smells, sampleCu);
        assertEquals(3, suggestions.size());
    }

    @Test
    void testCanAutoFix() {
        CodeSmell fieldInjectionSmell = new CodeSmell(
                CodeSmellType.FIELD_INJECTION,
                "Test.java",
                1,
                "Field injection"
        );
        assertTrue(manager.canAutoFix(fieldInjectionSmell));

        CodeSmell godClassSmell = new CodeSmell(
                CodeSmellType.GOD_CLASS,
                "Test.java",
                1,
                "God class"
        );
        assertFalse(manager.canAutoFix(godClassSmell));
    }

    @Test
    void testGetStats() {
        RefactoringManager.RefactoringStats stats = manager.getStats();
        assertNotNull(stats);
        assertTrue(stats.getTotalRefactorers() > 0);
        assertTrue(stats.getSupportedSmellTypes() > 0);
    }

    @Test
    void testGetSupportedSmellTypes() {
        var supportedTypes = manager.getSupportedSmellTypes();
        assertFalse(supportedTypes.isEmpty());

        // Supported types are lowercase
        assertTrue(supportedTypes.stream().anyMatch(t -> t.equalsIgnoreCase("field injection")),
                "Should contain field injection. Actual: " + supportedTypes);
        assertTrue(supportedTypes.stream().anyMatch(t -> t.equalsIgnoreCase("magic number")));
        assertTrue(supportedTypes.stream().anyMatch(t -> t.equalsIgnoreCase("god class")));
        assertTrue(supportedTypes.stream().anyMatch(t -> t.equalsIgnoreCase("long method")));
        assertTrue(supportedTypes.stream().anyMatch(t -> t.equalsIgnoreCase("duplicated code")));
    }

    @Test
    void testSuggestionFormatting() {
        CodeSmell smell = new CodeSmell(
                CodeSmellType.FIELD_INJECTION,
                "TestService.java",
                5,
                "Field injection detected"
        );

        Optional<RefactoringSuggestion> suggestionOpt = manager.getSuggestion(smell, sampleCu);
        assertTrue(suggestionOpt.isPresent());

        RefactoringSuggestion suggestion = suggestionOpt.get();
        String formatted = suggestion.formatAsText();

        assertNotNull(formatted);
        assertTrue(formatted.contains("REFACTORING SUGGESTION"));
        assertTrue(formatted.contains("Convert Field Injection"));
        assertTrue(formatted.contains("Steps to Fix"));
        assertTrue(formatted.contains("Before:"));
        assertTrue(formatted.contains("After:"));
    }

    @Test
    void testSuggestionSteps() {
        CodeSmell smell = new CodeSmell(
                CodeSmellType.FIELD_INJECTION,
                "TestService.java",
                5,
                "Field injection detected"
        );

        Optional<RefactoringSuggestion> suggestionOpt = manager.getSuggestion(smell, sampleCu);
        assertTrue(suggestionOpt.isPresent());

        List<String> steps = suggestionOpt.get().getSteps();
        assertFalse(steps.isEmpty());
        assertTrue(steps.get(0).contains("Remove @Autowired"));
    }

    @Test
    void testDifficultyLevels() {
        CodeSmell easySmell = new CodeSmell(
                CodeSmellType.MAGIC_NUMBER,
                "Test.java",
                1,
                "Magic number"
        );
        Optional<RefactoringSuggestion> easySuggestion = manager.getSuggestion(easySmell, sampleCu);
        assertTrue(easySuggestion.isPresent());
        assertEquals(RefactoringSuggestion.Difficulty.EASY, easySuggestion.get().getDifficulty());

        CodeSmell mediumSmell = new CodeSmell(
                CodeSmellType.LONG_METHOD,
                "Test.java",
                1,
                "Long method"
        );
        Optional<RefactoringSuggestion> mediumSuggestion = manager.getSuggestion(mediumSmell, sampleCu);
        assertTrue(mediumSuggestion.isPresent());
        assertEquals(RefactoringSuggestion.Difficulty.MEDIUM, mediumSuggestion.get().getDifficulty());

        CodeSmell hardSmell = new CodeSmell(
                CodeSmellType.GOD_CLASS,
                "Test.java",
                1,
                "God class"
        );
        Optional<RefactoringSuggestion> hardSuggestion = manager.getSuggestion(hardSmell, sampleCu);
        assertTrue(hardSuggestion.isPresent());
        assertEquals(RefactoringSuggestion.Difficulty.HARD, hardSuggestion.get().getDifficulty());
    }
}

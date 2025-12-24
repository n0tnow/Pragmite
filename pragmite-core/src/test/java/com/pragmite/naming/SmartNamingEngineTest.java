package com.pragmite.naming;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SmartNamingEngineTest {

    private SmartNamingEngine engine;

    @BeforeEach
    void setUp() {
        engine = new SmartNamingEngine();
    }

    @Test
    void testSuggestVariableName_ForString() {
        List<SmartNamingEngine.NamingSuggestion> suggestions =
            engine.suggestVariableName("tmp", "String", "user input", null);

        assertFalse(suggestions.isEmpty(), "Should provide naming suggestions");
        assertTrue(suggestions.size() <= 5, "Should limit suggestions to 5");
    }

    @Test
    void testSuggestVariableName_ForInt() {
        List<SmartNamingEngine.NamingSuggestion> suggestions =
            engine.suggestVariableName("x", "int", "counter", null);

        assertFalse(suggestions.isEmpty());

        // Should suggest common int names
        boolean hasCommonIntName = suggestions.stream()
            .anyMatch(s -> s.getSuggestedName().equals("count") ||
                          s.getSuggestedName().equals("index") ||
                          s.getSuggestedName().equals("size"));
        assertTrue(hasCommonIntName, "Should suggest common int names");
    }

    @Test
    void testSuggestVariableName_ForBoolean() {
        // Use a poor name that will trigger suggestions
        List<SmartNamingEngine.NamingSuggestion> suggestions =
            engine.suggestVariableName("x", "boolean", "validation", null);

        // Poor boolean names should get suggestions with is/has/can prefixes
        assertFalse(suggestions.isEmpty(), "Poor boolean name should get suggestions");

        // Verify suggestions include typical boolean names
        boolean hasBooleanSuggestion = suggestions.stream()
            .anyMatch(s -> s.getSuggestedName().startsWith("is") ||
                          s.getSuggestedName().startsWith("has") ||
                          s.getSuggestedName().startsWith("can") ||
                          s.getSuggestedName().equals("enabled") ||
                          s.getSuggestedName().equals("isValid"));
        assertTrue(hasBooleanSuggestion, "Should suggest boolean-style names");
    }

    @Test
    void testSuggestVariableName_GoodNameReturnsEmpty() {
        List<SmartNamingEngine.NamingSuggestion> suggestions =
            engine.suggestVariableName("userName", "String", "", null);

        // Good camelCase name should return few or no suggestions
        assertTrue(suggestions.size() < 3, "Good names should have few suggestions");
    }

    @Test
    void testSuggestVariableName_WithCodebaseContext() {
        String code = """
            public class Test {
                String customerName;
                String customerEmail;
                String customerAddress;
            }
            """;

        CompilationUnit cu = StaticJavaParser.parse(code);

        // Use a clearly poor name to trigger suggestions
        List<SmartNamingEngine.NamingSuggestion> suggestions =
            engine.suggestVariableName("x", "String", "customer", cu);

        // Should provide suggestions when codebase context is available
        assertFalse(suggestions.isEmpty(), "Should provide naming suggestions with codebase context");

        // Verify suggestions are limited and scored
        assertTrue(suggestions.size() <= 5, "Should limit suggestions to 5");

        // All suggestions should have valid scores
        for (SmartNamingEngine.NamingSuggestion suggestion : suggestions) {
            assertTrue(suggestion.getScore() >= 0 && suggestion.getScore() <= 1,
                      "Score should be normalized between 0 and 1");
            assertNotNull(suggestion.getReason(), "Suggestion should have a reason");
        }
    }

    @Test
    void testSuggestionScoring() {
        List<SmartNamingEngine.NamingSuggestion> suggestions =
            engine.suggestVariableName("tmp", "String", "message text", null);

        if (!suggestions.isEmpty()) {
            // Suggestions should be sorted by score (highest first)
            for (int i = 0; i < suggestions.size() - 1; i++) {
                double currentScore = suggestions.get(i).getScore();
                double nextScore = suggestions.get(i + 1).getScore();
                assertTrue(currentScore >= nextScore,
                          "Suggestions should be sorted by score descending");
            }

            // All scores should be between 0 and 1
            for (SmartNamingEngine.NamingSuggestion suggestion : suggestions) {
                assertTrue(suggestion.getScore() >= 0 && suggestion.getScore() <= 1,
                          "Score should be normalized between 0 and 1");
            }
        }
    }

    @Test
    void testSuggestionToString() {
        List<SmartNamingEngine.NamingSuggestion> suggestions =
            engine.suggestVariableName("x", "int", "count", null);

        if (!suggestions.isEmpty()) {
            SmartNamingEngine.NamingSuggestion suggestion = suggestions.get(0);
            String str = suggestion.toString();

            assertNotNull(str);
            assertTrue(str.contains(suggestion.getSuggestedName()));
            assertTrue(str.contains("%"));  // Should show percentage
        }
    }
}

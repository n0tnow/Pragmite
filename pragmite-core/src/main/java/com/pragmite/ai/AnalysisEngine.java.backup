package com.pragmite.ai;

import com.pragmite.model.CodeSmell;
import com.pragmite.model.CodeSmellType;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Core AI analysis orchestrator that generates detailed explanations and AI prompts
 * for detected code smells.
 *
 * This engine:
 * 1. Analyzes code smells to identify root causes
 * 2. Assesses impact and provides recommendations
 * 3. Generates ready-to-use AI prompts for Claude/GPT-4/Gemini
 * 4. Extracts relevant code context
 *
 * @since 1.4.0
 */
public class AnalysisEngine {

    private final PromptGenerator promptGenerator;
    private final ContextExtractor contextExtractor;

    public AnalysisEngine() {
        this.promptGenerator = new PromptGenerator();
        this.contextExtractor = new ContextExtractor();
    }

    /**
     * Analyzes a single code smell and generates AI-powered insights.
     *
     * @param smell The code smell to analyze
     * @param sourceCode The complete source code of the file
     * @return AIAnalysisResult containing root cause, impact, recommendations, and AI prompt
     */
    public AIAnalysisResult analyze(CodeSmell smell, String sourceCode) {
        AIAnalysisResult.Builder builder = AIAnalysisResult.builder(smell);

        // Extract root cause based on smell type
        String rootCause = analyzeRootCause(smell);
        builder.rootCause(rootCause);

        // Assess impact
        String impact = assessImpact(smell);
        builder.impact(impact);

        // Generate recommendation
        String recommendation = generateRecommendation(smell);
        builder.recommendation(recommendation);

        // Extract relevant code snippets
        List<String> snippets = contextExtractor.extractContext(smell, sourceCode);
        snippets.forEach(builder::addCodeSnippet);

        // Generate AI prompt
        String aiPrompt = promptGenerator.generatePrompt(smell, sourceCode, snippets);
        builder.aiPrompt(aiPrompt);

        // Add metadata
        builder.addMetadata("severity", smell.getSeverity().toString());
        builder.addMetadata("category", getCategoryForType(smell.getType()));
        builder.addMetadata("generatedAt", java.time.Instant.now().toString());

        return builder.build();
    }

    /**
     * Analyzes multiple code smells and generates AI-powered insights for each.
     *
     * @param smells List of code smells to analyze
     * @param projectRoot Root directory of the project
     * @return List of AIAnalysisResult objects
     */
    public List<AIAnalysisResult> analyzeAll(List<CodeSmell> smells, Path projectRoot) {
        return analyzeAll(smells, projectRoot, null);
    }

    /**
     * Analyzes multiple code smells and generates AI-powered insights with optional auto-refactoring.
     *
     * @param smells List of code smells to analyze
     * @param projectRoot Root directory of the project
     * @param apiConfig API configuration for auto-refactoring (null to skip)
     * @return List of AIAnalysisResult objects
     */
    public List<AIAnalysisResult> analyzeAll(List<CodeSmell> smells, Path projectRoot, ApiConfig apiConfig) {
        List<AIAnalysisResult> results = new ArrayList<>();
        ClaudeApiClient claudeClient = null;

        // Initialize Claude client if auto-refactoring is enabled
        if (apiConfig != null && apiConfig.isValid()) {
            claudeClient = new ClaudeApiClient(apiConfig);
            System.out.println("🤖 Auto-refactoring enabled with Claude API");
        }

        for (CodeSmell smell : smells) {
            try {
                // Try to resolve as absolute first, if it fails, try relative to project root
                Path filePath = Paths.get(smell.getFilePath());
                if (!Files.exists(filePath)) {
                    filePath = projectRoot.resolve(smell.getFilePath());
                }
                String sourceCode = Files.readString(filePath);
                AIAnalysisResult baseResult = analyze(smell, sourceCode);

                // Generate refactored code if enabled
                if (claudeClient != null) {
                    String aiPrompt = baseResult.getAiPrompt();
                    RefactoredCode refactored = claudeClient.generateRefactoring(aiPrompt, sourceCode);

                    // Rebuild with refactored code
                    AIAnalysisResult.Builder builder = AIAnalysisResult.builder(smell)
                        .rootCause(baseResult.getRootCause())
                        .impact(baseResult.getImpact())
                        .recommendation(baseResult.getRecommendation())
                        .aiPrompt(aiPrompt)
                        .refactoredCode(refactored);

                    // Copy code snippets and metadata
                    baseResult.getCodeSnippets().forEach(builder::addCodeSnippet);
                    baseResult.getMetadata().forEach(builder::addMetadata);

                    if (refactored.isSuccessful()) {
                        System.out.println("  ✅ Refactored: " + smell.getFilePath() + ":" + smell.getLine());
                    } else {
                        System.out.println("  ⚠️  Refactoring failed: " + refactored.getErrorMessage());
                    }

                    results.add(builder.build());
                } else {
                    results.add(baseResult);
                }
            } catch (IOException e) {
                // Skip files that cannot be read
                System.err.println("Warning: Could not read file for AI analysis: " + smell.getFilePath());
            }
        }

        return results;
    }

    /**
     * Analyzes root cause of a code smell based on its type and context.
     */
    private String analyzeRootCause(CodeSmell smell) {
        return switch (smell.getType()) {
            case LONG_METHOD ->
                "This method exceeds " + extractNumber(smell.getMessage(), "default length threshold") +
                " lines, violating the Single Responsibility Principle. " +
                "It likely handles multiple concerns that should be separated into distinct methods.";

            case LARGE_CLASS ->
                "This class has " + extractNumber(smell.getMessage(), "many") +
                " methods, indicating it manages too many responsibilities. " +
                "This violates cohesion principles and makes the class difficult to maintain.";

            case HIGH_CYCLOMATIC_COMPLEXITY ->
                "Cyclomatic complexity of " + extractNumber(smell.getMessage(), "high") +
                " indicates too many decision points (if/else, switch, loops). " +
                "This makes the method difficult to understand, test, and maintain.";

            case DUPLICATED_CODE ->
                "Similar code blocks detected across multiple locations. " +
                "This violates the DRY (Don't Repeat Yourself) principle and creates maintenance burden.";

            case DEEPLY_NESTED_CODE ->
                "Nesting depth of " + extractNumber(smell.getMessage(), "several") +
                " levels makes the control flow difficult to follow. " +
                "This often indicates missing abstraction or early return opportunities.";

            case LONG_PARAMETER_LIST ->
                "Method has " + extractNumber(smell.getMessage(), "many") +
                " parameters, making the method signature complex and error-prone. " +
                "This often indicates missing parameter object abstraction.";

            case PRIMITIVE_OBSESSION ->
                "Using primitive types instead of domain objects reduces type safety " +
                "and scatters domain logic across the codebase.";

            case DATA_CLUMPS ->
                "The same group of variables appears together in multiple places, " +
                "indicating missing abstraction for a cohesive concept.";

            case FEATURE_ENVY ->
                "This method accesses data/methods from another class more than its own, " +
                "suggesting misplaced responsibility.";

            case GOD_CLASS ->
                "This class with " + extractNumber(smell.getMessage(), "many") +
                " lines knows too much and does too much, centralizing system knowledge.";

            case MAGIC_NUMBER ->
                "Unexplained numeric literals reduce code readability and maintainability. " +
                "The number's meaning is lost without context.";

            case MAGIC_STRING ->
                "Unexplained string literals reduce code readability and maintainability. " +
                "The string's purpose is unclear without context.";

            case DEAD_CODE ->
                "Unused code increases maintenance burden, confuses developers, " +
                "and may hide bugs or security issues.";

            case UNUSED_IMPORT, UNUSED_VARIABLE, UNUSED_PARAMETER ->
                "Unused code elements clutter the codebase and may indicate incomplete refactoring.";

            case EMPTY_CATCH_BLOCK ->
                "Empty catch blocks hide errors and can cause silent failures. " +
                "Errors should be logged, handled, or rethrown appropriately.";

            case MISSING_TRY_WITH_RESOURCES ->
                "Resources not managed with try-with-resources can lead to resource leaks, " +
                "file descriptor exhaustion, or connection pool starvation.";

            case STRING_CONCAT_IN_LOOP ->
                "String concatenation in loops creates multiple temporary objects, " +
                "causing performance degradation. Use StringBuilder instead.";

            case INEFFICIENT_COLLECTION ->
                "Inefficient collection usage can lead to poor performance. " +
                "Consider using appropriate data structures for the use case.";

            case FIELD_INJECTION ->
                "Field injection makes testing difficult and hides dependencies. " +
                "Constructor injection is preferred for better testability.";

            case DATA_CLASS ->
                "Class only holds data without behavior, violating object-oriented design principles. " +
                "Consider adding methods that operate on the data.";

            case INAPPROPRIATE_INTIMACY ->
                "Excessive coupling between classes reduces flexibility " +
                "and makes testing difficult. Classes should have well-defined boundaries.";

            case LAZY_CLASS ->
                "This class does too little to justify its existence. " +
                "Consider merging it with related classes or removing it.";

            case SPECULATIVE_GENERALITY ->
                "Unnecessary abstraction creates complexity without clear benefit. " +
                "Keep designs simple until abstraction is actually needed.";

            case SWITCH_STATEMENT ->
                "Large switch statements are hard to maintain and violate the Open/Closed Principle. " +
                "Consider using polymorphism or strategy pattern.";

            case MESSAGE_CHAIN ->
                "Long chain of method calls violates the Law of Demeter. " +
                "This creates tight coupling and makes code brittle.";

            case MIDDLE_MAN ->
                "Class delegates most of its work to another class, adding unnecessary indirection. " +
                "Consider removing the middleman or adding more value.";
        };
    }

    /**
     * Assesses the impact of a code smell.
     */
    private String assessImpact(CodeSmell smell) {
        String severityImpact = switch (smell.getSeverity()) {
            case BLOCKER -> "BLOCKER impact - Production deployment blocked. Immediate action required. ";
            case CRITICAL -> "CRITICAL impact - Immediate action required. ";
            case MAJOR -> "MAJOR impact - Should be addressed in current sprint. ";
            case MINOR -> "MINOR impact - Plan for upcoming sprint. ";
            case INFO -> "INFO - Address when refactoring nearby code. ";
        };

        String typeImpact = switch (smell.getType()) {
            case LONG_METHOD, HIGH_CYCLOMATIC_COMPLEXITY ->
                "Increases bug probability, makes testing difficult, and slows down feature development.";
            case LARGE_CLASS, GOD_CLASS ->
                "Creates bottleneck for changes, increases merge conflicts, and makes onboarding harder.";
            case DUPLICATED_CODE ->
                "Bugs must be fixed in multiple places, changes require finding all duplicates.";
            case DEEPLY_NESTED_CODE ->
                "Higher cognitive load for developers, increased bug risk in edge cases.";
            case LONG_PARAMETER_LIST ->
                "Error-prone method calls, difficult to refactor, poor readability.";
            case PRIMITIVE_OBSESSION ->
                "Loss of type safety, scattered validation logic, difficult to enforce invariants.";
            case INAPPROPRIATE_INTIMACY ->
                "Difficult to test in isolation, changes cascade across modules.";
            case MISSING_TRY_WITH_RESOURCES ->
                "Memory exhaustion, application crashes, performance degradation over time.";
            case EMPTY_CATCH_BLOCK ->
                "Hidden bugs, silent failures, difficult debugging.";
            case STRING_CONCAT_IN_LOOP, INEFFICIENT_COLLECTION ->
                "Performance degradation, especially with large data sets.";
            case UNUSED_IMPORT, UNUSED_VARIABLE, UNUSED_PARAMETER, DEAD_CODE ->
                "Code clutter, confusion about intent, maintenance overhead.";
            default ->
                "Reduced code quality, increased maintenance cost, technical debt accumulation.";
        };

        return severityImpact + typeImpact;
    }

    /**
     * Generates actionable recommendations for fixing a code smell.
     */
    private String generateRecommendation(CodeSmell smell) {
        return switch (smell.getType()) {
            case LONG_METHOD ->
                "Extract logical sections into separate methods with descriptive names. " +
                "Each method should do one thing well.";
            case LARGE_CLASS ->
                "Identify cohesive groups of methods and extract them into separate classes. " +
                "Consider applying Single Responsibility Principle.";
            case HIGH_CYCLOMATIC_COMPLEXITY ->
                "Reduce complexity by extracting conditions into named methods, " +
                "using early returns, or applying strategy pattern.";
            case DUPLICATED_CODE ->
                "Extract common logic into a shared method or class. " +
                "Use inheritance or composition if the duplicates have variations.";
            case DEEPLY_NESTED_CODE ->
                "Use early returns/continues to reduce nesting. " +
                "Extract nested blocks into separate methods.";
            case LONG_PARAMETER_LIST ->
                "Introduce a parameter object to group related parameters. " +
                "Consider if a method needs all these parameters.";
            case PRIMITIVE_OBSESSION ->
                "Create value objects to represent domain concepts. " +
                "Encapsulate validation and behavior within these objects.";
            case INAPPROPRIATE_INTIMACY ->
                "Introduce interfaces to depend on abstractions. " +
                "Use dependency injection to manage dependencies.";
            case MISSING_TRY_WITH_RESOURCES ->
                "Use try-with-resources for AutoCloseable objects. " +
                "Ensure cleanup in finally blocks if try-with-resources not applicable.";
            case STRING_CONCAT_IN_LOOP ->
                "Replace string concatenation with StringBuilder or String.join(). " +
                "This significantly improves performance.";
            case INEFFICIENT_COLLECTION ->
                "Review collection usage and choose appropriate data structures. " +
                "Consider time complexity of operations.";
            case MAGIC_NUMBER, MAGIC_STRING ->
                "Extract literals into named constants with descriptive names. " +
                "Document the meaning and rationale.";
            case DEAD_CODE, UNUSED_IMPORT, UNUSED_VARIABLE, UNUSED_PARAMETER ->
                "Remove unused code. Version control preserves history if needed later.";
            case EMPTY_CATCH_BLOCK ->
                "Add proper exception handling: log the error, rethrow, or recover gracefully. " +
                "Never silently swallow exceptions.";
            case FIELD_INJECTION ->
                "Use constructor injection instead of field injection. " +
                "This makes dependencies explicit and improves testability.";
            case DATA_CLASS ->
                "Add behavior to the class that operates on its data. " +
                "Move related logic from other classes into this class.";
            case FEATURE_ENVY ->
                "Move this method to the class it's most interested in. " +
                "This improves cohesion and reduces coupling.";
            case DATA_CLUMPS ->
                "Create a new class to group these related parameters. " +
                "This reduces parameter lists and improves maintainability.";
            case LAZY_CLASS ->
                "Merge this class with another related class or remove it entirely. " +
                "Only create classes that provide significant value.";
            case SPECULATIVE_GENERALITY ->
                "Simplify the design by removing unnecessary abstraction. " +
                "Follow YAGNI (You Aren't Gonna Need It) principle.";
            case SWITCH_STATEMENT ->
                "Replace with polymorphism using inheritance or composition. " +
                "Consider strategy pattern or command pattern.";
            case MESSAGE_CHAIN ->
                "Create helper methods to hide the chain. " +
                "Apply the Law of Demeter: only talk to immediate friends.";
            case MIDDLE_MAN ->
                "Either remove the middleman and access the delegate directly, " +
                "or add more functionality to justify the class's existence.";
            default ->
                "Review and refactor according to SOLID principles and clean code practices.";
        };
    }

    /**
     * Maps code smell type to a high-level category.
     */
    private String getCategoryForType(CodeSmellType type) {
        return switch (type) {
            case LONG_METHOD, HIGH_CYCLOMATIC_COMPLEXITY, DEEPLY_NESTED_CODE -> "Complexity";
            case LARGE_CLASS, GOD_CLASS -> "Bloaters";
            case DUPLICATED_CODE, DATA_CLUMPS -> "Duplication";
            case FEATURE_ENVY, INAPPROPRIATE_INTIMACY -> "Coupling";
            case PRIMITIVE_OBSESSION, LONG_PARAMETER_LIST -> "Abstraction";
            case MAGIC_NUMBER, MAGIC_STRING -> "Clarity";
            case DEAD_CODE, UNUSED_IMPORT, UNUSED_VARIABLE, UNUSED_PARAMETER, LAZY_CLASS -> "Technical Debt";
            case MISSING_TRY_WITH_RESOURCES, EMPTY_CATCH_BLOCK -> "Reliability";
            case STRING_CONCAT_IN_LOOP, INEFFICIENT_COLLECTION -> "Performance";
            case DATA_CLASS, FIELD_INJECTION -> "Design";
            case SPECULATIVE_GENERALITY, MIDDLE_MAN -> "Abstraction";
            case SWITCH_STATEMENT, MESSAGE_CHAIN -> "Object-Orientation";
        };
    }

    /**
     * Extracts a numeric value from the message text.
     * Falls back to provided default if no number is found.
     */
    private String extractNumber(String message, String defaultValue) {
        if (message == null) {
            return defaultValue;
        }

        // Try to find numbers in the message (e.g., "Method has 45 lines")
        Pattern pattern = Pattern.compile("\\b(\\d+)\\b");
        Matcher matcher = pattern.matcher(message);
        if (matcher.find()) {
            return matcher.group(1);
        }

        return defaultValue;
    }
}

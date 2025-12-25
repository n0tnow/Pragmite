package com.pragmite.ai;

import com.pragmite.model.CodeSmell;
import com.pragmite.model.CodeSmellType;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Generates AI prompts for code smell refactoring using template-based approach.
 * Supports Claude, GPT-4, and Gemini with optimized English prompts.
 *
 * @since 1.4.0
 */
public class PromptGenerator {

    private final Map<CodeSmellType, String> templates;

    public PromptGenerator() {
        this.templates = initializeTemplates();
    }

    /**
     * Generates a ready-to-use AI prompt for refactoring a code smell.
     *
     * @param smell The code smell to address
     * @param sourceCode Complete source code
     * @param snippets Relevant code snippets
     * @return Formatted AI prompt
     */
    public String generatePrompt(CodeSmell smell, String sourceCode, List<String> snippets) {
        String template = templates.getOrDefault(smell.getType(), getDefaultTemplate());

        // Replace template variables
        Map<String, String> variables = buildVariables(smell, snippets);
        String prompt = replaceVariables(template, variables);

        return prompt;
    }

    /**
     * Builds variable map for template substitution.
     */
    private Map<String, String> buildVariables(CodeSmell smell, List<String> snippets) {
        Map<String, String> vars = new HashMap<>();

        // Basic information
        vars.put("file", smell.getFilePath());
        vars.put("line", String.valueOf(smell.getLine()));
        vars.put("type", smell.getType().toString());
        vars.put("severity", smell.getSeverity().toString());
        vars.put("message", smell.getMessage() != null ? smell.getMessage() : "Code smell detected");

        // Code snippet
        String codeSnippet = snippets.isEmpty() ? "No code snippet available" : snippets.get(0);
        vars.put("code_snippet", codeSnippet);

        // Extract metrics from message text
        String message = smell.getMessage() != null ? smell.getMessage() : "";
        vars.put("line_count", extractNumber(message, "lineCount|lines", "N/A"));
        vars.put("complexity", extractNumber(message, "complexity|cyclomatic", "N/A"));
        vars.put("param_count", extractNumber(message, "parameter|param", "N/A"));
        vars.put("nesting_depth", extractNumber(message, "depth|nesting", "N/A"));
        vars.put("method_count", extractNumber(message, "method", "N/A"));

        // Thresholds (using sensible defaults)
        vars.put("threshold", "20");
        vars.put("complexity_threshold", "10");
        vars.put("param_threshold", "5");
        vars.put("nesting_threshold", "3");

        return vars;
    }

    /**
     * Extracts a number from message text based on keywords.
     */
    private String extractNumber(String message, String keywords, String defaultValue) {
        if (message == null || message.isEmpty()) {
            return defaultValue;
        }

        // Build regex pattern: look for keywords followed by number
        // e.g., "Method has 45 lines" or "complexity: 12"
        String[] keywordList = keywords.split("\\|");
        for (String keyword : keywordList) {
            Pattern pattern = Pattern.compile(keyword + "[\\s:]+?(\\d+)", Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(message);
            if (matcher.find()) {
                return matcher.group(1);
            }

            // Also try the reverse: number before keyword
            // e.g., "45 lines"
            Pattern reversePattern = Pattern.compile("(\\d+)[\\s]+?" + keyword, Pattern.CASE_INSENSITIVE);
            Matcher reverseMatcher = reversePattern.matcher(message);
            if (reverseMatcher.find()) {
                return reverseMatcher.group(1);
            }
        }

        // If no keyword match, try to find any number in the message
        Pattern anyNumber = Pattern.compile("\\b(\\d+)\\b");
        Matcher matcher = anyNumber.matcher(message);
        if (matcher.find()) {
            return matcher.group(1);
        }

        return defaultValue;
    }

    /**
     * Replaces template variables with actual values.
     */
    private String replaceVariables(String template, Map<String, String> variables) {
        String result = template;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            String placeholder = "{" + entry.getKey() + "}";
            result = result.replace(placeholder, entry.getValue());
        }
        return result;
    }

    /**
     * Initializes all prompt templates.
     */
    private Map<CodeSmellType, String> initializeTemplates() {
        Map<CodeSmellType, String> t = new HashMap<>();

        t.put(CodeSmellType.LONG_METHOD, """
            Refactor the following Java method to improve maintainability and readability.

            Current Issues:
            - Method length: {line_count} lines (recommended max: {threshold})
            - The method likely violates Single Responsibility Principle

            Refactoring Goals:
            1. Break into smaller, focused methods (max {threshold} lines each)
            2. Each extracted method should have a clear, single purpose
            3. Use descriptive method names that explain what, not how
            4. Maintain the original functionality exactly

            Code to Refactor:
            ```java
            {code_snippet}
            ```

            Please provide:
            1. Refactored code with extracted methods
            2. Brief explanation of the refactoring approach
            3. Benefits of the new structure
            """);

        t.put(CodeSmellType.HIGH_CYCLOMATIC_COMPLEXITY, """
            Reduce the complexity of this Java method to improve testability and maintainability.

            Current Issues:
            - Cyclomatic complexity: {complexity} (recommended max: {complexity_threshold})
            - Too many decision points (if/else, switch, loops)

            Refactoring Goals:
            1. Reduce complexity to below {complexity_threshold}
            2. Extract complex conditions into well-named methods
            3. Use early returns to reduce nesting
            4. Consider applying Strategy or State pattern if applicable

            Code to Refactor:
            ```java
            {code_snippet}
            ```

            Please provide:
            1. Refactored code with reduced complexity
            2. Explanation of complexity reduction techniques used
            3. How this improves testability
            """);

        t.put(CodeSmellType.LARGE_CLASS, """
            Refactor this large Java class to improve cohesion and maintainability.

            Current Issues:
            - Class has {method_count} methods
            - Likely violates Single Responsibility Principle
            - Multiple responsibilities mixed together

            Refactoring Goals:
            1. Identify cohesive groups of methods and data
            2. Extract separate classes for each responsibility
            3. Improve naming to reflect single responsibilities
            4. Maintain clear relationships between extracted classes

            Code to Refactor:
            ```java
            {code_snippet}
            ```

            Please provide:
            1. Proposed class structure (names and responsibilities)
            2. Sample implementation of at least one extracted class
            3. How classes should interact
            """);

        t.put(CodeSmellType.LONG_PARAMETER_LIST, """
            Refactor this method to reduce its parameter list and improve usability.

            Current Issues:
            - Method has {param_count} parameters (recommended max: {param_threshold})
            - Complex method signature is error-prone
            - May indicate missing abstraction

            Refactoring Goals:
            1. Reduce parameters to {param_threshold} or fewer
            2. Introduce parameter objects for related parameters
            3. Consider if method needs all parameters or if some can be instance variables
            4. Improve method signature readability

            Code to Refactor:
            ```java
            {code_snippet}
            ```

            Please provide:
            1. Refactored method signature
            2. Parameter object class(es) if applicable
            3. Updated method implementation
            """);

        t.put(CodeSmellType.DEEPLY_NESTED_CODE, """
            Reduce the nesting depth in this Java method to improve readability.

            Current Issues:
            - Nesting depth: {nesting_depth} levels (recommended max: {nesting_threshold})
            - Difficult to follow control flow
            - High cognitive load

            Refactoring Goals:
            1. Reduce nesting to {nesting_threshold} levels or fewer
            2. Use early returns/continues to flatten structure
            3. Extract nested blocks into separate methods
            4. Improve overall readability

            Code to Refactor:
            ```java
            {code_snippet}
            ```

            Please provide:
            1. Refactored code with reduced nesting
            2. Explanation of techniques used (early returns, extraction, etc.)
            3. How this improves readability
            """);

        t.put(CodeSmellType.DUPLICATED_CODE, """
            Eliminate code duplication by extracting common logic.

            Current Issues:
            - Similar code blocks detected in multiple locations
            - Violates DRY (Don't Repeat Yourself) principle
            - Changes must be made in multiple places

            Refactoring Goals:
            1. Extract common logic into a shared method or class
            2. Parameterize differences between duplicates
            3. Ensure single source of truth
            4. Maintain or improve readability

            Duplicated Code:
            ```java
            {code_snippet}
            ```

            Please provide:
            1. Extracted common method/class
            2. How to call it from original locations
            3. How differences are handled via parameters
            """);

        t.put(CodeSmellType.PRIMITIVE_OBSESSION, """
            Replace primitive types with domain objects to improve type safety and clarity.

            Current Issues:
            - Using primitives instead of domain objects
            - Loss of type safety
            - Validation logic scattered across codebase

            Refactoring Goals:
            1. Create value objects to represent domain concepts
            2. Encapsulate validation within value objects
            3. Improve type safety and self-documentation
            4. Enable compile-time checking

            Code with Primitive Obsession:
            ```java
            {code_snippet}
            ```

            Please provide:
            1. Value object class(es) with validation
            2. Refactored code using value objects
            3. Benefits of this approach
            """);

        t.put(CodeSmellType.DATA_CLUMPS, """
            Extract data clumps into cohesive objects.

            Current Issues:
            - Same group of variables appears together in multiple places
            - Missing abstraction for a cohesive concept
            - Changes require updating multiple locations

            Refactoring Goals:
            1. Identify the cohesive concept behind the data group
            2. Create a class to represent this concept
            3. Replace data clumps with the new class
            4. Move related behavior into the new class

            Code with Data Clumps:
            ```java
            {code_snippet}
            ```

            Please provide:
            1. New class representing the concept
            2. Refactored code using the new class
            3. Any related behavior to move into the class
            """);

        t.put(CodeSmellType.FEATURE_ENVY, """
            Move method to the class it's most interested in to improve cohesion.

            Current Issues:
            - Method uses data/methods from another class more than its own
            - Misplaced responsibility
            - Poor cohesion

            Refactoring Goals:
            1. Move method to the envied class
            2. Adjust access modifiers as needed
            3. Improve cohesion in both classes
            4. Update callers to use new location

            Envious Code:
            ```java
            {code_snippet}
            ```

            Please provide:
            1. Where the method should move to
            2. Refactored method in new location
            3. How to update callers
            """);

        t.put(CodeSmellType.GOD_CLASS, """
            Break down this God Class into focused, cohesive classes.

            Current Issues:
            - Class knows too much and does too much
            - Centralizes system knowledge
            - Violates Single Responsibility Principle

            Refactoring Goals:
            1. Identify distinct responsibilities
            2. Extract each responsibility into a separate class
            3. Define clear interfaces between classes
            4. Improve testability and maintainability

            God Class Code:
            ```java
            {code_snippet}
            ```

            Please provide:
            1. Proposed class breakdown with responsibilities
            2. Sample implementation of 2-3 extracted classes
            3. How classes collaborate
            """);

        t.put(CodeSmellType.MAGIC_NUMBER, """
            Replace magic numbers with named constants to improve code clarity.

            Current Issues:
            - Unexplained numeric literals reduce readability
            - Number's meaning is lost without context
            - Changes require finding all occurrences

            Refactoring Goals:
            1. Extract numbers into named constants
            2. Use descriptive names that explain the meaning
            3. Add documentation if needed
            4. Group related constants

            Code with Magic Numbers:
            ```java
            {code_snippet}
            ```

            Please provide:
            1. Named constants with descriptive names
            2. Refactored code using constants
            3. Brief explanation of what each number represents
            """);

        t.put(CodeSmellType.MAGIC_STRING, """
            Replace magic strings with named constants to improve code clarity.

            Current Issues:
            - Unexplained string literals reduce readability
            - String's purpose is unclear without context
            - Changes require finding all occurrences

            Refactoring Goals:
            1. Extract strings into named constants
            2. Use descriptive names that explain the meaning
            3. Consider using enums for related string constants
            4. Group related constants

            Code with Magic Strings:
            ```java
            {code_snippet}
            ```

            Please provide:
            1. Named constants or enums with descriptive names
            2. Refactored code using constants
            3. Brief explanation of what each string represents
            """);

        t.put(CodeSmellType.DEAD_CODE, """
            Remove unused code to reduce maintenance burden.

            Current Issues:
            - Code is not called/used anywhere
            - Increases codebase size unnecessarily
            - May confuse developers
            - Could hide bugs or security issues

            Action Required:
            Simply delete the unused code. Version control preserves history if needed later.

            Dead Code:
            ```java
            {code_snippet}
            ```

            Confirmation needed:
            1. Verify code is truly unused (search for references)
            2. Check if it's part of a public API
            3. Remove if confirmed unused
            """);

        t.put(CodeSmellType.UNUSED_IMPORT, """
            Remove unused import to clean up the code.

            Current Issue:
            - Import statement is not used anywhere in the file
            - Clutters the import section
            - May confuse developers

            Action Required:
            Delete the unused import statement.

            Code:
            ```java
            {code_snippet}
            ```

            Please confirm and remove the unused import.
            """);

        t.put(CodeSmellType.UNUSED_VARIABLE, """
            Remove unused variable to clean up the code.

            Current Issue:
            - Variable is declared but never used
            - Wastes memory
            - May confuse developers about intent

            Action Required:
            Delete the unused variable declaration.

            Code:
            ```java
            {code_snippet}
            ```

            Please confirm and remove the unused variable.
            """);

        t.put(CodeSmellType.UNUSED_PARAMETER, """
            Remove or use the unused parameter.

            Current Issue:
            - Parameter is declared but never used in the method
            - May indicate incomplete implementation
            - Clutters method signature

            Refactoring Options:
            1. Remove the parameter if truly not needed
            2. Implement the missing logic that uses the parameter
            3. If overriding a method, consider if it can be used

            Code:
            ```java
            {code_snippet}
            ```

            Please provide:
            1. Whether to remove or use the parameter
            2. Refactored code
            """);

        t.put(CodeSmellType.EMPTY_CATCH_BLOCK, """
            Add proper exception handling to avoid silent failures.

            Current Issues:
            - Empty catch block hides errors
            - May cause silent failures
            - Difficult to debug when things go wrong

            Refactoring Goals:
            1. Log the exception with appropriate level
            2. Rethrow if you can't handle it properly
            3. Recover gracefully if possible
            4. Never silently swallow exceptions

            Code with Empty Catch:
            ```java
            {code_snippet}
            ```

            Please provide:
            1. Proper exception handling
            2. Explanation of the approach
            3. Why this is better than empty catch
            """);

        t.put(CodeSmellType.MISSING_TRY_WITH_RESOURCES, """
            Fix resource management to prevent leaks using try-with-resources.

            Current Issues:
            - Resources not managed with try-with-resources
            - Can lead to resource leaks, file descriptor exhaustion
            - May cause application failures over time

            Refactoring Goals:
            1. Use try-with-resources for AutoCloseable objects
            2. Ensure cleanup happens automatically
            3. Handle exceptions during cleanup properly
            4. Simplify the code

            Code with Manual Resource Management:
            ```java
            {code_snippet}
            ```

            Please provide:
            1. Refactored code using try-with-resources
            2. Explanation of the improvement
            3. Why this prevents leaks
            """);

        t.put(CodeSmellType.STRING_CONCAT_IN_LOOP, """
            Replace string concatenation in loop with StringBuilder for better performance.

            Current Issues:
            - String concatenation creates multiple temporary objects
            - Causes performance degradation with many iterations
            - Wastes memory with intermediate String objects

            Refactoring Goals:
            1. Use StringBuilder instead of string concatenation
            2. Significantly improve performance
            3. Reduce memory allocations
            4. Maintain the same functionality

            Code with String Concatenation:
            ```java
            {code_snippet}
            ```

            Please provide:
            1. Refactored code using StringBuilder
            2. Performance benefit explanation
            3. When to use this pattern
            """);

        t.put(CodeSmellType.INEFFICIENT_COLLECTION, """
            Improve collection usage for better performance.

            Current Issues:
            - Inefficient collection usage
            - Wrong data structure for the use case
            - Performance impact on large data sets

            Refactoring Goals:
            1. Choose appropriate data structure (ArrayList, HashSet, HashMap, etc.)
            2. Consider time complexity of operations
            3. Improve performance
            4. Maintain correct functionality

            Code with Inefficient Collection:
            ```java
            {code_snippet}
            ```

            Please provide:
            1. Better collection choice with explanation
            2. Refactored code
            3. Performance improvement rationale
            """);

        t.put(CodeSmellType.FIELD_INJECTION, """
            Replace field injection with constructor injection for better testability.

            Current Issues:
            - Field injection hides dependencies
            - Makes testing difficult
            - Can't create immutable objects
            - Dependencies can be null

            Refactoring Goals:
            1. Use constructor injection instead
            2. Make dependencies explicit
            3. Improve testability
            4. Enable immutability

            Code with Field Injection:
            ```java
            {code_snippet}
            ```

            Please provide:
            1. Refactored code with constructor injection
            2. How this improves testability
            3. Benefits of constructor injection
            """);

        t.put(CodeSmellType.DATA_CLASS, """
            Add behavior to this data class to improve object-oriented design.

            Current Issues:
            - Class only holds data without behavior
            - Violates object-oriented design principles
            - Related logic scattered in other classes

            Refactoring Goals:
            1. Add methods that operate on the data
            2. Move related logic from other classes
            3. Create a rich domain model
            4. Improve encapsulation

            Data Class:
            ```java
            {code_snippet}
            ```

            Please provide:
            1. Suggested methods to add
            2. Refactored class with behavior
            3. How this improves the design
            """);

        t.put(CodeSmellType.INAPPROPRIATE_INTIMACY, """
            Reduce coupling between classes to improve maintainability.

            Current Issues:
            - Excessive coupling between classes
            - Classes know too much about each other's internals
            - Difficult to test in isolation
            - Changes have wide impact

            Refactoring Goals:
            1. Introduce interfaces to reduce coupling
            2. Use dependency injection
            3. Improve encapsulation
            4. Define clear boundaries

            Tightly Coupled Code:
            ```java
            {code_snippet}
            ```

            Please provide:
            1. How to reduce the coupling
            2. Refactored code with better boundaries
            3. Benefits of this approach
            """);

        t.put(CodeSmellType.LAZY_CLASS, """
            Merge or remove this lazy class that does too little.

            Current Issues:
            - Class does too little to justify its existence
            - Adds unnecessary complexity
            - Should be merged with another class or removed

            Refactoring Goals:
            1. Merge with a related class
            2. Or remove entirely and inline its functionality
            3. Simplify the design
            4. Reduce maintenance overhead

            Lazy Class:
            ```java
            {code_snippet}
            ```

            Please provide:
            1. Whether to merge or remove
            2. Refactored code
            3. Justification for the decision
            """);

        t.put(CodeSmellType.SPECULATIVE_GENERALITY, """
            Remove unnecessary abstraction to simplify the design.

            Current Issues:
            - Unnecessary abstraction without clear benefit
            - Adds complexity
            - Violates YAGNI (You Aren't Gonna Need It)

            Refactoring Goals:
            1. Simplify the design
            2. Remove unused abstraction
            3. Follow YAGNI principle
            4. Keep code simple until abstraction is needed

            Over-Abstracted Code:
            ```java
            {code_snippet}
            ```

            Please provide:
            1. Simplified version without excess abstraction
            2. What was removed and why
            3. When abstraction would be warranted
            """);

        t.put(CodeSmellType.SWITCH_STATEMENT, """
            Replace switch statement with polymorphism for better maintainability.

            Current Issues:
            - Large switch statement is hard to maintain
            - Violates Open/Closed Principle
            - Adding new cases requires modifying existing code

            Refactoring Goals:
            1. Use polymorphism (inheritance or composition)
            2. Apply strategy or command pattern
            3. Make code extensible without modification
            4. Improve maintainability

            Switch Statement:
            ```java
            {code_snippet}
            ```

            Please provide:
            1. Polymorphic design to replace switch
            2. Class structure and interfaces
            3. How this follows Open/Closed Principle
            """);

        t.put(CodeSmellType.MESSAGE_CHAIN, """
            Reduce message chain to follow Law of Demeter.

            Current Issues:
            - Long chain of method calls (a.b().c().d())
            - Violates Law of Demeter
            - Creates tight coupling
            - Brittle code that breaks easily

            Refactoring Goals:
            1. Create helper methods to hide the chain
            2. Follow Law of Demeter: only talk to immediate friends
            3. Reduce coupling
            4. Make code more robust

            Message Chain:
            ```java
            {code_snippet}
            ```

            Please provide:
            1. Refactored code with helper methods
            2. How this reduces coupling
            3. Benefits for maintainability
            """);

        t.put(CodeSmellType.MIDDLE_MAN, """
            Remove or enhance this middle man class.

            Current Issues:
            - Class delegates most work to another class
            - Adds unnecessary indirection
            - Should either be removed or add more value

            Refactoring Goals:
            1. Either remove the middleman and access delegate directly
            2. Or add more functionality to justify existence
            3. Simplify the design
            4. Reduce unnecessary indirection

            Middle Man Code:
            ```java
            {code_snippet}
            ```

            Please provide:
            1. Whether to remove or enhance
            2. Refactored code
            3. Justification for the approach
            """);

        return t;
    }

    /**
     * Returns a default template for types without specific templates.
     */
    private String getDefaultTemplate() {
        return """
            Refactor the following Java code to improve quality and maintainability.

            Issue Type: {type}
            Severity: {severity}
            Description: {message}

            Code to Refactor:
            ```java
            {code_snippet}
            ```

            Please provide:
            1. Refactored code following best practices
            2. Explanation of improvements made
            3. Benefits of the refactoring
            """;
    }
}

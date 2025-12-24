package com.pragmite.naming;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.MethodCallExpr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Smart Naming Engine that suggests meaningful names for variables and methods.
 *
 * Features:
 * - Context-aware name generation
 * - Learns from existing naming patterns in codebase
 * - Considers type information
 * - Generates multiple alternatives
 * - Scores suggestions by relevance
 */
public class SmartNamingEngine {
    private static final Logger logger = LoggerFactory.getLogger(SmartNamingEngine.class);

    private static final Map<String, List<String>> TYPE_TO_COMMON_NAMES = Map.of(
        "String", List.of("name", "text", "value", "message", "description", "content"),
        "int", List.of("count", "size", "index", "length", "total", "number"),
        "long", List.of("timestamp", "id", "duration", "amount"),
        "double", List.of("rate", "ratio", "percentage", "value", "amount"),
        "boolean", List.of("isValid", "hasValue", "canProcess", "shouldContinue", "enabled"),
        "List", List.of("items", "elements", "collection", "entries", "records"),
        "Map", List.of("mapping", "dictionary", "lookup", "index", "cache")
    );

    private static final List<String> VERB_PREFIXES = List.of(
        "get", "set", "is", "has", "can", "should", "will",
        "create", "build", "generate", "calculate", "compute",
        "validate", "verify", "check", "ensure",
        "process", "handle", "execute", "perform",
        "load", "save", "read", "write", "parse", "format",
        "add", "remove", "delete", "update", "modify",
        "find", "search", "filter", "sort", "group"
    );

    /**
     * Suggests better names for a variable based on context.
     */
    public List<NamingSuggestion> suggestVariableName(String currentName, String type,
                                                       String context, CompilationUnit cu) {
        List<NamingSuggestion> suggestions = new ArrayList<>();

        // Analyze why current name is poor
        NamingIssue issue = analyzeNaming(currentName, type, false);

        if (issue.isGood()) {
            return Collections.emptyList(); // Name is already good
        }

        // Generate suggestions based on type
        if (TYPE_TO_COMMON_NAMES.containsKey(type)) {
            for (String commonName : TYPE_TO_COMMON_NAMES.get(type)) {
                suggestions.add(new NamingSuggestion(
                    commonName,
                    1.0,
                    "Common name for " + type + " type"
                ));
            }
        }

        // Analyze context for domain-specific suggestions
        Set<String> contextWords = extractContextWords(context);
        for (String word : contextWords) {
            if (word.length() > 3 && !isStopWord(word)) {
                String suggestion = word.toLowerCase();
                suggestions.add(new NamingSuggestion(
                    suggestion,
                    0.9,
                    "Derived from context: '" + context + "'"
                ));
            }
        }

        // Learn from existing codebase patterns
        if (cu != null) {
            Map<String, Integer> existingPatterns = learnNamingPatterns(cu, type);
            for (Map.Entry<String, Integer> entry : existingPatterns.entrySet()) {
                suggestions.add(new NamingSuggestion(
                    entry.getKey(),
                    0.8 * (entry.getValue() / 10.0),
                    "Pattern found in codebase (" + entry.getValue() + " occurrences)"
                ));
            }
        }

        // If type suggests collection, pluralize
        if (type.contains("List") || type.contains("Set") || type.contains("Collection")) {
            suggestions.add(new NamingSuggestion(
                "items",
                0.7,
                "Generic plural name for collection"
            ));
        }

        // Remove duplicates and sort by score
        return suggestions.stream()
            .distinct()
            .sorted(Comparator.comparingDouble(NamingSuggestion::getScore).reversed())
            .limit(5)
            .collect(Collectors.toList());
    }

    /**
     * Suggests better names for a method based on its body and context.
     */
    public List<NamingSuggestion> suggestMethodName(MethodDeclaration method) {
        List<NamingSuggestion> suggestions = new ArrayList<>();
        String currentName = method.getNameAsString();

        // Analyze what the method does
        MethodPurpose purpose = analyzeMethodPurpose(method);

        // Generate name based on purpose
        List<String> verbs = selectAppropriateVerbs(purpose);

        for (String verb : verbs) {
            String noun = purpose.getPrimaryNoun();
            if (noun != null) {
                String suggestion = verb + capitalize(noun);
                suggestions.add(new NamingSuggestion(
                    suggestion,
                    0.95,
                    "Based on method behavior: " + purpose.getDescription()
                ));
            }
        }

        // If method returns boolean, suggest is/has/can prefix
        if (method.getType().asString().equals("boolean")) {
            String baseName = removeVerbPrefix(currentName);
            suggestions.add(new NamingSuggestion(
                "is" + capitalize(baseName),
                0.9,
                "Boolean method should start with is/has/can"
            ));
        }

        return suggestions.stream()
            .limit(5)
            .collect(Collectors.toList());
    }

    private NamingIssue analyzeNaming(String name, String type, boolean isMethod) {
        NamingIssue issue = new NamingIssue(name);

        // Check for single character (except i, j, k in loops)
        if (name.length() == 1 && !List.of("i", "j", "k").contains(name)) {
            issue.addProblem("Single character name - not descriptive");
            issue.setGood(false);
        }

        // Check for Hungarian notation
        if (Pattern.matches("^(str|int|bool|obj|arr|lst)[A-Z].*", name)) {
            issue.addProblem("Uses Hungarian notation - avoid type prefixes");
            issue.setGood(false);
        }

        // Check for non-descriptive names
        if (Pattern.matches("^(temp|tmp|val|var|data|info)\\d*$", name)) {
            issue.addProblem("Non-descriptive generic name");
            issue.setGood(false);
        }

        // Check length
        if (name.length() > 30) {
            issue.addProblem("Name is too long");
            issue.setGood(false);
        }

        // Check for camelCase
        if (!Pattern.matches("^[a-z][a-zA-Z0-9]*$", name) && !isMethod) {
            issue.addProblem("Does not follow camelCase convention");
            issue.setGood(false);
        }

        return issue;
    }

    private MethodPurpose analyzeMethodPurpose(MethodDeclaration method) {
        MethodPurpose purpose = new MethodPurpose();

        if (!method.getBody().isPresent()) {
            return purpose;
        }

        String bodyStr = method.getBody().get().toString().toLowerCase();

        // Detect primary action
        if (bodyStr.contains("return") && !bodyStr.contains("void")) {
            purpose.setAction("get");
        }
        if (bodyStr.contains("validate") || bodyStr.contains("check")) {
            purpose.setAction("validate");
        }
        if (bodyStr.contains("calculate") || bodyStr.contains("compute")) {
            purpose.setAction("calculate");
        }
        if (bodyStr.contains("format") || bodyStr.contains("tostring")) {
            purpose.setAction("format");
        }
        if (bodyStr.contains("process") || bodyStr.contains("handle")) {
            purpose.setAction("process");
        }

        // Find method calls to infer purpose
        List<String> methodCalls = method.findAll(MethodCallExpr.class).stream()
            .map(call -> call.getNameAsString())
            .collect(Collectors.toList());

        if (!methodCalls.isEmpty()) {
            purpose.setMethodCalls(methodCalls);
        }

        return purpose;
    }

    private List<String> selectAppropriateVerbs(MethodPurpose purpose) {
        String action = purpose.getAction();
        if (action != null) {
            return List.of(action);
        }
        return List.of("process", "handle", "execute");
    }

    private Set<String> extractContextWords(String context) {
        Set<String> words = new HashSet<>();
        if (context == null) return words;

        String[] tokens = context.split("[^a-zA-Z]+");
        for (String token : tokens) {
            if (token.length() > 3) {
                words.add(token);
            }
        }
        return words;
    }

    private Map<String, Integer> learnNamingPatterns(CompilationUnit cu, String type) {
        Map<String, Integer> patterns = new HashMap<>();

        // Find all variables of the same type and count naming patterns
        cu.findAll(VariableDeclarator.class).forEach(var -> {
            if (var.getTypeAsString().equals(type)) {
                String name = var.getNameAsString();
                patterns.put(name, patterns.getOrDefault(name, 0) + 1);
            }
        });

        return patterns;
    }

    private boolean isStopWord(String word) {
        Set<String> stopWords = Set.of("the", "and", "for", "with", "this", "that", "from");
        return stopWords.contains(word.toLowerCase());
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    private String removeVerbPrefix(String methodName) {
        for (String verb : VERB_PREFIXES) {
            if (methodName.startsWith(verb) && methodName.length() > verb.length()) {
                return methodName.substring(verb.length());
            }
        }
        return methodName;
    }

    public static class NamingSuggestion {
        private final String suggestedName;
        private final double score;
        private final String reason;

        public NamingSuggestion(String suggestedName, double score, String reason) {
            this.suggestedName = suggestedName;
            this.score = Math.min(1.0, score);
            this.reason = reason;
        }

        public String getSuggestedName() { return suggestedName; }
        public double getScore() { return score; }
        public String getReason() { return reason; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof NamingSuggestion)) return false;
            NamingSuggestion that = (NamingSuggestion) o;
            return suggestedName.equals(that.suggestedName);
        }

        @Override
        public int hashCode() {
            return suggestedName.hashCode();
        }

        @Override
        public String toString() {
            return String.format("%s (%.0f%%) - %s", suggestedName, score * 100, reason);
        }
    }

    private static class NamingIssue {
        private final String name;
        private final List<String> problems = new ArrayList<>();
        private boolean isGood = true;

        public NamingIssue(String name) {
            this.name = name;
        }

        public void addProblem(String problem) {
            problems.add(problem);
        }

        public boolean isGood() { return isGood; }
        public void setGood(boolean good) { isGood = good; }
        public List<String> getProblems() { return problems; }
    }

    private static class MethodPurpose {
        private String action;
        private String primaryNoun;
        private String description;
        private List<String> methodCalls = new ArrayList<>();

        public String getAction() { return action; }
        public void setAction(String action) { this.action = action; }
        public String getPrimaryNoun() { return primaryNoun; }
        public void setPrimaryNoun(String noun) { this.primaryNoun = noun; }
        public String getDescription() { return description; }
        public void setDescription(String desc) { this.description = desc; }
        public List<String> getMethodCalls() { return methodCalls; }
        public void setMethodCalls(List<String> calls) { this.methodCalls = calls; }
    }
}

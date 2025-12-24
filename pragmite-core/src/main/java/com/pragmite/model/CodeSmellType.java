package com.pragmite.model;

/**
 * Supported code smell types.
 */
public enum CodeSmellType {
    // Method-level smells
    LONG_METHOD("Long Method", "Method is too long", Severity.MAJOR, PragmaticPrinciple.READABILITY),
    LONG_PARAMETER_LIST("Long Parameter List", "Parameter list is too long", Severity.MINOR, PragmaticPrinciple.ORTHOGONALITY),

    // Class-level smells
    GOD_CLASS("God Class", "Class has too many responsibilities", Severity.CRITICAL, PragmaticPrinciple.ORTHOGONALITY),
    LARGE_CLASS("Large Class", "Class is too large", Severity.MAJOR, PragmaticPrinciple.DRY),
    DATA_CLASS("Data Class", "Class only holds data without behavior", Severity.MINOR, PragmaticPrinciple.ORTHOGONALITY),

    // Code duplication
    DUPLICATED_CODE("Duplicated Code", "Repeated code block", Severity.MAJOR, PragmaticPrinciple.DRY),

    // Magic values
    MAGIC_NUMBER("Magic Number", "Unexplained numeric literal", Severity.MINOR, PragmaticPrinciple.CORRECTNESS),
    MAGIC_STRING("Magic String", "Unexplained string literal", Severity.MINOR, PragmaticPrinciple.CORRECTNESS),

    // Unused code
    DEAD_CODE("Dead Code", "Unreachable or unused code", Severity.MINOR, PragmaticPrinciple.MAINTAINABILITY),
    UNUSED_IMPORT("Unused Import", "Unused import statement", Severity.INFO, PragmaticPrinciple.MAINTAINABILITY),
    UNUSED_VARIABLE("Unused Variable", "Unused local variable", Severity.MINOR, PragmaticPrinciple.MAINTAINABILITY),
    UNUSED_PARAMETER("Unused Parameter", "Unused method parameter", Severity.MINOR, PragmaticPrinciple.MAINTAINABILITY),

    // Performance
    STRING_CONCAT_IN_LOOP("String Concatenation in Loop", "String concatenation inside loop", Severity.MAJOR, PragmaticPrinciple.PERFORMANCE),
    INEFFICIENT_COLLECTION("Inefficient Collection Usage", "Inefficient collection usage", Severity.MAJOR, PragmaticPrinciple.PERFORMANCE),

    // Resource management
    MISSING_TRY_WITH_RESOURCES("Missing Try-With-Resources", "AutoCloseable should use try-with-resources", Severity.MAJOR, PragmaticPrinciple.CORRECTNESS),

    // Complexity
    HIGH_CYCLOMATIC_COMPLEXITY("High Cyclomatic Complexity", "High cyclomatic complexity", Severity.MAJOR, PragmaticPrinciple.ORTHOGONALITY),
    DEEPLY_NESTED_CODE("Deeply Nested Code", "Deeply nested code blocks", Severity.MAJOR, PragmaticPrinciple.ORTHOGONALITY),

    // Other
    EMPTY_CATCH_BLOCK("Empty Catch Block", "Empty catch block", Severity.MAJOR, PragmaticPrinciple.CORRECTNESS),
    FIELD_INJECTION("Field Injection", "Field injection anti-pattern", Severity.MINOR, PragmaticPrinciple.MAINTAINABILITY),

    // Advanced detectors (Phase 2)
    FEATURE_ENVY("Feature Envy", "Method uses data from another class excessively", Severity.MINOR, PragmaticPrinciple.ORTHOGONALITY),
    DATA_CLUMPS("Data Clumps", "Same group of parameters appear together repeatedly", Severity.MINOR, PragmaticPrinciple.ORTHOGONALITY),
    INAPPROPRIATE_INTIMACY("Inappropriate Intimacy", "Excessive coupling between classes", Severity.MAJOR, PragmaticPrinciple.ORTHOGONALITY),
    LAZY_CLASS("Lazy Class", "Class does too little to justify its existence", Severity.MINOR, PragmaticPrinciple.MAINTAINABILITY),
    SPECULATIVE_GENERALITY("Speculative Generality", "Unnecessary abstraction", Severity.MINOR, PragmaticPrinciple.ORTHOGONALITY),

    // Production-ready additions (Faz 3)
    PRIMITIVE_OBSESSION("Primitive Obsession", "Overuse of primitives instead of small objects", Severity.MINOR, PragmaticPrinciple.ORTHOGONALITY),
    SWITCH_STATEMENT("Switch Statement", "Large switch that should be replaced with polymorphism", Severity.MINOR, PragmaticPrinciple.ORTHOGONALITY),
    MESSAGE_CHAIN("Message Chain", "Long chain of method calls violates Law of Demeter", Severity.MINOR, PragmaticPrinciple.ORTHOGONALITY),
    MIDDLE_MAN("Middle Man", "Class delegates most of its work to another class", Severity.MINOR, PragmaticPrinciple.MAINTAINABILITY);

    private final String name;
    private final String description;
    private final Severity defaultSeverity;
    private final PragmaticPrinciple pragmaticPrinciple;

    CodeSmellType(String name, String description, Severity defaultSeverity, PragmaticPrinciple pragmaticPrinciple) {
        this.name = name;
        this.description = description;
        this.defaultSeverity = defaultSeverity;
        this.pragmaticPrinciple = pragmaticPrinciple;
    }

    public String getName() { return name; }
    public String getDescription() { return description; }
    public Severity getDefaultSeverity() { return defaultSeverity; }
    public PragmaticPrinciple getPragmaticPrinciple() { return pragmaticPrinciple; }
}

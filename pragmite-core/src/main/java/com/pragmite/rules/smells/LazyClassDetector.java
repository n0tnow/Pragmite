package com.pragmite.rules.smells;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.pragmite.model.CodeSmell;
import com.pragmite.model.CodeSmellType;

import java.util.ArrayList;
import java.util.List;

/**
 * Lazy Class detector.
 * Detects classes that do very little work and don't justify their existence.
 * These classes should be merged with other classes or removed entirely.
 *
 * Criteria for lazy class:
 * - Few methods (< 3 non-trivial methods)
 * - Small total LOC (< 50 lines)
 * - No significant business logic
 */
public class LazyClassDetector implements SmellDetector {

    private static final int MAX_METHODS = 2;
    private static final int MAX_TOTAL_LINES = 80;  // IMPROVED: DTOs/Models can be up to 80 lines
    private static final int MIN_METHOD_LINES = 3; // Methods with < 3 lines are trivial

    @Override
    public List<CodeSmell> detect(CompilationUnit cu, String filePath, String content) {
        List<CodeSmell> smells = new ArrayList<>();

        cu.accept(new VoidVisitorAdapter<Void>() {
            @Override
            public void visit(ClassOrInterfaceDeclaration cid, Void arg) {
                super.visit(cid, arg);

                // Skip interfaces and abstract classes (they're allowed to be small)
                if (cid.isInterface() || cid.isAbstract()) {
                    return;
                }

                // Skip enums and records
                if (cid.isEnumDeclaration() || cid.isRecordDeclaration()) {
                    return;
                }

                // Skip common DTO/Model patterns (Entity, DTO, Model, Request, Response, Config)
                String className = cid.getNameAsString();
                if (className.endsWith("DTO") || className.endsWith("Entity") ||
                    className.endsWith("Model") || className.endsWith("Request") ||
                    className.endsWith("Response") || className.endsWith("Config") ||
                    className.endsWith("Bean") || className.endsWith("Data") ||
                    cid.getAnnotations().stream().anyMatch(a ->
                        a.getNameAsString().contains("Entity") ||
                        a.getNameAsString().contains("Table") ||
                        a.getNameAsString().contains("Document"))) {
                    return;  // DTOs and entities are allowed to be simple
                }

                // Count non-trivial methods
                List<MethodDeclaration> methods = cid.getMethods();
                int nonTrivialMethods = 0;
                int totalLines = 0;

                for (MethodDeclaration md : methods) {
                    // Skip constructors, getters, setters
                    String methodName = md.getNameAsString();
                    if (methodName.equals(cid.getNameAsString()) || isAccessorMethod(md)) {
                        continue;
                    }

                    int methodLines = md.getEnd().map(e -> e.line).orElse(0) -
                                     md.getBegin().map(b -> b.line).orElse(0) + 1;

                    totalLines += methodLines;

                    if (methodLines >= MIN_METHOD_LINES) {
                        nonTrivialMethods++;
                    }
                }

                // Calculate total class lines
                int classLines = cid.getEnd().map(e -> e.line).orElse(0) -
                                cid.getBegin().map(b -> b.line).orElse(0) + 1;

                // Detect lazy class
                if (nonTrivialMethods <= MAX_METHODS && classLines <= MAX_TOTAL_LINES) {
                    int line = cid.getBegin().map(pos -> pos.line).orElse(0);

                    CodeSmell smell = new CodeSmell(
                        CodeSmellType.LAZY_CLASS,
                        filePath,
                        line,
                        String.format("Class '%s' does very little work (%d methods, %d lines)",
                            cid.getNameAsString(), nonTrivialMethods, classLines)
                    );
                    smell.withSuggestion(
                        "Consider merging this class with another class or removing it if it's not needed. " +
                        "Classes should justify their existence with sufficient functionality."
                    ).withAutoFix(false);

                    smells.add(smell);
                }
            }
        }, null);

        return smells;
    }

    /**
     * Checks if a method is a simple accessor (getter/setter).
     */
    private boolean isAccessorMethod(MethodDeclaration md) {
        String name = md.getNameAsString();

        // Getter pattern: getName(), isEnabled(), hasChildren()
        if ((name.startsWith("get") || name.startsWith("is") || name.startsWith("has")) &&
            md.getParameters().isEmpty()) {
            return true;
        }

        // Setter pattern: setName(value)
        if (name.startsWith("set") && md.getParameters().size() == 1) {
            return true;
        }

        return false;
    }
}

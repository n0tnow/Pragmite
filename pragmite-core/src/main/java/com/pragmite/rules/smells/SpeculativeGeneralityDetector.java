package com.pragmite.rules.smells;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.pragmite.model.CodeSmell;
import com.pragmite.model.CodeSmellType;

import java.util.ArrayList;
import java.util.List;

/**
 * Speculative Generality detector.
 * Detects unnecessary abstraction and over-engineering:
 * - Abstract classes with only one concrete implementation
 * - Unused parameters in methods
 * - Interfaces with only one implementing class
 * - Overly generic method signatures that are never used generically
 *
 * Note: Full detection requires cross-file analysis. This implementation focuses on
 * single-file patterns like unused parameters and suspicious abstract classes.
 */
public class SpeculativeGeneralityDetector implements SmellDetector {

    @Override
    public List<CodeSmell> detect(CompilationUnit cu, String filePath, String content) {
        List<CodeSmell> smells = new ArrayList<>();

        cu.accept(new VoidVisitorAdapter<Void>() {
            @Override
            public void visit(ClassOrInterfaceDeclaration cid, Void arg) {
                super.visit(cid, arg);

                // Check for abstract classes with very few methods (likely unnecessary)
                if (cid.isAbstract() && !cid.isInterface()) {
                    int abstractMethods = 0;
                    int concreteMethods = 0;

                    for (MethodDeclaration md : cid.getMethods()) {
                        if (md.isAbstract()) {
                            abstractMethods++;
                        } else {
                            concreteMethods++;
                        }
                    }

                    // Suspicious: abstract class with only 1 abstract method and no concrete methods
                    if (abstractMethods == 1 && concreteMethods == 0) {
                        int line = cid.getBegin().map(pos -> pos.line).orElse(0);

                        CodeSmell smell = new CodeSmell(
                            CodeSmellType.SPECULATIVE_GENERALITY,
                            filePath,
                            line,
                            String.format("Abstract class '%s' has only one abstract method - may be unnecessary abstraction",
                                cid.getNameAsString())
                        );
                        smell.withSuggestion(
                            "Consider using a concrete class or functional interface instead. " +
                            "Abstract classes should provide substantial shared behavior."
                        ).withAutoFix(false);

                        smells.add(smell);
                    }
                }

                // Check for interfaces with no methods (marker interfaces)
                if (cid.isInterface()) {
                    List<MethodDeclaration> methods = cid.getMethods();

                    if (methods.isEmpty()) {
                        int line = cid.getBegin().map(pos -> pos.line).orElse(0);

                        CodeSmell smell = new CodeSmell(
                            CodeSmellType.SPECULATIVE_GENERALITY,
                            filePath,
                            line,
                            String.format("Interface '%s' has no methods - marker interfaces are often unnecessary",
                                cid.getNameAsString())
                        );
                        smell.withSuggestion(
                            "Consider using annotations instead of marker interfaces. " +
                            "Marker interfaces were common in older Java but are now an anti-pattern."
                        ).withAutoFix(false);

                        smells.add(smell);
                    }
                }
            }

            @Override
            public void visit(MethodDeclaration md, Void arg) {
                super.visit(md, arg);

                // Check for unused parameters (speculative generality)
                List<Parameter> params = md.getParameters();

                for (Parameter param : params) {
                    String paramName = param.getNameAsString();

                    // Skip intentionally unused parameters (prefixed with _)
                    if (paramName.startsWith("_")) {
                        continue;
                    }

                    // Check if parameter is used in method body
                    md.getBody().ifPresent(body -> {
                        String bodyStr = body.toString();

                        // Simple check: is the parameter name mentioned in the body?
                        // (More sophisticated would be to check actual usage in AST)
                        if (!bodyStr.contains(paramName)) {
                            int line = param.getBegin().map(pos -> pos.line).orElse(0);

                            CodeSmell smell = new CodeSmell(
                                CodeSmellType.SPECULATIVE_GENERALITY,
                                filePath,
                                line,
                                String.format("Parameter '%s' in method '%s' is never used - speculative generality",
                                    paramName, md.getNameAsString())
                            );
                            smell.withSuggestion(
                                "Remove the unused parameter. If it's for future extension, " +
                                "implement it when actually needed (YAGNI principle)."
                            ).withAutoFix(true);

                            smells.add(smell);
                        }
                    });
                }

                // Check for overly generic method names with no real abstraction
                String methodName = md.getNameAsString();
                if (methodName.matches("^(handle|process|execute|manage|perform|do)[A-Z].*")) {
                    // These generic names are often a sign of speculative generality
                    // Only flag if method is very short (< 5 lines)
                    md.getBody().ifPresent(body -> {
                        int lines = body.getEnd().map(e -> e.line).orElse(0) -
                                   body.getBegin().map(b -> b.line).orElse(0) + 1;

                        if (lines < 5) {
                            int line = md.getBegin().map(pos -> pos.line).orElse(0);

                            CodeSmell smell = new CodeSmell(
                                CodeSmellType.SPECULATIVE_GENERALITY,
                                filePath,
                                line,
                                String.format("Method '%s' has overly generic name but does very little - possible over-abstraction",
                                    methodName)
                            );
                            smell.withSuggestion(
                                "Use a more specific method name that describes what it actually does. " +
                                "Generic names like 'handle' and 'process' often hide lack of clarity."
                            ).withAutoFix(false);

                            smells.add(smell);
                        }
                    });
                }
            }
        }, null);

        return smells;
    }
}

package com.pragmite.rules.smells;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.pragmite.model.CodeSmell;
import com.pragmite.model.CodeSmellType;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Detects inconsistent naming conventions in code.
 * Identifies variables and methods that don't follow standard Java naming conventions.
 *
 * Conventions checked:
 * - Methods: camelCase, start with lowercase, descriptive (not single char)
 * - Variables: camelCase, start with lowercase, not all uppercase (unless constant)
 * - No Hungarian notation (strName, intCount, etc.)
 */
@SuppressWarnings("unchecked")
public class InconsistentNamingDetector implements SmellDetector {

    private static final Pattern CAMEL_CASE = Pattern.compile("^[a-z][a-zA-Z0-9]*$");
    private static final Pattern UPPER_CASE = Pattern.compile("^[A-Z_]+$");
    private static final Pattern HUNGARIAN = Pattern.compile("^(str|int|bool|obj|arr|lst)[A-Z]");
    private static final Pattern SINGLE_CHAR = Pattern.compile("^[a-z]$");

    @Override
    public List<CodeSmell> detect(CompilationUnit cu, String filePath, String content) {
        List<CodeSmell> smells = new ArrayList<>();

        // Check method naming
        cu.findAll(MethodDeclaration.class).forEach(method -> {
            String name = method.getNameAsString();

            // Skip constructors and standard methods
            if (method.isStatic() && method.getNameAsString().equals(method.findAncestor(com.github.javaparser.ast.body.ClassOrInterfaceDeclaration.class).map(c -> c.getNameAsString()).orElse(""))) {
                return; // Skip constructor-like methods
            }

            if (isStandardMethod(name)) {
                return;
            }

            // Check if method name violates conventions
            if (!CAMEL_CASE.matcher(name).matches()) {
                CodeSmell smell = new CodeSmell(
                    CodeSmellType.UNUSED_VARIABLE, // Reusing existing type for naming issues
                    filePath,
                    method.getBegin().get().line,
                    "Method name '" + name + "' does not follow camelCase convention"
                );
                smell.withAffectedElement(name)
                    .withSuggestion("Rename to camelCase: " + toCamelCase(name))
                    .withAutoFix(false);
                smells.add(smell);
            }

            // Check for single-character method names (except x, y, z in math contexts)
            if (SINGLE_CHAR.matcher(name).matches() && !isMathContext(name)) {
                CodeSmell smell = new CodeSmell(
                    CodeSmellType.UNUSED_VARIABLE,
                    filePath,
                    method.getBegin().get().line,
                    "Method has non-descriptive single-character name: '" + name + "'"
                );
                smell.withAffectedElement(name)
                    .withSuggestion("Use a descriptive name that explains what the method does")
                    .withAutoFix(false);
                smells.add(smell);
            }

            // Check for Hungarian notation
            if (HUNGARIAN.matcher(name).find()) {
                CodeSmell smell = new CodeSmell(
                    CodeSmellType.UNUSED_VARIABLE,
                    filePath,
                    method.getBegin().get().line,
                    "Method name uses Hungarian notation: '" + name + "'"
                );
                smell.withAffectedElement(name)
                    .withSuggestion("Remove type prefix: " + removeHungarianPrefix(name))
                    .withAutoFix(false);
                smells.add(smell);
            }
        });

        // Check variable naming
        cu.findAll(VariableDeclarator.class).forEach(var -> {
            String name = var.getNameAsString();

            // Skip if it's likely a constant (all uppercase)
            if (UPPER_CASE.matcher(name).matches()) {
                return;
            }

            // Check if variable name violates conventions
            if (!CAMEL_CASE.matcher(name).matches() && !SINGLE_CHAR.matcher(name).matches()) {
                CodeSmell smell = new CodeSmell(
                    CodeSmellType.UNUSED_VARIABLE,
                    filePath,
                    var.getBegin().get().line,
                    "Variable name '" + name + "' does not follow camelCase convention"
                );
                smell.withSuggestion("Rename to camelCase: " + toCamelCase(name))
                    .withAutoFix(false);
                smells.add(smell);
            }

            // Check for Hungarian notation in variables
            if (HUNGARIAN.matcher(name).find()) {
                CodeSmell smell = new CodeSmell(
                    CodeSmellType.UNUSED_VARIABLE,
                    filePath,
                    var.getBegin().get().line,
                    "Variable uses Hungarian notation: '" + name + "'"
                );
                smell.withSuggestion("Remove type prefix: " + removeHungarianPrefix(name))
                    .withAutoFix(false);
                smells.add(smell);
            }
        });

        return smells;
    }

    private boolean isStandardMethod(String name) {
        return name.equals("toString") || name.equals("equals") || name.equals("hashCode") ||
               name.equals("main") || name.equals("run") || name.equals("call");
    }

    private boolean isMathContext(String name) {
        return name.equals("x") || name.equals("y") || name.equals("z") ||
               name.equals("i") || name.equals("j") || name.equals("k");
    }

    private String toCamelCase(String name) {
        // Simple conversion: remove underscores and capitalize
        if (name.contains("_")) {
            String[] parts = name.split("_");
            StringBuilder result = new StringBuilder(parts[0].toLowerCase());
            for (int i = 1; i < parts.length; i++) {
                if (!parts[i].isEmpty()) {
                    result.append(parts[i].substring(0, 1).toUpperCase());
                    if (parts[i].length() > 1) {
                        result.append(parts[i].substring(1).toLowerCase());
                    }
                }
            }
            return result.toString();
        }

        // If starts with uppercase, make lowercase
        if (Character.isUpperCase(name.charAt(0))) {
            return name.substring(0, 1).toLowerCase() + name.substring(1);
        }

        return name;
    }

    private String removeHungarianPrefix(String name) {
        // Remove common Hungarian prefixes
        String[] prefixes = {"str", "int", "bool", "obj", "arr", "lst"};

        for (String prefix : prefixes) {
            if (name.startsWith(prefix) && name.length() > prefix.length() &&
                Character.isUpperCase(name.charAt(prefix.length()))) {
                String withoutPrefix = name.substring(prefix.length());
                return withoutPrefix.substring(0, 1).toLowerCase() + withoutPrefix.substring(1);
            }
        }

        return name;
    }
}

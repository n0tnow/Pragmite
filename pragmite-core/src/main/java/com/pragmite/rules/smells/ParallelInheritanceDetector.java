package com.pragmite.rules.smells;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.pragmite.model.CodeSmell;
import com.pragmite.model.CodeSmellType;

import java.util.*;

/**
 * Detects Parallel Inheritance Hierarchies.
 * When you make a subclass of one class, you also have to make a subclass of another.
 *
 * Example:
 * - Employee, Manager, Salesman (one hierarchy)
 * - EmployeeData, ManagerData, SalesmanData (parallel hierarchy)
 */
public class ParallelInheritanceDetector implements SmellDetector {

    @Override
    public List<CodeSmell> detect(CompilationUnit cu, String filePath, String content) {
        List<CodeSmell> smells = new ArrayList<>();

        // Collect all class names and their parent classes
        Map<String, String> classHierarchy = new HashMap<>();

        cu.findAll(ClassOrInterfaceDeclaration.class).forEach(classDecl -> {
            if (classDecl.isInterface()) return;

            String className = classDecl.getNameAsString();

            // Get parent class if exists
            if (!classDecl.getExtendedTypes().isEmpty()) {
                ClassOrInterfaceType parent = classDecl.getExtendedTypes().get(0);
                classHierarchy.put(className, parent.getNameAsString());
            }
        });

        // Detect parallel naming patterns
        List<String> classNames = new ArrayList<>(classHierarchy.keySet());

        for (int i = 0; i < classNames.size(); i++) {
            for (int j = i + 1; j < classNames.size(); j++) {
                String class1 = classNames.get(i);
                String class2 = classNames.get(j);

                // Check if they have similar naming pattern (e.g., XxxData, XxxInfo, XxxImpl)
                if (haveSimilarSuffix(class1, class2)) {
                    String parent1 = classHierarchy.get(class1);
                    String parent2 = classHierarchy.get(class2);

                    if (parent1 != null && parent2 != null && haveSimilarSuffix(parent1, parent2)) {
                        // Found parallel inheritance!
                        CodeSmell smell = new CodeSmell(
                            CodeSmellType.INAPPROPRIATE_INTIMACY, // Reusing existing type
                            filePath,
                            1, // Class level
                            "Parallel inheritance hierarchy detected: '" + class1 + "' and '" + class2 +
                                "' follow same pattern as their parents '" + parent1 + "' and '" + parent2 + "'"
                        );
                        smell.withSuggestion("Consider merging the hierarchies or using composition instead of inheritance")
                            .withAutoFix(false);
                        smells.add(smell);
                        break; // Only report once per file
                    }
                }
            }
        }

        return smells;
    }

    /**
     * Checks if two class names have similar suffix patterns.
     */
    private boolean haveSimilarSuffix(String name1, String name2) {
        String[] commonSuffixes = {"Data", "Info", "Impl", "Handler", "Manager", "Service", "Controller", "DTO"};

        for (String suffix : commonSuffixes) {
            if (name1.endsWith(suffix) && name2.endsWith(suffix)) {
                // Both have same suffix - check if base names are related
                String base1 = name1.substring(0, name1.length() - suffix.length());
                String base2 = name2.substring(0, name2.length() - suffix.length());

                // If base names are different but both use same suffix, it might be parallel
                return !base1.equals(base2);
            }
        }

        return false;
    }
}

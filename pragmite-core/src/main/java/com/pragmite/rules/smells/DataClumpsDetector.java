package com.pragmite.rules.smells;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.pragmite.model.CodeSmell;
import com.pragmite.model.CodeSmellType;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Data Clumps detector.
 * Detects groups of parameters that appear together in multiple method signatures.
 * This suggests they should be refactored into a dedicated class (Parameter Object pattern).
 */
public class DataClumpsDetector implements SmellDetector {

    private static final int MIN_PARAMETERS_IN_CLUMP = 3;
    private static final int MIN_OCCURRENCES = 2; // Appears in at least 2 methods

    @Override
    public List<CodeSmell> detect(CompilationUnit cu, String filePath, String content) {
        List<CodeSmell> smells = new ArrayList<>();
        Map<String, List<MethodInfo>> classMethodsMap = new HashMap<>();

        // Collect all methods grouped by class
        cu.accept(new VoidVisitorAdapter<Void>() {
            private String currentClass = "";

            @Override
            public void visit(ClassOrInterfaceDeclaration cid, Void arg) {
                currentClass = cid.getNameAsString();
                super.visit(cid, arg);
            }

            @Override
            public void visit(MethodDeclaration md, Void arg) {
                super.visit(md, arg);

                if (md.getParameters().size() >= MIN_PARAMETERS_IN_CLUMP) {
                    List<String> paramTypes = md.getParameters().stream()
                        .map(p -> p.getType().asString())
                        .collect(Collectors.toList());

                    int line = md.getBegin().map(pos -> pos.line).orElse(0);

                    MethodInfo info = new MethodInfo(
                        md.getNameAsString(),
                        line,
                        paramTypes
                    );

                    classMethodsMap.computeIfAbsent(currentClass, k -> new ArrayList<>()).add(info);
                }
            }
        }, null);

        // Analyze each class for data clumps
        for (Map.Entry<String, List<MethodInfo>> entry : classMethodsMap.entrySet()) {
            String className = entry.getKey();
            List<MethodInfo> methods = entry.getValue();

            if (methods.size() < MIN_OCCURRENCES) {
                continue;
            }

            // Find parameter clumps (3+ consecutive parameters that appear together)
            Map<String, List<MethodInfo>> clumps = new HashMap<>();

            for (int i = 0; i < methods.size(); i++) {
                for (int j = i + 1; j < methods.size(); j++) {
                    MethodInfo method1 = methods.get(i);
                    MethodInfo method2 = methods.get(j);

                    List<String> commonParams = findCommonParameterSequence(
                        method1.paramTypes,
                        method2.paramTypes
                    );

                    if (commonParams.size() >= MIN_PARAMETERS_IN_CLUMP) {
                        String clumpKey = String.join(",", commonParams);

                        clumps.computeIfAbsent(clumpKey, k -> new ArrayList<>());
                        if (!clumps.get(clumpKey).contains(method1)) {
                            clumps.get(clumpKey).add(method1);
                        }
                        if (!clumps.get(clumpKey).contains(method2)) {
                            clumps.get(clumpKey).add(method2);
                        }
                    }
                }
            }

            // Report clumps
            for (Map.Entry<String, List<MethodInfo>> clumpEntry : clumps.entrySet()) {
                if (clumpEntry.getValue().size() >= MIN_OCCURRENCES) {
                    String clumpParams = clumpEntry.getKey();
                    List<MethodInfo> affectedMethods = clumpEntry.getValue();

                    String methodNames = affectedMethods.stream()
                        .map(m -> m.name)
                        .collect(Collectors.joining(", "));

                    int firstLine = affectedMethods.get(0).line;

                    CodeSmell smell = new CodeSmell(
                        CodeSmellType.DATA_CLUMPS,
                        filePath,
                        firstLine,
                        String.format("Data clump detected: parameters (%s) appear together in %d methods: %s",
                            clumpParams, affectedMethods.size(), methodNames)
                    );
                    smell.withSuggestion("Consider creating a Parameter Object class to group these related parameters")
                         .withAutoFix(false);

                    smells.add(smell);
                }
            }
        }

        return smells;
    }

    /**
     * Finds longest common parameter sequence between two parameter lists.
     * Supports both consecutive and non-consecutive matching (maintains order).
     */
    private List<String> findCommonParameterSequence(List<String> params1, List<String> params2) {
        // First try to find consecutive sequence
        List<String> consecutiveSequence = findConsecutiveSequence(params1, params2);

        if (consecutiveSequence.size() >= MIN_PARAMETERS_IN_CLUMP) {
            return consecutiveSequence;
        }

        // If no consecutive sequence, find common parameters in order
        List<String> commonInOrder = new ArrayList<>();
        int j = 0;

        for (String param1 : params1) {
            while (j < params2.size()) {
                if (params2.get(j).equals(param1)) {
                    commonInOrder.add(param1);
                    j++;
                    break;
                }
                j++;
            }
        }

        return commonInOrder;
    }

    /**
     * Finds longest consecutive sequence.
     */
    private List<String> findConsecutiveSequence(List<String> params1, List<String> params2) {
        List<String> longestSequence = new ArrayList<>();

        for (int i = 0; i < params1.size(); i++) {
            for (int j = 0; j < params2.size(); j++) {
                List<String> currentSequence = new ArrayList<>();

                int k = 0;
                while (i + k < params1.size() &&
                       j + k < params2.size() &&
                       params1.get(i + k).equals(params2.get(j + k))) {
                    currentSequence.add(params1.get(i + k));
                    k++;
                }

                if (currentSequence.size() > longestSequence.size()) {
                    longestSequence = currentSequence;
                }
            }
        }

        return longestSequence;
    }

    /**
     * Method information for data clump analysis.
     */
    private static class MethodInfo {
        final String name;
        final int line;
        final List<String> paramTypes;

        MethodInfo(String name, int line, List<String> paramTypes) {
            this.name = name;
            this.line = line;
            this.paramTypes = paramTypes;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            MethodInfo that = (MethodInfo) o;
            return line == that.line && Objects.equals(name, that.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, line);
        }
    }
}

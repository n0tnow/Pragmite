package com.pragmite.metrics;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.pragmite.analyzer.CyclomaticComplexityCalculator;

import java.util.*;

/**
 * Chidamber & Kemerer (CK) Metrics Suite implementation.
 *
 * Implements the following OO design metrics:
 * - WMC (Weighted Methods per Class)
 * - DIT (Depth of Inheritance Tree)
 * - NOC (Number of Children)
 * - CBO (Coupling Between Objects)
 * - RFC (Response For a Class)
 * - LCOM (Lack of Cohesion in Methods)
 *
 * Reference: "A Metrics Suite for Object Oriented Design"
 * IEEE Transactions on Software Engineering, 1994
 */
public class CKMetricsCalculator {

    private final CyclomaticComplexityCalculator complexityCalculator;

    public CKMetricsCalculator() {
        this.complexityCalculator = new CyclomaticComplexityCalculator();
    }

    /**
     * Calculates all CK metrics for a given compilation unit.
     */
    public Map<String, CKMetrics> calculateAll(CompilationUnit cu, String filePath) {
        Map<String, CKMetrics> metricsMap = new HashMap<>();

        // First pass: collect class hierarchy information
        Map<String, String> parentClasses = new HashMap<>();
        Map<String, Set<String>> childrenMap = new HashMap<>();

        collectHierarchyInfo(cu, parentClasses, childrenMap);

        // Second pass: calculate metrics for each class
        cu.accept(new VoidVisitorAdapter<Void>() {
            @Override
            public void visit(ClassOrInterfaceDeclaration cid, Void arg) {
                super.visit(cid, arg);

                if (cid.isInterface()) {
                    return; // Skip interfaces
                }

                String className = cid.getNameAsString();
                int line = cid.getBegin().map(pos -> pos.line).orElse(0);

                CKMetrics metrics = new CKMetrics(className, filePath, line);

                // Calculate each metric
                metrics.setWmc(calculateWMC(cid));
                metrics.setDit(calculateDIT(className, parentClasses));
                metrics.setNoc(calculateNOC(className, childrenMap));
                metrics.setCbo(calculateCBO(cid));
                metrics.setRfc(calculateRFC(cid));
                metrics.setLcom(calculateLCOM(cid));

                metricsMap.put(className, metrics);
            }
        }, null);

        return metricsMap;
    }

    /**
     * WMC (Weighted Methods per Class)
     * Sum of cyclomatic complexities of all methods in a class.
     * Higher values indicate more complex classes.
     */
    private int calculateWMC(ClassOrInterfaceDeclaration cid) {
        int[] totalComplexity = {0};

        cid.accept(new VoidVisitorAdapter<Void>() {
            @Override
            public void visit(MethodDeclaration md, Void arg) {
                super.visit(md, arg);
                int complexity = complexityCalculator.calculate(md);
                totalComplexity[0] += complexity;
            }
        }, null);

        return totalComplexity[0];
    }

    /**
     * DIT (Depth of Inheritance Tree)
     * Maximum length from class to root of inheritance tree.
     * Higher values indicate more inheritance levels (harder to understand).
     */
    private int calculateDIT(String className, Map<String, String> parentClasses) {
        int depth = 0;
        String current = className;

        // Traverse up the inheritance tree
        Set<String> visited = new HashSet<>();
        while (parentClasses.containsKey(current) && !visited.contains(current)) {
            visited.add(current);
            current = parentClasses.get(current);
            depth++;

            // Prevent infinite loops
            if (depth > 20) {
                break;
            }
        }

        return depth;
    }

    /**
     * NOC (Number of Children)
     * Number of immediate subclasses of a class.
     * Higher values indicate more responsibility and reuse.
     */
    private int calculateNOC(String className, Map<String, Set<String>> childrenMap) {
        Set<String> children = childrenMap.get(className);
        return children != null ? children.size() : 0;
    }

    /**
     * CBO (Coupling Between Objects)
     * Number of classes to which a class is coupled.
     * Counts unique classes referenced in method calls, field access, etc.
     * Higher values indicate more dependencies (harder to reuse/maintain).
     */
    private int calculateCBO(ClassOrInterfaceDeclaration cid) {
        Set<String> coupledClasses = new HashSet<>();

        cid.accept(new VoidVisitorAdapter<Void>() {
            @Override
            public void visit(MethodCallExpr mce, Void arg) {
                super.visit(mce, arg);

                // Get scope of method call (the object it's called on)
                mce.getScope().ifPresent(scope -> {
                    String scopeStr = scope.toString();
                    if (!scopeStr.equals("this") && !scopeStr.equals("super")) {
                        // Extract class name (simple heuristic)
                        String className = extractClassName(scopeStr);
                        if (className != null && !isPrimitive(className)) {
                            coupledClasses.add(className);
                        }
                    }
                });
            }

            @Override
            public void visit(FieldAccessExpr fae, Void arg) {
                super.visit(fae, arg);

                String scopeStr = fae.getScope().toString();
                if (!scopeStr.equals("this") && !scopeStr.equals("super")) {
                    String className = extractClassName(scopeStr);
                    if (className != null && !isPrimitive(className)) {
                        coupledClasses.add(className);
                    }
                }
            }

            @Override
            public void visit(ClassOrInterfaceType type, Void arg) {
                super.visit(type, arg);

                // Count type dependencies (fields, parameters, return types)
                String typeName = type.getNameAsString();
                if (!isPrimitive(typeName) && !typeName.equals(cid.getNameAsString())) {
                    coupledClasses.add(typeName);
                }
            }
        }, null);

        return coupledClasses.size();
    }

    /**
     * RFC (Response For a Class)
     * Number of methods that can be invoked in response to a message.
     * Includes all methods in the class plus all external methods called.
     * Higher values indicate more complex behavior.
     */
    private int calculateRFC(ClassOrInterfaceDeclaration cid) {
        Set<String> responseMethods = new HashSet<>();

        // Add all methods in the class
        for (MethodDeclaration md : cid.getMethods()) {
            responseMethods.add(md.getNameAsString());
        }

        // Add all external methods called
        cid.accept(new VoidVisitorAdapter<Void>() {
            @Override
            public void visit(MethodCallExpr mce, Void arg) {
                super.visit(mce, arg);

                // Count external method calls
                mce.getScope().ifPresent(scope -> {
                    String scopeStr = scope.toString();
                    if (!scopeStr.equals("this") && !scopeStr.equals("super")) {
                        // External method call
                        responseMethods.add(scopeStr + "." + mce.getNameAsString());
                    }
                });
            }
        }, null);

        return responseMethods.size();
    }

    /**
     * LCOM (Lack of Cohesion in Methods)
     * Measures cohesion among methods by analyzing shared field access.
     *
     * Algorithm:
     * - P = pairs of methods that don't share fields
     * - Q = pairs of methods that share fields
     * - LCOM = P - Q (if positive), else 0
     *
     * Higher values indicate low cohesion (class should be split).
     */
    private int calculateLCOM(ClassOrInterfaceDeclaration cid) {
        List<MethodDeclaration> methods = cid.getMethods();

        // Need at least 2 methods
        if (methods.size() < 2) {
            return 0;
        }

        // Build field access map for each method
        Map<MethodDeclaration, Set<String>> methodFieldAccess = new HashMap<>();

        for (MethodDeclaration md : methods) {
            Set<String> fieldsAccessed = new HashSet<>();

            md.accept(new VoidVisitorAdapter<Void>() {
                @Override
                public void visit(FieldAccessExpr fae, Void arg) {
                    super.visit(fae, arg);

                    // Only count 'this' field accesses
                    if (fae.getScope().toString().equals("this")) {
                        fieldsAccessed.add(fae.getNameAsString());
                    }
                }

                @Override
                public void visit(com.github.javaparser.ast.expr.NameExpr ne, Void arg) {
                    super.visit(ne, arg);

                    // Check if it's a field access (simple heuristic)
                    String name = ne.getNameAsString();
                    if (isLikelyFieldAccess(name, cid)) {
                        fieldsAccessed.add(name);
                    }
                }
            }, null);

            methodFieldAccess.put(md, fieldsAccessed);
        }

        // Count pairs
        int pairsShareFields = 0;    // Q
        int pairsNoShareFields = 0;   // P

        for (int i = 0; i < methods.size(); i++) {
            for (int j = i + 1; j < methods.size(); j++) {
                MethodDeclaration m1 = methods.get(i);
                MethodDeclaration m2 = methods.get(j);

                Set<String> fields1 = methodFieldAccess.get(m1);
                Set<String> fields2 = methodFieldAccess.get(m2);

                // Check if they share any fields
                Set<String> intersection = new HashSet<>(fields1);
                intersection.retainAll(fields2);

                if (!intersection.isEmpty()) {
                    pairsShareFields++;
                } else {
                    pairsNoShareFields++;
                }
            }
        }

        // LCOM = P - Q, or 0 if negative
        int lcom = pairsNoShareFields - pairsShareFields;
        return Math.max(0, lcom);
    }

    /**
     * Collects inheritance hierarchy information.
     */
    private void collectHierarchyInfo(CompilationUnit cu,
                                     Map<String, String> parentClasses,
                                     Map<String, Set<String>> childrenMap) {
        cu.accept(new VoidVisitorAdapter<Void>() {
            @Override
            public void visit(ClassOrInterfaceDeclaration cid, Void arg) {
                super.visit(cid, arg);

                if (cid.isInterface()) {
                    return;
                }

                String className = cid.getNameAsString();

                // Get parent class
                if (!cid.getExtendedTypes().isEmpty()) {
                    String parentName = cid.getExtendedTypes(0).getNameAsString();
                    parentClasses.put(className, parentName);

                    // Add to children map
                    childrenMap.computeIfAbsent(parentName, k -> new HashSet<>()).add(className);
                }
            }
        }, null);
    }

    /**
     * Heuristic to check if a name is likely a field access.
     */
    private boolean isLikelyFieldAccess(String name, ClassOrInterfaceDeclaration cid) {
        // Check if the name matches any field in the class
        for (FieldDeclaration fd : cid.getFields()) {
            if (fd.getVariables().stream()
                .anyMatch(v -> v.getNameAsString().equals(name))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Extracts class name from a scope expression.
     */
    private String extractClassName(String scope) {
        // Handle cases like:
        // - "object" -> "object"
        // - "obj.method()" -> "obj"
        // - "SomeClass" -> "SomeClass"

        if (scope == null || scope.isEmpty()) {
            return null;
        }

        // Take first identifier
        int dotIndex = scope.indexOf('.');
        if (dotIndex > 0) {
            return scope.substring(0, dotIndex);
        }

        int parenIndex = scope.indexOf('(');
        if (parenIndex > 0) {
            return scope.substring(0, parenIndex);
        }

        return scope;
    }

    /**
     * Checks if a type name is a primitive or common JDK class.
     */
    private boolean isPrimitive(String typeName) {
        return typeName.equals("String") ||
               typeName.equals("Integer") ||
               typeName.equals("Long") ||
               typeName.equals("Double") ||
               typeName.equals("Float") ||
               typeName.equals("Boolean") ||
               typeName.equals("Character") ||
               typeName.equals("Byte") ||
               typeName.equals("Short") ||
               typeName.equals("Object") ||
               typeName.equals("System") ||
               typeName.equals("Math");
    }
}

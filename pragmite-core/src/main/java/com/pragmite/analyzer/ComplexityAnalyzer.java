package com.pragmite.analyzer;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.stmt.*;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.pragmite.model.BigOComplexity;
import com.pragmite.model.ComplexityInfo;

import java.util.*;

/**
 * Big-O karmaşıklık analizi yapan sınıf.
 * Döngüler, recursion, stream'ler ve koleksiyon işlemlerini analiz eder.
 */
public class ComplexityAnalyzer {

    // O(n) karmaşıklığına sahip stream/koleksiyon metodları
    private static final Set<String> LINEAR_METHODS = Set.of(
        "contains", "indexOf", "lastIndexOf", "remove", "removeAll",
        "retainAll", "containsAll", "equals", "toArray",
        "forEach", "filter", "map", "reduce", "collect",
        "anyMatch", "allMatch", "noneMatch", "findFirst", "findAny",
        "count", "min", "max", "sum", "average"
    );

    // O(log n) karmaşıklığına sahip metodlar
    private static final Set<String> LOG_METHODS = Set.of(
        "binarySearch", "floor", "ceiling", "lower", "higher",
        "get", "put", "remove", "containsKey" // TreeMap/TreeSet için
    );

    // O(n log n) karmaşıklığına sahip metodlar
    private static final Set<String> N_LOG_N_METHODS = Set.of(
        "sort", "sorted", "parallelSort"
    );

    // Stream başlatan metodlar
    private static final Set<String> STREAM_STARTERS = Set.of(
        "stream", "parallelStream", "of", "iterate", "generate"
    );

    // O(n) olabilecek String metodları
    private static final Set<String> STRING_LINEAR_METHODS = Set.of(
        "contains", "indexOf", "lastIndexOf", "startsWith", "endsWith",
        "replace", "replaceAll", "split", "substring", "toLowerCase", "toUpperCase"
    );

    public List<ComplexityInfo> analyze(CompilationUnit cu, String filePath) {
        List<ComplexityInfo> results = new ArrayList<>();

        cu.accept(new VoidVisitorAdapter<Void>() {
            @Override
            public void visit(MethodDeclaration md, Void arg) {
                super.visit(md, arg);

                String methodName = md.getNameAsString();
                int startLine = md.getBegin().map(pos -> pos.line).orElse(0);

                // 1. Döngü analizi
                LoopAnalysisResult loopResult = analyzeLoops(md);

                // 2. Recursion analizi
                RecursionResult recursionResult = analyzeRecursion(md, cu);

                // 3. Stream ve koleksiyon analizi
                StreamAnalysisResult streamResult = analyzeStreamsAndCollections(md);

                // 4. O(log n) method call analizi
                MethodCallAnalysisResult methodCallResult = analyzeMethodCalls(md);

                // En yüksek karmaşıklığı bul
                BigOComplexity finalComplexity = BigOComplexity.O_1;
                StringBuilder reasons = new StringBuilder();

                if (loopResult.maxDepth > 0) {
                    finalComplexity = BigOComplexity.dominant(finalComplexity, loopResult.complexity);
                    if (reasons.length() > 0) reasons.append("; ");
                    reasons.append(loopResult.reason);
                }

                if (recursionResult.isRecursive) {
                    finalComplexity = BigOComplexity.dominant(finalComplexity, recursionResult.complexity);
                    if (reasons.length() > 0) reasons.append("; ");
                    reasons.append(recursionResult.reason);
                }

                if (streamResult.hasComplexOperations) {
                    finalComplexity = BigOComplexity.dominant(finalComplexity, streamResult.complexity);
                    if (reasons.length() > 0) reasons.append("; ");
                    reasons.append(streamResult.reason);
                }

                if (methodCallResult.hasLogNMethods) {
                    finalComplexity = BigOComplexity.dominant(finalComplexity, methodCallResult.complexity);
                    if (reasons.length() > 0) reasons.append("; ");
                    reasons.append(methodCallResult.reason);
                }

                // İç içe döngü içinde stream varsa karmaşıklığı çarp
                if (loopResult.maxDepth > 0 && streamResult.hasComplexOperations) {
                    finalComplexity = multiplyComplexity(loopResult.complexity, streamResult.complexity);
                    reasons.append(" [nested stream in loop]");
                }

                // Stream'in kendisi döngü içinde başlatılmışsa (stream().filter() in loop)
                if (streamResult.hasNestedStreamInLoop && !reasons.toString().contains("[nested stream in loop]")) {
                    finalComplexity = BigOComplexity.dominant(finalComplexity, BigOComplexity.O_N_SQUARED);
                    reasons.append(" [stream started in loop]");
                }

                // Döngü içinde O(log n) metot çağrısı varsa O(n log n)
                if (loopResult.maxDepth > 0 && methodCallResult.hasLogNMethods) {
                    finalComplexity = BigOComplexity.O_N_LOG_N;
                    if (!reasons.toString().contains("[nested")) {
                        reasons.append(" [O(log n) in loop]");
                    }
                }

                if (finalComplexity != BigOComplexity.O_1) {
                    ComplexityInfo info = new ComplexityInfo(filePath, methodName, startLine, finalComplexity);
                    info.setNestedLoopDepth(loopResult.maxDepth);
                    info.setReason(reasons.toString());
                    results.add(info);
                }
            }
        }, null);

        return results;
    }

    /**
     * Metottaki döngüleri analiz eder.
     */
    private LoopAnalysisResult analyzeLoops(MethodDeclaration md) {
        int[] maxDepth = {0};
        int[] currentDepth = {0};
        BigOComplexity[] complexity = {BigOComplexity.O_1};
        StringBuilder reason = new StringBuilder();

        // İç içe olmayan ardışık döngüleri takip et
        List<Integer> sequentialLoopDepths = new ArrayList<>();

        md.accept(new VoidVisitorAdapter<Void>() {
            private void enterLoop(String loopType) {
                // Eğer depth 0 ise (top-level loop), ardışık döngü olarak kaydet
                if (currentDepth[0] == 0) {
                    sequentialLoopDepths.add(1);
                }

                currentDepth[0]++;
                if (currentDepth[0] > maxDepth[0]) {
                    maxDepth[0] = currentDepth[0];
                    updateComplexity();
                }
            }

            private void exitLoop() {
                currentDepth[0]--;
            }

            private void updateComplexity() {
                switch (maxDepth[0]) {
                    case 1:
                        complexity[0] = BigOComplexity.O_N;
                        reason.setLength(0);
                        reason.append("single loop");
                        break;
                    case 2:
                        complexity[0] = BigOComplexity.O_N_SQUARED;
                        reason.setLength(0);
                        reason.append("nested loop (depth 2)");
                        break;
                    case 3:
                        complexity[0] = BigOComplexity.O_N_CUBED;
                        reason.setLength(0);
                        reason.append("nested loop (depth 3)");
                        break;
                    default:
                        complexity[0] = BigOComplexity.O_N_CUBED;
                        reason.setLength(0);
                        reason.append("deeply nested loop (depth " + maxDepth[0] + ")");
                }
            }

            @Override
            public void visit(ForStmt n, Void arg) {
                enterLoop("for");
                super.visit(n, arg);
                exitLoop();
            }

            @Override
            public void visit(ForEachStmt n, Void arg) {
                enterLoop("foreach");
                super.visit(n, arg);
                exitLoop();
            }

            @Override
            public void visit(WhileStmt n, Void arg) {
                enterLoop("while");
                super.visit(n, arg);
                exitLoop();
            }

            @Override
            public void visit(DoStmt n, Void arg) {
                enterLoop("do-while");
                super.visit(n, arg);
                exitLoop();
            }
        }, null);

        // Ardışık döngüler hakkında bilgi ekle
        if (sequentialLoopDepths.size() > 1 && maxDepth[0] == 1) {
            reason.append(" (").append(sequentialLoopDepths.size()).append(" sequential loops, still O(n))");
        }

        return new LoopAnalysisResult(maxDepth[0], complexity[0], reason.toString());
    }

    /**
     * Recursion analizi yapar.
     */
    private RecursionResult analyzeRecursion(MethodDeclaration md, CompilationUnit cu) {
        String methodName = md.getNameAsString();
        int paramCount = md.getParameters().size();
        boolean[] isRecursive = {false};
        boolean[] isTailRecursive = {false};
        int[] recursiveCallCount = {0};

        md.accept(new VoidVisitorAdapter<Void>() {
            @Override
            public void visit(MethodCallExpr mce, Void arg) {
                super.visit(mce, arg);

                // Aynı isimde ve parametre sayısında metot çağrısı var mı?
                if (mce.getNameAsString().equals(methodName)) {
                    // Basit recursion kontrolü
                    if (mce.getArguments().size() == paramCount) {
                        isRecursive[0] = true;
                        recursiveCallCount[0]++;

                        // Tail recursion kontrolü (son statement mı?)
                        if (isLastStatement(mce, md)) {
                            isTailRecursive[0] = true;
                        }
                    }
                }
            }
        }, null);

        if (!isRecursive[0]) {
            return new RecursionResult(false, BigOComplexity.O_1, "");
        }

        // Recursion karmaşıklığı tahmini
        BigOComplexity complexity;
        String reason;

        // Check for divide-and-conquer pattern (binary search, etc.)
        boolean isDivideAndConquer = checkDivideAndConquerPattern(md);

        if (recursiveCallCount[0] == 1) {
            // Single recursive call - likely O(n) or O(log n)
            if (isDivideAndConquer) {
                complexity = BigOComplexity.O_LOG_N;
                reason = "divide-and-conquer recursion (binary search pattern)";
            } else if (isTailRecursive[0]) {
                complexity = BigOComplexity.O_N;
                reason = "tail recursion";
            } else {
                complexity = BigOComplexity.O_N;
                reason = "linear recursion";
            }
        } else if (recursiveCallCount[0] == 2) {
            // Two recursive calls - check if it's divide-and-conquer or binary recursion
            if (isDivideAndConquer) {
                // Binary search with two branches (both won't execute)
                complexity = BigOComplexity.O_LOG_N;
                reason = "divide-and-conquer with conditional branches";
            } else {
                // Both calls execute (like fibonacci)
                complexity = BigOComplexity.O_2_N;
                reason = "binary recursion (exponential)";
            }
        } else {
            // Multiple recursive calls
            complexity = BigOComplexity.O_2_N;
            reason = "multiple recursion calls (" + recursiveCallCount[0] + ")";
        }

        return new RecursionResult(true, complexity, reason);
    }

    /**
     * Checks if method contains divide-and-conquer pattern (dividing input by 2).
     * Common patterns: mid = (low + high) / 2, n / 2
     */
    private boolean checkDivideAndConquerPattern(MethodDeclaration md) {
        boolean[] hasDivideByTwo = {false};
        boolean[] hasConditionalRecursion = {false};

        md.accept(new VoidVisitorAdapter<Void>() {
            @Override
            public void visit(com.github.javaparser.ast.expr.BinaryExpr be, Void arg) {
                super.visit(be, arg);

                // Check for division by 2: / 2
                if (be.getOperator() == com.github.javaparser.ast.expr.BinaryExpr.Operator.DIVIDE) {
                    String right = be.getRight().toString();
                    if (right.equals("2") || right.equals("2.0")) {
                        hasDivideByTwo[0] = true;
                    }
                }
            }

            @Override
            public void visit(com.github.javaparser.ast.stmt.IfStmt is, Void arg) {
                super.visit(is, arg);
                // Has if/else with recursive calls in different branches
                hasConditionalRecursion[0] = true;
            }
        }, null);

        return hasDivideByTwo[0] && hasConditionalRecursion[0];
    }

    /**
     * Checks if expression is the last statement in the method.
     */
    private boolean isLastStatement(MethodCallExpr mce, MethodDeclaration md) {
        // Simple check: is it inside a return statement?
        return mce.getParentNode()
            .filter(parent -> parent instanceof ReturnStmt)
            .isPresent();
    }

    /**
     * Stream ve koleksiyon işlemlerini analiz eder.
     */
    private StreamAnalysisResult analyzeStreamsAndCollections(MethodDeclaration md) {
        List<String> streamOperations = new ArrayList<>();
        boolean[] hasNestedStreamInLoop = {false};
        int[] loopDepth = {0};
        BigOComplexity[] maxComplexity = {BigOComplexity.O_1};
        StringBuilder reason = new StringBuilder();

        md.accept(new VoidVisitorAdapter<Void>() {
            @Override
            public void visit(ForStmt n, Void arg) {
                loopDepth[0]++;
                super.visit(n, arg);
                loopDepth[0]--;
            }

            @Override
            public void visit(ForEachStmt n, Void arg) {
                loopDepth[0]++;
                super.visit(n, arg);
                loopDepth[0]--;
            }

            @Override
            public void visit(WhileStmt n, Void arg) {
                loopDepth[0]++;
                super.visit(n, arg);
                loopDepth[0]--;
            }

            @Override
            public void visit(MethodCallExpr mce, Void arg) {
                super.visit(mce, arg);

                String callName = mce.getNameAsString();

                // Stream başlatıcı
                if (STREAM_STARTERS.contains(callName)) {
                    streamOperations.add(callName);
                    if (loopDepth[0] > 0) {
                        hasNestedStreamInLoop[0] = true;
                    }
                }

                // O(n log n) işlemler
                if (N_LOG_N_METHODS.contains(callName)) {
                    maxComplexity[0] = BigOComplexity.dominant(maxComplexity[0], BigOComplexity.O_N_LOG_N);
                    if (reason.length() > 0) reason.append(", ");
                    reason.append(callName).append("()");
                }

                // O(n) işlemler
                if (LINEAR_METHODS.contains(callName)) {
                    if (maxComplexity[0].ordinal() < BigOComplexity.O_N.ordinal()) {
                        maxComplexity[0] = BigOComplexity.O_N;
                    }
                    streamOperations.add(callName);
                }

                // String metodları döngü içinde O(n*m) olabilir
                if (STRING_LINEAR_METHODS.contains(callName) && loopDepth[0] > 0) {
                    maxComplexity[0] = BigOComplexity.dominant(maxComplexity[0], BigOComplexity.O_N_SQUARED);
                    if (reason.length() > 0) reason.append(", ");
                    reason.append("String.").append(callName).append("() in loop");
                }

                // flatMap özel durumu - O(n*m)
                if (callName.equals("flatMap")) {
                    maxComplexity[0] = BigOComplexity.dominant(maxComplexity[0], BigOComplexity.O_N_SQUARED);
                    if (reason.length() > 0) reason.append(", ");
                    reason.append("flatMap (O(n*m))");
                }
            }
        }, null);

        boolean hasComplexOps = maxComplexity[0] != BigOComplexity.O_1 || streamOperations.size() > 2;

        if (hasComplexOps && reason.length() == 0 && !streamOperations.isEmpty()) {
            reason.append("stream chain: ").append(String.join(" -> ", streamOperations));
        }

        return new StreamAnalysisResult(hasComplexOps, maxComplexity[0], reason.toString(), hasNestedStreamInLoop[0]);
    }

    /**
     * İki karmaşıklığı çarpar (iç içe durumlar için).
     */
    private BigOComplexity multiplyComplexity(BigOComplexity a, BigOComplexity b) {
        // O(n) * O(n) = O(n²)
        if (a == BigOComplexity.O_N && b == BigOComplexity.O_N) return BigOComplexity.O_N_SQUARED;
        // O(n²) * O(n) = O(n³)
        if ((a == BigOComplexity.O_N_SQUARED && b == BigOComplexity.O_N) ||
            (a == BigOComplexity.O_N && b == BigOComplexity.O_N_SQUARED)) return BigOComplexity.O_N_CUBED;
        // O(n) * O(log n) = O(n log n)
        if ((a == BigOComplexity.O_N && b == BigOComplexity.O_LOG_N) ||
            (a == BigOComplexity.O_LOG_N && b == BigOComplexity.O_N)) return BigOComplexity.O_N_LOG_N;

        return BigOComplexity.dominant(a, b);
    }

    /**
     * Analyzes method calls for O(log n) complexity operations.
     * Detects: binarySearch, TreeMap/TreeSet operations, etc.
     */
    private MethodCallAnalysisResult analyzeMethodCalls(MethodDeclaration md) {
        boolean[] hasLogNCall = {false};
        List<String> logNMethods = new ArrayList<>();

        md.accept(new VoidVisitorAdapter<Void>() {
            @Override
            public void visit(MethodCallExpr mce, Void arg) {
                super.visit(mce, arg);

                String methodName = mce.getNameAsString();

                // Check for O(log n) methods
                if (LOG_METHODS.contains(methodName)) {
                    hasLogNCall[0] = true;
                    logNMethods.add(methodName);
                }
            }
        }, null);

        if (hasLogNCall[0]) {
            String reason = "O(log n) method calls: " + String.join(", ", logNMethods);
            return new MethodCallAnalysisResult(true, BigOComplexity.O_LOG_N, reason);
        }

        return new MethodCallAnalysisResult(false, BigOComplexity.O_1, "");
    }

    // Inner classes for results
    private static class LoopAnalysisResult {
        final int maxDepth;
        final BigOComplexity complexity;
        final String reason;

        LoopAnalysisResult(int maxDepth, BigOComplexity complexity, String reason) {
            this.maxDepth = maxDepth;
            this.complexity = complexity;
            this.reason = reason;
        }
    }

    private static class RecursionResult {
        final boolean isRecursive;
        final BigOComplexity complexity;
        final String reason;

        RecursionResult(boolean isRecursive, BigOComplexity complexity, String reason) {
            this.isRecursive = isRecursive;
            this.complexity = complexity;
            this.reason = reason;
        }
    }

    private static class StreamAnalysisResult {
        final boolean hasComplexOperations;
        final BigOComplexity complexity;
        final String reason;
        final boolean hasNestedStreamInLoop;

        StreamAnalysisResult(boolean hasComplexOperations, BigOComplexity complexity,
                            String reason, boolean hasNestedStreamInLoop) {
            this.hasComplexOperations = hasComplexOperations;
            this.complexity = complexity;
            this.reason = reason;
            this.hasNestedStreamInLoop = hasNestedStreamInLoop;
        }
    }

    private static class MethodCallAnalysisResult {
        final boolean hasLogNMethods;
        final BigOComplexity complexity;
        final String reason;

        MethodCallAnalysisResult(boolean hasLogNMethods, BigOComplexity complexity, String reason) {
            this.hasLogNMethods = hasLogNMethods;
            this.complexity = complexity;
            this.reason = reason;
        }
    }
}

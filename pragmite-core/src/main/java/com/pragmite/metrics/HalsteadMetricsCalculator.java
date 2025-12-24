package com.pragmite.metrics;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.*;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

import java.util.*;

/**
 * Halstead Complexity Metrics Calculator.
 *
 * Halstead metrics are based on counting operators and operands in code:
 * - n1 = number of distinct operators
 * - n2 = number of distinct operands
 * - N1 = total number of operators
 * - N2 = total number of operands
 *
 * Derived metrics:
 * - Program Vocabulary: n = n1 + n2
 * - Program Length: N = N1 + N2
 * - Calculated Program Length: N^ = n1 * log2(n1) + n2 * log2(n2)
 * - Volume: V = N * log2(n)
 * - Difficulty: D = (n1/2) * (N2/n2)
 * - Effort: E = D * V
 * - Time to Program: T = E / 18 seconds
 * - Number of Delivered Bugs: B = V / 3000
 *
 * Reference: Halstead, Maurice H. (1977). "Elements of Software Science"
 */
public class HalsteadMetricsCalculator {

    /**
     * Calculates Halstead metrics for all methods in a compilation unit.
     */
    public Map<String, HalsteadMetrics> calculateAll(CompilationUnit cu, String filePath) {
        Map<String, HalsteadMetrics> metricsMap = new HashMap<>();

        cu.accept(new VoidVisitorAdapter<Void>() {
            @Override
            public void visit(MethodDeclaration md, Void arg) {
                super.visit(md, arg);

                String methodName = md.getDeclarationAsString(false, false, false);
                int line = md.getBegin().map(pos -> pos.line).orElse(0);

                HalsteadMetrics metrics = calculateForMethod(md, filePath, line);
                metricsMap.put(methodName, metrics);
            }
        }, null);

        return metricsMap;
    }

    /**
     * Calculates Halstead metrics for a single method.
     */
    public HalsteadMetrics calculateForMethod(MethodDeclaration md, String filePath, int line) {
        OperatorOperandCollector collector = new OperatorOperandCollector();
        md.accept(collector, null);

        int n1 = collector.distinctOperators.size();
        int n2 = collector.distinctOperands.size();
        int N1 = collector.totalOperators;
        int N2 = collector.totalOperands;

        return new HalsteadMetrics(
            md.getNameAsString(),
            filePath,
            line,
            n1, n2, N1, N2
        );
    }

    /**
     * Visitor to collect operators and operands from AST.
     */
    private static class OperatorOperandCollector extends VoidVisitorAdapter<Void> {
        final Set<String> distinctOperators = new HashSet<>();
        final Set<String> distinctOperands = new HashSet<>();
        int totalOperators = 0;
        int totalOperands = 0;

        // Operators

        @Override
        public void visit(BinaryExpr n, Void arg) {
            super.visit(n, arg);
            addOperator(n.getOperator().asString());
        }

        @Override
        public void visit(UnaryExpr n, Void arg) {
            super.visit(n, arg);
            addOperator(n.getOperator().asString());
        }

        @Override
        public void visit(AssignExpr n, Void arg) {
            super.visit(n, arg);
            addOperator(n.getOperator().asString());
        }

        @Override
        public void visit(MethodCallExpr n, Void arg) {
            super.visit(n, arg);
            addOperator("()");  // Method call operator
            addOperand(n.getNameAsString());  // Method name as operand
        }

        @Override
        public void visit(ObjectCreationExpr n, Void arg) {
            super.visit(n, arg);
            addOperator("new");
            addOperand(n.getType().getNameAsString());
        }

        @Override
        public void visit(IfStmt n, Void arg) {
            super.visit(n, arg);
            addOperator("if");
        }

        @Override
        public void visit(WhileStmt n, Void arg) {
            super.visit(n, arg);
            addOperator("while");
        }

        @Override
        public void visit(ForStmt n, Void arg) {
            super.visit(n, arg);
            addOperator("for");
        }

        @Override
        public void visit(ForEachStmt n, Void arg) {
            super.visit(n, arg);
            addOperator("foreach");
        }

        @Override
        public void visit(DoStmt n, Void arg) {
            super.visit(n, arg);
            addOperator("do");
        }

        @Override
        public void visit(SwitchStmt n, Void arg) {
            super.visit(n, arg);
            addOperator("switch");
        }

        @Override
        public void visit(SwitchEntry n, Void arg) {
            super.visit(n, arg);
            addOperator("case");
        }

        @Override
        public void visit(ReturnStmt n, Void arg) {
            super.visit(n, arg);
            addOperator("return");
        }

        @Override
        public void visit(ThrowStmt n, Void arg) {
            super.visit(n, arg);
            addOperator("throw");
        }

        @Override
        public void visit(TryStmt n, Void arg) {
            super.visit(n, arg);
            addOperator("try");
        }

        @Override
        public void visit(CatchClause n, Void arg) {
            super.visit(n, arg);
            addOperator("catch");
        }

        @Override
        public void visit(ConditionalExpr n, Void arg) {
            super.visit(n, arg);
            addOperator("?:");
        }

        @Override
        public void visit(ArrayAccessExpr n, Void arg) {
            super.visit(n, arg);
            addOperator("[]");
        }

        @Override
        public void visit(FieldAccessExpr n, Void arg) {
            super.visit(n, arg);
            addOperator(".");
            addOperand(n.getNameAsString());
        }

        @Override
        public void visit(CastExpr n, Void arg) {
            super.visit(n, arg);
            addOperator("cast");
        }

        @Override
        public void visit(InstanceOfExpr n, Void arg) {
            super.visit(n, arg);
            addOperator("instanceof");
        }

        // Operands

        @Override
        public void visit(NameExpr n, Void arg) {
            super.visit(n, arg);
            addOperand(n.getNameAsString());
        }

        @Override
        public void visit(IntegerLiteralExpr n, Void arg) {
            super.visit(n, arg);
            addOperand(n.getValue());
        }

        @Override
        public void visit(LongLiteralExpr n, Void arg) {
            super.visit(n, arg);
            addOperand(n.getValue());
        }

        @Override
        public void visit(DoubleLiteralExpr n, Void arg) {
            super.visit(n, arg);
            addOperand(n.getValue());
        }

        @Override
        public void visit(StringLiteralExpr n, Void arg) {
            super.visit(n, arg);
            addOperand("\"" + n.getValue() + "\"");
        }

        @Override
        public void visit(BooleanLiteralExpr n, Void arg) {
            super.visit(n, arg);
            addOperand(String.valueOf(n.getValue()));
        }

        @Override
        public void visit(CharLiteralExpr n, Void arg) {
            super.visit(n, arg);
            addOperand("'" + n.getValue() + "'");
        }

        @Override
        public void visit(NullLiteralExpr n, Void arg) {
            super.visit(n, arg);
            addOperand("null");
        }

        @Override
        public void visit(ThisExpr n, Void arg) {
            super.visit(n, arg);
            addOperand("this");
        }

        @Override
        public void visit(SuperExpr n, Void arg) {
            super.visit(n, arg);
            addOperand("super");
        }

        private void addOperator(String operator) {
            distinctOperators.add(operator);
            totalOperators++;
        }

        private void addOperand(String operand) {
            distinctOperands.add(operand);
            totalOperands++;
        }
    }
}

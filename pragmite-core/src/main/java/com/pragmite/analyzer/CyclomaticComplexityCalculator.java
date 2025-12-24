package com.pragmite.analyzer;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.ConditionalExpr;
import com.github.javaparser.ast.stmt.*;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

/**
 * McCabe Cyclomatic Complexity hesaplayıcı.
 * CC = E - N + 2P formülü yerine karar noktalarını sayar.
 * Her if, while, for, case, catch, && ve || operatörü +1 ekler.
 */
public class CyclomaticComplexityCalculator {

    public static int calculate(MethodDeclaration method) {
        int[] complexity = {1}; // Başlangıç değeri 1

        method.accept(new VoidVisitorAdapter<Void>() {
            @Override
            public void visit(IfStmt n, Void arg) {
                super.visit(n, arg);
                complexity[0]++;
            }

            @Override
            public void visit(WhileStmt n, Void arg) {
                super.visit(n, arg);
                complexity[0]++;
            }

            @Override
            public void visit(ForStmt n, Void arg) {
                super.visit(n, arg);
                complexity[0]++;
            }

            @Override
            public void visit(ForEachStmt n, Void arg) {
                super.visit(n, arg);
                complexity[0]++;
            }

            @Override
            public void visit(SwitchEntry n, Void arg) {
                super.visit(n, arg);
                // Default case hariç her case +1
                if (!n.getLabels().isEmpty()) {
                    complexity[0]++;
                }
            }

            @Override
            public void visit(CatchClause n, Void arg) {
                super.visit(n, arg);
                complexity[0]++;
            }

            @Override
            public void visit(ConditionalExpr n, Void arg) {
                super.visit(n, arg);
                complexity[0]++; // Ternary operator: condition ? a : b
            }

            @Override
            public void visit(BinaryExpr n, Void arg) {
                super.visit(n, arg);
                // && ve || operatörleri
                if (n.getOperator() == BinaryExpr.Operator.AND ||
                    n.getOperator() == BinaryExpr.Operator.OR) {
                    complexity[0]++;
                }
            }
        }, null);

        return complexity[0];
    }
}

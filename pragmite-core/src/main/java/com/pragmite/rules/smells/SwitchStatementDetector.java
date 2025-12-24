package com.pragmite.rules.smells;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.stmt.SwitchEntry;
import com.github.javaparser.ast.stmt.SwitchStmt;
import com.pragmite.model.CodeSmell;
import com.pragmite.model.CodeSmellType;

import java.util.ArrayList;
import java.util.List;

/**
 * Detects problematic switch statements that should be replaced with polymorphism.
 * Large switch statements often indicate missing abstraction and violate Open/Closed Principle.
 */
public class SwitchStatementDetector implements SmellDetector {

    private static final int MAX_CASES_THRESHOLD = 5;

    @Override
    public List<CodeSmell> detect(CompilationUnit cu, String filePath, String content) {
        List<CodeSmell> smells = new ArrayList<>();

        cu.findAll(SwitchStmt.class).forEach(switchStmt -> {
            List<SwitchEntry> entries = switchStmt.getEntries();
            int caseCount = (int) entries.stream()
                .filter(entry -> !entry.getLabels().isEmpty())
                .count();

            if (caseCount > MAX_CASES_THRESHOLD) {
                CodeSmell smell = new CodeSmell(
                    CodeSmellType.SWITCH_STATEMENT,
                    filePath,
                    switchStmt.getBegin().get().line,
                    "Switch statement with " + caseCount + " cases. " +
                        "Consider using polymorphism or strategy pattern."
                );
                smell.withSuggestion("Replace switch with polymorphism: create subclasses or use Strategy pattern")
                    .withAutoFix(false);
                smells.add(smell);
            }

            // Check for duplicated logic across cases
            if (hasDuplicatedLogic(entries)) {
                CodeSmell smell = new CodeSmell(
                    CodeSmellType.SWITCH_STATEMENT,
                    filePath,
                    switchStmt.getBegin().get().line,
                    "Switch statement contains duplicated logic across cases"
                );
                smell.withSuggestion("Extract common logic to a separate method")
                    .withAutoFix(false);
                smells.add(smell);
            }
        });

        return smells;
    }

    /**
     * Heuristic to detect if switch cases have duplicated logic.
     */
    private boolean hasDuplicatedLogic(List<SwitchEntry> entries) {
        if (entries.size() < 3) return false;

        // Simple heuristic: if many cases have the same number of statements,
        // there might be duplication
        List<Integer> statementCounts = new ArrayList<>();
        for (SwitchEntry entry : entries) {
            if (!entry.getLabels().isEmpty()) {
                statementCounts.add(entry.getStatements().size());
            }
        }

        if (statementCounts.size() < 2) return false;

        // Count how many cases have the same statement count
        int mode = findMode(statementCounts);
        long count = statementCounts.stream().filter(c -> c == mode).count();

        // If more than half of cases have the same statement count, flag as suspicious
        return count > statementCounts.size() / 2 && mode > 1;
    }

    private int findMode(List<Integer> list) {
        return list.stream()
            .max((a, b) -> Integer.compare(
                (int) list.stream().filter(x -> x.equals(a)).count(),
                (int) list.stream().filter(x -> x.equals(b)).count()
            ))
            .orElse(0);
    }
}

package com.pragmite.refactor.strategies;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.SwitchStmt;
import com.pragmite.model.CodeSmell;
import com.pragmite.model.CodeSmellType;
import com.pragmite.refactor.RefactoringStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Suggests replacing complex conditional logic with polymorphism.
 * Recommends using Strategy or State pattern instead of switch/if-else chains.
 */
public class ReplaceConditionalWithPolymorphismStrategy implements RefactoringStrategy {

    private static final Logger logger = LoggerFactory.getLogger(ReplaceConditionalWithPolymorphismStrategy.class);
    private static final int MIN_SWITCH_CASES = 4;
    private static final int MIN_IF_ELSE_CHAIN = 3;

    @Override
    public String getName() {
        return "Replace Conditional with Polymorphism";
    }

    @Override
    public boolean canHandle(CodeSmell smell) {
        return smell.getType() == CodeSmellType.SWITCH_STATEMENT ||
               smell.getType() == CodeSmellType.HIGH_CYCLOMATIC_COMPLEXITY;
    }

    @Override
    public boolean validate(CodeSmell smell) throws Exception {
        Path filePath = Paths.get(smell.getFilePath());

        if (!Files.exists(filePath)) {
            logger.warn("File does not exist: {}", filePath);
            return false;
        }

        return true;
    }

    @Override
    public String apply(CodeSmell smell) throws Exception {
        Path filePath = Paths.get(smell.getFilePath());
        String sourceCode = Files.readString(filePath);
        CompilationUnit cu = com.github.javaparser.StaticJavaParser.parse(sourceCode);

        StringBuilder report = new StringBuilder();
        int complexConditionals = 0;

        // Analyze switch statements
        for (SwitchStmt switchStmt : cu.findAll(SwitchStmt.class)) {
            int caseCount = (int) switchStmt.getEntries().stream()
                .filter(entry -> !entry.getLabels().isEmpty())
                .count();

            if (caseCount >= MIN_SWITCH_CASES) {
                complexConditionals++;
                report.append(String.format("Switch with %d cases at line %d:%n",
                                          caseCount, switchStmt.getBegin().get().line));
                report.append(generatePolymorphismSuggestion());
                report.append("\n");
            }
        }

        // Analyze if-else chains
        for (MethodDeclaration method : cu.findAll(MethodDeclaration.class)) {
            for (IfStmt ifStmt : method.findAll(IfStmt.class)) {
                int chainLength = calculateIfElseChainLength(ifStmt);
                if (chainLength >= MIN_IF_ELSE_CHAIN) {
                    complexConditionals++;
                    report.append(String.format("If-else chain of length %d in method '%s' at line %d%n",
                                              chainLength, method.getNameAsString(),
                                              ifStmt.getBegin().get().line));
                }
            }
        }

        if (complexConditionals > 0) {
            logger.info("Found {} complex conditional(s) in {}", complexConditionals, filePath.getFileName());
            return String.format("Found %d complex conditional structure(s) that could use polymorphism:%n%s",
                               complexConditionals, report.toString());
        } else {
            return "No complex conditionals found";
        }
    }

    private String generatePolymorphismSuggestion() {
        return "  Suggested approach:\n" +
               "  1. Create an interface or abstract class for the behavior\n" +
               "  2. Create concrete implementations for each case\n" +
               "  3. Use a factory or map to select the appropriate implementation\n" +
               "  4. Replace switch/if-else with polymorphic method call\n" +
               "  Example:\n" +
               "    interface PaymentStrategy { void pay(double amount); }\n" +
               "    class CreditCardPayment implements PaymentStrategy { ... }\n" +
               "    class PayPalPayment implements PaymentStrategy { ... }\n";
    }

    private int calculateIfElseChainLength(IfStmt ifStmt) {
        int length = 1;
        IfStmt current = ifStmt;

        while (current.getElseStmt().isPresent()) {
            length++;
            if (current.getElseStmt().get() instanceof IfStmt) {
                current = (IfStmt) current.getElseStmt().get();
            } else {
                break;
            }
        }

        return length;
    }

    @Override
    public String getDescription() {
        return "Replace Conditional with Polymorphism: Suggests using Strategy/State pattern instead of complex conditionals";
    }
}

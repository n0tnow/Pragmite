package com.pragmite.rules.smells;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.pragmite.model.CodeSmell;
import com.pragmite.model.CodeSmellType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FeatureEnvyDetectorTest {

    private FeatureEnvyDetector detector;

    @BeforeEach
    void setUp() {
        detector = new FeatureEnvyDetector();
    }

    @Test
    void shouldDetectFeatureEnvy() {
        String code = """
            public class Customer {
                private Account account;

                public void processAccount() {
                    account.getBalance();
                    account.getInterestRate();
                    account.calculateInterest();
                    account.getAccountNumber();
                    account.updateBalance();
                    account.setInterestRate(0.05);
                }
            }
            """;

        CompilationUnit cu = StaticJavaParser.parse(code);
        List<CodeSmell> smells = detector.detect(cu, "Customer.java", code);

        assertTrue(smells.size() > 0, "Should detect feature envy");
        assertTrue(smells.stream().anyMatch(s ->
            s.getType() == CodeSmellType.FEATURE_ENVY &&
            s.getMessage().contains("account")));
    }

    @Test
    void shouldNotDetectNormalDelegation() {
        String code = """
            public class OrderProcessor {
                private Order order;

                public void process() {
                    validate();
                    calculate();
                    order.execute();
                }

                private void validate() {
                    // Own logic
                }

                private void calculate() {
                    // Own logic
                }
            }
            """;

        CompilationUnit cu = StaticJavaParser.parse(code);
        List<CodeSmell> smells = detector.detect(cu, "OrderProcessor.java", code);

        assertEquals(0, smells.size(), "Should not detect normal delegation");
    }

    @Test
    void shouldDetectMultipleFieldEnvy() {
        String code = """
            public class ReportGenerator {
                private DataSource source;

                public void generateReport() {
                    source.getData();
                    source.filter();
                    source.sort();
                    source.aggregate();
                    source.format();
                    source.export();
                    source.validate();
                }
            }
            """;

        CompilationUnit cu = StaticJavaParser.parse(code);
        List<CodeSmell> smells = detector.detect(cu, "ReportGenerator.java", code);

        assertTrue(smells.size() > 0, "Should detect excessive dependency on another object");
    }
}

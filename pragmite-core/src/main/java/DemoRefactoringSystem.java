import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.pragmite.model.CodeSmell;
import com.pragmite.model.CodeSmellType;
import com.pragmite.refactoring.RefactoringManager;
import com.pragmite.refactoring.RefactoringSuggestion;

import java.util.Optional;

/**
 * Demonstrates the refactoring suggestion and auto-fix system.
 * Shows how Pragmite can detect code smells and provide actionable fix suggestions.
 */
public class DemoRefactoringSystem {

    public static void main(String[] args) {
        System.out.println("═══════════════════════════════════════════════════════════════");
        System.out.println("  PRAGMITE REFACTORING SYSTEM DEMO");
        System.out.println("  Detecting code smells and providing actionable fix suggestions");
        System.out.println("═══════════════════════════════════════════════════════════════");
        System.out.println();

        RefactoringManager manager = new RefactoringManager();

        // Demo 1: Field Injection (Auto-fixable)
        demoFieldInjection(manager);

        // Demo 2: Magic Number (Auto-fixable)
        demoMagicNumber(manager);

        // Demo 3: God Class (Manual refactoring)
        demoGodClass(manager);

        // Demo 4: Long Method (Manual refactoring)
        demoLongMethod(manager);

        // Demo 5: Duplicate Code (Manual refactoring)
        demoDuplicateCode(manager);

        // System statistics
        System.out.println("\n" + "═".repeat(70));
        System.out.println("SYSTEM STATISTICS");
        System.out.println("═".repeat(70));
        var stats = manager.getStats();
        System.out.println(stats);
        System.out.println("\nSupported smell types:");
        manager.getSupportedSmellTypes().forEach(type ->
            System.out.println("  • " + type));
        System.out.println("\n" + "═".repeat(70));
    }

    private static void demoFieldInjection(RefactoringManager manager) {
        String sampleCode = """
                package com.example.service;
                import org.springframework.beans.factory.annotation.Autowired;

                public class UserService {
                    @Autowired
                    private UserRepository userRepository;

                    @Autowired
                    private EmailService emailService;

                    public void createUser(User user) {
                        userRepository.save(user);
                        emailService.sendWelcomeEmail(user);
                    }
                }
                """;

        CompilationUnit cu = new JavaParser().parse(sampleCode).getResult().orElseThrow();
        CodeSmell smell = new CodeSmell(
                CodeSmellType.FIELD_INJECTION,
                "UserService.java",
                5,
                "Field injection anti-pattern detected"
        );

        Optional<RefactoringSuggestion> suggestion = manager.getSuggestion(smell, cu);
        suggestion.ifPresent(s -> {
            System.out.println(s.formatAsText());
            System.out.println("✓ AUTO-FIX AVAILABLE: This smell can be automatically fixed!");
            System.out.println();
        });
    }

    private static void demoMagicNumber(RefactoringManager manager) {
        String sampleCode = """
                public class DiscountCalculator {
                    public double calculateDiscount(double price, int quantity) {
                        double discount = 0;
                        if (quantity > 100) {
                            discount = price * 0.25;
                        } else if (quantity > 50) {
                            discount = price * 0.15;
                        } else {
                            discount = price * 0.10;
                        }
                        return discount;
                    }
                }
                """;

        CompilationUnit cu = new JavaParser().parse(sampleCode).getResult().orElseThrow();
        CodeSmell smell = new CodeSmell(
                CodeSmellType.MAGIC_NUMBER,
                "DiscountCalculator.java",
                5,
                "Magic numbers detected in discount calculation"
        );

        Optional<RefactoringSuggestion> suggestion = manager.getSuggestion(smell, cu);
        suggestion.ifPresent(s -> {
            System.out.println(s.formatAsText());
            System.out.println("✓ AUTO-FIX AVAILABLE: Magic numbers can be extracted to constants!");
            System.out.println();
        });
    }

    private static void demoGodClass(RefactoringManager manager) {
        String sampleCode = """
                public class OrderService {
                    // 50 methods, 30 fields, 500+ lines
                    // Handles: orders, payments, inventory, emails, notifications, logging...
                }
                """;

        CompilationUnit cu = new JavaParser().parse(sampleCode).getResult().orElseThrow();
        CodeSmell smell = new CodeSmell(
                CodeSmellType.GOD_CLASS,
                "OrderService.java",
                1,
                "God class detected - too many responsibilities"
        );

        Optional<RefactoringSuggestion> suggestion = manager.getSuggestion(smell, cu);
        suggestion.ifPresent(s -> {
            System.out.println(s.formatAsText());
            System.out.println("⚠ MANUAL REFACTORING REQUIRED: This requires architectural decisions");
            System.out.println();
        });
    }

    private static void demoLongMethod(RefactoringManager manager) {
        String sampleCode = """
                public class OrderProcessor {
                    public void processOrder(Order order) {
                        // 150 lines of code including:
                        // - validation logic
                        // - price calculation
                        // - inventory checks
                        // - payment processing
                        // - order creation
                        // - notification sending
                    }
                }
                """;

        CompilationUnit cu = new JavaParser().parse(sampleCode).getResult().orElseThrow();
        CodeSmell smell = new CodeSmell(
                CodeSmellType.LONG_METHOD,
                "OrderProcessor.java",
                3,
                "Method is too long (150 lines)"
        );

        Optional<RefactoringSuggestion> suggestion = manager.getSuggestion(smell, cu);
        suggestion.ifPresent(s -> {
            System.out.println(s.formatAsText());
            System.out.println("⚠ MANUAL REFACTORING REQUIRED: Extract logical sections to methods");
            System.out.println();
        });
    }

    private static void demoDuplicateCode(RefactoringManager manager) {
        String sampleCode = """
                public class UserMapper {
                    public UserDto toDto1(User user) {
                        UserDto dto = new UserDto();
                        dto.setId(user.getId());
                        dto.setName(user.getName());
                        dto.setEmail(user.getEmail());
                        return dto;
                    }

                    public UserDto toDto2(User user) {
                        UserDto dto = new UserDto();
                        dto.setId(user.getId());
                        dto.setName(user.getName());
                        dto.setEmail(user.getEmail());
                        return dto;
                    }
                }
                """;

        CompilationUnit cu = new JavaParser().parse(sampleCode).getResult().orElseThrow();
        CodeSmell smell = new CodeSmell(
                CodeSmellType.DUPLICATED_CODE,
                "UserMapper.java",
                3,
                "Duplicate mapping logic detected"
        );

        Optional<RefactoringSuggestion> suggestion = manager.getSuggestion(smell, cu);
        suggestion.ifPresent(s -> {
            System.out.println(s.formatAsText());
            System.out.println("⚠ MANUAL REFACTORING REQUIRED: Extract common logic to shared method");
            System.out.println();
        });
    }
}

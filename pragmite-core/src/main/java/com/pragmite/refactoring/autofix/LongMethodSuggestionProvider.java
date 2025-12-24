package com.pragmite.refactoring.autofix;

import com.github.javaparser.ast.CompilationUnit;
import com.pragmite.model.CodeSmell;
import com.pragmite.refactoring.AutoRefactorer;
import com.pragmite.refactoring.RefactoringSuggestion;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Suggestion provider for Long Method code smell.
 * Provides guidance on breaking down long methods into smaller, focused ones.
 */
public class LongMethodSuggestionProvider implements AutoRefactorer {

    @Override
    public boolean canAutoFix(CodeSmell smell) {
        // Long method decomposition requires understanding of business logic
        return false;
    }

    @Override
    public Optional<CompilationUnit> generateFixedCode(CodeSmell smell, CompilationUnit originalCu) {
        return Optional.empty();
    }

    @Override
    public RefactoringResult applyFix(CodeSmell smell, Path filePath, CompilationUnit originalCu) {
        return new RefactoringResult(false,
                "Long method requires manual decomposition. See suggestion for guidance.",
                originalCu.toString(), "", List.of());
    }

    @Override
    public RefactoringSuggestion getSuggestion(CodeSmell smell, CompilationUnit originalCu) {
        String beforeCode = "public Order processOrder(OrderRequest request) {\n" +
                "    // Validation (20 lines)\n" +
                "    if (request.getItems().isEmpty()) {\n" +
                "        throw new IllegalArgumentException(\"Empty order\");\n" +
                "    }\n" +
                "    for (Item item : request.getItems()) {\n" +
                "        if (item.getQuantity() <= 0) {\n" +
                "            throw new IllegalArgumentException(\"Invalid quantity\");\n" +
                "        }\n" +
                "    }\n\n" +
                "    // Price calculation (30 lines)\n" +
                "    double total = 0;\n" +
                "    for (Item item : request.getItems()) {\n" +
                "        double price = item.getPrice();\n" +
                "        if (item.getQuantity() > 100) {\n" +
                "            price *= 0.9; // bulk discount\n" +
                "        }\n" +
                "        total += price * item.getQuantity();\n" +
                "    }\n\n" +
                "    // Inventory check (25 lines)\n" +
                "    for (Item item : request.getItems()) {\n" +
                "        int available = inventoryService.getStock(item.getId());\n" +
                "        if (available < item.getQuantity()) {\n" +
                "            throw new InsufficientStockException();\n" +
                "        }\n" +
                "    }\n\n" +
                "    // Payment processing (30 lines)\n" +
                "    PaymentResult payment = paymentGateway.process(...);\n" +
                "    if (!payment.isSuccess()) {\n" +
                "        throw new PaymentException();\n" +
                "    }\n\n" +
                "    // Order creation (20 lines)\n" +
                "    Order order = new Order();\n" +
                "    order.setCustomerId(request.getCustomerId());\n" +
                "    order.setTotal(total);\n" +
                "    // ... many more setters\n" +
                "    return orderRepository.save(order);\n" +
                "}";

        String afterCode = "public Order processOrder(OrderRequest request) {\n" +
                "    validateOrder(request);\n" +
                "    double total = calculateTotal(request.getItems());\n" +
                "    checkInventory(request.getItems());\n" +
                "    processPayment(request.getCustomerId(), total);\n" +
                "    return createOrder(request, total);\n" +
                "}\n\n" +
                "private void validateOrder(OrderRequest request) {\n" +
                "    if (request.getItems().isEmpty()) {\n" +
                "        throw new IllegalArgumentException(\"Empty order\");\n" +
                "    }\n" +
                "    request.getItems().forEach(this::validateItem);\n" +
                "}\n\n" +
                "private void validateItem(Item item) {\n" +
                "    if (item.getQuantity() <= 0) {\n" +
                "        throw new IllegalArgumentException(\"Invalid quantity\");\n" +
                "    }\n" +
                "}\n\n" +
                "private double calculateTotal(List<Item> items) {\n" +
                "    return items.stream()\n" +
                "        .mapToDouble(this::calculateItemPrice)\n" +
                "        .sum();\n" +
                "}\n\n" +
                "// ... other extracted methods";

        return new RefactoringSuggestion.Builder()
                .title("Extract Long Method into Smaller Methods")
                .description("Long methods are hard to understand, test, and maintain. Break them down into smaller methods, each doing one thing well.")
                .difficulty(RefactoringSuggestion.Difficulty.MEDIUM)
                .addStep("Read through the method and identify logical sections")
                .addStep("Look for code blocks separated by blank lines or comments")
                .addStep("Each section likely represents a separate concern")
                .addStep("Extract each section into a private method with a descriptive name")
                .addStep("The method name should describe what it does, not how")
                .addStep("Pass only necessary parameters to extracted methods")
                .addStep("Aim for methods with 10-20 lines maximum")
                .addStep("The original method should read like a high-level algorithm")
                .beforeCode(beforeCode)
                .afterCode(afterCode)
                .autoFixAvailable(false)
                .relatedSmell(smell)
                .build();
    }

    @Override
    public List<String> getSupportedSmellTypes() {
        return Arrays.asList("Long Method");
    }
}

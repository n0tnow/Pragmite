package com.pragmite.interactive;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;

/**
 * Manual test for InteractiveApprovalManager
 *
 * Run this manually to see the interactive diff preview in action.
 * This cannot be automated as it requires user input.
 */
public class InteractiveApprovalManagerTest {

    @Test
    @Disabled("Manual test only - requires user interaction")
    public void testInteractiveDiffPreview() {
        String beforeCode = """
            public class UserService {
                private UserRepository repository;
                private EmailService emailService;

                public void saveUser(User user) {
                    // Validate user
                    if (user == null) {
                        throw new IllegalArgumentException("User cannot be null");
                    }

                    // Save to database
                    repository.save(user);

                    // Send welcome email
                    emailService.sendWelcomeEmail(user.getEmail());

                    // Log the action
                    System.out.println("User saved: " + user.getId());
                }
            }
            """;

        String afterCode = """
            public class UserService {
                private UserRepository repository;
                private EmailService emailService;

                public void saveUser(User user) {
                    validateUser(user);
                    repository.save(user);
                    emailService.sendWelcomeEmail(user.getEmail());
                    logUserSaved(user);
                }

                private void validateUser(User user) {
                    if (user == null) {
                        throw new IllegalArgumentException("User cannot be null");
                    }
                }

                private void logUserSaved(User user) {
                    System.out.println("User saved: " + user.getId());
                }
            }
            """;

        InteractiveApprovalManager manager = new InteractiveApprovalManager();

        try {
            InteractiveApprovalManager.Decision decision = manager.askForApproval(
                "UserService.java",
                "Long Method → Extract Method",
                beforeCode,
                afterCode,
                1,
                1
            );

            manager.printSummary(
                decision == InteractiveApprovalManager.Decision.APPLY ? 1 : 0,
                decision == InteractiveApprovalManager.Decision.SKIP ? 1 : 0,
                1
            );

            System.out.println("\nDecision: " + decision);

        } finally {
            manager.close();
        }
    }

    public static void main(String[] args) {
        // Manual test runner
        InteractiveApprovalManagerTest test = new InteractiveApprovalManagerTest();
        test.testInteractiveDiffPreview();
    }
}

package com.pragmite.refactor;

import com.pragmite.model.CodeSmell;
import com.pragmite.model.CodeSmellType;
import com.pragmite.model.Severity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RefactoringEngineTest {

    @TempDir
    Path tempDir;

    private RefactoringEngine engine;
    private Path testFile;

    @BeforeEach
    void setUp() throws IOException {
        // Create engine with custom backup root
        engine = new RefactoringEngine();

        // Create a test file
        testFile = tempDir.resolve("Test.java");
        Files.writeString(testFile, "public class Test { }");
    }

    @Test
    void testEngineInitialization() {
        assertNotNull(engine);
        assertNotNull(engine.getBackupManager());
    }

    @Test
    void testRegisterCustomStrategy() {
        TestRefactoringStrategy customStrategy = new TestRefactoringStrategy();
        engine.registerStrategy(customStrategy);

        // Strategy should be registered and usable
        CodeSmell smell = createTestSmell(true);
        RefactoringPlan plan = engine.createPlan(List.of(smell));

        // Plan should include action from custom strategy
        assertEquals(1, plan.size());
    }

    @Test
    void testCreatePlanWithAutoFixableSmells() {
        List<CodeSmell> smells = new ArrayList<>();
        smells.add(createTestSmell(true));  // auto-fixable
        smells.add(createTestSmell(true));  // auto-fixable

        RefactoringPlan plan = engine.createPlan(smells);

        assertEquals(2, plan.size());
    }

    @Test
    void testCreatePlanSkipsNonAutoFixableSmells() {
        List<CodeSmell> smells = new ArrayList<>();
        smells.add(createTestSmell(false)); // NOT auto-fixable
        smells.add(createTestSmell(true));  // auto-fixable

        RefactoringPlan plan = engine.createPlan(smells);

        // Only the auto-fixable smell should be in plan
        assertEquals(1, plan.size());
    }

    @Test
    void testCreatePlanWithNoApplicableStrategy() {
        // Create smell that no strategy can handle
        CodeSmell smell = new CodeSmell(
            CodeSmellType.GOD_CLASS,
            testFile.toString(),
            1,
            "Unknown smell type"
        );
        smell.setAutoFixAvailable(true);

        RefactoringPlan plan = engine.createPlan(List.of(smell));

        // No actions should be added since no strategy handles it
        assertEquals(0, plan.size());
    }

    @Test
    void testExecuteDryRun() throws IOException {
        TestRefactoringStrategy strategy = new TestRefactoringStrategy();
        engine.registerStrategy(strategy);

        CodeSmell smell = createTestSmell(true);
        RefactoringPlan plan = engine.createPlan(List.of(smell));

        RefactoringResult result = engine.execute(plan, true);

        assertTrue(result.isDryRun());
        assertTrue(result.isSuccess());
        assertEquals(1, result.getSuccessCount());
        assertEquals(0, result.getFailureCount());
        assertNull(result.getBackupId()); // No backup in dry-run
    }

    @Test
    void testExecuteWithBackup() throws IOException {
        // Create a simple plan manually to avoid built-in strategy conflicts
        TestRefactoringStrategy strategy = new TestRefactoringStrategy();
        CodeSmell smell = createTestSmell(true);

        RefactoringPlan plan = new RefactoringPlan();
        plan.addAction(new RefactoringAction(smell, strategy));

        RefactoringResult result = engine.execute(plan, false);

        assertFalse(result.isDryRun());
        assertTrue(result.isSuccess());
        assertNotNull(result.getBackupId()); // Backup should be created
    }

    @Test
    void testExecuteEmptyPlan() throws IOException {
        RefactoringPlan emptyPlan = new RefactoringPlan();

        RefactoringResult result = engine.execute(emptyPlan, false);

        assertTrue(result.isSuccess());
        assertEquals(0, result.getSuccessCount());
        assertEquals(0, result.getFailureCount());
    }

    @Test
    void testExecuteWithFailure() throws IOException {
        // Register strategy that always fails
        FailingRefactoringStrategy failStrategy = new FailingRefactoringStrategy();
        engine.registerStrategy(failStrategy);

        CodeSmell smell = createTestSmell(true);
        RefactoringPlan plan = engine.createPlan(List.of(smell));

        RefactoringResult result = engine.execute(plan, false);

        assertFalse(result.isSuccess());
        assertEquals(0, result.getSuccessCount());
        assertEquals(1, result.getFailureCount());
    }

    @Test
    void testExecuteStopsOnFirstFailure() throws IOException {
        // Register failing and succeeding strategies
        FailingRefactoringStrategy failStrategy = new FailingRefactoringStrategy();
        engine.registerStrategy(failStrategy);

        TestRefactoringStrategy successStrategy = new TestRefactoringStrategy();
        engine.registerStrategy(successStrategy);

        CodeSmell smell1 = createTestSmell(true);
        CodeSmell smell2 = createTestSmell(true);

        RefactoringPlan plan = new RefactoringPlan();
        plan.addAction(new RefactoringAction(smell1, failStrategy));
        plan.addAction(new RefactoringAction(smell2, successStrategy));

        RefactoringResult result = engine.execute(plan, false);

        assertFalse(result.isSuccess());
        assertEquals(0, result.getSuccessCount());
        assertEquals(1, result.getFailureCount());

        // Second action should not have been executed due to stop-on-failure
        assertEquals(1, result.getOutcomes().size());
    }

    @Test
    void testExecuteDryRunContinuesAfterFailure() throws IOException {
        // In dry-run mode, should continue after validation failures
        FailingRefactoringStrategy failStrategy = new FailingRefactoringStrategy();
        engine.registerStrategy(failStrategy);

        TestRefactoringStrategy successStrategy = new TestRefactoringStrategy();
        engine.registerStrategy(successStrategy);

        CodeSmell smell1 = createTestSmell(true);
        CodeSmell smell2 = createTestSmell(true);

        RefactoringPlan plan = new RefactoringPlan();
        plan.addAction(new RefactoringAction(smell1, failStrategy));
        plan.addAction(new RefactoringAction(smell2, successStrategy));

        RefactoringResult result = engine.execute(plan, true);

        assertFalse(result.isSuccess());
        assertEquals(1, result.getSuccessCount());
        assertEquals(1, result.getFailureCount());

        // Both actions should have been attempted in dry-run
        assertEquals(2, result.getOutcomes().size());
    }

    @Test
    void testRollback() throws IOException {
        TestRefactoringStrategy strategy = new TestRefactoringStrategy();
        engine.registerStrategy(strategy);

        // Original content
        String originalContent = "public class Original { }";
        Files.writeString(testFile, originalContent);

        CodeSmell smell = createTestSmell(true);
        RefactoringPlan plan = engine.createPlan(List.of(smell));

        // Execute refactoring
        RefactoringResult result = engine.execute(plan, false);
        String backupId = result.getBackupId();

        // Modify file to simulate refactoring
        Files.writeString(testFile, "public class Modified { }");

        // Rollback
        engine.rollback(backupId);

        // File should be restored to original
        String restoredContent = Files.readString(testFile);
        assertEquals(originalContent, restoredContent);
    }

    @Test
    void testResultToString() throws IOException {
        TestRefactoringStrategy strategy = new TestRefactoringStrategy();
        engine.registerStrategy(strategy);

        CodeSmell smell = createTestSmell(true);
        RefactoringPlan plan = engine.createPlan(List.of(smell));

        RefactoringResult result = engine.execute(plan, true);

        String resultString = result.toString();

        assertNotNull(resultString);
        assertTrue(resultString.contains("Refactoring Result"));
        assertTrue(resultString.contains("DRY RUN"));
        assertTrue(resultString.contains("SUCCESS"));
    }

    /**
     * Helper to create a test CodeSmell.
     */
    private CodeSmell createTestSmell(boolean autoFixable) {
        CodeSmell smell = new CodeSmell(
            CodeSmellType.UNUSED_IMPORT,
            testFile.toString(),
            1,
            "Test smell"
        );
        smell.setAutoFixAvailable(autoFixable);
        return smell;
    }

    /**
     * Test refactoring strategy that always succeeds.
     */
    private static class TestRefactoringStrategy implements RefactoringStrategy {
        @Override
        public String getName() {
            return "TestStrategy";
        }

        @Override
        public boolean canHandle(CodeSmell smell) {
            return smell.getType() == CodeSmellType.UNUSED_IMPORT;
        }

        @Override
        public boolean validate(CodeSmell smell) {
            return true;
        }

        @Override
        public String apply(CodeSmell smell) {
            return "Applied test refactoring";
        }

        @Override
        public String getDescription() {
            return "Test refactoring strategy";
        }
    }

    /**
     * Test refactoring strategy that always fails.
     */
    private static class FailingRefactoringStrategy implements RefactoringStrategy {
        @Override
        public String getName() {
            return "FailingStrategy";
        }

        @Override
        public boolean canHandle(CodeSmell smell) {
            return true;
        }

        @Override
        public boolean validate(CodeSmell smell) {
            return false;
        }

        @Override
        public String apply(CodeSmell smell) throws Exception {
            throw new Exception("Simulated failure");
        }

        @Override
        public String getDescription() {
            return "Failing test strategy";
        }
    }
}

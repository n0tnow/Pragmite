package com.pragmite.ai;

import com.pragmite.interactive.InteractiveApprovalManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for AnalysisEngine with InteractiveApprovalManager.
 *
 * Tests that interactive approval mode can be enabled/disabled and integrated.
 *
 * @author Pragmite Team
 * @version 1.6.3 - Integration Sprint Task 2
 * @since 2025-12-28
 */
class AnalysisEngineInteractiveTest {

    private AnalysisEngine engine;

    @BeforeEach
    void setUp() {
        engine = new AnalysisEngine();
    }

    @Test
    @DisplayName("Should enable and disable interactive mode")
    void testEnableDisableInteractiveMode() {
        // Given: AnalysisEngine without interactive mode
        assertFalse(engine.isInteractiveModeEnabled(), "Interactive mode should be disabled by default");
        assertNull(engine.getApprovalManager(), "Approval manager should be null by default");

        // When: Enabling interactive mode
        engine.enableInteractiveMode();

        // Then: Interactive mode should be enabled
        assertTrue(engine.isInteractiveModeEnabled(), "Interactive mode should be enabled");
        assertNotNull(engine.getApprovalManager(), "Approval manager should be initialized");

        // When: Disabling interactive mode
        engine.disableInteractiveMode();

        // Then: Interactive mode should be disabled
        assertFalse(engine.isInteractiveModeEnabled(), "Interactive mode should be disabled");
        assertNotNull(engine.getApprovalManager(), "Approval manager should still exist (not cleaned up)");
    }

    @Test
    @DisplayName("Should create approval manager on first enable")
    void testApprovalManagerCreation() {
        // Given: Fresh engine
        assertNull(engine.getApprovalManager());

        // When: Enabling interactive mode
        engine.enableInteractiveMode();

        // Then: Approval manager should be created
        InteractiveApprovalManager manager = engine.getApprovalManager();
        assertNotNull(manager, "Approval manager should be created");

        // When: Enabling again
        engine.enableInteractiveMode();

        // Then: Should reuse same approval manager
        assertSame(manager, engine.getApprovalManager(), "Should reuse existing approval manager");
    }

    @Test
    @DisplayName("Should remain enabled after multiple enable calls")
    void testMultipleEnableCalls() {
        // When: Enabling multiple times
        engine.enableInteractiveMode();
        engine.enableInteractiveMode();
        engine.enableInteractiveMode();

        // Then: Should remain enabled
        assertTrue(engine.isInteractiveModeEnabled(), "Should remain enabled");
        assertNotNull(engine.getApprovalManager(), "Approval manager should exist");
    }

    @Test
    @DisplayName("Interactive mode should be disabled by default")
    void testDefaultState() {
        // Given: New AnalysisEngine
        AnalysisEngine newEngine = new AnalysisEngine();

        // Then: Interactive mode should be disabled by default
        assertFalse(newEngine.isInteractiveModeEnabled(), "Should be disabled by default");
        assertNull(newEngine.getApprovalManager(), "No approval manager by default");
    }

    @Test
    @DisplayName("Should toggle interactive mode multiple times")
    void testToggleInteractiveMode() {
        // First enable
        engine.enableInteractiveMode();
        assertTrue(engine.isInteractiveModeEnabled());

        // First disable
        engine.disableInteractiveMode();
        assertFalse(engine.isInteractiveModeEnabled());

        // Second enable
        engine.enableInteractiveMode();
        assertTrue(engine.isInteractiveModeEnabled());

        // Second disable
        engine.disableInteractiveMode();
        assertFalse(engine.isInteractiveModeEnabled());
    }

    @Test
    @DisplayName("Approval manager should persist after disable")
    void testApprovalManagerPersistence() {
        // Given: Interactive mode enabled
        engine.enableInteractiveMode();
        InteractiveApprovalManager originalManager = engine.getApprovalManager();
        assertNotNull(originalManager);

        // When: Disabling interactive mode
        engine.disableInteractiveMode();

        // Then: Approval manager should still exist
        assertNotNull(engine.getApprovalManager(), "Approval manager should persist");
        assertSame(originalManager, engine.getApprovalManager(), "Should be same instance");

        // When: Re-enabling
        engine.enableInteractiveMode();

        // Then: Should reuse same manager
        assertSame(originalManager, engine.getApprovalManager(), "Should reuse persisted manager");
    }

    @Test
    @DisplayName("Multiple AnalysisEngine instances should have independent interactive modes")
    void testIndependentInstances() {
        // Given: Two AnalysisEngine instances
        AnalysisEngine engine1 = new AnalysisEngine();
        AnalysisEngine engine2 = new AnalysisEngine();

        // When: Enabling interactive mode on first engine only
        engine1.enableInteractiveMode();

        // Then: Only first engine should have interactive mode enabled
        assertTrue(engine1.isInteractiveModeEnabled(), "Engine 1 should be enabled");
        assertFalse(engine2.isInteractiveModeEnabled(), "Engine 2 should be disabled");
        assertNotNull(engine1.getApprovalManager(), "Engine 1 should have manager");
        assertNull(engine2.getApprovalManager(), "Engine 2 should not have manager");
    }
}

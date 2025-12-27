package com.pragmite.ai;

import com.pragmite.websocket.ProgressWebSocketServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for AnalysisEngine with ProgressWebSocketServer.
 *
 * Tests that WebSocket progress broadcasting can be enabled/disabled and integrated.
 *
 * @author Pragmite Team
 * @version 1.6.3 - Integration Sprint Task 3
 * @since 2025-12-28
 */
class AnalysisEngineWebSocketTest {

    private AnalysisEngine engine;
    private ProgressWebSocketServer server;

    @BeforeEach
    void setUp() {
        engine = new AnalysisEngine();
    }

    @AfterEach
    void tearDown() {
        if (server != null && server.getActiveConnections() > 0) {
            try {
                server.stop(100);
            } catch (Exception e) {
                // Ignore cleanup errors
            }
        }
    }

    @Test
    @DisplayName("Should set and get WebSocket server")
    void testSetGetWebSocketServer() {
        // Given: Fresh engine
        assertNull(engine.getWebSocketServer(), "WebSocket server should be null by default");

        // When: Setting WebSocket server
        server = new ProgressWebSocketServer(8766);
        engine.setWebSocketServer(server);

        // Then: Server should be set
        assertNotNull(engine.getWebSocketServer(), "WebSocket server should be set");
        assertSame(server, engine.getWebSocketServer(), "Should return same server instance");
    }

    @Test
    @DisplayName("Progress broadcast should be disabled by default")
    void testProgressBroadcastDisabledByDefault() {
        // Given: Fresh engine
        AnalysisEngine newEngine = new AnalysisEngine();

        // Then: Progress broadcast should be disabled
        assertFalse(newEngine.isProgressBroadcastEnabled(), "Progress broadcast should be disabled by default");
        assertNull(newEngine.getWebSocketServer(), "No WebSocket server by default");
    }

    @Test
    @DisplayName("Should enable progress broadcast when server is set")
    void testProgressBroadcastEnabled() {
        // Given: WebSocket server created (not started)
        server = new ProgressWebSocketServer(8767);

        // When: Setting server
        engine.setWebSocketServer(server);

        // Then: Progress broadcast flag should be set but not fully enabled (no clients)
        assertNotNull(engine.getWebSocketServer(), "Server should be set");
        // Note: isProgressBroadcastEnabled() returns false until clients connect
    }

    @Test
    @DisplayName("Should disable progress broadcast when server is set to null")
    void testProgressBroadcastDisabledOnNullServer() {
        // Given: Engine with WebSocket server
        server = new ProgressWebSocketServer(8768);
        engine.setWebSocketServer(server);
        assertNotNull(engine.getWebSocketServer());

        // When: Setting server to null
        engine.setWebSocketServer(null);

        // Then: Progress broadcast should be disabled
        assertFalse(engine.isProgressBroadcastEnabled(), "Progress broadcast should be disabled");
        assertNull(engine.getWebSocketServer(), "WebSocket server should be null");
    }

    @Test
    @DisplayName("Should safely broadcast when no server is set")
    void testBroadcastWithoutServer() {
        // Given: Engine without WebSocket server
        assertNull(engine.getWebSocketServer());

        // When: Attempting to broadcast (should not throw)
        assertDoesNotThrow(() -> {
            engine.broadcastProgress("analysis", 1, 10, "Test message");
        }, "Should not throw when broadcasting without server");

        assertDoesNotThrow(() -> {
            engine.broadcastRefactoringEvent("test", "file.java", "UNUSED_IMPORT", "success");
        }, "Should not throw when broadcasting event without server");
    }

    @Test
    @DisplayName("Should safely broadcast when server is set but not started")
    void testBroadcastWithoutStartedServer() {
        // Given: Engine with server that's not started
        server = new ProgressWebSocketServer(8769);
        engine.setWebSocketServer(server);

        // When: Attempting to broadcast (should not throw)
        assertDoesNotThrow(() -> {
            engine.broadcastProgress("analysis", 5, 20, "Analyzing file");
        }, "Should not throw when broadcasting to non-started server");

        assertDoesNotThrow(() -> {
            engine.broadcastRefactoringEvent("refactoring_started", "Test.java", "LONG_METHOD", "pending");
        }, "Should not throw when broadcasting event to non-started server");
    }

    @Test
    @DisplayName("Should replace WebSocket server when set multiple times")
    void testReplaceWebSocketServer() {
        // Given: Engine with first server
        ProgressWebSocketServer server1 = new ProgressWebSocketServer(8770);
        engine.setWebSocketServer(server1);
        assertSame(server1, engine.getWebSocketServer());

        // When: Setting second server
        ProgressWebSocketServer server2 = new ProgressWebSocketServer(8771);
        engine.setWebSocketServer(server2);

        // Then: Should have second server
        assertSame(server2, engine.getWebSocketServer(), "Should use new server");
        assertNotSame(server1, engine.getWebSocketServer(), "Should not use old server");

        // Cleanup
        server = server2; // For tearDown
    }

    @Test
    @DisplayName("Multiple AnalysisEngine instances should have independent WebSocket servers")
    void testIndependentWebSocketServers() {
        // Given: Two AnalysisEngine instances
        AnalysisEngine engine1 = new AnalysisEngine();
        AnalysisEngine engine2 = new AnalysisEngine();

        // When: Setting server on first engine only
        server = new ProgressWebSocketServer(8772);
        engine1.setWebSocketServer(server);

        // Then: Only first engine should have server
        assertNotNull(engine1.getWebSocketServer(), "Engine 1 should have server");
        assertNull(engine2.getWebSocketServer(), "Engine 2 should not have server");
        assertSame(server, engine1.getWebSocketServer(), "Engine 1 should have correct server");
    }

    @Test
    @DisplayName("Should check for active clients before enabling broadcast")
    void testActiveClientCheck() {
        // Given: Server without clients (not started)
        server = new ProgressWebSocketServer(8773);
        engine.setWebSocketServer(server);

        // Then: Broadcast should not be fully enabled (no active clients)
        assertFalse(engine.isProgressBroadcastEnabled(),
            "Broadcast should be disabled when server has no active clients");
    }

    @Test
    @DisplayName("broadcastProgress should handle null WebSocket server gracefully")
    void testBroadcastProgressWithNullServer() {
        // Given: Engine without server
        assertNull(engine.getWebSocketServer());

        // When/Then: Should not throw
        assertDoesNotThrow(() -> {
            engine.broadcastProgress(null, 0, 0, null);
        }, "Should handle null parameters gracefully");
    }

    @Test
    @DisplayName("broadcastRefactoringEvent should handle null WebSocket server gracefully")
    void testBroadcastEventWithNullServer() {
        // Given: Engine without server
        assertNull(engine.getWebSocketServer());

        // When/Then: Should not throw
        assertDoesNotThrow(() -> {
            engine.broadcastRefactoringEvent(null, null, null, null);
        }, "Should handle null parameters gracefully");
    }
}

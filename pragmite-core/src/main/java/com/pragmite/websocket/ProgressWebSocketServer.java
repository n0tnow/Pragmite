package com.pragmite.websocket;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * WebSocket server for broadcasting real-time progress updates
 * during code analysis and refactoring operations.
 *
 * Version: 1.6.2 (Phase 4, Sprint 3, Task 3.1)
 *
 * Features:
 * - Real-time progress broadcasting
 * - Multiple client support
 * - Connection management
 * - Auto-reconnection support
 * - JSON message protocol
 *
 * Usage:
 * <pre>
 * ProgressWebSocketServer server = new ProgressWebSocketServer(8080);
 * server.start();
 * server.broadcast Progress("analysis", 50, 100, "Analyzing UserService.java");
 * server.stop();
 * </pre>
 *
 * @author Pragmite Team
 * @version 1.6.2
 * @since 2025-12-28
 */
public class ProgressWebSocketServer extends WebSocketServer {

    private static final Logger logger = LoggerFactory.getLogger(ProgressWebSocketServer.class);
    private static final Gson gson = new Gson();

    /**
     * Active WebSocket connections
     */
    private final CopyOnWriteArraySet<WebSocket> clients = new CopyOnWriteArraySet<>();

    /**
     * Session metadata for each client
     */
    private final Map<WebSocket, ClientSession> sessions = new ConcurrentHashMap<>();

    /**
     * Default port for WebSocket server
     */
    public static final int DEFAULT_PORT = 8765;

    /**
     * Create WebSocket server with default port
     */
    public ProgressWebSocketServer() {
        this(DEFAULT_PORT);
    }

    /**
     * Create WebSocket server with custom port
     *
     * @param port Port number to listen on
     */
    public ProgressWebSocketServer(int port) {
        super(new InetSocketAddress(port));
        logger.info("ProgressWebSocketServer initialized on port {}", port);
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        clients.add(conn);

        ClientSession session = new ClientSession(
            conn.getRemoteSocketAddress().toString(),
            System.currentTimeMillis()
        );
        sessions.put(conn, session);

        logger.info("New WebSocket connection: {} (Total clients: {})",
            session.clientId, clients.size());

        // Send welcome message
        ProgressMessage welcome = new ProgressMessage(
            "connected",
            "Connected to Pragmite WebSocket Server",
            0, 0, 0
        );
        send(conn, welcome);
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        clients.remove(conn);
        ClientSession session = sessions.remove(conn);

        if (session != null) {
            logger.info("WebSocket connection closed: {} - {} (Total clients: {})",
                session.clientId, reason, clients.size());
        }
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        logger.debug("Received message from {}: {}",
            conn.getRemoteSocketAddress(), message);

        // Handle ping/pong for keep-alive
        if ("ping".equals(message)) {
            send(conn, new ProgressMessage("pong", "pong", 0, 0, 0));
        }
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        String clientId = conn != null ?
            conn.getRemoteSocketAddress().toString() : "unknown";
        logger.error("WebSocket error for client {}: {}", clientId, ex.getMessage(), ex);
    }

    @Override
    public void onStart() {
        logger.info("ProgressWebSocketServer started successfully on port {}", getPort());
        setConnectionLostTimeout(30); // 30 seconds timeout
    }

    /**
     * Broadcast progress update to all connected clients
     *
     * @param stage Current stage name (e.g., "analysis", "refactoring")
     * @param current Current progress value
     * @param total Total progress value
     * @param message Progress message
     */
    public void broadcastProgress(String stage, int current, int total, String message) {
        ProgressMessage progress = new ProgressMessage(
            "progress",
            message,
            stage,
            current,
            total,
            calculatePercentage(current, total)
        );

        broadcast(progress);
    }

    /**
     * Broadcast refactoring event to all connected clients
     *
     * @param type Event type (e.g., "refactoring_started", "refactoring_completed")
     * @param fileName File being refactored
     * @param refactoringType Type of refactoring
     * @param status Status (e.g., "success", "failed", "skipped")
     */
    public void broadcastRefactoringEvent(
        String type,
        String fileName,
        String refactoringType,
        String status
    ) {
        RefactoringEvent event = new RefactoringEvent(
            type,
            fileName,
            refactoringType,
            status,
            System.currentTimeMillis()
        );

        broadcast(event);
    }

    /**
     * Broadcast analysis complete event
     *
     * @param totalRefactorings Total refactorings found
     * @param applied Number of refactorings applied
     * @param failed Number of refactorings failed
     * @param skipped Number of refactorings skipped
     */
    public void broadcastAnalysisComplete(
        int totalRefactorings,
        int applied,
        int failed,
        int skipped
    ) {
        AnalysisCompleteEvent event = new AnalysisCompleteEvent(
            "analysis_complete",
            totalRefactorings,
            applied,
            failed,
            skipped,
            System.currentTimeMillis()
        );

        broadcast(event);
    }

    /**
     * Send message to specific client
     */
    private void send(WebSocket conn, Object message) {
        if (conn != null && conn.isOpen()) {
            String json = gson.toJson(message);
            conn.send(json);
        }
    }

    /**
     * Broadcast message to all connected clients
     */
    private void broadcast(Object message) {
        String json = gson.toJson(message);

        for (WebSocket client : clients) {
            if (client.isOpen()) {
                client.send(json);
            }
        }

        logger.debug("Broadcast message to {} clients: {}", clients.size(), json);
    }

    /**
     * Calculate percentage
     */
    private int calculatePercentage(int current, int total) {
        if (total == 0) return 0;
        return (int) ((current / (double) total) * 100);
    }

    /**
     * Get number of active connections
     */
    public int getActiveConnections() {
        return clients.size();
    }

    /**
     * Check if server has any active clients
     */
    public boolean hasActiveClients() {
        return !clients.isEmpty();
    }

    // ==================== Message Classes ====================

    /**
     * Progress message format
     */
    public static class ProgressMessage {
        private final String type;
        private final String message;
        private final String stage;
        private final int current;
        private final int total;
        private final int percentage;
        private final long timestamp;

        public ProgressMessage(String type, String message, int current, int total, int percentage) {
            this(type, message, "", current, total, percentage);
        }

        public ProgressMessage(String type, String message, String stage,
                             int current, int total, int percentage) {
            this.type = type;
            this.message = message;
            this.stage = stage;
            this.current = current;
            this.total = total;
            this.percentage = percentage;
            this.timestamp = System.currentTimeMillis();
        }
    }

    /**
     * Refactoring event format
     */
    public static class RefactoringEvent {
        private final String type;
        private final String fileName;
        private final String refactoringType;
        private final String status;
        private final long timestamp;

        public RefactoringEvent(String type, String fileName,
                              String refactoringType, String status, long timestamp) {
            this.type = type;
            this.fileName = fileName;
            this.refactoringType = refactoringType;
            this.status = status;
            this.timestamp = timestamp;
        }
    }

    /**
     * Analysis complete event format
     */
    public static class AnalysisCompleteEvent {
        private final String type;
        private final int total;
        private final int applied;
        private final int failed;
        private final int skipped;
        private final long timestamp;

        public AnalysisCompleteEvent(String type, int total, int applied,
                                    int failed, int skipped, long timestamp) {
            this.type = type;
            this.total = total;
            this.applied = applied;
            this.failed = failed;
            this.skipped = skipped;
            this.timestamp = timestamp;
        }
    }

    /**
     * Client session metadata
     */
    private static class ClientSession {
        final String clientId;
        final long connectedAt;

        ClientSession(String clientId, long connectedAt) {
            this.clientId = clientId;
            this.connectedAt = connectedAt;
        }
    }
}

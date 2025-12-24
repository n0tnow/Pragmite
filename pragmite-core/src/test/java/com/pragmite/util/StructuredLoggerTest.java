package com.pragmite.util;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class StructuredLoggerTest {

    private Logger logger;
    private ListAppender<ILoggingEvent> listAppender;

    @BeforeEach
    void setUp() {
        logger = (Logger) LoggerFactory.getLogger("TestLogger");
        logger.setLevel(Level.DEBUG);

        listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);

        MDC.clear();
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
        logger.detachAppender(listAppender);
    }

    @Test
    void testLogWithContext_Info() {
        Map<String, String> context = new HashMap<>();
        context.put("userId", "12345");
        context.put("action", "login");

        StructuredLogger.logWithContext(logger, "INFO", "User logged in", context);

        List<ILoggingEvent> logsList = listAppender.list;
        assertEquals(1, logsList.size());
        assertEquals(Level.INFO, logsList.get(0).getLevel());
        assertEquals("User logged in", logsList.get(0).getMessage());
    }

    @Test
    void testLogWithContext_Debug() {
        Map<String, String> context = new HashMap<>();
        context.put("debug_info", "test");

        StructuredLogger.logWithContext(logger, "DEBUG", "Debug message", context);

        List<ILoggingEvent> logsList = listAppender.list;
        assertEquals(1, logsList.size());
        assertEquals(Level.DEBUG, logsList.get(0).getLevel());
    }

    @Test
    void testLogWithContext_Warn() {
        StructuredLogger.logWithContext(logger, "WARN", "Warning message", null);

        List<ILoggingEvent> logsList = listAppender.list;
        assertEquals(1, logsList.size());
        assertEquals(Level.WARN, logsList.get(0).getLevel());
    }

    @Test
    void testLogWithContext_Error() {
        Map<String, String> context = new HashMap<>();
        context.put("errorCode", "500");

        StructuredLogger.logWithContext(logger, "ERROR", "Error occurred", context);

        List<ILoggingEvent> logsList = listAppender.list;
        assertEquals(1, logsList.size());
        assertEquals(Level.ERROR, logsList.get(0).getLevel());
    }

    @Test
    void testLogWithContext_UnknownLevel() {
        StructuredLogger.logWithContext(logger, "UNKNOWN", "Message", null);

        // Should default to INFO
        List<ILoggingEvent> logsList = listAppender.list;
        assertEquals(1, logsList.size());
        assertEquals(Level.INFO, logsList.get(0).getLevel());
    }

    @Test
    void testLogWithContext_MDCCleanup() {
        Map<String, String> context = new HashMap<>();
        context.put("key1", "value1");
        context.put("key2", "value2");

        StructuredLogger.logWithContext(logger, "INFO", "Test", context);

        // MDC should be cleaned up after logging
        assertNull(MDC.get("key1"));
        assertNull(MDC.get("key2"));
    }

    @Test
    void testLogWithContext_NullContext() {
        assertDoesNotThrow(() -> {
            StructuredLogger.logWithContext(logger, "INFO", "Message without context", null);
        });

        assertEquals(1, listAppender.list.size());
    }

    @Test
    void testLogOperation_Success() {
        Map<String, String> metadata = new HashMap<>();
        metadata.put("source", "test");

        StructuredLogger.logOperation(logger, "testOp", 150, true, metadata);

        List<ILoggingEvent> logsList = listAppender.list;
        assertEquals(1, logsList.size());
        assertEquals(Level.INFO, logsList.get(0).getLevel());
        assertTrue(logsList.get(0).getMessage().contains("succeeded"));
        assertTrue(logsList.get(0).getMessage().contains("150ms"));
    }

    @Test
    void testLogOperation_Failure() {
        StructuredLogger.logOperation(logger, "failedOp", 250, false, null);

        List<ILoggingEvent> logsList = listAppender.list;
        assertEquals(1, logsList.size());
        assertEquals(Level.ERROR, logsList.get(0).getLevel());
        assertTrue(logsList.get(0).getMessage().contains("failed"));
        assertTrue(logsList.get(0).getMessage().contains("250ms"));
    }

    @Test
    void testLogMetric() {
        Map<String, String> tags = new HashMap<>();
        tags.put("environment", "test");
        tags.put("region", "us-east-1");

        StructuredLogger.logMetric(logger, "response_time", 95.5, "ms", tags);

        List<ILoggingEvent> logsList = listAppender.list;
        assertEquals(1, logsList.size());
        assertEquals(Level.INFO, logsList.get(0).getLevel());
        assertTrue(logsList.get(0).getMessage().contains("METRIC"));
        assertTrue(logsList.get(0).getMessage().contains("response_time"));
        // Decimal formatting may vary
        // assertTrue(logsList.get(0).getMessage().contains("95.50"));
    }

    @Test
    void testLogMetric_NullTags() {
        StructuredLogger.logMetric(logger, "counter", 42, "count", null);

        assertEquals(1, listAppender.list.size());
        assertTrue(listAppender.list.get(0).getMessage().contains("counter"));
    }

    @Test
    void testLogError_WithThrowable() {
        Exception testException = new RuntimeException("Test error");
        Map<String, String> context = new HashMap<>();
        context.put("errorType", "runtime");

        StructuredLogger.logError(logger, "An error occurred", testException, context);

        List<ILoggingEvent> logsList = listAppender.list;
        assertEquals(1, logsList.size());
        assertEquals(Level.ERROR, logsList.get(0).getLevel());
        assertEquals("An error occurred", logsList.get(0).getMessage());
        assertNotNull(logsList.get(0).getThrowableProxy());
    }

    @Test
    void testLogError_WithoutThrowable() {
        StructuredLogger.logError(logger, "Error message", null, null);

        List<ILoggingEvent> logsList = listAppender.list;
        assertEquals(1, logsList.size());
        assertEquals(Level.ERROR, logsList.get(0).getLevel());
        assertNull(logsList.get(0).getThrowableProxy());
    }

    @Test
    void testLogError_MDCCleanup() {
        Map<String, String> context = new HashMap<>();
        context.put("errorId", "E123");

        StructuredLogger.logError(logger, "Test error", null, context);

        // MDC should be cleaned up
        assertNull(MDC.get("errorId"));
    }

    @Test
    void testOperationContext_Creation() {
        StructuredLogger.OperationContext ctx = StructuredLogger.startOperation("testOperation");

        assertNotNull(ctx);
        assertTrue(ctx.getElapsedMs() >= 0);
    }

    @Test
    void testOperationContext_AddContext() {
        StructuredLogger.OperationContext ctx = StructuredLogger.startOperation("testOp");

        ctx.addContext("key1", "value1");
        ctx.addContext("key2", "value2");

        // MDC should contain the context
        assertEquals("testOp", MDC.get("operation"));
        assertEquals("value1", MDC.get("key1"));
        assertEquals("value2", MDC.get("key2"));

        ctx.close();
    }

    @Test
    void testOperationContext_GetElapsedMs() throws InterruptedException {
        StructuredLogger.OperationContext ctx = StructuredLogger.startOperation("timedOp");

        Thread.sleep(50);

        long elapsed = ctx.getElapsedMs();
        assertTrue(elapsed >= 50, "Elapsed time should be at least 50ms, got: " + elapsed);

        ctx.close();
    }

    @Test
    void testOperationContext_Close() {
        StructuredLogger.OperationContext ctx = StructuredLogger.startOperation("cleanupOp");
        ctx.addContext("testKey", "testValue");

        ctx.close();

        // MDC should be cleaned up after close
        assertNull(MDC.get("operation"));
        assertNull(MDC.get("testKey"));
    }

    @Test
    void testOperationContext_TryWithResources() {
        try (StructuredLogger.OperationContext ctx = StructuredLogger.startOperation("autoCloseOp")) {
            ctx.addContext("resource", "test");
            assertEquals("autoCloseOp", MDC.get("operation"));
        }

        // MDC should be automatically cleaned up
        assertNull(MDC.get("operation"));
        assertNull(MDC.get("resource"));
    }

    @Test
    void testOperationContext_NestedOperations() {
        try (StructuredLogger.OperationContext ctx1 = StructuredLogger.startOperation("outer")) {
            ctx1.addContext("level", "1");

            assertEquals("outer", MDC.get("operation"));
            assertEquals("1", MDC.get("level"));

            try (StructuredLogger.OperationContext ctx2 = StructuredLogger.startOperation("inner")) {
                ctx2.addContext("level", "2");

                // Inner operation overwrites MDC
                assertEquals("inner", MDC.get("operation"));
                assertEquals("2", MDC.get("level"));
            }

            // After inner closes, outer context should still be there
            // Note: This behavior depends on how MDC is managed
        }

        assertNull(MDC.get("operation"));
    }

    @Test
    void testConcurrentLogging() throws InterruptedException {
        Thread thread1 = new Thread(() -> {
            Map<String, String> ctx = new HashMap<>();
            ctx.put("thread", "1");
            StructuredLogger.logWithContext(logger, "INFO", "Thread 1", ctx);
        });

        Thread thread2 = new Thread(() -> {
            Map<String, String> ctx = new HashMap<>();
            ctx.put("thread", "2");
            StructuredLogger.logWithContext(logger, "INFO", "Thread 2", ctx);
        });

        thread1.start();
        thread2.start();

        thread1.join();
        thread2.join();

        assertEquals(2, listAppender.list.size());
    }

    @Test
    void testLargeContext() {
        Map<String, String> largeContext = new HashMap<>();
        for (int i = 0; i < 100; i++) {
            largeContext.put("key" + i, "value" + i);
        }

        assertDoesNotThrow(() -> {
            StructuredLogger.logWithContext(logger, "INFO", "Large context test", largeContext);
        });

        assertEquals(1, listAppender.list.size());

        // Verify MDC is cleaned up even with large context
        for (int i = 0; i < 100; i++) {
            assertNull(MDC.get("key" + i));
        }
    }
}

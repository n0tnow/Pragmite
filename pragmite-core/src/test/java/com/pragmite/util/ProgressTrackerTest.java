package com.pragmite.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ProgressTrackerTest {

    private TestProgressListener listener;
    private ProgressTracker tracker;

    @BeforeEach
    void setUp() {
        listener = new TestProgressListener();
        tracker = new ProgressTracker("Test Operation", 100, listener);
    }

    @Test
    void testBasicProgress() {
        tracker.start();
        assertTrue(tracker.isStarted());
        assertFalse(tracker.isCompleted());

        tracker.update(50, "Halfway done");
        assertEquals(50, tracker.getCurrent());
        assertEquals(50.0, tracker.getProgressPercentage(), 0.01);

        tracker.complete(true);
        assertTrue(tracker.isCompleted());
        assertEquals(100, tracker.getCurrent());
    }

    @Test
    void testIncrement() {
        tracker.start();

        tracker.increment("First item");
        assertEquals(1, tracker.getCurrent());

        tracker.incrementBy(9, "Ten items processed");
        assertEquals(10, tracker.getCurrent());
        assertEquals(10.0, tracker.getProgressPercentage(), 0.01);
    }

    @Test
    void testETACalculation() throws InterruptedException {
        tracker.start();

        // Process 25% of work
        tracker.update(25, "Quarter done");

        // Wait a bit to ensure elapsed time > 0
        Thread.sleep(50);

        // ETA should be calculable now
        long eta = tracker.getETAMillis();
        assertTrue(eta > 0, "ETA should be positive");

        // ETA should decrease as we make more progress
        tracker.update(50, "Half done");
        Thread.sleep(50);
        long eta2 = tracker.getETAMillis();
        assertTrue(eta2 < eta, "ETA should decrease with progress");
    }

    @Test
    void testProgressListenerCallbacks() {
        tracker.start();
        assertEquals(1, listener.startCalls.size());
        assertEquals("Test Operation", listener.startCalls.get(0));

        tracker.update(30, "Processing");
        assertTrue(listener.progressCalls.size() > 0);

        tracker.complete(true);
        assertEquals(1, listener.completeCalls.size());
        assertTrue(listener.completeCalls.get(0));
    }

    @Test
    void testProgressClamping() {
        tracker.start();

        // Try to set progress beyond total
        tracker.update(150, "Overshoot");

        // Should be clamped to total
        assertEquals(100, tracker.getCurrent());
        assertEquals(100.0, tracker.getProgressPercentage(), 0.01);
    }

    @Test
    void testZeroTotal() {
        ProgressTracker zeroTracker = new ProgressTracker("Zero Total", 0, listener);
        zeroTracker.start();

        assertEquals(0.0, zeroTracker.getProgressPercentage(), 0.01);
        assertEquals(-1, zeroTracker.getETAMillis());
    }

    @Test
    void testElapsedTime() throws InterruptedException {
        tracker.start();

        Thread.sleep(100);

        long elapsed = tracker.getElapsedMillis();
        assertTrue(elapsed >= 100, "Elapsed time should be at least 100ms");
    }

    @Test
    void testChildTracker() {
        tracker.start();

        // Create child tracker representing 50% of parent's work
        ProgressTracker childTracker = tracker.createChildTracker(
            "Child Task",
            50,
            0.5
        );

        childTracker.start();
        childTracker.update(25, "Child halfway");

        // Parent should have some progress from child
        assertTrue(tracker.getCurrent() > 0, "Parent should have progress from child");

        childTracker.complete(true);
    }

    @Test
    void testNoOpListener() {
        ProgressTracker noOpTracker = new ProgressTracker(
            "No-op Test",
            100,
            ProgressListener.NOOP
        );

        // Should not throw exceptions
        noOpTracker.start();
        noOpTracker.update(50, "Test");
        noOpTracker.complete(true);
    }

    @Test
    void testDoubleStart() {
        tracker.start();
        tracker.start(); // Should log warning but not fail

        assertEquals(1, listener.startCalls.size(), "Should only call onStart once");
    }

    @Test
    void testUpdateBeforeStart() {
        tracker.update(50, "Premature update");

        // Should log warning but not update current value
        assertEquals(0, tracker.getCurrent());
        assertFalse(tracker.isStarted());
    }

    @Test
    void testDoubleComplete() {
        tracker.start();
        tracker.complete(true);
        tracker.complete(false); // Second call

        assertEquals(1, listener.completeCalls.size(), "Should only call onComplete once");
    }

    /**
     * Test implementation of ProgressListener.
     */
    private static class TestProgressListener implements ProgressListener {
        final List<String> startCalls = new ArrayList<>();
        final List<String> progressCalls = new ArrayList<>();
        final List<Boolean> completeCalls = new ArrayList<>();

        @Override
        public void onStart(String operationName, long estimatedTotal) {
            startCalls.add(operationName);
        }

        @Override
        public void onProgress(long current, long total, String message) {
            progressCalls.add(message);
        }

        @Override
        public void onComplete(String operationName, boolean success) {
            completeCalls.add(success);
        }
    }
}

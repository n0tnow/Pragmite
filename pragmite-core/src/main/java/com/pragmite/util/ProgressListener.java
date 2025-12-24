package com.pragmite.util;

/**
 * Listener interface for progress updates during long-running operations.
 */
public interface ProgressListener {

    /**
     * Called when progress is updated.
     *
     * @param current Current progress value
     * @param total Total progress value (for percentage calculation)
     * @param message Descriptive message about current operation
     */
    void onProgress(long current, long total, String message);

    /**
     * Called when an operation starts.
     *
     * @param operationName Name of the operation
     * @param estimatedTotal Estimated total units of work
     */
    void onStart(String operationName, long estimatedTotal);

    /**
     * Called when an operation completes.
     *
     * @param operationName Name of the operation
     * @param success Whether the operation succeeded
     */
    void onComplete(String operationName, boolean success);

    /**
     * Called when a subtask starts (for nested operations).
     *
     * @param taskName Name of the subtask
     * @param parentProgress Current progress of parent operation
     */
    default void onSubtaskStart(String taskName, double parentProgress) {
        // Default: no-op
    }

    /**
     * Called when a subtask completes.
     *
     * @param taskName Name of the subtask
     * @param success Whether the subtask succeeded
     */
    default void onSubtaskComplete(String taskName, boolean success) {
        // Default: no-op
    }

    /**
     * No-op implementation for cases where progress tracking is not needed.
     */
    ProgressListener NOOP = new ProgressListener() {
        @Override
        public void onProgress(long current, long total, String message) {
            // No-op
        }

        @Override
        public void onStart(String operationName, long estimatedTotal) {
            // No-op
        }

        @Override
        public void onComplete(String operationName, boolean success) {
            // No-op
        }
    };
}

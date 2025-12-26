package com.pragmite.autofix;

import java.time.Duration;
import java.time.Instant;

/**
 * Metrics for code application operation.
 *
 * Tracks performance and impact of auto-apply.
 *
 * Version: v0.5
 */
public class ApplicationMetrics {
    private final Instant startTime;
    private final Instant endTime;
    private final long linesChanged;
    private final long linesAdded;
    private final long linesRemoved;
    private final int complexityBefore;
    private final int complexityAfter;
    private final boolean compilationSucceeded;

    private ApplicationMetrics(Builder builder) {
        this.startTime = builder.startTime;
        this.endTime = builder.endTime;
        this.linesChanged = builder.linesChanged;
        this.linesAdded = builder.linesAdded;
        this.linesRemoved = builder.linesRemoved;
        this.complexityBefore = builder.complexityBefore;
        this.complexityAfter = builder.complexityAfter;
        this.compilationSucceeded = builder.compilationSucceeded;
    }

    public Instant getStartTime() {
        return startTime;
    }

    public Instant getEndTime() {
        return endTime;
    }

    public Duration getDuration() {
        return Duration.between(startTime, endTime);
    }

    public long getLinesChanged() {
        return linesChanged;
    }

    public long getLinesAdded() {
        return linesAdded;
    }

    public long getLinesRemoved() {
        return linesRemoved;
    }

    public int getComplexityBefore() {
        return complexityBefore;
    }

    public int getComplexityAfter() {
        return complexityAfter;
    }

    public int getComplexityReduction() {
        return complexityBefore - complexityAfter;
    }

    public double getComplexityReductionPercent() {
        if (complexityBefore == 0) return 0.0;
        return ((double) (complexityBefore - complexityAfter) / complexityBefore) * 100.0;
    }

    public boolean isCompilationSucceeded() {
        return compilationSucceeded;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Instant startTime = Instant.now();
        private Instant endTime = Instant.now();
        private long linesChanged = 0;
        private long linesAdded = 0;
        private long linesRemoved = 0;
        private int complexityBefore = 0;
        private int complexityAfter = 0;
        private boolean compilationSucceeded = false;

        public Builder startTime(Instant startTime) {
            this.startTime = startTime;
            return this;
        }

        public Builder endTime(Instant endTime) {
            this.endTime = endTime;
            return this;
        }

        public Builder linesChanged(long linesChanged) {
            this.linesChanged = linesChanged;
            return this;
        }

        public Builder linesAdded(long linesAdded) {
            this.linesAdded = linesAdded;
            return this;
        }

        public Builder linesRemoved(long linesRemoved) {
            this.linesRemoved = linesRemoved;
            return this;
        }

        public Builder complexityBefore(int complexityBefore) {
            this.complexityBefore = complexityBefore;
            return this;
        }

        public Builder complexityAfter(int complexityAfter) {
            this.complexityAfter = complexityAfter;
            return this;
        }

        public Builder compilationSucceeded(boolean compilationSucceeded) {
            this.compilationSucceeded = compilationSucceeded;
            return this;
        }

        public ApplicationMetrics build() {
            return new ApplicationMetrics(this);
        }
    }

    @Override
    public String toString() {
        return String.format(
            "ApplicationMetrics{duration=%dms, lines=%d, complexity=%d→%d (-%d%%), compiled=%s}",
            getDuration().toMillis(),
            linesChanged,
            complexityBefore,
            complexityAfter,
            (int) getComplexityReductionPercent(),
            compilationSucceeded ? "✓" : "✗"
        );
    }
}

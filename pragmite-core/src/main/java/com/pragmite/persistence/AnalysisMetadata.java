package com.pragmite.persistence;

import java.time.LocalDateTime;

/**
 * Lightweight metadata for an analysis result.
 * Used for listing history without loading full analysis data.
 */
public class AnalysisMetadata {
    private LocalDateTime timestamp;
    private String projectPath;
    private int totalFiles;
    private int totalSmells;
    private double qualityScore;
    private String filePath;

    public AnalysisMetadata() {}

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getProjectPath() {
        return projectPath;
    }

    public void setProjectPath(String projectPath) {
        this.projectPath = projectPath;
    }

    public int getTotalFiles() {
        return totalFiles;
    }

    public void setTotalFiles(int totalFiles) {
        this.totalFiles = totalFiles;
    }

    public int getTotalSmells() {
        return totalSmells;
    }

    public void setTotalSmells(int totalSmells) {
        this.totalSmells = totalSmells;
    }

    public double getQualityScore() {
        return qualityScore;
    }

    public void setQualityScore(double qualityScore) {
        this.qualityScore = qualityScore;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    @Override
    public String toString() {
        return String.format("[%s] %s - Files: %d, Smells: %d, Score: %.1f",
            timestamp, projectPath, totalFiles, totalSmells, qualityScore);
    }
}

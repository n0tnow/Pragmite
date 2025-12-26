package com.pragmite.output;

import com.github.difflib.DiffUtils;
import com.github.difflib.patch.AbstractDelta;
import com.github.difflib.patch.Patch;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.pragmite.ai.AIAnalysisResult;
import com.pragmite.ai.RefactoredCode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Enhanced JSON output with diff data for v1.6.0
 * Provides structured diff information for UI consumption
 */
public class EnhancedJsonOutput {

    /**
     * Main container for enhanced JSON output
     */
    public static class EnhancedAnalysisOutput {
        private final String version = "1.6.0";
        private final String timestamp;
        private final ProjectInfo project;
        private final List<RefactoringInfo> refactorings;
        private final Summary summary;

        public EnhancedAnalysisOutput(
            String projectPath,
            int totalFiles,
            List<AIAnalysisResult> results
        ) {
            this.timestamp = Instant.now().toString();
            this.project = new ProjectInfo(projectPath, totalFiles);
            this.refactorings = new ArrayList<>();

            // Convert AI results to refactoring info
            int id = 1;
            for (AIAnalysisResult result : results) {
                if (result.getRefactoredCode() != null && result.getRefactoredCode().isSuccessful()) {
                    refactorings.add(new RefactoringInfo(
                        String.format("ref-%03d", id++),
                        result
                    ));
                }
            }

            this.summary = new Summary(refactorings.size());
        }

        public String getVersion() { return version; }
        public String getTimestamp() { return timestamp; }
        public ProjectInfo getProject() { return project; }
        public List<RefactoringInfo> getRefactorings() { return refactorings; }
        public Summary getSummary() { return summary; }
    }

    /**
     * Project information
     */
    public static class ProjectInfo {
        private final String path;
        private final int totalFiles;

        public ProjectInfo(String path, int totalFiles) {
            this.path = path;
            this.totalFiles = totalFiles;
        }

        public String getPath() { return path; }
        public int getTotalFiles() { return totalFiles; }
    }

    /**
     * Refactoring information with diff data
     */
    public static class RefactoringInfo {
        private final String id;
        private final String type;
        private final String severity;
        private final FileInfo file;
        private final LocationInfo location;
        private final DiffInfo diff;
        private final String status;
        private final String beforeCode;
        private final String afterCode;
        private final String explanation;

        public RefactoringInfo(String id, AIAnalysisResult result) {
            this.id = id;
            this.type = result.getSmellType().toString();
            this.severity = result.getOriginalSmell().getSeverity().toString();

            RefactoredCode refactored = result.getRefactoredCode();
            this.beforeCode = refactored.getOriginalCode();
            this.afterCode = refactored.getRefactoredCode();
            this.explanation = result.getRecommendation();

            // File info
            this.file = new FileInfo(
                result.getFilePath(),
                calculateChecksum(beforeCode),
                calculateChecksum(afterCode)
            );

            // Location info
            this.location = new LocationInfo(
                result.getLineNumber(),
                result.getLineNumber() + countLines(beforeCode) - 1,
                0, 0  // columns not tracked yet
            );

            // Generate diff
            this.diff = generateDiff(beforeCode, afterCode);

            this.status = "pending";
        }

        private String calculateChecksum(String code) {
            try {
                java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
                byte[] hash = md.digest(code.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                StringBuilder sb = new StringBuilder();
                for (byte b : hash) {
                    sb.append(String.format("%02x", b));
                }
                return sb.toString();
            } catch (Exception e) {
                return "unknown";
            }
        }

        private int countLines(String code) {
            return (int) code.lines().count();
        }

        private DiffInfo generateDiff(String before, String after) {
            List<String> beforeLines = Arrays.asList(before.split("\n"));
            List<String> afterLines = Arrays.asList(after.split("\n"));

            try {
                Patch<String> patch = DiffUtils.diff(beforeLines, afterLines);

                // Create simple unified diff format manually
                StringBuilder unifiedDiffBuilder = new StringBuilder();
                unifiedDiffBuilder.append("--- before.java\n");
                unifiedDiffBuilder.append("+++ after.java\n");

                // Extract line-level changes
                List<DiffChange> changes = new ArrayList<>();
                for (AbstractDelta<String> delta : patch.getDeltas()) {
                    switch (delta.getType()) {
                        case DELETE:
                            for (int i = 0; i < delta.getSource().getLines().size(); i++) {
                                changes.add(new DiffChange(
                                    "delete",
                                    delta.getSource().getPosition() + i + 1,
                                    delta.getSource().getLines().get(i)
                                ));
                            }
                            break;

                        case INSERT:
                            for (int i = 0; i < delta.getTarget().getLines().size(); i++) {
                                changes.add(new DiffChange(
                                    "insert",
                                    delta.getTarget().getPosition() + i + 1,
                                    delta.getTarget().getLines().get(i)
                                ));
                            }
                            break;

                        case CHANGE:
                            // Add deletions
                            for (int i = 0; i < delta.getSource().getLines().size(); i++) {
                                changes.add(new DiffChange(
                                    "delete",
                                    delta.getSource().getPosition() + i + 1,
                                    delta.getSource().getLines().get(i)
                                ));
                            }
                            // Add insertions
                            for (int i = 0; i < delta.getTarget().getLines().size(); i++) {
                                changes.add(new DiffChange(
                                    "insert",
                                    delta.getTarget().getPosition() + i + 1,
                                    delta.getTarget().getLines().get(i)
                                ));
                            }
                            break;
                    }
                }

                // Build unified diff from deltas
                for (AbstractDelta<String> delta : patch.getDeltas()) {
                    unifiedDiffBuilder.append(String.format("@@ -%d,%d +%d,%d @@\n",
                        delta.getSource().getPosition() + 1,
                        delta.getSource().size(),
                        delta.getTarget().getPosition() + 1,
                        delta.getTarget().size()
                    ));
                }

                return new DiffInfo(unifiedDiffBuilder.toString(), changes);

            } catch (Exception e) {
                return new DiffInfo("Diff generation failed", new ArrayList<>());
            }
        }

        // Getters
        public String getId() { return id; }
        public String getType() { return type; }
        public String getSeverity() { return severity; }
        public FileInfo getFile() { return file; }
        public LocationInfo getLocation() { return location; }
        public DiffInfo getDiff() { return diff; }
        public String getStatus() { return status; }
        public String getBeforeCode() { return beforeCode; }
        public String getAfterCode() { return afterCode; }
        public String getExplanation() { return explanation; }
    }

    /**
     * File information
     */
    public static class FileInfo {
        private final String path;
        private final String beforeChecksum;
        private final String afterChecksum;

        public FileInfo(String path, String beforeChecksum, String afterChecksum) {
            this.path = path;
            this.beforeChecksum = beforeChecksum;
            this.afterChecksum = afterChecksum;
        }

        public String getPath() { return path; }
        public String getBeforeChecksum() { return beforeChecksum; }
        public String getAfterChecksum() { return afterChecksum; }
    }

    /**
     * Location information
     */
    public static class LocationInfo {
        private final int startLine;
        private final int endLine;
        private final int startColumn;
        private final int endColumn;

        public LocationInfo(int startLine, int endLine, int startColumn, int endColumn) {
            this.startLine = startLine;
            this.endLine = endLine;
            this.startColumn = startColumn;
            this.endColumn = endColumn;
        }

        public int getStartLine() { return startLine; }
        public int getEndLine() { return endLine; }
        public int getStartColumn() { return startColumn; }
        public int getEndColumn() { return endColumn; }
    }

    /**
     * Diff information
     */
    public static class DiffInfo {
        private final String unified;
        private final List<DiffChange> changes;

        public DiffInfo(String unified, List<DiffChange> changes) {
            this.unified = unified;
            this.changes = changes;
        }

        public String getUnified() { return unified; }
        public List<DiffChange> getChanges() { return changes; }
    }

    /**
     * Individual diff change
     */
    public static class DiffChange {
        private final String type;
        private final int lineNumber;
        private final String content;

        public DiffChange(String type, int lineNumber, String content) {
            this.type = type;
            this.lineNumber = lineNumber;
            this.content = content;
        }

        public String getType() { return type; }
        public int getLineNumber() { return lineNumber; }
        public String getContent() { return content; }
    }

    /**
     * Summary statistics
     */
    public static class Summary {
        private final int total;
        private final int applied;
        private final int pending;
        private final int failed;

        public Summary(int total) {
            this.total = total;
            this.applied = 0;
            this.pending = total;
            this.failed = 0;
        }

        public int getTotal() { return total; }
        public int getApplied() { return applied; }
        public int getPending() { return pending; }
        public int getFailed() { return failed; }
    }

    /**
     * Write enhanced JSON output to file
     */
    public static void writeToFile(EnhancedAnalysisOutput output, Path outputPath) throws IOException {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String json = gson.toJson(output);
        Files.writeString(outputPath, json);
    }

    /**
     * Convert to JSON string
     */
    public static String toJsonString(EnhancedAnalysisOutput output) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        return gson.toJson(output);
    }
}

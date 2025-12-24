package com.pragmite.persistence;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.pragmite.model.AnalysisResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * Repository for persisting and retrieving analysis results.
 * Supports JSON-based storage with versioning and history tracking.
 *
 * Storage structure:
 * <pre>
 * .pragmite/
 *   ├── history/
 *   │   ├── analysis-2025-01-26T14-30-45.json
 *   │   ├── analysis-2025-01-26T15-45-20.json
 *   │   └── ...
 *   └── latest.json (symlink or copy of most recent)
 * </pre>
 */
public class AnalysisRepository {
    private static final Logger logger = LoggerFactory.getLogger(AnalysisRepository.class);

    private static final String DEFAULT_STORAGE_DIR = ".pragmite";
    private static final String HISTORY_DIR = "history";
    private static final String LATEST_FILE = "latest.json";
    private static final DateTimeFormatter TIMESTAMP_FORMAT =
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH-mm-ss");

    private final Path storageDir;
    private final Path historyDir;
    private final Gson gson;

    /**
     * Creates repository in the default directory (.pragmite in current working directory).
     */
    public AnalysisRepository() throws IOException {
        this(Paths.get(DEFAULT_STORAGE_DIR));
    }

    /**
     * Creates repository in a custom directory.
     */
    public AnalysisRepository(Path storageDir) throws IOException {
        this.storageDir = storageDir;
        this.historyDir = storageDir.resolve(HISTORY_DIR);

        // Create directories if they don't exist
        Files.createDirectories(historyDir);

        // Initialize Gson with custom serialization for LocalDateTime
        this.gson = new GsonBuilder()
            .setPrettyPrinting()
            .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
            .create();

        logger.info("Analysis repository initialized at: {}", storageDir.toAbsolutePath());
    }

    /**
     * Saves analysis result to storage.
     * Creates a timestamped file in history and updates the latest.json.
     */
    public void save(AnalysisResult result) throws IOException {
        // Generate timestamped filename
        String timestamp = result.getAnalyzedAt().format(TIMESTAMP_FORMAT);
        String filename = String.format("analysis-%s.json", timestamp);
        Path historyFile = historyDir.resolve(filename);

        // Serialize to JSON
        String json = gson.toJson(result);

        // Write to history
        Files.writeString(historyFile, json);
        logger.info("Saved analysis to: {}", historyFile);

        // Update latest.json
        Path latestFile = storageDir.resolve(LATEST_FILE);
        Files.writeString(latestFile, json);
        logger.info("Updated latest analysis: {}", latestFile);
    }

    /**
     * Loads the most recent analysis result.
     */
    public AnalysisResult loadLatest() throws IOException {
        Path latestFile = storageDir.resolve(LATEST_FILE);

        if (!Files.exists(latestFile)) {
            throw new IOException("No analysis results found. Run analysis first.");
        }

        String json = Files.readString(latestFile);
        return gson.fromJson(json, AnalysisResult.class);
    }

    /**
     * Loads a specific analysis by timestamp.
     */
    public AnalysisResult loadByTimestamp(LocalDateTime timestamp) throws IOException {
        String filename = String.format("analysis-%s.json", timestamp.format(TIMESTAMP_FORMAT));
        Path file = historyDir.resolve(filename);

        if (!Files.exists(file)) {
            throw new IOException("Analysis not found: " + filename);
        }

        String json = Files.readString(file);
        return gson.fromJson(json, AnalysisResult.class);
    }

    /**
     * Lists all stored analysis results (metadata only).
     */
    public List<AnalysisMetadata> listHistory() throws IOException {
        List<AnalysisMetadata> history = new ArrayList<>();

        try (Stream<Path> files = Files.list(historyDir)) {
            files.filter(p -> p.toString().endsWith(".json"))
                .forEach(file -> {
                    try {
                        String json = Files.readString(file);
                        AnalysisResult result = gson.fromJson(json, AnalysisResult.class);

                        AnalysisMetadata metadata = new AnalysisMetadata();
                        metadata.setTimestamp(result.getAnalyzedAt());
                        metadata.setProjectPath(result.getProjectPath());
                        metadata.setTotalFiles(result.getTotalFiles());
                        metadata.setTotalSmells(result.getCodeSmells().size());
                        metadata.setQualityScore(result.getQualityScore() != null
                            ? result.getQualityScore().getOverallScore()
                            : 0.0);
                        metadata.setFilePath(file.toString());

                        history.add(metadata);
                    } catch (IOException | JsonSyntaxException e) {
                        logger.warn("Failed to read analysis file: {}", file, e);
                    }
                });
        }

        // Sort by timestamp descending (newest first)
        history.sort(Comparator.comparing(AnalysisMetadata::getTimestamp).reversed());

        return history;
    }

    /**
     * Compares two analysis results (e.g., latest vs. previous).
     * Returns a diff showing changes in quality metrics.
     */
    public AnalysisComparison compare(AnalysisResult baseline, AnalysisResult current) {
        AnalysisComparison comparison = new AnalysisComparison();
        comparison.setBaseline(baseline);
        comparison.setCurrent(current);

        // Calculate deltas
        comparison.setSmellDelta(current.getCodeSmells().size() - baseline.getCodeSmells().size());

        if (baseline.getQualityScore() != null && current.getQualityScore() != null) {
            double scoreDelta = current.getQualityScore().getOverallScore()
                - baseline.getQualityScore().getOverallScore();
            comparison.setQualityScoreDelta(scoreDelta);
        }

        comparison.setLineDelta(current.getTotalLines() - baseline.getTotalLines());

        return comparison;
    }

    /**
     * Compares current analysis with the previous one in history.
     */
    public AnalysisComparison compareWithPrevious(AnalysisResult current) throws IOException {
        List<AnalysisMetadata> history = listHistory();

        if (history.size() < 2) {
            throw new IOException("Not enough history for comparison. Need at least 2 analyses.");
        }

        // Load the previous analysis (second newest)
        AnalysisMetadata previousMeta = history.get(1);
        AnalysisResult previous = loadByTimestamp(previousMeta.getTimestamp());

        return compare(previous, current);
    }

    /**
     * Deletes old analysis results, keeping only the N most recent.
     */
    public void cleanupHistory(int keepCount) throws IOException {
        List<AnalysisMetadata> history = listHistory();

        if (history.size() <= keepCount) {
            logger.info("History size ({}) is within limit ({}). No cleanup needed.",
                history.size(), keepCount);
            return;
        }

        // Delete oldest entries
        int deleteCount = history.size() - keepCount;
        for (int i = history.size() - 1; i >= history.size() - deleteCount; i--) {
            AnalysisMetadata meta = history.get(i);
            Path file = Paths.get(meta.getFilePath());
            Files.deleteIfExists(file);
            logger.info("Deleted old analysis: {}", file.getFileName());
        }

        logger.info("Cleaned up {} old analysis results", deleteCount);
    }

    /**
     * Gets the storage directory path.
     */
    public Path getStorageDir() {
        return storageDir;
    }

    /**
     * Gets the history directory path.
     */
    public Path getHistoryDir() {
        return historyDir;
    }
}

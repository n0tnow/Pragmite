package com.pragmite.database;

import com.pragmite.model.AnalysisResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Database Manager for Pragmite v1.3.0
 * Manages SQLite database for analysis history, auto-fix tracking, and rollback support.
 */
public class DatabaseManager implements AutoCloseable {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseManager.class);
    private static final String DEFAULT_DB_NAME = ".pragmite.db";

    private Connection connection;
    private Path dbPath;

    /**
     * Initialize database connection.
     * Creates database if it doesn't exist and runs schema migrations.
     */
    public void init(Path projectRoot) throws SQLException {
        this.dbPath = projectRoot.resolve(DEFAULT_DB_NAME);

        logger.info("Initializing database: {}", dbPath);

        // Create connection
        String url = "jdbc:sqlite:" + dbPath.toAbsolutePath();
        connection = DriverManager.getConnection(url);

        // Enable foreign keys
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("PRAGMA foreign_keys = ON");
            stmt.execute("PRAGMA journal_mode = WAL"); // Write-Ahead Logging for better concurrency
        }

        // Run schema if needed
        initializeSchema();

        logger.info("Database initialized successfully");
    }

    /**
     * Initialize database schema from schema.sql file.
     */
    private void initializeSchema() throws SQLException {
        // Check if schema exists
        boolean schemaExists = false;
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(
                 "SELECT name FROM sqlite_master WHERE type='table' AND name='analysis_runs'")) {
            schemaExists = rs.next();
        }

        if (!schemaExists) {
            logger.info("Creating database schema...");

            // Read schema.sql from resources
            try (InputStream is = getClass().getResourceAsStream("/db/schema.sql")) {
                if (is == null) {
                    throw new SQLException("Schema file not found in resources");
                }

                String schema = new BufferedReader(new InputStreamReader(is))
                    .lines()
                    .collect(Collectors.joining("\n"));

                // Execute schema (split by semicolon for multiple statements)
                String[] statements = schema.split(";");
                for (String sql : statements) {
                    String trimmed = sql.trim();
                    if (!trimmed.isEmpty() && !trimmed.startsWith("--")) {
                        try (Statement stmt = connection.createStatement()) {
                            stmt.execute(trimmed);
                        } catch (SQLException e) {
                            // Ignore "table already exists" errors
                            if (!e.getMessage().contains("already exists")) {
                                logger.warn("Error executing schema statement: {}", e.getMessage());
                            }
                        }
                    }
                }

                logger.info("Schema created successfully");
            } catch (Exception e) {
                throw new SQLException("Failed to load schema", e);
            }
        }
    }

    /**
     * Save analysis run to database.
     */
    public long saveAnalysisRun(AnalysisResult result) throws SQLException {
        String sql = """
            INSERT INTO analysis_runs (
                project_path, project_name, quality_score,
                dry_score, orthogonality_score, correctness_score, performance_score,
                quality_grade, total_issues, critical_issues, major_issues, minor_issues,
                files_analyzed, total_lines, duration_ms, pragmite_version
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

        try (PreparedStatement pstmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, result.getProjectPath());
            pstmt.setString(2, result.getProjectName());
            pstmt.setInt(3, (int) result.getQualityScore().getPragmaticScore());
            pstmt.setDouble(4, result.getQualityScore().getDryScore());
            pstmt.setDouble(5, result.getQualityScore().getOrthogonalityScore());
            pstmt.setDouble(6, result.getQualityScore().getCorrectnessScore());
            pstmt.setDouble(7, result.getQualityScore().getPerfScore());
            pstmt.setString(8, result.getQualityScore().getGrade());
            pstmt.setInt(9, result.getCodeSmells().size());

            // Count by severity
            long critical = result.getCodeSmells().stream()
                .filter(s -> "CRITICAL".equals(s.getSeverity().toString())).count();
            long major = result.getCodeSmells().stream()
                .filter(s -> "MAJOR".equals(s.getSeverity().toString())).count();
            long minor = result.getCodeSmells().stream()
                .filter(s -> "MINOR".equals(s.getSeverity().toString())).count();

            pstmt.setLong(10, critical);
            pstmt.setLong(11, major);
            pstmt.setLong(12, minor);
            pstmt.setInt(13, result.getTotalFiles());
            pstmt.setInt(14, result.getTotalLines());

            Long durationMs = result.getAnalysisTimeMs();
            if (durationMs != null) {
                pstmt.setLong(15, durationMs);
            } else {
                pstmt.setNull(15, Types.INTEGER);
            }

            pstmt.setString(16, "1.3.0");

            pstmt.executeUpdate();

            // Get generated ID
            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) {
                    long runId = rs.getLong(1);
                    logger.info("Saved analysis run with ID: {}", runId);

                    // Save code smells
                    saveCodeSmells(runId, result);

                    return runId;
                }
            }
        }

        throw new SQLException("Failed to save analysis run");
    }

    /**
     * Save code smells for an analysis run.
     */
    private void saveCodeSmells(long runId, AnalysisResult result) throws SQLException {
        String sql = """
            INSERT INTO code_smells (
                run_id, file_path, line_number, smell_type, severity,
                description, suggestion, auto_fixable
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """;

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            for (var smell : result.getCodeSmells()) {
                pstmt.setLong(1, runId);
                pstmt.setString(2, smell.getFilePath());
                pstmt.setInt(3, smell.getLine());
                pstmt.setString(4, smell.getType().toString());
                pstmt.setString(5, smell.getSeverity().toString());
                pstmt.setString(6, smell.getMessage());
                pstmt.setString(7, smell.getSuggestion());
                pstmt.setBoolean(8, smell.isAutoFixAvailable());

                pstmt.addBatch();
            }

            pstmt.executeBatch();
            logger.info("Saved {} code smells for run {}", result.getCodeSmells().size(), runId);
        }
    }

    /**
     * Get recent analysis runs.
     */
    public List<AnalysisRun> getRecentRuns(int limit) throws SQLException {
        String sql = "SELECT * FROM v_recent_analyses LIMIT ?";
        List<AnalysisRun> runs = new ArrayList<>();

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, limit);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    AnalysisRun run = new AnalysisRun();
                    run.setId(rs.getLong("id"));
                    run.setProjectName(rs.getString("project_name"));
                    run.setTimestamp(rs.getTimestamp("timestamp").toLocalDateTime());
                    run.setQualityScore(rs.getInt("quality_score"));
                    run.setQualityGrade(rs.getString("quality_grade"));
                    run.setTotalIssues(rs.getInt("total_issues"));
                    run.setCriticalIssues(rs.getInt("critical_issues"));
                    run.setFilesAnalyzed(rs.getInt("files_analyzed"));
                    run.setDurationMs(rs.getLong("duration_ms"));
                    runs.add(run);
                }
            }
        }

        return runs;
    }

    /**
     * Get quality trend for last N days.
     */
    public List<TrendData> getQualityTrend(int days) throws SQLException {
        String sql = """
            SELECT
                DATE(timestamp) as date,
                AVG(quality_score) as avg_score,
                MIN(quality_score) as min_score,
                MAX(quality_score) as max_score,
                AVG(total_issues) as avg_issues
            FROM analysis_runs
            WHERE timestamp >= datetime('now', '-' || ? || ' days')
            GROUP BY DATE(timestamp)
            ORDER BY date ASC
            """;

        List<TrendData> trend = new ArrayList<>();

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, days);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    TrendData data = new TrendData();
                    data.setDate(rs.getString("date"));
                    data.setAvgScore(rs.getDouble("avg_score"));
                    data.setMinScore(rs.getDouble("min_score"));
                    data.setMaxScore(rs.getDouble("max_score"));
                    data.setAvgIssues(rs.getDouble("avg_issues"));
                    trend.add(data);
                }
            }
        }

        return trend;
    }

    /**
     * Cleanup old records according to retention policy.
     */
    public void cleanup() throws SQLException {
        logger.info("Running database cleanup...");

        // Get retention policy
        int keepDays = 90; // default

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT keep_analysis_runs_days FROM cleanup_policy WHERE id = 1")) {
            if (rs.next()) {
                keepDays = rs.getInt(1);
            }
        }

        // Delete old analysis runs (cascade will delete code_smells)
        String sql = "DELETE FROM analysis_runs WHERE timestamp < datetime('now', '-' || ? || ' days')";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, keepDays);
            int deleted = pstmt.executeUpdate();
            logger.info("Deleted {} old analysis runs", deleted);
        }

        // Update last cleanup time
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("UPDATE cleanup_policy SET last_cleanup_at = CURRENT_TIMESTAMP WHERE id = 1");
        }

        // Vacuum to reclaim space
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("VACUUM");
        }

        logger.info("Cleanup completed");
    }

    @Override
    public void close() {
        if (connection != null) {
            try {
                connection.close();
                logger.info("Database connection closed");
            } catch (SQLException e) {
                logger.error("Error closing database connection", e);
            }
        }
    }

    /**
     * Analysis run summary model.
     */
    public static class AnalysisRun {
        private long id;
        private String projectName;
        private LocalDateTime timestamp;
        private int qualityScore;
        private String qualityGrade;
        private int totalIssues;
        private int criticalIssues;
        private int filesAnalyzed;
        private long durationMs;

        // Getters and setters
        public long getId() { return id; }
        public void setId(long id) { this.id = id; }

        public String getProjectName() { return projectName; }
        public void setProjectName(String projectName) { this.projectName = projectName; }

        public LocalDateTime getTimestamp() { return timestamp; }
        public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

        public int getQualityScore() { return qualityScore; }
        public void setQualityScore(int qualityScore) { this.qualityScore = qualityScore; }

        public String getQualityGrade() { return qualityGrade; }
        public void setQualityGrade(String qualityGrade) { this.qualityGrade = qualityGrade; }

        public int getTotalIssues() { return totalIssues; }
        public void setTotalIssues(int totalIssues) { this.totalIssues = totalIssues; }

        public int getCriticalIssues() { return criticalIssues; }
        public void setCriticalIssues(int criticalIssues) { this.criticalIssues = criticalIssues; }

        public int getFilesAnalyzed() { return filesAnalyzed; }
        public void setFilesAnalyzed(int filesAnalyzed) { this.filesAnalyzed = filesAnalyzed; }

        public long getDurationMs() { return durationMs; }
        public void setDurationMs(long durationMs) { this.durationMs = durationMs; }
    }

    /**
     * Quality trend data model.
     */
    public static class TrendData {
        private String date;
        private double avgScore;
        private double minScore;
        private double maxScore;
        private double avgIssues;

        // Getters and setters
        public String getDate() { return date; }
        public void setDate(String date) { this.date = date; }

        public double getAvgScore() { return avgScore; }
        public void setAvgScore(double avgScore) { this.avgScore = avgScore; }

        public double getMinScore() { return minScore; }
        public void setMinScore(double minScore) { this.minScore = minScore; }

        public double getMaxScore() { return maxScore; }
        public void setMaxScore(double maxScore) { this.maxScore = maxScore; }

        public double getAvgIssues() { return avgIssues; }
        public void setAvgIssues(double avgIssues) { this.avgIssues = avgIssues; }
    }
}

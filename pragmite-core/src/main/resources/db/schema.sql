-- Pragmite v1.3.0 - Database Schema
-- SQLite Database for Analysis History, Auto-Fix Tracking, and Rollback Support

-- Schema version tracking
CREATE TABLE IF NOT EXISTS schema_version (
    version INTEGER PRIMARY KEY,
    applied_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

INSERT OR IGNORE INTO schema_version (version) VALUES (1);

-- Analysis run records
CREATE TABLE IF NOT EXISTS analysis_runs (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    project_path TEXT NOT NULL,
    project_name TEXT,
    timestamp DATETIME DEFAULT CURRENT_TIMESTAMP,

    -- Quality metrics
    quality_score INTEGER,
    dry_score REAL,
    orthogonality_score REAL,
    correctness_score REAL,
    performance_score REAL,
    quality_grade TEXT,

    -- Issue counts
    total_issues INTEGER DEFAULT 0,
    critical_issues INTEGER DEFAULT 0,
    major_issues INTEGER DEFAULT 0,
    minor_issues INTEGER DEFAULT 0,

    -- File statistics
    files_analyzed INTEGER DEFAULT 0,
    total_lines INTEGER DEFAULT 0,

    -- Performance
    duration_ms INTEGER,

    -- Metadata
    pragmite_version TEXT DEFAULT '1.3.0',
    config_file TEXT,

    INDEX idx_project_path (project_path),
    INDEX idx_timestamp (timestamp)
);

-- Code smell records (detected issues)
CREATE TABLE IF NOT EXISTS code_smells (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    run_id INTEGER NOT NULL,

    -- Location
    file_path TEXT NOT NULL,
    line_number INTEGER,

    -- Issue details
    smell_type TEXT NOT NULL,
    severity TEXT NOT NULL,  -- CRITICAL, MAJOR, MINOR
    description TEXT,
    suggestion TEXT,

    -- Auto-fix capability
    auto_fixable BOOLEAN DEFAULT 0,
    fix_complexity TEXT,  -- SIMPLE, MODERATE, COMPLEX

    -- Metadata
    detected_at DATETIME DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (run_id) REFERENCES analysis_runs(id) ON DELETE CASCADE,
    INDEX idx_run_id (run_id),
    INDEX idx_file_path (file_path),
    INDEX idx_smell_type (smell_type),
    INDEX idx_auto_fixable (auto_fixable)
);

-- Fix operations (auto-fix execution records)
CREATE TABLE IF NOT EXISTS fix_operations (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    run_id INTEGER,

    -- Timing
    started_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    completed_at DATETIME,

    -- Operation details
    fix_type TEXT,  -- SINGLE, BATCH, SELECTIVE
    fix_mode TEXT DEFAULT 'SAFE',  -- SAFE, AGGRESSIVE, DRY_RUN

    -- Filters
    smell_types_filter TEXT,  -- JSON array of smell types
    file_pattern_filter TEXT,  -- Glob pattern

    -- Results
    total_fixes_attempted INTEGER DEFAULT 0,
    total_fixes_succeeded INTEGER DEFAULT 0,
    total_fixes_failed INTEGER DEFAULT 0,
    total_fixes_skipped INTEGER DEFAULT 0,

    -- Status
    status TEXT DEFAULT 'PENDING',  -- PENDING, IN_PROGRESS, SUCCESS, PARTIAL, FAILED, ROLLED_BACK
    error_message TEXT,

    -- Backup
    backup_created BOOLEAN DEFAULT 0,
    backup_location TEXT,

    FOREIGN KEY (run_id) REFERENCES analysis_runs(id) ON DELETE SET NULL,
    INDEX idx_status (status),
    INDEX idx_started_at (started_at)
);

-- Individual fix records (one per smell fixed)
CREATE TABLE IF NOT EXISTS individual_fixes (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    operation_id INTEGER NOT NULL,
    smell_id INTEGER,

    -- File details
    file_path TEXT NOT NULL,

    -- Fix details
    smell_type TEXT NOT NULL,
    fix_action TEXT NOT NULL,  -- REMOVE_IMPORT, REMOVE_VARIABLE, EXTRACT_CONSTANT, etc.

    -- Status
    status TEXT DEFAULT 'PENDING',  -- PENDING, SUCCESS, FAILED, ROLLED_BACK
    error_message TEXT,

    -- Timing
    applied_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    rolled_back_at DATETIME,

    FOREIGN KEY (operation_id) REFERENCES fix_operations(id) ON DELETE CASCADE,
    FOREIGN KEY (smell_id) REFERENCES code_smells(id) ON DELETE SET NULL,
    INDEX idx_operation_id (operation_id),
    INDEX idx_file_path (file_path),
    INDEX idx_status (status)
);

-- File backups (for rollback support)
CREATE TABLE IF NOT EXISTS file_backups (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    fix_operation_id INTEGER NOT NULL,

    -- File details
    file_path TEXT NOT NULL,
    file_hash TEXT,  -- SHA-256 of original content

    -- Content
    original_content BLOB NOT NULL,
    modified_content BLOB,

    -- Metadata
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    file_size_bytes INTEGER,
    compression_used BOOLEAN DEFAULT 0,

    -- Rollback tracking
    is_restored BOOLEAN DEFAULT 0,
    restored_at DATETIME,

    FOREIGN KEY (fix_operation_id) REFERENCES fix_operations(id) ON DELETE CASCADE,
    INDEX idx_fix_operation_id (fix_operation_id),
    INDEX idx_file_path (file_path),
    INDEX idx_is_restored (is_restored)
);

-- Quality trend (aggregated daily statistics)
CREATE TABLE IF NOT EXISTS quality_trends (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    project_path TEXT NOT NULL,
    date DATE NOT NULL,

    -- Aggregated scores
    avg_quality_score REAL,
    min_quality_score REAL,
    max_quality_score REAL,

    -- Aggregated issue counts
    avg_total_issues REAL,
    avg_critical_issues REAL,

    -- Run statistics
    total_runs INTEGER DEFAULT 0,

    UNIQUE(project_path, date),
    INDEX idx_project_date (project_path, date)
);

-- Configuration snapshots (track config changes)
CREATE TABLE IF NOT EXISTS config_snapshots (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    run_id INTEGER NOT NULL,

    -- Config content
    config_content TEXT,  -- YAML content
    config_hash TEXT,     -- SHA-256 of config

    -- Source
    config_source TEXT,  -- FILE, CLI, DEFAULT
    config_file_path TEXT,

    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (run_id) REFERENCES analysis_runs(id) ON DELETE CASCADE,
    INDEX idx_run_id (run_id)
);

-- Cleanup policy configuration
CREATE TABLE IF NOT EXISTS cleanup_policy (
    id INTEGER PRIMARY KEY CHECK (id = 1),  -- Only one row

    -- Retention periods (in days)
    keep_analysis_runs_days INTEGER DEFAULT 90,
    keep_fix_operations_days INTEGER DEFAULT 30,
    keep_file_backups_days INTEGER DEFAULT 7,

    -- Automatic cleanup
    auto_cleanup_enabled BOOLEAN DEFAULT 1,
    last_cleanup_at DATETIME,

    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

INSERT OR IGNORE INTO cleanup_policy (id) VALUES (1);

-- Views for common queries

-- Recent analysis runs with summary
CREATE VIEW IF NOT EXISTS v_recent_analyses AS
SELECT
    ar.id,
    ar.project_name,
    ar.timestamp,
    ar.quality_score,
    ar.quality_grade,
    ar.total_issues,
    ar.critical_issues,
    ar.files_analyzed,
    ar.duration_ms,
    COUNT(DISTINCT cs.id) as detected_smells,
    COUNT(DISTINCT CASE WHEN cs.auto_fixable = 1 THEN cs.id END) as fixable_smells
FROM analysis_runs ar
LEFT JOIN code_smells cs ON ar.id = cs.run_id
GROUP BY ar.id
ORDER BY ar.timestamp DESC;

-- Fixable issues summary
CREATE VIEW IF NOT EXISTS v_fixable_issues AS
SELECT
    cs.smell_type,
    cs.severity,
    COUNT(*) as issue_count,
    COUNT(DISTINCT cs.file_path) as affected_files
FROM code_smells cs
WHERE cs.auto_fixable = 1
  AND cs.run_id = (SELECT MAX(id) FROM analysis_runs)
GROUP BY cs.smell_type, cs.severity
ORDER BY
    CASE cs.severity
        WHEN 'CRITICAL' THEN 1
        WHEN 'MAJOR' THEN 2
        WHEN 'MINOR' THEN 3
    END,
    issue_count DESC;

-- Fix operation summary
CREATE VIEW IF NOT EXISTS v_fix_operations_summary AS
SELECT
    fo.id,
    fo.started_at,
    fo.status,
    fo.fix_type,
    fo.total_fixes_attempted,
    fo.total_fixes_succeeded,
    fo.total_fixes_failed,
    fo.backup_created,
    COUNT(fb.id) as backup_files_count,
    SUM(fb.file_size_bytes) as total_backup_size
FROM fix_operations fo
LEFT JOIN file_backups fb ON fo.id = fb.fix_operation_id
GROUP BY fo.id
ORDER BY fo.started_at DESC;

-- Quality trend (last 30 days)
CREATE VIEW IF NOT EXISTS v_quality_trend_30d AS
SELECT
    DATE(timestamp) as date,
    AVG(quality_score) as avg_score,
    MIN(quality_score) as min_score,
    MAX(quality_score) as max_score,
    AVG(total_issues) as avg_issues,
    COUNT(*) as run_count
FROM analysis_runs
WHERE timestamp >= datetime('now', '-30 days')
GROUP BY DATE(timestamp)
ORDER BY date DESC;

-- Triggers for automatic cleanup

-- Trigger: Update quality trends after analysis insert
CREATE TRIGGER IF NOT EXISTS trg_update_quality_trends
AFTER INSERT ON analysis_runs
BEGIN
    INSERT OR REPLACE INTO quality_trends (
        project_path,
        date,
        avg_quality_score,
        min_quality_score,
        max_quality_score,
        avg_total_issues,
        avg_critical_issues,
        total_runs
    )
    SELECT
        NEW.project_path,
        DATE(NEW.timestamp),
        AVG(quality_score),
        MIN(quality_score),
        MAX(quality_score),
        AVG(total_issues),
        AVG(critical_issues),
        COUNT(*)
    FROM analysis_runs
    WHERE project_path = NEW.project_path
      AND DATE(timestamp) = DATE(NEW.timestamp);
END;

-- Trigger: Mark fix operation complete when all individual fixes done
CREATE TRIGGER IF NOT EXISTS trg_update_fix_operation_status
AFTER UPDATE ON individual_fixes
BEGIN
    UPDATE fix_operations
    SET
        status = CASE
            WHEN (SELECT COUNT(*) FROM individual_fixes WHERE operation_id = NEW.operation_id AND status = 'PENDING') = 0 THEN
                CASE
                    WHEN (SELECT COUNT(*) FROM individual_fixes WHERE operation_id = NEW.operation_id AND status = 'FAILED') = 0 THEN 'SUCCESS'
                    WHEN (SELECT COUNT(*) FROM individual_fixes WHERE operation_id = NEW.operation_id AND status = 'SUCCESS') = 0 THEN 'FAILED'
                    ELSE 'PARTIAL'
                END
            ELSE status
        END,
        completed_at = CASE
            WHEN (SELECT COUNT(*) FROM individual_fixes WHERE operation_id = NEW.operation_id AND status = 'PENDING') = 0 THEN CURRENT_TIMESTAMP
            ELSE completed_at
        END
    WHERE id = NEW.operation_id;
END;

-- Trigger: Auto-mark backup as restored when rollback occurs
CREATE TRIGGER IF NOT EXISTS trg_mark_backup_restored
AFTER UPDATE ON individual_fixes
WHEN NEW.status = 'ROLLED_BACK' AND OLD.status != 'ROLLED_BACK'
BEGIN
    UPDATE file_backups
    SET
        is_restored = 1,
        restored_at = CURRENT_TIMESTAMP
    WHERE fix_operation_id = NEW.operation_id
      AND file_path = NEW.file_path;
END;

-- End of schema

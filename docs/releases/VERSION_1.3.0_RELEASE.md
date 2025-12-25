# Pragmite v1.3.0 Release Notes

**Release Date:** December 25, 2024
**Version:** 1.3.0
**Codename:** "History & Auto-Fix"

## 🎯 Overview

Pragmite v1.3.0 introduces powerful new capabilities for tracking analysis history and automating code quality improvements:

- **SQL Database Integration** - SQLite database for persistent analysis history tracking
- **Auto-Fix Infrastructure** - Framework for one-click automatic fixes
- **Rollback System** - Safely undo auto-fix operations

## 🚀 New Features

### 1. SQL Database for History Tracking

Store and query analysis results across time with SQLite integration.

**Key Features:**
- Automatic database initialization (`.pragmite.db`)
- Analysis run tracking with quality scores
- Individual code smell tracking
- Quality trend analysis over time
- Automatic cleanup policy (90-day retention by default)

**New CLI Commands:**
```bash
# Save analysis results to database
pragmite --save-to-db

# Show last 10 analysis runs
pragmite --show-history 10

# Show 30-day quality trend
pragmite --show-trend 30
```

**Database Schema:**
- `analysis_runs` - Each analysis execution
- `code_smells` - Individual detected issues
- `fix_operations` - Batch auto-fix executions
- `individual_fixes` - Individual fix records
- `file_backups` - Original file content for rollback
- `quality_trends` - Aggregated daily statistics

### 2. Auto-Fix Infrastructure

Framework for automatically applying fixes to detected code smells.

**Key Features:**
- Automatic backup creation before fixes
- Dry-run mode for previewing changes
- Type-specific fix filtering
- Error handling with optional stop-on-error
- File pattern filtering

**New CLI Commands:**
```bash
# Apply all auto-fixes
pragmite --apply-fixes

# Apply specific fix types
pragmite --apply-fixes --fix-type UNUSED_IMPORT,MAGIC_NUMBER

# Preview fixes without applying
pragmite --dry-run --apply-fixes

# Skip backup creation
pragmite --apply-fixes --no-backup
```

**Supported Fix Types (Framework Ready):**
- Infrastructure ready for:
  - UNUSED_IMPORT
  - UNUSED_VARIABLE
  - MAGIC_NUMBER
  - MAGIC_STRING
  - MISSING_TRY_WITH_RESOURCES

*Note: Actual fixer implementations will be added in v1.3.x patch releases*

### 3. Rollback System

Safely undo automatic fixes with database-tracked backups.

**Key Features:**
- File content stored in database (BLOB)
- Rollback by operation ID, file path, or "last"
- Operation status tracking (SUCCESS, PARTIAL, ROLLED_BACK)
- List all rollbackable operations

**New CLI Commands:**
```bash
# Rollback most recent fix operation
pragmite --rollback-last

# Rollback specific operation by ID
pragmite --rollback 123

# Rollback all fixes for a specific file
pragmite --rollback-file src/Main.java

# List available rollback operations
pragmite --list-rollbacks
```

**Rollback Output:**
```
🔄 En son işlem geri alınıyor...

✅ Rollback Başarılı:
   Toplam yedek: 5
   Geri yüklenen: 5
   Atlanan: 0

📁 Geri yüklenen dosyalar:
   - src/Main.java
   - src/Utils.java
```

## 📊 Database Integration

### Schema Overview

**Core Tables:**
```sql
analysis_runs
├── id (PRIMARY KEY)
├── project_path
├── quality_score
├── total_issues
├── critical_issues
└── timestamp

code_smells
├── id (PRIMARY KEY)
├── run_id (FK -> analysis_runs)
├── file_path
├── line_number
├── smell_type
└── severity

fix_operations
├── id (PRIMARY KEY)
├── fix_type
├── total_fixes_attempted
├── total_fixes_succeeded
└── status
```

### Automated Cleanup

Database automatically maintains:
- 90-day retention policy (configurable)
- Automatic VACUUM for space reclamation
- Cascade delete for referential integrity

### Views for Analytics

Pre-built views for common queries:
- `v_recent_analyses` - Last analysis runs with smell counts
- `v_smell_trends` - Smell type distribution over time
- `v_file_hotspots` - Files with most frequent issues

## 🔧 Technical Details

### Database Connection

- **Type:** SQLite 3.45.0.0
- **Location:** `{project_root}/.pragmite.db`
- **Mode:** Write-Ahead Logging (WAL) for better concurrency
- **Foreign Keys:** Enabled with cascade deletes

### Auto-Fix Architecture

```
AutoFixEngine
├── FixOptions (configuration)
├── FixResult (tracking)
├── BackupManager (file backups)
└── RefactoringStrategy (fixer interface)
```

### Rollback Architecture

```
RollbackManager
├── RollbackResult (result tracking)
├── RollbackableOperation (operation metadata)
└── Database integration (BLOB storage)
```

## 📈 Version Comparison

| Feature | v1.2.0 | v1.3.0 |
|---------|--------|--------|
| Analysis History | ❌ | ✅ SQLite DB |
| Quality Trends | ❌ | ✅ Time-based |
| Auto-Fix | ❌ | ✅ Infrastructure |
| Rollback | ❌ | ✅ Full support |
| Configuration | ✅ YAML | ✅ YAML |
| Incremental | ✅ Cache | ✅ Cache |
| Reports | ✅ HTML/JSON | ✅ HTML/JSON |
| CI/CD | ✅ Quality Gates | ✅ Quality Gates |

## 🔄 Upgrade Guide

### From v1.2.0 to v1.3.0

**Breaking Changes:** None

**New Dependencies:**
- org.xerial:sqlite-jdbc:3.45.0.0

**Maven Update:**
```xml
<dependency>
    <groupId>com.pragmite</groupId>
    <artifactId>pragmite-core</artifactId>
    <version>1.3.0</version>
</dependency>
```

**Gradle Update:**
```gradle
implementation 'com.pragmite:pragmite-core:1.3.0'
```

### Database Migration

No migration needed - database is automatically created on first use.

To enable database tracking:
```bash
# Add --save-to-db to your analysis command
pragmite --save-to-db

# Or update CI/CD workflows
pragmite --save-to-db --fail-on-critical
```

## 📝 Usage Examples

### Example 1: Track Quality Over Time

```bash
# Day 1: Analyze and save to database
pragmite --save-to-db

# Day 30: Compare trend
pragmite --show-trend 30
```

**Output:**
```
📈 Son 30 Günlük Kalite Trendi:

Tarih          | Ort.Skor | Min  | Max  | Ort.Sorun
---------------|----------|------|------|----------
2024-12-01     |     78.5 |   72 |   85 |      12.3
2024-12-15     |     82.3 |   79 |   87 |       8.7
2024-12-25     |     88.1 |   85 |   92 |       4.2
```

### Example 2: Safe Auto-Fix Workflow

```bash
# Step 1: Preview fixes
pragmite --dry-run --apply-fixes

# Step 2: Apply fixes with backup
pragmite --apply-fixes

# Step 3: Verify changes (git diff, tests, etc.)

# Step 4: Rollback if needed
pragmite --rollback-last
```

### Example 3: CI/CD with History

```yaml
# GitHub Actions example
- name: Analyze with Pragmite
  run: |
    java -jar pragmite-core-1.3.0.jar \
      --save-to-db \
      --fail-on-critical \
      --min-quality-score 75
```

## 🐛 Known Issues

1. **Auto-Fix Implementations** - Fixer implementations not yet added (framework only)
2. **Rollback UI** - Command-line only (VSCode UI pending)
3. **Database Sync** - No multi-process locking (single process only)

## 🛣️ Roadmap

### v1.3.1 (Next Patch)
- Implement UNUSED_IMPORT fixer
- Implement MAGIC_NUMBER fixer
- Add VSCode rollback UI

### v1.3.2
- Implement remaining fixers
- Add database export/import
- Performance optimizations

### v1.4.0
- Team collaboration features
- Central database server option
- Advanced analytics dashboard

## 🤝 Contributing

We welcome contributions! Areas needing help:
- **Fixer Implementations** - See `RefactoringStrategy` interface
- **Database Optimizations** - Query performance
- **Documentation** - More examples and guides

## 📄 License

MIT License - See LICENSE file for details

## 🙏 Acknowledgments

- SQLite team for the excellent embedded database
- JavaParser team for AST manipulation support
- Community contributors and testers

## 📞 Support

- **Issues:** https://github.com/pragmite/pragmite/issues
- **Discussions:** https://github.com/pragmite/pragmite/discussions
- **Email:** support@pragmite.com

---

**Full Changelog:** https://github.com/pragmite/pragmite/compare/v1.2.0...v1.3.0

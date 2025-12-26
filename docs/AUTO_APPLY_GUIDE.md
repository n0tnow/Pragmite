# Pragmite Auto-Apply Feature Guide

**Version:** 1.5.0
**Last Updated:** December 26, 2025

---

## 📋 Table of Contents

1. [Overview](#overview)
2. [Quick Start](#quick-start)
3. [Features](#features)
4. [Usage Examples](#usage-examples)
5. [Command-Line Options](#command-line-options)
6. [Safety Features](#safety-features)
7. [Backup & Rollback](#backup--rollback)
8. [Known Limitations](#known-limitations)
9. [Troubleshooting](#troubleshooting)
10. [Best Practices](#best-practices)

---

## Overview

The **Auto-Apply** feature in Pragmite v1.5.0 automatically applies AI-generated refactoring suggestions to your code. Instead of manually copying and pasting refactored code, Pragmite can now intelligently apply changes while maintaining safety through automatic backups and compilation validation.

### Key Benefits

- ✅ **Automatic Refactoring**: Apply AI suggestions with a single command
- ✅ **Safe Operations**: Automatic backups before every change
- ✅ **Validation**: Syntax checking ensures code remains compilable
- ✅ **Rollback Support**: Easily revert changes if needed
- ✅ **Dry-Run Mode**: Preview changes without modifying files
- ✅ **Batch Processing**: Apply multiple refactorings in one go

---

## Quick Start

### Basic Usage

Analyze and automatically apply refactorings:

```bash
java -jar pragmite-core-1.5.0.jar --analyze src/ --auto-apply
```

### Dry-Run (Preview Only)

Preview what would be changed without modifying files:

```bash
java -jar pragmite-core-1.5.0.jar --analyze src/ --auto-apply --dry-run
```

### With AI Analysis

Combine with AI-powered refactoring suggestions:

```bash
java -jar pragmite-core-1.5.0.jar --analyze src/ --ai-analysis --auto-apply
```

---

## Features

### 1. Automatic Code Application

Pragmite automatically applies refactored code to source files:

- Replaces entire file contents with refactored version
- Validates syntax after each change
- Creates backup before modification
- Reports success/failure for each application

### 2. Backup System

Every file modification creates a timestamped backup:

- **Location**: `%TEMP%/pragmite-backups` (Windows) or `/tmp/pragmite-backups` (Linux/Mac)
- **Format**: `<filename>.backup.<timestamp>`
- **Retention**: Last 10 backups per file (automatic cleanup)
- **Checksum**: MD5 verification for backup integrity

### 3. Compilation Validation

Two validation strategies ensure code quality:

#### Fast Validation (Default)
- Uses JavaParser for syntax checking
- Fast execution (milliseconds)
- Catches most common errors

#### Strict Validation (Optional)
- Uses javac compiler for full semantic checking
- Slower but more thorough
- Enable with `--strict-validation` flag (future enhancement)

### 4. Rollback Support

Quickly revert changes if something goes wrong:

```bash
# List all backups
java -jar pragmite-core-1.5.0.jar --list-backups

# List backups for specific file
java -jar pragmite-core-1.5.0.jar --list-backups-for UserService.java

# Rollback to latest backup
java -jar pragmite-core-1.5.0.jar --rollback-file-backup UserService.java
```

---

## Usage Examples

### Example 1: Basic Auto-Apply

Analyze a single file and apply refactorings:

```bash
java -jar pragmite-core-1.5.0.jar --analyze src/UserService.java --auto-apply
```

**Output:**
```
🔍 Analysis Results:
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

Found 12 code smells

🔧 Auto-Applying Refactorings...

[1/12] Applying: God Class (UserService.java:1)
  ✅ Applied successfully
  💾 Backup: C:\Users\...\UserService.java.backup.20251226045131

[2/12] Applying: Long Method (UserService.java:45)
  ✅ Applied successfully
  💾 Backup: C:\Users\...\UserService.java.backup.20251226045132

...

📊 Auto-Apply Summary:
  Total: 12
  ✅ Successfully applied: 10
  ❌ Failed: 2
  ⏭️  Skipped: 0
  Success rate: 83.3%
```

---

### Example 2: Dry-Run Mode

Preview changes without modifying files:

```bash
java -jar pragmite-core-1.5.0.jar --analyze src/ --auto-apply --dry-run
```

**Output:**
```
🔍 Analysis Results: Found 12 code smells

🧪 DRY-RUN MODE: No files will be modified

[1/12] Would apply: God Class (UserService.java:1)
  ✅ Validation: PASSED
  📄 Would create backup: UserService.java.backup.<timestamp>

[2/12] Would apply: Long Method (UserService.java:45)
  ✅ Validation: PASSED
  📄 Would create backup: UserService.java.backup.<timestamp>

...

📊 Dry-Run Summary:
  Total: 12
  ✅ Would succeed: 10
  ❌ Would fail: 2

💡 Run without --dry-run to apply changes
```

---

### Example 3: AI Analysis + Auto-Apply

Use AI to generate refactorings and apply them:

```bash
java -jar pragmite-core-1.5.0.jar \
  --analyze src/UserService.java \
  --ai-analysis \
  --auto-apply
```

**Output:**
```
🤖 AI Analysis: Generating refactoring suggestions...
  Model: Claude Sonnet 4.5
  Detected 12 code smells
  Generated 12 refactoring suggestions

🔧 Auto-Applying AI Refactorings...

[1/12] Applying: Extract service layer
  🤖 AI Suggestion: Split UserService into UserService + UserRepository
  ✅ Applied successfully
  📊 Metrics: Complexity 12 → 4 (↓67%)

...
```

---

### Example 4: Disable Backups (Not Recommended)

Skip backup creation for faster execution:

```bash
java -jar pragmite-core-1.5.0.jar --analyze src/ --auto-apply --no-backup
```

⚠️ **Warning:** Not recommended! Always keep backups enabled unless you have version control.

---

### Example 5: View and Rollback

List backups and rollback if needed:

```bash
# Step 1: List all backups
java -jar pragmite-core-1.5.0.jar --list-backups

# Output:
# 📦 File-Based Backups (Auto-Apply):
# Location: C:\Users\bkaya\AppData\Local\Temp\pragmite-backups
#
# Total backups: 10
#
# File                  Created              Size
# ───────────────────────────────────────────────────
# UserService.java      2025-12-26 04:51:31  4.1 KB
# UserService.java      2025-12-26 04:51:30  6.7 KB
# ...

# Step 2: List backups for specific file
java -jar pragmite-core-1.5.0.jar --list-backups-for UserService.java

# Step 3: Rollback to latest backup
java -jar pragmite-core-1.5.0.jar --rollback-file-backup UserService.java

# Output:
# 🔙 Rolling back UserService.java...
#
# ✅ Rollback successful!
#
# Details:
# • Target file: UserService.java
# • Restored from: UserService.java.backup.20251226045131
# • Safety backup created: UserService.java.backup.20251226050245
#
# 💡 A safety backup was created before rollback
```

---

## Command-Line Options

### Analysis Flags

| Flag | Description |
|------|-------------|
| `--analyze <path>` | Analyze Java files at path |
| `--ai-analysis` | Enable AI-powered refactoring suggestions |
| `--auto-refactor` | Alias for `--ai-analysis` |

### Auto-Apply Flags

| Flag | Description | Default |
|------|-------------|---------|
| `--auto-apply` | Enable automatic code application | Disabled |
| `--dry-run` | Preview changes without modifying files | Disabled |
| `--backup` | Create backups before modifications | Enabled |
| `--no-backup` | Disable backup creation (⚠️ not recommended) | - |

### Backup Management Flags

| Flag | Description |
|------|-------------|
| `--list-backups` | List all file-based backups |
| `--list-backups-for <filename>` | List backups for specific file |
| `--rollback-file-backup <filename>` | Rollback file to latest backup |

### Future Flags (v1.6.0+)

| Flag | Description | Status |
|------|-------------|--------|
| `--strict-validation` | Use javac for full semantic validation | Planned |
| `--interactive` | Ask for confirmation before each change | Planned |
| `--format json` | Output results in JSON format | Planned |

---

## Safety Features

### 1. Automatic Backups

Every modification creates a backup:

```
Original file:  src/UserService.java
Backup created: %TEMP%/pragmite-backups/UserService.java.backup.20251226045131
```

Backups include:
- Full file contents
- MD5 checksum
- Creation timestamp
- Original file path

### 2. Validation Before Apply

Each change is validated before being written:

```java
// Validation Pipeline:
1. Parse refactored code with JavaParser
2. Check for syntax errors
3. If valid → Apply changes
4. If invalid → Skip and report error
```

### 3. Rollback on Failure

If a change fails validation:

```
[5/12] Applying: Complex Method (UserService.java:78)
  ❌ Failed: Syntax error at line 82
  🔙 Rolling back to previous backup...
  ✅ File restored to original state
```

### 4. Safety Backup Before Rollback

Rolling back creates a safety backup:

```
Rolling back UserService.java...
  💾 Safety backup created: UserService.java.backup.20251226050245
  🔙 Restored from: UserService.java.backup.20251226045131
  ✅ Rollback successful
```

### 5. Automatic Cleanup

Old backups are automatically cleaned:

- Keeps last 10 backups per file
- Deletes oldest backups first
- Runs after each backup creation

---

## Backup & Rollback

### Backup Location

**Windows:**
```
C:\Users\<username>\AppData\Local\Temp\pragmite-backups\
```

**Linux/Mac:**
```
/tmp/pragmite-backups/
```

### Backup Format

```
<filename>.backup.<timestamp>

Example:
UserService.java.backup.20251226045131
```

### Listing Backups

**List all backups:**
```bash
java -jar pragmite-core-1.5.0.jar --list-backups
```

**List backups for specific file:**
```bash
java -jar pragmite-core-1.5.0.jar --list-backups-for UserService.java
```

**Output format:**
```
File                  Created              Size
───────────────────────────────────────────────────
UserService.java      2025-12-26 04:51:31  4.1 KB
UserService.java      2025-12-26 04:51:30  6.7 KB
UserService.java      2025-12-26 04:51:29  3.1 KB
```

### Rollback Operations

**Rollback to latest backup:**
```bash
java -jar pragmite-core-1.5.0.jar --rollback-file-backup UserService.java
```

**Manual rollback (copy backup file):**
```bash
# Windows
copy %TEMP%\pragmite-backups\UserService.java.backup.20251226045131 src\UserService.java

# Linux/Mac
cp /tmp/pragmite-backups/UserService.java.backup.20251226045131 src/UserService.java
```

---

## Known Limitations

### 1. JavaParser Validation Limitation

**Issue:** Default validation only catches syntax errors, not semantic errors.

**Example:**
```java
// This will pass validation (but won't compile):
ınt x = 5;  // Turkish 'ı' instead of 'i'
```

**Impact:** Low - AI-generated code is usually syntactically correct

**Workaround:**
- Future: Use `--strict-validation` flag (v1.6.0)
- Current: Run `mvn compile` after auto-apply

### 2. Full File Replacement

**Issue:** Current implementation replaces entire file contents, not surgical edits.

**Example:**
```java
// Cannot replace just one method
// Instead, replaces entire file
```

**Impact:** Medium - Works but less elegant

**Reason:** CodeSmell model doesn't provide method/class names

**Future:** Enhance when CodeSmell model is improved (v1.6.0+)

### 3. Multiple Sequential Applications

**Issue:** Multiple refactorings to same file create multiple backups.

**Example:**
```
UserService.java.backup.20251226045131  (after refactoring 1)
UserService.java.backup.20251226045132  (after refactoring 2)
UserService.java.backup.20251226045133  (after refactoring 3)
...
```

**Impact:** Low - Backup cleanup handles this automatically

**Design Decision:** Intentional for safety (each state is preserved)

---

## Troubleshooting

### Problem: "Backup creation failed"

**Symptom:**
```
❌ Failed to create backup: Permission denied
```

**Solution:**
1. Check write permissions for temp directory
2. Ensure disk space is available
3. Try running as administrator (Windows) or with sudo (Linux)

---

### Problem: "Validation failed: Syntax error"

**Symptom:**
```
❌ Failed: Syntax error at line 42
```

**Solution:**
1. Check if AI suggestion is valid Java code
2. Run with `--dry-run` to preview changes
3. Manually inspect the suggested refactoring
4. Report issue if AI generated invalid code

---

### Problem: "No backups found"

**Symptom:**
```
❌ No backups found for: UserService.java
```

**Solution:**
1. Verify backup directory exists: `%TEMP%/pragmite-backups`
2. Check if backups were created (run with `--backup` flag)
3. Ensure filename matches exactly (case-sensitive)

---

### Problem: "Rollback failed: Target file does not exist"

**Symptom:**
```
❌ Rollback failed: Target file does not exist: src/UserService.java
```

**Solution:**
1. Verify file path is correct
2. Run command from project root directory
3. Check if file was moved or deleted

---

### Problem: Auto-apply success rate is low

**Symptom:**
```
Success rate: 25% (3/12 applied)
```

**Possible Causes:**
1. AI suggestions may be invalid
2. Code has complex dependencies
3. Syntax errors in refactored code

**Solution:**
1. Use `--dry-run` to identify issues
2. Check validation errors in output
3. Manually review failed applications
4. Report patterns to Pragmite team

---

## Best Practices

### 1. Always Use Version Control

Before running auto-apply:

```bash
# Commit your changes first
git add .
git commit -m "Before Pragmite auto-apply"

# Then run auto-apply
java -jar pragmite-core-1.5.0.jar --analyze src/ --auto-apply

# Review changes
git diff

# Commit if satisfied, or revert if not
git commit -m "Applied Pragmite refactorings"
# OR
git reset --hard HEAD
```

### 2. Start with Dry-Run

Always preview changes first:

```bash
# Step 1: Dry-run
java -jar pragmite-core-1.5.0.jar --analyze src/ --auto-apply --dry-run

# Step 2: Review output

# Step 3: Apply for real
java -jar pragmite-core-1.5.0.jar --analyze src/ --auto-apply
```

### 3. Apply to Small Batches

Start with single files:

```bash
# Good: Apply to one file
java -jar pragmite-core-1.5.0.jar --analyze src/UserService.java --auto-apply

# Risky: Apply to entire project at once
java -jar pragmite-core-1.5.0.jar --analyze src/ --auto-apply
```

### 4. Validate After Apply

Run tests and compilation:

```bash
# Apply refactorings
java -jar pragmite-core-1.5.0.jar --analyze src/ --auto-apply

# Validate
mvn clean compile test

# If tests fail, rollback
java -jar pragmite-core-1.5.0.jar --rollback-file-backup <file>
```

### 5. Keep Backups Enabled

Never disable backups in production:

```bash
# Good
java -jar pragmite-core-1.5.0.jar --analyze src/ --auto-apply

# Bad (no safety net)
java -jar pragmite-core-1.5.0.jar --analyze src/ --auto-apply --no-backup
```

### 6. Review Before Commit

Always review changes before committing:

```bash
# Apply
java -jar pragmite-core-1.5.0.jar --analyze src/ --auto-apply

# Review with git diff
git diff src/

# Review specific file
git diff src/UserService.java

# Commit only if satisfied
git add src/UserService.java
git commit -m "Refactor: Extract service layer"
```

---

## Integration with CI/CD

### GitHub Actions Example

```yaml
name: Pragmite Auto-Refactor

on:
  schedule:
    - cron: '0 2 * * 1'  # Every Monday at 2 AM

jobs:
  refactor:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3

      - name: Setup Java
        uses: actions/setup-java@v3
        with:
          java-version: '17'

      - name: Download Pragmite
        run: |
          wget https://github.com/your-repo/pragmite/releases/download/v1.5.0/pragmite-core-1.5.0.jar

      - name: Run Pragmite Auto-Apply
        run: |
          java -jar pragmite-core-1.5.0.jar \
            --analyze src/ \
            --ai-analysis \
            --auto-apply

      - name: Run Tests
        run: mvn test

      - name: Create Pull Request
        if: success()
        uses: peter-evans/create-pull-request@v5
        with:
          title: "Automated refactoring by Pragmite"
          body: "AI-powered refactoring suggestions applied automatically"
          branch: pragmite-refactor-${{ github.run_number }}
```

---

## Advanced Usage

### Combining with AI Analysis

```bash
# Generate AI refactorings and apply them
java -jar pragmite-core-1.5.0.jar \
  --analyze src/ \
  --ai-analysis \
  --auto-apply \
  --dry-run
```

### Custom Backup Directory (Future)

```bash
# Not yet implemented (planned for v1.6.0)
java -jar pragmite-core-1.5.0.jar \
  --analyze src/ \
  --auto-apply \
  --backup-dir /custom/path
```

### Batch Rollback (Future)

```bash
# Not yet implemented (planned for v1.6.0)
java -jar pragmite-core-1.5.0.jar \
  --rollback-all-since 2025-12-26T04:00:00
```

---

## FAQ

### Q: Is auto-apply safe?

**A:** Yes, with multiple safety features:
- Automatic backups before every change
- Syntax validation before applying
- Rollback support
- Dry-run mode for preview

However, always use version control as an additional safety net.

---

### Q: Can I undo auto-applied changes?

**A:** Yes, three ways:
1. Use `--rollback-file-backup <filename>` command
2. Manually restore from `%TEMP%/pragmite-backups`
3. Use Git to revert: `git reset --hard HEAD`

---

### Q: What happens if validation fails?

**A:** The change is skipped and reported as failed. The original file remains unchanged. No backup is created for failed applications.

---

### Q: How many backups are kept?

**A:** Last 10 backups per file. Older backups are automatically deleted.

---

### Q: Can I disable backups?

**A:** Yes, with `--no-backup` flag. However, this is **not recommended** unless you have version control or other backup mechanisms.

---

### Q: Does auto-apply work with all code smells?

**A:** It works with code smells that have AI-generated refactored code. Currently supports:
- God Class
- Long Method
- Feature Envy
- Data Clumps
- And others detected by AI analysis

---

### Q: Can I apply only specific refactorings?

**A:** Currently, auto-apply processes all detected issues. Interactive mode (v1.6.0) will allow selective application.

---

## Version History

### v1.5.0 (December 26, 2025) - Initial Release
- ✅ Automatic code application
- ✅ Backup system with MD5 checksums
- ✅ JavaParser validation
- ✅ Rollback support
- ✅ Dry-run mode
- ✅ Batch processing

### Future Versions

**v1.6.0 (Planned Q1 2026):**
- Interactive mode (user confirmation)
- Strict validation (javac)
- JSON output format
- VSCode extension integration

**v1.7.0 (Planned Q2 2026):**
- Web UI
- Real-time progress updates
- Custom backup directory
- Batch rollback

---

## Support

### Documentation
- [README.md](../README.md) - General overview
- [PHASE_3_AUTO_APPLY_DESIGN.md](PHASE_3_AUTO_APPLY_DESIGN.md) - Technical design
- [VERSION_1.5.0_RELEASE.md](releases/VERSION_1.5.0_RELEASE.md) - Release notes

### Reporting Issues
- GitHub Issues: https://github.com/your-repo/pragmite/issues
- Email: support@pragmite.com

### Community
- Discord: https://discord.gg/pragmite
- Stack Overflow: Tag `pragmite`

---

**Last Updated:** December 26, 2025
**Document Version:** 1.0
**Pragmite Version:** 1.5.0

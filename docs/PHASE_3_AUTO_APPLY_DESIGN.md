# Phase 3: Automatic Code Application - Design Document

**Version:** 1.5.0
**Status:** Planning
**Priority:** ⭐⭐⭐⭐⭐ (PRIMARY FOCUS)

---

## 🎯 Goal

Enable **automatic application** of AI-generated refactored code to source files with:
- AST-based smart replacement (not regex)
- Automatic backup before changes
- Compilation verification
- Rollback on failure
- Dry-run mode for preview
- Interactive approval (optional)

---

## 🏗️ Architecture

### Core Components

#### 1. `CodeApplicator` - Main Application Engine
**Responsibility:** Apply refactored code to source files using AST-based replacement

```java
public class CodeApplicator {
    private final BackupManager backupManager;
    private final CompilationValidator validator;
    private final ASTReplacer astReplacer;

    /**
     * Apply refactored code to source file.
     * @return ApplicationResult with success/failure status
     */
    public ApplicationResult apply(RefactoredCode refactored, Path sourceFile) {
        // 1. Create backup
        Backup backup = backupManager.createBackup(sourceFile);

        // 2. Parse original and refactored code to AST
        CompilationUnit originalAST = parseToAST(sourceFile);
        CompilationUnit refactoredAST = parseToAST(refactored.getRefactoredCode());

        // 3. Find and replace matching AST nodes
        boolean replaced = astReplacer.replace(originalAST, refactoredAST, refactored);

        // 4. Write modified code back to file
        Files.writeString(sourceFile, originalAST.toString());

        // 5. Validate compilation
        CompilationResult compilation = validator.validate(sourceFile);

        if (!compilation.isSuccess()) {
            // Rollback on compilation failure
            backupManager.restore(backup);
            return ApplicationResult.failed("Compilation failed", compilation.getErrors());
        }

        return ApplicationResult.success(sourceFile, backup);
    }
}
```

#### 2. `ASTReplacer` - AST-Based Code Replacement
**Responsibility:** Find and replace code using AST matching (not text-based)

```java
public class ASTReplacer {
    /**
     * Replace matching AST nodes in original with refactored nodes.
     * Uses structural matching to find the exact location to replace.
     */
    public boolean replace(CompilationUnit original, CompilationUnit refactored, RefactoredCode context) {
        // Strategy 1: Match by method signature
        if (context.getSmell().getType() == CodeSmellType.LONG_METHOD) {
            return replaceMethod(original, refactored, context);
        }

        // Strategy 2: Match by code snippet location
        if (context.getSmell().hasLineNumber()) {
            return replaceByLocation(original, refactored, context);
        }

        // Strategy 3: Match by AST structure similarity
        return replaceByStructure(original, refactored, context);
    }

    private boolean replaceMethod(CompilationUnit original, CompilationUnit refactored, RefactoredCode context) {
        String methodName = context.getSmell().getMethodName();

        // Find method in original AST
        Optional<MethodDeclaration> originalMethod = original.findFirst(
            MethodDeclaration.class,
            m -> m.getNameAsString().equals(methodName)
        );

        // Find method in refactored AST
        Optional<MethodDeclaration> refactoredMethod = refactored.findFirst(
            MethodDeclaration.class,
            m -> m.getNameAsString().equals(methodName)
        );

        if (originalMethod.isPresent() && refactoredMethod.isPresent()) {
            // Replace method body
            originalMethod.get().setBody(refactoredMethod.get().getBody().get());
            return true;
        }

        return false;
    }
}
```

#### 3. `BackupManager` - Backup and Restore
**Responsibility:** Create backups before changes, restore on failure

```java
public class BackupManager {
    private final Path backupDir;

    public BackupManager() {
        this.backupDir = Paths.get(System.getProperty("java.io.tmpdir"), "pragmite-backups");
        Files.createDirectories(backupDir);
    }

    /**
     * Create backup of file before modification.
     */
    public Backup createBackup(Path sourceFile) throws IOException {
        String timestamp = Instant.now().toString().replace(":", "-");
        String backupFileName = sourceFile.getFileName() + ".backup." + timestamp;
        Path backupPath = backupDir.resolve(backupFileName);

        Files.copy(sourceFile, backupPath, StandardCopyOption.REPLACE_EXISTING);

        return new Backup(sourceFile, backupPath, Instant.now());
    }

    /**
     * Restore file from backup.
     */
    public void restore(Backup backup) throws IOException {
        Files.copy(backup.getBackupPath(), backup.getOriginalPath(),
                   StandardCopyOption.REPLACE_EXISTING);
        logger.info("Restored {} from backup", backup.getOriginalPath());
    }

    /**
     * Clean up old backups (keep last 10).
     */
    public void cleanupOldBackups() {
        // Keep only last 10 backups per file
    }
}
```

#### 4. `CompilationValidator` - Verify Code Compiles
**Responsibility:** Validate that modified code compiles successfully

```java
public class CompilationValidator {
    private final JavaCompiler compiler;

    public CompilationValidator() {
        this.compiler = ToolProvider.getSystemJavaCompiler();
    }

    /**
     * Validate that file compiles successfully.
     */
    public CompilationResult validate(Path sourceFile) {
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, null, null);

        Iterable<? extends JavaFileObject> compilationUnits =
            fileManager.getJavaFileObjectsFromFiles(Collections.singletonList(sourceFile.toFile()));

        JavaCompiler.CompilationTask task = compiler.getTask(
            null, fileManager, diagnostics, null, null, compilationUnits
        );

        boolean success = task.call();

        if (!success) {
            List<String> errors = diagnostics.getDiagnostics().stream()
                .map(Diagnostic::getMessage)
                .collect(Collectors.toList());
            return CompilationResult.failed(errors);
        }

        return CompilationResult.success();
    }
}
```

#### 5. `ApplicationResult` - Result Model
```java
public class ApplicationResult {
    private final boolean success;
    private final Path appliedFile;
    private final Backup backup;
    private final List<String> errors;
    private final ApplicationMetrics metrics;

    public static ApplicationResult success(Path file, Backup backup) {
        return new ApplicationResult(true, file, backup, Collections.emptyList(), null);
    }

    public static ApplicationResult failed(String reason, List<String> errors) {
        return new ApplicationResult(false, null, null, errors, null);
    }
}
```

---

## 🚀 CLI Integration

### New Flags

```java
@Option(names = {"--auto-apply"}, description = "Automatically apply refactored code to source files")
private boolean autoApply;

@Option(names = {"--dry-run"}, description = "Show what would be changed without applying")
private boolean dryRun;

@Option(names = {"--backup"}, description = "Create backups before applying (default: true)")
private boolean createBackup = true;

@Option(names = {"--no-backup"}, description = "Skip backup creation (dangerous!)")
private boolean noBackup;

@Option(names = {"--interactive"}, description = "Ask for confirmation before each change")
private boolean interactive;

@Option(names = {"--rollback-on-failure"}, description = "Automatically rollback on compilation failure (default: true)")
private boolean rollbackOnFailure = true;
```

### Usage Examples

```bash
# Dry run - preview changes without applying
java -jar pragmite-core.jar ./project --auto-refactor --auto-apply --dry-run

# Apply with automatic backup
java -jar pragmite-core.jar ./project --auto-refactor --auto-apply

# Interactive mode - ask before each change
java -jar pragmite-core.jar ./project --auto-refactor --auto-apply --interactive

# No backup (dangerous!)
java -jar pragmite-core.jar ./project --auto-refactor --auto-apply --no-backup
```

---

## 🔄 Workflow

### Standard Auto-Apply Flow

```
1. Analyze code (detect smells)
   ↓
2. Generate AI prompts
   ↓
3. Call Claude API (get refactored code)
   ↓
4. FOR EACH refactored code:
   a. Create backup of source file
   b. Parse original + refactored to AST
   c. Find matching nodes in AST
   d. Replace nodes
   e. Write to file
   f. Validate compilation
   g. If compilation fails → ROLLBACK
   h. If compilation succeeds → KEEP
   ↓
5. Report results:
   - ✅ X files successfully refactored
   - ❌ Y files failed (rolled back)
   - 📊 Total improvements
```

### Dry-Run Flow

```
1-3. Same as above
   ↓
4. FOR EACH refactored code:
   a. Parse to AST
   b. Find what would be replaced
   c. Show diff preview
   d. DO NOT APPLY
   ↓
5. Report what would happen:
   - 📝 File: UserService.java
     - Would replace method: processUser()
     - Lines affected: 15-45
     - Changes: Extract validation, reduce complexity
```

---

## 🛡️ Safety Features

### 1. Automatic Backup
- Every file backed up before modification
- Backups stored in temp directory with timestamp
- Automatic cleanup of old backups

### 2. Compilation Validation
- Modified file must compile
- If compilation fails → automatic rollback
- Compilation errors shown to user

### 3. Rollback Support
```java
public class RollbackManager {
    public void rollbackAll(List<ApplicationResult> results) {
        for (ApplicationResult result : results) {
            if (result.hasBackup()) {
                backupManager.restore(result.getBackup());
            }
        }
    }
}
```

### 4. Interactive Approval
```java
if (interactive) {
    System.out.println("About to apply refactoring to: " + file);
    System.out.println("Changes:");
    showDiff(originalCode, refactoredCode);

    System.out.print("Apply? [y/N]: ");
    String response = scanner.nextLine();

    if (!response.equalsIgnoreCase("y")) {
        return ApplicationResult.skipped("User declined");
    }
}
```

---

## 📊 Metrics and Reporting

### Application Summary
```
🤖 Auto-Apply Summary
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

✅ Successfully Applied: 8 files
   - UserService.java (3 methods refactored)
   - OrderProcessor.java (1 method refactored)
   - PaymentService.java (2 methods refactored)
   ...

❌ Failed (Rolled Back): 2 files
   - BuggyClass.java (compilation error)
   - ComplexService.java (AST replacement failed)

⏭️  Skipped: 1 file
   - LegacyCode.java (user declined)

📈 Improvements:
   - Complexity reduced: 45 → 28 (-38%)
   - Lines of code: 1250 → 980 (-22%)
   - Code smells fixed: 11 / 13 (85%)

💾 Backups saved to: /tmp/pragmite-backups
```

---

## 🧪 Testing Strategy

### Unit Tests
1. `CodeApplicatorTest` - Test application logic
2. `ASTReplacerTest` - Test AST replacement strategies
3. `BackupManagerTest` - Test backup/restore
4. `CompilationValidatorTest` - Test compilation validation

### Integration Tests
1. Apply refactoring to real Java files
2. Verify compilation succeeds
3. Test rollback on intentional failures
4. Test dry-run mode
5. Test interactive mode

### Test Project
Use `test-project/UserService.java` for testing:
- Has known code smells (magic numbers, long method)
- Will be refactored by AI
- Auto-apply should work successfully

---

## 🚧 Edge Cases

### 1. Multiple Smells in Same File
**Problem:** File has 3 code smells, how to apply multiple refactorings?

**Solution:**
- Apply refactorings sequentially
- Re-parse AST after each successful application
- If any fails, rollback all changes to that file

### 2. AST Replacement Fails
**Problem:** Can't find matching AST nodes

**Solution:**
- Fall back to line number-based replacement
- If that fails, skip and report to user
- Keep backup, don't modify file

### 3. Compilation Fails After Apply
**Problem:** Refactored code doesn't compile

**Solution:**
- Automatic rollback to backup
- Report compilation errors
- Flag as "failed application"

### 4. File Modified During Analysis
**Problem:** User modifies file while Pragmite is running

**Solution:**
- Check file modification time before apply
- If modified, skip and warn user
- Suggest re-running analysis

---

## 📝 Implementation Plan

### Step 1: Core Classes (Day 1)
- [ ] Create `CodeApplicator.java`
- [ ] Create `ASTReplacer.java`
- [ ] Create `BackupManager.java`
- [ ] Create `CompilationValidator.java`
- [ ] Create result models

### Step 2: CLI Integration (Day 1)
- [ ] Add CLI flags
- [ ] Integrate with existing `handleAiAnalysis()`
- [ ] Add dry-run mode
- [ ] Add interactive mode

### Step 3: Testing (Day 2)
- [ ] Unit tests for each component
- [ ] Integration test with test-project
- [ ] Test rollback scenarios
- [ ] Test edge cases

### Step 4: Documentation (Day 2)
- [ ] User guide: AUTO_APPLY_GUIDE.md
- [ ] Update README.md
- [ ] Add examples
- [ ] Update roadmap

---

## 🎯 Success Criteria

✅ Phase 3 is complete when:

1. Auto-apply works for all code smell types
2. Compilation validation catches errors
3. Rollback works reliably
4. Dry-run mode shows accurate preview
5. Interactive mode works
6. All tests pass
7. Documentation is complete
8. Successfully tested on test-project

---

**Next Steps:** Start implementation with `CodeApplicator.java`

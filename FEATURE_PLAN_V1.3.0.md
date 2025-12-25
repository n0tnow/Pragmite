# 🚀 Pragmite v1.3.0 - Yeni Özellikler Planı

**Planlanan Versiyon:** 1.3.0
**Hedef:** Otomatik Düzeltme + Tarihsel İzleme + Rollback
**Tahmini Süre:** 4-6 saat

---

## 🎯 İstenen Özellikler

### 1. ✨ Tek Tuşla Düzeltme (One-Click Auto-Fix)
**İhtiyaç:** Kod kokularını tek bir komutla otomatik düzelt

**Özellikler:**
- CLI'dan: `--apply-fixes` veya `--auto-fix` parametresi
- VSCode'dan: "Fix All Issues" komutu
- Batch fix: Tüm auto-fixable sorunları düzelt
- Selective fix: Sadece belirli türdeki sorunları düzelt
- Safe mode: Önce backup al, sonra düzelt

**Örnek Kullanım:**
```bash
# Tüm düzeltilebilir sorunları düzelt
java -jar pragmite-core-1.3.0.jar --apply-fixes my-project/

# Sadece unused imports düzelt
java -jar pragmite-core-1.3.0.jar --apply-fixes=UNUSED_IMPORT my-project/

# Backup ile birlikte düzelt
java -jar pragmite-core-1.3.0.jar --apply-fixes --create-backup my-project/
```

### 2. 🗄️ SQL Database (Tarihsel İzleme)
**İhtiyaç:** Analiz geçmişini kaydet, trend analizi yap

**Özellikler:**
- SQLite database (embedded, kurulum gerektirmez)
- Analiz geçmişi (timestamp, quality score, issue count)
- Dosya bazlı geçmiş (hangi dosya ne zaman değişti)
- Trend raporu (quality score grafiği)
- Karşılaştırma (2 analiz arası fark)

**Schema:**
```sql
-- Analiz çalıştırma kayıtları
CREATE TABLE analysis_runs (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    project_path TEXT NOT NULL,
    timestamp DATETIME DEFAULT CURRENT_TIMESTAMP,
    quality_score INTEGER,
    total_issues INTEGER,
    critical_issues INTEGER,
    major_issues INTEGER,
    minor_issues INTEGER,
    files_analyzed INTEGER,
    duration_ms INTEGER
);

-- Kod kokusu kayıtları
CREATE TABLE code_smells (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    run_id INTEGER,
    file_path TEXT,
    smell_type TEXT,
    severity TEXT,
    line_number INTEGER,
    description TEXT,
    suggestion TEXT,
    auto_fixable BOOLEAN,
    FOREIGN KEY (run_id) REFERENCES analysis_runs(id)
);

-- Düzeltme işlemleri
CREATE TABLE fix_operations (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    run_id INTEGER,
    smell_id INTEGER,
    timestamp DATETIME DEFAULT CURRENT_TIMESTAMP,
    fix_type TEXT,
    status TEXT, -- SUCCESS, FAILED, ROLLED_BACK
    backup_path TEXT,
    FOREIGN KEY (run_id) REFERENCES analysis_runs(id),
    FOREIGN KEY (smell_id) REFERENCES code_smells(id)
);

-- Dosya backup'ları (rollback için)
CREATE TABLE file_backups (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    fix_operation_id INTEGER,
    file_path TEXT,
    original_content BLOB,
    modified_content BLOB,
    timestamp DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (fix_operation_id) REFERENCES fix_operations(id)
);
```

**Örnek Sorgular:**
```sql
-- Son 7 günün quality trend'i
SELECT DATE(timestamp) as date, AVG(quality_score) as avg_score
FROM analysis_runs
WHERE timestamp >= datetime('now', '-7 days')
GROUP BY DATE(timestamp);

-- En çok sorun olan dosyalar
SELECT file_path, COUNT(*) as issue_count
FROM code_smells
WHERE run_id = (SELECT MAX(id) FROM analysis_runs)
GROUP BY file_path
ORDER BY issue_count DESC LIMIT 10;
```

### 3. ⏮️ Rollback (Geri Alma)
**İhtiyaç:** Otomatik düzeltmeleri geri al

**Özellikler:**
- Fix operation tracking (her düzeltme kaydedilir)
- File-level rollback (dosya bazında geri al)
- Batch rollback (tüm son düzeltmeleri geri al)
- Selective rollback (sadece belirli düzeltmeleri geri al)
- Backup management (eski backup'ları temizle)

**Örnek Kullanım:**
```bash
# Son düzeltmeyi geri al
java -jar pragmite-core-1.3.0.jar --rollback-last

# Belirli bir fix operation'ı geri al
java -jar pragmite-core-1.3.0.jar --rollback=<fix_id>

# Belirli bir dosyanın düzeltmelerini geri al
java -jar pragmite-core-1.3.0.jar --rollback-file=src/Foo.java

# Tüm düzeltmeleri geri al
java -jar pragmite-core-1.3.0.jar --rollback-all
```

---

## 📦 Yeni Modüller

### 1. Database Module (`com.pragmite.database`)

**DatabaseManager.java**
```java
public class DatabaseManager {
    private Connection connection;

    public void init(Path dbPath);
    public void saveAnalysisRun(AnalysisResult result);
    public List<AnalysisRun> getHistory(int limit);
    public List<CodeSmell> getSmellsForRun(long runId);
    public AnalysisTrend getTrend(int days);
}
```

**AnalysisRepository.java**
```java
public class AnalysisRepository {
    public void insert(AnalysisRun run);
    public AnalysisRun findById(long id);
    public List<AnalysisRun> findByProject(String projectPath);
    public List<AnalysisRun> findRecent(int days);
}
```

### 2. Auto-Fix Module (`com.pragmite.autofix`)

**AutoFixEngine.java**
```java
public class AutoFixEngine {
    public FixResult applyFixes(List<CodeSmell> smells, FixOptions options);
    public FixResult applyFix(CodeSmell smell, FixOptions options);
    public boolean canFix(CodeSmell smell);
}
```

**FixOptions.java**
```java
public class FixOptions {
    private boolean createBackup = true;
    private boolean dryRun = false;
    private Set<CodeSmellType> allowedTypes;
    private boolean stopOnError = false;
}
```

**FixResult.java**
```java
public class FixResult {
    private int successCount;
    private int failureCount;
    private List<FixOperation> operations;
    private List<String> errors;
}
```

### 3. Rollback Module (`com.pragmite.rollback`)

**RollbackManager.java**
```java
public class RollbackManager {
    public void rollbackLast();
    public void rollback(long fixOperationId);
    public void rollbackFile(String filePath);
    public void rollbackAll();
    public List<FixOperation> getRollbackableOperations();
}
```

**BackupManager.java**
```java
public class BackupManager {
    public void createBackup(Path file, long fixOperationId);
    public void restoreBackup(long backupId);
    public void cleanOldBackups(int days);
}
```

---

## 🔧 CLI Güncellemeleri

**Yeni Parametreler:**
```bash
# Auto-fix
--apply-fixes                    # Tüm düzeltilebilir sorunları düzelt
--apply-fixes=<type>            # Sadece belirli tipteki sorunları düzelt
--dry-run                       # Sadece göster, değiştirme
--create-backup                 # Düzeltmeden önce backup al

# Database/History
--save-to-db                    # Sonuçları database'e kaydet
--show-history                  # Son 10 analiz sonucunu göster
--show-trend=<days>             # Son N günün trend'ini göster
--compare=<run_id>              # İki analizi karşılaştır

# Rollback
--rollback-last                 # Son düzeltmeyi geri al
--rollback=<fix_id>             # Belirli düzeltmeyi geri al
--rollback-file=<path>          # Dosya bazında geri al
--rollback-all                  # Tüm düzeltmeleri geri al
--list-rollbacks                # Geri alınabilir işlemleri listele
```

---

## 📊 VSCode Extension Güncellemeleri

**Yeni Komutlar:**
```typescript
// One-click fix
pragmite.applyAllFixes          // Tüm düzeltilebilir sorunları düzelt
pragmite.applyFixesForFile      // Sadece bu dosyanın sorunlarını düzelt
pragmite.applyFixForSmell       // Tek bir kod kokusunu düzelt

// History
pragmite.showHistory            // Analiz geçmişini göster
pragmite.showTrend              // Quality trend grafiğini göster
pragmite.compareWithPrevious    // Önceki analiz ile karşılaştır

// Rollback
pragmite.rollbackLast           // Son düzeltmeyi geri al
pragmite.rollbackFile           // Bu dosyanın düzeltmelerini geri al
pragmite.listRollbacks          // Geri alınabilir işlemler
```

**UI İyileştirmeleri:**
```typescript
// Code Actions
class PragmiteCodeActionProvider {
    // Her kod kokusunun yanında "Fix" butonu göster
    provideCodeActions(document, range, context) {
        // context.diagnostics'den Pragmite uyarılarını bul
        // Her biri için QuickFix action oluştur
        return [
            {
                title: "Fix: Remove unused import",
                command: "pragmite.applyFixForSmell",
                arguments: [smell]
            }
        ];
    }
}
```

---

## 📈 Implementation Roadmap

### Phase 1: Database Layer (2 saat)
1. ✅ SQLite JDBC dependency ekle (pom.xml)
2. ✅ Schema oluştur (schema.sql)
3. ✅ DatabaseManager implement et
4. ✅ AnalysisRepository implement et
5. ✅ Migration system (schema versioning)
6. ✅ Unit testler

### Phase 2: Auto-Fix Engine (2 saat)
1. ✅ AutoFixEngine class
2. ✅ Mevcut fixer'ları refactor et (UnusedImportFixer, etc.)
3. ✅ FixOptions ve FixResult models
4. ✅ Batch fix logic
5. ✅ Dry-run mode
6. ✅ Integration testler

### Phase 3: Rollback System (1.5 saat)
1. ✅ BackupManager implement et
2. ✅ RollbackManager implement et
3. ✅ File restore logic
4. ✅ Rollback verification
5. ✅ Cleanup old backups
6. ✅ Unit testler

### Phase 4: CLI Integration (30 dk)
1. ✅ Yeni parametreleri PragmiteCLI'a ekle
2. ✅ Auto-fix komutlarını wire et
3. ✅ Database save logic
4. ✅ History/Trend raporları
5. ✅ Rollback komutları

### Phase 5: VSCode Extension (optional, gelecek için)
1. ⬜ Code Action Provider
2. ⬜ Quick Fix UI
3. ⬜ History webview
4. ⬜ Trend visualization

---

## 🔐 Güvenlik ve Performans

**Güvenlik:**
- File permissions check (yazılabilir mi?)
- Backup integrity (checksum)
- SQL injection prevention (prepared statements)
- Concurrent access handling (file locks)

**Performans:**
- Batch inserts (database)
- Connection pooling
- Lazy backup loading (sadece gerektiğinde)
- Background cleanup (eski backup'lar)

---

## 📝 Migration Guide (v1.2.0 → v1.3.0)

**Breaking Changes:**
- None (backward compatible)

**New Features:**
```bash
# Eski yöntem (v1.2.0)
java -jar pragmite-core-1.2.0.jar my-project/

# Yeni yöntem (v1.3.0) - Auto-fix ile
java -jar pragmite-core-1.3.0.jar --apply-fixes --save-to-db my-project/

# Tarihsel analiz
java -jar pragmite-core-1.3.0.jar --show-trend=7

# Rollback
java -jar pragmite-core-1.3.0.jar --rollback-last
```

---

## ✅ Checklist

### Database
- [ ] SQLite JDBC dependency
- [ ] Schema.sql dosyası
- [ ] DatabaseManager class
- [ ] AnalysisRepository class
- [ ] Migration system
- [ ] Unit testler (10+ test)

### Auto-Fix
- [ ] AutoFixEngine class
- [ ] FixOptions model
- [ ] FixResult model
- [ ] Batch fix logic
- [ ] Dry-run mode
- [ ] Integration testler (5+ test)

### Rollback
- [ ] BackupManager class
- [ ] RollbackManager class
- [ ] File restore logic
- [ ] Cleanup mechanism
- [ ] Unit testler (8+ test)

### CLI
- [ ] 10 yeni parametre ekle
- [ ] Help documentation
- [ ] Error handling
- [ ] Integration test

### Documentation
- [ ] README güncelle
- [ ] CHANGELOG.md
- [ ] API documentation
- [ ] Usage examples

---

**Toplam Tahmini Süre:** 6 saat
**Öncelik:** YÜKSEK
**Versiyon:** 1.3.0
**Hedef Tarih:** 26 Aralık 2025

# Pragmite

**Java Code Quality Analysis Tool** - SonarQube-like Static Analysis with Big-O Complexity Detection

> Java kod tabanları için statik analiz aracı:
> - **30+ kod kokusu dedektörü** (Long Method, God Class, Magic Numbers, Duplicate Code, vb.)
> - **Büyük-O karmaşıklık analizi** (döngüler, özyineleme, stream API)
> - **Siklomatic karmaşıklık** hesaplaması
> - **Pragmatic Programmer skorlaması** (DRY, Orthogonality, Correctness, Performance)
> - **HTML/PDF raporlar** - Chart.js ile profesyonel raporlar
> - **Incremental Analysis** - Cache ile 10x hızlı analiz
> - **Configuration System** - .pragmite.yaml ile proje bazlı ayarlar
> - **CI/CD Integration** - Quality gates ve exit codes
> - **SQL Database** - Analiz geçmişi takibi (SQLite)
> - **Auto-Fix Infrastructure** - Otomatik düzeltme altyapısı
> - **Rollback System** - Güvenli geri alma sistemi
> - **AI-Powered Analysis** - AI prompts for Claude/GPT-4/Gemini
- **Auto-Refactoring** - AI-generated code improvements with Claude API
- **Auto-Apply** 🆕 - Automatically apply AI refactorings with safety backups
- **Rollback System** 🆕 - File-based rollback for auto-apply operations

**Current Version:** v1.6.3 | **Release Date:** December 29, 2025

---

## İçindekiler
- [Mevcut Özellikler](#mevcut-özellikler)
- [Kurulum](#kurulum)
- [Kullanım](#kullanım)
- [Kod Kokusu Dedektörleri](#kod-kokusu-dedektörleri)
- [Karmaşıklık Analizi](#karmaşıklık-analizi)
- [Skorlama Sistemi](#skorlama-sistemi)
- [Örnek Çıktı](#örnek-çıktı)
- [Gelecek Özellikler](#gelecek-özellikler)
- [Mimari](#mimari)

---

## Mevcut Özellikler

### ✅ Şu An Çalışan Özellikler

**Statik Analiz ve Kod Kokuları (30+ Dedektör):**
- **Long Method** - Uzun metotlar (varsayılan: 30 satır)
- **God Class** - Çok fazla sorumluluk taşıyan sınıflar
- **Magic Numbers** - Sabit kodlanmış sayılar (Hex/Binary/Octal/Float desteği)
- **Magic Strings** - Sabit kodlanmış string'ler
- **Unused Import** - Kullanılmayan import'lar (inner class, annotation, generic desteği)
- **Unused Variable** - Kullanılmayan yerel değişkenler
- **Empty Catch Block** - Boş catch blokları (kasıtlı ignore tespiti)
- **Missing Try-With-Resources** - AutoCloseable kaynaklar için try-with-resources eksikliği
- **Deep Nesting** - Derin iç içe kod blokları (4+ seviye)
- **Data Class** - Sadece getter/setter içeren sınıflar (Anemic Domain Model)
- **Long Parameter List** - Çok parametreli metotlar (6+)
- **Complex Boolean Expression** - Karmaşık boolean ifadeler (4+ operatör)
- **Missing Javadoc** - Public API'lerde eksik dokümantasyon
- **Raw Type Usage** - Generics kullanmayan koleksiyon tanımları
- **Duplicate Code** 🆕 - Tekrarlanan kod blokları (Type-2 clone detection)
- **Data Clumps** 🆕 - Gruplar halinde geçen parametreler
- **Feature Envy** 🆕 - Başka sınıfın verilerini çok kullanan metodlar
- **Inappropriate Intimacy** 🆕 - Sınıflar arası aşırı bağımlılık
- **Lazy Class** 🆕 - Çok az iş yapan sınıflar
- **Speculative Generality** 🆕 - Gereksiz soyutlama

**Karmaşıklık Analizi:**
- **Big-O Complexity** - O(1), O(n), O(n²), O(n³), O(log n), O(n log n), O(2^n)
  - İç içe döngü analizi
  - Özyinelemeli algoritmalar (tail recursion, binary recursion, exponential recursion)
  - Stream API analizi (filter, map, flatMap, sorted operations)
  - Koleksiyon işlemleri (nested streams, stream in loop)
- **Cyclomatic Complexity** - McCabe karmaşıklık metriği

**Skorlama:**
- **Pragmatic Programmer İlkeleri**: DRY, Orthogonality, Correctness, Performance
- **Genel Kalite Skoru**: 0-100 arası ağırlıklı skor
- **Harf Notu**: A+ (95-100), A (90-94), B (80-89), C (70-79), D (60-69), F (<60)

**Raporlama:**
- **Konsol Çıktısı**: Renkli ve okunabilir tablo formatı
- **JSON Export**: Detaylı analiz sonuçları ve metrikleri
- **Otomatik Düzeltme Önerileri**: Her koku için aksiyon alabilen öneriler

---

## Kurulum

**Gereksinimler:**
- JDK 21+
- Maven 3.8+

**Derleme:**

```bash
cd pragmite-core
mvn clean package
```

JAR dosyası oluşturulacak: `target/pragmite-core-1.0-SNAPSHOT.jar`

---

## Kullanım

### Temel Kullanım

**Bir projeyi analiz et:**
```bash
java -jar pragmite-core-1.6.3.jar /path/to/java/project
```

**JSON raporu oluştur:**
```bash
java -jar pragmite-core-1.6.3.jar /path/to/java/project -o report.json
```

**Sadece konsol çıktısı:**
```bash
java -jar pragmite-core-1.6.3.jar /path/to/java/project -f console
```

**JSON ve konsol birlikte:**
```bash
java -jar pragmite-core-1.6.3.jar /path/to/java/project -f both -o report.json
```

### CLI Parametreleri

**Temel Parametreler:**
- `<projectDir>` - Analiz edilecek proje dizini (zorunlu)
- `-f, --format` - Çıktı formatı: `console`, `json`, `html`, `pdf`, `both` (varsayılan: both)
- `-o, --output` - Rapor dosya adı (varsayılan: pragmite-report.json)
- `-v, --verbose` - Ayrıntılı çıktı
- `-h, --help` - Yardım mesajını göster
- `-V, --version` - Sürüm bilgisini göster

**Configuration (v1.2.0):**
- `--config` - YAML konfigürasyon dosyası (.pragmite.yaml)
- `--generate-config` - Örnek .pragmite.yaml dosyası oluştur

**Thresholds:**
- `--complexity-threshold` - Cyclomatic complexity eşik değeri (varsayılan: 15)
- `--method-length` - Maksimum metot uzunluğu (varsayılan: 50)
- `--param-count` - Maksimum parametre sayısı (varsayılan: 5)

**Filters:**
- `--exclude` - Hariç tutulacak dizinler (virgülle ayrılmış)
- `--include` - Dahil edilecek dosya pattern (glob)

**Performance (v1.2.0):**
- `--incremental` - Sadece değişen dosyaları analiz et (cache kullan)
- `--clear-cache` - Analiz cache'ini temizle ve çık

**Quality Gates (v1.2.0):**
- `--fail-on-critical` - Kritik sorun varsa exit code 1 ile çık
- `--min-quality-score` - Minimum kalite skoru (0-100)
- `--max-critical-issues` - Maksimum kritik sorun sayısı

**Auto-Fix (v1.3.0):**
- `--apply-fixes` - Tüm otomatik düzeltmeleri uygula
- `--fix-type <types>` - Sadece belirtilen tipteki sorunları düzelt (virgülle ayrılmış)
- `--dry-run` - Düzeltmeleri önizle, uygulamadan göster
- `--no-backup` - Düzeltme sırasında yedek oluşturma

**Database & History (v1.3.0):**
- `--save-to-db` - Analiz sonuçlarını veritabanına kaydet
- `--show-history <N>` - Son N analiz sonucunu göster
- `--show-trend <days>` - Son N günün kalite trendini göster

**Rollback (v1.3.0):**
- `--rollback-last` - En son düzeltme işlemini geri al
- `--rollback <id>` - Belirtilen ID'li düzeltme işlemini geri al
- `--rollback-file <path>` - Belirtilen dosyadaki tüm düzeltmeleri geri al
- `--list-rollbacks` - Geri alınabilir düzeltme işlemlerini listele

**AI-Powered Analysis (v1.4.0):**
- `--generate-ai-prompts` - Generate AI-powered analysis with ready-to-use prompts
- `--ai-output <path>` - AI analysis output file (JSON format, default: pragmite-ai-analysis.json)
- `--auto-refactor` - 🆕 Automatically generate refactored code using Claude API
- `--claude-api-key <key>` - 🆕 Claude API key (or use CLAUDE_API_KEY env var)

**Auto-Apply (v1.5.0):**
- `--auto-apply` - 🆕 Automatically apply AI refactorings to source files
- `--dry-run` - 🆕 Preview changes without modifying files
- `--backup` / `--no-backup` - Control backup creation (enabled by default)

**File-Based Rollback (v1.5.0):**
- `--list-backups` - 🆕 List all file-based backups
- `--list-backups-for <filename>` - 🆕 List backups for specific file
- `--rollback-file-backup <filename>` - 🆕 Rollback to latest backup

**Auto-Apply Example (v1.5.0):**
```bash
# Set API key
export CLAUDE_API_KEY="sk-ant-..."

# Run analysis with AI refactoring + auto-apply
java -jar pragmite-core-1.5.0.jar ./my-project --ai-analysis --auto-apply

# Output:
# 🔧 Auto-Applying Refactorings...
# [1/12] Applying: God Class (UserService.java:1)
#   ✅ Applied successfully
#   💾 Backup: UserService.java.backup.20251226045131
# ...
# 📊 Success rate: 83.3% (10/12 applied)

# Preview without applying (dry-run)
java -jar pragmite-core-1.5.0.jar ./my-project --ai-analysis --auto-apply --dry-run

# List backups
java -jar pragmite-core-1.5.0.jar --list-backups-for UserService.java

# Rollback if needed
java -jar pragmite-core-1.5.0.jar --rollback-file-backup UserService.java
```

**Features:**
- ✅ Automatic code application with safety backups
- ✅ JavaParser validation before applying changes
- ✅ Dry-run mode for preview
- ✅ File-based rollback system
- ✅ MD5 checksum verification
- ✅ Automatic cleanup (keeps last 10 backups)

📖 **Complete Guide:** See [docs/AUTO_APPLY_GUIDE.md](docs/AUTO_APPLY_GUIDE.md)

---

## Kod Kokusu Dedektörleri

### Metot Seviyesi Kokular

**Long Method (Uzun Metot)**
- Eşik: 30 satır
- Öneri: Metodu daha küçük parçalara ayırın
- Otomatik Düzeltme: ❌

**Long Parameter List (Uzun Parametre Listesi)**
- Eşik: 6 parametre
- Öneri: Parameter Object pattern kullanın
- Otomatik Düzeltme: ❌

**Complex Boolean Expression (Karmaşık Boolean)**
- Eşik: 4 operatör
- Öneri: Boolean ifadeyi açıklayıcı değişkenlere ayırın
- Otomatik Düzeltme: ❌

**Deep Nesting (Derin İç İçe)**
- Eşik: 4 seviye
- Öneri: Guard clauses kullanın, early return yapın
- Otomatik Düzeltme: ❌

### Sınıf Seviyesi Kokular

**God Class (Tanrı Sınıfı)**
- Metrik: 20+ metot VE 15+ alan
- Öneri: Single Responsibility Principle uygulayın
- Otomatik Düzeltme: ❌

**Data Class (Veri Sınıfı)**
- Metrik: 90%+ accessor metot, 0 iş mantığı
- Öneri: İlgili iş mantığını bu sınıfa taşıyın veya Record kullanın
- Otomatik Düzeltme: ❌

### Kod Kalitesi Kokular

**Magic Numbers (Sihirli Sayılar)**
- Tespit: Hex (0xFF), Binary (0b1010), Octal (077), Float (3.14)
- Öneri: `private static final` sabitler kullanın
- Otomatik Düzeltme: ✅

**Magic Strings (Sihirli String'ler)**
- Tespit: 3+ karakter String literaller
- Öneri: Sabit tanımlayın
- Otomatik Düzeltme: ✅

**Unused Import (Kullanılmayan Import)**
- Gelişmiş Tespit: Inner class, annotation, generic, method reference desteği
- Öneri: Import'u kaldırın
- Otomatik Düzeltme: ✅

**Unused Variable (Kullanılmayan Değişken)**
- Tespit: Tanımlanan ama kullanılmayan yerel değişkenler
- İstisna: `_` ile başlayan değişkenler (kasıtlı ignore)
- Otomatik Düzeltme: ✅

**Empty Catch Block (Boş Catch)**
- Akıllı Tespit: "ignore", "suppress", "expected" yorumlarını kontrol eder
- Öneri: Exception'ı logla veya yeniden fırlat
- Otomatik Düzeltme: ❌

**Missing Try-With-Resources**
- Tespit: AutoCloseable kaynaklar (InputStream, Connection, Socket, vb.)
- Öneri: try-with-resources kullanın
- Otomatik Düzeltme: ✅

**Raw Type Usage**
- Tespit: Generics kullanmayan koleksiyon tanımları
- Öneri: Type-safe generic tanımlar kullanın
- Otomatik Düzeltme: ❌

**Missing Javadoc**
- Tespit: Public class/method/interface'lerde eksik dokümantasyon
- Öneri: API dokümantasyonu ekleyin
- Otomatik Düzeltme: ❌

---

## Karmaşıklık Analizi

### Big-O Complexity (Zaman Karmaşıklığı)

**Döngü Analizi:**
```java
// O(n) - Tek döngü
for (int i = 0; i < n; i++) { }

// O(n²) - İç içe döngü
for (int i = 0; i < n; i++) {
    for (int j = 0; j < n; j++) { }
}

// O(n·m) - Farklı değişkenler
for (int i = 0; i < n; i++) {
    for (int j = 0; j < m; j++) { }
}
```

**Özyineleme Analizi:**
```java
// O(n) - Lineer recursion
int factorial(int n) {
    if (n <= 1) return 1;
    return n * factorial(n - 1);
}

// O(2^n) - Binary recursion (Exponential)
int fibonacci(int n) {
    if (n <= 1) return n;
    return fibonacci(n - 1) + fibonacci(n - 2);
}

// O(log n) - Tail recursion (optimizable)
int binarySearch(int[] arr, int target, int low, int high) {
    if (low > high) return -1;
    int mid = (low + high) / 2;
    if (arr[mid] == target) return mid;
    if (arr[mid] > target) return binarySearch(arr, target, low, mid - 1);
    return binarySearch(arr, target, mid + 1, high);
}
```

**Stream API Analizi:**
```java
// O(n) - filter + map
list.stream().filter(x -> x > 0).map(x -> x * 2).collect(toList());

// O(n log n) - sorted
list.stream().sorted().collect(toList());

// O(n²) - flatMap içinde stream
list.stream().flatMap(x -> anotherList.stream()).collect(toList());

// O(n²) - Stream in loop (nested)
for (Item item : items) {
    list.stream().filter(x -> x.matches(item)).findFirst();
}
```

### Cyclomatic Complexity (Siklomatic Karmaşıklık)

McCabe metriği - karar noktalarını sayar:
- **1-10**: Basit, test edilmesi kolay
- **11-20**: Orta karmaşıklık, kabul edilebilir
- **21-50**: Yüksek karmaşıklık, refactor düşünün
- **50+**: Çok yüksek risk, mutlaka refactor edin

---

## Skorlama Sistemi

### Pragmatic Programmer İlkeleri

**DRY Score (Don't Repeat Yourself)**
- Metrik: Kod tekrarı yoğunluğu
- Hesaplama: 1 - (tekrar sayısı / toplam satır)
- Hedef: ≥ 0.90 (90%)

**Orthogonality Score (Bağımsızlık)**
- Metrik: Sınıf bağımlılıkları ve coupling
- Hesaplama: 1 - (normalize edilmiş bağımlılık / toplam sınıf)
- Hedef: ≥ 0.80 (80%)

**Correctness Score (Doğruluk)**
- Metrik: Kod kokuları ve hata yoğunluğu
- Hesaplama: 1 - (ağırlıklı kokular / KLoC)
- Hedef: ≥ 0.85 (85%)

**Performance Score (Performans)**
- Metrik: Karmaşık algoritmalar ve verimsiz kod
- Hesaplama: Yüksek karmaşıklık tespit oranı
- Hedef: ≥ 0.75 (75%)

### Genel Kalite Skoru

**Formül:**
```
Pragmatic Score = (0.30 × DRY) + (0.25 × Orthogonality) +
                  (0.30 × Correctness) + (0.15 × Performance)
```

**Harf Notu Sistemi:**
- **A+**: 95-100 - Mükemmel kalite
- **A**: 90-94 - Çok iyi kalite
- **B**: 80-89 - İyi kalite
- **C**: 70-79 - Orta kalite
- **D**: 60-69 - Kabul edilebilir kalite
- **F**: 0-59 - Yetersiz kalite

---

## Örnek Çıktı

### Konsol Raporu

```
╔═══════════════════════════════════════════════════════════════╗
║                    PRAGMITE ANALIZ RAPORU                    ║
╚═══════════════════════════════════════════════════════════════╝

📊 PROJE İSTATİSTİKLERİ
├─ Toplam Dosya: 14
├─ Toplam Satır: 1,544
├─ Analiz Süresi: 465 ms
└─ Tespit Edilen Kokular: 182

🎯 PRAGMATIC PROGRAMMER SKORLARI
├─ DRY Score: 0.75 (75%) - C
├─ Orthogonality: 0.68 (68%) - D
├─ Correctness: 0.45 (45%) - F
├─ Performance: 0.52 (52%) - F
└─ ⭐ Pragmatic Score: 21/100 (F)

📋 KOD KOKULARI (TOP 10)
┌─────┬─────────────────────┬──────┬─────────────────────────┐
│ #   │ Tür                 │ Adet │ Öncelik                 │
├─────┼─────────────────────┼──────┼─────────────────────────┤
│ 1   │ Magic Number        │  47  │ Orta                    │
│ 2   │ Missing Javadoc     │  31  │ Düşük                   │
│ 3   │ Long Method         │  12  │ Yüksek                  │
│ 4   │ God Class           │   8  │ Kritik                  │
│ 5   │ Deep Nesting        │   6  │ Yüksek                  │
└─────┴─────────────────────┴──────┴─────────────────────────┘

💡 ÖNERİLER
├─ 47 magic number sabit tanımlarına dönüştürülebilir
├─ 12 uzun metot daha küçük parçalara ayrılmalı
├─ 8 God Class Single Responsibility'ye uygun şekilde bölünmeli
└─ 6 derin iç içe blok guard clauses ile basitleştirilebilir
```

### JSON Raporu Örneği

```json
{
  "timestamp": "2025-01-25T14:30:45",
  "projectPath": "/path/to/project",
  "statistics": {
    "totalFiles": 14,
    "totalLines": 1544,
    "analysisTimeMs": 465,
    "totalSmells": 182
  },
  "scores": {
    "dryScore": 0.75,
    "orthogonalityScore": 0.68,
    "correctnessScore": 0.45,
    "performanceScore": 0.52,
    "pragmaticScore": 21,
    "grade": "F"
  },
  "smells": [
    {
      "type": "MAGIC_NUMBER",
      "filePath": "src/main/java/Example.java",
      "line": 42,
      "message": "Sihirli sayı: 3.14159",
      "severity": "MEDIUM",
      "affectedElement": "calculateArea",
      "suggestion": "Bu sayıyı private static final bir sabite dönüştürün",
      "autoFix": true
    }
  ],
  "complexity": {
    "methods": [
      {
        "name": "processOrders",
        "bigO": "O(n²)",
        "cyclomaticComplexity": 15,
        "explanation": "Nested loops detected"
      }
    ]
  }
}
```

---

## Gelecek Özellikler

### 🚧 Planlanan (Faz 3-4)

**Web UI ve IDE Entegrasyonları:**
- Spring Boot tabanlı web dashboard
- VS Code eklentisi (LSP/Code Actions)
- IntelliJ IDEA plugin
- Real-time analiz ve CodeLens entegrasyonu

**Performans Profiling:**
- JFR (Java Flight Recorder) entegrasyonu
- JMH (Java Microbenchmark Harness) otomatik test üretimi
- Hotspot analizi ve flamegraph görselleştirme
- Kanıta dayalı performans önerileri

**Gelişmiş Özellikler:**
- Güvenli otomatik refactoring (Apply Fix butonu)
- Build/test sonrası otomatik revert mekanizması
- CI/CD kalite kapıları (threshold-based)
- Zaman serisinde regresyon alarmı
- Hafif ML tabanlı öneri sıralama

**✅ Faz 2 Tamamlandı (v1.1.0):**
- ✅ Duplicated code (Type-2 clone detection with Jaccard similarity)
- ✅ Data clumps (Parameter pattern analysis)
- ✅ Feature envy (External dependency analysis)
- ✅ Inappropriate intimacy (Class coupling detection)
- ✅ Lazy class (Minimal functionality detection)
- ✅ Speculative generality (Over-abstraction detection)

**✅ Faz 3 Tamamlandı (v1.2.0 - Enterprise Features):**
- ✅ Configuration System (.pragmite.yaml) - Project-specific settings with YAML
- ✅ HTML/PDF Report Export - Professional reports with Chart.js visualization
- ✅ Incremental Analysis (Cache) - 10x faster analysis with SHA-256 file hashing
- ✅ CI/CD Quality Gates - GitHub Actions & GitLab CI integration
- ✅ Parallel analysis (Multi-threaded file processing, configurable threads)
- ✅ JFR (Java Flight Recorder) - Runtime performance profiling & hotspot detection
- ✅ Performance optimization - 2-4x faster on large projects

**✅ Faz 4 (v1.3.0 - Completed):**
- ✅ SQL Database - Historical analysis tracking with SQLite
- ✅ Auto-Fix Infrastructure - Framework for automatic fixes
- ✅ Rollback System - Undo automatic fixes safely with database backups

**✅ Faz 5 (v1.4.0 - Completed):**
- ✅ AI-Powered Error Analysis - Detailed explanations with ready-to-use AI prompts
- ✅ Auto-Refactoring - AI-generated code improvements with Claude API
- ✅ HTML Report Integration - Before/after code comparison in reports

**✅ Phase 3 (v1.5.0 - Completed - December 26, 2025):**
- ✅ Auto-Apply System - Automatically apply AI refactorings to source files
- ✅ File-Based Backup - Timestamped backups with MD5 checksums
- ✅ Compilation Validation - JavaParser syntax checking before apply
- ✅ File-Based Rollback - Rollback auto-applied changes safely
- ✅ Dry-Run Mode - Preview changes without modifying files
- ✅ Automatic Cleanup - Keep last 10 backups per file

**🚀 Phase 4 (v1.6.0+ - Planned):**
- 🎨 VSCode Extension - Interactive sidebar with diff preview (Q1 2026)
- 🌐 Web UI - Browser-based dashboard with Monaco Editor (Q2 2026)
- 🔄 WebSocket API - Real-time progress updates
- 🤝 Interactive Mode - User confirmation before each change
- 📊 Advanced Reporting - JSON output, custom formats
- 🧪 Strict Validation - javac-based semantic validation

📖 **Full Stack UI Plan:** See [docs/FULL_STACK_UI_PLAN.md](docs/FULL_STACK_UI_PLAN.md)

---

## Mimari

### Modül Yapısı

```
pragmite-core/
├── src/main/java/com/pragmite/
│   ├── analyzer/
│   │   ├── ComplexityAnalyzer.java      # Big-O ve cyclomatic complexity
│   │   └── ProjectAnalyzer.java         # Ana analiz orkestratörü
│   ├── model/
│   │   ├── CodeSmell.java              # Kod kokusu model
│   │   ├── CodeSmellType.java          # Koku tipleri enum
│   │   ├── MethodInfo.java             # Metot bilgileri
│   │   ├── QualityScore.java           # Kalite skorları
│   │   └── PragmaticPrinciple.java     # Pragmatic ilkeler
│   ├── rules/
│   │   ├── SmellDetector.java          # Dedektör interface
│   │   ├── RuleEngine.java             # Kural motoru
│   │   └── smells/                     # 15+ dedektör implementasyonu
│   ├── scoring/
│   │   └── ScoreCalculator.java        # Skor hesaplayıcı
│   ├── output/
│   │   ├── ConsoleReportWriter.java    # Konsol raporu
│   │   └── JsonReportWriter.java       # JSON raporu
│   └── cli/
│       └── PragmiteCLI.java            # CLI arayüzü (Picocli)
└── pom.xml
```

### Teknolojiler

- **Java 21** - Record, Pattern Matching, Virtual Threads
- **JavaParser 3.25.5** - AST analizi ve kod parsing
- **Picocli 4.7.5** - CLI framework
- **Gson 2.10.1** - JSON serialization
- **Maven** - Build tool

### Geliştirme ve Test

**Test projesi ile denemek:**
```bash
cd pragmite-core
mvn clean package
java -jar target/pragmite-core-1.0.0.jar ../test-ecommerce
```

**Unit testleri çalıştır:**
```bash
cd pragmite-core
mvn test
```

Test projesi: `test-ecommerce/` - Kasıtlı olarak kod kokuları içeren örnek e-ticaret uygulaması (14 dosya, 1,544 satır)

**Test Sonuçları:**
- ✅ **46 unit test** - Tümü geçiyor (%100 başarı)
- ✅ **ComplexityAnalyzer** - 8/8 test geçti (Binary search O(log n), Fibonacci O(2^n))
- ✅ **MagicNumberDetector** - 9/9 test geçti (Hex, Binary, Octal, Float desteği)
- ✅ **UnusedImportDetector** - 10/10 test geçti (Inner class, Annotation, Generic desteği)
- ✅ **DuplicateCodeDetector** - 3/3 test geçti (Type-2 clone detection)
- ✅ **DataClumpsDetector** - 3/3 test geçti (Parameter pattern detection)
- ✅ **FeatureEnvyDetector** - 3/3 test geçti (External dependency detection)
- ✅ **LazyClassDetector** - 4/4 test geçti (Minimal class detection)
- ✅ **SpeculativeGeneralityDetector** - 6/6 test geçti (Over-abstraction detection)

---

## Katkı ve Lisans

**Katkı Yapmak:**
- Issue açın veya pull request gönderin
- Her yeni dedektör için test case'ler ekleyin
- Kod stiline uygun şekilde geliştirme yapın

**Lisans:**
- Apache-2.0 (Açık Kaynak)

---

## İletişim

**Sorularınız için:**
- GitHub Issues: Hata bildirimleri ve özellik istekleri
- Dokümantasyon: Bu README dosyası

---

**Not:** Bu proje aktif geliştirme aşamasındadır. Gelecek sürümlerde web UI, IDE entegrasyonları ve performans profiling özellikleri eklenecektir.

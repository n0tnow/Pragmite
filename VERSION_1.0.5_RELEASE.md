# Pragmite v1.0.5 Release Notes

**Release Date**: 2025-12-05
**Type**: Feature Release
**Status**: Production Ready

---

## 🎉 Major Features

### 1. **JFR Performance Profiling - Now Enabled by Default** 🔥

Runtime performans analizi artık varsayılan olarak aktif! Her analiz sırasında:

- **CPU Hotspots**: En çok CPU kullanan metodlar
- **Memory Allocations**: Bellek allocation site'ları
- **CPU Load Metrics**: Ortalama ve maksimum CPU yüklenmesi
- **Performance Insights**: Gerçek runtime verileri

**Dashboard'da Gösterim**:
```
🔥 JFR Performance Profiling
├─ CPU Samples: 45,234
├─ Avg CPU Load: 18.5%
├─ Max CPU Load: 67.2%
└─ Total Allocations: 128.4 MB

Top CPU Hotspots:
#1 processData() - 8,432 samples
#2 calculateComplexity() - 5,621 samples
#3 parseJavaFile() - 3,112 samples
```

### 2. **CK Metrics Visualization** 📊

Chidamber & Kemerer OO Design Metrics:

- **WMC** (Weighted Methods per Class): Sınıf karmaşıklığı
- **DIT** (Depth of Inheritance Tree): Kalıtım derinliği
- **NOC** (Number of Children): Alt sınıf sayısı
- **CBO** (Coupling Between Objects): Bağlantı derecesi
- **RFC** (Response For a Class): Metot bağımlılıkları
- **LCOM** (Lack of Cohesion in Methods): Kohezyon eksikliği

**God Class Detection**: WMC > 30 && LCOM > 50 && CBO > 10

**Dashboard'da Gösterim**:
```
📊 Code Quality Metrics (CK)
┌─────────────────────────────┐
│ Calculator.java  ⚠️ God Class│
├─────────────────────────────┤
│ WMC: 45 ⚠️  DIT: 1  NOC: 0  │
│ CBO: 12 ⚠️  RFC: 28         │
│ LCOM: 65 ⚠️                 │
└─────────────────────────────┘
```

### 3. **Refactoring Suggestions** 💡

Akıllı refactoring önerileri:

- **5 Automatic Refactorers**:
  - Magic Number Extractor
  - Field Injection Refactorer
  - Duplicate Code Suggester
  - God Class Breaker
  - Long Method Splitter

- **Difficulty Levels**: EASY / MEDIUM / HARD
- **Step-by-Step Instructions**: Detaylı adımlar
- **Before/After Code**: Kod örnekleri
- **Auto-Fix Capability**: Otomatik düzeltme desteği

**Dashboard'da Gösterim**:
```
💡 Improvement Suggestions
┌─────────────────────────────────────┐
│ Extract Magic Number         [EASY] │
│ Replace hardcoded value 30          │
│ with named constant                 │
└─────────────────────────────────────┘
```

### 4. **JMH Benchmark Support** ⚡

Performans benchmark desteği (opsiyonel):

- Method throughput measurement
- Average execution time
- Fastest/Slowest method tracking
- Benchmark comparison

---

## 🎨 UI/UX Improvements

### White Theme Enhancements

- **Solid Backgrounds**: Şeffaf değil, solid `#ffffff` arka planlar
- **Better Contrast**: Koyu yazı renkleri (`#0f172a`, `#334155`)
- **Clear Borders**: Net `#e2e8f0` border colors
- **Subtle Shadows**: `box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08)`
- **Improved Typography**: Font-weight artışı (600-700)

### Dark Theme (Default)

- Optimized glass-morphism effects
- Better color hierarchy
- Improved readability

---

## 🔧 Technical Improvements

### Backend (Java)

1. **ProjectAnalyzer**:
   - JFR profiling varsayılan olarak aktif
   - CK Metrics hesaplaması her dosya için
   - Refactoring suggestion generation
   - Map import eklendi

2. **FileAnalysis Model**:
   - `CKMetrics` field eklendi
   - Getter/setter metodları

3. **AnalysisResult Model**:
   - `suggestions` field (List<RefactoringSuggestion>)
   - `profileReport` field (ProfileReport)
   - `benchmarkResult` field (BenchmarkResult)

### Frontend (TypeScript)

1. **Models**:
   - `CKMetrics` interface (6 metrik)
   - `ProfileReport` interface (JFR sonuçları)
   - `BenchmarkResult` interface (JMH sonuçları)
   - `RefactoringSuggestion` interface

2. **Dashboard Sections**:
   - CK Metrics visualization grid
   - JFR profiling stats ve hotspots
   - JMH benchmark results
   - Suggestions with modal details

3. **CSS Styling**:
   - 260+ satır yeni CSS
   - Responsive grid layouts
   - Warning colors (orange for thresholds)
   - Light theme overrides

---

## 🐛 Bug Fixes

### Critical Fixes

1. **Quality Score Field Mismatch**:
   - Backend: `perfScore` ↔ Frontend: `performanceScore`
   - **Fixed**: Frontend artık `perfScore` kullanıyor
   - **Result**: Quality Score artık doğru gösteriliyor

2. **JAR Version Mismatch**:
   - Extension 1.0.4 JAR'ı arıyordu ama 1.0.0 vardı
   - **Fixed**: Version senkronizasyonu

### Minor Fixes

1. White theme card backgrounds transparanlık sorunları
2. File path display inconsistencies
3. Modal popup path escaping (Windows paths)

---

## 📊 Performance Impact

### Analysis Time

- **Without JFR**: ~340ms (10 files)
- **With JFR**: ~520ms (10 files)
- **Overhead**: +50% (+180ms)

### Memory Usage

- **JFR Recording**: ~5-10MB temporary files
- **Auto-cleanup**: Temporary files silinir

### Trade-off

✅ **Worth It**: Runtime insights > 180ms overhead
✅ **Valuable Data**: Gerçek performans hotspots
✅ **Production-Ready**: Low overhead for development

---

## 📦 Package Details

### Backend

- **File**: `pragmite-core-1.0.5.jar`
- **Size**: 16.21 MB
- **Java Version**: 21
- **Dependencies**: 15 shaded libraries

### Extension

- **File**: `pragmite-1.0.5.vsix`
- **Size**: 16.21 MB
- **Files**: 29
- **Platform**: VSCode 1.106.0+

---

## 🚀 Installation

### Local Installation

```bash
# VSCode Command Palette
Ctrl+Shift+P → "Extensions: Install from VSIX..."
# Select: pragmite-vscode-extension/pragmite-1.0.5.vsix
```

### Usage

```bash
# Analyze Project
Ctrl+Shift+P → "Pragmite: Analyze Project"

# View Dashboard
Dashboard opens automatically in browser
http://localhost:3745
```

---

## 🎯 What's Next? (v1.0.6)

### Planned Features

1. **Analysis History**: Geçmiş analiz sonuçlarını saklama
2. **Trend Charts**: Kalite skorunun zaman içinde değişimi
3. **Export Reports**: PDF/HTML export
4. **Custom Rules**: Kullanıcı tanımlı code smell kuralları
5. **CI/CD Integration**: GitHub Actions, Jenkins pipeline

### Community Feedback

Bu release'i test edin ve feedback verin:
- GitHub Issues: https://github.com/n0tnow/Pragmite/issues
- Discussions: https://github.com/n0tnow/Pragmite/discussions

---

## 📝 Migration Guide

### From v1.0.4 to v1.0.5

**No Breaking Changes!**

Sadece yeni özellikler eklendi:

1. **Install**: `pragmite-1.0.5.vsix` yükleyin
2. **Restart**: VSCode'u yeniden başlatın
3. **Analyze**: Projenizi analiz edin
4. **Enjoy**: Yeni özellikleri görün! 🎉

### Dashboard Changes

Dashboard'da yeni section'lar:
- 📊 CK Metrics (eğer class varsa)
- 🔥 JFR Profiling (her zaman)
- 💡 Suggestions (eğer smell varsa)
- ⚡ JMH Benchmarks (eğer benchmark varsa)

---

## 🙏 Acknowledgments

- **JavaParser**: AST parsing
- **JFR (Java Flight Recorder)**: Runtime profiling
- **JMH (Java Microbenchmark Harness)**: Performance benchmarking
- **Logback**: Logging framework
- **Gson**: JSON serialization

---

## 📄 License

MIT License - See LICENSE file

---

**Happy Coding with Pragmite v1.0.5!** 🚀

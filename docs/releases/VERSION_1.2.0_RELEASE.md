# 🚀 Pragmite v1.2.0 Release Notes

**Release Date:** December 25, 2025
**Type:** Major Feature Release - Configuration & CI/CD
**Codename:** "Project Customization"

---

## 📋 Overview

Version 1.2.0 brings two critical features that make Pragmite production-ready for enterprise use:
1. **Configuration System** - Project-specific customization via `.pragmite.yaml`
2. **CI/CD Integration** - Quality gates for automated pipelines

This release transforms Pragmite from a standalone analysis tool to a fully configurable, CI/CD-ready quality gate system.

---

## ✨ Major Features

### 1. Configuration System (.pragmite.yaml) ✅

**Problem:** Users couldn't customize thresholds, exclude patterns, or quality weights per project.

**Solution:** Complete YAML-based configuration system with inheritance and CLI overrides.

#### Features:
- **Custom Thresholds:** Override any detector threshold
- **Exclude Patterns:** Glob-based file/directory exclusion
- **Severity Overrides:** Customize severity levels per detector
- **Quality Weights:** Adjust DRY/Orthogonality/Correctness/Performance weights
- **Analysis Options:** Incremental analysis, parallel processing, report formats

#### Configuration Example:
```yaml
# .pragmite.yaml
thresholds:
  cyclomaticComplexity: 20      # Project-specific threshold
  longMethod: 60                # Increased for data processing code
  largeClass.lines: 500         # Larger classes accepted

exclude:
  - "**/generated/**"           # Exclude generated code
  - "**/vendor/**"              # Exclude third-party libraries

qualityWeights:
  dry: 0.40                     # Higher weight on DRY
  orthogonality: 0.30
  correctness: 0.20
  performance: 0.10

analysis:
  parallel: true
  maxThreads: 16
  reportFormat: json
```

#### CLI Integration:
```bash
# Generate template
java -jar pragmite-core.jar --generate-config

# Use custom config
java -jar pragmite-core.jar --config custom-pragmite.yaml

# CLI overrides config (precedence: CLI > config file > defaults)
java -jar pragmite-core.jar --complexity-threshold 25
```

#### Configuration Hierarchy:
1. **Hardcoded defaults** (in PragmiteConfig.java)
2. `.pragmite-defaults.yaml` (if exists - for organization-wide standards)
3. `.pragmite.yaml` (project-specific)
4. **CLI flags** (highest precedence)

---

### 2. CI/CD Integration (Quality Gate) ✅

**Problem:** No way to fail builds based on code quality metrics.

**Solution:** Complete quality gate system with exit codes and CI/CD examples.

#### Features:
- **Exit Codes:**
  - `0` = Quality gate passed
  - `1` = Quality gate failed (quality issues)
  - `2` = Analysis error (tool failure)

- **Quality Gate Flags:**
  - `--fail-on-critical` - Fail if any critical issues found
  - `--min-quality-score N` - Fail if quality score < N (0-100)
  - `--max-critical-issues N` - Fail if critical issues > N

- **CI-Friendly Output:**
```
✅ Quality Gate: PASSED
   Quality Score: 85/100
   Critical Issues: 0
```

```
❌ Quality Gate: FAILED
   Reason: Quality score 65 < minimum 70
```

#### GitHub Actions Example:
```yaml
name: Pragmite Quality Gate

on:
  pull_request:
    branches: [ main ]

jobs:
  code-quality:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with:
          java-version: '21'

      - name: Run Pragmite
        run: |
          java -jar pragmite-core.jar \
            --fail-on-critical \
            --min-quality-score 70 \
            --max-critical-issues 0 \
            --format json \
            --output pragmite-report.json

      - name: Upload Report
        uses: actions/upload-artifact@v4
        with:
          name: pragmite-report
          path: pragmite-report.json
```

#### GitLab CI Example:
```yaml
pragmite-analysis:
  stage: quality
  image: eclipse-temurin:21-jdk
  script:
    - |
      java -jar pragmite-core.jar \
        --fail-on-critical \
        --min-quality-score 70 \
        --format json
  artifacts:
    reports:
      codequality: pragmite-report.json
```

---

## 🏗️ Architecture

### New Components:

#### 1. PragmiteConfig.java
```java
public class PragmiteConfig {
    private Map<String, Integer> thresholds;
    private List<String> excludePatterns;
    private Map<String, String> severityOverrides;
    private QualityWeights qualityWeights;
    private AnalysisOptions analysisOptions;

    // Glob pattern matching for excludes
    public boolean isExcluded(String filePath);

    // Merge configurations (supports inheritance)
    public void merge(PragmiteConfig other);
}
```

#### 2. ConfigLoader.java
```java
public class ConfigLoader {
    // Load from project root (searches for .pragmite.yaml)
    public static PragmiteConfig load(Path projectRoot);

    // Load from specific file
    public static PragmiteConfig loadFromFile(Path configPath);

    // Generate template file
    public static void createTemplate(Path outputPath);

    // Validation with helpful error messages
    private static void validateConfig(PragmiteConfig config);
}
```

#### 3. Enhanced PragmiteCLI.java
```java
@Command(name = "pragmite", version = "1.2.0")
public class PragmiteCLI {
    // New configuration options
    @Option(names = {"--config"})
    private File configFile;

    @Option(names = {"--generate-config"})
    private boolean generateConfig;

    // CI/CD quality gate options
    @Option(names = {"--fail-on-critical"})
    private boolean failOnCritical;

    @Option(names = {"--min-quality-score"})
    private Integer minQualityScore;

    @Option(names = {"--max-critical-issues"})
    private Integer maxCriticalIssues;

    // Quality gate logic
    private int checkQualityGate(AnalysisResult result, PragmiteConfig config);
}
```

---

## 📊 Technical Details

### Configuration File Format:

**YAML Schema:**
```yaml
# Thresholds section (all optional)
thresholds:
  cyclomaticComplexity: int
  longMethod: int
  largeClass.lines: int
  largeClass.methods: int
  longParameterList: int
  deepNesting: int
  switchStatement: int
  tooManyLiterals.numeric: int
  tooManyLiterals.string: int
  lazyClass: int

# Exclude patterns (glob)
exclude:
  - string (glob pattern)

# Severity overrides
severity:
  DETECTOR_NAME: "ERROR|WARNING|INFO"

# Quality score weights (must sum to 1.0)
qualityWeights:
  dry: float
  orthogonality: float
  correctness: float
  performance: float

# Analysis options
analysis:
  incremental: boolean
  parallel: boolean
  maxThreads: int
  reportFormat: "json|html|pdf|both"
  failOnCritical: boolean
  minQualityScore: int
  maxCriticalIssues: int
```

### Validation Rules:
1. Quality weights must sum to 1.0 (±0.001 tolerance)
2. All thresholds must be positive integers
3. Report format must be: `json`, `html`, `pdf`, or `both`
4. Exclude patterns are glob expressions (`**/*.java`, `**/target/**`)

### Glob Pattern Support:
- `*` - Matches any characters except `/`
- `**` - Matches any path (including nested directories)
- `?` - Matches single character
- Examples:
  - `**/target/**` → Exclude all target directories
  - `**/*Test.java` → Exclude all test files
  - `src/main/**/*.java` → Include only main source files

---

## 📦 Deliverables

### Core Library
- **File:** `pragmite-core-1.2.0.jar`
- **Size:** 9.0 MB
- **Platform:** Java 21+
- **Dependencies:** All bundled (shaded JAR)
- **New Features:**
  - Configuration system
  - CI/CD quality gates
  - Template generator

### Configuration Template
- **File:** `.pragmite.yaml.template` (embedded in JAR)
- **Location:** `src/main/resources/.pragmite.yaml.template`
- **Usage:** `java -jar pragmite-core.jar --generate-config`

### CI/CD Examples
- **GitHub Actions:** `.github/workflows/pragmite-quality-gate.yml`
- **GitLab CI:** `.gitlab-ci-example.yml`

---

## 🔄 Migration Guide

### From 1.1.0 → 1.2.0

**No Breaking Changes!** Fully backward compatible.

#### 1. Update JAR:
```bash
# Download new version
wget https://github.com/n0tnow/Pragmite/releases/latest/download/pragmite-core.jar

# Or use Maven dependency
<dependency>
    <groupId>com.pragmite</groupId>
    <artifactId>pragmite-core</artifactId>
    <version>1.2.0</version>
</dependency>
```

#### 2. Optional: Generate Config
```bash
cd your-project
java -jar pragmite-core.jar --generate-config

# Edit .pragmite.yaml to customize thresholds
```

#### 3. Optional: Add to CI/CD
```yaml
# Add to .github/workflows/quality.yml
- name: Quality Gate
  run: java -jar pragmite-core.jar --min-quality-score 70
```

#### 4. Expected Changes:
- **Config file support** - Now reads `.pragmite.yaml` if exists
- **New CLI flags** - `--fail-on-critical`, `--min-quality-score`, etc.
- **Quality gate output** - Shows pass/fail summary

---

## 💡 Usage Examples

### Example 1: Custom Thresholds for Data Processing Project
```yaml
# .pragmite.yaml
thresholds:
  cyclomaticComplexity: 25      # Complex algorithms acceptable
  longMethod: 80                # Data transformations can be long
  largeClass.lines: 600         # Service classes can be large

exclude:
  - "**/generated/**"
  - "**/proto/**"
```

### Example 2: Strict Quality Gate for Core Library
```yaml
# .pragmite.yaml
thresholds:
  cyclomaticComplexity: 10      # Strict complexity limit
  longMethod: 30                # Short methods enforced

analysis:
  failOnCritical: true
  minQualityScore: 90           # Very high bar
  maxCriticalIssues: 0          # Zero tolerance
```

### Example 3: Weighted Towards Correctness
```yaml
# .pragmite.yaml - Financial system
qualityWeights:
  dry: 0.20                     # Less important
  orthogonality: 0.20
  correctness: 0.50             # Critical!
  performance: 0.10
```

---

## 🔧 Implementation Details

### Files Created:
1. **PragmiteConfig.java** (332 lines)
   - Configuration data model
   - Nested classes: QualityWeights, AnalysisOptions
   - Glob pattern matching
   - Configuration merging

2. **ConfigLoader.java** (296 lines)
   - YAML parsing (SnakeYAML 2.2)
   - File discovery (.pragmite.yaml)
   - Inheritance support (.pragmite-defaults.yaml)
   - Validation with helpful errors
   - Template generation

### Files Modified:
1. **PragmiteCLI.java** (+150 lines)
   - Config loading logic
   - New CLI flags (8 new options)
   - Quality gate checking
   - Template generation command

2. **pom.xml**
   - Version: 1.1.0 → 1.2.0
   - (SnakeYAML already present)

---

## 📚 Documentation

### New Documents:
- [VERSION_1.2.0_RELEASE.md](VERSION_1.2.0_RELEASE.md) - This document
- [.github/workflows/pragmite-quality-gate.yml](.github/workflows/pragmite-quality-gate.yml) - GitHub Actions example
- [.gitlab-ci-example.yml](.gitlab-ci-example.yml) - GitLab CI example
- [.pragmite.yaml.template](../../pragmite-core/src/main/resources/.pragmite.yaml.template) - Configuration template

### Updated Documents:
- [CRITICAL_FEATURES_TRACKER.md](../../CRITICAL_FEATURES_TRACKER.md) - Marked features #1 and #2 as complete

### Existing Documentation:
- [VERSION_1.1.0_RELEASE.md](VERSION_1.1.0_RELEASE.md) - v1.1.0 release notes
- [PRAGMITE_MATHEMATICAL_ANALYSIS.md](../../PRAGMITE_MATHEMATICAL_ANALYSIS.md) - Mathematical formulas

---

## 🎯 What's Next?

### Remaining Critical Features (v1.3.0):

#### 3. HTML/PDF Report Export (Planned)
- Professional HTML reports with charts
- PDF generation for stakeholders
- `--format html` / `--format pdf` flags

#### 4. Incremental Analysis (Planned)
- Cache-based analysis (10x faster)
- Git integration for changed files
- `--incremental` flag

### Future Enhancements (v2.0):
- Historical trend tracking
- IntelliJ IDEA plugin
- Kotlin support
- AI-powered refactoring suggestions

---

## 🐛 Known Issues

None reported for v1.2.0.

---

## 📞 Support

- **Issues:** https://github.com/n0tnow/Pragmite/issues
- **Documentation:** https://github.com/n0tnow/Pragmite#readme
- **Discussions:** https://github.com/n0tnow/Pragmite/discussions

---

## 📄 License

MIT License - See [LICENSE](../../LICENSE)

---

## 🙏 Acknowledgments

- **SnakeYAML** - YAML parsing library
- **PicoCLI** - Command-line interface framework
- **CI/CD Community** - Inspiration for quality gate features

---

## 📥 Download Links

- [pragmite-core-1.2.0.jar](../../pragmite-core/target/pragmite-core-1.2.0.jar) - 9.0 MB
- [GitHub Release](https://github.com/n0tnow/Pragmite/releases/tag/v1.2.0)

**Full Changelog:** [v1.1.0...v1.2.0](https://github.com/n0tnow/Pragmite/compare/v1.1.0...v1.2.0)

---

**Summary:** v1.2.0 brings project-specific configuration and CI/CD quality gates, making Pragmite production-ready for enterprise use. 50% of critical features now complete! 🎯

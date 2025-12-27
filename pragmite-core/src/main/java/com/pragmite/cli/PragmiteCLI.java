package com.pragmite.cli;

import com.pragmite.analyzer.ProjectAnalyzer;
import com.pragmite.config.ConfigLoader;
import com.pragmite.config.PragmiteConfig;
import com.pragmite.model.AnalysisResult;
import com.pragmite.model.CodeSmellType;
import com.pragmite.model.CodeSmell;
import com.pragmite.output.JsonReportWriter;
import com.pragmite.output.ConsoleReportWriter;
import com.pragmite.report.HtmlReportGenerator;
import com.pragmite.cache.CacheManager;
import com.pragmite.database.DatabaseManager;
import com.pragmite.autofix.AutoFixEngine;
import com.pragmite.autofix.FixOptions;
import com.pragmite.autofix.FixResult;
import com.pragmite.autofix.RollbackManager;
import com.pragmite.ai.AnalysisEngine;
import com.pragmite.ai.AIAnalysisResult;
import com.pragmite.websocket.ProgressWebSocketServer;
import com.pragmite.validation.JavacValidator;
import com.pragmite.validation.ValidationResult;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.File;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;

/**
 * Pragmite CLI - Java kod kalitesi analiz aracı.
 */
@Command(
    name = "pragmite",
    mixinStandardHelpOptions = true,
    version = "Pragmite 1.4.0",
    description = "Java kod kalitesi ve karmaşıklık analizi aracı"
)
public class PragmiteCLI implements Callable<Integer> {

    @Parameters(index = "0", description = "Analiz edilecek proje dizini", defaultValue = ".")
    private File projectDir;

    @Option(names = {"-o", "--output"}, description = "JSON rapor çıktı dosyası")
    private File outputFile;

    @Option(names = {"-f", "--format"}, description = "Çıktı formatı: console, json, both", defaultValue = "console")
    private String format;

    // v1.6.0 - Enhanced JSON Output
    @Option(names = {"--output-format"}, description = "Enhanced output format: standard, enhanced-json (with diff data)")
    private String outputFormat;

    @Option(names = {"--complexity-threshold"}, description = "Cyclomatic complexity eşik değeri", defaultValue = "10")
    private int complexityThreshold;

    @Option(names = {"--method-length"}, description = "Maksimum metot uzunluğu", defaultValue = "30")
    private int maxMethodLength;

    @Option(names = {"--param-count"}, description = "Maksimum parametre sayısı", defaultValue = "4")
    private int maxParamCount;

    @Option(names = {"-v", "--verbose"}, description = "Ayrıntılı çıktı")
    private boolean verbose;

    @Option(names = {"--include"}, description = "Dahil edilecek dosya pattern (glob)", defaultValue = "**/*.java")
    private String includePattern;

    @Option(names = {"--exclude"}, description = "Hariç tutulacak dizinler (virgülle ayrılmış)")
    private String excludeDirs;

    @Option(names = {"--config"}, description = "Yapılandırma dosyası yolu (.pragmite.yaml)")
    private File configFile;

    @Option(names = {"--fail-on-critical"}, description = "Kritik sorun varsa 1 ile çık")
    private boolean failOnCritical;

    @Option(names = {"--min-quality-score"}, description = "Minimum kalite skoru (0-100)")
    private Integer minQualityScore;

    @Option(names = {"--max-critical-issues"}, description = "Maksimum kritik sorun sayısı")
    private Integer maxCriticalIssues;

    @Option(names = {"--generate-config"}, description = "Örnek .pragmite.yaml dosyası oluştur ve çık")
    private boolean generateConfig;

    @Option(names = {"--incremental"}, description = "Sadece değiş dosyaları analiz et (cache kullan)")
    private boolean incrementalAnalysis;

    @Option(names = {"--clear-cache"}, description = "Analiz cache'ini temizle ve çık")
    private boolean clearCache;

    // v1.3.0 - Auto-Fix Options
    @Option(names = {"--apply-fixes"}, description = "Tüm otomatik düzeltmeleri uygula")
    private boolean applyFixes;

    @Option(names = {"--fix-type"}, description = "Sadece belirtilen tipteki sorunları düzelt (virgülle ayrılmış)")
    private String fixTypes;

    @Option(names = {"--dry-run"}, description = "Düzeltmeleri önizle, uygulamadan göster")
    private boolean dryRun;

    @Option(names = {"--no-backup"}, description = "Düzeltme sırasında yedek oluşturma")
    private boolean noBackup;

    // v1.3.0 - Database Options
    @Option(names = {"--save-to-db"}, description = "Analiz sonuçlarını veritabanına kaydet")
    private boolean saveToDb;

    @Option(names = {"--show-history"}, description = "Son N analiz sonucunu göster")
    private Integer showHistory;

    @Option(names = {"--show-trend"}, description = "Son N günün kalite trendini göster")
    private Integer showTrend;

    // v1.3.0 - Rollback Options
    @Option(names = {"--rollback-last"}, description = "En son düzeltme işlemini geri al")
    private boolean rollbackLast;

    @Option(names = {"--rollback"}, description = "Belirtilen ID'li düzeltme işlemini geri al")
    private Long rollbackId;

    @Option(names = {"--rollback-file"}, description = "Belirtilen dosyadaki tüm düzeltmeleri geri al")
    private String rollbackFile;

    @Option(names = {"--list-rollbacks"}, description = "Geri alınabilir düzeltme işlemlerini listele")
    private boolean listRollbacks;

    // v1.5.0 - File-Based Rollback Options (Phase 3)
    @Option(names = {"--list-backups"}, description = "List all file-based backups for auto-apply operations")
    private boolean listBackups;

    @Option(names = {"--list-backups-for"}, description = "List backups for a specific file")
    private String listBackupsFor;

    @Option(names = {"--rollback-file-backup"}, description = "Rollback file to latest auto-apply backup")
    private String rollbackFileBackup;

    // v1.4.0 - AI-Powered Error Analysis Options
    @Option(names = {"--generate-ai-prompts"}, description = "Generate AI-powered analysis with prompts for each issue")
    private boolean generateAiPrompts;

    @Option(names = {"--ai-output"}, description = "AI analysis output file (JSON format)")
    private File aiOutputFile;

    @Option(names = {"--auto-refactor"}, description = "Automatically generate refactored code using Claude API")
    private boolean autoRefactor;

    @Option(names = {"--claude-api-key"}, description = "Claude API key (or use CLAUDE_API_KEY environment variable)")
    private String claudeApiKey;

    // v1.5.0 - Auto-Apply Options (Phase 3)
    @Option(names = {"--auto-apply"}, description = "Automatically apply AI-generated refactored code to source files")
    private boolean autoApply;

    // v1.6.0 - Interactive Mode (Phase 4)
    @Option(names = {"--interactive"}, description = "Interactive mode: ask for confirmation before applying each change")
    private boolean interactive;

    // v1.6.2 - WebSocket Real-Time Progress (Phase 4, Sprint 3)
    @Option(names = {"--websocket"}, description = "Enable WebSocket server for real-time progress updates")
    private boolean enableWebSocket;

    @Option(names = {"--websocket-port"}, description = "WebSocket server port", defaultValue = "8765")
    private int websocketPort;

    // v1.6.3 - Strict Validation (Phase 4, Sprint 4)
    @Option(names = {"--strict-validation"}, description = "Enable strict validation using javac compiler")
    private boolean strictValidation;

    @Override
    public Integer call() throws Exception {
        // Initialize database connection if needed
        DatabaseManager dbManager = null;
        Connection dbConnection = null;

        if (saveToDb || showHistory != null || showTrend != null ||
            rollbackLast || rollbackId != null || rollbackFile != null || listRollbacks) {
            try {
                dbManager = new DatabaseManager();
                dbManager.init(projectDir.toPath());
                String dbUrl = "jdbc:sqlite:" + projectDir.toPath().resolve(".pragmite.db").toAbsolutePath();
                dbConnection = DriverManager.getConnection(dbUrl);
            } catch (Exception e) {
                System.err.println("⚠️  Database error: " + e.getMessage());
                if (verbose) {
                    e.printStackTrace();
                }
            }
        }

        // v1.6.2 - WebSocket server instance
        ProgressWebSocketServer websocketServer = null;

        try {
            // v1.6.2 - Start WebSocket server if enabled
            if (enableWebSocket) {
                try {
                    websocketServer = new ProgressWebSocketServer(websocketPort);
                    websocketServer.start();
                    System.out.println("🌐 WebSocket server started on port " + websocketPort);
                } catch (Exception e) {
                    System.err.println("⚠️  Failed to start WebSocket server: " + e.getMessage());
                    if (verbose) {
                        e.printStackTrace();
                    }
                }
            }

            // Generate config template if requested
            if (generateConfig) {
                Path templatePath = projectDir.toPath().resolve(".pragmite.yaml");
                ConfigLoader.createTemplate(templatePath);
                System.out.println("✅ Configuration template created: " + templatePath.toAbsolutePath());
                System.out.println("📝 Edit this file to customize Pragmite for your project.");
                return 0;
            }

            // Clear cache if requested
            if (clearCache) {
                CacheManager cacheManager = new CacheManager(projectDir.toPath());
                cacheManager.clearCache();
                System.out.println("✅ Analysis cache cleared");
                return 0;
            }

            // Handle rollback operations (v1.3.0 - database-based)
            if (dbConnection != null && (rollbackLast || rollbackId != null || rollbackFile != null || listRollbacks)) {
                return handleRollbackOperations(dbConnection);
            }

            // Handle file-based rollback operations (v1.5.0 - auto-apply backups)
            if (listBackups || listBackupsFor != null || rollbackFileBackup != null) {
                return handleFileBackupOperations();
            }

            // Handle history/trend display
            if (dbManager != null && (showHistory != null || showTrend != null)) {
                return handleHistoryDisplay(dbManager);
            }

            if (!projectDir.exists() || !projectDir.isDirectory()) {
                System.err.println("Hata: Geçersiz proje dizini: " + projectDir.getAbsolutePath());
                return 1;
            }

            // Load configuration
            PragmiteConfig config;
            if (configFile != null) {
                config = ConfigLoader.loadFromFile(configFile.toPath());
                if (verbose) {
                    System.out.println("📋 Loaded config from: " + configFile.getAbsolutePath());
                }
            } else {
                config = ConfigLoader.load(projectDir.toPath());
            }

            // Override config with CLI flags (CLI takes precedence)
            applyCliOverrides(config);

            System.out.println("╔══════════════════════════════════════════════════════════════╗");
            System.out.println("║                    PRAGMITE ANALYZER v1.4.0                  ║");
            System.out.println("║          Java Kod Kalitesi ve Karmaşıklık Analizi            ║");
            System.out.println("╚══════════════════════════════════════════════════════════════╝");
            System.out.println();

            if (verbose) {
                System.out.println("Proje dizini: " + projectDir.getAbsolutePath());
                System.out.println("Eşik değerleri:");
                System.out.println("  - Cyclomatic Complexity: " + config.getThreshold("cyclomaticComplexity", 15));
                System.out.println("  - Maksimum Metot Uzunluğu: " + config.getThreshold("longMethod", 50));
                System.out.println("  - Maksimum Parametre Sayısı: " + config.getThreshold("longParameterList", 5));
                System.out.println("Exclude patterns: " + config.getExcludePatterns());
                System.out.println();
            }

            System.out.println("Analiz başlatılıyor...");

            // Analyzer oluştur ve çalıştır
            ProjectAnalyzer analyzer = new ProjectAnalyzer(projectDir.toPath());
            AnalysisResult result = analyzer.analyze();

            // v1.4.0: Generate AI analysis if requested (do this before reporting so HTML can include it)
            // v1.5.0: Also run AI analysis if auto-refactor or auto-apply is enabled
            List<AIAnalysisResult> aiResults = null;
            if (generateAiPrompts || autoRefactor || autoApply) {
                aiResults = handleAiAnalysis(result);
            }

            // Çıktı formatına göre rapor yaz
            switch (format.toLowerCase()) {
                case "json":
                    writeJsonReport(result);
                    break;
                case "html":
                    writeHtmlReport(result, aiResults);
                    break;
                case "pdf":
                    writeHtmlReport(result, aiResults); // PDF is HTML-based for now
                    System.out.println("📄 PDF generation via HTML report (print to PDF from browser)");
                    break;
                case "both":
                    writeConsoleReport(result);
                    writeJsonReport(result);
                    writeHtmlReport(result, aiResults);
                    break;
                case "console":
                default:
                    writeConsoleReport(result);
                    break;
            }

            // v1.3.0: Apply auto-fixes if requested
            if (applyFixes || fixTypes != null) {
                handleAutoFix(result, dbConnection);
            }

            // v1.3.0: Save to database if requested
            if (saveToDb && dbManager != null) {
                try {
                    long runId = dbManager.saveAnalysisRun(result);
                    System.out.println("\n💾 Analysis saved to database (ID: " + runId + ")");
                } catch (Exception e) {
                    System.err.println("⚠️  Failed to save to database: " + e.getMessage());
                }
            }

            // Apply quality gate checks
            return checkQualityGate(result, config);

        } catch (Exception e) {
            System.err.println("Analiz sırasında hata oluştu: " + e.getMessage());
            if (verbose) {
                e.printStackTrace();
            }
            return 2; // Exit code 2 = analysis error
        } finally {
            // v1.6.2 - Stop WebSocket server
            if (websocketServer != null) {
                try {
                    websocketServer.stop();
                    System.out.println("🛑 WebSocket server stopped");
                } catch (Exception e) {
                    // Ignore
                }
            }

            // Close database connection
            if (dbManager != null) {
                try {
                    dbManager.close();
                } catch (Exception e) {
                    // Ignore
                }
            }
            if (dbConnection != null) {
                try {
                    dbConnection.close();
                } catch (Exception e) {
                    // Ignore
                }
            }
        }
    }

    /**
     * Applies CLI flag overrides to configuration.
     */
    private void applyCliOverrides(PragmiteConfig config) {
        // Override thresholds if specified via CLI
        if (complexityThreshold != 10) { // 10 is default
            config.getThresholds().put("cyclomaticComplexity", complexityThreshold);
        }
        if (maxMethodLength != 30) { // 30 is default
            config.getThresholds().put("longMethod", maxMethodLength);
        }
        if (maxParamCount != 4) { // 4 is default
            config.getThresholds().put("longParameterList", maxParamCount);
        }

        // Override analysis options
        PragmiteConfig.AnalysisOptions options = config.getAnalysisOptions();
        if (failOnCritical) {
            options.setFailOnCritical(true);
        }
        if (minQualityScore != null) {
            options.setMinQualityScore(minQualityScore);
        }
        if (maxCriticalIssues != null) {
            options.setMaxCriticalIssues(maxCriticalIssues);
        }
        if (format != null) {
            options.setReportFormat(format);
        }
        if (incrementalAnalysis) {
            options.setIncrementalAnalysis(true);
        }
    }

    /**
     * Checks quality gate and returns appropriate exit code.
     * Exit codes:
     * - 0 = passed
     * - 1 = quality gate failed
     * - 2 = analysis error
     */
    private int checkQualityGate(AnalysisResult result, PragmiteConfig config) {
        PragmiteConfig.AnalysisOptions options = config.getAnalysisOptions();

        int criticalCount = (int) result.getCodeSmells().stream()
            .filter(smell -> smell.getSeverity().toString().equals("CRITICAL"))
            .count();

        int qualityScore = result.getQualityScore() != null
            ? (int) result.getQualityScore().getOverallScore()
            : 100;

        // Check fail-on-critical
        if (options.isFailOnCritical() && criticalCount > 0) {
            System.err.println("\n❌ Quality Gate: FAILED");
            System.err.println("   Reason: Critical issues found (" + criticalCount + ")");
            return 1;
        }

        // Check minimum quality score
        if (qualityScore < options.getMinQualityScore()) {
            System.err.println("\n❌ Quality Gate: FAILED");
            System.err.println("   Reason: Quality score " + qualityScore + " < minimum " + options.getMinQualityScore());
            return 1;
        }

        // Check maximum critical issues
        if (options.getMaxCriticalIssues() >= 0 && criticalCount > options.getMaxCriticalIssues()) {
            System.err.println("\n❌ Quality Gate: FAILED");
            System.err.println("   Reason: Critical issues " + criticalCount + " > maximum " + options.getMaxCriticalIssues());
            return 1;
        }

        // All checks passed
        if (options.isFailOnCritical() || options.getMinQualityScore() > 0 || options.getMaxCriticalIssues() >= 0) {
            System.out.println("\n✅ Quality Gate: PASSED");
            System.out.println("   Quality Score: " + qualityScore + "/100");
            System.out.println("   Critical Issues: " + criticalCount);
        }

        return 0; // Success
    }

    private void writeConsoleReport(AnalysisResult result) {
        ConsoleReportWriter writer = new ConsoleReportWriter(verbose);
        writer.write(result);
    }

    private void writeJsonReport(AnalysisResult result) throws Exception {
        Path outputPath = outputFile != null
            ? outputFile.toPath()
            : projectDir.toPath().resolve("pragmite-report.json");

        JsonReportWriter writer = new JsonReportWriter();
        writer.write(result, outputPath);

        System.out.println("\nJSON raporu yazıldı: " + outputPath.toAbsolutePath());
    }

    private void writeHtmlReport(AnalysisResult result, List<AIAnalysisResult> aiResults) throws Exception {
        Path outputPath = outputFile != null
            ? outputFile.toPath().resolveSibling(outputFile.getName().replace(".json", ".html"))
            : projectDir.toPath().resolve("pragmite-report.html");

        HtmlReportGenerator generator = new HtmlReportGenerator();
        generator.generate(result, aiResults, outputPath);

        System.out.println("\n📊 HTML raporu yazıldı: " + outputPath.toAbsolutePath());
        if (aiResults != null && !aiResults.isEmpty()) {
            System.out.println("   🤖 AI analysis section included with " + aiResults.size() + " detailed insights");
        }
        System.out.println("   Tarayıcınızda açmak için: file:///" + outputPath.toAbsolutePath().toString().replace("\\", "/"));
    }

    /**
     * Handle auto-fix operations (v1.3.0).
     */
    private void handleAutoFix(AnalysisResult result, Connection dbConnection) {
        try {
            System.out.println("\n🔧 Auto-Fix başlatılıyor...");

            // Configure fix options
            FixOptions options = new FixOptions();
            options.setCreateBackup(!noBackup);
            options.setDryRun(dryRun);

            // Parse allowed fix types if specified
            if (fixTypes != null && !fixTypes.isEmpty()) {
                Set<CodeSmellType> allowedTypes = new HashSet<>();
                for (String type : fixTypes.split(",")) {
                    try {
                        allowedTypes.add(CodeSmellType.valueOf(type.trim().toUpperCase()));
                    } catch (IllegalArgumentException e) {
                        System.err.println("⚠️  Unknown fix type: " + type);
                    }
                }
                options.setAllowedTypes(allowedTypes);
            }

            // Apply fixes
            AutoFixEngine engine = new AutoFixEngine();
            FixResult fixResult = engine.applyFixes(result.getCodeSmells(), options);

            // Display results
            if (dryRun) {
                System.out.println("\n📋 Dry-Run Sonuçları:");
            } else {
                System.out.println("\n✅ Auto-Fix Tamamlandı:");
            }
            System.out.println("   Denenen: " + fixResult.getTotalAttempted());
            System.out.println("   Başarılı: " + fixResult.getSuccessCount());
            System.out.println("   Başarısız: " + fixResult.getFailureCount());
            System.out.println("   Atlanan: " + fixResult.getSkippedCount());

            if (!fixResult.getErrors().isEmpty()) {
                System.out.println("\n⚠️  Hatalar:");
                fixResult.getErrors().forEach(error -> System.out.println("   - " + error));
            }

            if (!dryRun && fixResult.getSuccessCount() > 0) {
                System.out.println("\n💡 İpucu: Değişiklikleri geri almak için --rollback-last kullanın");
            }

        } catch (Exception e) {
            System.err.println("❌ Auto-fix hatası: " + e.getMessage());
            if (verbose) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Handle rollback operations (v1.3.0).
     */
    private int handleRollbackOperations(Connection connection) {
        try {
            RollbackManager rollbackManager = new RollbackManager(connection);

            // List rollbacks
            if (listRollbacks) {
                System.out.println("📋 Geri Alınabilir İşlemler:\n");
                List<RollbackManager.RollbackableOperation> operations = rollbackManager.getRollbackableOperations();

                if (operations.isEmpty()) {
                    System.out.println("   Geri alınabilir işlem bulunamadı.");
                    return 0;
                }

                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                for (RollbackManager.RollbackableOperation op : operations) {
                    System.out.printf("   [%d] %s - %s%n",
                        op.getId(),
                        op.getStartedAt().format(formatter),
                        op.getFixType());
                    System.out.printf("       Başarılı: %d, Yedek: %d, Geri alınmamış: %d%n",
                        op.getSuccessCount(),
                        op.getBackupCount(),
                        op.getUnrestoredCount());
                }
                return 0;
            }

            // Rollback last
            if (rollbackLast) {
                System.out.println("🔄 En son işlem geri alınıyor...");
                RollbackManager.RollbackResult result = rollbackManager.rollbackLast();
                displayRollbackResult(result);
                return result.isSuccess() ? 0 : 1;
            }

            // Rollback by ID
            if (rollbackId != null) {
                System.out.println("🔄 İşlem #" + rollbackId + " geri alınıyor...");
                RollbackManager.RollbackResult result = rollbackManager.rollback(rollbackId);
                displayRollbackResult(result);
                return result.isSuccess() ? 0 : 1;
            }

            // Rollback file
            if (rollbackFile != null) {
                System.out.println("🔄 Dosya geri alınıyor: " + rollbackFile);
                RollbackManager.RollbackResult result = rollbackManager.rollbackFile(rollbackFile);
                displayRollbackResult(result);
                return result.isSuccess() ? 0 : 1;
            }

        } catch (Exception e) {
            System.err.println("❌ Rollback hatası: " + e.getMessage());
            if (verbose) {
                e.printStackTrace();
            }
            return 1;
        }

        return 0;
    }

    /**
     * Display rollback result.
     */
    private void displayRollbackResult(RollbackManager.RollbackResult result) {
        if (result.isSuccess()) {
            System.out.println("\n✅ Rollback Başarılı:");
            System.out.println("   Toplam yedek: " + result.getTotalBackups());
            System.out.println("   Geri yüklenen: " + result.getRestoredCount());
            System.out.println("   Atlanan: " + result.getSkippedCount());

            if (!result.getRestoredFiles().isEmpty()) {
                System.out.println("\n📁 Geri yüklenen dosyalar:");
                result.getRestoredFiles().forEach(file -> System.out.println("   - " + file));
            }
        } else {
            System.out.println("\n❌ Rollback Başarısız:");
            if (!result.getErrors().isEmpty()) {
                result.getErrors().forEach(error -> System.out.println("   - " + error));
            }
        }
    }

    /**
     * Handle file-based backup operations (v1.5.0 - auto-apply backups).
     */
    private int handleFileBackupOperations() {
        try {
            // Create BackupManager and RollbackManager
            com.pragmite.autofix.BackupManager backupManager = new com.pragmite.autofix.BackupManager(true);
            com.pragmite.autofix.RollbackManager rollbackManager =
                new com.pragmite.autofix.RollbackManager(null, backupManager);

            // List all backups
            if (listBackups) {
                System.out.println("\n📦 File-Based Backups (Auto-Apply):");
                System.out.println("Location: " + backupManager.getBackupDir());
                System.out.println();

                try (java.util.stream.Stream<Path> paths = java.nio.file.Files.list(backupManager.getBackupDir())) {
                    java.util.List<Path> backups = paths
                        .filter(p -> p.getFileName().toString().contains(".backup."))
                        .sorted(java.util.Comparator.<Path, java.nio.file.attribute.FileTime>comparing(p -> {
                            try {
                                return java.nio.file.Files.getLastModifiedTime(p);
                            } catch (java.io.IOException e) {
                                return java.nio.file.attribute.FileTime.fromMillis(0);
                            }
                        }).reversed())
                        .toList();

                    if (backups.isEmpty()) {
                        System.out.println("No backups found.");
                        return 0;
                    }

                    System.out.println("Total backups: " + backups.size());
                    System.out.println();
                    System.out.printf("%-40s %-20s %-10s%n", "File", "Created", "Size");
                    System.out.println("─".repeat(75));

                    for (Path backup : backups) {
                        String fileName = backup.getFileName().toString();
                        String originalFile = fileName.substring(0, fileName.indexOf(".backup."));
                        java.nio.file.attribute.BasicFileAttributes attrs =
                            java.nio.file.Files.readAttributes(backup, java.nio.file.attribute.BasicFileAttributes.class);
                        long size = java.nio.file.Files.size(backup);

                        String timestamp = java.time.format.DateTimeFormatter
                            .ofPattern("yyyy-MM-dd HH:mm:ss")
                            .withZone(java.time.ZoneId.systemDefault())
                            .format(attrs.creationTime().toInstant());

                        String sizeStr;
                        if (size < 1024) {
                            sizeStr = size + " B";
                        } else if (size < 1024 * 1024) {
                            sizeStr = String.format("%.1f KB", size / 1024.0);
                        } else {
                            sizeStr = String.format("%.1f MB", size / (1024.0 * 1024.0));
                        }

                        System.out.printf("%-40s %-20s %-10s%n",
                            originalFile.length() > 37 ? originalFile.substring(0, 37) + "..." : originalFile,
                            timestamp,
                            sizeStr);
                    }
                } catch (java.io.IOException e) {
                    System.err.println("Error reading backups: " + e.getMessage());
                    return 1;
                }

                return 0;
            }

            // List backups for specific file
            if (listBackupsFor != null) {
                System.out.println("\n📦 Backups for: " + listBackupsFor);
                System.out.println();

                java.util.List<com.pragmite.autofix.RollbackManager.FileBackupInfo> backups =
                    rollbackManager.listFileBackups(listBackupsFor);

                if (backups.isEmpty()) {
                    System.out.println("No backups found for: " + listBackupsFor);
                    return 0;
                }

                System.out.println("Total backups: " + backups.size());
                System.out.println();
                System.out.printf("%-20s %-10s %-50s%n", "Created", "Size", "Backup File");
                System.out.println("─".repeat(85));

                for (com.pragmite.autofix.RollbackManager.FileBackupInfo backup : backups) {
                    System.out.printf("%-20s %-10s %-50s%n",
                        backup.getFormattedTimestamp(),
                        backup.getFormattedSize(),
                        backup.getBackupPath().getFileName());
                }

                System.out.println();
                System.out.println("💡 To rollback: --rollback-file-backup " + listBackupsFor);

                return 0;
            }

            // Rollback file to latest backup
            if (rollbackFileBackup != null) {
                Path targetFile = Path.of(rollbackFileBackup).toAbsolutePath();

                if (!java.nio.file.Files.exists(targetFile)) {
                    System.err.println("❌ File not found: " + rollbackFileBackup);
                    return 1;
                }

                System.out.println("\n🔄 Rolling back: " + rollbackFileBackup);

                com.pragmite.autofix.RollbackManager.FileRollbackResult result =
                    rollbackManager.rollbackToLatestFileBackup(targetFile);

                if (result.isSuccess()) {
                    System.out.println("✅ Rollback successful!");
                    System.out.println("   File: " + result.getTargetFile().getFileName());
                    System.out.println("   Restored from: " + result.getBackupPath().getFileName());
                    System.out.println("   Safety backup created: " +
                        result.getSafetyBackup().getBackupPath().getFileName());
                } else {
                    System.err.println("❌ Rollback failed: " + result.getErrorMessage());
                    return 1;
                }

                return 0;
            }

        } catch (Exception e) {
            System.err.println("\n❌ Error: " + e.getMessage());
            if (verbose) {
                e.printStackTrace();
            }
            return 1;
        }

        return 0;
    }

    /**
     * Handle AI-powered analysis (v1.4.0).
     * Returns the list of AI analysis results for use in HTML report.
     */
    private List<AIAnalysisResult> handleAiAnalysis(AnalysisResult result) {
        try {
            System.out.println("\n🤖 Generating AI-Powered Analysis...");

            // Configure API for auto-refactoring if enabled
            com.pragmite.ai.ApiConfig apiConfig = null;
            if (autoRefactor) {
                apiConfig = com.pragmite.ai.ApiConfig.fromEnvironment();

                // Override with CLI-provided API key if specified
                if (claudeApiKey != null && !claudeApiKey.isEmpty()) {
                    apiConfig.setApiKey(claudeApiKey);
                    apiConfig.setEnabled(true);
                }

                if (!apiConfig.isValid()) {
                    System.err.println("⚠️  Auto-refactor enabled but API key not found.");
                    System.err.println("   Set CLAUDE_API_KEY environment variable or use --claude-api-key option.");
                    apiConfig = null; // Disable auto-refactoring
                }
            }

            AnalysisEngine aiEngine = new AnalysisEngine();
            List<AIAnalysisResult> aiResults = aiEngine.analyzeAll(
                result.getCodeSmells(),
                projectDir.toPath(),
                apiConfig
            );

            if (aiResults.isEmpty()) {
                System.out.println("   No issues to analyze.");
                return aiResults;
            }

            System.out.println("   Generated " + aiResults.size() + " AI analysis reports.\n");

            // v1.5.0 Phase 3: Auto-apply refactored code if enabled
            if (autoApply && apiConfig != null && apiConfig.isValid()) {
                handleAutoApply(aiResults, result);
            }

            // Display AI analysis in console
            for (AIAnalysisResult aiResult : aiResults) {
                System.out.println(aiResult.toConsoleFormat());
                System.out.println(); // Separator between results
            }

            // Save to JSON file if specified
            if (aiOutputFile != null) {
                Path outputPath = aiOutputFile.toPath();
                writeAiAnalysisJson(aiResults, outputPath);
                System.out.println("\n💾 AI analysis saved to: " + outputPath.toAbsolutePath());
            } else {
                // Save to default location
                Path defaultPath = projectDir.toPath().resolve("pragmite-ai-analysis.json");
                writeAiAnalysisJson(aiResults, defaultPath);
                System.out.println("\n💾 AI analysis saved to: " + defaultPath.toAbsolutePath());
            }

            return aiResults;

        } catch (Exception e) {
            System.err.println("❌ AI analysis error: " + e.getMessage());
            if (verbose) {
                e.printStackTrace();
            }
            return null;
        }
    }

    /**
     * Writes AI analysis results to JSON file.
     */
    private void writeAiAnalysisJson(List<AIAnalysisResult> aiResults, Path outputPath) throws Exception {
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"generatedAt\": \"").append(java.time.Instant.now()).append("\",\n");
        json.append("  \"totalAnalyses\": ").append(aiResults.size()).append(",\n");
        json.append("  \"analyses\": [\n");

        for (int i = 0; i < aiResults.size(); i++) {
            json.append("    ").append(aiResults.get(i).toJSON());
            if (i < aiResults.size() - 1) {
                json.append(",");
            }
            json.append("\n");
        }

        json.append("  ]\n");
        json.append("}\n");

        java.nio.file.Files.writeString(outputPath, json.toString());
    }

    /**
     * Handle auto-apply of refactored code (v1.5.0 Phase 3).
     */
    private void handleAutoApply(List<AIAnalysisResult> aiResults, AnalysisResult analysisResult) {
        try {
            System.out.println("\n🔧 Applying AI-Generated Refactorings...");

            // Create CodeApplicator with settings from CLI flags
            boolean enableBackup = !noBackup;
            com.pragmite.autofix.CodeApplicator applicator =
                new com.pragmite.autofix.CodeApplicator(dryRun, enableBackup);

            if (dryRun) {
                System.out.println("   DRY RUN MODE - No files will be modified\n");
            }

            int totalApplied = 0;
            int totalFailed = 0;
            int totalSkipped = 0;

            // Apply each refactored code
            for (AIAnalysisResult aiResult : aiResults) {
                if (!aiResult.hasRefactoredCode()) {
                    continue; // Skip if no refactored code available
                }

                com.pragmite.ai.RefactoredCode refactored = aiResult.getRefactoredCode();
                CodeSmell smell = aiResult.getOriginalSmell();

                // Get source file path - smell.getFilePath() is relative to current directory
                Path sourceFile = Path.of(smell.getFilePath()).toAbsolutePath();

                // If file doesn't exist, try resolving from projectDir
                if (!java.nio.file.Files.exists(sourceFile)) {
                    sourceFile = projectDir.toPath().resolve(smell.getFilePath());
                }

                if (!java.nio.file.Files.exists(sourceFile)) {
                    System.err.println("   ⚠️  File not found: " + smell.getFilePath());
                    totalSkipped++;
                    continue;
                }

                // Apply refactoring
                System.out.print("   Applying to " + smell.getFilePath() + "... ");

                com.pragmite.autofix.ApplicationResult result = applicator.apply(refactored, sourceFile);

                if (result.isSuccess()) {
                    System.out.println("✅ Success");
                    totalApplied++;

                    if (result.getMetrics() != null && verbose) {
                        System.out.println("      " + result.getMetrics());
                    }
                } else if (result.wasSkipped()) {
                    System.out.println("⏭️  Skipped");
                    totalSkipped++;
                } else {
                    System.out.println("❌ Failed");
                    totalFailed++;

                    if (verbose && !result.getErrors().isEmpty()) {
                        for (String error : result.getErrors()) {
                            System.err.println("      Error: " + error);
                        }
                    }
                }
            }

            // Summary
            System.out.println("\n📊 Auto-Apply Summary:");
            System.out.println("   ✅ Successfully applied: " + totalApplied);
            System.out.println("   ❌ Failed: " + totalFailed);
            System.out.println("   ⏭️  Skipped: " + totalSkipped);

            if (totalApplied > 0 && enableBackup) {
                System.out.println("\n💾 Backups saved to: " + applicator.getBackupDir());
            }

            if (dryRun) {
                System.out.println("\n💡 Run without --dry-run to apply changes");
            }

        } catch (Exception e) {
            System.err.println("\n❌ Auto-apply error: " + e.getMessage());
            if (verbose) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Handle history and trend display (v1.3.0).
     */
    private int handleHistoryDisplay(DatabaseManager dbManager) {
        try {
            // Show history
            if (showHistory != null) {
                System.out.println("📊 Son " + showHistory + " Analiz:\n");
                List<DatabaseManager.AnalysisRun> runs = dbManager.getRecentRuns(showHistory);

                if (runs.isEmpty()) {
                    System.out.println("   Kayıtlı analiz bulunamadı.");
                    return 0;
                }

                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                for (DatabaseManager.AnalysisRun run : runs) {
                    System.out.printf("[%d] %s - %s%n",
                        run.getId(),
                        run.getTimestamp().format(formatter),
                        run.getProjectName());
                    System.out.printf("    Kalite: %d/100 (%s), Sorunlar: %d (Kritik: %d), Dosyalar: %d%n",
                        run.getQualityScore(),
                        run.getQualityGrade(),
                        run.getTotalIssues(),
                        run.getCriticalIssues(),
                        run.getFilesAnalyzed());
                    System.out.println();
                }
            }

            // Show trend
            if (showTrend != null) {
                System.out.println("📈 Son " + showTrend + " Günlük Kalite Trendi:\n");
                List<DatabaseManager.TrendData> trend = dbManager.getQualityTrend(showTrend);

                if (trend.isEmpty()) {
                    System.out.println("   Trend verisi bulunamadı.");
                    return 0;
                }

                System.out.println("Tarih          | Ort.Skor | Min  | Max  | Ort.Sorun");
                System.out.println("---------------|----------|------|------|----------");
                for (DatabaseManager.TrendData data : trend) {
                    System.out.printf("%-14s | %8.1f | %4.0f | %4.0f | %9.1f%n",
                        data.getDate(),
                        data.getAvgScore(),
                        data.getMinScore(),
                        data.getMaxScore(),
                        data.getAvgIssues());
                }
            }

            return 0;

        } catch (Exception e) {
            System.err.println("❌ Veritabanı hatası: " + e.getMessage());
            if (verbose) {
                e.printStackTrace();
            }
            return 1;
        }
    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new PragmiteCLI()).execute(args);
        System.exit(exitCode);
    }
}

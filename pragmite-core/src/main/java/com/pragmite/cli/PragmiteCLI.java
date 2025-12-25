package com.pragmite.cli;

import com.pragmite.analyzer.ProjectAnalyzer;
import com.pragmite.config.ConfigLoader;
import com.pragmite.config.PragmiteConfig;
import com.pragmite.model.AnalysisResult;
import com.pragmite.output.JsonReportWriter;
import com.pragmite.output.ConsoleReportWriter;
import com.pragmite.report.HtmlReportGenerator;
import com.pragmite.cache.CacheManager;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.File;
import java.nio.file.Path;
import java.util.concurrent.Callable;

/**
 * Pragmite CLI - Java kod kalitesi analiz aracı.
 */
@Command(
    name = "pragmite",
    mixinStandardHelpOptions = true,
    version = "Pragmite 1.0.0",
    description = "Java kod kalitesi ve karmaşıklık analizi aracı"
)
public class PragmiteCLI implements Callable<Integer> {

    @Parameters(index = "0", description = "Analiz edilecek proje dizini", defaultValue = ".")
    private File projectDir;

    @Option(names = {"-o", "--output"}, description = "JSON rapor çıktı dosyası")
    private File outputFile;

    @Option(names = {"-f", "--format"}, description = "Çıktı formatı: console, json, both", defaultValue = "console")
    private String format;

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

    @Override
    public Integer call() throws Exception {
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
        System.out.println("║                    PRAGMITE ANALYZER v1.1                    ║");
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

        try {
            // Analyzer oluştur ve çalıştır
            ProjectAnalyzer analyzer = new ProjectAnalyzer(projectDir.toPath());
            AnalysisResult result = analyzer.analyze();

            // Çıktı formatına göre rapor yaz
            switch (format.toLowerCase()) {
                case "json":
                    writeJsonReport(result);
                    break;
                case "html":
                    writeHtmlReport(result);
                    break;
                case "pdf":
                    writeHtmlReport(result); // PDF is HTML-based for now
                    System.out.println("📄 PDF generation via HTML report (print to PDF from browser)");
                    break;
                case "both":
                    writeConsoleReport(result);
                    writeJsonReport(result);
                    writeHtmlReport(result);
                    break;
                case "console":
                default:
                    writeConsoleReport(result);
                    break;
            }

            // Apply quality gate checks
            return checkQualityGate(result, config);

        } catch (Exception e) {
            System.err.println("Analiz sırasında hata oluştu: " + e.getMessage());
            if (verbose) {
                e.printStackTrace();
            }
            return 2; // Exit code 2 = analysis error
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

    private void writeHtmlReport(AnalysisResult result) throws Exception {
        Path outputPath = outputFile != null
            ? outputFile.toPath().resolveSibling(outputFile.getName().replace(".json", ".html"))
            : projectDir.toPath().resolve("pragmite-report.html");

        HtmlReportGenerator generator = new HtmlReportGenerator();
        generator.generate(result, outputPath);

        System.out.println("\n📊 HTML raporu yazıldı: " + outputPath.toAbsolutePath());
        System.out.println("   Tarayıcınızda açmak için: file:///" + outputPath.toAbsolutePath().toString().replace("\\", "/"));
    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new PragmiteCLI()).execute(args);
        System.exit(exitCode);
    }
}

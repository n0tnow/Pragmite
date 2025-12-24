package com.pragmite.cli;

import com.pragmite.analyzer.ProjectAnalyzer;
import com.pragmite.model.AnalysisResult;
import com.pragmite.output.JsonReportWriter;
import com.pragmite.output.ConsoleReportWriter;
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

    @Override
    public Integer call() throws Exception {
        if (!projectDir.exists() || !projectDir.isDirectory()) {
            System.err.println("Hata: Geçersiz proje dizini: " + projectDir.getAbsolutePath());
            return 1;
        }

        System.out.println("╔══════════════════════════════════════════════════════════════╗");
        System.out.println("║                    PRAGMITE ANALYZER v1.0                    ║");
        System.out.println("║          Java Kod Kalitesi ve Karmaşıklık Analizi            ║");
        System.out.println("╚══════════════════════════════════════════════════════════════╝");
        System.out.println();

        if (verbose) {
            System.out.println("Proje dizini: " + projectDir.getAbsolutePath());
            System.out.println("Eşik değerleri:");
            System.out.println("  - Cyclomatic Complexity: " + complexityThreshold);
            System.out.println("  - Maksimum Metot Uzunluğu: " + maxMethodLength);
            System.out.println("  - Maksimum Parametre Sayısı: " + maxParamCount);
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
                case "both":
                    writeConsoleReport(result);
                    writeJsonReport(result);
                    break;
                case "console":
                default:
                    writeConsoleReport(result);
                    break;
            }

            // Çıkış kodu: Kritik sorun varsa 1, yoksa 0
            return result.hasBlockerIssues() ? 1 : 0;

        } catch (Exception e) {
            System.err.println("Analiz sırasında hata oluştu: " + e.getMessage());
            if (verbose) {
                e.printStackTrace();
            }
            return 2;
        }
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

    public static void main(String[] args) {
        int exitCode = new CommandLine(new PragmiteCLI()).execute(args);
        System.exit(exitCode);
    }
}

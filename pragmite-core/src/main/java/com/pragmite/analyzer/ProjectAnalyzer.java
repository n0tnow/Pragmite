package com.pragmite.analyzer;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.pragmite.config.AnalysisConfig;
import com.pragmite.metrics.CKMetrics;
import com.pragmite.metrics.CKMetricsCalculator;
import com.pragmite.model.*;
import com.pragmite.profiling.JfrProfiler;
import com.pragmite.profiling.ProfileReport;
import com.pragmite.refactoring.RefactoringManager;
import com.pragmite.refactoring.RefactoringSuggestion;
import com.pragmite.rules.RuleEngine;
import com.pragmite.scoring.ScoreCalculator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

/**
 * Project analyzer with parallel processing support.
 * Analyzes all Java files in a directory tree.
 */
public class ProjectAnalyzer {
    private static final Logger logger = LoggerFactory.getLogger(ProjectAnalyzer.class);

    private final JavaParser javaParser;
    private final ComplexityAnalyzer complexityAnalyzer;
    private final RuleEngine ruleEngine;
    private final ScoreCalculator scoreCalculator;
    private final RefactoringManager refactoringManager;
    private final CKMetricsCalculator ckMetricsCalculator;
    private final AnalysisConfig config;
    private final JfrProfiler jfrProfiler;

    private Path projectPath;
    private boolean enableProfiling = true;  // Enabled by default for performance insights

    public ProjectAnalyzer() {
        this(AnalysisConfig.defaultConfig());
    }

    public ProjectAnalyzer(AnalysisConfig config) {
        this.javaParser = new JavaParser();
        this.complexityAnalyzer = new ComplexityAnalyzer();
        this.ruleEngine = new RuleEngine();
        this.scoreCalculator = new ScoreCalculator();
        this.refactoringManager = new RefactoringManager();
        this.ckMetricsCalculator = new CKMetricsCalculator();
        this.config = config;
        this.jfrProfiler = new JfrProfiler();
    }

    public ProjectAnalyzer(Path projectPath) {
        this();
        this.projectPath = projectPath;
    }

    public ProjectAnalyzer(Path projectPath, AnalysisConfig config) {
        this(config);
        this.projectPath = projectPath;
    }

    /**
     * Constructor'da verilen proje dizinini analiz eder.
     */
    public AnalysisResult analyze() throws IOException {
        if (this.projectPath == null) {
            throw new IllegalStateException("Proje yolu belirtilmemiş. ProjectAnalyzer(Path) constructor kullanın.");
        }
        return analyze(this.projectPath.toString());
    }

    /**
     * Analyzes all Java files in the specified directory.
     * Uses parallel processing if enabled in config.
     */
    public AnalysisResult analyze(String projectPath) throws IOException {
        long startTime = System.currentTimeMillis();
        logger.info("Starting analysis: {}", projectPath);

        Path path = Paths.get(projectPath);
        if (!Files.exists(path)) {
            throw new IllegalArgumentException("Project directory not found: " + projectPath);
        }

        // Start JFR profiling if enabled
        if (enableProfiling) {
            try {
                logger.info("Starting JFR profiling...");
                jfrProfiler.start();
            } catch (IOException e) {
                logger.warn("Failed to start JFR profiling", e);
            }
        }

        AnalysisResult result = new AnalysisResult(projectPath);
        List<Path> javaFiles = findJavaFiles(path);

        logger.info("Found {} Java files", javaFiles.size());
        result.setTotalFiles(javaFiles.size());

        // Choose parallel or sequential analysis based on config
        List<FileAnalysis> fileAnalyses;
        if (config.isEnableParallelAnalysis() && javaFiles.size() > 10) {
            logger.info("Using parallel analysis with {} threads", config.getParallelThreads());
            fileAnalyses = analyzeFilesParallel(javaFiles);
        } else {
            logger.info("Using sequential analysis");
            fileAnalyses = analyzeFilesSequential(javaFiles);
        }

        // Aggregate results
        int totalLines = 0;
        for (FileAnalysis fileAnalysis : fileAnalyses) {
            result.addFileAnalysis(fileAnalysis);
            totalLines += fileAnalysis.getLineCount();
            fileAnalysis.getSmells().forEach(result::addCodeSmell);
            fileAnalysis.getComplexities().forEach(result::addComplexityInfo);
        }

        result.setTotalLines(totalLines);

        // Calculate quality scores
        QualityScore score = scoreCalculator.calculate(result.getFileAnalyses(), result.getCodeSmells());
        result.setQualityScore(score);

        // Generate refactoring suggestions for code smells
        logger.info("Generating refactoring suggestions...");
        List<RefactoringSuggestion> suggestions = generateSuggestions(result.getCodeSmells(), javaFiles);
        result.setSuggestions(suggestions);
        logger.info("Generated {} refactoring suggestions", suggestions.size());

        // Record analysis duration
        long endTime = System.currentTimeMillis();
        result.setAnalysisDurationMs(endTime - startTime);

        // Stop JFR profiling and collect results
        if (enableProfiling) {
            try {
                logger.info("Stopping JFR profiling...");
                ProfileReport profileReport = jfrProfiler.stop();
                result.setProfileReport(profileReport);
                logger.info("JFR profiling complete");
            } catch (IOException e) {
                logger.warn("Failed to collect JFR profiling data", e);
            }
        }

        logger.info("Analysis complete. Found {} code smells in {} ms",
            result.getCodeSmells().size(), result.getAnalysisDurationMs());

        return result;
    }

    /**
     * Enables JFR profiling for performance analysis.
     */
    public ProjectAnalyzer withProfiling(boolean enable) {
        this.enableProfiling = enable;
        return this;
    }

    /**
     * Analyzes files sequentially (single-threaded).
     */
    private List<FileAnalysis> analyzeFilesSequential(List<Path> javaFiles) {
        List<FileAnalysis> analyses = new ArrayList<>();

        for (Path javaFile : javaFiles) {
            try {
                FileAnalysis fileAnalysis = analyzeFile(javaFile);
                analyses.add(fileAnalysis);
            } catch (Exception e) {
                logger.warn("Failed to analyze file: {} - {}", javaFile, e.getMessage());
            }
        }

        return analyses;
    }

    /**
     * Analyzes files in parallel (multi-threaded).
     */
    private List<FileAnalysis> analyzeFilesParallel(List<Path> javaFiles) {
        ExecutorService executor = Executors.newFixedThreadPool(config.getParallelThreads());
        List<Future<FileAnalysis>> futures = new ArrayList<>();

        // Submit all analysis tasks
        for (Path javaFile : javaFiles) {
            Future<FileAnalysis> future = executor.submit(() -> {
                try {
                    return analyzeFile(javaFile);
                } catch (Exception e) {
                    logger.warn("Failed to analyze file: {} - {}", javaFile, e.getMessage());
                    return null;
                }
            });
            futures.add(future);
        }

        // Collect results
        List<FileAnalysis> analyses = new ArrayList<>();
        for (Future<FileAnalysis> future : futures) {
            try {
                FileAnalysis analysis = future.get();
                if (analysis != null) {
                    analyses.add(analysis);
                }
            } catch (InterruptedException | ExecutionException e) {
                logger.error("Error getting analysis result", e);
            }
        }

        executor.shutdown();
        try {
            if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }

        return analyses;
    }

    /**
     * Tek bir Java dosyasını analiz eder.
     */
    public FileAnalysis analyzeFile(Path filePath) throws IOException {
        logger.debug("Dosya analiz ediliyor: {}", filePath);

        String content = Files.readString(filePath);
        FileAnalysis analysis = new FileAnalysis(filePath.toString());

        // Satır sayısını hesapla
        int lineCount = content.split("\n").length;
        analysis.setLineCount(lineCount);

        // Parse et
        ParseResult<CompilationUnit> parseResult = javaParser.parse(content);

        if (parseResult.isSuccessful() && parseResult.getResult().isPresent()) {
            CompilationUnit cu = parseResult.getResult().get();

            // Sınıf adını al
            cu.getPrimaryTypeName().ifPresent(analysis::setClassName);

            // Metot bilgilerini topla
            List<MethodInfo> methods = MethodExtractor.extractMethods(cu);
            analysis.setMethods(methods);
            analysis.setMethodCount(methods.size());

            // Karmaşıklık analizi
            List<ComplexityInfo> complexities = complexityAnalyzer.analyze(cu, filePath.toString());
            analysis.setComplexities(complexities);

            // Kod kokusu tespiti
            List<CodeSmell> smells = ruleEngine.analyze(cu, filePath.toString(), content);
            analysis.setSmells(smells);

            // CK Metrics hesaplama
            Map<String, CKMetrics> metricsMap = ckMetricsCalculator.calculateAll(cu, filePath.toString());
            // İlk (ve genellikle tek) sınıf için metrics'i al
            if (!metricsMap.isEmpty()) {
                analysis.setCkMetrics(metricsMap.values().iterator().next());
            }

        } else {
            // Detaylı parse hatası loglama
            if (parseResult.getProblems().isEmpty()) {
                logger.warn("Parse failed for file: {} (no specific problems reported)", filePath);
            } else {
                StringBuilder errorDetails = new StringBuilder();
                errorDetails.append("Parse failed for file: ").append(filePath).append("\n");
                parseResult.getProblems().forEach(problem -> {
                    errorDetails.append("  - ").append(problem.getVerboseMessage()).append("\n");
                });
                logger.warn(errorDetails.toString().trim());
            }
        }

        return analysis;
    }

    /**
     * Generates refactoring suggestions for detected code smells.
     */
    private List<RefactoringSuggestion> generateSuggestions(List<CodeSmell> codeSmells, List<Path> javaFiles) {
        List<RefactoringSuggestion> suggestions = new ArrayList<>();

        // Group smells by file path
        Map<String, List<CodeSmell>> smellsByFile = codeSmells.stream()
            .collect(java.util.stream.Collectors.groupingBy(CodeSmell::getFilePath));

        // Generate suggestions for each file
        for (Path filePath : javaFiles) {
            String filePathStr = filePath.toString();
            List<CodeSmell> fileSmells = smellsByFile.get(filePathStr);

            if (fileSmells == null || fileSmells.isEmpty()) {
                continue;
            }

            try {
                // Parse the file
                String content = Files.readString(filePath);
                ParseResult<CompilationUnit> parseResult = javaParser.parse(content);

                if (parseResult.isSuccessful() && parseResult.getResult().isPresent()) {
                    CompilationUnit cu = parseResult.getResult().get();

                    // Generate suggestions for each smell in this file
                    for (CodeSmell smell : fileSmells) {
                        refactoringManager.getSuggestion(smell, cu)
                            .ifPresent(suggestions::add);
                    }
                }
            } catch (IOException e) {
                logger.warn("Failed to generate suggestions for file: {} - {}", filePath, e.getMessage());
            }
        }

        return suggestions;
    }

    /**
     * Belirtilen dizinde tüm Java dosyalarını bulur.
     */
    private List<Path> findJavaFiles(Path rootPath) throws IOException {
        List<Path> javaFiles = new ArrayList<>();

        Files.walkFileTree(rootPath, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                if (file.toString().endsWith(".java")) {
                    // Test dosyalarını ve build dizinlerini atla
                    String pathStr = file.toString();
                    if (!pathStr.contains("build") && !pathStr.contains("target")) {
                        javaFiles.add(file);
                    }
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                String dirName = dir.getFileName().toString();
                // Gizli dizinleri ve build dizinlerini atla
                if (dirName.startsWith(".") || dirName.equals("build") || dirName.equals("target") || dirName.equals("node_modules")) {
                    return FileVisitResult.SKIP_SUBTREE;
                }
                return FileVisitResult.CONTINUE;
            }
        });

        return javaFiles;
    }
}

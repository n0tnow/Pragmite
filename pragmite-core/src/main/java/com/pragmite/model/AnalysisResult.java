package com.pragmite.model;

import com.pragmite.benchmark.BenchmarkResult;
import com.pragmite.profiling.ProfileReport;
import com.pragmite.refactoring.RefactoringSuggestion;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Bir projenin analiz sonuçlarını tutan ana sınıf.
 */
public class AnalysisResult {
    private String projectPath;
    private LocalDateTime analyzedAt;
    private int totalFiles;
    private int totalLines;
    private List<FileAnalysis> fileAnalyses;
    private List<CodeSmell> codeSmells;
    private List<ComplexityInfo> complexityInfos;
    private QualityScore qualityScore;
    private List<RefactoringSuggestion> suggestions;

    // Performance profiling results
    private ProfileReport profileReport;
    private BenchmarkResult benchmarkResult;

    public AnalysisResult() {
        this.analyzedAt = LocalDateTime.now();
        this.fileAnalyses = new ArrayList<>();
        this.codeSmells = new ArrayList<>();
        this.complexityInfos = new ArrayList<>();
        this.suggestions = new ArrayList<>();
    }

    public AnalysisResult(String projectPath) {
        this();
        this.projectPath = projectPath;
    }

    // Getters and Setters
    public String getProjectPath() { return projectPath; }
    public void setProjectPath(String projectPath) { this.projectPath = projectPath; }

    public LocalDateTime getAnalyzedAt() { return analyzedAt; }
    public void setAnalyzedAt(LocalDateTime analyzedAt) { this.analyzedAt = analyzedAt; }

    public int getTotalFiles() { return totalFiles; }
    public void setTotalFiles(int totalFiles) { this.totalFiles = totalFiles; }

    public int getTotalLines() { return totalLines; }
    public void setTotalLines(int totalLines) { this.totalLines = totalLines; }

    public List<FileAnalysis> getFileAnalyses() { return fileAnalyses; }
    public void setFileAnalyses(List<FileAnalysis> fileAnalyses) { this.fileAnalyses = fileAnalyses; }

    public List<CodeSmell> getCodeSmells() { return codeSmells; }
    public void setCodeSmells(List<CodeSmell> codeSmells) { this.codeSmells = codeSmells; }

    public List<ComplexityInfo> getComplexityInfos() { return complexityInfos; }
    public void setComplexityInfos(List<ComplexityInfo> complexityInfos) { this.complexityInfos = complexityInfos; }

    public QualityScore getQualityScore() { return qualityScore; }
    public void setQualityScore(QualityScore qualityScore) { this.qualityScore = qualityScore; }

    public void addFileAnalysis(FileAnalysis analysis) {
        this.fileAnalyses.add(analysis);
    }

    public void addCodeSmell(CodeSmell smell) {
        this.codeSmells.add(smell);
    }

    public void addComplexityInfo(ComplexityInfo info) {
        this.complexityInfos.add(info);
    }

    // Analiz süresi (milisaniye)
    private long analysisDurationMs;

    public long getAnalysisDurationMs() { return analysisDurationMs; }
    public void setAnalysisDurationMs(long analysisDurationMs) { this.analysisDurationMs = analysisDurationMs; }

    public ProfileReport getProfileReport() { return profileReport; }
    public void setProfileReport(ProfileReport profileReport) { this.profileReport = profileReport; }

    public BenchmarkResult getBenchmarkResult() { return benchmarkResult; }
    public void setBenchmarkResult(BenchmarkResult benchmarkResult) { this.benchmarkResult = benchmarkResult; }

    public List<RefactoringSuggestion> getSuggestions() { return suggestions; }
    public void setSuggestions(List<RefactoringSuggestion> suggestions) { this.suggestions = suggestions; }

    public void addSuggestion(RefactoringSuggestion suggestion) {
        this.suggestions.add(suggestion);
    }

    /**
     * Checks if profiling data is available.
     */
    public boolean hasProfilingData() {
        return profileReport != null;
    }

    /**
     * Checks if benchmark data is available.
     */
    public boolean hasBenchmarkData() {
        return benchmarkResult != null;
    }

    /**
     * Blocker seviyesinde sorun olup olmadığını kontrol eder.
     */
    public boolean hasBlockerIssues() {
        return codeSmells.stream()
            .anyMatch(s -> s.getType().getDefaultSeverity() == Severity.BLOCKER);
    }

    /**
     * Kritik veya daha yüksek seviyede sorun olup olmadığını kontrol eder.
     */
    public boolean hasCriticalIssues() {
        return codeSmells.stream()
            .anyMatch(s -> {
                Severity sev = s.getType().getDefaultSeverity();
                return sev == Severity.BLOCKER || sev == Severity.CRITICAL;
            });
    }
}

package com.pragmite.config;

import java.util.*;

/**
 * Configuration model for Pragmite analysis.
 * Loaded from .pragmite.yaml in the project root.
 *
 * Supports:
 * - Custom thresholds for detectors
 * - Exclude patterns (files/directories)
 * - Severity customization
 * - Quality score weights
 */
public class PragmiteConfig {

    // Thresholds for detectors
    private Map<String, Integer> thresholds = new HashMap<>();

    // Exclude patterns (glob patterns)
    private List<String> excludePatterns = new ArrayList<>();

    // Severity overrides (detector -> severity)
    private Map<String, String> severityOverrides = new HashMap<>();

    // Quality score weights
    private QualityWeights qualityWeights = new QualityWeights();

    // Analysis options
    private AnalysisOptions analysisOptions = new AnalysisOptions();

    // Default constructor
    public PragmiteConfig() {
        setDefaultValues();
    }

    private void setDefaultValues() {
        // Default thresholds
        thresholds.put("cyclomaticComplexity", 15);
        thresholds.put("longMethod", 50);
        thresholds.put("largeClass.lines", 400);
        thresholds.put("largeClass.methods", 25);
        thresholds.put("longParameterList", 5);
        thresholds.put("deepNesting", 5);
        thresholds.put("switchStatement", 7);
        thresholds.put("tooManyLiterals.numeric", 7);
        thresholds.put("tooManyLiterals.string", 5);
        thresholds.put("lazyClass", 80);

        // Default excludes
        excludePatterns.add("**/target/**");
        excludePatterns.add("**/build/**");
        excludePatterns.add("**/node_modules/**");
        excludePatterns.add("**/.git/**");
        excludePatterns.add("**/test-output/**");

        // Default quality weights (from QualityScore.java)
        qualityWeights.setDryWeight(0.30);
        qualityWeights.setOrthogonalityWeight(0.30);
        qualityWeights.setCorrectnessWeight(0.25);
        qualityWeights.setPerformanceWeight(0.15);
    }

    // Getters and setters
    public Map<String, Integer> getThresholds() {
        return thresholds;
    }

    public void setThresholds(Map<String, Integer> thresholds) {
        this.thresholds = thresholds;
    }

    public Integer getThreshold(String key) {
        return thresholds.get(key);
    }

    public Integer getThreshold(String key, int defaultValue) {
        return thresholds.getOrDefault(key, defaultValue);
    }

    public List<String> getExcludePatterns() {
        return excludePatterns;
    }

    public void setExcludePatterns(List<String> excludePatterns) {
        this.excludePatterns = excludePatterns;
    }

    public Map<String, String> getSeverityOverrides() {
        return severityOverrides;
    }

    public void setSeverityOverrides(Map<String, String> severityOverrides) {
        this.severityOverrides = severityOverrides;
    }

    public QualityWeights getQualityWeights() {
        return qualityWeights;
    }

    public void setQualityWeights(QualityWeights qualityWeights) {
        this.qualityWeights = qualityWeights;
    }

    public AnalysisOptions getAnalysisOptions() {
        return analysisOptions;
    }

    public void setAnalysisOptions(AnalysisOptions analysisOptions) {
        this.analysisOptions = analysisOptions;
    }

    /**
     * Checks if a file path should be excluded based on patterns.
     */
    public boolean isExcluded(String filePath) {
        if (filePath == null) return false;

        // Normalize path separators
        String normalizedPath = filePath.replace('\\', '/');

        for (String pattern : excludePatterns) {
            if (matchesGlobPattern(normalizedPath, pattern)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Simple glob pattern matching.
     * Supports: ** (any path), * (any name)
     */
    private boolean matchesGlobPattern(String path, String pattern) {
        // Normalize pattern
        String normalizedPattern = pattern.replace('\\', '/');

        // Convert glob to regex with proper handling
        String regex = normalizedPattern
            .replace(".", "\\.")                    // Escape dots
            .replace("**/", "§DOUBLESTAR_SLASH§")   // Placeholder for **/
            .replace("/**", "§SLASH_DOUBLESTAR§")   // Placeholder for /**
            .replace("**", "§DOUBLESTAR§")          // Placeholder for **
            .replace("*", "[^/]*")                  // * -> match anything except /
            .replace("§DOUBLESTAR_SLASH§", "(.*/)?") // **/ -> optional prefix
            .replace("§SLASH_DOUBLESTAR§", "(/.*)?") // /** -> optional suffix
            .replace("§DOUBLESTAR§", ".*");          // ** -> match everything

        return path.matches(regex);
    }

    /**
     * Merges this configuration with another (CLI overrides config file).
     */
    public void merge(PragmiteConfig other) {
        if (other == null) return;

        // Merge thresholds
        this.thresholds.putAll(other.thresholds);

        // Merge exclude patterns (additive)
        this.excludePatterns.addAll(other.excludePatterns);

        // Merge severity overrides
        this.severityOverrides.putAll(other.severityOverrides);

        // Merge quality weights
        if (other.qualityWeights != null) {
            this.qualityWeights = other.qualityWeights;
        }

        // Merge analysis options
        if (other.analysisOptions != null) {
            this.analysisOptions.merge(other.analysisOptions);
        }
    }

    /**
     * Quality score weights configuration.
     */
    public static class QualityWeights {
        private double dryWeight = 0.30;
        private double orthogonalityWeight = 0.30;
        private double correctnessWeight = 0.25;
        private double performanceWeight = 0.15;

        public double getDryWeight() {
            return dryWeight;
        }

        public void setDryWeight(double dryWeight) {
            this.dryWeight = dryWeight;
        }

        public double getOrthogonalityWeight() {
            return orthogonalityWeight;
        }

        public void setOrthogonalityWeight(double orthogonalityWeight) {
            this.orthogonalityWeight = orthogonalityWeight;
        }

        public double getCorrectnessWeight() {
            return correctnessWeight;
        }

        public void setCorrectnessWeight(double correctnessWeight) {
            this.correctnessWeight = correctnessWeight;
        }

        public double getPerformanceWeight() {
            return performanceWeight;
        }

        public void setPerformanceWeight(double performanceWeight) {
            this.performanceWeight = performanceWeight;
        }

        /**
         * Validates that weights sum to 1.0.
         */
        public boolean isValid() {
            double sum = dryWeight + orthogonalityWeight + correctnessWeight + performanceWeight;
            return Math.abs(sum - 1.0) < 0.001; // Allow small floating point errors
        }
    }

    /**
     * Analysis options configuration.
     */
    public static class AnalysisOptions {
        private boolean incrementalAnalysis = false;
        private boolean parallelAnalysis = true;
        private int maxThreads = Runtime.getRuntime().availableProcessors();
        private boolean generateReports = true;
        private String reportFormat = "json"; // json, html, pdf, both
        private boolean failOnCritical = false;
        private int minQualityScore = 0;
        private int maxCriticalIssues = -1; // -1 means unlimited

        public boolean isIncrementalAnalysis() {
            return incrementalAnalysis;
        }

        public void setIncrementalAnalysis(boolean incrementalAnalysis) {
            this.incrementalAnalysis = incrementalAnalysis;
        }

        public boolean isParallelAnalysis() {
            return parallelAnalysis;
        }

        public void setParallelAnalysis(boolean parallelAnalysis) {
            this.parallelAnalysis = parallelAnalysis;
        }

        public int getMaxThreads() {
            return maxThreads;
        }

        public void setMaxThreads(int maxThreads) {
            this.maxThreads = maxThreads;
        }

        public boolean isGenerateReports() {
            return generateReports;
        }

        public void setGenerateReports(boolean generateReports) {
            this.generateReports = generateReports;
        }

        public String getReportFormat() {
            return reportFormat;
        }

        public void setReportFormat(String reportFormat) {
            this.reportFormat = reportFormat;
        }

        public boolean isFailOnCritical() {
            return failOnCritical;
        }

        public void setFailOnCritical(boolean failOnCritical) {
            this.failOnCritical = failOnCritical;
        }

        public int getMinQualityScore() {
            return minQualityScore;
        }

        public void setMinQualityScore(int minQualityScore) {
            this.minQualityScore = minQualityScore;
        }

        public int getMaxCriticalIssues() {
            return maxCriticalIssues;
        }

        public void setMaxCriticalIssues(int maxCriticalIssues) {
            this.maxCriticalIssues = maxCriticalIssues;
        }

        /**
         * Merges with another AnalysisOptions (other takes precedence).
         */
        public void merge(AnalysisOptions other) {
            if (other == null) return;

            this.incrementalAnalysis = other.incrementalAnalysis;
            this.parallelAnalysis = other.parallelAnalysis;
            this.maxThreads = other.maxThreads;
            this.generateReports = other.generateReports;
            this.reportFormat = other.reportFormat;
            this.failOnCritical = other.failOnCritical;
            this.minQualityScore = other.minQualityScore;
            this.maxCriticalIssues = other.maxCriticalIssues;
        }
    }

    @Override
    public String toString() {
        return "PragmiteConfig{" +
                "thresholds=" + thresholds.size() + " items, " +
                "excludePatterns=" + excludePatterns.size() + " patterns, " +
                "severityOverrides=" + severityOverrides.size() + " overrides, " +
                "qualityWeights=" + qualityWeights + ", " +
                "analysisOptions=" + analysisOptions +
                '}';
    }
}

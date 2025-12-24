package com.pragmite.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Configuration for code analysis.
 * Supports loading from .pragmite.properties or programmatic configuration.
 */
public class AnalysisConfig {

    // Method complexity thresholds
    private int longMethodThreshold = 30;
    private int cyclomaticComplexityThreshold = 10;
    private int nestingDepthThreshold = 4;
    private int parameterCountThreshold = 6;

    // Class complexity thresholds
    private int godClassMethodCount = 20;
    private int godClassFieldCount = 15;
    private int largeClassLineCount = 300;
    private int lazyClassLineCount = 50;
    private int lazyClassMethodCount = 2;

    // Code quality thresholds
    private int duplicateCodeMinLines = 6;
    private double duplicateCodeSimilarity = 0.85;
    private int dataClumpsMinParams = 3;
    private int dataClumpsMinOccurrences = 2;
    private double featureEnvyThreshold = 0.60;
    private int inappropriateIntimacyThreshold = 8;

    // Performance thresholds
    private boolean enableParallelAnalysis = true;
    private int parallelThreads = Runtime.getRuntime().availableProcessors();

    // Excluded patterns
    private Set<String> excludePatterns = new HashSet<>(Arrays.asList(
        "**/target/**",
        "**/build/**",
        "**/.git/**",
        "**/node_modules/**"
    ));

    // Enabled detectors (all enabled by default)
    private Set<String> disabledDetectors = new HashSet<>();

    /**
     * Loads configuration from .pragmite.properties file in project root.
     */
    public static AnalysisConfig fromFile(Path configFile) throws IOException {
        AnalysisConfig config = new AnalysisConfig();

        if (!Files.exists(configFile)) {
            return config; // Return defaults
        }

        Properties props = new Properties();
        try (InputStream in = Files.newInputStream(configFile)) {
            props.load(in);
        }

        // Load method thresholds
        config.longMethodThreshold = getIntProperty(props, "method.longMethod.threshold", 30);
        config.cyclomaticComplexityThreshold = getIntProperty(props, "method.cyclomaticComplexity.threshold", 10);
        config.nestingDepthThreshold = getIntProperty(props, "method.nestingDepth.threshold", 4);
        config.parameterCountThreshold = getIntProperty(props, "method.parameterCount.threshold", 6);

        // Load class thresholds
        config.godClassMethodCount = getIntProperty(props, "class.godClass.methodCount", 20);
        config.godClassFieldCount = getIntProperty(props, "class.godClass.fieldCount", 15);
        config.largeClassLineCount = getIntProperty(props, "class.largeClass.lineCount", 300);
        config.lazyClassLineCount = getIntProperty(props, "class.lazyClass.lineCount", 50);
        config.lazyClassMethodCount = getIntProperty(props, "class.lazyClass.methodCount", 2);

        // Load code quality thresholds
        config.duplicateCodeMinLines = getIntProperty(props, "quality.duplicateCode.minLines", 6);
        config.duplicateCodeSimilarity = getDoubleProperty(props, "quality.duplicateCode.similarity", 0.85);
        config.dataClumpsMinParams = getIntProperty(props, "quality.dataClumps.minParams", 3);
        config.dataClumpsMinOccurrences = getIntProperty(props, "quality.dataClumps.minOccurrences", 2);
        config.featureEnvyThreshold = getDoubleProperty(props, "quality.featureEnvy.threshold", 0.60);
        config.inappropriateIntimacyThreshold = getIntProperty(props, "quality.inappropriateIntimacy.threshold", 8);

        // Load performance settings
        config.enableParallelAnalysis = getBooleanProperty(props, "performance.parallel.enabled", true);
        config.parallelThreads = getIntProperty(props, "performance.parallel.threads",
            Runtime.getRuntime().availableProcessors());

        // Load exclude patterns
        String excludes = props.getProperty("analysis.exclude");
        if (excludes != null && !excludes.trim().isEmpty()) {
            config.excludePatterns = new HashSet<>(Arrays.asList(excludes.split(",")));
        }

        // Load disabled detectors
        String disabled = props.getProperty("detectors.disabled");
        if (disabled != null && !disabled.trim().isEmpty()) {
            config.disabledDetectors = new HashSet<>(Arrays.asList(disabled.split(",")));
        }

        return config;
    }

    /**
     * Creates default configuration.
     */
    public static AnalysisConfig defaultConfig() {
        return new AnalysisConfig();
    }

    // Getters
    public int getLongMethodThreshold() { return longMethodThreshold; }
    public int getCyclomaticComplexityThreshold() { return cyclomaticComplexityThreshold; }
    public int getNestingDepthThreshold() { return nestingDepthThreshold; }
    public int getParameterCountThreshold() { return parameterCountThreshold; }
    public int getGodClassMethodCount() { return godClassMethodCount; }
    public int getGodClassFieldCount() { return godClassFieldCount; }
    public int getLargeClassLineCount() { return largeClassLineCount; }
    public int getLazyClassLineCount() { return lazyClassLineCount; }
    public int getLazyClassMethodCount() { return lazyClassMethodCount; }
    public int getDuplicateCodeMinLines() { return duplicateCodeMinLines; }
    public double getDuplicateCodeSimilarity() { return duplicateCodeSimilarity; }
    public int getDataClumpsMinParams() { return dataClumpsMinParams; }
    public int getDataClumpsMinOccurrences() { return dataClumpsMinOccurrences; }
    public double getFeatureEnvyThreshold() { return featureEnvyThreshold; }
    public int getInappropriateIntimacyThreshold() { return inappropriateIntimacyThreshold; }
    public boolean isEnableParallelAnalysis() { return enableParallelAnalysis; }
    public int getParallelThreads() { return parallelThreads; }
    public Set<String> getExcludePatterns() { return excludePatterns; }
    public Set<String> getDisabledDetectors() { return disabledDetectors; }

    // Setters for programmatic configuration
    public void setLongMethodThreshold(int value) { this.longMethodThreshold = value; }
    public void setCyclomaticComplexityThreshold(int value) { this.cyclomaticComplexityThreshold = value; }
    public void setNestingDepthThreshold(int value) { this.nestingDepthThreshold = value; }
    public void setParameterCountThreshold(int value) { this.parameterCountThreshold = value; }
    public void setEnableParallelAnalysis(boolean value) { this.enableParallelAnalysis = value; }

    // Helper methods
    private static int getIntProperty(Properties props, String key, int defaultValue) {
        String value = props.getProperty(key);
        if (value != null) {
            try {
                return Integer.parseInt(value.trim());
            } catch (NumberFormatException e) {
                System.err.println("Invalid integer value for " + key + ": " + value);
            }
        }
        return defaultValue;
    }

    private static double getDoubleProperty(Properties props, String key, double defaultValue) {
        String value = props.getProperty(key);
        if (value != null) {
            try {
                return Double.parseDouble(value.trim());
            } catch (NumberFormatException e) {
                System.err.println("Invalid double value for " + key + ": " + value);
            }
        }
        return defaultValue;
    }

    private static boolean getBooleanProperty(Properties props, String key, boolean defaultValue) {
        String value = props.getProperty(key);
        if (value != null) {
            return Boolean.parseBoolean(value.trim());
        }
        return defaultValue;
    }

    /**
     * Exports current configuration to Properties format.
     */
    public Properties toProperties() {
        Properties props = new Properties();

        props.setProperty("method.longMethod.threshold", String.valueOf(longMethodThreshold));
        props.setProperty("method.cyclomaticComplexity.threshold", String.valueOf(cyclomaticComplexityThreshold));
        props.setProperty("method.nestingDepth.threshold", String.valueOf(nestingDepthThreshold));
        props.setProperty("method.parameterCount.threshold", String.valueOf(parameterCountThreshold));

        props.setProperty("class.godClass.methodCount", String.valueOf(godClassMethodCount));
        props.setProperty("class.godClass.fieldCount", String.valueOf(godClassFieldCount));
        props.setProperty("class.largeClass.lineCount", String.valueOf(largeClassLineCount));
        props.setProperty("class.lazyClass.lineCount", String.valueOf(lazyClassLineCount));
        props.setProperty("class.lazyClass.methodCount", String.valueOf(lazyClassMethodCount));

        props.setProperty("quality.duplicateCode.minLines", String.valueOf(duplicateCodeMinLines));
        props.setProperty("quality.duplicateCode.similarity", String.valueOf(duplicateCodeSimilarity));
        props.setProperty("quality.dataClumps.minParams", String.valueOf(dataClumpsMinParams));
        props.setProperty("quality.dataClumps.minOccurrences", String.valueOf(dataClumpsMinOccurrences));
        props.setProperty("quality.featureEnvy.threshold", String.valueOf(featureEnvyThreshold));
        props.setProperty("quality.inappropriateIntimacy.threshold", String.valueOf(inappropriateIntimacyThreshold));

        props.setProperty("performance.parallel.enabled", String.valueOf(enableParallelAnalysis));
        props.setProperty("performance.parallel.threads", String.valueOf(parallelThreads));

        props.setProperty("analysis.exclude", String.join(",", excludePatterns));
        props.setProperty("detectors.disabled", String.join(",", disabledDetectors));

        return props;
    }
}

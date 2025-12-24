package com.pragmite.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * Configuration loader for Pragmite.
 * Loads settings from .pragmite.yaml file with sensible defaults.
 *
 * Priority:
 * 1. Project root .pragmite.yaml
 * 2. User home .pragmite.yaml
 * 3. Built-in defaults
 */
public class PragmiteConfig {
    private static final Logger logger = LoggerFactory.getLogger(PragmiteConfig.class);
    private static final String CONFIG_FILE = ".pragmite.yaml";

    // Detection thresholds
    private int longMethodThreshold = 30;
    private int longParameterListThreshold = 4;
    private int complexityThreshold = 10;
    private int nestingDepthThreshold = 4;
    private int largeClassThreshold = 500;
    private int godClassMethodThreshold = 20;
    private int longLineThreshold = 120;
    private int magicNumberThreshold = 5;
    private int stringLiteralThreshold = 3;

    // Refactoring settings
    private boolean autoApplyRefactorings = false;
    private boolean createBackups = true;
    private int backupRetentionCount = 10;
    private boolean runTestsAfterRefactoring = true;
    private boolean rollbackOnTestFailure = true;

    // Performance settings
    private boolean enableCaching = true;
    private boolean enableParallelAnalysis = true;
    private int parallelThreadCount = Runtime.getRuntime().availableProcessors();
    private boolean enableProfiling = false;

    // Output settings
    private String outputFormat = "json";
    private boolean verboseLogging = false;
    private boolean generateReports = true;

    // Exclusions
    private List<String> excludedPaths = Arrays.asList("target", "build", "node_modules", ".git");
    private List<String> excludedFiles = Arrays.asList("*Test.java", "*Tests.java");

    public static PragmiteConfig loadConfig() {
        return loadConfig(null);
    }

    public static PragmiteConfig loadConfig(Path projectRoot) {
        PragmiteConfig config = new PragmiteConfig();

        // Try to load from project root
        if (projectRoot != null) {
            Path projectConfig = projectRoot.resolve(CONFIG_FILE);
            if (Files.exists(projectConfig)) {
                logger.info("Loading config from: {}", projectConfig);
                config.loadFromFile(projectConfig);
                return config;
            }
        }

        // Try to load from user home
        Path homeConfig = Paths.get(System.getProperty("user.home"), CONFIG_FILE);
        if (Files.exists(homeConfig)) {
            logger.info("Loading config from: {}", homeConfig);
            config.loadFromFile(homeConfig);
            return config;
        }

        // Use defaults
        logger.info("Using default configuration");
        return config;
    }

    @SuppressWarnings("unchecked")
    private void loadFromFile(Path configFile) {
        try (InputStream input = Files.newInputStream(configFile)) {
            Yaml yaml = new Yaml();
            Map<String, Object> data = yaml.load(input);

            if (data == null) {
                logger.warn("Config file is empty, using defaults");
                return;
            }

            // Load detection thresholds
            if (data.containsKey("thresholds")) {
                Map<String, Object> thresholds = (Map<String, Object>) data.get("thresholds");
                longMethodThreshold = getIntValue(thresholds, "long_method", longMethodThreshold);
                longParameterListThreshold = getIntValue(thresholds, "long_parameter_list", longParameterListThreshold);
                complexityThreshold = getIntValue(thresholds, "complexity", complexityThreshold);
                nestingDepthThreshold = getIntValue(thresholds, "nesting_depth", nestingDepthThreshold);
                largeClassThreshold = getIntValue(thresholds, "large_class", largeClassThreshold);
                godClassMethodThreshold = getIntValue(thresholds, "god_class_methods", godClassMethodThreshold);
                longLineThreshold = getIntValue(thresholds, "long_line", longLineThreshold);
                magicNumberThreshold = getIntValue(thresholds, "magic_numbers", magicNumberThreshold);
                stringLiteralThreshold = getIntValue(thresholds, "string_literals", stringLiteralThreshold);
            }

            // Load refactoring settings
            if (data.containsKey("refactoring")) {
                Map<String, Object> refactoring = (Map<String, Object>) data.get("refactoring");
                autoApplyRefactorings = getBoolValue(refactoring, "auto_apply", autoApplyRefactorings);
                createBackups = getBoolValue(refactoring, "create_backups", createBackups);
                backupRetentionCount = getIntValue(refactoring, "backup_retention", backupRetentionCount);
                runTestsAfterRefactoring = getBoolValue(refactoring, "run_tests", runTestsAfterRefactoring);
                rollbackOnTestFailure = getBoolValue(refactoring, "rollback_on_failure", rollbackOnTestFailure);
            }

            // Load performance settings
            if (data.containsKey("performance")) {
                Map<String, Object> performance = (Map<String, Object>) data.get("performance");
                enableCaching = getBoolValue(performance, "enable_caching", enableCaching);
                enableParallelAnalysis = getBoolValue(performance, "enable_parallel", enableParallelAnalysis);
                parallelThreadCount = getIntValue(performance, "thread_count", parallelThreadCount);
                enableProfiling = getBoolValue(performance, "enable_profiling", enableProfiling);
            }

            // Load output settings
            if (data.containsKey("output")) {
                Map<String, Object> output = (Map<String, Object>) data.get("output");
                outputFormat = getStringValue(output, "format", outputFormat);
                verboseLogging = getBoolValue(output, "verbose", verboseLogging);
                generateReports = getBoolValue(output, "generate_reports", generateReports);
            }

            // Load exclusions
            if (data.containsKey("exclusions")) {
                Map<String, Object> exclusions = (Map<String, Object>) data.get("exclusions");
                if (exclusions.containsKey("paths")) {
                    excludedPaths = (List<String>) exclusions.get("paths");
                }
                if (exclusions.containsKey("files")) {
                    excludedFiles = (List<String>) exclusions.get("files");
                }
            }

            logger.info("Configuration loaded successfully");

        } catch (IOException e) {
            logger.error("Failed to load config file: {}", configFile, e);
        } catch (Exception e) {
            logger.error("Error parsing config file: {}", configFile, e);
        }
    }

    private int getIntValue(Map<String, Object> map, String key, int defaultValue) {
        Object value = map.get(key);
        if (value instanceof Integer) return (Integer) value;
        if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                logger.warn("Invalid integer value for {}: {}", key, value);
            }
        }
        return defaultValue;
    }

    private boolean getBoolValue(Map<String, Object> map, String key, boolean defaultValue) {
        Object value = map.get(key);
        if (value instanceof Boolean) return (Boolean) value;
        if (value instanceof String) {
            return Boolean.parseBoolean((String) value);
        }
        return defaultValue;
    }

    private String getStringValue(Map<String, Object> map, String key, String defaultValue) {
        Object value = map.get(key);
        return value != null ? value.toString() : defaultValue;
    }

    // Getters
    public int getLongMethodThreshold() { return longMethodThreshold; }
    public int getLongParameterListThreshold() { return longParameterListThreshold; }
    public int getComplexityThreshold() { return complexityThreshold; }
    public int getNestingDepthThreshold() { return nestingDepthThreshold; }
    public int getLargeClassThreshold() { return largeClassThreshold; }
    public int getGodClassMethodThreshold() { return godClassMethodThreshold; }
    public int getLongLineThreshold() { return longLineThreshold; }
    public int getMagicNumberThreshold() { return magicNumberThreshold; }
    public int getStringLiteralThreshold() { return stringLiteralThreshold; }

    public boolean isAutoApplyRefactorings() { return autoApplyRefactorings; }
    public boolean isCreateBackups() { return createBackups; }
    public int getBackupRetentionCount() { return backupRetentionCount; }
    public boolean isRunTestsAfterRefactoring() { return runTestsAfterRefactoring; }
    public boolean isRollbackOnTestFailure() { return rollbackOnTestFailure; }

    public boolean isEnableCaching() { return enableCaching; }
    public boolean isEnableParallelAnalysis() { return enableParallelAnalysis; }
    public int getParallelThreadCount() { return parallelThreadCount; }
    public boolean isEnableProfiling() { return enableProfiling; }

    public String getOutputFormat() { return outputFormat; }
    public boolean isVerboseLogging() { return verboseLogging; }
    public boolean isGenerateReports() { return generateReports; }

    public List<String> getExcludedPaths() { return excludedPaths; }
    public List<String> getExcludedFiles() { return excludedFiles; }

    @Override
    public String toString() {
        return String.format("PragmiteConfig{longMethod=%d, complexity=%d, parallel=%s, caching=%s}",
                           longMethodThreshold, complexityThreshold, enableParallelAnalysis, enableCaching);
    }
}

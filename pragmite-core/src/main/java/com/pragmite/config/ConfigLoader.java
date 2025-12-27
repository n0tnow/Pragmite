package com.pragmite.config;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.error.YAMLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * Loads Pragmite configuration from .pragmite.yaml file.
 *
 * Search order:
 * 1. .pragmite.yaml in project root
 * 2. .pragmite.yml in project root
 * 3. Default configuration (hardcoded)
 *
 * Also loads .pragmite-defaults.yaml if exists (for inheritance).
 */
public class ConfigLoader {

    private static final Logger logger = LoggerFactory.getLogger(ConfigLoader.class);

    private static final String[] CONFIG_FILENAMES = {
        ".pragmite.yaml",
        ".pragmite.yml"
    };

    private static final String DEFAULTS_FILENAME = ".pragmite-defaults.yaml";

    /**
     * Loads configuration from the project root directory.
     */
    public static PragmiteConfig load(Path projectRoot) {
        PragmiteConfig config = new PragmiteConfig(); // Start with defaults

        // Try to load defaults file first (if exists)
        Path defaultsPath = projectRoot.resolve(DEFAULTS_FILENAME);
        if (Files.exists(defaultsPath)) {
            try {
                PragmiteConfig defaults = loadFromFile(defaultsPath);
                config.merge(defaults);
                logger.info("Loaded defaults from: {}", defaultsPath);
            } catch (Exception e) {
                logger.warn("Failed to load defaults from {}: {}", defaultsPath, e.getMessage());
            }
        }

        // Try to load project-specific config
        for (String filename : CONFIG_FILENAMES) {
            Path configPath = projectRoot.resolve(filename);
            if (Files.exists(configPath)) {
                try {
                    PragmiteConfig projectConfig = loadFromFile(configPath);
                    config.merge(projectConfig);
                    logger.info("Loaded configuration from: {}", configPath);
                    return config;
                } catch (Exception e) {
                    logger.error("Failed to load configuration from {}: {}", configPath, e.getMessage());
                    throw new ConfigurationException("Failed to load configuration from " + configPath, e);
                }
            }
        }

        // No config file found, use defaults
        logger.info("No configuration file found, using default configuration");
        return config;
    }

    /**
     * Loads configuration from a specific file.
     */
    public static PragmiteConfig loadFromFile(Path configPath) throws IOException {
        logger.debug("Loading configuration from: {}", configPath);

        try (InputStream input = Files.newInputStream(configPath)) {
            Yaml yaml = new Yaml();
            ConfigData data = yaml.loadAs(input, ConfigData.class);

            PragmiteConfig config = convertToConfig(data);

            // Validate configuration
            validateConfig(config);

            return config;
        } catch (YAMLException e) {
            throw new IOException("Invalid YAML format in " + configPath + ": " + e.getMessage(), e);
        }
    }

    /**
     * Converts YAML data to PragmiteConfig object.
     */
    private static PragmiteConfig convertToConfig(ConfigData data) {
        PragmiteConfig config = new PragmiteConfig();

        if (data == null) {
            return config; // Return defaults
        }

        // Thresholds
        if (data.thresholds != null) {
            config.setThresholds(data.thresholds);
        }

        // Exclude patterns
        if (data.exclude != null) {
            config.setExcludePatterns(data.exclude);
        }

        // Severity overrides
        if (data.severity != null) {
            config.setSeverityOverrides(data.severity);
        }

        // Quality weights
        if (data.qualityWeights != null) {
            PragmiteConfig.QualityWeights weights = new PragmiteConfig.QualityWeights();
            if (data.qualityWeights.dry != null) {
                weights.setDryWeight(data.qualityWeights.dry);
            }
            if (data.qualityWeights.orthogonality != null) {
                weights.setOrthogonalityWeight(data.qualityWeights.orthogonality);
            }
            if (data.qualityWeights.correctness != null) {
                weights.setCorrectnessWeight(data.qualityWeights.correctness);
            }
            if (data.qualityWeights.performance != null) {
                weights.setPerformanceWeight(data.qualityWeights.performance);
            }
            config.setQualityWeights(weights);
        }

        // Analysis options
        if (data.analysis != null) {
            PragmiteConfig.AnalysisOptions options = new PragmiteConfig.AnalysisOptions();
            if (data.analysis.incremental != null) {
                options.setIncrementalAnalysis(data.analysis.incremental);
            }
            if (data.analysis.parallel != null) {
                options.setParallelAnalysis(data.analysis.parallel);
            }
            if (data.analysis.maxThreads != null) {
                options.setMaxThreads(data.analysis.maxThreads);
            }
            if (data.analysis.reportFormat != null) {
                options.setReportFormat(data.analysis.reportFormat);
            }
            if (data.analysis.failOnCritical != null) {
                options.setFailOnCritical(data.analysis.failOnCritical);
            }
            if (data.analysis.minQualityScore != null) {
                options.setMinQualityScore(data.analysis.minQualityScore);
            }
            if (data.analysis.maxCriticalIssues != null) {
                options.setMaxCriticalIssues(data.analysis.maxCriticalIssues);
            }
            config.setAnalysisOptions(options);
        }

        return config;
    }

    /**
     * Validates configuration values.
     */
    private static void validateConfig(PragmiteConfig config) {
        // Validate quality weights
        PragmiteConfig.QualityWeights weights = config.getQualityWeights();
        if (!weights.isValid()) {
            throw new ConfigurationException(
                "Quality weights must sum to 1.0, got: " +
                (weights.getDryWeight() + weights.getOrthogonalityWeight() +
                 weights.getCorrectnessWeight() + weights.getPerformanceWeight())
            );
        }

        // Validate thresholds (must be positive)
        for (Map.Entry<String, Integer> entry : config.getThresholds().entrySet()) {
            if (entry.getValue() < 0) {
                throw new ConfigurationException(
                    "Threshold '" + entry.getKey() + "' must be positive, got: " + entry.getValue()
                );
            }
        }

        // Validate report format
        String format = config.getAnalysisOptions().getReportFormat();
        if (!format.matches("json|html|pdf|both")) {
            throw new ConfigurationException(
                "Invalid report format: " + format + " (must be: json, html, pdf, or both)"
            );
        }

        logger.debug("Configuration validated successfully");
    }

    /**
     * Creates a default .pragmite.yaml template file.
     */
    public static void createTemplate(Path outputPath) throws IOException {
        String template = generateTemplate();
        Files.writeString(outputPath, template);
        logger.info("Created configuration template at: {}", outputPath);
    }

    /**
     * Generates the YAML template content.
     */
    private static String generateTemplate() {
        return """
# Pragmite Configuration File
# Documentation: https://github.com/n0tnow/Pragmite#configuration

# Custom thresholds for code smell detectors
thresholds:
  cyclomaticComplexity: 15      # Maximum cyclomatic complexity
  longMethod: 50                # Maximum method lines
  largeClass.lines: 400         # Maximum class lines
  largeClass.methods: 25        # Maximum methods per class
  longParameterList: 5          # Maximum parameters
  deepNesting: 5                # Maximum nesting depth
  switchStatement: 7            # Maximum switch cases
  tooManyLiterals.numeric: 7    # Maximum numeric literals
  tooManyLiterals.string: 5     # Maximum string literals
  lazyClass: 80                 # Minimum class lines

# Files/directories to exclude (glob patterns)
exclude:
  - "**/target/**"
  - "**/build/**"
  - "**/node_modules/**"
  - "**/.git/**"
  - "**/test-output/**"
  # Add your custom excludes here:
  # - "**/generated/**"
  # - "**/vendor/**"

# Severity overrides (default is based on code smell type)
# Options: ERROR, WARNING, INFO
severity:
  # DUPLICATED_CODE: ERROR
  # MAGIC_NUMBER: WARNING
  # UNUSED_VARIABLE: INFO

# Quality score weights (must sum to 1.0)
qualityWeights:
  dry: 0.30              # DRY (Don't Repeat Yourself)
  orthogonality: 0.30    # Orthogonality (low coupling)
  correctness: 0.25      # Correctness (no bugs)
  performance: 0.15      # Performance (efficiency)

# Analysis options
analysis:
  incremental: false          # Enable incremental analysis (cache)
  parallel: true              # Enable parallel analysis
  maxThreads: 8               # Maximum threads for parallel analysis
  reportFormat: json          # Report format: json, html, pdf, both

  # CI/CD Quality Gate options
  failOnCritical: false       # Exit with code 1 if critical issues found
  minQualityScore: 0          # Minimum quality score (0-100)
  maxCriticalIssues: -1       # Maximum critical issues (-1 = unlimited)
""";
    }

    /**
     * YAML data structure for deserialization.
     */
    public static class ConfigData {
        public Map<String, Integer> thresholds;
        public List<String> exclude;
        public Map<String, String> severity;
        public QualityWeightsData qualityWeights;
        public AnalysisData analysis;
    }

    public static class QualityWeightsData {
        public Double dry;
        public Double orthogonality;
        public Double correctness;
        public Double performance;
    }

    public static class AnalysisData {
        public Boolean incremental;
        public Boolean parallel;
        public Integer maxThreads;
        public String reportFormat;
        public Boolean failOnCritical;
        public Integer minQualityScore;
        public Integer maxCriticalIssues;

        // Setters for snake_case YAML compatibility (v1.6.3 - Config test fix)
        public void setIncrementalAnalysis(Boolean incremental) { this.incremental = incremental; }
        public void setParallelAnalysis(Boolean parallel) { this.parallel = parallel; }
        public void setMaxThreads(Integer maxThreads) { this.maxThreads = maxThreads; }
        public void setReportFormat(String reportFormat) { this.reportFormat = reportFormat; }
        public void setFailOnCritical(Boolean failOnCritical) { this.failOnCritical = failOnCritical; }
        public void setMinQualityScore(Integer minQualityScore) { this.minQualityScore = minQualityScore; }
        public void setMaxCriticalIssues(Integer maxCriticalIssues) { this.maxCriticalIssues = maxCriticalIssues; }
    }

    /**
     * Exception thrown when configuration is invalid.
     */
    public static class ConfigurationException extends RuntimeException {
        public ConfigurationException(String message) {
            super(message);
        }

        public ConfigurationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}

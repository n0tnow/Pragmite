package com.pragmite.ai;

/**
 * Configuration for AI API providers (Claude, GPT-4, Gemini).
 * Supports multiple configuration sources: environment variables, config file, CLI args.
 *
 * @since 1.4.0
 */
public class ApiConfig {

    private String provider;      // "claude", "gpt-4", "gemini"
    private String apiKey;
    private String model;
    private boolean enabled;
    private boolean cacheResults;
    private int maxRetries;
    private int timeoutSeconds;

    public ApiConfig() {
        // Default values
        this.provider = "claude";
        this.model = "claude-sonnet-4-5";
        this.enabled = false;
        this.cacheResults = true;
        this.maxRetries = 3;
        this.timeoutSeconds = 30;
    }

    /**
     * Load configuration from environment variables.
     * Priority: CLI args > Environment variables > Config file > Defaults
     */
    public static ApiConfig fromEnvironment() {
        ApiConfig config = new ApiConfig();

        // Try to load API key from environment
        String apiKey = System.getenv("CLAUDE_API_KEY");
        if (apiKey == null) {
            apiKey = System.getenv("ANTHROPIC_API_KEY");
        }

        if (apiKey != null && !apiKey.isEmpty()) {
            config.setApiKey(apiKey);
            config.setEnabled(true);
        }

        // Allow override of model
        String model = System.getenv("CLAUDE_MODEL");
        if (model != null && !model.isEmpty()) {
            config.setModel(model);
        }

        return config;
    }

    /**
     * Validate that configuration is complete and valid.
     */
    public boolean isValid() {
        return enabled && apiKey != null && !apiKey.isEmpty() && model != null && !model.isEmpty();
    }

    // Getters and Setters

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isCacheResults() {
        return cacheResults;
    }

    public void setCacheResults(boolean cacheResults) {
        this.cacheResults = cacheResults;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public void setMaxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
    }

    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public void setTimeoutSeconds(int timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }
}

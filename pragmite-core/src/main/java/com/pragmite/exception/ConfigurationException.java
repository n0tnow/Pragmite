package com.pragmite.exception;

/**
 * Exception for configuration-related errors.
 */
public class ConfigurationException extends PragmiteException {

    private final String configKey;

    public ConfigurationException(ErrorCode errorCode, String message) {
        super(errorCode, message);
        this.configKey = null;
    }

    public ConfigurationException(ErrorCode errorCode, String configKey, String message) {
        super(errorCode, String.format("Configuration error for '%s': %s", configKey, message));
        this.configKey = configKey;
    }

    public ConfigurationException(ErrorCode errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
        this.configKey = null;
    }

    public String getConfigKey() {
        return configKey;
    }
}

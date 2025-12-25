package com.pragmite.ai;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents AI-generated refactored code with explanations and metrics.
 *
 * @since 1.4.0
 */
public class RefactoredCode {

    private final String originalCode;
    private final String refactoredCode;
    private final String explanation;
    private final List<String> changes;
    private final Map<String, String> beforeMetrics;
    private final Map<String, String> afterMetrics;
    private final String whyBetter;
    private final boolean successful;
    private final String errorMessage;

    private RefactoredCode(Builder builder) {
        this.originalCode = builder.originalCode;
        this.refactoredCode = builder.refactoredCode;
        this.explanation = builder.explanation;
        this.changes = new ArrayList<>(builder.changes);
        this.beforeMetrics = new HashMap<>(builder.beforeMetrics);
        this.afterMetrics = new HashMap<>(builder.afterMetrics);
        this.whyBetter = builder.whyBetter;
        this.successful = builder.successful;
        this.errorMessage = builder.errorMessage;
    }

    public static Builder builder() {
        return new Builder();
    }

    // Getters

    public String getOriginalCode() {
        return originalCode;
    }

    public String getRefactoredCode() {
        return refactoredCode;
    }

    public String getExplanation() {
        return explanation;
    }

    public List<String> getChanges() {
        return new ArrayList<>(changes);
    }

    public Map<String, String> getBeforeMetrics() {
        return new HashMap<>(beforeMetrics);
    }

    public Map<String, String> getAfterMetrics() {
        return new HashMap<>(afterMetrics);
    }

    public String getWhyBetter() {
        return whyBetter;
    }

    public boolean isSuccessful() {
        return successful;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * Convert to JSON format.
     */
    public String toJSON() {
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"successful\": ").append(successful).append(",\n");

        if (successful) {
            json.append("  \"originalCode\": \"").append(escapeJson(originalCode)).append("\",\n");
            json.append("  \"refactoredCode\": \"").append(escapeJson(refactoredCode)).append("\",\n");
            json.append("  \"explanation\": \"").append(escapeJson(explanation)).append("\",\n");
            json.append("  \"whyBetter\": \"").append(escapeJson(whyBetter)).append("\",\n");

            json.append("  \"changes\": [");
            for (int i = 0; i < changes.size(); i++) {
                json.append("\"").append(escapeJson(changes.get(i))).append("\"");
                if (i < changes.size() - 1) json.append(", ");
            }
            json.append("],\n");

            json.append("  \"beforeMetrics\": ").append(metricsToJson(beforeMetrics)).append(",\n");
            json.append("  \"afterMetrics\": ").append(metricsToJson(afterMetrics)).append("\n");
        } else {
            json.append("  \"error\": \"").append(escapeJson(errorMessage)).append("\"\n");
        }

        json.append("}");
        return json.toString();
    }

    private String metricsToJson(Map<String, String> metrics) {
        StringBuilder json = new StringBuilder("{");
        int count = 0;
        for (Map.Entry<String, String> entry : metrics.entrySet()) {
            if (count > 0) json.append(", ");
            json.append("\"").append(escapeJson(entry.getKey())).append("\": \"")
                .append(escapeJson(entry.getValue())).append("\"");
            count++;
        }
        json.append("}");
        return json.toString();
    }

    private String escapeJson(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t");
    }

    /**
     * Builder pattern for RefactoredCode.
     */
    public static class Builder {
        private String originalCode = "";
        private String refactoredCode = "";
        private String explanation = "";
        private List<String> changes = new ArrayList<>();
        private Map<String, String> beforeMetrics = new HashMap<>();
        private Map<String, String> afterMetrics = new HashMap<>();
        private String whyBetter = "";
        private boolean successful = true;
        private String errorMessage = "";

        public Builder originalCode(String originalCode) {
            this.originalCode = originalCode;
            return this;
        }

        public Builder refactoredCode(String refactoredCode) {
            this.refactoredCode = refactoredCode;
            return this;
        }

        public Builder explanation(String explanation) {
            this.explanation = explanation;
            return this;
        }

        public Builder addChange(String change) {
            this.changes.add(change);
            return this;
        }

        public Builder addBeforeMetric(String key, String value) {
            this.beforeMetrics.put(key, value);
            return this;
        }

        public Builder addAfterMetric(String key, String value) {
            this.afterMetrics.put(key, value);
            return this;
        }

        public Builder whyBetter(String whyBetter) {
            this.whyBetter = whyBetter;
            return this;
        }

        public Builder successful(boolean successful) {
            this.successful = successful;
            return this;
        }

        public Builder errorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
            return this;
        }

        public RefactoredCode build() {
            return new RefactoredCode(this);
        }
    }
}

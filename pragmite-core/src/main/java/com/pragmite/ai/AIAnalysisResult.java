package com.pragmite.ai;

import com.pragmite.model.CodeSmell;
import com.pragmite.model.CodeSmellType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents the AI-powered analysis result for a code smell.
 * Contains detailed explanations, root cause analysis, impact assessment,
 * and ready-to-use AI prompts for developers.
 *
 * @since 1.4.0
 */
public class AIAnalysisResult {

    private final CodeSmell originalSmell;
    private final String rootCause;
    private final String impact;
    private final String recommendation;
    private final String aiPrompt;
    private final List<String> codeSnippets;
    private final Map<String, String> metadata;
    private final RefactoredCode refactoredCode;  // v1.4.0 Phase 2

    private AIAnalysisResult(Builder builder) {
        this.originalSmell = builder.originalSmell;
        this.rootCause = builder.rootCause;
        this.impact = builder.impact;
        this.recommendation = builder.recommendation;
        this.aiPrompt = builder.aiPrompt;
        this.codeSnippets = new ArrayList<>(builder.codeSnippets);
        this.metadata = new HashMap<>(builder.metadata);
        this.refactoredCode = builder.refactoredCode;
    }

    public CodeSmell getOriginalSmell() {
        return originalSmell;
    }

    public String getRootCause() {
        return rootCause;
    }

    public String getImpact() {
        return impact;
    }

    public String getRecommendation() {
        return recommendation;
    }

    public String getAiPrompt() {
        return aiPrompt;
    }

    public List<String> getCodeSnippets() {
        return new ArrayList<>(codeSnippets);
    }

    public Map<String, String> getMetadata() {
        return new HashMap<>(metadata);
    }

    public CodeSmellType getSmellType() {
        return originalSmell.getType();
    }

    public String getFilePath() {
        return originalSmell.getFilePath();
    }

    public int getLineNumber() {
        return originalSmell.getLine();
    }

    public RefactoredCode getRefactoredCode() {
        return refactoredCode;
    }

    public boolean hasRefactoredCode() {
        return refactoredCode != null && refactoredCode.isSuccessful();
    }

    /**
     * Returns a formatted text representation suitable for console output.
     */
    public String toConsoleFormat() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n╔════════════════════════════════════════════════════════════════════════════╗\n");
        sb.append("║ AI-POWERED ANALYSIS                                                        ║\n");
        sb.append("╚════════════════════════════════════════════════════════════════════════════╝\n\n");

        sb.append("📍 Location: ").append(originalSmell.getFilePath())
          .append(":").append(originalSmell.getLine()).append("\n");
        sb.append("🔍 Issue Type: ").append(originalSmell.getType()).append("\n\n");

        sb.append("🎯 Root Cause:\n");
        sb.append("   ").append(rootCause).append("\n\n");

        sb.append("⚠️  Impact:\n");
        sb.append("   ").append(impact).append("\n\n");

        sb.append("✅ Recommendation:\n");
        sb.append("   ").append(recommendation).append("\n\n");

        if (!codeSnippets.isEmpty()) {
            sb.append("📝 Code Context:\n");
            for (int i = 0; i < codeSnippets.size(); i++) {
                sb.append("   Snippet ").append(i + 1).append(":\n");
                sb.append("   ```java\n");
                sb.append(indentCode(codeSnippets.get(i), 3));
                sb.append("\n   ```\n\n");
            }
        }

        sb.append("🤖 AI Prompt (Copy & Paste to Claude/GPT-4/Gemini):\n");
        sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
        sb.append(aiPrompt).append("\n");
        sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");

        return sb.toString();
    }

    /**
     * Returns a JSON representation suitable for API/file output.
     */
    public String toJSON() {
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"file\": \"").append(escapeJSON(originalSmell.getFilePath())).append("\",\n");
        json.append("  \"line\": ").append(originalSmell.getLine()).append(",\n");
        json.append("  \"type\": \"").append(originalSmell.getType()).append("\",\n");
        json.append("  \"severity\": \"").append(originalSmell.getSeverity()).append("\",\n");
        json.append("  \"rootCause\": \"").append(escapeJSON(rootCause)).append("\",\n");
        json.append("  \"impact\": \"").append(escapeJSON(impact)).append("\",\n");
        json.append("  \"recommendation\": \"").append(escapeJSON(recommendation)).append("\",\n");
        json.append("  \"aiPrompt\": \"").append(escapeJSON(aiPrompt)).append("\",\n");
        json.append("  \"codeSnippets\": [\n");
        for (int i = 0; i < codeSnippets.size(); i++) {
            json.append("    \"").append(escapeJSON(codeSnippets.get(i))).append("\"");
            if (i < codeSnippets.size() - 1) json.append(",");
            json.append("\n");
        }
        json.append("  ],\n");
        json.append("  \"metadata\": {\n");
        int count = 0;
        for (Map.Entry<String, String> entry : metadata.entrySet()) {
            json.append("    \"").append(escapeJSON(entry.getKey())).append("\": \"")
                .append(escapeJSON(entry.getValue())).append("\"");
            if (++count < metadata.size()) json.append(",");
            json.append("\n");
        }
        json.append("  }");

        // Add refactored code if available (v1.4.0 Phase 2)
        if (hasRefactoredCode()) {
            json.append(",\n");
            json.append("  \"refactoredCode\": ");
            json.append(refactoredCode.toJSON());
        }

        json.append("\n}");
        return json.toString();
    }

    private String indentCode(String code, int spaces) {
        String indent = " ".repeat(spaces);
        return indent + code.replace("\n", "\n" + indent);
    }

    private String escapeJSON(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }

    public static Builder builder(CodeSmell smell) {
        return new Builder(smell);
    }

    public static class Builder {
        private final CodeSmell originalSmell;
        private String rootCause = "";
        private String impact = "";
        private String recommendation = "";
        private String aiPrompt = "";
        private final List<String> codeSnippets = new ArrayList<>();
        private final Map<String, String> metadata = new HashMap<>();
        private RefactoredCode refactoredCode = null;

        private Builder(CodeSmell smell) {
            this.originalSmell = smell;
        }

        public Builder rootCause(String rootCause) {
            this.rootCause = rootCause;
            return this;
        }

        public Builder impact(String impact) {
            this.impact = impact;
            return this;
        }

        public Builder recommendation(String recommendation) {
            this.recommendation = recommendation;
            return this;
        }

        public Builder aiPrompt(String aiPrompt) {
            this.aiPrompt = aiPrompt;
            return this;
        }

        public Builder addCodeSnippet(String snippet) {
            this.codeSnippets.add(snippet);
            return this;
        }

        public Builder addMetadata(String key, String value) {
            this.metadata.put(key, value);
            return this;
        }

        public Builder refactoredCode(RefactoredCode refactoredCode) {
            this.refactoredCode = refactoredCode;
            return this;
        }

        public AIAnalysisResult build() {
            return new AIAnalysisResult(this);
        }
    }
}

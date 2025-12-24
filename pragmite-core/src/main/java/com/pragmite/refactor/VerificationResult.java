package com.pragmite.refactor;

import java.util.ArrayList;
import java.util.List;

/**
 * Result of build/test verification.
 */
public class VerificationResult {
    private String phase;
    private boolean success;
    private int exitCode;
    private List<String> output;

    public VerificationResult() {
        this.output = new ArrayList<>();
    }

    public String getPhase() {
        return phase;
    }

    public void setPhase(String phase) {
        this.phase = phase;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public int getExitCode() {
        return exitCode;
    }

    public void setExitCode(int exitCode) {
        this.exitCode = exitCode;
    }

    public List<String> getOutput() {
        return output;
    }

    public void setOutput(List<String> output) {
        this.output = output;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n=== ").append(phase).append(" Verification ===\n");
        sb.append("Status: ").append(success ? "✅ PASSED" : "❌ FAILED").append("\n");
        sb.append("Exit Code: ").append(exitCode).append("\n");

        if (!output.isEmpty()) {
            sb.append("\nOutput:\n");
            output.forEach(line -> sb.append("  ").append(line).append("\n"));
        }

        sb.append("==============================\n");
        return sb.toString();
    }
}

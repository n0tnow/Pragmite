package com.pragmite.model;

/**
 * Karmaşıklık bilgisi - bir metot veya döngü için.
 */
public class ComplexityInfo {
    private String filePath;
    private String methodName;
    private int line;
    private BigOComplexity timeComplexity;
    private BigOComplexity spaceComplexity;
    private String reason; // Karmaşıklığın sebebi (ör. "nested loop", "recursive call")
    private int nestedLoopDepth;

    public ComplexityInfo() {}

    public ComplexityInfo(String filePath, String methodName, int line, BigOComplexity timeComplexity) {
        this.filePath = filePath;
        this.methodName = methodName;
        this.line = line;
        this.timeComplexity = timeComplexity;
    }

    // Getters and Setters
    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }

    public String getMethodName() { return methodName; }
    public void setMethodName(String methodName) { this.methodName = methodName; }

    public int getLine() { return line; }
    public void setLine(int line) { this.line = line; }

    public BigOComplexity getTimeComplexity() { return timeComplexity; }
    public void setTimeComplexity(BigOComplexity timeComplexity) { this.timeComplexity = timeComplexity; }

    public BigOComplexity getSpaceComplexity() { return spaceComplexity; }
    public void setSpaceComplexity(BigOComplexity spaceComplexity) { this.spaceComplexity = spaceComplexity; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public int getNestedLoopDepth() { return nestedLoopDepth; }
    public void setNestedLoopDepth(int nestedLoopDepth) { this.nestedLoopDepth = nestedLoopDepth; }

    @Override
    public String toString() {
        return String.format("%s.%s (line %d): %s%s",
            filePath, methodName, line,
            timeComplexity != null ? timeComplexity.getNotation() : "?",
            reason != null ? " - " + reason : "");
    }
}

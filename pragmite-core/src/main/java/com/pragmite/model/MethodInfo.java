package com.pragmite.model;

/**
 * Bir metodun bilgilerini tutan sınıf.
 */
public class MethodInfo {
    private String name;
    private String signature;
    private int startLine;
    private int endLine;
    private int lineCount;
    private int parameterCount;
    private int cyclomaticComplexity;
    private BigOComplexity timeComplexity;
    private BigOComplexity spaceComplexity;

    public MethodInfo() {}

    public MethodInfo(String name, int startLine, int endLine) {
        this.name = name;
        this.startLine = startLine;
        this.endLine = endLine;
        this.lineCount = endLine - startLine + 1;
    }

    // Getters and Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSignature() { return signature; }
    public void setSignature(String signature) { this.signature = signature; }

    public int getStartLine() { return startLine; }
    public void setStartLine(int startLine) { this.startLine = startLine; }

    public int getEndLine() { return endLine; }
    public void setEndLine(int endLine) { this.endLine = endLine; }

    public int getLineCount() { return lineCount; }
    public void setLineCount(int lineCount) { this.lineCount = lineCount; }

    public int getParameterCount() { return parameterCount; }
    public void setParameterCount(int parameterCount) { this.parameterCount = parameterCount; }

    public int getCyclomaticComplexity() { return cyclomaticComplexity; }
    public void setCyclomaticComplexity(int cyclomaticComplexity) { this.cyclomaticComplexity = cyclomaticComplexity; }

    public BigOComplexity getTimeComplexity() { return timeComplexity; }
    public void setTimeComplexity(BigOComplexity timeComplexity) { this.timeComplexity = timeComplexity; }

    public BigOComplexity getSpaceComplexity() { return spaceComplexity; }
    public void setSpaceComplexity(BigOComplexity spaceComplexity) { this.spaceComplexity = spaceComplexity; }

    public BigOComplexity getBigOComplexity() {
        // Zaman karmaşıklığını döndür (varsayılan)
        return timeComplexity != null ? timeComplexity : BigOComplexity.O_1;
    }
}

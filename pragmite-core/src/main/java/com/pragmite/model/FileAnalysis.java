package com.pragmite.model;

import com.pragmite.metrics.CKMetrics;

import java.util.ArrayList;
import java.util.List;

/**
 * Tek bir Java dosyasının analiz sonucu.
 */
public class FileAnalysis {
    private String filePath;
    private String className;
    private int lineCount;
    private int methodCount;
    private List<MethodInfo> methods;
    private List<CodeSmell> smells;
    private List<ComplexityInfo> complexities;
    private CKMetrics ckMetrics;

    public FileAnalysis() {
        this.methods = new ArrayList<>();
        this.smells = new ArrayList<>();
        this.complexities = new ArrayList<>();
    }

    public FileAnalysis(String filePath) {
        this();
        this.filePath = filePath;
    }

    // Getters and Setters
    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }

    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }

    public int getLineCount() { return lineCount; }
    public void setLineCount(int lineCount) { this.lineCount = lineCount; }

    public int getMethodCount() { return methodCount; }
    public void setMethodCount(int methodCount) { this.methodCount = methodCount; }

    public List<MethodInfo> getMethods() { return methods; }
    public void setMethods(List<MethodInfo> methods) { this.methods = methods; }

    public List<CodeSmell> getSmells() { return smells; }
    public void setSmells(List<CodeSmell> smells) { this.smells = smells; }

    public List<ComplexityInfo> getComplexities() { return complexities; }
    public void setComplexities(List<ComplexityInfo> complexities) { this.complexities = complexities; }

    public void addMethod(MethodInfo method) {
        this.methods.add(method);
    }

    public void addSmell(CodeSmell smell) {
        this.smells.add(smell);
    }

    public void addComplexity(ComplexityInfo complexity) {
        this.complexities.add(complexity);
    }

    public CKMetrics getCkMetrics() { return ckMetrics; }
    public void setCkMetrics(CKMetrics ckMetrics) { this.ckMetrics = ckMetrics; }
}

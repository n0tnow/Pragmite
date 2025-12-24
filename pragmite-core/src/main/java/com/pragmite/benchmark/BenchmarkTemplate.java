package com.pragmite.benchmark;

import com.pragmite.model.ComplexityInfo;

/**
 * Represents a generated JMH benchmark template.
 * Contains metadata about the original method and the generated benchmark code.
 */
public class BenchmarkTemplate {
    private String originalClass;
    private String originalMethod;
    private String originalPackage;
    private String benchmarkClassName;
    private String benchmarkCode;
    private ComplexityInfo complexityInfo;

    public BenchmarkTemplate() {}

    public String getOriginalClass() {
        return originalClass;
    }

    public void setOriginalClass(String originalClass) {
        this.originalClass = originalClass;
    }

    public String getOriginalMethod() {
        return originalMethod;
    }

    public void setOriginalMethod(String originalMethod) {
        this.originalMethod = originalMethod;
    }

    public String getOriginalPackage() {
        return originalPackage;
    }

    public void setOriginalPackage(String originalPackage) {
        this.originalPackage = originalPackage;
    }

    public String getBenchmarkClassName() {
        return benchmarkClassName;
    }

    public void setBenchmarkClassName(String benchmarkClassName) {
        this.benchmarkClassName = benchmarkClassName;
    }

    public String getBenchmarkCode() {
        return benchmarkCode;
    }

    public void setBenchmarkCode(String benchmarkCode) {
        this.benchmarkCode = benchmarkCode;
    }

    public ComplexityInfo getComplexityInfo() {
        return complexityInfo;
    }

    public void setComplexityInfo(ComplexityInfo complexityInfo) {
        this.complexityInfo = complexityInfo;
    }

    @Override
    public String toString() {
        return String.format("BenchmarkTemplate[%s.%s -> %s]",
            originalClass, originalMethod, benchmarkClassName);
    }
}

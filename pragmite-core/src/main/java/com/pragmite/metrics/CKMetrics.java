package com.pragmite.metrics;

/**
 * Container for CK (Chidamber & Kemerer) metrics for a single class.
 *
 * Metrics interpretation:
 * - WMC: Higher = more complex (typical range: 1-50, threshold: >30)
 * - DIT: Higher = deeper inheritance (typical range: 0-5, threshold: >5)
 * - NOC: Higher = more children (typical range: 0-10, threshold: >10)
 * - CBO: Higher = more coupling (typical range: 0-10, threshold: >10)
 * - RFC: Higher = more complexity (typical range: 1-50, threshold: >30)
 * - LCOM: Higher = less cohesive (typical range: 0-100, threshold: >50)
 */
public class CKMetrics {

    private final String className;
    private final String filePath;
    private final int lineNumber;

    private int wmc;  // Weighted Methods per Class
    private int dit;  // Depth of Inheritance Tree
    private int noc;  // Number of Children
    private int cbo;  // Coupling Between Objects
    private int rfc;  // Response For a Class
    private int lcom; // Lack of Cohesion in Methods

    public CKMetrics(String className, String filePath, int lineNumber) {
        this.className = className;
        this.filePath = filePath;
        this.lineNumber = lineNumber;
    }

    // Getters and Setters
    public String getClassName() {
        return className;
    }

    public String getFilePath() {
        return filePath;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public int getWmc() {
        return wmc;
    }

    public void setWmc(int wmc) {
        this.wmc = wmc;
    }

    public int getDit() {
        return dit;
    }

    public void setDit(int dit) {
        this.dit = dit;
    }

    public int getNoc() {
        return noc;
    }

    public void setNoc(int noc) {
        this.noc = noc;
    }

    public int getCbo() {
        return cbo;
    }

    public void setCbo(int cbo) {
        this.cbo = cbo;
    }

    public int getRfc() {
        return rfc;
    }

    public void setRfc(int rfc) {
        this.rfc = rfc;
    }

    public int getLcom() {
        return lcom;
    }

    public void setLcom(int lcom) {
        this.lcom = lcom;
    }

    /**
     * Returns true if this class is a "God Class" based on CK metrics.
     * Criteria: High WMC (>30) AND High LCOM (>50) AND High CBO (>10)
     */
    public boolean isGodClass() {
        return wmc > 30 && lcom > 50 && cbo > 10;
    }

    /**
     * Returns true if this class has high coupling (Feature Envy risk).
     * Criteria: High CBO (>10)
     */
    public boolean isHighlyCoupled() {
        return cbo > 10;
    }

    /**
     * Returns true if this class has low cohesion.
     * Criteria: High LCOM (>50)
     */
    public boolean hasLowCohesion() {
        return lcom > 50;
    }

    /**
     * Returns true if this class is too deep in inheritance.
     * Criteria: DIT > 5
     */
    public boolean isTooDeepInheritance() {
        return dit > 5;
    }

    /**
     * Returns true if this class has too many children.
     * Criteria: NOC > 10
     */
    public boolean hasTooManyChildren() {
        return noc > 10;
    }

    /**
     * Calculates an overall quality score (0-100, higher is better).
     * Based on normalized inverse of metric violations.
     */
    public int getQualityScore() {
        int violations = 0;

        // Each threshold violation reduces score
        if (wmc > 30) violations++;
        if (dit > 5) violations++;
        if (noc > 10) violations++;
        if (cbo > 10) violations++;
        if (rfc > 30) violations++;
        if (lcom > 50) violations++;

        // Quality score: 100 - (violations * 15)
        return Math.max(0, 100 - (violations * 15));
    }

    @Override
    public String toString() {
        return String.format(
            "CK Metrics for %s (line %d):\n" +
            "  WMC (Weighted Methods): %d\n" +
            "  DIT (Inheritance Depth): %d\n" +
            "  NOC (Number of Children): %d\n" +
            "  CBO (Coupling): %d\n" +
            "  RFC (Response): %d\n" +
            "  LCOM (Lack of Cohesion): %d\n" +
            "  Quality Score: %d/100",
            className, lineNumber, wmc, dit, noc, cbo, rfc, lcom, getQualityScore()
        );
    }

    /**
     * Returns a compact one-line representation.
     */
    public String toCompactString() {
        return String.format(
            "%s: WMC=%d DIT=%d NOC=%d CBO=%d RFC=%d LCOM=%d (Score: %d)",
            className, wmc, dit, noc, cbo, rfc, lcom, getQualityScore()
        );
    }
}

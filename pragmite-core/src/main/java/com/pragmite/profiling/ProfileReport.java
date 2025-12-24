package com.pragmite.profiling;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Performance profile report from JFR analysis.
 * Contains CPU hotspots, memory allocation sites, and performance metrics.
 */
public class ProfileReport {

    private List<Map.Entry<String, Long>> topCpuMethods = new ArrayList<>();
    private List<Map.Entry<String, Long>> topAllocationSites = new ArrayList<>();
    private List<Double> cpuSamples = new ArrayList<>();

    private long totalCpuSamples;
    private long totalAllocations;

    public void setTopCpuMethods(List<Map.Entry<String, Long>> methods) {
        this.topCpuMethods = methods;
    }

    public void setTopAllocationSites(List<Map.Entry<String, Long>> sites) {
        this.topAllocationSites = sites;
    }

    public void addCpuSample(double load) {
        cpuSamples.add(load);
    }

    public void setTotalCpuSamples(long total) {
        this.totalCpuSamples = total;
    }

    public void setTotalAllocations(long total) {
        this.totalAllocations = total;
    }

    public List<Map.Entry<String, Long>> getTopCpuMethods() {
        return topCpuMethods;
    }

    public List<Map.Entry<String, Long>> getTopAllocationSites() {
        return topAllocationSites;
    }

    public long getTotalCpuSamples() {
        return totalCpuSamples;
    }

    public long getTotalAllocations() {
        return totalAllocations;
    }

    public double getAverageCpuLoad() {
        return cpuSamples.isEmpty() ? 0.0 :
            cpuSamples.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
    }

    public double getMaxCpuLoad() {
        return cpuSamples.stream().mapToDouble(Double::doubleValue).max().orElse(0.0);
    }

    /**
     * Formats the report as a human-readable string.
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n=== Performance Profile Report ===\n\n");

        sb.append("CPU Metrics:\n");
        sb.append(String.format("  Total samples: %d\n", totalCpuSamples));
        sb.append(String.format("  Average CPU load: %.2f%%\n", getAverageCpuLoad() * 100));
        sb.append(String.format("  Peak CPU load: %.2f%%\n\n", getMaxCpuLoad() * 100));

        sb.append("Top 10 CPU Hotspots:\n");
        for (int i = 0; i < topCpuMethods.size(); i++) {
            Map.Entry<String, Long> entry = topCpuMethods.get(i);
            double percentage = (entry.getValue() * 100.0) / totalCpuSamples;
            sb.append(String.format("  %2d. %-50s %6d samples (%.2f%%)\n",
                i + 1, entry.getKey(), entry.getValue(), percentage));
        }

        sb.append("\nMemory Metrics:\n");
        sb.append(String.format("  Total allocations: %,d bytes (%.2f MB)\n\n",
            totalAllocations, totalAllocations / (1024.0 * 1024.0)));

        sb.append("Top 10 Allocation Sites:\n");
        for (int i = 0; i < topAllocationSites.size(); i++) {
            Map.Entry<String, Long> entry = topAllocationSites.get(i);
            double percentage = (entry.getValue() * 100.0) / totalAllocations;
            double mb = entry.getValue() / (1024.0 * 1024.0);
            sb.append(String.format("  %2d. %-50s %.2f MB (%.2f%%)\n",
                i + 1, entry.getKey(), mb, percentage));
        }

        sb.append("\n=================================\n");
        return sb.toString();
    }
}

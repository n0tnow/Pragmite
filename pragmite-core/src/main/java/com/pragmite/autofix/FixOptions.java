package com.pragmite.autofix;

import com.pragmite.model.CodeSmellType;

import java.util.HashSet;
import java.util.Set;

/**
 * Configuration options for auto-fix operations.
 */
public class FixOptions {

    private boolean createBackup = true;
    private boolean dryRun = false;
    private boolean stopOnError = false;
    private FixMode mode = FixMode.SAFE;
    private Set<CodeSmellType> allowedTypes = new HashSet<>();
    private String filePatternFilter;

    public enum FixMode {
        SAFE,       // Only apply safe, well-tested fixes
        AGGRESSIVE, // Apply all available fixes
        DRY_RUN     // Show what would be fixed without applying
    }

    public FixOptions() {
    }

    public static FixOptions createDefault() {
        FixOptions options = new FixOptions();
        options.setCreateBackup(true);
        options.setMode(FixMode.SAFE);
        return options;
    }

    public static FixOptions createDryRun() {
        FixOptions options = new FixOptions();
        options.setDryRun(true);
        options.setCreateBackup(false);
        return options;
    }

    // Getters and setters
    public boolean isCreateBackup() {
        return createBackup;
    }

    public void setCreateBackup(boolean createBackup) {
        this.createBackup = createBackup;
    }

    public boolean isDryRun() {
        return dryRun;
    }

    public void setDryRun(boolean dryRun) {
        this.dryRun = dryRun;
        if (dryRun) {
            this.mode = FixMode.DRY_RUN;
        }
    }

    public boolean isStopOnError() {
        return stopOnError;
    }

    public void setStopOnError(boolean stopOnError) {
        this.stopOnError = stopOnError;
    }

    public FixMode getMode() {
        return mode;
    }

    public void setMode(FixMode mode) {
        this.mode = mode;
    }

    public Set<CodeSmellType> getAllowedTypes() {
        return allowedTypes;
    }

    public void setAllowedTypes(Set<CodeSmellType> allowedTypes) {
        this.allowedTypes = allowedTypes;
    }

    public void addAllowedType(CodeSmellType type) {
        this.allowedTypes.add(type);
    }

    public boolean isTypeAllowed(CodeSmellType type) {
        return allowedTypes.isEmpty() || allowedTypes.contains(type);
    }

    public String getFilePatternFilter() {
        return filePatternFilter;
    }

    public void setFilePatternFilter(String filePatternFilter) {
        this.filePatternFilter = filePatternFilter;
    }

    @Override
    public String toString() {
        return "FixOptions{" +
                "createBackup=" + createBackup +
                ", dryRun=" + dryRun +
                ", mode=" + mode +
                ", allowedTypes=" + allowedTypes.size() +
                ", fileFilter='" + filePatternFilter + '\'' +
                '}';
    }
}

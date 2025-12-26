package com.pragmite.interactive;

import com.github.difflib.DiffUtils;
import com.github.difflib.patch.AbstractDelta;
import com.github.difflib.patch.Patch;
import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.AnsiConsole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;

/**
 * Interactive Approval Manager for Pragmite v1.6.0
 * Manages user confirmation for refactoring changes with terminal diff preview
 */
public class InteractiveApprovalManager {

    private static final Logger logger = LoggerFactory.getLogger(InteractiveApprovalManager.class);
    private final BufferedReader reader;
    private boolean applyAll = false;
    private boolean skipAll = false;

    public InteractiveApprovalManager() {
        this.reader = new BufferedReader(new InputStreamReader(System.in));
        AnsiConsole.systemInstall();
    }

    /**
     * Decision result for a refactoring approval
     */
    public enum Decision {
        APPLY,      // Apply this change
        SKIP,       // Skip this change
        APPLY_ALL,  // Apply all remaining changes
        SKIP_ALL,   // Skip all remaining changes
        QUIT        // Quit and exit
    }

    /**
     * Ask user for approval with diff preview
     */
    public Decision askForApproval(
        String fileName,
        String refactoringType,
        String beforeCode,
        String afterCode,
        int currentIndex,
        int totalCount
    ) {
        // Check if user already made a global decision
        if (applyAll) {
            return Decision.APPLY;
        }
        if (skipAll) {
            return Decision.SKIP;
        }

        // Clear screen and show header
        printHeader(fileName, refactoringType, currentIndex, totalCount);

        // Show diff
        printDiff(beforeCode, afterCode);

        // Show prompt and get decision
        return getDecision();
    }

    private void printHeader(String fileName, String refactoringType, int current, int total) {
        System.out.println();
        System.out.println(Ansi.ansi()
            .bold()
            .fg(Ansi.Color.CYAN)
            .a("━".repeat(80))
            .reset());

        System.out.println(Ansi.ansi()
            .bold()
            .fg(Ansi.Color.YELLOW)
            .a(String.format("[%d/%d] %s: %s", current, total, refactoringType, fileName))
            .reset());

        System.out.println(Ansi.ansi()
            .bold()
            .fg(Ansi.Color.CYAN)
            .a("━".repeat(80))
            .reset());
        System.out.println();
    }

    private void printDiff(String beforeCode, String afterCode) {
        List<String> beforeLines = Arrays.asList(beforeCode.split("\n"));
        List<String> afterLines = Arrays.asList(afterCode.split("\n"));

        try {
            // Generate diff using java-diff-utils
            Patch<String> patch = DiffUtils.diff(beforeLines, afterLines);
            List<AbstractDelta<String>> deltas = patch.getDeltas();

            if (deltas.isEmpty()) {
                System.out.println(Ansi.ansi()
                    .fg(Ansi.Color.YELLOW)
                    .a("  (No visible changes)")
                    .reset());
                return;
            }

            // Print header
            System.out.println(Ansi.ansi()
                .bold()
                .a("  BEFORE")
                .a(" ".repeat(35))
                .a("AFTER")
                .reset());
            System.out.println(Ansi.ansi()
                .fg(Ansi.Color.CYAN)
                .a("  " + "─".repeat(76))
                .reset());

            // Track line numbers
            int beforeLineNum = 1;
            int afterLineNum = 1;
            int lastBeforeLine = 0;

            for (AbstractDelta<String> delta : deltas) {
                int deltaStart = delta.getSource().getPosition();

                // Print unchanged lines before this delta (context)
                while (beforeLineNum <= deltaStart && beforeLineNum <= beforeLines.size()) {
                    if (beforeLineNum > lastBeforeLine) {
                        String line = beforeLines.get(beforeLineNum - 1);
                        printUnchangedLine(beforeLineNum, line, afterLineNum, line);
                        afterLineNum++;
                    }
                    beforeLineNum++;
                }

                // Print delta changes
                switch (delta.getType()) {
                    case DELETE:
                        for (String line : delta.getSource().getLines()) {
                            printDeletedLine(beforeLineNum++, line);
                        }
                        break;

                    case INSERT:
                        for (String line : delta.getTarget().getLines()) {
                            printInsertedLine(afterLineNum++, line);
                        }
                        break;

                    case CHANGE:
                        // Print deleted lines
                        for (String line : delta.getSource().getLines()) {
                            printDeletedLine(beforeLineNum++, line);
                        }
                        // Print inserted lines
                        for (String line : delta.getTarget().getLines()) {
                            printInsertedLine(afterLineNum++, line);
                        }
                        break;
                }

                lastBeforeLine = beforeLineNum - 1;
            }

            // Print remaining unchanged lines (up to 3 for context)
            int contextLines = 0;
            while (beforeLineNum <= beforeLines.size() && contextLines < 3) {
                String line = beforeLines.get(beforeLineNum - 1);
                printUnchangedLine(beforeLineNum, line, afterLineNum, line);
                beforeLineNum++;
                afterLineNum++;
                contextLines++;
            }

            if (beforeLineNum <= beforeLines.size()) {
                System.out.println(Ansi.ansi()
                    .fg(Ansi.Color.CYAN)
                    .a("  ... (" + (beforeLines.size() - beforeLineNum + 1) + " more lines)")
                    .reset());
            }

        } catch (Exception e) {
            logger.error("Error generating diff", e);
            System.out.println(Ansi.ansi()
                .fg(Ansi.Color.RED)
                .a("  Error displaying diff: " + e.getMessage())
                .reset());
        }

        System.out.println();
    }

    private void printUnchangedLine(int beforeNum, String beforeLine, int afterNum, String afterLine) {
        String truncated = truncateLine(beforeLine, 35);
        System.out.print(Ansi.ansi()
            .fg(Ansi.Color.WHITE)
            .a(String.format("  %3d  %-35s", beforeNum, truncated))
            .reset());

        truncated = truncateLine(afterLine, 35);
        System.out.println(Ansi.ansi()
            .fg(Ansi.Color.WHITE)
            .a(String.format("  %3d  %s", afterNum, truncated))
            .reset());
    }

    private void printDeletedLine(int lineNum, String line) {
        String truncated = truncateLine(line, 35);
        System.out.println(Ansi.ansi()
            .fg(Ansi.Color.RED)
            .a(String.format("  %3d- %-74s", lineNum, truncated))
            .reset());
    }

    private void printInsertedLine(int lineNum, String line) {
        String truncated = truncateLine(line, 70);
        System.out.println(Ansi.ansi()
            .fg(Ansi.Color.GREEN)
            .a(String.format("       %3d+ %s", lineNum, truncated))
            .reset());
    }

    private String truncateLine(String line, int maxLength) {
        if (line.length() <= maxLength) {
            return line;
        }
        return line.substring(0, maxLength - 3) + "...";
    }

    private Decision getDecision() {
        System.out.println(Ansi.ansi()
            .fg(Ansi.Color.CYAN)
            .a("  " + "─".repeat(76))
            .reset());
        System.out.println();
        System.out.println("  Options:");
        System.out.println("    " + Ansi.ansi().bold().a("y").reset() + " - Apply this change");
        System.out.println("    " + Ansi.ansi().bold().a("n").reset() + " - Skip this change");
        System.out.println("    " + Ansi.ansi().bold().a("a").reset() + " - Apply ALL remaining changes");
        System.out.println("    " + Ansi.ansi().bold().a("s").reset() + " - Skip ALL remaining changes");
        System.out.println("    " + Ansi.ansi().bold().a("q").reset() + " - Quit (save progress)");
        System.out.println();
        System.out.print("  Your choice [y/n/a/s/q]: ");

        try {
            String input = reader.readLine().trim().toLowerCase();

            switch (input) {
                case "y":
                case "yes":
                    return Decision.APPLY;

                case "n":
                case "no":
                    return Decision.SKIP;

                case "a":
                case "all":
                    applyAll = true;
                    return Decision.APPLY_ALL;

                case "s":
                case "skip":
                    skipAll = true;
                    return Decision.SKIP_ALL;

                case "q":
                case "quit":
                    return Decision.QUIT;

                default:
                    System.out.println(Ansi.ansi()
                        .fg(Ansi.Color.RED)
                        .a("  Invalid choice. Please enter y, n, a, s, or q.")
                        .reset());
                    return getDecision(); // Ask again
            }

        } catch (IOException e) {
            logger.error("Error reading user input", e);
            return Decision.SKIP;
        }
    }

    /**
     * Print session summary
     */
    public void printSummary(int applied, int skipped, int total) {
        System.out.println();
        System.out.println(Ansi.ansi()
            .bold()
            .fg(Ansi.Color.CYAN)
            .a("━".repeat(80))
            .reset());
        System.out.println(Ansi.ansi()
            .bold()
            .fg(Ansi.Color.YELLOW)
            .a("  INTERACTIVE SESSION SUMMARY")
            .reset());
        System.out.println(Ansi.ansi()
            .bold()
            .fg(Ansi.Color.CYAN)
            .a("━".repeat(80))
            .reset());
        System.out.println();

        System.out.println(String.format("  Total refactorings: %d", total));
        System.out.println(Ansi.ansi()
            .fg(Ansi.Color.GREEN)
            .a(String.format("  Applied: %d (%.1f%%)", applied, (applied * 100.0 / total)))
            .reset());
        System.out.println(Ansi.ansi()
            .fg(Ansi.Color.YELLOW)
            .a(String.format("  Skipped: %d (%.1f%%)", skipped, (skipped * 100.0 / total)))
            .reset());

        System.out.println();
        System.out.println(Ansi.ansi()
            .bold()
            .fg(Ansi.Color.CYAN)
            .a("━".repeat(80))
            .reset());
        System.out.println();
    }

    /**
     * Cleanup resources
     */
    public void close() {
        try {
            reader.close();
            AnsiConsole.systemUninstall();
        } catch (IOException e) {
            logger.error("Error closing interactive manager", e);
        }
    }
}

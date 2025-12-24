package com.pragmite.refactor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Verifies that refactored code still builds and passes tests.
 * Executes Maven/Gradle build and test commands.
 */
public class BuildTestVerifier {
    private static final Logger logger = LoggerFactory.getLogger(BuildTestVerifier.class);

    private static final int DEFAULT_TIMEOUT_SECONDS = 300; // 5 minutes
    private static final int MAX_TIMEOUT_SECONDS = 3600; // 1 hour max

    private final Path projectRoot;
    private BuildTool buildTool;
    private int timeoutSeconds = DEFAULT_TIMEOUT_SECONDS;

    public BuildTestVerifier() {
        this(Paths.get(System.getProperty("user.dir")));
    }

    public BuildTestVerifier(Path projectRoot) {
        this.projectRoot = projectRoot;
        this.buildTool = detectBuildTool();
    }

    /**
     * Detects the build tool used by the project.
     */
    private BuildTool detectBuildTool() {
        if (Files.exists(projectRoot.resolve("pom.xml"))) {
            logger.info("Detected Maven project");
            return BuildTool.MAVEN;
        } else if (Files.exists(projectRoot.resolve("build.gradle")) ||
                   Files.exists(projectRoot.resolve("build.gradle.kts"))) {
            logger.info("Detected Gradle project");
            return BuildTool.GRADLE;
        } else {
            logger.warn("No build tool detected, using Maven as default");
            return BuildTool.MAVEN;
        }
    }

    /**
     * Runs the build command to verify code compiles.
     */
    public VerificationResult runBuild() throws IOException, InterruptedException {
        logger.info("Running build verification...");

        String[] command = buildTool == BuildTool.MAVEN
            ? new String[]{"mvn", "clean", "compile", "-q"}
            : new String[]{"gradle", "clean", "compileJava", "-q"};

        return executeCommand("Build", command);
    }

    /**
     * Runs tests to verify functionality is preserved.
     */
    public VerificationResult runTests() throws IOException, InterruptedException {
        logger.info("Running test verification...");

        String[] command = buildTool == BuildTool.MAVEN
            ? new String[]{"mvn", "test", "-q"}
            : new String[]{"gradle", "test", "-q"};

        return executeCommand("Test", command);
    }

    /**
     * Runs both build and tests.
     */
    public VerificationResult runBuildAndTest() throws IOException, InterruptedException {
        logger.info("Running full build and test verification...");

        // First, try build
        VerificationResult buildResult = runBuild();
        if (!buildResult.isSuccess()) {
            return buildResult; // Stop if build fails
        }

        // Then run tests
        VerificationResult testResult = runTests();

        // Combine results
        VerificationResult combined = new VerificationResult();
        combined.setPhase("Build & Test");
        combined.setSuccess(buildResult.isSuccess() && testResult.isSuccess());
        combined.setExitCode(testResult.getExitCode());

        List<String> allOutput = new ArrayList<>();
        allOutput.add("=== Build Output ===");
        allOutput.addAll(buildResult.getOutput());
        allOutput.add("\n=== Test Output ===");
        allOutput.addAll(testResult.getOutput());
        combined.setOutput(allOutput);

        return combined;
    }

    /**
     * Executes a build/test command and captures output.
     */
    private VerificationResult executeCommand(String phase, String[] command) throws IOException, InterruptedException {
        VerificationResult result = new VerificationResult();
        result.setPhase(phase);

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(projectRoot.toFile());
        pb.redirectErrorStream(true); // Merge stderr into stdout

        logger.debug("Executing: {} (timeout: {}s)", String.join(" ", command), timeoutSeconds);

        long startTime = System.currentTimeMillis();
        Process process = pb.start();

        // Capture output in a separate thread to avoid blocking
        List<String> output = new ArrayList<>();
        Thread outputReader = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.add(line);
                    logger.debug(line);
                }
            } catch (IOException e) {
                logger.warn("Error reading process output: {}", e.getMessage());
            }
        });
        outputReader.setDaemon(true);
        outputReader.start();

        // Wait for completion with timeout
        boolean completed = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);

        if (!completed) {
            // Timeout occurred - try graceful shutdown first
            logger.warn("{} verification timed out after {}s, attempting graceful shutdown", phase, timeoutSeconds);
            process.destroy();

            // Give it 5 more seconds for graceful shutdown
            boolean gracefulShutdown = process.waitFor(5, TimeUnit.SECONDS);

            if (!gracefulShutdown) {
                logger.error("{} verification force killed after timeout", phase);
                process.destroyForcibly();
                // Wait a bit more for forceful kill
                process.waitFor(2, TimeUnit.SECONDS);
            }

            long elapsedTime = (System.currentTimeMillis() - startTime) / 1000;
            result.setSuccess(false);
            result.setExitCode(-1);

            List<String> timeoutOutput = new ArrayList<>(output);
            timeoutOutput.add("");
            timeoutOutput.add("ERROR: " + phase + " verification timed out after " + timeoutSeconds + " seconds");
            timeoutOutput.add("Actual elapsed time: " + elapsedTime + " seconds");
            timeoutOutput.add("The process was " + (gracefulShutdown ? "gracefully terminated" : "force killed"));
            result.setOutput(timeoutOutput);

            logger.error("{} verification timed out ({}s configured, {}s elapsed)",
                        phase, timeoutSeconds, elapsedTime);
            return result;
        }

        // Process completed normally
        // Wait for output reader thread to finish (with timeout)
        outputReader.join(2000);

        long elapsedTime = (System.currentTimeMillis() - startTime) / 1000;
        int exitCode = process.exitValue();
        result.setExitCode(exitCode);
        result.setSuccess(exitCode == 0);
        result.setOutput(output);

        if (exitCode == 0) {
            logger.info("{} verification PASSED ({}s)", phase, elapsedTime);
        } else {
            logger.error("{} verification FAILED (exit code: {}, {}s)", phase, exitCode, elapsedTime);
        }

        return result;
    }

    /**
     * Sets the timeout for build/test commands.
     * @param timeoutSeconds Timeout in seconds (min: 10, max: 3600)
     * @throws IllegalArgumentException if timeout is out of valid range
     */
    public void setTimeoutSeconds(int timeoutSeconds) {
        if (timeoutSeconds < 10) {
            throw new IllegalArgumentException("Timeout must be at least 10 seconds, got: " + timeoutSeconds);
        }
        if (timeoutSeconds > MAX_TIMEOUT_SECONDS) {
            throw new IllegalArgumentException("Timeout cannot exceed " + MAX_TIMEOUT_SECONDS +
                                             " seconds, got: " + timeoutSeconds);
        }
        this.timeoutSeconds = timeoutSeconds;
        logger.debug("Build/test timeout set to {}s", timeoutSeconds);
    }

    /**
     * Gets the current timeout value.
     */
    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    /**
     * Sets the build tool to use.
     */
    public void setBuildTool(BuildTool buildTool) {
        this.buildTool = buildTool;
    }

    /**
     * Supported build tools.
     */
    public enum BuildTool {
        MAVEN,
        GRADLE
    }
}

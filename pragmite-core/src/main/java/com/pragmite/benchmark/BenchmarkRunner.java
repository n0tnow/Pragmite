package com.pragmite.benchmark;

import org.openjdk.jmh.results.RunResult;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

/**
 * Runs JMH benchmarks and collects performance results.
 * Provides a simplified interface to execute benchmarks programmatically.
 *
 * Usage:
 * <pre>
 * BenchmarkRunner runner = new BenchmarkRunner();
 * BenchmarkResult result = runner.runBenchmark(".*MyClass_myMethod.*");
 * </pre>
 */
public class BenchmarkRunner {
    private static final Logger logger = LoggerFactory.getLogger(BenchmarkRunner.class);

    private int warmupIterations = 3;
    private int measurementIterations = 5;
    private int forks = 2;
    private TimeValue warmupTime = TimeValue.seconds(1);
    private TimeValue measurementTime = TimeValue.seconds(1);

    public BenchmarkRunner() {}

    /**
     * Runs benchmarks matching the given pattern.
     * @param benchmarkPattern Regex pattern to match benchmark methods (e.g., ".*MyBenchmark.*")
     * @return Benchmark results
     */
    public BenchmarkResult runBenchmark(String benchmarkPattern) throws RunnerException {
        logger.info("Running JMH benchmarks matching: {}", benchmarkPattern);

        Options opt = new OptionsBuilder()
            .include(benchmarkPattern)
            .warmupIterations(warmupIterations)
            .warmupTime(warmupTime)
            .measurementIterations(measurementIterations)
            .measurementTime(measurementTime)
            .forks(forks)
            .shouldFailOnError(true)
            .shouldDoGC(true)
            .build();

        Runner runner = new Runner(opt);
        Collection<RunResult> results = runner.run();

        logger.info("Benchmark completed. {} results collected", results.size());

        return parseBenchmarkResults(results);
    }

    /**
     * Runs all available benchmarks.
     */
    public BenchmarkResult runAllBenchmarks() throws RunnerException {
        return runBenchmark(".*");
    }

    /**
     * Runs benchmarks for a specific class.
     */
    public BenchmarkResult runBenchmarksForClass(String className) throws RunnerException {
        return runBenchmark(".*" + className + ".*");
    }

    /**
     * Parses JMH RunResults into our custom BenchmarkResult format.
     */
    private BenchmarkResult parseBenchmarkResults(Collection<RunResult> jmhResults) {
        BenchmarkResult result = new BenchmarkResult();

        for (RunResult runResult : jmhResults) {
            String benchmarkName = runResult.getParams().getBenchmark();
            double score = runResult.getPrimaryResult().getScore();
            double scoreError = runResult.getPrimaryResult().getScoreError();
            String unit = runResult.getPrimaryResult().getScoreUnit();

            BenchmarkResult.MethodResult methodResult = new BenchmarkResult.MethodResult(
                benchmarkName, score, scoreError, unit
            );

            result.addMethodResult(methodResult);

            logger.debug("Benchmark: {} - Score: {} ± {} {}",
                benchmarkName, score, scoreError, unit);
        }

        return result;
    }

    // Configuration setters

    public BenchmarkRunner withWarmupIterations(int iterations) {
        this.warmupIterations = iterations;
        return this;
    }

    public BenchmarkRunner withMeasurementIterations(int iterations) {
        this.measurementIterations = iterations;
        return this;
    }

    public BenchmarkRunner withForks(int forks) {
        this.forks = forks;
        return this;
    }

    public BenchmarkRunner withWarmupTime(int seconds) {
        this.warmupTime = TimeValue.seconds(seconds);
        return this;
    }

    public BenchmarkRunner withMeasurementTime(int seconds) {
        this.measurementTime = TimeValue.seconds(seconds);
        return this;
    }
}

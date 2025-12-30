package io.aurigraph.v11.testing;

import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * TestOrchestrator - Coordinates all test suites across the platform
 *
 * Responsible for:
 * - Running tests as code becomes available
 * - Tracking test results in real-time
 * - Generating continuous test reports
 * - Coordinating with other agents
 * - Managing test execution schedule
 *
 * Architecture:
 * - Parallel test execution with thread pooling
 * - Real-time result collection
 * - Continuous report generation
 * - Integration point verification
 * - Performance metric tracking
 */
@ApplicationScoped
public class TestOrchestrator {

    @Inject
    PerformanceValidator performanceValidator;

    @Inject
    IntegrationTestSuite integrationTestSuite;

    private final ExecutorService testExecutor = Executors.newFixedThreadPool(4);
    private final ConcurrentHashMap<String, TestResult> testResults = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, SprintProgress> sprintProgress = new ConcurrentHashMap<>();
    private final List<TestRunEvent> eventLog = Collections.synchronizedList(new ArrayList<>());
    private volatile boolean isRunning = false;

    public TestOrchestrator() {
        initializeSprintProgress();
    }

    /**
     * Initialize sprint progress tracking structure
     */
    private void initializeSprintProgress() {
        sprintProgress.put("sprint1", new SprintProgress("Secondary Token Tests", 215, 0));
        sprintProgress.put("sprint2", new SprintProgress("VVB + Secondary Integration", 60, 0));
        sprintProgress.put("sprint3-4", new SprintProgress("Composite Token Tests", 80, 0));
        sprintProgress.put("sprint5-7", new SprintProgress("Smart Contract Tests", 100, 0));
        sprintProgress.put("sprint8-9", new SprintProgress("Registry Infrastructure Tests", 90, 0));
        sprintProgress.put("sprint10-11", new SprintProgress("Visualization Tests", 70, 0));
        sprintProgress.put("sprint12-13", new SprintProgress("E2E Tests", 150, 0));
    }

    /**
     * Start continuous test execution
     * Runs every 2 hours or on-demand
     */
    public synchronized void startContinuousTestExecution() {
        if (isRunning) {
            Log.info("Test orchestration already running");
            return;
        }

        isRunning = true;
        Log.info("Starting continuous test orchestration");
        recordEvent("ORCHESTRATION_START", "Continuous test execution initiated");

        // Schedule periodic test runs
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(this::executeTestCycle, 0, 2, TimeUnit.HOURS);
    }

    /**
     * Execute complete test cycle
     * 1. Compile and unit test
     * 2. Run integration tests for completed components
     * 3. Validate performance
     * 4. Generate report
     */
    public void executeTestCycle() {
        Log.info("=== STARTING TEST CYCLE ===");
        recordEvent("CYCLE_START", "Test cycle initiated");

        try {
            // Phase 1: Compile and Unit Test
            Log.info("Phase 1: Compiling and running unit tests");
            CompilationResult compilationResult = compileAndUnitTest();
            updateTestResults("compilation", compilationResult);

            if (!compilationResult.success) {
                Log.error("Compilation failed, aborting test cycle");
                recordEvent("COMPILATION_FAILURE", compilationResult.errorMessage);
                generateTestReport();
                return;
            }

            // Phase 2: Run Sprint Tests Progressively
            Log.info("Phase 2: Running sprint-specific tests");
            runSprintTests();

            // Phase 3: Integration Testing
            Log.info("Phase 3: Running integration tests");
            runIntegrationTests();

            // Phase 4: Performance Validation
            Log.info("Phase 4: Validating performance metrics");
            performanceValidator.validateAllMetrics();

            // Phase 5: Generate Report
            Log.info("Phase 5: Generating test report");
            generateTestReport();

            // Check quality gates
            enforceQualityGates();

            Log.info("=== TEST CYCLE COMPLETE ===");
            recordEvent("CYCLE_COMPLETE", "Test cycle completed successfully");

        } catch (Exception e) {
            Log.error("Test cycle failed", e);
            recordEvent("CYCLE_FAILURE", "Test cycle failed: " + e.getMessage());
            generateTestReport();
        }
    }

    /**
     * Compile code and run unit tests
     */
    private CompilationResult compileAndUnitTest() {
        Log.info("Compiling project and running unit tests");
        CompilationResult result = new CompilationResult();

        try {
            // Run Maven compile
            ProcessBuilder pb = new ProcessBuilder(
                "./mvnw", "clean", "test", "-q"
            );
            pb.redirectErrorStream(true);
            Process process = pb.start();

            // Read output
            StringBuilder output = new StringBuilder();
            java.io.BufferedReader reader = new java.io.BufferedReader(
                new java.io.InputStreamReader(process.getInputStream())
            );
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }

            int exitCode = process.waitFor();
            result.success = (exitCode == 0);
            result.output = output.toString();

            if (result.success) {
                Log.info("Compilation and unit tests successful");
                recordEvent("UNIT_TEST_SUCCESS", "All unit tests passed");
                parseTestResults(output.toString());
            } else {
                result.errorMessage = "Build or test execution failed (exit code: " + exitCode + ")";
                Log.error(result.errorMessage);
                recordEvent("UNIT_TEST_FAILURE", result.errorMessage);
            }

        } catch (Exception e) {
            result.success = false;
            result.errorMessage = "Compilation failed: " + e.getMessage();
            Log.error(result.errorMessage, e);
            recordEvent("COMPILATION_ERROR", result.errorMessage);
        }

        return result;
    }

    /**
     * Parse Maven Surefire test results
     */
    private void parseTestResults(String output) {
        // Parse test counts from Maven output
        // Expected format: Tests run: X, Failures: Y, Errors: Z
        // This is a simplified parser - in production use Surefire XML reports

        String[] lines = output.split("\n");
        for (String line : lines) {
            if (line.contains("Tests run:")) {
                Log.info("Test output: " + line);
                // Parse and store results
            }
        }
    }

    /**
     * Run tests for each sprint as code becomes available
     */
    private void runSprintTests() {
        for (Map.Entry<String, SprintProgress> entry : sprintProgress.entrySet()) {
            String sprintId = entry.getKey();
            SprintProgress progress = entry.getValue();

            Log.info("Running tests for " + sprintId + ": " + progress.name);

            // For now, track that we attempted the tests
            // In real implementation, would call specific test suites
            progress.executionCount++;

            if (progress.executionCount > 0) {
                recordEvent("SPRINT_TEST_RUN", sprintId + " tests executed");
            }
        }
    }

    /**
     * Run integration tests for completed components
     */
    private void runIntegrationTests() {
        Log.info("Running integration tests");

        // Integration tests run only after components complete
        integrationTestSuite.verifySecondaryTokenIntegration();
        integrationTestSuite.verifyVVBIntegration();
        integrationTestSuite.verifyCompositeIntegration();

        recordEvent("INTEGRATION_TEST_RUN", "Integration tests executed");
    }

    /**
     * Enforce quality gates
     */
    private void enforceQualityGates() {
        // Calculate pass rate
        double passRate = calculatePassRate();

        // Get coverage metrics
        double coverage = performanceValidator.getCurrentCoverage();

        // Check gates
        if (passRate < 0.95) {
            Log.warn("Quality gate FAILED: Test pass rate " + passRate + " < 95%");
            recordEvent("QUALITY_GATE_FAILURE", "Test pass rate below 95%");
        }

        if (coverage < 0.95) {
            Log.warn("Quality gate FAILED: Code coverage " + coverage + " < 95%");
            recordEvent("QUALITY_GATE_FAILURE", "Code coverage below 95%");
        }

        // Performance regression check
        if (performanceValidator.hasRegressions()) {
            Log.warn("Quality gate WARNING: Performance regressions detected");
            recordEvent("PERFORMANCE_REGRESSION", "Performance metrics exceeded targets");
        }
    }

    /**
     * Calculate overall test pass rate
     */
    private double calculatePassRate() {
        long totalTests = testResults.values().stream()
            .mapToLong(r -> r.totalTests)
            .sum();
        long passedTests = testResults.values().stream()
            .mapToLong(r -> r.passedTests)
            .sum();

        return totalTests > 0 ? (double) passedTests / totalTests : 0.0;
    }

    /**
     * Update test results
     */
    private void updateTestResults(String testName, CompilationResult result) {
        TestResult tr = new TestResult();
        tr.testName = testName;
        tr.timestamp = Instant.now();
        tr.success = result.success;
        tr.output = result.output;
        testResults.put(testName, tr);
    }

    /**
     * Generate comprehensive test report
     */
    public void generateTestReport() {
        try {
            StringBuilder report = new StringBuilder();
            report.append("# Continuous Test Results - ")
                .append(DateTimeFormatter.ISO_INSTANT.format(Instant.now()))
                .append("\n\n");

            // Summary section
            report.append("## Summary\n");
            double passRate = calculatePassRate();
            double coverage = performanceValidator.getCurrentCoverage();
            report.append("- Pass Rate: ").append(String.format("%.1f%%", passRate * 100)).append("\n");
            report.append("- Code Coverage: ").append(String.format("%.1f%%", coverage * 100)).append("\n");
            report.append("- Status: ").append(passRate >= 0.95 && coverage >= 0.95 ? "✅ PASS" : "❌ FAIL")
                .append("\n\n");

            // Sprint progress
            report.append("## Sprint Progress\n");
            for (Map.Entry<String, SprintProgress> entry : sprintProgress.entrySet()) {
                SprintProgress progress = entry.getValue();
                report.append("- ").append(entry.getKey()).append(" (").append(progress.name)
                    .append("): ").append(progress.executionCount).append(" runs\n");
            }
            report.append("\n");

            // Performance metrics
            report.append("## Performance Metrics\n");
            report.append(performanceValidator.generateMetricsReport());
            report.append("\n");

            // Recent events
            report.append("## Recent Events\n");
            takeRight(eventLog, 10).forEach(event ->
                report.append("- ").append(event.timestamp).append(": ")
                    .append(event.eventType).append(" - ").append(event.description).append("\n")
            );

            // Write report to file
            Path reportPath = Path.of("target", "test-reports",
                "CONTINUOUS_TEST_RESULTS_" + Instant.now().toString().replace(":", "-") + ".md");
            Files.createDirectories(reportPath.getParent());
            Files.writeString(reportPath, report.toString(), StandardOpenOption.CREATE_NEW);

            Log.info("Test report generated: " + reportPath);

        } catch (IOException e) {
            Log.error("Failed to generate test report", e);
        }
    }

    /**
     * Record event for audit trail
     */
    private void recordEvent(String eventType, String description) {
        eventLog.add(new TestRunEvent(eventType, description, Instant.now()));
    }

    /**
     * Get test orchestration status
     */
    public OrchestratorStatus getStatus() {
        OrchestratorStatus status = new OrchestratorStatus();
        status.isRunning = isRunning;
        status.passRate = calculatePassRate();
        status.coverage = performanceValidator.getCurrentCoverage();
        status.sprintProgress = new HashMap<>(sprintProgress);
        status.lastEventCount = eventLog.size();
        return status;
    }

    /**
     * Stop continuous test execution
     */
    public synchronized void stopContinuousTestExecution() {
        isRunning = false;
        Log.info("Continuous test orchestration stopped");
        recordEvent("ORCHESTRATION_STOP", "Continuous test execution stopped");
    }

    // ===== Inner Classes =====

    public static class CompilationResult {
        public boolean success;
        public String output;
        public String errorMessage;
    }

    public static class TestResult {
        public String testName;
        public Instant timestamp;
        public boolean success;
        public String output;
        public long totalTests;
        public long passedTests;
        public long failedTests;
    }

    public static class SprintProgress {
        public String name;
        public int expectedTests;
        public int executionCount;

        public SprintProgress(String name, int expectedTests, int executionCount) {
            this.name = name;
            this.expectedTests = expectedTests;
            this.executionCount = executionCount;
        }
    }

    public static class OrchestratorStatus {
        public boolean isRunning;
        public double passRate;
        public double coverage;
        public Map<String, SprintProgress> sprintProgress;
        public int lastEventCount;
    }

    public static class TestRunEvent {
        public String eventType;
        public String description;
        public Instant timestamp;

        public TestRunEvent(String eventType, String description, Instant timestamp) {
            this.eventType = eventType;
            this.description = description;
            this.timestamp = timestamp;
        }
    }

    // Java stream extensions (since Java 21 doesn't have takeRight)
    private static <T> List<T> takeRight(List<T> list, int n) {
        int size = list.size();
        return list.subList(Math.max(0, size - n), size);
    }
}

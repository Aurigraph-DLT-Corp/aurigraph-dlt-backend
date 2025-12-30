package io.aurigraph.v11.testing;

import io.quarkus.logging.Log;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;

/**
 * TestExecutionScheduler - Manages test execution schedule across 18 phases
 *
 * Execution Timeline:
 * Hour 0-2:   Set up test infrastructure + Agent 1 tests
 * Hour 2-4:   Sprint 1 tests + Agent 2 prep
 * Hour 4-6:   Sprint 1+2 tests (275 total) + Agent 3 prep
 * Hour 6-8:   Sprint 1+2+3-4 tests (355 total) + Agent 4+5 prep
 * Hour 8-10:  Sprint 1+2+3-4+5-7 tests (455 total) + Agent 6 prep
 * Hour 10-12: Sprint 1+2+3-4+5-7+8-9 tests (545 total) + Agent 7 prep
 * Hour 12-14: Sprint 1+2+3-4+5-7+8-9+10-11 tests (615 total) + Agent 7 execution
 * Hour 14-16: All Sprint tests (765 total) + E2E tests
 * Hour 16-18: Final validation + performance analysis
 */
@ApplicationScoped
public class TestExecutionScheduler {

    @Inject
    TestOrchestrator testOrchestrator;

    @Inject
    PerformanceValidator performanceValidator;

    @Inject
    IntegrationTestSuite integrationTestSuite;

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    private final Map<String, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();
    private volatile Instant executionStartTime;
    private volatile boolean isSchedulerRunning = false;

    // Test execution phases
    private static final Map<String, TestPhase> EXECUTION_PHASES = new LinkedHashMap<>();

    static {
        EXECUTION_PHASES.put("PHASE_0_2", new TestPhase("Infrastructure + Agent 1", 0, 2, 215));
        EXECUTION_PHASES.put("PHASE_2_4", new TestPhase("Sprint 1 + Agent 2", 2, 4, 215));
        EXECUTION_PHASES.put("PHASE_4_6", new TestPhase("Sprint 1+2 + Agent 3", 4, 6, 275));
        EXECUTION_PHASES.put("PHASE_6_8", new TestPhase("Sprint 1+2+3-4 + Agent 4+5", 6, 8, 355));
        EXECUTION_PHASES.put("PHASE_8_10", new TestPhase("Sprint 1+2+3-4+5-7 + Agent 6", 8, 10, 455));
        EXECUTION_PHASES.put("PHASE_10_12", new TestPhase("Sprint 1+2+3-4+5-7+8-9 + Agent 7", 10, 12, 545));
        EXECUTION_PHASES.put("PHASE_12_14", new TestPhase("Sprint 1+2+3-4+5-7+8-9+10-11", 12, 14, 615));
        EXECUTION_PHASES.put("PHASE_14_16", new TestPhase("All Sprint + E2E", 14, 16, 765));
        EXECUTION_PHASES.put("PHASE_16_18", new TestPhase("Final Validation", 16, 18, 765));
    }

    /**
     * Initialize scheduler on application startup
     */
    public void onStart(@Observes StartupEvent ev) {
        Log.info("Test Execution Scheduler initialized");
    }

    /**
     * Start the test execution schedule
     */
    public synchronized void startScheduledExecution() {
        if (isSchedulerRunning) {
            Log.info("Test scheduler already running");
            return;
        }

        isSchedulerRunning = true;
        executionStartTime = Instant.now();
        Log.info("Starting scheduled test execution");

        // Schedule each phase
        for (Map.Entry<String, TestPhase> entry : EXECUTION_PHASES.entrySet()) {
            String phaseId = entry.getKey();
            TestPhase phase = entry.getValue();

            // Convert hours to milliseconds
            long delayMinutes = phase.startHour * 60;

            ScheduledFuture<?> task = scheduler.schedule(
                () -> executePhase(phaseId, phase),
                delayMinutes,
                TimeUnit.MINUTES
            );

            scheduledTasks.put(phaseId, task);
            Log.info("Scheduled " + phaseId + " for execution in " + phase.startHour + " hours");
        }
    }

    /**
     * Execute a test phase
     */
    private void executePhase(String phaseId, TestPhase phase) {
        Log.info("========== EXECUTING " + phaseId + " ==========");
        Log.info("Phase: " + phase.description);
        Log.info("Expected tests: " + phase.expectedTestCount);

        try {
            long phaseStartTime = System.currentTimeMillis();

            // Run the orchestrator's test cycle
            testOrchestrator.executeTestCycle();

            // Calculate phase duration
            long phaseDuration = System.currentTimeMillis() - phaseStartTime;
            Log.info("Phase " + phaseId + " completed in " + phaseDuration + "ms");

            // Generate phase report
            generatePhaseReport(phaseId, phase, phaseDuration);

            // Check if next phase can start
            scheduleNextPhase(phaseId);

        } catch (Exception e) {
            Log.error("Phase " + phaseId + " failed", e);
        }
    }

    /**
     * Generate report for a test phase
     */
    private void generatePhaseReport(String phaseId, TestPhase phase, long duration) {
        StringBuilder report = new StringBuilder();
        report.append("# Test Execution Report - ").append(phaseId).append("\n\n");
        report.append("## Phase Information\n");
        report.append("- Phase: ").append(phase.description).append("\n");
        report.append("- Duration: ").append(duration).append("ms\n");
        report.append("- Expected Tests: ").append(phase.expectedTestCount).append("\n");
        report.append("- Start Time: ").append(phase.startHour).append(" hours\n");
        report.append("- End Time: ").append(phase.endHour).append(" hours\n\n");

        report.append("## Execution Status\n");
        // Would include actual test results here
        report.append("- Test execution completed\n");
        report.append("- Orchestrator cycle finished\n\n");

        report.append("## Next Actions\n");
        report.append("- Monitor agent progress\n");
        report.append("- Validate integration points\n");
        report.append("- Continue quality gate enforcement\n");

        Log.info(report.toString());
    }

    /**
     * Schedule the next phase if applicable
     */
    private void scheduleNextPhase(String currentPhaseId) {
        List<String> phaseIds = new ArrayList<>(EXECUTION_PHASES.keySet());
        int currentIndex = phaseIds.indexOf(currentPhaseId);

        if (currentIndex < phaseIds.size() - 1) {
            String nextPhaseId = phaseIds.get(currentIndex + 1);
            TestPhase nextPhase = EXECUTION_PHASES.get(nextPhaseId);

            Log.info("Next phase scheduled: " + nextPhaseId + " at hour " + nextPhase.startHour);
        } else {
            Log.info("All test phases completed");
            finalizeExecution();
        }
    }

    /**
     * Finalize execution and generate final report
     */
    private void finalizeExecution() {
        Log.info("========== FINALIZING TEST EXECUTION ==========");

        try {
            // Generate final metrics
            String metricsReport = performanceValidator.generateMetricsReport();
            String trendReport = performanceValidator.generateTrendReport();

            // Generate integration summary
            IntegrationTestSuite.IntegrationTestSummary integrationSummary = integrationTestSuite.getSummary();

            Log.info("Integration Test Summary: " + integrationSummary);
            Log.info("Metrics Report:\n" + metricsReport);
            Log.info("Trend Report:\n" + trendReport);

            // Calculate total duration
            Duration totalDuration = Duration.between(executionStartTime, Instant.now());
            Log.info("Total Execution Time: " + totalDuration.toMinutes() + " minutes");

            // Stop scheduler
            stopScheduler();

        } catch (Exception e) {
            Log.error("Finalization failed", e);
        }
    }

    /**
     * Get execution status
     */
    public ExecutionStatus getStatus() {
        ExecutionStatus status = new ExecutionStatus();
        status.isRunning = isSchedulerRunning;
        status.startTime = executionStartTime;
        status.executionPhases = new HashMap<>(EXECUTION_PHASES);
        status.completedPhases = countCompletedPhases();
        status.totalPhases = EXECUTION_PHASES.size();
        return status;
    }

    /**
     * Count completed phases
     */
    private int countCompletedPhases() {
        return (int) scheduledTasks.values().stream()
            .filter(ScheduledFuture::isDone)
            .count();
    }

    /**
     * Stop the scheduler
     */
    public synchronized void stopScheduler() {
        isSchedulerRunning = false;
        scheduler.shutdown();
        Log.info("Test execution scheduler stopped");
    }

    // ===== Inner Classes =====

    public static class TestPhase {
        public String description;
        public int startHour;
        public int endHour;
        public int expectedTestCount;

        public TestPhase(String description, int startHour, int endHour, int expectedTestCount) {
            this.description = description;
            this.startHour = startHour;
            this.endHour = endHour;
            this.expectedTestCount = expectedTestCount;
        }
    }

    public static class ExecutionStatus {
        public boolean isRunning;
        public Instant startTime;
        public Map<String, TestPhase> executionPhases;
        public int completedPhases;
        public int totalPhases;

        public double getProgressPercentage() {
            return totalPhases > 0 ? (double) completedPhases / totalPhases * 100 : 0;
        }
    }
}

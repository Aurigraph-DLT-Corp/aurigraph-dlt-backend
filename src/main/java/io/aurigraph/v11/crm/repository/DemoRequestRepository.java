package io.aurigraph.v11.crm.repository;

import io.aurigraph.v11.crm.entity.DemoRequest;
import io.quarkus.hibernate.orm.panache.PanacheRepository;

import jakarta.enterprise.context.ApplicationScoped;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * DemoRequestRepository - Data access layer for DemoRequest entities
 *
 * Provides methods for demo scheduling, status tracking, and follow-up management
 */
@ApplicationScoped
public class DemoRequestRepository implements PanacheRepository<DemoRequest> {

    /**
     * Find demo by UUID ID
     */
    public DemoRequest findById(UUID id) {
        return find("id = ?1", id).firstResult();
    }

    /**
     * Delete demo by UUID ID
     */
    public void deleteById(UUID id) {
        delete("id = ?1", id);
    }

    /**
     * Find demos for a lead
     */
    public List<DemoRequest> findByLeadId(UUID leadId) {
        return find("leadId = ?1 ORDER BY scheduledAt DESC", leadId).list();
    }

    /**
     * Find pending demos (scheduled but not completed)
     */
    public List<DemoRequest> findPendingDemos() {
        return find("status IN ?1 ORDER BY scheduledAt ASC",
                List.of(DemoRequest.DemoStatus.REQUESTED,
                        DemoRequest.DemoStatus.SCHEDULED,
                        DemoRequest.DemoStatus.CONFIRMED))
                .list();
    }

    /**
     * Find demos scheduled for today
     */
    public List<DemoRequest> findTodaysDemos() {
        ZonedDateTime today = ZonedDateTime.now().withHour(0).withMinute(0).withSecond(0);
        ZonedDateTime tomorrow = today.plusDays(1);

        return find("scheduledAt >= ?1 AND scheduledAt < ?2 ORDER BY scheduledAt ASC", today, tomorrow)
                .list();
    }

    /**
     * Find demos awaiting confirmation
     */
    public List<DemoRequest> findAwaitingConfirmation() {
        return find("status = ?1 ORDER BY scheduledAt ASC", DemoRequest.DemoStatus.SCHEDULED)
                .list();
    }

    /**
     * Find completed demos awaiting follow-up
     */
    public List<DemoRequest> findCompletedAwaitingFollowUp() {
        return find("status = ?1 AND followUpEmailSent = FALSE ORDER BY completedAt ASC",
                DemoRequest.DemoStatus.COMPLETED)
                .list();
    }

    /**
     * Find demos needing reminders
     */
    public List<DemoRequest> findDemosNeedingReminders(ZonedDateTime within24h, ZonedDateTime within1h) {
        return find("(status = ?1 AND scheduledAt <= ?2 AND reminder24hSent = FALSE) OR " +
                   "(status = ?1 AND scheduledAt <= ?3 AND reminder1hSent = FALSE)",
                DemoRequest.DemoStatus.CONFIRMED, within24h, within1h)
                .list();
    }

    /**
     * Get demo completion rate
     */
    public long countCompleted() {
        return count("status", DemoRequest.DemoStatus.COMPLETED);
    }

    /**
     * Get demo cancellation rate
     */
    public long countCancelled() {
        return count("status", DemoRequest.DemoStatus.CANCELLED);
    }

    /**
     * Get demo no-show rate
     */
    public long countNoShows() {
        return count("status", DemoRequest.DemoStatus.NO_SHOW);
    }

    /**
     * Find demos by outcome
     */
    public List<DemoRequest> findByOutcome(DemoRequest.DemoOutcome outcome) {
        return find("demoOutcome", outcome).list();
    }

    /**
     * Update demo status
     */
    public void updateStatus(UUID demoId, DemoRequest.DemoStatus newStatus) {
        update("status = ?1, updatedAt = ?2 WHERE id = ?3",
                newStatus, ZonedDateTime.now(), demoId);
    }

    /**
     * Mark reminder as sent
     */
    public void markReminder24hSent(UUID demoId) {
        update("reminder24hSent = TRUE, reminder24hSentAt = ?1, updatedAt = ?1 WHERE id = ?2",
                ZonedDateTime.now(), demoId);
    }

    /**
     * Mark 1-hour reminder as sent
     */
    public void markReminder1hSent(UUID demoId) {
        update("reminder1hSent = TRUE, reminder1hSentAt = ?1, updatedAt = ?1 WHERE id = ?2",
                ZonedDateTime.now(), demoId);
    }

    /**
     * Demo statistics DTO
     */
    public record DemoStatistics(
            long totalDemos,
            long completedDemos,
            long cancelledDemos,
            long noShows,
            double completionRate,
            double averageSatisfaction
    ) {}
}

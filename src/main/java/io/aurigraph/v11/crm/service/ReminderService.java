package io.aurigraph.v11.crm.service;

import io.aurigraph.v11.crm.entity.DemoRequest;
import io.aurigraph.v11.crm.repository.DemoRequestRepository;
import io.aurigraph.v11.crm.entity.Lead;
import io.aurigraph.v11.crm.repository.LeadRepository;
import lombok.extern.slf4j.Slf4j;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

/**
 * ReminderService - Manages automated reminders for scheduled demos
 *
 * Features:
 * - 24-hour pre-demo reminder
 * - 1-hour pre-demo reminder
 * - Configurable reminder windows
 * - Prevent duplicate reminders
 * - Track reminder delivery status
 */
@Slf4j
@ApplicationScoped
public class ReminderService {

    @Inject
    DemoRequestRepository demoRepository;

    @Inject
    LeadRepository leadRepository;

    @Inject
    EmailService emailService;

    /**
     * Process 24-hour reminders
     * Should be called hourly via scheduled task
     */
    @Transactional
    public void processReminders24h() {
        log.info("Processing 24-hour demo reminders...");

        ZonedDateTime now = ZonedDateTime.now();
        ZonedDateTime in24h = now.plus(24, ChronoUnit.HOURS);

        // Find demos scheduled within the next 24 hours that haven't been reminded
        List<DemoRequest> demosForReminder = demoRepository.find(
            "status = ?1 AND scheduledAt >= ?2 AND scheduledAt <= ?3 AND reminder24hSent = FALSE",
            DemoRequest.DemoStatus.CONFIRMED, now, in24h
        ).list();

        for (DemoRequest demo : demosForReminder) {
            sendReminder24h(demo);
        }

        log.info("Processed {} 24-hour reminders", demosForReminder.size());
    }

    /**
     * Process 1-hour reminders
     * Should be called every 30 minutes via scheduled task
     */
    @Transactional
    public void processReminders1h() {
        log.info("Processing 1-hour demo reminders...");

        ZonedDateTime now = ZonedDateTime.now();
        ZonedDateTime in1h = now.plus(1, ChronoUnit.HOURS);

        // Find demos scheduled within the next 1 hour that haven't been reminded
        List<DemoRequest> demosForReminder = demoRepository.find(
            "status = ?1 AND scheduledAt >= ?2 AND scheduledAt <= ?3 AND reminder1hSent = FALSE",
            DemoRequest.DemoStatus.CONFIRMED, now, in1h
        ).list();

        for (DemoRequest demo : demosForReminder) {
            sendReminder1h(demo);
        }

        log.info("Processed {} 1-hour reminders", demosForReminder.size());
    }

    /**
     * Send 24-hour reminder for specific demo
     */
    @Transactional
    void sendReminder24h(DemoRequest demo) {
        log.info("Sending 24-hour reminder for demo {}", demo.getId());

        try {
            Optional<Lead> leadOpt = leadRepository.find("id", demo.getLeadId()).firstResultOptional();

            if (leadOpt.isEmpty()) {
                log.warn("Lead not found for demo {}", demo.getId());
                return;
            }

            Lead lead = leadOpt.get();

            // Send email reminder
            emailService.sendReminder24h(lead, demo);

            // Mark as sent
            demoRepository.update(
                "reminder24hSent = TRUE, reminder24hSentAt = ?1, updatedAt = ?1 WHERE id = ?2",
                ZonedDateTime.now(), demo.getId()
            );

            log.info("24-hour reminder sent for demo {}", demo.getId());
        } catch (Exception e) {
            log.error("Failed to send 24-hour reminder for demo {}", demo.getId(), e);
        }
    }

    /**
     * Send 1-hour reminder for specific demo
     */
    @Transactional
    void sendReminder1h(DemoRequest demo) {
        log.info("Sending 1-hour reminder for demo {}", demo.getId());

        try {
            Optional<Lead> leadOpt = leadRepository.find("id", demo.getLeadId()).firstResultOptional();

            if (leadOpt.isEmpty()) {
                log.warn("Lead not found for demo {}", demo.getId());
                return;
            }

            Lead lead = leadOpt.get();

            // Send email reminder
            emailService.sendReminder1h(lead, demo);

            // Mark as sent
            demoRepository.update(
                "reminder1hSent = TRUE, reminder1hSentAt = ?1, updatedAt = ?1 WHERE id = ?2",
                ZonedDateTime.now(), demo.getId()
            );

            log.info("1-hour reminder sent for demo {}", demo.getId());
        } catch (Exception e) {
            log.error("Failed to send 1-hour reminder for demo {}", demo.getId(), e);
        }
    }

    /**
     * Check if demo has passed and should be marked completed
     */
    @Transactional
    public void processMissedDemos() {
        log.info("Processing missed demos...");

        ZonedDateTime now = ZonedDateTime.now();

        // Find confirmed demos that have passed their end time
        List<DemoRequest> missedDemos = demoRepository.find(
            "status = ?1 AND endTime <= ?2",
            DemoRequest.DemoStatus.CONFIRMED, now
        ).list();

        for (DemoRequest demo : missedDemos) {
            // Auto-mark as no-show after 1 hour
            if (demo.getEndTime().isBefore(now.minus(1, ChronoUnit.HOURS))) {
                markAsNoShow(demo);
            }
        }

        log.info("Processed {} missed demos", missedDemos.size());
    }

    /**
     * Mark demo as no-show
     */
    @Transactional
    void markAsNoShow(DemoRequest demo) {
        log.info("Marking demo {} as no-show", demo.getId());

        demoRepository.update(
            "status = ?1, demoOutcome = ?2, completedAt = ?3, updatedAt = ?3 WHERE id = ?4",
            DemoRequest.DemoStatus.NO_SHOW,
            DemoRequest.DemoOutcome.NO_SHOW,
            ZonedDateTime.now(),
            demo.getId()
        );

        // Reduce lead score for no-show
        try {
            Optional<Lead> leadOpt = leadRepository.find("id", demo.getLeadId()).firstResultOptional();
            if (leadOpt.isPresent()) {
                Lead lead = leadOpt.get();
                int newScore = Math.max(0, lead.getLeadScore() - 15);
                leadRepository.update(
                    "leadScore = ?1, updatedAt = ?2 WHERE id = ?3",
                    newScore, ZonedDateTime.now(), lead.getId()
                );
                log.info("Lead score reduced by 15 for no-show demo");
            }
        } catch (Exception e) {
            log.error("Failed to update lead score for no-show", e);
        }
    }

    /**
     * Get statistics for reminders
     */
    public ReminderStatistics getStatistics() {
        long total24hDue = demoRepository.count(
            "status = ?1 AND scheduledAt <= ?2 AND reminder24hSent = FALSE",
            DemoRequest.DemoStatus.CONFIRMED, ZonedDateTime.now()
        );

        long total1hDue = demoRepository.count(
            "status = ?1 AND scheduledAt <= ?2 AND reminder1hSent = FALSE",
            DemoRequest.DemoStatus.CONFIRMED, ZonedDateTime.now().plus(1, ChronoUnit.HOURS)
        );

        long totalSent24h = demoRepository.count(
            "reminder24hSent = TRUE"
        );

        long totalSent1h = demoRepository.count(
            "reminder1hSent = TRUE"
        );

        return new ReminderStatistics(total24hDue, total1hDue, totalSent24h, totalSent1h);
    }

    /**
     * Reminder statistics DTO
     */
    public record ReminderStatistics(
        long pending24hReminders,
        long pending1hReminders,
        long sentReminders24h,
        long sentReminders1h
    ) {}
}

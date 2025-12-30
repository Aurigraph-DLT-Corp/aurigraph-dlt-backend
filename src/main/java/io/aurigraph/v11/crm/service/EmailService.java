package io.aurigraph.v11.crm.service;

import io.aurigraph.v11.crm.entity.DemoRequest;
import io.aurigraph.v11.crm.entity.Lead;
import lombok.extern.slf4j.Slf4j;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * EmailService - Handles email communications for CRM
 *
 * Features:
 * - Calendar invite generation (iCalendar format)
 * - Demo reminder emails (24h, 1h before)
 * - Follow-up sequences
 * - Personalized templates
 */
@Slf4j
@ApplicationScoped
public class EmailService {

    private static final String COMPANY_EMAIL = "noreply@aurigraph.io";
    private static final String COMPANY_NAME = "Aurigraph";
    private static final String DEMO_TIMEZONE = "America/New_York";

    /**
     * Send calendar invite with meeting details
     */
    public void sendCalendarInvite(Lead lead, DemoRequest demo) {
        log.info("Sending calendar invite for demo {} to {}", demo.getId(), lead.getEmail());

        try {
            String subject = "Aurigraph Demo Confirmation - " + formatDateTime(demo.getStartTime());
            String meetingTitle = "Aurigraph Platform Demo";

            String iCalContent = generateICalendar(
                meetingTitle,
                demo.getStartTime(),
                demo.getEndTime(),
                demo.getMeetingUrl(),
                lead,
                demo
            );

            // In production, this would integrate with email provider
            // (SendGrid, AWS SES, Mailgun, etc.)
            log.debug("iCalendar content:\n{}", iCalContent);

            // Send email with calendar attachment
            sendEmailWithAttachment(
                lead.getEmail(),
                subject,
                buildInviteHtml(lead, demo),
                "calendar.ics",
                iCalContent.getBytes()
            );

            log.info("Calendar invite sent successfully to {}", lead.getEmail());
        } catch (Exception e) {
            log.error("Failed to send calendar invite for demo {}", demo.getId(), e);
            throw new RuntimeException("Failed to send calendar invite", e);
        }
    }

    /**
     * Send 24-hour reminder
     */
    public void sendReminder24h(Lead lead, DemoRequest demo) {
        log.info("Sending 24h reminder for demo {} to {}", demo.getId(), lead.getEmail());

        String subject = "Reminder: Your Aurigraph demo is in 24 hours";
        String htmlContent = buildReminderHtml(lead, demo, "tomorrow");

        sendEmail(lead.getEmail(), subject, htmlContent);
        log.info("24h reminder sent to {}", lead.getEmail());
    }

    /**
     * Send 1-hour reminder
     */
    public void sendReminder1h(Lead lead, DemoRequest demo) {
        log.info("Sending 1h reminder for demo {} to {}", demo.getId(), lead.getEmail());

        String subject = "Reminder: Your Aurigraph demo starts in 1 hour";
        String htmlContent = buildReminderHtml(lead, demo, "soon");

        sendEmail(lead.getEmail(), subject, htmlContent);
        log.info("1h reminder sent to {}", lead.getEmail());
    }

    /**
     * Send follow-up email after demo completion
     */
    public void sendFollowUp(Lead lead, DemoRequest demo) {
        log.info("Sending follow-up email for demo {} to {}", demo.getId(), lead.getEmail());

        String subject = "Thank you for your Aurigraph demo - Next Steps";
        String htmlContent = buildFollowUpHtml(lead, demo);

        sendEmail(lead.getEmail(), subject, htmlContent);
        log.info("Follow-up email sent to {}", lead.getEmail());
    }

    /**
     * Generate iCalendar format string for calendar invites
     */
    private String generateICalendar(String title, ZonedDateTime start, ZonedDateTime end,
                                     String meetingUrl, Lead lead, DemoRequest demo) {
        DateTimeFormatter icalFormat = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'");

        return "BEGIN:VCALENDAR\n" +
               "VERSION:2.0\n" +
               "PRODID:-//Aurigraph//Aurigraph Platform//EN\n" +
               "CALSCALE:GREGORIAN\n" +
               "METHOD:REQUEST\n" +
               "BEGIN:VEVENT\n" +
               "UID:demo-" + demo.getId() + "@aurigraph.io\n" +
               "DTSTAMP:" + icalFormat.format(ZonedDateTime.now()) + "\n" +
               "DTSTART:" + icalFormat.format(start) + "\n" +
               "DTEND:" + icalFormat.format(end) + "\n" +
               "SUMMARY:" + title + "\n" +
               "DESCRIPTION:Platform Demo with " + COMPANY_NAME + "\n" +
               "LOCATION:" + meetingUrl + "\n" +
               "ORGANIZER:mailto:" + COMPANY_EMAIL + "\n" +
               "ATTENDEE;CN=\"" + lead.getFirstName() + " " + lead.getLastName() +
               "\":mailto:" + lead.getEmail() + "\n" +
               "URL:" + meetingUrl + "\n" +
               "STATUS:CONFIRMED\n" +
               "SEQUENCE:0\n" +
               "END:VEVENT\n" +
               "END:VCALENDAR";
    }

    /**
     * Build HTML for calendar invite email
     */
    private String buildInviteHtml(Lead lead, DemoRequest demo) {
        return "<html><body>" +
               "<h2>Your Aurigraph Demo is Confirmed</h2>" +
               "<p>Hi " + lead.getFirstName() + ",</p>" +
               "<p>Thank you for scheduling a demo with Aurigraph! We're excited to show you our platform.</p>" +
               "<div style='background: #f5f5f5; padding: 20px; border-radius: 5px;'>" +
               "<p><strong>Demo Details:</strong></p>" +
               "<p>Date & Time: " + formatDateTime(demo.getStartTime()) + "</p>" +
               "<p>Duration: " + demo.getDurationMinutes() + " minutes</p>" +
               "<p>Platform: " + demo.getMeetingPlatform() + "</p>" +
               "<p><a href='" + demo.getMeetingUrl() + "' style='background: #667eea; color: white; padding: 10px 20px; border-radius: 5px; text-decoration: none;'>Join Demo</a></p>" +
               "</div>" +
               "<p>If you have any questions, please reply to this email.</p>" +
               "<p>Best regards,<br/>The Aurigraph Team</p>" +
               "</body></html>";
    }

    /**
     * Build HTML for reminder emails
     */
    private String buildReminderHtml(Lead lead, DemoRequest demo, String timeFrame) {
        String message = timeFrame.equals("soon")
            ? "Your demo starts in 1 hour!"
            : "Your demo is tomorrow at " + formatDateTime(demo.getStartTime());

        return "<html><body>" +
               "<h2>Demo Reminder</h2>" +
               "<p>Hi " + lead.getFirstName() + ",</p>" +
               "<p>" + message + "</p>" +
               "<p><a href='" + demo.getMeetingUrl() + "' style='background: #667eea; color: white; padding: 10px 20px; border-radius: 5px; text-decoration: none;'>Join Now</a></p>" +
               "<p>See you soon!</p>" +
               "</body></html>";
    }

    /**
     * Build HTML for follow-up email
     */
    private String buildFollowUpHtml(Lead lead, DemoRequest demo) {
        String outcome = demo.getDemoOutcome() != null ? demo.getDemoOutcome().toString() : "COMPLETED";

        return "<html><body>" +
               "<h2>Thank You for Your Demo</h2>" +
               "<p>Hi " + lead.getFirstName() + ",</p>" +
               "<p>Thank you for taking the time to explore Aurigraph today. We hope you enjoyed the demonstration.</p>" +
               "<div style='background: #f5f5f5; padding: 20px; border-radius: 5px;'>" +
               "<p><strong>Next Steps:</strong></p>" +
               "<ul>" +
               "<li>Explore our documentation and tutorials</li>" +
               "<li>Schedule a technical deep dive with our team</li>" +
               "<li>Request a pricing quote for your use case</li>" +
               "</ul>" +
               "</div>" +
               "<p>Our team is here to answer any questions. Feel free to reach out anytime.</p>" +
               "<p>Best regards,<br/>The Aurigraph Sales Team</p>" +
               "</body></html>";
    }

    /**
     * Send simple email (stub - actual implementation would use email provider SDK)
     */
    private void sendEmail(String to, String subject, String htmlContent) {
        log.info("Email stub - would send to: {}, subject: {}", to, subject);
        // In production: integrate with SendGrid, AWS SES, Mailgun, etc.
    }

    /**
     * Send email with attachment (stub)
     */
    private void sendEmailWithAttachment(String to, String subject, String htmlContent,
                                        String attachmentName, byte[] attachmentContent) {
        log.info("Email with attachment stub - would send to: {}, subject: {}", to, subject);
        // In production: use email provider's attachment API
    }

    /**
     * Format datetime for display
     */
    private String formatDateTime(ZonedDateTime dateTime) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM d, yyyy 'at' h:mm a z");
        return dateTime.format(formatter);
    }
}

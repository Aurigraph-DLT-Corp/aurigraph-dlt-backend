package io.aurigraph.v11.billing.services;

import io.aurigraph.v11.billing.models.Billing;
import io.aurigraph.v11.billing.models.Invoice;
import io.aurigraph.v11.billing.models.Subscription;
import io.aurigraph.v11.billing.repositories.InvoiceRepository;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
@Transactional
public class InvoiceService {

    @Inject
    InvoiceRepository invoiceRepository;

    public Invoice createInvoiceFromBilling(Billing billing, Subscription subscription) throws Exception {
        Log.infof("Creating invoice from billing: %d", billing.id);

        Invoice invoice = Invoice.builder()
            .subscriptionId(billing.subscriptionId)
            .userId(billing.userId)
            .subtotal(billing.amount)
            .taxAmount(billing.taxAmount)
            .totalAmount(billing.totalAmount)
            .status(Invoice.InvoiceStatus.DRAFT)
            .invoiceDate(LocalDateTime.now())
            .currency(billing.currency)
            .paymentTerms("Net 30")
            .notes("Invoice for " + subscription.plan.name + " subscription")
            .build();

        invoiceRepository.persist(invoice);
        Log.infof("Invoice created: %s", invoice.invoiceNumber);

        return invoice;
    }

    public Invoice sendInvoice(Long invoiceId) throws Exception {
        Log.infof("Sending invoice: %d", invoiceId);

        Invoice invoice = invoiceRepository.findById(invoiceId);
        if (invoice == null) {
            throw new IllegalArgumentException("Invoice not found");
        }

        if (invoice.status != Invoice.InvoiceStatus.DRAFT) {
            throw new IllegalArgumentException("Invoice must be in draft status to send");
        }

        invoice.status = Invoice.InvoiceStatus.SENT;
        invoice.sentDate = LocalDateTime.now();
        invoice.updatedAt = LocalDateTime.now();

        invoiceRepository.persist(invoice);

        // Send email (async)
        try {
            sendInvoiceEmail(invoice);
        } catch (Exception e) {
            Log.warnf("Failed to send invoice email: %s", e.getMessage());
        }

        return invoice;
    }

    private void sendInvoiceEmail(Invoice invoice) {
        // TODO: Implement email sending via Mailer
        Log.infof("Sending invoice email for: %s to user: %s", invoice.invoiceNumber, invoice.userId);
    }

    public Invoice markAsViewed(Long invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId);
        if (invoice == null) {
            throw new IllegalArgumentException("Invoice not found");
        }

        if (invoice.status == Invoice.InvoiceStatus.SENT) {
            invoice.status = Invoice.InvoiceStatus.VIEWED;
            invoice.updatedAt = LocalDateTime.now();
            invoiceRepository.persist(invoice);
        }

        return invoice;
    }

    public Invoice recordPartialPayment(Long invoiceId, BigDecimal amount) {
        Invoice invoice = invoiceRepository.findById(invoiceId);
        if (invoice == null) {
            throw new IllegalArgumentException("Invoice not found");
        }

        BigDecimal newAmountPaid = (invoice.amountPaid != null ? invoice.amountPaid : BigDecimal.ZERO)
            .add(amount);

        if (newAmountPaid.compareTo(invoice.totalAmount) > 0) {
            throw new IllegalArgumentException("Payment exceeds invoice total");
        }

        invoice.amountPaid = newAmountPaid;
        invoice.updatedAt = LocalDateTime.now();

        if (invoice.isFullyPaid()) {
            invoice.status = Invoice.InvoiceStatus.PAID;
            invoice.paidDate = LocalDateTime.now();
        } else {
            invoice.status = Invoice.InvoiceStatus.PARTIALLY_PAID;
        }

        invoiceRepository.persist(invoice);
        return invoice;
    }

    public Invoice cancelInvoice(Long invoiceId, String reason) {
        Invoice invoice = invoiceRepository.findById(invoiceId);
        if (invoice == null) {
            throw new IllegalArgumentException("Invoice not found");
        }

        invoice.status = Invoice.InvoiceStatus.CANCELLED;
        invoice.notes = reason;
        invoice.updatedAt = LocalDateTime.now();

        invoiceRepository.persist(invoice);
        return invoice;
    }

    public Optional<Invoice> getInvoiceById(Long invoiceId) {
        return invoiceRepository.findByIdOptional(invoiceId);
    }

    public Optional<Invoice> getInvoiceByNumber(String invoiceNumber) {
        return invoiceRepository.findByInvoiceNumber(invoiceNumber);
    }

    public List<Invoice> getUserInvoices(String userId) {
        return invoiceRepository.findUserInvoicesOrderByDateDesc(userId);
    }

    public List<Invoice> getSubscriptionInvoices(Long subscriptionId) {
        return invoiceRepository.findBySubscriptionId(subscriptionId);
    }

    public List<Invoice> getPaidInvoices() {
        return invoiceRepository.findPaidInvoices();
    }

    public List<Invoice> getUnpaidInvoices() {
        return invoiceRepository.findUnpaidInvoices();
    }

    public List<Invoice> getOverdueInvoices() {
        return invoiceRepository.findOverdueInvoices();
    }

    public List<Invoice> getUserUnpaidInvoices(String userId) {
        return invoiceRepository.findByUserIdAndStatus(
            userId,
            Invoice.InvoiceStatus.SENT
        );
    }

    public List<Invoice> getInvoicesByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return invoiceRepository.findByInvoiceDateRange(startDate, endDate);
    }

    public List<Invoice> getUserInvoicesByDateRange(
        String userId,
        LocalDateTime startDate,
        LocalDateTime endDate
    ) {
        return invoiceRepository.findByUserIdAndDateRange(userId, startDate, endDate);
    }

    public long getOverdueInvoiceCountByUserId(String userId) {
        return invoiceRepository.countOverdueInvoicesByUserId(userId);
    }

    public List<Invoice> getInvoicesNeedingReminder() {
        return invoiceRepository.findInvoicesNeedingToBeReminded(
            LocalDateTime.now().minusDays(7)
        );
    }

    public void sendReminderForOverdueInvoice(Long invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId);
        if (invoice != null && invoice.isOverdue()) {
            try {
                sendInvoiceEmail(invoice);
                Log.infof("Reminder sent for invoice: %s", invoice.invoiceNumber);
            } catch (Exception e) {
                Log.warnf("Failed to send reminder for invoice: %s", invoice.invoiceNumber);
            }
        }
    }

    public long getPaidInvoiceCount() {
        return invoiceRepository.count("status", Invoice.InvoiceStatus.PAID);
    }

    public long getUnpaidInvoiceCount() {
        return invoiceRepository.count("status in ?1", List.of(
            Invoice.InvoiceStatus.SENT,
            Invoice.InvoiceStatus.VIEWED,
            Invoice.InvoiceStatus.OVERDUE
        ));
    }
}

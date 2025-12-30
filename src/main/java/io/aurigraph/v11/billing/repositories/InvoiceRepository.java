package io.aurigraph.v11.billing.repositories;

import io.aurigraph.v11.billing.models.Invoice;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class InvoiceRepository implements PanacheRepository<Invoice> {

    public Optional<Invoice> findByInvoiceNumber(String invoiceNumber) {
        return find("invoiceNumber", invoiceNumber).firstResultOptional();
    }

    public List<Invoice> findByUserId(String userId) {
        return list("userId", userId);
    }

    public List<Invoice> findBySubscriptionId(Long subscriptionId) {
        return list("subscriptionId", subscriptionId);
    }

    public List<Invoice> findByStatus(Invoice.InvoiceStatus status) {
        return list("status", status);
    }

    public List<Invoice> findPaidInvoices() {
        return list("status", Invoice.InvoiceStatus.PAID);
    }

    public List<Invoice> findUnpaidInvoices() {
        return list(
            "status in ?1",
            List.of(
                Invoice.InvoiceStatus.SENT,
                Invoice.InvoiceStatus.VIEWED,
                Invoice.InvoiceStatus.OVERDUE,
                Invoice.InvoiceStatus.PARTIALLY_PAID
            )
        );
    }

    public List<Invoice> findOverdueInvoices() {
        LocalDateTime now = LocalDateTime.now();
        return list(
            "dueDate < ?1 and status != ?2",
            now,
            Invoice.InvoiceStatus.PAID
        );
    }

    public List<Invoice> findByUserIdAndStatus(String userId, Invoice.InvoiceStatus status) {
        return list("userId = ?1 and status = ?2", userId, status);
    }

    public List<Invoice> findByInvoiceDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return list(
            "invoiceDate >= ?1 and invoiceDate <= ?2",
            startDate,
            endDate
        );
    }

    public List<Invoice> findByUserIdAndDateRange(
        String userId,
        LocalDateTime startDate,
        LocalDateTime endDate
    ) {
        return list(
            "userId = ?1 and invoiceDate >= ?2 and invoiceDate <= ?3",
            userId,
            startDate,
            endDate
        );
    }

    public List<Invoice> findDraftInvoices() {
        return list("status", Invoice.InvoiceStatus.DRAFT);
    }

    public List<Invoice> findSentInvoices() {
        return list("status", Invoice.InvoiceStatus.SENT);
    }

    public List<Invoice> findCancelledInvoices() {
        return list("status", Invoice.InvoiceStatus.CANCELLED);
    }

    public long countPaidInvoicesByUserId(String userId) {
        return count("userId = ?1 and status = ?2", userId, Invoice.InvoiceStatus.PAID);
    }

    public long countOverdueInvoicesByUserId(String userId) {
        LocalDateTime now = LocalDateTime.now();
        return count(
            "userId = ?1 and dueDate < ?2 and status != ?3",
            userId,
            now,
            Invoice.InvoiceStatus.PAID
        );
    }

    public Optional<Invoice> findByStripeInvoiceId(String stripeInvoiceId) {
        return find("stripeInvoiceId", stripeInvoiceId).firstResultOptional();
    }

    public List<Invoice> findMonthlyInvoices(int year, int month) {
        LocalDateTime startDate = LocalDateTime.of(year, month, 1, 0, 0);
        LocalDateTime endDate = startDate.plusMonths(1);
        return list(
            "invoiceDate >= ?1 and invoiceDate < ?2",
            startDate,
            endDate
        );
    }

    public List<Invoice> findInvoicesNeedingToBeReminded(LocalDateTime before) {
        return list(
            "status in ?1 and sentDate <= ?2 and dueDate > ?3",
            List.of(
                Invoice.InvoiceStatus.SENT,
                Invoice.InvoiceStatus.VIEWED,
                Invoice.InvoiceStatus.PARTIALLY_PAID
            ),
            before,
            LocalDateTime.now()
        );
    }

    public List<Invoice> findUserInvoicesOrderByDateDesc(String userId) {
        return find("userId order by invoiceDate desc", userId).list();
    }
}

package io.aurigraph.v11.demo.model;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Demo Entity - Persists demo configurations with timeout management
 */
@Entity
@Table(name = "demos")
public class Demo extends PanacheEntityBase {

    @Id
    @Column(length = 64, nullable = false)
    public String id;

    @NotBlank
    @Column(name = "demo_name", nullable = false, length = 255)
    public String demoName;

    @NotBlank
    @Email
    @Column(name = "user_email", nullable = false, length = 255)
    public String userEmail;

    @NotBlank
    @Column(name = "user_name", nullable = false, length = 255)
    public String userName;

    @Column(length = 1000)
    public String description;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    public DemoStatus status = DemoStatus.PENDING;

    @Column(name = "created_at", nullable = false)
    public LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "last_activity", nullable = false)
    public LocalDateTime lastActivity = LocalDateTime.now();

    @Column(name = "expires_at", nullable = false)
    public LocalDateTime expiresAt;

    @Column(name = "duration_minutes", nullable = false)
    public int durationMinutes = 10;

    @Column(name = "is_admin_demo", nullable = false)
    public boolean isAdminDemo = false;

    @Column(name = "transaction_count", nullable = false)
    public long transactionCount = 0;

    @Column(name = "merkle_root", length = 64)
    public String merkleRoot;

    // Store channels as JSON
    @Column(name = "channels_json", columnDefinition = "TEXT")
    public String channelsJson;

    // Store validators as JSON
    @Column(name = "validators_json", columnDefinition = "TEXT")
    public String validatorsJson;

    // Store business nodes as JSON
    @Column(name = "business_nodes_json", columnDefinition = "TEXT")
    public String businessNodesJson;

    // Store slim nodes as JSON
    @Column(name = "slim_nodes_json", columnDefinition = "TEXT")
    public String slimNodesJson;

    public enum DemoStatus {
        PENDING,
        RUNNING,
        STOPPED,
        EXPIRED,
        ERROR
    }

    /**
     * Check if demo is expired
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    /**
     * Extend demo duration
     */
    public void extend(int additionalMinutes) {
        this.expiresAt = this.expiresAt.plusMinutes(additionalMinutes);
        this.durationMinutes += additionalMinutes;
        this.lastActivity = LocalDateTime.now();
    }

    /**
     * Mark as expired
     */
    public void expire() {
        this.status = DemoStatus.EXPIRED;
        this.lastActivity = LocalDateTime.now();
    }

    /**
     * Increment transaction count
     */
    public void addTransactions(long count) {
        this.transactionCount += count;
        this.lastActivity = LocalDateTime.now();
    }

    // Static finder methods
    public static List<Demo> findAllActive() {
        return list("status != ?1 ORDER BY createdAt DESC", DemoStatus.EXPIRED);
    }

    public static List<Demo> findExpired() {
        return list("expiresAt < ?1 AND status != ?2", LocalDateTime.now(), DemoStatus.EXPIRED);
    }

    public static Demo findByIdOptional(String id) {
        return findById(id);
    }
}

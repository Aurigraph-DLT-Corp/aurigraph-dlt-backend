package io.aurigraph.v11.mobile;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Mobile App Management Service
 *
 * Handles mobile app user registration, management, and analytics.
 *
 * @version 11.4.0
 * @since 2025-10-13
 */
@ApplicationScoped
public class MobileAppService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MobileAppService.class);

    // In-memory storage (will be migrated to database)
    private final Map<String, MobileAppUser> users = new ConcurrentHashMap<>();

    /**
     * Register a new mobile app user
     */
    public Uni<MobileAppUser> registerUser(MobileAppUser user) {
        return Uni.createFrom().item(() -> {
            LOGGER.info("Registering mobile user: {}", user.getEmail());

            // Generate user ID
            user.setUserId("MU-" + UUID.randomUUID().toString());
            user.setRegisteredAt(Instant.now());
            user.setActive(true);

            // Store user
            users.put(user.getUserId(), user);

            LOGGER.info("Mobile user registered: {}", user.getUserId());
            return user;
        });
    }

    /**
     * Get user by ID
     */
    public Uni<MobileAppUser> getUser(String userId) {
        return Uni.createFrom().item(() -> {
            MobileAppUser user = users.get(userId);
            if (user == null) {
                throw new UserNotFoundException("User not found: " + userId);
            }
            return user;
        });
    }

    /**
     * Get user by email
     */
    public Uni<MobileAppUser> getUserByEmail(String email) {
        return Uni.createFrom().item(() -> {
            return users.values().stream()
                    .filter(u -> email.equalsIgnoreCase(u.getEmail()))
                    .findFirst()
                    .orElseThrow(() -> new UserNotFoundException("User not found with email: " + email));
        });
    }

    /**
     * List all users
     */
    public Uni<List<MobileAppUser>> listUsers() {
        return Uni.createFrom().item(() -> new ArrayList<>(users.values()));
    }

    /**
     * List users by device type
     */
    public Uni<List<MobileAppUser>> listUsersByDevice(MobileAppUser.DeviceType deviceType) {
        return Uni.createFrom().item(() ->
                users.values().stream()
                        .filter(u -> u.getDeviceType() == deviceType)
                        .collect(Collectors.toList())
        );
    }

    /**
     * Update user status
     */
    public Uni<MobileAppUser> updateUserStatus(String userId, boolean isActive) {
        return getUser(userId).map(user -> {
            user.setActive(isActive);
            users.put(userId, user);
            LOGGER.info("User status updated: {} - active: {}", userId, isActive);
            return user;
        });
    }

    /**
     * Update user KYC status
     */
    public Uni<MobileAppUser> updateKycStatus(String userId, String kycStatus) {
        return getUser(userId).map(user -> {
            user.setKycStatus(kycStatus);
            if ("VERIFIED".equals(kycStatus)) {
                user.setUserTier("VERIFIED");
            }
            users.put(userId, user);
            LOGGER.info("User KYC status updated: {} - {}", userId, kycStatus);
            return user;
        });
    }

    /**
     * Record user login
     */
    public Uni<MobileAppUser> recordLogin(String userId) {
        return getUser(userId).map(user -> {
            user.setLastLoginAt(Instant.now());
            users.put(userId, user);
            return user;
        });
    }

    /**
     * Get mobile app statistics
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();

        stats.put("totalUsers", users.size());
        stats.put("activeUsers", users.values().stream().filter(MobileAppUser::isActive).count());
        stats.put("iosUsers", users.values().stream()
                .filter(u -> u.getDeviceType() == MobileAppUser.DeviceType.IOS).count());
        stats.put("androidUsers", users.values().stream()
                .filter(u -> u.getDeviceType() == MobileAppUser.DeviceType.ANDROID).count());
        stats.put("verifiedUsers", users.values().stream()
                .filter(u -> "VERIFIED".equals(u.getKycStatus())).count());
        stats.put("pendingKyc", users.values().stream()
                .filter(u -> "PENDING".equals(u.getKycStatus())).count());

        return stats;
    }

    /**
     * Delete user (GDPR compliance)
     */
    public Uni<Void> deleteUser(String userId) {
        return Uni.createFrom().item(() -> {
            users.remove(userId);
            LOGGER.info("User deleted: {}", userId);
            return null;
        });
    }

    // Custom Exception
    public static class UserNotFoundException extends RuntimeException {
        public UserNotFoundException(String message) {
            super(message);
        }
    }
}

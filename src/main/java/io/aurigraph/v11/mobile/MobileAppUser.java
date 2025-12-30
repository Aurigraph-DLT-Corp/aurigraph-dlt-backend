package io.aurigraph.v11.mobile;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

/**
 * Mobile App User Model
 *
 * Represents a user who has signed up via the mobile app.
 *
 * @version 11.4.0
 * @since 2025-10-13
 */
public class MobileAppUser {

    @JsonProperty("userId")
    private String userId;

    @JsonProperty("email")
    private String email;

    @JsonProperty("phoneNumber")
    private String phoneNumber;

    @JsonProperty("fullName")
    private String fullName;

    @JsonProperty("deviceType")
    private DeviceType deviceType; // IOS, ANDROID

    @JsonProperty("deviceToken")
    private String deviceToken; // For push notifications

    @JsonProperty("appVersion")
    private String appVersion;

    @JsonProperty("platform")
    private String platform; // iOS 17.0, Android 14, etc.

    @JsonProperty("registeredAt")
    private Instant registeredAt;

    @JsonProperty("lastLoginAt")
    private Instant lastLoginAt;

    @JsonProperty("isActive")
    private boolean isActive = true;

    @JsonProperty("kycStatus")
    private String kycStatus = "PENDING"; // PENDING, VERIFIED, REJECTED

    @JsonProperty("userTier")
    private String userTier = "BASIC"; // BASIC, VERIFIED, PREMIUM

    @JsonProperty("walletAddress")
    private String walletAddress;

    // Constructors
    public MobileAppUser() {
        this.registeredAt = Instant.now();
    }

    public MobileAppUser(String email, String fullName, DeviceType deviceType) {
        this();
        this.email = email;
        this.fullName = fullName;
        this.deviceType = deviceType;
    }

    // Getters and setters
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public DeviceType getDeviceType() { return deviceType; }
    public void setDeviceType(DeviceType deviceType) { this.deviceType = deviceType; }

    public String getDeviceToken() { return deviceToken; }
    public void setDeviceToken(String deviceToken) { this.deviceToken = deviceToken; }

    public String getAppVersion() { return appVersion; }
    public void setAppVersion(String appVersion) { this.appVersion = appVersion; }

    public String getPlatform() { return platform; }
    public void setPlatform(String platform) { this.platform = platform; }

    public Instant getRegisteredAt() { return registeredAt; }
    public void setRegisteredAt(Instant registeredAt) { this.registeredAt = registeredAt; }

    public Instant getLastLoginAt() { return lastLoginAt; }
    public void setLastLoginAt(Instant lastLoginAt) { this.lastLoginAt = lastLoginAt; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    public String getKycStatus() { return kycStatus; }
    public void setKycStatus(String kycStatus) { this.kycStatus = kycStatus; }

    public String getUserTier() { return userTier; }
    public void setUserTier(String userTier) { this.userTier = userTier; }

    public String getWalletAddress() { return walletAddress; }
    public void setWalletAddress(String walletAddress) { this.walletAddress = walletAddress; }

    // Device Type Enum
    public enum DeviceType {
        IOS,
        ANDROID,
        WEB
    }

    @Override
    public String toString() {
        return String.format("MobileAppUser{userId='%s', email='%s', deviceType=%s, tier='%s'}",
                userId, email, deviceType, userTier);
    }
}

package io.aurigraph.v11.tokenization.models;

/**
 * Storage Information Model
 *
 * Represents LevelDB storage information for ALL nodes
 *
 * @version 11.3.0
 * @author Backend Development Agent (BDA)
 */
public class StorageInfo {

    public String basePath;
    public long totalSize;  // Total bytes stored
    public int slimNodeCount;  // Number of slim nodes (legacy field)
    public int channelCount;  // Number of active channels
    public boolean compressionEnabled;
    public boolean encryptionEnabled;

    public StorageInfo() {
        // Default constructor for Jackson/JSON deserialization
    }

    public StorageInfo(String basePath, long totalSize, int slimNodeCount,
                      int channelCount, boolean compressionEnabled,
                      boolean encryptionEnabled) {
        this.basePath = basePath;
        this.totalSize = totalSize;
        this.slimNodeCount = slimNodeCount;
        this.channelCount = channelCount;
        this.compressionEnabled = compressionEnabled;
        this.encryptionEnabled = encryptionEnabled;
    }
}

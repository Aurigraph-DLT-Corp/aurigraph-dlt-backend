package io.aurigraph.v11.contracts.composite;

import java.time.Instant;
import java.util.*;

/**
 * Media Token (ERC-1155) - Tracks media files and documents associated with the asset
 */
public class MediaToken extends SecondaryToken {
    private List<MediaAsset> mediaAssets;

    public MediaToken(String tokenId, String compositeId, List<MediaAsset> mediaAssets) {
        super(tokenId, compositeId, SecondaryTokenType.MEDIA);
        this.mediaAssets = new ArrayList<>(mediaAssets);
    }

    @Override
    public void updateData(Map<String, Object> updateData) {
        this.lastUpdated = Instant.now();
        this.data.putAll(updateData);
    }

    public void addMedia(MediaAsset asset) {
        mediaAssets.add(asset);
        this.lastUpdated = Instant.now();
    }

    public void removeMedia(String mediaId) {
        mediaAssets.removeIf(asset -> mediaId.equals(asset.getMediaId()));
        this.lastUpdated = Instant.now();
    }

    // Getters
    public List<MediaAsset> getMediaAssets() { return List.copyOf(mediaAssets); }

    /**
     * Media asset record
     */
    public static class MediaAsset {
        private final String mediaId;
        private final String mediaType;
        private final String fileName;
        private final String ipfsHash;
        private final long fileSize;
        private final Instant uploadedAt;
        private String accessLevel;
        private Map<String, Object> metadata;

        public MediaAsset(String mediaId, String mediaType, String fileName, 
                         String ipfsHash, long fileSize) {
            this.mediaId = mediaId;
            this.mediaType = mediaType;
            this.fileName = fileName;
            this.ipfsHash = ipfsHash;
            this.fileSize = fileSize;
            this.uploadedAt = Instant.now();
            this.accessLevel = "PUBLIC";
            this.metadata = new HashMap<>();
        }

        // Getters and setters
        public String getMediaId() { return mediaId; }
        public String getMediaType() { return mediaType; }
        public String getFileName() { return fileName; }
        public String getIpfsHash() { return ipfsHash; }
        public long getFileSize() { return fileSize; }
        public Instant getUploadedAt() { return uploadedAt; }
        public String getAccessLevel() { return accessLevel; }
        public void setAccessLevel(String accessLevel) { this.accessLevel = accessLevel; }
        public Map<String, Object> getMetadata() { return metadata; }
    }
}
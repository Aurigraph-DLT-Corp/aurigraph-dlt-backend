package io.aurigraph.v11.channels.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

/**
 * Message Model for Aurigraph V11 - LevelDB Compatible
 *
 * Represents a message in a channel.
 * Supports text, attachments, reactions, and threading.
 *
 * LevelDB Storage: Uses messageId as primary key
 * JSON Serializable: All fields stored as JSON in LevelDB
 *
 * @version 4.0.0 (LevelDB Migration - Oct 8, 2025)
 * @author Aurigraph V11 Development Team
 */
public class Message {

    @JsonProperty("messageId")
    private String messageId;

    @JsonProperty("channelId")
    private String channelId;

    @JsonProperty("senderAddress")
    private String senderAddress;

    @JsonProperty("messageType")
    private MessageType messageType;

    @JsonProperty("content")
    private String content;

    @JsonProperty("contentHash")
    private String contentHash;

    // Threading
    @JsonProperty("threadId")
    private String threadId;

    @JsonProperty("replyToMessageId")
    private String replyToMessageId;

    // Attachments
    @JsonProperty("hasAttachments")
    private Boolean hasAttachments = false;

    @JsonProperty("attachmentUrls")
    private String attachmentUrls;

    @JsonProperty("attachmentCount")
    private Integer attachmentCount = 0;

    // Reactions
    @JsonProperty("reactions")
    private String reactions;

    @JsonProperty("reactionCount")
    private Integer reactionCount = 0;

    // Status
    @JsonProperty("status")
    private MessageStatus status;

    @JsonProperty("isEdited")
    private Boolean isEdited = false;

    @JsonProperty("isDeleted")
    private Boolean isDeleted = false;

    // Timestamps
    @JsonProperty("createdAt")
    private Instant createdAt;

    @JsonProperty("editedAt")
    private Instant editedAt;

    @JsonProperty("deletedAt")
    private Instant deletedAt;

    @JsonProperty("readAt")
    private Instant readAt;

    // Metadata
    @JsonProperty("metadata")
    private String metadata;

    @JsonProperty("mentions")
    private String mentions;

    // Encryption
    @JsonProperty("isEncrypted")
    private Boolean isEncrypted = false;

    @JsonProperty("encryptionKey")
    private String encryptionKey;

    // ==================== CONSTRUCTORS ====================

    public Message() {
        this.createdAt = Instant.now();
        this.status = MessageStatus.SENT;
        this.hasAttachments = false;
        this.attachmentCount = 0;
        this.reactionCount = 0;
        this.isEdited = false;
        this.isDeleted = false;
        this.isEncrypted = false;
    }

    public Message(String messageId, String channelId, String senderAddress, String content) {
        this();
        this.messageId = messageId;
        this.channelId = channelId;
        this.senderAddress = senderAddress;
        this.content = content;
        this.messageType = MessageType.TEXT;
    }

    // ==================== BUSINESS LOGIC METHODS ====================

    /**
     * Ensure createdAt is set (call before first persist)
     */
    public void ensureCreatedAt() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    /**
     * Mark message as read
     */
    public void markAsRead() {
        if (status == MessageStatus.SENT) {
            this.status = MessageStatus.READ;
            this.readAt = Instant.now();
        }
    }

    /**
     * Edit message content
     */
    public void edit(String newContent) {
        if (isDeleted) {
            throw new IllegalStateException("Cannot edit a deleted message");
        }
        this.content = newContent;
        this.isEdited = true;
        this.editedAt = Instant.now();
    }

    /**
     * Delete message
     */
    public void delete() {
        if (isDeleted) {
            throw new IllegalStateException("Message is already deleted");
        }
        this.isDeleted = true;
        this.deletedAt = Instant.now();
        this.status = MessageStatus.DELETED;
    }

    /**
     * Add reaction
     */
    public void addReaction(String reaction) {
        this.reactionCount++;
        String currentReactions = this.reactions != null ? this.reactions : "";
        this.reactions = currentReactions.isEmpty() ? reaction : currentReactions + "," + reaction;
    }

    /**
     * Remove reaction
     */
    public void removeReaction() {
        if (this.reactionCount > 0) {
            this.reactionCount--;
        }
    }

    // ==================== GETTERS AND SETTERS ====================

    public String getMessageId() { return messageId; }
    public void setMessageId(String messageId) { this.messageId = messageId; }

    public String getChannelId() { return channelId; }
    public void setChannelId(String channelId) { this.channelId = channelId; }

    public String getSenderAddress() { return senderAddress; }
    public void setSenderAddress(String senderAddress) { this.senderAddress = senderAddress; }

    public MessageType getMessageType() { return messageType; }
    public void setMessageType(MessageType messageType) { this.messageType = messageType; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getContentHash() { return contentHash; }
    public void setContentHash(String contentHash) { this.contentHash = contentHash; }

    public String getThreadId() { return threadId; }
    public void setThreadId(String threadId) { this.threadId = threadId; }

    public String getReplyToMessageId() { return replyToMessageId; }
    public void setReplyToMessageId(String replyToMessageId) { this.replyToMessageId = replyToMessageId; }

    public Boolean getHasAttachments() { return hasAttachments; }
    public void setHasAttachments(Boolean hasAttachments) { this.hasAttachments = hasAttachments; }

    public String getAttachmentUrls() { return attachmentUrls; }
    public void setAttachmentUrls(String attachmentUrls) { this.attachmentUrls = attachmentUrls; }

    public Integer getAttachmentCount() { return attachmentCount; }
    public void setAttachmentCount(Integer attachmentCount) { this.attachmentCount = attachmentCount; }

    public String getReactions() { return reactions; }
    public void setReactions(String reactions) { this.reactions = reactions; }

    public Integer getReactionCount() { return reactionCount; }
    public void setReactionCount(Integer reactionCount) { this.reactionCount = reactionCount; }

    public MessageStatus getStatus() { return status; }
    public void setStatus(MessageStatus status) { this.status = status; }

    public Boolean getIsEdited() { return isEdited; }
    public void setIsEdited(Boolean isEdited) { this.isEdited = isEdited; }

    public Boolean getIsDeleted() { return isDeleted; }
    public void setIsDeleted(Boolean isDeleted) { this.isDeleted = isDeleted; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getEditedAt() { return editedAt; }
    public void setEditedAt(Instant editedAt) { this.editedAt = editedAt; }

    public Instant getDeletedAt() { return deletedAt; }
    public void setDeletedAt(Instant deletedAt) { this.deletedAt = deletedAt; }

    public Instant getReadAt() { return readAt; }
    public void setReadAt(Instant readAt) { this.readAt = readAt; }

    public String getMetadata() { return metadata; }
    public void setMetadata(String metadata) { this.metadata = metadata; }

    public String getMentions() { return mentions; }
    public void setMentions(String mentions) { this.mentions = mentions; }

    public Boolean getIsEncrypted() { return isEncrypted; }
    public void setIsEncrypted(Boolean isEncrypted) { this.isEncrypted = isEncrypted; }

    public String getEncryptionKey() { return encryptionKey; }
    public void setEncryptionKey(String encryptionKey) { this.encryptionKey = encryptionKey; }

    // ==================== ENUM DEFINITIONS ====================

    public enum MessageType {
        TEXT,           // Plain text message
        IMAGE,          // Image message
        FILE,           // File attachment
        VOICE,          // Voice message
        VIDEO,          // Video message
        SYSTEM,         // System notification
        COMMAND,        // Bot command
        REACTION        // Reaction to another message
    }

    public enum MessageStatus {
        SENT,           // Message sent
        DELIVERED,      // Message delivered
        READ,           // Message read
        FAILED,         // Message failed to send
        DELETED         // Message deleted
    }

    @Override
    public String toString() {
        return String.format("Message{messageId='%s', channelId='%s', sender='%s', type=%s, status=%s}",
                messageId, channelId, senderAddress, messageType, status);
    }
}

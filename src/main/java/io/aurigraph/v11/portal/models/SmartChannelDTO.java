package io.aurigraph.v11.portal.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.List;

public class SmartChannelDTO {
    @JsonProperty("channel_id")
    private String channelId;
    @JsonProperty("type")
    private String type;
    @JsonProperty("name")
    private String name;
    @JsonProperty("description")
    private String description;
    @JsonProperty("status")
    private String status;
    @JsonProperty("total_channels")
    private Integer totalChannels;
    @JsonProperty("active_channels")
    private Integer activeChannels;
    @JsonProperty("total_capacity")
    private String totalCapacity;
    @JsonProperty("average_route_length")
    private Double averageRouteLength;
    @JsonProperty("routing_success")
    private Double routingSuccess;
    @JsonProperty("transaction_volume")
    private String transactionVolume;
    @JsonProperty("average_fee")
    private Double averageFee;
    @JsonProperty("created_at")
    private Instant createdAt;

    public SmartChannelDTO() {}

    private SmartChannelDTO(Builder builder) {
        this.channelId = builder.channelId;
        this.type = builder.type;
        this.name = builder.name;
        this.description = builder.description;
        this.status = builder.status;
        this.totalChannels = builder.totalChannels;
        this.activeChannels = builder.activeChannels;
        this.totalCapacity = builder.totalCapacity;
        this.averageRouteLength = builder.averageRouteLength;
        this.routingSuccess = builder.routingSuccess;
        this.transactionVolume = builder.transactionVolume;
        this.averageFee = builder.averageFee;
        this.createdAt = builder.createdAt;
    }

    public String getChannelId() { return channelId; }
    public String getType() { return type; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getStatus() { return status; }
    public Integer getTotalChannels() { return totalChannels; }
    public Integer getActiveChannels() { return activeChannels; }
    public String getTotalCapacity() { return totalCapacity; }
    public Double getAverageRouteLength() { return averageRouteLength; }
    public Double getRoutingSuccess() { return routingSuccess; }
    public String getTransactionVolume() { return transactionVolume; }
    public Double getAverageFee() { return averageFee; }
    public Instant getCreatedAt() { return createdAt; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private String channelId;
        private String type;
        private String name;
        private String description;
        private String status;
        private Integer totalChannels;
        private Integer activeChannels;
        private String totalCapacity;
        private Double averageRouteLength;
        private Double routingSuccess;
        private String transactionVolume;
        private Double averageFee;
        private Instant createdAt;

        public Builder channelId(String channelId) { this.channelId = channelId; return this; }
        public Builder type(String type) { this.type = type; return this; }
        public Builder name(String name) { this.name = name; return this; }
        public Builder description(String description) { this.description = description; return this; }
        public Builder status(String status) { this.status = status; return this; }
        public Builder totalChannels(Integer totalChannels) { this.totalChannels = totalChannels; return this; }
        public Builder activeChannels(Integer activeChannels) { this.activeChannels = activeChannels; return this; }
        public Builder totalCapacity(String totalCapacity) { this.totalCapacity = totalCapacity; return this; }
        public Builder averageRouteLength(Double averageRouteLength) { this.averageRouteLength = averageRouteLength; return this; }
        public Builder routingSuccess(Double routingSuccess) { this.routingSuccess = routingSuccess; return this; }
        public Builder transactionVolume(String transactionVolume) { this.transactionVolume = transactionVolume; return this; }
        public Builder averageFee(Double averageFee) { this.averageFee = averageFee; return this; }
        public Builder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }

        public SmartChannelDTO build() { return new SmartChannelDTO(this); }
    }
}

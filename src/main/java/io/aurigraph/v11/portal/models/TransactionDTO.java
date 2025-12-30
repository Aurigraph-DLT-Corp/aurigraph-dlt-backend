package io.aurigraph.v11.portal.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.List;

public class TransactionDTO {
    @JsonProperty("tx_hash")
    private String txHash;
    @JsonProperty("from")
    private String from;
    @JsonProperty("to")
    private String to;
    @JsonProperty("amount")
    private String amount;
    @JsonProperty("gas_used")
    private Long gasUsed;
    @JsonProperty("gas_price")
    private String gasPrice;
    @JsonProperty("status")
    private String status;
    @JsonProperty("block_height")
    private Long blockHeight;
    @JsonProperty("timestamp")
    private Instant timestamp;
    @JsonProperty("nonce")
    private Long nonce;
    @JsonProperty("type")
    private String type;
    @JsonProperty("fee")
    private String fee;

    public TransactionDTO() {}

    private TransactionDTO(Builder builder) {
        this.txHash = builder.txHash;
        this.from = builder.from;
        this.to = builder.to;
        this.amount = builder.amount;
        this.gasUsed = builder.gasUsed;
        this.gasPrice = builder.gasPrice;
        this.status = builder.status;
        this.blockHeight = builder.blockHeight;
        this.timestamp = builder.timestamp;
        this.nonce = builder.nonce;
        this.type = builder.type;
        this.fee = builder.fee;
    }

    public String getTxHash() { return txHash; }
    public String getFrom() { return from; }
    public String getTo() { return to; }
    public String getAmount() { return amount; }
    public Long getGasUsed() { return gasUsed; }
    public String getGasPrice() { return gasPrice; }
    public String getStatus() { return status; }
    public Long getBlockHeight() { return blockHeight; }
    public Instant getTimestamp() { return timestamp; }
    public Long getNonce() { return nonce; }
    public String getType() { return type; }
    public String getFee() { return fee; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private String txHash;
        private String from;
        private String to;
        private String amount;
        private Long gasUsed;
        private String gasPrice;
        private String status;
        private Long blockHeight;
        private Instant timestamp;
        private Long nonce;
        private String type;
        private String fee;

        public Builder txHash(String txHash) { this.txHash = txHash; return this; }
        public Builder from(String from) { this.from = from; return this; }
        public Builder to(String to) { this.to = to; return this; }
        public Builder amount(String amount) { this.amount = amount; return this; }
        public Builder gasUsed(Long gasUsed) { this.gasUsed = gasUsed; return this; }
        public Builder gasPrice(String gasPrice) { this.gasPrice = gasPrice; return this; }
        public Builder status(String status) { this.status = status; return this; }
        public Builder blockHeight(Long blockHeight) { this.blockHeight = blockHeight; return this; }
        public Builder timestamp(Instant timestamp) { this.timestamp = timestamp; return this; }
        public Builder nonce(Long nonce) { this.nonce = nonce; return this; }
        public Builder type(String type) { this.type = type; return this; }
        public Builder fee(String fee) { this.fee = fee; return this; }

        public TransactionDTO build() { return new TransactionDTO(this); }
    }
}

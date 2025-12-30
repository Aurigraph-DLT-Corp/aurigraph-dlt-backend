package io.aurigraph.v11.portal.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.List;

public class BlockDTO {
    @JsonProperty("block_height")
    private Long blockHeight;
    @JsonProperty("block_hash")
    private String blockHash;
    @JsonProperty("timestamp")
    private Instant timestamp;
    @JsonProperty("transaction_count")
    private Integer transactionCount;
    @JsonProperty("miner")
    private String miner;
    @JsonProperty("difficulty")
    private String difficulty;
    @JsonProperty("gas_used")
    private Long gasUsed;
    @JsonProperty("gas_limit")
    private Long gasLimit;
    @JsonProperty("block_time")
    private Integer blockTime;
    @JsonProperty("validator_signatures")
    private Integer validatorSignatures;

    public BlockDTO() {}

    private BlockDTO(Builder builder) {
        this.blockHeight = builder.blockHeight;
        this.blockHash = builder.blockHash;
        this.timestamp = builder.timestamp;
        this.transactionCount = builder.transactionCount;
        this.miner = builder.miner;
        this.difficulty = builder.difficulty;
        this.gasUsed = builder.gasUsed;
        this.gasLimit = builder.gasLimit;
        this.blockTime = builder.blockTime;
        this.validatorSignatures = builder.validatorSignatures;
    }

    public Long getBlockHeight() { return blockHeight; }
    public String getBlockHash() { return blockHash; }
    public Instant getTimestamp() { return timestamp; }
    public Integer getTransactionCount() { return transactionCount; }
    public String getMiner() { return miner; }
    public String getDifficulty() { return difficulty; }
    public Long getGasUsed() { return gasUsed; }
    public Long getGasLimit() { return gasLimit; }
    public Integer getBlockTime() { return blockTime; }
    public Integer getValidatorSignatures() { return validatorSignatures; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Long blockHeight;
        private String blockHash;
        private Instant timestamp;
        private Integer transactionCount;
        private String miner;
        private String difficulty;
        private Long gasUsed;
        private Long gasLimit;
        private Integer blockTime;
        private Integer validatorSignatures;

        public Builder blockHeight(Long blockHeight) { this.blockHeight = blockHeight; return this; }
        public Builder blockHash(String blockHash) { this.blockHash = blockHash; return this; }
        public Builder timestamp(Instant timestamp) { this.timestamp = timestamp; return this; }
        public Builder transactionCount(Integer transactionCount) { this.transactionCount = transactionCount; return this; }
        public Builder miner(String miner) { this.miner = miner; return this; }
        public Builder difficulty(String difficulty) { this.difficulty = difficulty; return this; }
        public Builder gasUsed(Long gasUsed) { this.gasUsed = gasUsed; return this; }
        public Builder gasLimit(Long gasLimit) { this.gasLimit = gasLimit; return this; }
        public Builder blockTime(Integer blockTime) { this.blockTime = blockTime; return this; }
        public Builder validatorSignatures(Integer validatorSignatures) { this.validatorSignatures = validatorSignatures; return this; }

        public BlockDTO build() { return new BlockDTO(this); }
    }
}

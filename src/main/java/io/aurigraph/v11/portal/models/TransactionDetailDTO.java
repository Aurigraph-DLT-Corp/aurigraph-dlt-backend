package io.aurigraph.v11.portal.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.List;

public class TransactionDetailDTO {
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
    @JsonProperty("gas_limit")
    private Long gasLimit;
    @JsonProperty("status")
    private String status;
    @JsonProperty("block_height")
    private Long blockHeight;
    @JsonProperty("block_hash")
    private String blockHash;
    @JsonProperty("timestamp")
    private Instant timestamp;
    @JsonProperty("nonce")
    private Long nonce;
    @JsonProperty("type")
    private String type;
    @JsonProperty("fee")
    private String fee;
    @JsonProperty("confirmations")
    private Integer confirmations;
    @JsonProperty("transaction_index")
    private Integer transactionIndex;
    @JsonProperty("input_data")
    private String inputData;
    @JsonProperty("output_data")
    private String outputData;
    @JsonProperty("contract_address")
    private String contractAddress;
    @JsonProperty("error")
    private String error;

    public TransactionDetailDTO() {}

    private TransactionDetailDTO(Builder builder) {
        this.txHash = builder.txHash;
        this.from = builder.from;
        this.to = builder.to;
        this.amount = builder.amount;
        this.gasUsed = builder.gasUsed;
        this.gasPrice = builder.gasPrice;
        this.gasLimit = builder.gasLimit;
        this.status = builder.status;
        this.blockHeight = builder.blockHeight;
        this.blockHash = builder.blockHash;
        this.timestamp = builder.timestamp;
        this.nonce = builder.nonce;
        this.type = builder.type;
        this.fee = builder.fee;
        this.confirmations = builder.confirmations;
        this.transactionIndex = builder.transactionIndex;
        this.inputData = builder.inputData;
        this.outputData = builder.outputData;
        this.contractAddress = builder.contractAddress;
        this.error = builder.error;
    }

    public String getTxHash() { return txHash; }
    public String getFrom() { return from; }
    public String getTo() { return to; }
    public String getAmount() { return amount; }
    public Long getGasUsed() { return gasUsed; }
    public String getGasPrice() { return gasPrice; }
    public Long getGasLimit() { return gasLimit; }
    public String getStatus() { return status; }
    public Long getBlockHeight() { return blockHeight; }
    public String getBlockHash() { return blockHash; }
    public Instant getTimestamp() { return timestamp; }
    public Long getNonce() { return nonce; }
    public String getType() { return type; }
    public String getFee() { return fee; }
    public Integer getConfirmations() { return confirmations; }
    public Integer getTransactionIndex() { return transactionIndex; }
    public String getInputData() { return inputData; }
    public String getOutputData() { return outputData; }
    public String getContractAddress() { return contractAddress; }
    public String getError() { return error; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private String txHash;
        private String from;
        private String to;
        private String amount;
        private Long gasUsed;
        private String gasPrice;
        private Long gasLimit;
        private String status;
        private Long blockHeight;
        private String blockHash;
        private Instant timestamp;
        private Long nonce;
        private String type;
        private String fee;
        private Integer confirmations;
        private Integer transactionIndex;
        private String inputData;
        private String outputData;
        private String contractAddress;
        private String error;

        public Builder txHash(String txHash) { this.txHash = txHash; return this; }
        public Builder from(String from) { this.from = from; return this; }
        public Builder to(String to) { this.to = to; return this; }
        public Builder amount(String amount) { this.amount = amount; return this; }
        public Builder gasUsed(Long gasUsed) { this.gasUsed = gasUsed; return this; }
        public Builder gasPrice(String gasPrice) { this.gasPrice = gasPrice; return this; }
        public Builder gasLimit(Long gasLimit) { this.gasLimit = gasLimit; return this; }
        public Builder status(String status) { this.status = status; return this; }
        public Builder blockHeight(Long blockHeight) { this.blockHeight = blockHeight; return this; }
        public Builder blockHash(String blockHash) { this.blockHash = blockHash; return this; }
        public Builder timestamp(Instant timestamp) { this.timestamp = timestamp; return this; }
        public Builder nonce(Long nonce) { this.nonce = nonce; return this; }
        public Builder type(String type) { this.type = type; return this; }
        public Builder fee(String fee) { this.fee = fee; return this; }
        public Builder confirmations(Integer confirmations) { this.confirmations = confirmations; return this; }
        public Builder transactionIndex(Integer transactionIndex) { this.transactionIndex = transactionIndex; return this; }
        public Builder inputData(String inputData) { this.inputData = inputData; return this; }
        public Builder outputData(String outputData) { this.outputData = outputData; return this; }
        public Builder contractAddress(String contractAddress) { this.contractAddress = contractAddress; return this; }
        public Builder error(String error) { this.error = error; return this; }

        public TransactionDetailDTO build() { return new TransactionDetailDTO(this); }
    }
}

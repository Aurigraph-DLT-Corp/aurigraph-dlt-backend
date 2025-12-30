package io.aurigraph.v11.portal.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.List;

public class AnalyticsDTO {
    @JsonProperty("period")
    private String period;
    @JsonProperty("total_transactions")
    private Integer totalTransactions;
    @JsonProperty("total_volume")
    private String totalVolume;
    @JsonProperty("average_transaction_value")
    private String averageTransactionValue;
    @JsonProperty("peak_transaction_rate")
    private Integer peakTransactionRate;
    @JsonProperty("peak_transaction_time")
    private String peakTransactionTime;
    @JsonProperty("unique_users")
    private Integer uniqueUsers;
    @JsonProperty("new_users")
    private Integer newUsers;
    @JsonProperty("returning_users")
    private Integer returningUsers;
    @JsonProperty("wallet_creations")
    private Integer walletCreations;
    @JsonProperty("contract_deployments")
    private Integer contractDeployments;
    @JsonProperty("token_transfers")
    private Integer tokenTransfers;
    @JsonProperty("nft_transactions")
    private Integer nftTransactions;
    @JsonProperty("top_token_by_volume")
    private String topTokenByVolume;
    @JsonProperty("top_token_volume")
    private String topTokenVolume;
    @JsonProperty("top_contract")
    private String topContract;
    @JsonProperty("top_contract_volume")
    private String topContractVolume;
    @JsonProperty("total_fees")
    private String totalFees;
    @JsonProperty("average_fee")
    private String averageFee;
    @JsonProperty("network_congestion")
    private String networkCongestion;
    @JsonProperty("error")
    private String error;

    public AnalyticsDTO() {}

    private AnalyticsDTO(Builder builder) {
        this.period = builder.period;
        this.totalTransactions = builder.totalTransactions;
        this.totalVolume = builder.totalVolume;
        this.averageTransactionValue = builder.averageTransactionValue;
        this.peakTransactionRate = builder.peakTransactionRate;
        this.peakTransactionTime = builder.peakTransactionTime;
        this.uniqueUsers = builder.uniqueUsers;
        this.newUsers = builder.newUsers;
        this.returningUsers = builder.returningUsers;
        this.walletCreations = builder.walletCreations;
        this.contractDeployments = builder.contractDeployments;
        this.tokenTransfers = builder.tokenTransfers;
        this.nftTransactions = builder.nftTransactions;
        this.topTokenByVolume = builder.topTokenByVolume;
        this.topTokenVolume = builder.topTokenVolume;
        this.topContract = builder.topContract;
        this.topContractVolume = builder.topContractVolume;
        this.totalFees = builder.totalFees;
        this.averageFee = builder.averageFee;
        this.networkCongestion = builder.networkCongestion;
        this.error = builder.error;
    }

    public String getPeriod() { return period; }
    public Integer getTotalTransactions() { return totalTransactions; }
    public String getTotalVolume() { return totalVolume; }
    public String getAverageTransactionValue() { return averageTransactionValue; }
    public Integer getPeakTransactionRate() { return peakTransactionRate; }
    public String getPeakTransactionTime() { return peakTransactionTime; }
    public Integer getUniqueUsers() { return uniqueUsers; }
    public Integer getNewUsers() { return newUsers; }
    public Integer getReturningUsers() { return returningUsers; }
    public Integer getWalletCreations() { return walletCreations; }
    public Integer getContractDeployments() { return contractDeployments; }
    public Integer getTokenTransfers() { return tokenTransfers; }
    public Integer getNftTransactions() { return nftTransactions; }
    public String getTopTokenByVolume() { return topTokenByVolume; }
    public String getTopTokenVolume() { return topTokenVolume; }
    public String getTopContract() { return topContract; }
    public String getTopContractVolume() { return topContractVolume; }
    public String getTotalFees() { return totalFees; }
    public String getAverageFee() { return averageFee; }
    public String getNetworkCongestion() { return networkCongestion; }
    public String getError() { return error; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private String period;
        private Integer totalTransactions;
        private String totalVolume;
        private String averageTransactionValue;
        private Integer peakTransactionRate;
        private String peakTransactionTime;
        private Integer uniqueUsers;
        private Integer newUsers;
        private Integer returningUsers;
        private Integer walletCreations;
        private Integer contractDeployments;
        private Integer tokenTransfers;
        private Integer nftTransactions;
        private String topTokenByVolume;
        private String topTokenVolume;
        private String topContract;
        private String topContractVolume;
        private String totalFees;
        private String averageFee;
        private String networkCongestion;
        private String error;

        public Builder period(String period) { this.period = period; return this; }
        public Builder totalTransactions(Integer totalTransactions) { this.totalTransactions = totalTransactions; return this; }
        public Builder totalVolume(String totalVolume) { this.totalVolume = totalVolume; return this; }
        public Builder averageTransactionValue(String averageTransactionValue) { this.averageTransactionValue = averageTransactionValue; return this; }
        public Builder peakTransactionRate(Integer peakTransactionRate) { this.peakTransactionRate = peakTransactionRate; return this; }
        public Builder peakTransactionTime(String peakTransactionTime) { this.peakTransactionTime = peakTransactionTime; return this; }
        public Builder uniqueUsers(Integer uniqueUsers) { this.uniqueUsers = uniqueUsers; return this; }
        public Builder newUsers(Integer newUsers) { this.newUsers = newUsers; return this; }
        public Builder returningUsers(Integer returningUsers) { this.returningUsers = returningUsers; return this; }
        public Builder walletCreations(Integer walletCreations) { this.walletCreations = walletCreations; return this; }
        public Builder contractDeployments(Integer contractDeployments) { this.contractDeployments = contractDeployments; return this; }
        public Builder tokenTransfers(Integer tokenTransfers) { this.tokenTransfers = tokenTransfers; return this; }
        public Builder nftTransactions(Integer nftTransactions) { this.nftTransactions = nftTransactions; return this; }
        public Builder topTokenByVolume(String topTokenByVolume) { this.topTokenByVolume = topTokenByVolume; return this; }
        public Builder topTokenVolume(String topTokenVolume) { this.topTokenVolume = topTokenVolume; return this; }
        public Builder topContract(String topContract) { this.topContract = topContract; return this; }
        public Builder topContractVolume(String topContractVolume) { this.topContractVolume = topContractVolume; return this; }
        public Builder totalFees(String totalFees) { this.totalFees = totalFees; return this; }
        public Builder averageFee(String averageFee) { this.averageFee = averageFee; return this; }
        public Builder networkCongestion(String networkCongestion) { this.networkCongestion = networkCongestion; return this; }
        public Builder error(String error) { this.error = error; return this; }

        public AnalyticsDTO build() { return new AnalyticsDTO(this); }
    }
}

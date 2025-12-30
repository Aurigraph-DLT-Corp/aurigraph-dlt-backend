package io.aurigraph.v11.contracts.models;

import io.aurigraph.v11.contracts.ContractStatus;

import java.time.Instant;
import java.util.List;

public class ContractSearchCriteria {

    private String contractType;
    private ContractStatus status;
    private String creatorAddress;
    private String partyAddress;
    private Instant createdAfter;
    private Instant createdBefore;
    private List<String> tags;
    private String templateId;
    private int page = 0;
    private int size = 20;
    private String sortBy = "createdAt";
    private String sortDirection = "DESC";

    // Constructors
    public ContractSearchCriteria() {
    }

    public ContractSearchCriteria(String contractType, ContractStatus status, String creatorAddress,
                                  String partyAddress, Instant createdAfter, Instant createdBefore,
                                  List<String> tags, String templateId, int page, int size,
                                  String sortBy, String sortDirection) {
        this.contractType = contractType;
        this.status = status;
        this.creatorAddress = creatorAddress;
        this.partyAddress = partyAddress;
        this.createdAfter = createdAfter;
        this.createdBefore = createdBefore;
        this.tags = tags;
        this.templateId = templateId;
        this.page = page;
        this.size = size;
        this.sortBy = sortBy;
        this.sortDirection = sortDirection;
    }

    // Getters
    public String getContractType() {
        return contractType;
    }

    public ContractStatus getStatus() {
        return status;
    }

    public String getCreatorAddress() {
        return creatorAddress;
    }

    public String getPartyAddress() {
        return partyAddress;
    }

    public Instant getCreatedAfter() {
        return createdAfter;
    }

    public Instant getCreatedBefore() {
        return createdBefore;
    }

    public List<String> getTags() {
        return tags;
    }

    public String getTemplateId() {
        return templateId;
    }

    public int getPage() {
        return page;
    }

    public int getSize() {
        return size;
    }

    public String getSortBy() {
        return sortBy;
    }

    public String getSortDirection() {
        return sortDirection;
    }

    // Setters
    public void setContractType(String contractType) {
        this.contractType = contractType;
    }

    public void setStatus(ContractStatus status) {
        this.status = status;
    }

    public void setCreatorAddress(String creatorAddress) {
        this.creatorAddress = creatorAddress;
    }

    public void setPartyAddress(String partyAddress) {
        this.partyAddress = partyAddress;
    }

    public void setCreatedAfter(Instant createdAfter) {
        this.createdAfter = createdAfter;
    }

    public void setCreatedBefore(Instant createdBefore) {
        this.createdBefore = createdBefore;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public void setTemplateId(String templateId) {
        this.templateId = templateId;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public void setSortBy(String sortBy) {
        this.sortBy = sortBy;
    }

    public void setSortDirection(String sortDirection) {
        this.sortDirection = sortDirection;
    }
}
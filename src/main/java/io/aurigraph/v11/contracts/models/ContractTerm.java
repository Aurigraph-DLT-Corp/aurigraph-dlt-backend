package io.aurigraph.v11.contracts.models;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.HashMap;

/**
 * Represents a specific term or condition in a Ricardian contract
 */
public class ContractTerm {

    private String termId;
    private String title;
    private String description;
    private String termType; // e.g., "PAYMENT", "DELIVERY", "PERFORMANCE", "PENALTY"
    private BigDecimal value;
    private String currency;
    private Instant effectiveDate;
    private Instant expirationDate;
    private boolean mandatory = true;
    private String condition; // Condition that triggers this term
    private String penaltyClause;
    private Map<String, Object> parameters = new HashMap<>();

    // Constructors
    public ContractTerm() {
    }

    public ContractTerm(String termId, String title, String description, String termType) {
        this.termId = termId;
        this.title = title;
        this.description = description;
        this.termType = termType;
        this.parameters = new HashMap<>();
    }

    public ContractTerm(String termId, String title, String description, String termType,
                       BigDecimal value, String currency, Instant effectiveDate, Instant expirationDate,
                       boolean mandatory, String condition, String penaltyClause, Map<String, Object> parameters) {
        this.termId = termId;
        this.title = title;
        this.description = description;
        this.termType = termType;
        this.value = value;
        this.currency = currency;
        this.effectiveDate = effectiveDate;
        this.expirationDate = expirationDate;
        this.mandatory = mandatory;
        this.condition = condition;
        this.penaltyClause = penaltyClause;
        this.parameters = parameters != null ? parameters : new HashMap<>();
    }

    // Getters
    public String getTermId() {
        return termId;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getTermType() {
        return termType;
    }

    public BigDecimal getValue() {
        return value;
    }

    public String getCurrency() {
        return currency;
    }

    public Instant getEffectiveDate() {
        return effectiveDate;
    }

    public Instant getExpirationDate() {
        return expirationDate;
    }

    public boolean isMandatory() {
        return mandatory;
    }

    public String getCondition() {
        return condition;
    }

    public String getPenaltyClause() {
        return penaltyClause;
    }

    public Map<String, Object> getParameters() {
        return parameters;
    }

    // Setters
    public void setTermId(String termId) {
        this.termId = termId;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setTermType(String termType) {
        this.termType = termType;
    }

    public void setValue(BigDecimal value) {
        this.value = value;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public void setEffectiveDate(Instant effectiveDate) {
        this.effectiveDate = effectiveDate;
    }

    public void setExpirationDate(Instant expirationDate) {
        this.expirationDate = expirationDate;
    }

    public void setMandatory(boolean mandatory) {
        this.mandatory = mandatory;
    }

    public void setCondition(String condition) {
        this.condition = condition;
    }

    public void setPenaltyClause(String penaltyClause) {
        this.penaltyClause = penaltyClause;
    }

    public void setParameters(Map<String, Object> parameters) {
        this.parameters = parameters;
    }
}
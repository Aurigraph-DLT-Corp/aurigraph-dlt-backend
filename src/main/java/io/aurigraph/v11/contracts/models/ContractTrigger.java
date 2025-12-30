package io.aurigraph.v11.contracts.models;

import java.time.Instant;
import java.util.Map;
import java.util.HashMap;

/**
 * Represents a trigger that can execute contract logic
 */
public class ContractTrigger {

    private String triggerId;
    private String name;
    private String description;
    private TriggerType type;
    private boolean enabled = true;
    private String condition; // The condition that activates this trigger
    private String action; // The action to execute when triggered
    private Map<String, Object> parameters = new HashMap<>();
    private Instant createdAt;
    private Instant lastTriggeredAt;
    private int triggerCount = 0;

    // Constructors
    public ContractTrigger() {
    }

    public ContractTrigger(String triggerId, String name, TriggerType type, String condition, String action) {
        this.triggerId = triggerId;
        this.name = name;
        this.type = type;
        this.condition = condition;
        this.action = action;
        this.createdAt = Instant.now();
        this.parameters = new HashMap<>();
    }

    public ContractTrigger(String triggerId, String name, String description, TriggerType type,
                          boolean enabled, String condition, String action, Map<String, Object> parameters,
                          Instant createdAt, Instant lastTriggeredAt, int triggerCount) {
        this.triggerId = triggerId;
        this.name = name;
        this.description = description;
        this.type = type;
        this.enabled = enabled;
        this.condition = condition;
        this.action = action;
        this.parameters = parameters;
        this.createdAt = createdAt;
        this.lastTriggeredAt = lastTriggeredAt;
        this.triggerCount = triggerCount;
    }

    // Getters
    public String getTriggerId() {
        return triggerId;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public TriggerType getType() {
        return type;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getCondition() {
        return condition;
    }

    public String getAction() {
        return action;
    }

    public Map<String, Object> getParameters() {
        return parameters;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getLastTriggeredAt() {
        return lastTriggeredAt;
    }

    public int getTriggerCount() {
        return triggerCount;
    }

    // Setters
    public void setTriggerId(String triggerId) {
        this.triggerId = triggerId;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setType(TriggerType type) {
        this.type = type;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setCondition(String condition) {
        this.condition = condition;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public void setParameters(Map<String, Object> parameters) {
        this.parameters = parameters;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public void setLastTriggeredAt(Instant lastTriggeredAt) {
        this.lastTriggeredAt = lastTriggeredAt;
    }

    public void setTriggerCount(int triggerCount) {
        this.triggerCount = triggerCount;
    }
}
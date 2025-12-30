package io.aurigraph.v11.bridge;

/**
 * Bridge validator information
 */
public class BridgeValidator {
    private final String validatorId;
    private final String name;
    private final boolean active;

    public BridgeValidator(String validatorId, String name, boolean active) {
        this.validatorId = validatorId;
        this.name = name;
        this.active = active;
    }

    // Getters
    public String getValidatorId() { return validatorId; }
    public String getName() { return name; }
    public boolean isActive() { return active; }
}

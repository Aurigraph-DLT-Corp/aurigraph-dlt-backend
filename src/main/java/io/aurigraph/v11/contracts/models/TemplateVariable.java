package io.aurigraph.v11.contracts.models;

/**
 * Represents a variable in a contract template
 */
public class TemplateVariable {

    private String name;
    private String type; // STRING, NUMBER, BOOLEAN, DATE, ADDRESS
    private String description;
    private boolean required = true;
    private Object defaultValue;
    private String validationPattern; // Regex pattern for validation
    private String example;
    private String category; // GROUP, PARTY, ASSET, FINANCIAL, etc.

    // Constructors
    public TemplateVariable() {
    }

    public TemplateVariable(String name, String type, String description, boolean required) {
        this.name = name;
        this.type = type;
        this.description = description;
        this.required = required;
    }

    public TemplateVariable(String name, String type, String description, boolean required,
                           Object defaultValue, String validationPattern, String example, String category) {
        this.name = name;
        this.type = type;
        this.description = description;
        this.required = required;
        this.defaultValue = defaultValue;
        this.validationPattern = validationPattern;
        this.example = example;
        this.category = category;
    }

    // Getters
    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public String getDescription() {
        return description;
    }

    public boolean isRequired() {
        return required;
    }

    public Object getDefaultValue() {
        return defaultValue;
    }

    public String getValidationPattern() {
        return validationPattern;
    }

    public String getExample() {
        return example;
    }

    public String getCategory() {
        return category;
    }

    // Setters
    public void setName(String name) {
        this.name = name;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setRequired(boolean required) {
        this.required = required;
    }

    public void setDefaultValue(Object defaultValue) {
        this.defaultValue = defaultValue;
    }

    public void setValidationPattern(String validationPattern) {
        this.validationPattern = validationPattern;
    }

    public void setExample(String example) {
        this.example = example;
    }

    public void setCategory(String category) {
        this.category = category;
    }
}
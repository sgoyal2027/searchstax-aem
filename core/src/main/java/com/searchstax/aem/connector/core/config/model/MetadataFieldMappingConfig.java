package com.searchstax.aem.connector.core.config.model;

public class MetadataFieldMappingConfig {

    private String aemField;
    private String customProperty;
    private String searchStaxField;
    private String type;
    private boolean enabled;

    public String getAemField() {
        return aemField;
    }

    public void setAemField(String aemField) {
        this.aemField = aemField;
    }

    public String getCustomProperty() {
        return customProperty;
    }

    public void setCustomProperty(String customProperty) {
        this.customProperty = customProperty;
    }

    public String getSearchStaxField() {
        return searchStaxField;
    }

    public void setSearchStaxField(String searchStaxField) {
        this.searchStaxField = searchStaxField;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}

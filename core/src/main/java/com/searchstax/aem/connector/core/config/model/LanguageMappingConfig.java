package com.searchstax.aem.connector.core.config.model;

public class LanguageMappingConfig {

    private String aemLanguage;
    private String searchStaxLanguage;
    private boolean enabled;

    public String getAemLanguage() {
        return aemLanguage;
    }

    public void setAemLanguage(final String aemLanguage) {
        this.aemLanguage = aemLanguage;
    }

    public String getSearchStaxLanguage() {
        return searchStaxLanguage;
    }

    public void setSearchStaxLanguage(final String searchStaxLanguage) {
        this.searchStaxLanguage = searchStaxLanguage;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(final boolean enabled) {
        this.enabled = enabled;
    }
}

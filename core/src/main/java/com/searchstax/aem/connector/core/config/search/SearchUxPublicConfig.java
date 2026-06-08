package com.searchstax.aem.connector.core.config.search;

/**
 * Publish-safe SearchStax UX configuration mapped from connector wizard settings.
 * Only exposes endpoints and credentials required for client-side search — never update/indexing secrets.
 */
public class SearchUxPublicConfig {

    private boolean enabled;
    private String language;
    private String searchUrl;
    private String suggesterUrl;
    private String searchAuth;
    private String authType;
    private String trackApiKey;
    private String relatedSearchesUrl;
    private String relatedSearchesApiKey;
    private String analyticsBaseUrl;
    private String forwardGeocodingEndpoint;
    private String reverseGeocodingEndpoint;
    private String message;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(final boolean enabled) {
        this.enabled = enabled;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(final String language) {
        this.language = language;
    }

    public String getSearchUrl() {
        return searchUrl;
    }

    public void setSearchUrl(final String searchUrl) {
        this.searchUrl = searchUrl;
    }

    public String getSuggesterUrl() {
        return suggesterUrl;
    }

    public void setSuggesterUrl(final String suggesterUrl) {
        this.suggesterUrl = suggesterUrl;
    }

    public String getSearchAuth() {
        return searchAuth;
    }

    public void setSearchAuth(final String searchAuth) {
        this.searchAuth = searchAuth;
    }

    public String getAuthType() {
        return authType;
    }

    public void setAuthType(final String authType) {
        this.authType = authType;
    }

    public String getTrackApiKey() {
        return trackApiKey;
    }

    public void setTrackApiKey(final String trackApiKey) {
        this.trackApiKey = trackApiKey;
    }

    public String getRelatedSearchesUrl() {
        return relatedSearchesUrl;
    }

    public void setRelatedSearchesUrl(final String relatedSearchesUrl) {
        this.relatedSearchesUrl = relatedSearchesUrl;
    }

    public String getRelatedSearchesApiKey() {
        return relatedSearchesApiKey;
    }

    public void setRelatedSearchesApiKey(final String relatedSearchesApiKey) {
        this.relatedSearchesApiKey = relatedSearchesApiKey;
    }

    public String getAnalyticsBaseUrl() {
        return analyticsBaseUrl;
    }

    public void setAnalyticsBaseUrl(final String analyticsBaseUrl) {
        this.analyticsBaseUrl = analyticsBaseUrl;
    }

    public String getForwardGeocodingEndpoint() {
        return forwardGeocodingEndpoint;
    }

    public void setForwardGeocodingEndpoint(final String forwardGeocodingEndpoint) {
        this.forwardGeocodingEndpoint = forwardGeocodingEndpoint;
    }

    public String getReverseGeocodingEndpoint() {
        return reverseGeocodingEndpoint;
    }

    public void setReverseGeocodingEndpoint(final String reverseGeocodingEndpoint) {
        this.reverseGeocodingEndpoint = reverseGeocodingEndpoint;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(final String message) {
        this.message = message;
    }
}

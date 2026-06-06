package com.searchstax.aem.connector.core.services;

/**
 * Provides read access to SearchStax API settings persisted in JCR.
 * Indexing components should read settings exclusively through this interface.
 */
public interface SearchStaxConfigurationService {

    String getEndpointUrl();

    String getApiToken();

    String getSelectEndpoint();

    String getSelectToken();

    String getUpdateEndpoint();

    String getUpdateToken();

    String getAutoSuggestApi();

    String getRelatedSearchesEndpoint();

    String getPopularSearchesEndpoint();

    String getDiscoveryApiKey();

    String getAnalyticsTrackingUrl();

    String getAnalyticsTrackingKey();

    String getAnalyticsReportingUrl();

    String getAnalyticsReportingApiKey();

    String getForwardGeocodingEndpoint();

    String getReverseGeocodingEndpoint();
}

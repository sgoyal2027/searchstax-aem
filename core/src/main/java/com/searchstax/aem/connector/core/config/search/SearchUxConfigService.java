package com.searchstax.aem.connector.core.config.search;

import org.apache.sling.api.SlingHttpServletRequest;

/**
 * Builds publish-safe SearchStax UX configuration from connector wizard settings.
 */
public interface SearchUxConfigService {

    SearchUxPublicConfig getPublicConfig(SlingHttpServletRequest request);
}

package com.searchstax.aem.connector.core.config.search.impl;

import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;
import com.searchstax.aem.connector.core.config.InitialSetupConfigService;
import com.searchstax.aem.connector.core.config.LanguageConfigService;
import com.searchstax.aem.connector.core.config.model.InitialSetupConfig;
import com.searchstax.aem.connector.core.config.search.SearchUxConfigService;
import com.searchstax.aem.connector.core.config.search.SearchUxPublicConfig;
import com.searchstax.aem.connector.core.incremental.AemLanguageResolver;
import com.searchstax.aem.connector.core.services.SearchStaxConfigurationService;
import com.searchstax.aem.connector.core.utils.MultifieldParseHelper;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(service = SearchUxConfigService.class)
public class SearchUxConfigServiceImpl implements SearchUxConfigService {

    private static final String PARAM_LANGUAGE = "language";

    @Reference
    private InitialSetupConfigService initialSetupConfigService;

    @Reference
    private SearchStaxConfigurationService searchStaxConfigurationService;

    @Reference
    private LanguageConfigService languageConfigService;

    @Override
    public SearchUxPublicConfig getPublicConfig(final SlingHttpServletRequest request) {
        final SearchUxPublicConfig config = new SearchUxPublicConfig();
        final InitialSetupConfig setup = initialSetupConfigService.getConfiguration();

        if (!setup.isEnableConnector()) {
            config.setEnabled(false);
            config.setMessage("SearchStax connector is disabled in Initial Setup.");
            return config;
        }

        final String searchUrl = firstNonBlank(
                searchStaxConfigurationService.getSelectEndpoint(),
                searchStaxConfigurationService.getEndpointUrl());
        final String searchAuth = firstNonBlank(
                searchStaxConfigurationService.getSelectToken(),
                searchStaxConfigurationService.getApiToken());

        if (isBlank(searchUrl) || isBlank(searchAuth)) {
            config.setEnabled(false);
            config.setMessage("Search endpoint or credentials are not configured in API Configuration.");
            return config;
        }

        config.setEnabled(true);
        config.setLanguage(resolveLanguage(request));
        config.setSearchUrl(searchUrl.trim());
        config.setSuggesterUrl(MultifieldParseHelper.trimToEmpty(searchStaxConfigurationService.getAutoSuggestApi()));
        config.setSearchAuth(searchAuth.trim());
        config.setAuthType("token");
        config.setTrackApiKey(MultifieldParseHelper.trimToEmpty(searchStaxConfigurationService.getAnalyticsTrackingKey()));
        config.setRelatedSearchesUrl(
                MultifieldParseHelper.trimToEmpty(searchStaxConfigurationService.getRelatedSearchesEndpoint()));
        config.setRelatedSearchesApiKey(
                MultifieldParseHelper.trimToEmpty(searchStaxConfigurationService.getDiscoveryApiKey()));
        config.setAnalyticsBaseUrl(
                MultifieldParseHelper.trimToEmpty(searchStaxConfigurationService.getAnalyticsTrackingUrl()));
        config.setForwardGeocodingEndpoint(
                MultifieldParseHelper.trimToEmpty(searchStaxConfigurationService.getForwardGeocodingEndpoint()));
        config.setReverseGeocodingEndpoint(
                MultifieldParseHelper.trimToEmpty(searchStaxConfigurationService.getReverseGeocodingEndpoint()));
        config.setMessage("");
        return config;
    }

    private String resolveLanguage(final SlingHttpServletRequest request) {
        final String requestedLanguage = MultifieldParseHelper.trimToEmpty(request.getParameter(PARAM_LANGUAGE));
        if (!requestedLanguage.isEmpty()) {
            return mapLanguage(requestedLanguage);
        }

        final ResourceResolver resolver = request.getResourceResolver();
        final Resource current = request.getResource();
        if (current != null) {
            final PageManager pageManager = resolver.adaptTo(PageManager.class);
            if (pageManager != null) {
                final Page page = pageManager.getContainingPage(current);
                if (page != null) {
                    final ValueMap properties = page.getContentResource() != null
                            ? page.getContentResource().getValueMap()
                            : ValueMap.EMPTY;
                    final String aemLanguage = AemLanguageResolver.resolve(
                            resolver,
                            page.getPath(),
                            properties);
                    return mapLanguage(aemLanguage);
                }
            }
        }

        return mapLanguage("en");
    }

    private String mapLanguage(final String aemLanguage) {
        return languageConfigService.mapToSearchStaxLanguage(aemLanguage)
                .filter(mapped -> mapped != null && !mapped.isBlank())
                .orElseGet(() -> defaultIfBlank(aemLanguage, "en"));
    }

    private static String firstNonBlank(final String primary, final String fallback) {
        if (!isBlank(primary)) {
            return primary;
        }
        return fallback == null ? "" : fallback;
    }

    private static boolean isBlank(final String value) {
        return value == null || value.isBlank();
    }

    private static String defaultIfBlank(final String value, final String defaultValue) {
        return isBlank(value) ? defaultValue : value.trim();
    }
}

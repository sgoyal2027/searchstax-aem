package com.searchstax.aem.connector.core.config.search.impl;

import com.searchstax.aem.connector.core.config.InitialSetupConfigService;
import com.searchstax.aem.connector.core.config.LanguageConfigService;
import com.searchstax.aem.connector.core.config.model.InitialSetupConfig;
import com.searchstax.aem.connector.core.config.search.SearchUxPublicConfig;
import com.searchstax.aem.connector.core.services.SearchStaxConfigurationService;
import com.searchstax.aem.connector.core.testcontext.AppAemContext;
import com.searchstax.aem.connector.core.testcontext.TestReflection;
import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith({AemContextExtension.class, MockitoExtension.class})
class SearchUxConfigServiceImplTest {

    private final AemContext context = AppAemContext.newAemContext();

    @Mock
    private InitialSetupConfigService initialSetupConfigService;

    @Mock
    private SearchStaxConfigurationService searchStaxConfigurationService;

    @Mock
    private LanguageConfigService languageConfigService;

    private SearchUxConfigServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new SearchUxConfigServiceImpl();
        TestReflection.inject(service, "initialSetupConfigService", initialSetupConfigService);
        TestReflection.inject(service, "searchStaxConfigurationService", searchStaxConfigurationService);
        TestReflection.inject(service, "languageConfigService", languageConfigService);
    }

    @Test
    void returnsDisabledWhenConnectorNotEnabled() {
        final InitialSetupConfig setup = new InitialSetupConfig();
        setup.setEnableConnector(false);
        when(initialSetupConfigService.getConfiguration()).thenReturn(setup);

        final SearchUxPublicConfig config = service.getPublicConfig(context.request());

        assertFalse(config.isEnabled());
    }

    @Test
    void mapsApiConfigurationToPublicSearchUxConfig() {
        final InitialSetupConfig setup = new InitialSetupConfig();
        setup.setEnableConnector(true);
        when(initialSetupConfigService.getConfiguration()).thenReturn(setup);
        when(searchStaxConfigurationService.getSelectEndpoint()).thenReturn("https://search.example/emselect");
        when(searchStaxConfigurationService.getSelectToken()).thenReturn("select-token");
        when(searchStaxConfigurationService.getAutoSuggestApi()).thenReturn("https://search.example/emsuggest");
        when(searchStaxConfigurationService.getApiToken()).thenReturn("");
        when(searchStaxConfigurationService.getEndpointUrl()).thenReturn("");
        when(searchStaxConfigurationService.getAnalyticsTrackingKey()).thenReturn("track-key");
        when(searchStaxConfigurationService.getRelatedSearchesEndpoint()).thenReturn("https://app.searchstax.com/related");
        when(searchStaxConfigurationService.getDiscoveryApiKey()).thenReturn("discovery-key");
        when(searchStaxConfigurationService.getAnalyticsTrackingUrl()).thenReturn("https://analytics.searchstax.com");
        when(searchStaxConfigurationService.getForwardGeocodingEndpoint()).thenReturn("");
        when(searchStaxConfigurationService.getReverseGeocodingEndpoint()).thenReturn("");
        when(languageConfigService.mapToSearchStaxLanguage("fr")).thenReturn(Optional.of("fr"));

        context.request().setParameterMap(java.util.Collections.singletonMap("language", new String[] {"fr"}));

        final SearchUxPublicConfig config = service.getPublicConfig(context.request());

        assertTrue(config.isEnabled());
        assertEquals("fr", config.getLanguage());
        assertEquals("https://search.example/emselect", config.getSearchUrl());
        assertEquals("select-token", config.getSearchAuth());
        assertEquals("track-key", config.getTrackApiKey());
    }
}

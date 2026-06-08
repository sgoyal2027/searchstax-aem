package com.searchstax.aem.connector.core.services.impl;

import com.searchstax.aem.connector.core.config.ApiConfigService;
import com.searchstax.aem.connector.core.config.model.ApiConfig;
import com.searchstax.aem.connector.core.testcontext.TestReflection;
import com.searchstax.aem.connector.core.utils.ProtectedValueCodec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SearchStaxConfigurationServiceImplExtendedTest {

    @Mock
    private ApiConfigService apiConfigService;

    @Mock
    private ProtectedValueCodec protectedValueCodec;

    private SearchStaxConfigurationServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new SearchStaxConfigurationServiceImpl();
        TestReflection.inject(service, "apiConfigService", apiConfigService);
        TestReflection.inject(service, "protectedValueCodec", protectedValueCodec);
    }

    @Test
    void exposesAllApiConfigurationValues() {
        final ApiConfig config = populatedConfig();
        when(apiConfigService.getConfiguration()).thenReturn(config);
        when(protectedValueCodec.unprotectIfNeeded(org.mockito.ArgumentMatchers.anyString()))
                .thenAnswer(invocation -> "plain-" + invocation.getArgument(0));

        assertEquals("endpoint", service.getEndpointUrl());
        assertEquals("plain-token", service.getApiToken());
        assertEquals("select", service.getSelectEndpoint());
        assertEquals("plain-select", service.getSelectToken());
        assertEquals("update", service.getUpdateEndpoint());
        assertEquals("plain-update", service.getUpdateToken());
        assertEquals("suggest", service.getAutoSuggestApi());
        assertEquals("related", service.getRelatedSearchesEndpoint());
        assertEquals("popular", service.getPopularSearchesEndpoint());
        assertEquals("plain-discovery", service.getDiscoveryApiKey());
        assertEquals("track", service.getAnalyticsTrackingUrl());
        assertEquals("plain-track", service.getAnalyticsTrackingKey());
        assertEquals("report", service.getAnalyticsReportingUrl());
        assertEquals("plain-report", service.getAnalyticsReportingApiKey());
        assertEquals("forward", service.getForwardGeocodingEndpoint());
        assertEquals("reverse", service.getReverseGeocodingEndpoint());
    }

    private static ApiConfig populatedConfig() {
        final ApiConfig config = new ApiConfig();
        config.setEndpointUrl("endpoint");
        config.setApiToken("token");
        config.setSelectEndpoint("select");
        config.setSelectToken("select");
        config.setUpdateEndpoint("update");
        config.setUpdateToken("update");
        config.setAutoSuggestApi("suggest");
        config.setRelatedSearchesEndpoint("related");
        config.setPopularSearchesEndpoint("popular");
        config.setDiscoveryApiKey("discovery");
        config.setAnalyticsTrackingUrl("track");
        config.setAnalyticsTrackingKey("track");
        config.setAnalyticsReportingUrl("report");
        config.setAnalyticsReportingApiKey("report");
        config.setForwardGeocodingEndpoint("forward");
        config.setReverseGeocodingEndpoint("reverse");
        return config;
    }
}

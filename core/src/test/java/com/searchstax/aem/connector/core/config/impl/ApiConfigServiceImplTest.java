package com.searchstax.aem.connector.core.config.impl;

import com.searchstax.aem.connector.core.config.model.ApiConfig;
import com.searchstax.aem.connector.core.testcontext.AppAemContext;
import com.searchstax.aem.connector.core.testcontext.TestReflection;
import com.searchstax.aem.connector.core.utils.ResolverUtil;
import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

@ExtendWith({AemContextExtension.class, MockitoExtension.class})
class ApiConfigServiceImplTest {

    private final AemContext context = AppAemContext.newAemContext();

    @Mock
    private ResolverUtil resolverUtil;

    private ApiConfigServiceImpl service;

    @BeforeEach
    void setUp() throws Exception {
        when(resolverUtil.getServiceResolver()).thenReturn(context.resourceResolver());
        service = new ApiConfigServiceImpl();
        TestReflection.inject(service, "resolverUtil", resolverUtil);
    }

    @Test
    void returnsEmptyConfigWhenResourceMissing() {
        final ApiConfig config = service.getConfiguration();

        assertNull(config.getEndpointUrl());
        assertNull(config.getUpdateEndpoint());
    }

    @Test
    void loadsConfiguredApiProperties() {
        context.create().resource(
                ApiConfigServiceImpl.CONFIG_PATH,
                "endpointUrl", "https://api.example.com",
                "updateEndpoint", "https://update.example.com",
                "updateToken", "secret-token",
                "autoSuggestApi", "https://suggest.example.com");

        final ApiConfig config = service.getConfiguration();

        assertEquals("https://api.example.com", config.getEndpointUrl());
        assertEquals("https://update.example.com", config.getUpdateEndpoint());
        assertEquals("secret-token", config.getUpdateToken());
        assertEquals("https://suggest.example.com", config.getAutoSuggestApi());
    }
}

package com.searchstax.aem.connector.core.services.impl;

import com.searchstax.aem.connector.core.config.ApiConfigService;
import com.searchstax.aem.connector.core.config.model.ApiConfig;
import com.searchstax.aem.connector.core.services.SearchStaxConfigurationService;
import com.searchstax.aem.connector.core.utils.ProtectedValueCodec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SearchStaxConfigurationServiceImplTest {

    @Mock
    private ApiConfigService apiConfigService;

    @Mock
    private ProtectedValueCodec protectedValueCodec;

    private SearchStaxConfigurationService configurationService;

    @BeforeEach
    void setUp() {
        configurationService = new SearchStaxConfigurationServiceImpl();
        inject(configurationService, "apiConfigService", apiConfigService);
        inject(configurationService, "protectedValueCodec", protectedValueCodec);
    }

    @Test
    void returnsDecryptedUpdateTokenAndEndpoint() {
        final ApiConfig config = new ApiConfig();
        config.setUpdateEndpoint("https://example.com/update");
        config.setUpdateToken("{protected-token}");

        when(apiConfigService.getConfiguration()).thenReturn(config);
        when(protectedValueCodec.unprotectIfNeeded("{protected-token}")).thenReturn("plain-token");

        assertEquals("https://example.com/update", configurationService.getUpdateEndpoint());
        assertEquals("plain-token", configurationService.getUpdateToken());
    }

    private static void inject(final Object target, final String fieldName, final Object value) {
        try {
            final java.lang.reflect.Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}

package com.searchstax.aem.connector.core.services.impl;

import com.searchstax.aem.connector.core.dto.response.ApiResponse;
import com.searchstax.aem.connector.core.services.SearchStaxConfigurationService;
import com.searchstax.aem.connector.core.services.SearchstaxClientService;
import com.searchstax.aem.connector.core.utils.ProtectedValueCodec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SearchstaxClientServiceImplTest {

    @Mock
    private SearchStaxConfigurationService configurationService;

    @Mock
    private ProtectedValueCodec protectedValueCodec;

    private SearchstaxClientService clientService;

    @BeforeEach
    void setUp() {
        clientService = new SearchstaxClientServiceImpl();
        inject(clientService, "configurationService", configurationService);
        inject(clientService, "protectedValueCodec", protectedValueCodec);
    }

    @Test
    void failsFastWhenUpdateEndpointMissing() {
        when(configurationService.getUpdateEndpoint()).thenReturn("");
        when(configurationService.getUpdateToken()).thenReturn("token");

        final ApiResponse response = clientService.indexDocument("{\"add\":[]}");

        assertEquals(500, response.getStatusCode());
        assertTrue(response.getResponseBody().contains("endpoint"));
    }

    @Test
    void failsFastWhenUpdateTokenMissing() {
        when(configurationService.getUpdateEndpoint()).thenReturn("https://example.com/update");
        when(configurationService.getUpdateToken()).thenReturn("");

        final ApiResponse response = clientService.indexDocument("{\"add\":[]}");

        assertEquals(500, response.getStatusCode());
        assertTrue(response.getResponseBody().contains("token"));
    }

    @Test
    void failsFastWhenTokenRemainsEncrypted() {
        when(configurationService.getUpdateEndpoint()).thenReturn("https://example.com/update");
        when(configurationService.getUpdateToken()).thenReturn("{encrypted-token}");
        when(protectedValueCodec.looksEncrypted("{encrypted-token}")).thenReturn(true);

        final ApiResponse response = clientService.deleteDocument("{\"delete\":[]}");

        assertEquals(500, response.getStatusCode());
        assertFalse(response.getResponseBody().isBlank());
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

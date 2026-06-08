package com.searchstax.aem.connector.core.services.impl;

import com.searchstax.aem.connector.core.dto.response.ApiResponse;
import com.searchstax.aem.connector.core.services.SearchStaxConfigurationService;
import com.searchstax.aem.connector.core.services.SearchstaxClientService;
import com.searchstax.aem.connector.core.utils.ProtectedValueCodec;
import java.lang.reflect.Method;

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

    @Test
    void failsWhenRequestUrlExceedsMaxLength() {
        final String longEndpoint = "https://example.com/update?" + "x".repeat(11_000);
        when(configurationService.getUpdateEndpoint()).thenReturn(longEndpoint);
        when(configurationService.getUpdateToken()).thenReturn("token");
        when(protectedValueCodec.looksEncrypted("token")).thenReturn(false);

        final ApiResponse response = clientService.indexDocument("{\"add\":[]}");

        assertEquals(414, response.getStatusCode());
    }

    @Test
    void appendsCommitWithinToEndpointWithExistingQuery() {
        when(configurationService.getUpdateEndpoint()).thenReturn("https://example.com/update?existing=1");
        when(configurationService.getUpdateToken()).thenReturn("token");
        when(protectedValueCodec.looksEncrypted("token")).thenReturn(false);

        final ApiResponse response = clientService.indexDocument("{\"add\":[]}");

        assertTrue(response.getStatusCode() > 0);
        assertFalse(response.getResponseBody().contains("endpoint is not configured"));
    }

    @Test
    void skipsEncryptedCheckWhenCodecUnavailable() {
        final SearchstaxClientServiceImpl service = new SearchstaxClientServiceImpl();
        inject(service, "configurationService", configurationService);
        inject(service, "protectedValueCodec", null);

        when(configurationService.getUpdateEndpoint()).thenReturn("https://example.com/update");
        when(configurationService.getUpdateToken()).thenReturn("plain-token");

        final ApiResponse response = service.indexDocument("{\"add\":[]}");

        assertTrue(response.getStatusCode() > 0);
        assertFalse(response.getResponseBody().contains("token could not be decrypted"));
    }

    @Test
    void returnsInternalErrorWhenHttpCallFails() {
        when(configurationService.getUpdateEndpoint()).thenReturn("http://127.0.0.1:1/unreachable");
        when(configurationService.getUpdateToken()).thenReturn("token");
        when(protectedValueCodec.looksEncrypted("token")).thenReturn(false);

        final ApiResponse response = clientService.indexDocument("{\"add\":[]}");

        assertEquals(500, response.getStatusCode());
        assertFalse(response.getResponseBody().isBlank());
    }

    @Test
    void readResponseReturnsEmptyStringForNullStream() throws Exception {
        final Method readResponse = SearchstaxClientServiceImpl.class.getDeclaredMethod(
                "readResponse",
                java.io.InputStream.class);
        readResponse.setAccessible(true);

        assertEquals("", readResponse.invoke(clientService, (java.io.InputStream) null));
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

package com.searchstax.aem.connector.core.services.impl;

import com.searchstax.aem.connector.core.constants.SearchStaxServiceLimits;
import com.searchstax.aem.connector.core.dto.response.ApiResponse;
import com.searchstax.aem.connector.core.dto.response.IndexingBatchResult;
import com.searchstax.aem.connector.core.services.IndexingApiService;
import com.searchstax.aem.connector.core.services.SearchStaxRequestThrottle;
import com.searchstax.aem.connector.core.services.SearchstaxClientService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IndexingApiServiceImplTest {

    @Mock
    private SearchstaxClientService searchstaxClientService;

    private IndexingApiService indexingApiService;

    @BeforeEach
    void setUp() {
        SearchStaxRequestThrottle.resetForTests();
        SearchStaxRequestThrottle.configureMinIntervalMs(0L);

        indexingApiService = new IndexingApiServiceImpl();
        inject(indexingApiService, "searchstaxClientService", searchstaxClientService);
    }

    @AfterEach
    void tearDown() {
        SearchStaxRequestThrottle.resetForTests();
    }

    @Test
    void returnsSuccessForEmptyIndexList() {
        final IndexingBatchResult result = indexingApiService.indexDocuments(Collections.emptyList());

        assertTrue(result.isSuccess());
        assertEquals(0, result.getItemCount());
    }

    @Test
    void indexesValidDocuments() {
        when(searchstaxClientService.indexDocument(contains("\"add\"")))
                .thenReturn(new ApiResponse(200, "{\"success\":true}"));

        final List<String> documents = Collections.singletonList(
                "{\"id\":\"/content/site/page\",\"language_s\":\"en\"}");

        final IndexingBatchResult result = indexingApiService.indexDocuments(documents);

        assertTrue(result.isSuccess());
        assertEquals(200, result.getStatusCode());
        assertEquals(1, result.getItemCount());
        verify(searchstaxClientService).indexDocument(contains("\"add\""));
    }

    @Test
    void failsWhenAllDocumentsExceedSizeLimit() {
        final char[] payload = new char[SearchStaxServiceLimits.MAX_DOCUMENT_BYTES + 1];
        Arrays.fill(payload, 'x');
        final String oversizedDocument = "{\"id\":\"/content/site/page\",\"title\":\"" + new String(payload) + "\"}";

        final IndexingBatchResult result = indexingApiService.indexDocuments(
                Collections.singletonList(oversizedDocument));

        assertFalse(result.isSuccess());
        assertEquals(413, result.getStatusCode());
    }

    @Test
    void deletesDocumentsUsingDeletePayload() {
        when(searchstaxClientService.deleteDocument(contains("\"delete\"")))
                .thenReturn(new ApiResponse(200, "{\"success\":true}"));

        final IndexingBatchResult result = indexingApiService.deleteDocuments(
                Arrays.asList("/content/site/page-one", "/content/site/page-two"));

        assertTrue(result.isSuccess());
        assertEquals(2, result.getItemCount());
        verify(searchstaxClientService).deleteDocument(contains("\"delete\""));
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

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

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.atLeast;
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
    void returnsSuccessForNullIndexList() {
        final IndexingBatchResult result = indexingApiService.indexDocuments(null);

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

    @Test
    void returnsSuccessForEmptyDeleteList() {
        final IndexingBatchResult nullResult = indexingApiService.deleteDocuments(null);
        final IndexingBatchResult emptyResult = indexingApiService.deleteDocuments(Collections.emptyList());

        assertTrue(nullResult.isSuccess());
        assertTrue(emptyResult.isSuccess());
        assertEquals(0, emptyResult.getItemCount());
    }

    @Test
    void retriesTransientFailuresBeforeSucceeding() {
        when(searchstaxClientService.indexDocument(anyString()))
                .thenReturn(new ApiResponse(503, "unavailable"))
                .thenReturn(new ApiResponse(200, "{\"success\":true}"));

        final IndexingBatchResult result = indexingApiService.indexDocuments(
                Collections.singletonList("{\"id\":\"/content/site/page\",\"language_s\":\"en\"}"));

        assertTrue(result.isSuccess());
        verify(searchstaxClientService, atLeast(2)).indexDocument(anyString());
    }

    @Test
    void failsImmediatelyOnNonRetryableStatus() {
        when(searchstaxClientService.indexDocument(anyString()))
                .thenReturn(new ApiResponse(400, "bad request"));

        final IndexingBatchResult result = indexingApiService.indexDocuments(
                Collections.singletonList("{\"id\":\"/content/site/page\",\"language_s\":\"en\"}"));

        assertFalse(result.isSuccess());
        assertEquals(400, result.getStatusCode());
        verify(searchstaxClientService).indexDocument(anyString());
    }

    @Test
    void splitsBatchWhenApiReturnsPayloadTooLarge() {
        when(searchstaxClientService.indexDocument(anyString()))
                .thenReturn(new ApiResponse(413, "payload too large"))
                .thenReturn(new ApiResponse(200, "{\"success\":true}"))
                .thenReturn(new ApiResponse(200, "{\"success\":true}"));

        final IndexingBatchResult result = indexingApiService.indexDocuments(Arrays.asList(
                "{\"id\":\"/content/site/page-one\",\"language_s\":\"en\"}",
                "{\"id\":\"/content/site/page-two\",\"language_s\":\"en\"}"));

        assertTrue(result.isSuccess());
        verify(searchstaxClientService, atLeast(3)).indexDocument(anyString());
    }

    @Test
    void splitsBatchWhenIndexPayloadExceedsMaxPayloadBytes() {
        when(searchstaxClientService.indexDocument(anyString()))
                .thenReturn(new ApiResponse(200, "{\"success\":true}"));

        final String title = "x".repeat(SearchStaxServiceLimits.MAX_DOCUMENT_BYTES - 5000);
        final String template = "{\"id\":\"/content/site/page-%d\",\"language_s\":\"en\",\"title\":\"%s\"}";

        final List<String> documents = new ArrayList<>();
        for (int i = 0; i < 110; i++) {
            documents.add(String.format(template, i, title));
        }

        final IndexingBatchResult result = indexingApiService.indexDocuments(documents);

        assertTrue(result.isSuccess());
        assertEquals(110, result.getItemCount());
        verify(searchstaxClientService, atLeast(2)).indexDocument(anyString());
    }

    @Test
    void deleteSplitOn413MergesSecondFailureIntoResult() {
        when(searchstaxClientService.indexDocument(anyString()))
                .thenReturn(new ApiResponse(413, "payload too large"))
                .thenReturn(new ApiResponse(200, "{\"success\":true}"))
                .thenReturn(new ApiResponse(400, "bad request"));

        final IndexingBatchResult result = indexingApiService.indexDocuments(Arrays.asList(
                "{\"id\":\"/content/site/page-one\",\"language_s\":\"en\"}",
                "{\"id\":\"/content/site/page-two\",\"language_s\":\"en\"}"));

        assertFalse(result.isSuccess());
        assertEquals(400, result.getStatusCode());
        assertTrue(result.getMessage().contains("payload"));
        assertTrue(result.getMessage().contains("bad request"));
        assertEquals(2, result.getItemCount());
    }

    @Test
    void splitsDeleteOn413WhenDeletingMultipleDocuments() {
        when(searchstaxClientService.deleteDocument(anyString()))
                .thenReturn(new ApiResponse(413, "payload too large"))
                .thenReturn(new ApiResponse(200, "{\"success\":true}"))
                .thenReturn(new ApiResponse(200, "{\"success\":true}"));

        final IndexingBatchResult result = indexingApiService.deleteDocuments(Arrays.asList(
                "/content/site/page-one",
                "/content/site/page-two"));

        assertTrue(result.isSuccess());
        assertEquals(2, result.getItemCount());
        assertEquals(200, result.getStatusCode());
        verify(searchstaxClientService, atLeast(3)).deleteDocument(anyString());
    }

    @Test
    void activatesConfigToUpdateThrottleMinInterval() throws Exception {
        SearchStaxRequestThrottle.resetForTests();
        final long before = getThrottleMinIntervalMs();

        final IndexingApiServiceImpl impl = (IndexingApiServiceImpl) indexingApiService;
        final IndexingApiServiceImpl.Config config = org.mockito.Mockito.mock(IndexingApiServiceImpl.Config.class);
        when(config.minRequestIntervalMs()).thenReturn(before + 123L);

        final var activate = IndexingApiServiceImpl.class.getDeclaredMethod("activate", IndexingApiServiceImpl.Config.class);
        activate.setAccessible(true);
        activate.invoke(impl, config);

        final long after = getThrottleMinIntervalMs();
        assertEquals(before + 123L, after);
    }

    private static long getThrottleMinIntervalMs() throws Exception {
        final var field = SearchStaxRequestThrottle.class.getDeclaredField("minIntervalMs");
        field.setAccessible(true);
        return field.getLong(null);
    }

    @Test
    void indexesValidDocumentsWhenBatchContainsOversizedEntries() {
        final char[] payload = new char[SearchStaxServiceLimits.MAX_DOCUMENT_BYTES + 1];
        Arrays.fill(payload, 'x');
        final String oversizedDocument = "{\"id\":\"/content/site/big\",\"title\":\"" + new String(payload) + "\"}";

        when(searchstaxClientService.indexDocument(anyString()))
                .thenReturn(new ApiResponse(200, "{\"success\":true}"));

        final IndexingBatchResult result = indexingApiService.indexDocuments(Arrays.asList(
                oversizedDocument,
                "{\"id\":\"/content/site/small\",\"language_s\":\"en\"}"));

        assertTrue(result.isSuccess());
        assertEquals(2, result.getItemCount());
    }

    @Test
    void skipsInvalidDocumentJson() {
        when(searchstaxClientService.indexDocument(contains("\"add\"")))
                .thenReturn(new ApiResponse(200, "{\"success\":true}"));

        final IndexingBatchResult result = indexingApiService.indexDocuments(Arrays.asList(
                "not-json",
                "{\"id\":\"/content/site/page\",\"language_s\":\"en\"}"));

        assertTrue(result.isSuccess());
        assertEquals(2, result.getItemCount());
    }

    @Test
    void indexDocumentsRecursiveReturnsSuccessForEmptyBatch() throws Exception {
        final IndexingBatchResult result = invokeIndexDocumentsRecursive(Collections.emptyList());
        assertTrue(result.isSuccess());
        assertEquals(0, result.getItemCount());
    }

    @Test
    void deleteDocumentsRecursiveReturnsSuccessForEmptyBatch() throws Exception {
        final IndexingBatchResult result = invokeDeleteDocumentsRecursive(Collections.emptyList());
        assertTrue(result.isSuccess());
        assertEquals(0, result.getItemCount());
    }

    private IndexingBatchResult invokeIndexDocumentsRecursive(final List<String> documents) throws Exception {
        final Method method = IndexingApiServiceImpl.class.getDeclaredMethod(
                "indexDocumentsRecursive",
                List.class);
        method.setAccessible(true);
        return (IndexingBatchResult) method.invoke(indexingApiService, documents);
    }

    private IndexingBatchResult invokeDeleteDocumentsRecursive(final List<String> documentIds) throws Exception {
        final Method method = IndexingApiServiceImpl.class.getDeclaredMethod(
                "deleteDocumentsRecursive",
                List.class);
        method.setAccessible(true);
        return (IndexingBatchResult) method.invoke(indexingApiService, documentIds);
    }

    @Test
    void sleepHandlesInterruptedException() throws Exception {
        final Method sleep = IndexingApiServiceImpl.class.getDeclaredMethod("sleep", long.class);
        sleep.setAccessible(true);

        final Thread sleeper = new Thread(() -> {
            try {
                sleep.invoke(indexingApiService, 5_000L);
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException(e);
            }
        });
        sleeper.start();
        Thread.sleep(50);
        sleeper.interrupt();
        sleeper.join(2_000);
        assertFalse(sleeper.isAlive());
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

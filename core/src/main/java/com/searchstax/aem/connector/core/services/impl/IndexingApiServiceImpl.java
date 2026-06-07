package com.searchstax.aem.connector.core.services.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.searchstax.aem.connector.core.constants.IncrementalIndexingDefaults;
import com.searchstax.aem.connector.core.constants.SearchStaxServiceLimits;
import com.searchstax.aem.connector.core.dto.response.ApiResponse;
import com.searchstax.aem.connector.core.dto.response.IndexingBatchResult;
import com.searchstax.aem.connector.core.services.IndexingApiService;
import com.searchstax.aem.connector.core.services.SearchStaxApiErrorPolicy;
import com.searchstax.aem.connector.core.services.SearchStaxRequestThrottle;
import com.searchstax.aem.connector.core.services.SearchstaxClientService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Component(service = IndexingApiService.class)
@Designate(ocd = IndexingApiServiceImpl.Config.class)
public class IndexingApiServiceImpl implements IndexingApiService {

    private static final Logger LOG = LoggerFactory.getLogger(IndexingApiServiceImpl.class);

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Reference
    private SearchstaxClientService searchstaxClientService;

    @Activate
    protected void activate(final Config config) {
        SearchStaxRequestThrottle.configureMinIntervalMs(config.minRequestIntervalMs());
    }

    @Override
    public IndexingBatchResult indexDocuments(final List<String> documentJsonBodies) {
        if (documentJsonBodies == null || documentJsonBodies.isEmpty()) {
            return successResult(0, 0L, "No documents to index");
        }

        final long started = System.currentTimeMillis();
        final IndexingBatchResult result = indexDocumentsRecursive(new ArrayList<>(documentJsonBodies));
        final long duration = System.currentTimeMillis() - started;

        LOG.info("{} Index API completed items={} success={} status={} durationMs={}",
                IncrementalIndexingDefaults.LOG_PREFIX,
                result.getItemCount(),
                result.isSuccess(),
                result.getStatusCode(),
                duration);

        return new IndexingBatchResult(
                result.isSuccess(),
                result.getStatusCode(),
                result.getMessage(),
                documentJsonBodies.size(),
                duration);
    }

    @Override
    public IndexingBatchResult deleteDocuments(final List<String> documentIds) {
        if (documentIds == null || documentIds.isEmpty()) {
            return successResult(0, 0L, "No documents to delete");
        }

        final long started = System.currentTimeMillis();
        final IndexingBatchResult result = deleteDocumentsRecursive(new ArrayList<>(documentIds));
        final long duration = System.currentTimeMillis() - started;

        LOG.info("{} Delete API completed items={} success={} status={} durationMs={}",
                IncrementalIndexingDefaults.LOG_PREFIX,
                result.getItemCount(),
                result.isSuccess(),
                result.getStatusCode(),
                duration);

        return new IndexingBatchResult(
                result.isSuccess(),
                result.getStatusCode(),
                result.getMessage(),
                documentIds.size(),
                duration);
    }

    private IndexingBatchResult indexDocumentsRecursive(final List<String> documentJsonBodies) {
        if (documentJsonBodies.isEmpty()) {
            return successResult(0, 0L, "No documents to index");
        }

        final List<String> validDocuments = new ArrayList<>();
        for (final String documentJson : documentJsonBodies) {
            final int documentBytes = documentJson.getBytes(StandardCharsets.UTF_8).length;
            if (documentBytes > SearchStaxServiceLimits.MAX_DOCUMENT_BYTES) {
                LOG.warn("{} Document exceeds SearchStax 100 KB limit ({} bytes); skipping",
                        IncrementalIndexingDefaults.LOG_PREFIX, documentBytes);
                continue;
            }
            validDocuments.add(documentJson);
        }

        if (validDocuments.isEmpty()) {
            return failureResult(
                    413,
                    SearchStaxApiErrorPolicy.resolveGuidanceMessage(413, null),
                    documentJsonBodies.size(),
                    0L);
        }

        final String payload = buildIndexPayload(validDocuments);
        final int payloadBytes = payload.getBytes(StandardCharsets.UTF_8).length;
        if (payloadBytes > SearchStaxServiceLimits.MAX_PAYLOAD_BYTES) {
            if (validDocuments.size() == 1) {
                return failureResult(
                        413,
                        SearchStaxApiErrorPolicy.resolveGuidanceMessage(413, null),
                        1,
                        0L);
            }
            return splitAndIndex(validDocuments);
        }

        final ApiResponse response = executeWithRetry(payload, true);
        if (SearchStaxApiErrorPolicy.shouldSplitBatch(response.getStatusCode()) && validDocuments.size() > 1) {
            LOG.warn("{} Received HTTP 413 for batch size={}; splitting and retrying",
                    IncrementalIndexingDefaults.LOG_PREFIX, validDocuments.size());
            return splitAndIndex(validDocuments);
        }

        return toBatchResult(response, validDocuments.size(), 0L, "Indexed documents");
    }

    private IndexingBatchResult splitAndIndex(final List<String> documents) {
        final int midpoint = documents.size() / 2;
        final IndexingBatchResult first = indexDocumentsRecursive(documents.subList(0, midpoint));
        final IndexingBatchResult second = indexDocumentsRecursive(documents.subList(midpoint, documents.size()));
        return mergeResults(first, second, documents.size());
    }

    private IndexingBatchResult deleteDocumentsRecursive(final List<String> documentIds) {
        if (documentIds.isEmpty()) {
            return successResult(0, 0L, "No documents to delete");
        }

        final String payload = buildDeletePayload(documentIds);
        final int payloadBytes = payload.getBytes(StandardCharsets.UTF_8).length;
        if (payloadBytes > SearchStaxServiceLimits.MAX_PAYLOAD_BYTES && documentIds.size() > 1) {
            final int midpoint = documentIds.size() / 2;
            final IndexingBatchResult first = deleteDocumentsRecursive(documentIds.subList(0, midpoint));
            final IndexingBatchResult second = deleteDocumentsRecursive(documentIds.subList(midpoint, documentIds.size()));
            return mergeResults(first, second, documentIds.size());
        }

        final ApiResponse response = executeWithRetry(payload, false);
        if (SearchStaxApiErrorPolicy.shouldSplitBatch(response.getStatusCode()) && documentIds.size() > 1) {
            final int midpoint = documentIds.size() / 2;
            final IndexingBatchResult first = deleteDocumentsRecursive(documentIds.subList(0, midpoint));
            final IndexingBatchResult second = deleteDocumentsRecursive(documentIds.subList(midpoint, documentIds.size()));
            return mergeResults(first, second, documentIds.size());
        }

        return toBatchResult(response, documentIds.size(), 0L, "Deleted documents");
    }

    private IndexingBatchResult mergeResults(
            final IndexingBatchResult first,
            final IndexingBatchResult second,
            final int totalItems) {

        final boolean success = first.isSuccess() && second.isSuccess();
        final int statusCode = success
                ? first.getStatusCode()
                : (first.isSuccess() ? second.getStatusCode() : first.getStatusCode());
        final String message = success
                ? first.getMessage()
                : (!first.isSuccess() ? first.getMessage() : second.getMessage());

        return new IndexingBatchResult(success, statusCode, message, totalItems, 0L);
    }

    private ApiResponse executeWithRetry(final String payload, final boolean indexOperation) {
        ApiResponse lastResponse = null;

        for (int attempt = 1; attempt <= SearchStaxApiErrorPolicy.MAX_POST_ATTEMPTS; attempt++) {
            SearchStaxRequestThrottle.awaitTurn();
            lastResponse = indexOperation
                    ? searchstaxClientService.indexDocument(payload)
                    : searchstaxClientService.deleteDocument(payload);

            final int statusCode = lastResponse.getStatusCode();
            if (SearchStaxApiErrorPolicy.isSuccessfulResponse(statusCode, lastResponse.getResponseBody())) {
                return lastResponse;
            }

            if (SearchStaxApiErrorPolicy.isNonRetryable(statusCode)
                    || SearchStaxApiErrorPolicy.shouldSplitBatch(statusCode)
                    || !SearchStaxApiErrorPolicy.isRetryable(statusCode)
                    || attempt == SearchStaxApiErrorPolicy.MAX_POST_ATTEMPTS) {
                LOG.error("{} SearchStax API failure status={} attempt={} guidance={}",
                        IncrementalIndexingDefaults.LOG_PREFIX,
                        statusCode,
                        attempt,
                        SearchStaxApiErrorPolicy.resolveGuidanceMessage(statusCode, lastResponse.getResponseBody()));
                return lastResponse;
            }

            final long backoff = SearchStaxApiErrorPolicy.backoffMillis(attempt);
            LOG.warn("{} Transient SearchStax API failure status={} attempt={} retrying in {}ms",
                    IncrementalIndexingDefaults.LOG_PREFIX, statusCode, attempt, backoff);
            sleep(backoff);
        }

        return lastResponse;
    }

    private String buildIndexPayload(final List<String> documentJsonBodies) {
        final ArrayNode documents = objectMapper.createArrayNode();
        for (final String documentJson : documentJsonBodies) {
            try {
                documents.add(objectMapper.readTree(documentJson));
            } catch (Exception e) {
                LOG.warn("{} Skipping invalid document JSON", IncrementalIndexingDefaults.LOG_PREFIX, e);
            }
        }

        final ObjectNode root = objectMapper.createObjectNode();
        root.set("add", documents);
        return root.toString();
    }

    private String buildDeletePayload(final List<String> documentIds) {
        final ArrayNode ids = objectMapper.createArrayNode();
        for (final String id : documentIds) {
            ids.add(id);
        }

        final ObjectNode root = objectMapper.createObjectNode();
        root.set("delete", ids);
        return root.toString();
    }

    private IndexingBatchResult toBatchResult(
            final ApiResponse response,
            final int itemCount,
            final long durationMs,
            final String successMessage) {

        final int statusCode = response.getStatusCode();
        final boolean success = SearchStaxApiErrorPolicy.isSuccessfulResponse(
                statusCode,
                response.getResponseBody());
        final String message = success
                ? successMessage
                : SearchStaxApiErrorPolicy.resolveGuidanceMessage(statusCode, response.getResponseBody());
        return new IndexingBatchResult(success, statusCode, message, itemCount, durationMs);
    }

    private IndexingBatchResult successResult(final int itemCount, final long durationMs, final String message) {
        return new IndexingBatchResult(true, 200, message, itemCount, durationMs);
    }

    private IndexingBatchResult failureResult(
            final int statusCode,
            final String message,
            final int itemCount,
            final long durationMs) {
        return new IndexingBatchResult(false, statusCode, message, itemCount, durationMs);
    }

    private void sleep(final long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @ObjectClassDefinition(name = "SearchStax Indexing API Service")
    public @interface Config {

        @AttributeDefinition(
                name = "Minimum interval between SearchStax API requests (ms)",
                description = "Helps stay under SearchStax rate limits (20+ req/sec production, 5 req/sec sandbox).")
        long minRequestIntervalMs() default SearchStaxServiceLimits.DEFAULT_MIN_REQUEST_INTERVAL_MS;
    }
}

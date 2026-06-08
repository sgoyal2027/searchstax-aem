package com.searchstax.aem.connector.core.jobs;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.searchstax.aem.connector.core.constants.IncrementalIndexingDefaults;
import com.searchstax.aem.connector.core.constants.SearchStaxFullIndexDefaults;
import com.searchstax.aem.connector.core.dto.response.IndexingBatchResult;
import com.searchstax.aem.connector.core.incremental.IndexingAction;
import com.searchstax.aem.connector.core.incremental.IndexingScopeDecision;
import com.searchstax.aem.connector.core.services.IndexingApiService;
import com.searchstax.aem.connector.core.services.IndexingAuditService;
import com.searchstax.aem.connector.core.services.IndexingFailureNotificationService;
import com.searchstax.aem.connector.core.services.IndexingScopeService;
import com.searchstax.aem.connector.core.services.SearchStaxDocumentBuilderService;
import com.searchstax.aem.connector.core.testcontext.AppAemContext;
import com.searchstax.aem.connector.core.utils.ResolverUtil;
import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;
import org.apache.sling.event.jobs.Job;
import org.apache.sling.event.jobs.consumer.JobConsumer.JobResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.ArgumentCaptor;

import org.apache.sling.api.resource.ResourceResolver;

import java.util.Optional;
import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

@ExtendWith({AemContextExtension.class, MockitoExtension.class})
class IncrementalIndexingJobConsumerTest {

    private static final String PAGE_PATH = "/content/wknd/us/en/page";

    private final AemContext context = AppAemContext.newAemContext();

    @Mock
    private ResolverUtil resolverUtil;

    @Mock
    private IndexingScopeService indexingScopeService;

    @Mock
    private SearchStaxDocumentBuilderService documentBuilderService;

    @Mock
    private IndexingApiService indexingApiService;

    @Mock
    private IndexingAuditService indexingAuditService;

    @Mock
    private IndexingFailureNotificationService failureNotificationService;

    @Mock
    private Job job;

    private IncrementalIndexingJobConsumer jobConsumer;

    @BeforeEach
    void setUp() throws Exception {
        lenient().when(resolverUtil.getServiceResolver()).thenReturn(context.resourceResolver());

        jobConsumer = new IncrementalIndexingJobConsumer();
        context.registerService(ResolverUtil.class, resolverUtil);
        context.registerService(IndexingScopeService.class, indexingScopeService);
        context.registerService(SearchStaxDocumentBuilderService.class, documentBuilderService);
        context.registerService(IndexingApiService.class, indexingApiService);
        context.registerService(IndexingAuditService.class, indexingAuditService);
        context.registerService(IndexingFailureNotificationService.class, failureNotificationService);
        context.registerInjectActivateService(jobConsumer);
    }

    @Test
    void skipsJobWhenConnectorDisabled() {
        when(indexingScopeService.isConnectorEnabled()).thenReturn(false);

        assertEquals(JobResult.OK, jobConsumer.process(job));

        verify(indexingApiService, never()).indexDocuments(anyList());
    }

    @Test
    void indexesInScopePathsAndRecordsSuccess() throws Exception {
        final ObjectNode document = new com.fasterxml.jackson.databind.ObjectMapper().createObjectNode();
        document.put("id", PAGE_PATH);
        document.put("language_s", "en");

        when(indexingScopeService.isConnectorEnabled()).thenReturn(true);
        when(job.getProperty(IncrementalIndexingDefaults.JOB_PROP_BATCH_ID, String.class)).thenReturn("batch-1");
        when(job.getProperty(IncrementalIndexingDefaults.JOB_PROP_INDEX_PATHS, String[].class))
                .thenReturn(new String[] {PAGE_PATH});
        when(job.getProperty(IncrementalIndexingDefaults.JOB_PROP_DELETE_PATHS, String[].class)).thenReturn(null);
        when(indexingScopeService.evaluate(any(), eq(PAGE_PATH))).thenReturn(IndexingScopeDecision.accept());
        when(documentBuilderService.buildDocument(any(), eq(PAGE_PATH))).thenReturn(Optional.of(document));
        when(documentBuilderService.resolveDocumentId(PAGE_PATH)).thenReturn(PAGE_PATH);
        when(indexingApiService.indexDocuments(anyList()))
                .thenReturn(new IndexingBatchResult(true, 200, "Indexed documents", 1, 10L));

        assertEquals(JobResult.OK, jobConsumer.process(job));

        verify(indexingApiService).indexDocuments(anyList());
        verify(indexingAuditService).record(
                eq(PAGE_PATH),
                eq(IndexingAction.INDEX),
                eq("SUCCESS"),
                eq("batch-1"),
                eq(200),
                anyString(),
                any(Long.class),
                eq(PAGE_PATH));
        verify(failureNotificationService, never()).notifyBatchFailure(anyString(), any(), any(), anyList());
    }

    @Test
    void recordsSkippedPathsOutsideScope() throws Exception {
        when(indexingScopeService.isConnectorEnabled()).thenReturn(true);
        when(job.getProperty(IncrementalIndexingDefaults.JOB_PROP_BATCH_ID, String.class)).thenReturn("batch-2");
        when(job.getProperty(IncrementalIndexingDefaults.JOB_PROP_INDEX_PATHS, String[].class))
                .thenReturn(new String[] {PAGE_PATH});
        when(job.getProperty(IncrementalIndexingDefaults.JOB_PROP_DELETE_PATHS, String[].class)).thenReturn(null);
        when(indexingScopeService.evaluate(any(), eq(PAGE_PATH)))
                .thenReturn(IndexingScopeDecision.reject("outside configured root paths"));
        when(documentBuilderService.resolveDocumentId(PAGE_PATH)).thenReturn(PAGE_PATH);

        jobConsumer.process(job);

        verify(indexingApiService, never()).indexDocuments(anyList());
        verify(indexingAuditService).record(
                eq(PAGE_PATH),
                eq(IndexingAction.INDEX),
                eq("SKIPPED"),
                eq("scope"),
                eq(0),
                eq("outside configured root paths"),
                eq(0L),
                eq(PAGE_PATH));
    }

    @Test
    void recordsFailureWhenDocumentCannotBeBuilt() throws Exception {
        when(indexingScopeService.isConnectorEnabled()).thenReturn(true);
        when(job.getProperty(IncrementalIndexingDefaults.JOB_PROP_BATCH_ID, String.class)).thenReturn("batch-build-fail");
        when(job.getProperty(IncrementalIndexingDefaults.JOB_PROP_INDEX_PATHS, String[].class))
                .thenReturn(new String[] {PAGE_PATH});
        when(job.getProperty(IncrementalIndexingDefaults.JOB_PROP_DELETE_PATHS, String[].class)).thenReturn(null);
        when(indexingScopeService.evaluate(any(), eq(PAGE_PATH))).thenReturn(IndexingScopeDecision.accept());
        when(documentBuilderService.buildDocument(any(), eq(PAGE_PATH))).thenReturn(Optional.empty());
        when(documentBuilderService.resolveDocumentId(PAGE_PATH)).thenReturn(PAGE_PATH);

        jobConsumer.process(job);

        verify(indexingApiService, never()).indexDocuments(anyList());
        verify(indexingAuditService).record(
                eq(PAGE_PATH),
                eq(IndexingAction.INDEX),
                eq("FAILURE"),
                eq("batch-build-fail"),
                eq(0),
                anyString(),
                eq(0L),
                eq(PAGE_PATH));
    }

    @Test
    void notifiesOnIndexBatchFailure() throws Exception {
        final ObjectNode document = new com.fasterxml.jackson.databind.ObjectMapper().createObjectNode();
        document.put("id", PAGE_PATH);

        when(indexingScopeService.isConnectorEnabled()).thenReturn(true);
        when(job.getProperty(IncrementalIndexingDefaults.JOB_PROP_BATCH_ID, String.class)).thenReturn("batch-index-fail");
        when(job.getProperty(IncrementalIndexingDefaults.JOB_PROP_INDEX_PATHS, String[].class))
                .thenReturn(new String[] {PAGE_PATH});
        when(job.getProperty(IncrementalIndexingDefaults.JOB_PROP_DELETE_PATHS, String[].class)).thenReturn(null);
        when(indexingScopeService.evaluate(any(), eq(PAGE_PATH))).thenReturn(IndexingScopeDecision.accept());
        when(documentBuilderService.buildDocument(any(), eq(PAGE_PATH))).thenReturn(Optional.of(document));
        when(documentBuilderService.resolveDocumentId(PAGE_PATH)).thenReturn(PAGE_PATH);
        when(indexingApiService.indexDocuments(anyList()))
                .thenReturn(new IndexingBatchResult(false, 500, "Server error", 1, 5L));

        jobConsumer.process(job);

        verify(failureNotificationService).notifyBatchFailure(
                eq("batch-index-fail"),
                eq(IndexingAction.INDEX),
                any(IndexingBatchResult.class),
                anyList());
    }

    @Test
    void deletesDocumentsAndNotifiesOnFailure() throws Exception {
        when(indexingScopeService.isConnectorEnabled()).thenReturn(true);
        when(job.getProperty(IncrementalIndexingDefaults.JOB_PROP_BATCH_ID, String.class)).thenReturn("batch-3");
        when(job.getProperty(IncrementalIndexingDefaults.JOB_PROP_INDEX_PATHS, String[].class)).thenReturn(null);
        when(job.getProperty(IncrementalIndexingDefaults.JOB_PROP_DELETE_PATHS, String[].class))
                .thenReturn(new String[] {PAGE_PATH});
        when(documentBuilderService.resolveDocumentId(PAGE_PATH)).thenReturn(PAGE_PATH);
        when(indexingApiService.deleteDocuments(anyList()))
                .thenReturn(new IndexingBatchResult(false, 500, "Server error", 1, 5L));

        jobConsumer.process(job);

        verify(indexingApiService).deleteDocuments(anyList());
        verify(failureNotificationService).notifyBatchFailure(
                eq("batch-3"),
                eq(IndexingAction.DELETE),
                any(IndexingBatchResult.class),
                anyList());
    }

    @Test
    void flushesIndexBatchWhenBatchSizeReached() throws Exception {
        when(indexingScopeService.isConnectorEnabled()).thenReturn(true);
        when(job.getProperty(IncrementalIndexingDefaults.JOB_PROP_BATCH_ID, String.class)).thenReturn("batch-flush");

        final String[] indexPaths = IntStream.range(0, SearchStaxFullIndexDefaults.BATCH_SIZE)
                .mapToObj(i -> "/content/site/page-" + i)
                .toArray(String[]::new);

        when(job.getProperty(IncrementalIndexingDefaults.JOB_PROP_INDEX_PATHS, String[].class))
                .thenReturn(indexPaths);
        when(job.getProperty(IncrementalIndexingDefaults.JOB_PROP_DELETE_PATHS, String[].class))
                .thenReturn(null);

        when(indexingScopeService.evaluate(any(), anyString())).thenReturn(IndexingScopeDecision.accept());

        final ObjectNode document = new com.fasterxml.jackson.databind.ObjectMapper().createObjectNode();
        document.put("id", PAGE_PATH);
        document.put("language_s", "en");
        when(documentBuilderService.buildDocument(any(), anyString())).thenReturn(Optional.of(document));

        when(documentBuilderService.resolveDocumentId(anyString()))
                .thenAnswer(inv -> inv.getArgument(0, String.class));

        when(indexingApiService.indexDocuments(anyList()))
                .thenReturn(new IndexingBatchResult(true, 200, "Indexed documents", SearchStaxFullIndexDefaults.BATCH_SIZE, 1000L));

        jobConsumer.process(job);

        verify(indexingApiService, times(1)).indexDocuments(anyList());
        verify(indexingAuditService, times(SearchStaxFullIndexDefaults.BATCH_SIZE)).record(
                anyString(),
                eq(IndexingAction.INDEX),
                eq("SUCCESS"),
                eq("batch-flush"),
                eq(200),
                eq("Indexed documents"),
                eq(10L),
                anyString());
        verify(failureNotificationService, never()).notifyBatchFailure(anyString(), any(), any(), anyList());
    }

    @Test
    void notifiesOnDeleteBatchFailureWhenBatchSizeReached() throws Exception {
        when(indexingScopeService.isConnectorEnabled()).thenReturn(true);
        when(job.getProperty(IncrementalIndexingDefaults.JOB_PROP_BATCH_ID, String.class)).thenReturn("batch-delete-fail");

        when(job.getProperty(IncrementalIndexingDefaults.JOB_PROP_INDEX_PATHS, String[].class)).thenReturn(null);

        final String[] deletePaths = IntStream.range(0, SearchStaxFullIndexDefaults.BATCH_SIZE)
                .mapToObj(i -> "/content/site/delete-" + i)
                .toArray(String[]::new);
        when(job.getProperty(IncrementalIndexingDefaults.JOB_PROP_DELETE_PATHS, String[].class)).thenReturn(deletePaths);

        when(documentBuilderService.resolveDocumentId(anyString()))
                .thenAnswer(inv -> inv.getArgument(0, String.class));

        when(indexingApiService.deleteDocuments(anyList()))
                .thenReturn(new IndexingBatchResult(false, 500, "Server error", SearchStaxFullIndexDefaults.BATCH_SIZE, 1000L));

        final java.util.concurrent.atomic.AtomicReference<List<String>> capturedPaths =
                new java.util.concurrent.atomic.AtomicReference<>();
        org.mockito.Mockito.doAnswer(invocation -> {
                    capturedPaths.set(new java.util.ArrayList<>(invocation.getArgument(3)));
                    return null;
                })
                .when(failureNotificationService)
                .notifyBatchFailure(
                        eq("batch-delete-fail"),
                        eq(IndexingAction.DELETE),
                        any(IndexingBatchResult.class),
                        anyList());

        jobConsumer.process(job);

        verify(indexingApiService, times(1)).deleteDocuments(anyList());
        verify(failureNotificationService).notifyBatchFailure(
                eq("batch-delete-fail"),
                eq(IndexingAction.DELETE),
                any(IndexingBatchResult.class),
                anyList());

        assertEquals(SearchStaxFullIndexDefaults.BATCH_SIZE, capturedPaths.get().size());

        verify(indexingAuditService, times(SearchStaxFullIndexDefaults.BATCH_SIZE)).record(
                anyString(),
                eq(IndexingAction.DELETE),
                eq("FAILURE"),
                eq("batch-delete-fail"),
                eq(500),
                eq("Server error"),
                eq(10L),
                anyString());
    }

    @Test
    void logsAndSwallowsUnexpectedExceptionsDuringIndexProcessing() throws Exception {
        when(indexingScopeService.isConnectorEnabled()).thenReturn(true);
        when(job.getProperty(IncrementalIndexingDefaults.JOB_PROP_BATCH_ID, String.class)).thenReturn("batch-error");
        when(job.getProperty(IncrementalIndexingDefaults.JOB_PROP_INDEX_PATHS, String[].class))
                .thenReturn(new String[] {PAGE_PATH});
        when(job.getProperty(IncrementalIndexingDefaults.JOB_PROP_DELETE_PATHS, String[].class)).thenReturn(null);
        when(resolverUtil.getServiceResolver()).thenThrow(new RuntimeException("resolver failed"));

        assertEquals(JobResult.OK, jobConsumer.process(job));
    }

    @Test
    void isInScopeReturnsTrueForDeleteAction() throws Exception {
        final var isInScopeMethod = IncrementalIndexingJobConsumer.class.getDeclaredMethod(
                "isInScope",
                ResourceResolver.class,
                String.class,
                IndexingAction.class);
        isInScopeMethod.setAccessible(true);

        final boolean inScope = (boolean) isInScopeMethod.invoke(
                jobConsumer,
                context.resourceResolver(),
                "/content/site/page-1",
                IndexingAction.DELETE);

        assertEquals(true, inScope);
        verify(indexingScopeService, never()).evaluate(any(), anyString());
    }
}

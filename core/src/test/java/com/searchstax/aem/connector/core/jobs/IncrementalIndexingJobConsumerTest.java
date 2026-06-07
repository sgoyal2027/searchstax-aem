package com.searchstax.aem.connector.core.jobs;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.searchstax.aem.connector.core.constants.IncrementalIndexingDefaults;
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

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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
}

package com.searchstax.aem.connector.core.listeners;

import com.day.cq.replication.ReplicationAction;
import com.day.cq.replication.ReplicationActionType;
import com.searchstax.aem.connector.core.incremental.IndexingAction;
import com.searchstax.aem.connector.core.services.IncrementalIndexingQueueService;
import com.searchstax.aem.connector.core.services.IndexingAuditService;
import com.searchstax.aem.connector.core.services.IndexingScopeService;
import com.searchstax.aem.connector.core.services.SearchStaxDocumentBuilderService;
import com.searchstax.aem.connector.core.testcontext.AppAemContext;
import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith({AemContextExtension.class, MockitoExtension.class})
class SearchStaxReplicationPreprocessorTest {

    private final AemContext context = AppAemContext.newAemContext();

    @Mock
    private IndexingScopeService indexingScopeService;

    @Mock
    private IncrementalIndexingQueueService incrementalIndexingQueueService;

    @Mock
    private IndexingAuditService indexingAuditService;

    @Mock
    private SearchStaxDocumentBuilderService documentBuilderService;

    private SearchStaxReplicationPreprocessor preprocessor;

    @BeforeEach
    void setUp() {
        preprocessor = new SearchStaxReplicationPreprocessor();
        context.registerService(IndexingScopeService.class, indexingScopeService);
        context.registerService(IncrementalIndexingQueueService.class, incrementalIndexingQueueService);
        context.registerService(IndexingAuditService.class, indexingAuditService);
        context.registerService(SearchStaxDocumentBuilderService.class, documentBuilderService);
        context.registerInjectActivateService(preprocessor);
    }

    @Test
    void queuesReplicationActivatePaths() throws Exception {
        final ReplicationAction action = mock(ReplicationAction.class);
        when(indexingScopeService.isConnectorEnabled()).thenReturn(true);
        when(action.getType()).thenReturn(ReplicationActionType.ACTIVATE);
        when(action.getPaths()).thenReturn(new String[] {"/content/site/page"});
        when(documentBuilderService.resolveDocumentId("/content/site/page")).thenReturn("/content/site/page");

        preprocessor.preprocess(action, null);

        verify(incrementalIndexingQueueService).enqueue("/content/site/page", IndexingAction.INDEX);
        verify(indexingAuditService).record(
                eq("/content/site/page"),
                eq(IndexingAction.INDEX),
                eq("QUEUED"),
                eq("replication"),
                eq(0),
                anyString(),
                anyLong(),
                eq("/content/site/page"));
    }

    @Test
    void ignoresReplicationWhenConnectorDisabled() throws Exception {
        final ReplicationAction action = mock(ReplicationAction.class);
        when(indexingScopeService.isConnectorEnabled()).thenReturn(false);

        preprocessor.preprocess(action, null);

        verify(incrementalIndexingQueueService, never()).enqueue(anyString(), any());
    }
}
